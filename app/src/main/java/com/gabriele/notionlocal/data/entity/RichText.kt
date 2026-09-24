package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable

/**
 * Un singolo "span" di testo con formattazione. Un blocco (BlockEntity)
 * contiene una lista di questi span serializzata in JSON: questo è ciò
 * che permette "questa parola in grassetto, questa in corsivo" dentro
 * lo stesso paragrafo — esattamente come funziona in Notion.
 *
 * Esempio: "Ciao **mondo**" diventa:
 *   [RichTextSpan("Ciao "), RichTextSpan("mondo", bold=true)]
 */
@Serializable
data class RichTextSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    /**
     * Testo coperto, come su Discord e Telegram: si vede una fascia
     * grigia al posto delle lettere finché non lo si tocca.
     *
     * Ha un valore predefinito come tutti gli altri, quindi il testo
     * già salvato — che questa chiave non ce l'ha — continua a
     * leggersi senza migrazione: manca il campo, vale `false`.
     */
    val spoiler: Boolean = false,
    /**
     * Colore del testo e colore dietro al testo, scritti come
     * esadecimale con il cancelletto (`#RRGGBB`). `null` vuol dire "il
     * colore di sempre": il bianco del tema per il testo, niente per lo
     * sfondo.
     *
     * Una stringa e non un intero perché è la stessa forma in cui i
     * colori si copiano da fuori — è quello che l'utente scrive nel
     * riquadro "Hex" — e perché nel JSON del testo si legge a occhio.
     */
    val color: String? = null,
    val background: String? = null
)

/** Il testo semplice risultante da una lista di span, concatenati in ordine. */
fun List<RichTextSpan>.plainText(): String = joinToString("") { it.text }

/**
 * Unisce gli span attaccati che hanno **la stessa identica
 * formattazione**: `["Ci"]["a"]["o"]` diventa `["Ciao"]`.
 *
 * Senza, ogni tasto battuto lascia il suo pezzetto separato — la
 * modifica si applica solo al tratto cambiato, e quel tratto resta uno
 * span a sé — e il JSON di un paragrafo cresce di una voce per lettera
 * scritta. Non è sbagliato da leggere, ma è un file che si gonfia per
 * niente e che non si riesce più a guardare a occhio quando qualcosa
 * va storto.
 */
private fun List<RichTextSpan>.merged(): List<RichTextSpan> {
    if (size < 2) return this
    val result = mutableListOf<RichTextSpan>()
    for (span in this) {
        val last = result.lastOrNull()
        // Confronto a testo vuoto: così a contare è solo la
        // formattazione, qualunque campo le si aggiunga in futuro.
        if (last != null && last.copy(text = "") == span.copy(text = "")) {
            result[result.size - 1] = last.copy(text = last.text + span.text)
        } else {
            result.add(span)
        }
    }
    return result
}

/**
 * Restituisce lo span (o meglio, la sua formattazione) a cui appartiene
 * la posizione data — usato per far ereditare al testo appena digitato
 * la formattazione del punto in cui viene inserito (scrivere in mezzo a
 * una parola in grassetto resta grassetto, come in qualsiasi editor).
 */
private fun formatAtPosition(spans: List<RichTextSpan>, position: Int): RichTextSpan {
    var pos = 0
    for (span in spans) {
        val spanEnd = pos + span.text.length
        if (position < spanEnd) return span
        pos = spanEnd
    }
    // **In fondo al testo lo spoiler non si eredita.** Per tutto il
    // resto continuare a scrivere continua anche la formattazione, ed è
    // quello che ci si aspetta da qualsiasi editor. Un testo coperto no:
    // finito di scriverlo il cursore resta subito dopo, e la parola
    // seguente diventerebbe invisibile senza che nessuno l'abbia chiesto.
    // Per allungare uno spoiler si seleziona e si tocca il pulsante.
    return spans.lastOrNull()?.copy(spoiler = false) ?: RichTextSpan(text = "")
}

/**
 * Applica alla lista di span la stessa modifica subita dal testo grezzo,
 * passando da oldText a newText — sostituisce solo la porzione
 * effettivamente cambiata (calcolata confrontando prefisso e suffisso
 * comuni tra vecchio e nuovo testo), lasciando invariato — e con la sua
 * formattazione intatta — tutto il resto. Il testo inserito eredita la
 * formattazione del punto in cui viene inserito.
 */
