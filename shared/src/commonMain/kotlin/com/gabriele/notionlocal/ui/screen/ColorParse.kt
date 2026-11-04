package com.gabriele.notionlocal.ui.screen

/**
 * **Un colore scritto come testo**, come lo legge `android.graphics.Color.parseColor`
 * (dal 04/11/2026, nel codice comune): "#RRGGBB", "#AARRGGBB" o uno dei nomi che
 * Android conosce ("red", "darkgray"…). Restituisce il colore in ARGB; un testo
 * che non è un colore dà `IllegalArgumentException`, come su Android.
 */
fun parseColorString(text: String): Int {
    if (text.startsWith("#")) {
        val hex = text.substring(1)
        val value = hex.toLongOrNull(16)
        when {
            value != null && hex.length == 6 -> return (value or 0xFF000000L).toInt()
            value != null && hex.length == 8 -> return value.toInt()
        }
        throw IllegalArgumentException("Unknown color")
    }
    return NAMED_COLORS[text.lowercase()] ?: throw IllegalArgumentException("Unknown color")
}

/** I nomi di `android.graphics.Color.parseColor`, con gli stessi valori. */
private val NAMED_COLORS: Map<String, Int> = mapOf(
    "black" to 0xFF000000.toInt(),
    "darkgray" to 0xFF444444.toInt(),
    "gray" to 0xFF888888.toInt(),
    "lightgray" to 0xFFCCCCCC.toInt(),
    "white" to 0xFFFFFFFF.toInt(),
    "red" to 0xFFFF0000.toInt(),
    "green" to 0xFF00FF00.toInt(),
    "blue" to 0xFF0000FF.toInt(),
    "yellow" to 0xFFFFFF00.toInt(),
    "cyan" to 0xFF00FFFF.toInt(),
    "magenta" to 0xFFFF00FF.toInt(),
    "aqua" to 0xFF00FFFF.toInt(),
    "fuchsia" to 0xFFFF00FF.toInt(),
    "darkgrey" to 0xFF444444.toInt(),
    "grey" to 0xFF888888.toInt(),
    "lightgrey" to 0xFFCCCCCC.toInt(),
    "lime" to 0xFF00FF00.toInt(),
    "maroon" to 0xFF800000.toInt(),
    "navy" to 0xFF000080.toInt(),
    "olive" to 0xFF808000.toInt(),
    "purple" to 0xFF800080.toInt(),
    "silver" to 0xFFC0C0C0.toInt(),
    "teal" to 0xFF008080.toInt(),
)
