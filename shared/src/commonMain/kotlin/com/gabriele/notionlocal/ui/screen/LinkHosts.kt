package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.platform.urlHost
import com.gabriele.notionlocal.platform.urlScheme

/** Un indirizzo pulito: "https://" se manca, e almeno un punto nel nome del sito. Null se non è un link. */
fun normalizedLink(input: String): String? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(text)) text else "https://$text"
    val host = urlHost(withScheme) ?: return null
    if (urlScheme(withScheme)?.lowercase() !in setOf("http", "https") || !host.contains('.')) return null
    return withScheme
}

/** Il nome del sito di un indirizzo, senza "www.": "youtube.com". */
fun linkHost(url: String): String =
    urlHost(url)?.removePrefix("www.") ?: url