fun applyTextEdit(spans: List<RichTextSpan>, oldText: String, newText: String): List<RichTextSpan> {
    if (oldText == newText) return spans

    var prefixLen = 0
    val minLen = minOf(oldText.length, newText.length)
    while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) prefixLen++

    var suffixLen = 0
    val maxSuffix = minLen - prefixLen
    while (suffixLen < maxSuffix &&
        oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
    ) suffixLen++

    val deleteStart = prefixLen
    val deleteEnd = oldText.length - suffixLen
    val insertText = newText.substring(prefixLen, newText.length - suffixLen)

    return spliceSpans(spans, deleteStart, deleteEnd, insertText)
}

/**
 * Sostituisce l'intervallo [deleteStart, deleteEnd) del testo con
 * insertText, spezzando gli span ai bordi dove necessario. Il testo
 * inserito eredita la formattazione dello span che copre deleteStart.
 */
fun spliceSpans(spans: List<RichTextSpan>, deleteStart: Int, deleteEnd: Int, insertText: String): List<RichTextSpan> {
    val inheritedFormat = if (insertText.isNotEmpty()) formatAtPosition(spans, deleteStart) else null

    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    var inserted = false
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        when {
            spanEnd <= deleteStart -> result.add(span)
            spanStart >= deleteEnd -> {
                if (!inserted && inheritedFormat != null) {
                    result.add(inheritedFormat.copy(text = insertText))
                    inserted = true
                }
                result.add(span)
            }
            else -> {
                if (spanStart < deleteStart) {
                    result.add(span.copy(text = span.text.substring(0, deleteStart - spanStart)))
                }
                if (!inserted && inheritedFormat != null) {
                    result.add(inheritedFormat.copy(text = insertText))
                    inserted = true
                }
                if (spanEnd > deleteEnd) {
                    result.add(span.copy(text = span.text.substring(deleteEnd - spanStart)))
                }
            }
        }
    }
    if (!inserted && inheritedFormat != null) {
        result.add(inheritedFormat.copy(text = insertText))
    }
    return result.filter { it.text.isNotEmpty() }.merged()
        .ifEmpty { listOf(RichTextSpan(text = "")) }
}

/**
 * Applica (o toglie) una formattazione a un intervallo di caratteri
 * dentro una lista di span, spezzando gli span esattamente ai bordi
 * della selezione. Se anche un solo carattere nell'intervallo non ha
 * già quella formattazione, l'azione la ATTIVA per tutto l'intervallo;
 * se ce l'hanno già tutti, la TOGLIE — lo standard in ogni editor
 * (Word, Google Docs, Notion).
 */
fun toggleFormatInRange(
    spans: List<RichTextSpan>,
    start: Int,
    end: Int,
    getFlag: (RichTextSpan) -> Boolean,
    setFlag: (RichTextSpan, Boolean) -> RichTextSpan
): List<RichTextSpan> {
    if (start >= end) return spans

    var pos = 0
    var allHaveFlag = true
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd
        val overlapStart = maxOf(spanStart, start)
        val overlapEnd = minOf(spanEnd, end)
        if (overlapStart < overlapEnd && !getFlag(span)) {
            allHaveFlag = false
        }
    }
    val newValue = !allHaveFlag

    val result = mutableListOf<RichTextSpan>()
    pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        if (spanEnd <= start || spanStart >= end) {
            result.add(span)
            continue
        }

        if (spanStart < start) {
            result.add(span.copy(text = span.text.substring(0, start - spanStart)))
        }
        val innerStart = maxOf(spanStart, start)
        val innerEnd = minOf(spanEnd, end)
        val innerText = span.text.substring(innerStart - spanStart, innerEnd - spanStart)
        result.add(setFlag(span.copy(text = innerText), newValue))
        if (spanEnd > end) {
            result.add(span.copy(text = span.text.substring(end - spanStart)))
        }
    }
    return result.filter { it.text.isNotEmpty() }.merged()
}

