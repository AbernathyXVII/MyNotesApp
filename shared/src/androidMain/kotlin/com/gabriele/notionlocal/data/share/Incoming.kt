package com.gabriele.notionlocal.data.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.mutableStateOf

// ---------------------------------------------------------------------------
// **"Condividi con notE"** (dal 02/11/2026, chiesto dall'utente: "da Chrome,
// dalla Galleria o da WhatsApp non puoi mandare niente a notE: manca proprio
// la voce Condividi"). Qui c'è come si legge quello che arriva: il testo, il
// link, i file. Lo mostra e lo salva `ShareActivity`.
// ---------------------------------------------------------------------------

/** Quello che un'altra app manda con "Condividi". */
data class Incoming(
    /** L'oggetto: Chrome ci mette il titolo della pagina. */
    val subject: String?,
    val text: String?,
    /** Le foto, i video, i file: si copiano dentro l'app salvando. */
    val files: List<Uri>,
    /** Il tipo dichiarato ("image/jpeg", "text/plain", o generico con l'asterisco). */
    val mime: String?
) {
    val isEmpty: Boolean get() = text.isNullOrBlank() && files.isEmpty()

    companion object {
        fun from(intent: Intent?): Incoming? {
            intent ?: return null
            if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return null
            val files = buildList {
                if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                    addAll(intent.streams())
                } else {
                    intent.stream()?.let(::add)
                }
            }
            return Incoming(
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()?.takeIf { it.isNotEmpty() },
                text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
                files = files,
                mime = intent.type
            )
        }

        @Suppress("DEPRECATION")
        private fun Intent.stream(): Uri? =
            if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else getParcelableExtra(Intent.EXTRA_STREAM)

        @Suppress("DEPRECATION")
        private fun Intent.streams(): List<Uri> =
            (if (Build.VERSION.SDK_INT >= 33) getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) else getParcelableArrayListExtra(Intent.EXTRA_STREAM))
                .orEmpty()
    }
}

/**
 * **Il testo condiviso, diviso**: il link (se il testo è un indirizzo, o
 * finisce con uno: "Guarda qui https://…"), le righe di testo che restano e
 * il titolo da proporre — l'oggetto, se c'è (Chrome ci mette il titolo della
 * pagina), altrimenti la prima riga, altrimenti il sito del link.
 */
data class SharedText(
    val title: String,
    val link: String?,
    /** Le righe sotto il titolo, per una pagina nuova. */
    val lines: List<String>,
    /** Tutte le righe, per quando non c'è un titolo: in fondo a una pagina che c'è già. */
    val allLines: List<String>
)

private val URL_AT_END = Regex("""(https?://\S+)\s*$""")
private const val TITLE_MAX = 80

fun splitSharedText(text: String?, subject: String?): SharedText {
    val subject = subject?.trim()?.takeIf { it.isNotEmpty() }
    val clean = text.orEmpty().replace("\r\n", "\n").trim()
    val match = URL_AT_END.find(clean)
    val link = match?.groupValues?.get(1)
    val rest = if (match != null) clean.substring(0, match.range.first).trim() else clean
    // Le righe come sono, ma mai due vuote di fila, né in cima o in fondo.
    val lines = mutableListOf<String>()
    for (line in rest.split("\n").map { it.trimEnd() }) {
        if (line.isBlank() && (lines.isEmpty() || lines.last().isBlank())) continue
        lines += line
    }
    while (lines.isNotEmpty() && lines.last().isBlank()) lines.removeAt(lines.lastIndex)
    val firstLine = lines.firstOrNull { it.isNotBlank() }?.trim()
    val title = (subject ?: firstLine ?: link?.let(::hostOf) ?: "").trim()
    // La prima riga diventata titolo non si ripete sotto (e Chrome a volte
    // manda il titolo anche come prima riga); una troppo lunga per un titolo sì.
    val body = if (firstLine != null && firstLine == title && firstLine.length <= TITLE_MAX) {
        lines.dropWhile { it.isBlank() }.drop(1).dropWhile { it.isBlank() }
    } else {
        lines
    }
    return SharedText(title.take(TITLE_MAX), link, body, lines)
}

/** "https://www.example.com/a/b" → "example.com". */
fun hostOf(url: String): String? =
    runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotEmpty() }

/** Vero se [intent] chiede una nota al volo; letto una volta: ruotando lo schermo non ne nasce un'altra. */
fun QuickNotes.takeFrom(intent: Intent?): Boolean {
    if (intent?.action != ACTION) return false
    intent.action = Intent.ACTION_MAIN
    return true
}
