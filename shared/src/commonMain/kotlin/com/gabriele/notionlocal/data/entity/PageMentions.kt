package com.gabriele.notionlocal.data.entity

import java.time.LocalDate
import java.util.UUID

// ---------------------------------------------------------------------------
// **Le menzioni "@"** (dal 03/11/2026, chiesto dall'utente: "le menzioni @
// dentro il testo per citare una pagina o una data al volo"). Una menzione è
// un pezzo di testo col suo collegamento, come quelli alle righe della pagina
// (`BlockLinks.kt`):
//
// - a una pagina, `note://page/<id della pagina>`: il testo è il titolo, e
//   toccandolo la pagina si apre. Il titolo si rimette in pari aprendo la
//   pagina che contiene la menzione (vedi `refreshPageMentions`);
// - a una data, `note://date/2026-11-03?m=<segno>`: il testo è la data
//   scritta per intero, e toccandola la si cambia. Il segno distingue due
//   menzioni dello stesso giorno, così se ne cambia una sola.
//
// Le menzioni di una pagina sono anche i suoi **backlink**: in fondo alla
// pagina citata si vede chi la cita (vedi `PageRepository.backlinksOf`).
// ---------------------------------------------------------------------------

const val PAGE_LINK_PREFIX = "note://page/"
const val DATE_LINK_PREFIX = "note://date/"

/** I collegamenti che non escono dall'app: righe, pagine, date. */
fun isInternalLink(link: String?): Boolean = link?.startsWith("note://") == true

fun pageLink(pageId: String): String = PAGE_LINK_PREFIX + pageId

/** La pagina a cui porta una menzione, o null se porta da un'altra parte. */
fun pageLinkTarget(link: String?): String? =
    link?.takeIf { it.startsWith(PAGE_LINK_PREFIX) }?.removePrefix(PAGE_LINK_PREFIX)?.takeIf { it.isNotBlank() }

/** Una menzione nuova del giorno [date], col suo segno. */
fun dateLink(date: LocalDate, mark: String = UUID.randomUUID().toString().take(8)): String =
    "$DATE_LINK_PREFIX$date?m=$mark"

/** Il giorno di una menzione di data, o null se il collegamento non è una data. */
fun dateLinkTarget(link: String?): LocalDate? {
    val rest = link?.takeIf { it.startsWith(DATE_LINK_PREFIX) }?.removePrefix(DATE_LINK_PREFIX) ?: return null
    return runCatching { LocalDate.parse(rest.substringBefore('?')) }.getOrNull()
}

/** La stessa menzione di data (lo stesso segno), spostata al giorno [date]. */
fun movedDateLink(link: String, date: LocalDate): String {
    val mark = link.substringAfter("?m=", "").takeIf { it.isNotEmpty() }
    return if (mark != null) dateLink(date, mark) else dateLink(date)
}

/** Le pagine menzionate nel testo. */
fun mentionedPageIds(spans: List<RichTextSpan>): Set<String> =
    spans.mapNotNullTo(LinkedHashSet()) { pageLinkTarget(it.link) }

/**
 * **Il testo di ogni pezzo con il collegamento [link]** diventa [text] (e il
 * collegamento [newLink]), tenendo la formattazione del suo primo pezzo. Un
 * collegamento può stare su più pezzi di fila (metà in grassetto): diventano
 * uno. Null se non c'è niente da cambiare.
 */
fun replaceLinkedText(spans: List<RichTextSpan>, link: String, text: String, newLink: String = link): List<RichTextSpan>? {
    val result = ArrayList<RichTextSpan>(spans.size)
    var changed = false
    var i = 0
    while (i < spans.size) {
        val span = spans[i]
        if (span.link != link) {
            result += span
            i++
            continue
        }
        var j = i
        val old = StringBuilder()
        while (j < spans.size && spans[j].link == link) old.append(spans[j++].text)
        if (old.toString() != text || newLink != link || j - i > 1) changed = true
        result += span.copy(text = text, link = newLink)
        i = j
    }
    return result.takeIf { changed }
}

/**
 * **I titoli delle pagine menzionate rimessi in pari**: ogni menzione di una
 * pagina di [titles] prende il suo titolo di adesso. Null se erano già tutti
 * giusti. Una pagina che non c'è più (non in [titles]) resta col titolo che
 * aveva.
 */
fun refreshPageMentions(spans: List<RichTextSpan>, titles: Map<String, String>, untitled: String): List<RichTextSpan>? {
    var current = spans
    var changed = false
    for (id in mentionedPageIds(spans)) {
        val title = titles[id] ?: continue
        val shown = title.ifBlank { untitled }.replace('\n', ' ')
        replaceLinkedText(current, pageLink(id), shown)?.let {
            current = it
            changed = true
        }
    }
    return current.takeIf { changed }
}