/**
 * Come `toggleFormatInRange`, ma **accende e basta**: non guarda cosa
 * c'era prima. Serve quando la formattazione non è una scelta dell'utente
 * su una selezione ma la conseguenza di quello che ha scritto — `||...||`
 * vuol dire "coprilo", non "invertilo", e se per caso quel pezzo era già
 * coperto la versione a interruttore lo scoprirebbe.
 */
fun setFormatInRange(
    spans: List<RichTextSpan>,
    start: Int,
    end: Int,
    setFlag: (RichTextSpan, Boolean) -> RichTextSpan
): List<RichTextSpan> {
    if (start >= end) return spans

    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        if (spanEnd <= start || spanStart >= end) {
            result.add(span)
            continue
        }
        if (spanStart < start) {
            result.add(span.copy(text = span.text.substring(0, start - spanStart)))
        }
        val innerStart = maxOf(spanStart, start)
        val innerEnd = minOf(spanEnd, end)
        result.add(
            setFlag(span.copy(text = span.text.substring(innerStart - spanStart, innerEnd - spanStart)), true)
        )
        if (spanEnd > end) {
            result.add(span.copy(text = span.text.substring(end - spanStart)))
        }
    }
    return result.filter { it.text.isNotEmpty() }.merged()
}

/**
 * Cambia il colore del testo, o quello dietro al testo, su un
 * intervallo di caratteri.
 *
 * Non è un interruttore come grassetto e corsivo: un colore si
 * **imposta**, non si inverte, e `null` lo toglie riportando il pezzo
 * al colore di sempre. Per il resto spezza gli span ai bordi della
 * selezione esattamente come `toggleFormatInRange`.
 */
fun setColorInRange(
    spans: List<RichTextSpan>,
    start: Int,
    end: Int,
    background: Boolean,
    hex: String?
): List<RichTextSpan> {
    if (start >= end) return spans

    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        if (spanEnd <= start || spanStart >= end) {
            result.add(span)
            continue
        }
        if (spanStart < start) {
            result.add(span.copy(text = span.text.substring(0, start - spanStart)))
        }
        val innerStart = maxOf(spanStart, start)
        val innerEnd = minOf(spanEnd, end)
        val inner = span.copy(text = span.text.substring(innerStart - spanStart, innerEnd - spanStart))
        result.add(if (background) inner.copy(background = hex) else inner.copy(color = hex))
        if (spanEnd > end) {
            result.add(span.copy(text = span.text.substring(end - spanStart)))
        }
    }
    return result.filter { it.text.isNotEmpty() }.merged()
}

/**
 * Spezza una lista di span sui ritorni a capo: una lista per riga.
 *
 * Il testo scorrevole è un campo solo ma tanti blocchi, uno per riga, e
 * ogni blocco tiene i propri span. Per far sopravvivere la formattazione
 * a una modifica che attraversa più righe conviene ragionare una volta
 * sola su tutto il pezzo e poi rimetterlo in righe: è quello che fa
 * questa, insieme a `joinLines`.
 */
fun List<RichTextSpan>.splitLines(): List<List<RichTextSpan>> {
    val lines = mutableListOf<List<RichTextSpan>>()
    var current = mutableListOf<RichTextSpan>()
    for (span in this) {
        val pieces = span.text.split('\n')
        for ((i, piece) in pieces.withIndex()) {
            if (i > 0) {
                lines.add(current)
                current = mutableListOf()
            }
            if (piece.isNotEmpty()) current.add(span.copy(text = piece))
        }
    }
    lines.add(current)
    return lines.map { it.ifEmpty { listOf(RichTextSpan(text = "")) } }
}

/**
 * Il contrario di `splitLines`: rimette insieme le righe con un a-capo
 * fra una e l'altra. L'a-capo è uno span **senza formattazione**, così è
 * anche il confine oltre il quale la formattazione non si propaga.
 */
fun List<List<RichTextSpan>>.joinLines(): List<RichTextSpan> {
    val result = mutableListOf<RichTextSpan>()
    for ((i, line) in this.withIndex()) {
        if (i > 0) result.add(RichTextSpan(text = "\n"))
        result.addAll(line.filter { it.text.isNotEmpty() })
    }
    return result.ifEmpty { listOf(RichTextSpan(text = "")) }
}
