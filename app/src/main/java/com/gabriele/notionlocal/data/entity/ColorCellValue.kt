package com.gabriele.notionlocal.data.entity

/**
 * **Il valore di una cella "Color cell"** (dal 26/09/2026): un colore che
 * riempie la cella, e — se si vuole — del testo scritto sopra.
 *
 * Si salva in una stringa sola, come tutti i valori delle celle:
 * `"#RRGGBB|testo"`, oppure solo `"#RRGGBB"`, oppure solo il testo. Il
 * testo può contenere qualsiasi cosa, anche `|`: il colore sono sempre i
 * primi sette caratteri. Un testo senza colore che comincia con `#` o `|`
 * si salva con una `|` davanti, così non lo si scambia per un colore.
 *
 * Cambiando il tipo della colonna i valori restano quelli (vedi
 * `DatabaseViewModel.updateColumn`): una colonna di testo che diventa
 * "Color cell" mostra il suo testo, senza colore.
 */
data class ColorCellValue(
    /** "#RRGGBB" in maiuscolo, o null per nessun colore. */
    val color: String?,
    val text: String
) {
    val isEmpty: Boolean get() = color == null && text.isEmpty()
}

private val COLOR_PREFIX = Regex("^#[0-9A-Fa-f]{6}")

fun parseColorCell(value: String): ColorCellValue {
    if (value.startsWith("|")) return ColorCellValue(null, value.substring(1))
    val color = COLOR_PREFIX.find(value)?.value ?: return ColorCellValue(null, value)
    val rest = value.substring(color.length)
    return when {
        rest.isEmpty() -> ColorCellValue(color.uppercase(), "")
        rest.startsWith("|") -> ColorCellValue(color.uppercase(), rest.substring(1))
        // "#123456789": non era un colore, era un testo che gli somiglia.
        else -> ColorCellValue(null, value)
    }
}

fun encodeColorCell(cell: ColorCellValue): String {
    val color = cell.color?.uppercase()
    return when {
        color == null && (cell.text.startsWith("#") || cell.text.startsWith("|")) -> "|${cell.text}"
        color == null -> cell.text
        cell.text.isEmpty() -> color
        else -> "$color|${cell.text}"
    }
}

/**
 * Se sopra questo colore il testo si legge meglio nero (true) o bianco:
 * la luminosità percepita, come fanno i fogli di calcolo. La soglia è a
 * metà (128): più in alto, il verde acceso scelto nella prova del
 * 26/09/2026 (#12F20D) prendeva il bianco, e non si leggeva.
 */
fun darkTextOn(hex: String): Boolean {
    val rgb = hex.removePrefix("#").toIntOrNull(16) ?: return false
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b) >= 128
}
