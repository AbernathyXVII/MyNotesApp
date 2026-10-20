package com.gabriele.notionlocal.data.webclip

import com.gabriele.notionlocal.data.entity.RichTextSpan
import java.net.URI

/** Quello che si è scelto nel pannello del Web Clipper. */
data class WebClipRequest(val url: String, val mode: WebClipMode, val title: String, val note: String)

/** Il grigio della riga "Clipped from", come il testo secondario di Notion. */
const val CLIP_SOURCE_COLOR = "#9B9A97"

/**
 * **Il link scritto o incollato nel Web Clipper**, pronto da aprire: senza
 * spazi ai lati, con "https://" davanti se manca ("game8.co/…" va bene). Null
 * se non è l'indirizzo di un sito: serve un nome con un punto, e solo
 * http o https.
 */
fun webClipUrl(input: String): String? {
    val text = input.trim().takeIf { it.isNotEmpty() && it.none { c -> c.isWhitespace() } } ?: return null
    val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(text)) text else "https://$text"
    val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
    val host = uri.host ?: return null
    if (!host.contains('.') || host.startsWith('.') || host.endsWith('.')) return null
    return withScheme
}

/** Il nome del sito di un link ("game8.co"), per chiamare la foto quando non ha un titolo. */
fun webClipHost(url: String): String = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: url

/**
 * **Le righe sotto la foto** (dal 20/10/2026): il titolo in grassetto, la
 * nota (una riga per ogni sua riga), e "Clipped from" col link, piccolo e
 * grigio, come OneNote. Quelle vuote non ci sono.
 */
fun webClipCaptions(title: String?, note: String?, url: String?, clippedFrom: String): List<List<RichTextSpan>> {
    val lines = mutableListOf<List<RichTextSpan>>()
    title?.trim()?.takeIf { it.isNotEmpty() }?.let { lines += listOf(RichTextSpan(text = it.replace('\n', ' '), bold = true)) }
    note?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { lines += listOf(RichTextSpan(text = it)) }
    url?.takeIf { it.isNotBlank() }?.let {
        lines += listOf(
            RichTextSpan(text = "$clippedFrom ", color = CLIP_SOURCE_COLOR, size = CLIP_SOURCE_SIZE),
            RichTextSpan(text = it, link = it, size = CLIP_SOURCE_SIZE)
        )
    }
    return lines
}

/** La grandezza della riga "Clipped from", in punti: più piccola del testo normale (16, vedi `RichTextSpan.size`). */
const val CLIP_SOURCE_SIZE = 13
