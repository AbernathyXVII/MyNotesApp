package com.gabriele.notionlocal.ui.screen

/**
 * L'indirizzo da aprire: com'è scritto se ha già il suo inizio
 * ("https://…", "mailto:…", "tel:…"), altrimenti con "https://" davanti —
 * "bulbapedia.bulbagarden.net/wiki/…" scritto a mano si apre lo stesso.
 */
fun linkUri(raw: String): String {
    val link = raw.trim()
    val hasScheme = link.contains("://") || listOf("mailto:", "tel:", "geo:").any { link.startsWith(it, ignoreCase = true) }
    return if (hasScheme) link else "https://$link"
}
