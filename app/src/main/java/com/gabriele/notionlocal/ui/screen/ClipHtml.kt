package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.entity.BlockType
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// **"Keep Source Formatting" legge com'era fatto il testo copiato** (dal
// 20/10/2026, chiesto dall'utente, che copia tutto da OneNote): dall'HTML
// che OneNote, Word o un browser mettono negli appunti si ricavano le righe,
// gli elenchi puntati e numerati col loro rientro, le tabelle (cella per
// cella) e, pezzo per pezzo, grassetto, corsivo, sottolineato, barrato,
// collegamenti e **grandezza**. Vedi `LinkClipboard.sourceFormatted`.
// ---------------------------------------------------------------------------

/** Un pezzo di testo copiato col suo stile; [sizePt] è la grandezza nella fonte, in punti (null: non detta). */
internal data class ClipRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val sizePt: Float? = null,
    val link: String? = null
)

/** Una riga copiata: il testo a pezzi, il tipo e il rientro; per una tabella, le celle riga per riga. */
internal data class ClipLine(
    val runs: List<ClipRun>,
    val type: BlockType = BlockType.PARAGRAPH,
    val indent: Int = 0,
    val table: List<List<String>>? = null
) {
    val text: String get() = runs.joinToString("") { it.text }
}

/** La grandezza normale delle pagine di notE (vedi `RichTextSpan.size`). */
private const val NOTE_BODY_SIZE = 16

/** Quella di un browser quando la pagina non ne dice una: 16px. */
private const val DEFAULT_SOURCE_PT = 12f

private val BLOCK_TAGS = setOf(
    "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "blockquote", "pre", "section", "article",
    "header", "footer", "dt", "dd", "dl", "figure", "figcaption", "address", "center", "main", "aside", "nav"
)
private val VOID_TAGS = setOf("br", "img", "hr", "meta", "link", "input", "col", "wbr", "area", "base", "source")
private val HEADING_SCALE = mapOf("h1" to 2f, "h2" to 1.5f, "h3" to 1.17f, "h4" to 1f, "h5" to 0.83f, "h6" to 0.67f)
private val SIZE_KEYWORDS = mapOf(
    "xx-small" to 7f, "x-small" to 7.5f, "small" to 10f, "medium" to 12f,
    "large" to 13.5f, "x-large" to 18f, "xx-large" to 24f, "xxx-large" to 36f
)
private val FONT_TAG_SIZES = mapOf(1 to 7.5f, 2 to 10f, 3 to 12f, 4 to 13.5f, 5 to 18f, 6 to 24f, 7 to 36f)

/**
 * Le righe del testo copiato, dall'HTML degli appunti. Il frammento copiato
 * (fra `StartFragment` ed `EndFragment`, come lo segnano Windows e Office) se
 * c'è; commenti, stili e script non sono testo.
 */
internal fun parseClipHtml(html: String): List<ClipLine> {
    val fragment = Regex("<!--StartFragment-->(.*?)<!--EndFragment-->", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1)
        ?: Regex("<body[^>]*>(.*)</body>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(html)?.groupValues?.get(1)
        ?: html
    val cleaned = fragment
        .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<(style|script|head|title|noscript|template)\\b[^>]*>.*?</\\1\\s*>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
    val parser = ClipParser()
    // OneNote e Word scrivono il carattere di tutto il testo sul `<body>`, fuori dal frammento.
    Regex("<body([^>]*)>", RegexOption.IGNORE_CASE).find(html)?.let { parser.open("body", it.groupValues[1]) }
    for (match in Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>|([^<]+)").findAll(cleaned)) {
        val text = match.groupValues[4]
        when {
            text.isNotEmpty() -> parser.text(decodeEntities(text))
            match.groupValues[1] == "/" -> parser.close(match.groupValues[2].lowercase())
            else -> parser.open(match.groupValues[2].lowercase(), match.groupValues[3])
        }
    }
    return parser.finish()
}

/**
 * **Le grandezze in punti di notE**: il corpo più usato nel testo copiato
 * diventa quello normale di notE, le altre in proporzione. Così il testo
 * normale di OneNote (11 punti) resta normale, e un titolo a 20 punti diventa
 * grande come in OneNote rispetto al resto (29). Null = quella normale.
 */
internal fun noteSizes(lines: List<ClipLine>): (Float?) -> Int? {
    val weights = HashMap<Float, Int>()
    if (lines.none { line -> line.runs.any { it.sizePt != null } }) return { null }
    for (line in lines) for (run in line.runs) {
        // Il testo che non dice la sua grandezza ha quella di sempre di un
        // browser: sulle pagine web spesso solo i titoli la dicono.
        val pt = run.sizePt ?: DEFAULT_SOURCE_PT
        if (run.text.isBlank() || line.table != null) continue
        val key = (pt * 2).roundToInt() / 2f
        weights[key] = (weights[key] ?: 0) + run.text.length
    }
    // A parità vince la più piccola: di solito è il testo, e i titoli sono di meno.
    val base = weights.entries.maxWithOrNull(compareBy<Map.Entry<Float, Int>> { it.value }.thenByDescending { it.key })?.key
        ?: DEFAULT_SOURCE_PT
    return { pt ->
        pt?.let { (NOTE_BODY_SIZE * it / base).roundToInt().coerceIn(5, 72) }?.takeIf { it != NOTE_BODY_SIZE }
    }
}

/** Come sta il testo dentro un elemento: quello che dice lui, e quello che eredita. */
private data class Frame(
    val tag: String,
    val bold: Boolean?,
    val italic: Boolean,
    val underline: Boolean,
    val strike: Boolean,
    val sizePt: Float?,
    val link: String?
)

private class ClipParser {
    private val lines = mutableListOf<ClipLine>()
    private val frames = mutableListOf<Frame>()
    private val lists = mutableListOf<Boolean>()

    private var runs = mutableListOf<ClipRun>()
    private var type = BlockType.PARAGRAPH
    private var indent = 0
    /** Una riga vuota voluta (un `&nbsp;` o un `<br>`): si tiene anche senza testo. */
    private var keepEmpty = false

    private var tableDepth = 0
    private var rows: MutableList<MutableList<StringBuilder>>? = null
    private var inCell = false

    private val bold get() = frames.lastOrNull { it.bold != null }?.bold == true
    private val italic get() = frames.any { it.italic }
    private val underline get() = frames.any { it.underline }
    private val strike get() = frames.any { it.strike }
    private val sizePt get() = frames.lastOrNull { it.sizePt != null }?.sizePt
    private val link get() = frames.lastOrNull { it.link != null }?.link

    fun text(raw: String) {
        if (tableDepth > 0) {
            if (!inCell) return
            val cell = rows?.lastOrNull()?.lastOrNull() ?: return
            val piece = raw.replace('\u00A0', ' ').replace(Regex("\\s+"), " ")
            if (cell.isEmpty() || cell.last() == '\n') cell.append(piece.trimStart()) else cell.append(piece)
            return
        }
        var piece = raw.replace(Regex("[ \\t\\r\\n\\u000B\\u000C]+"), " ")
        if (piece.isNotEmpty() && piece.all { it == '\u00A0' || it == ' ' } && piece.contains('\u00A0')) keepEmpty = true
        piece = piece.replace('\u00A0', ' ')
        if (runs.isEmpty() || runs.all { it.text.isBlank() }) piece = piece.trimStart()
        if (piece.isEmpty()) return
        val run = ClipRun(piece, bold, italic, underline, strike, sizePt, link)
        val last = runs.lastOrNull()
        if (last != null && last.copy(text = "") == run.copy(text = "")) {
            runs[runs.lastIndex] = last.copy(text = last.text + piece)
        } else {
            runs += run
        }
    }

    fun open(tag: String, attrs: String) {
        if (tag in VOID_TAGS || attrs.trimEnd().endsWith("/")) {
            if (tag == "br") lineBreak()
            return
        }
        when {
            tag == "table" -> {
                if (tableDepth == 0) {
                    flushLine()
                    rows = mutableListOf()
                }
                tableDepth++
            }
            tableDepth == 1 && tag == "tr" -> rows?.add(mutableListOf())
            tableDepth == 1 && (tag == "td" || tag == "th") -> {
                val table = rows ?: return
                if (table.isEmpty()) table.add(mutableListOf())
                table.last().add(StringBuilder())
                inCell = true
            }
            tableDepth > 0 -> if (inCell && tag in BLOCK_TAGS) cellBreak()
            tag == "ul" || tag == "ol" -> {
                flushLine()
                lists += tag == "ol"
            }
            tag == "li" -> {
                flushLine()
                type = if (lists.lastOrNull() == true) BlockType.NUMBERED_LIST_ITEM else BlockType.BULLET_LIST_ITEM
                indent = (lists.size - 1).coerceAtLeast(0)
            }
            tag in BLOCK_TAGS -> flushLine()
        }
        frames += frameOf(tag, attrs)
    }

    fun close(tag: String) {
        val at = frames.indexOfLast { it.tag == tag }
        if (at >= 0) while (frames.size > at) frames.removeAt(frames.lastIndex)
        when {
            tag == "table" && tableDepth > 0 -> {
                tableDepth--
                if (tableDepth == 0) emitTable()
            }
            tableDepth == 1 && (tag == "td" || tag == "th") -> inCell = false
            tableDepth > 0 -> if (inCell && tag in BLOCK_TAGS) cellBreak()
            tag == "ul" || tag == "ol" -> {
                flushLine()
                if (lists.isNotEmpty()) lists.removeAt(lists.lastIndex)
            }
            tag in BLOCK_TAGS -> flushLine()
        }
    }

    fun finish(): List<ClipLine> {
        flushLine()
        if (tableDepth > 0) emitTable()
        val first = lines.indexOfFirst { it.text.isNotBlank() || it.table != null }
        if (first < 0) return emptyList()
        val last = lines.indexOfLast { it.text.isNotBlank() || it.table != null }
        return lines.subList(first, last + 1).toList()
    }

    private fun lineBreak() {
        if (tableDepth > 0) {
            if (inCell) rows?.lastOrNull()?.lastOrNull()?.append('\n')
            return
        }
        keepEmpty = true
        flushLine()
    }

    private fun cellBreak() {
        val cell = rows?.lastOrNull()?.lastOrNull() ?: return
        if (cell.isNotEmpty() && cell.last() != '\n') cell.append('\n')
    }

    private fun flushLine() {
        // Gli spazi in fondo alla riga non sono testo.
        while (runs.isNotEmpty() && runs.last().text.isBlank()) runs.removeAt(runs.lastIndex)
        runs.lastOrNull()?.let { runs[runs.lastIndex] = it.copy(text = it.text.trimEnd()) }
        if (runs.isNotEmpty() || keepEmpty) lines += ClipLine(runs.toList(), if (runs.isEmpty()) BlockType.PARAGRAPH else type, indent)
        runs = mutableListOf()
        keepEmpty = false
        type = BlockType.PARAGRAPH
        indent = 0
    }

    private fun emitTable() {
        val table = rows ?: return
        rows = null
        inCell = false
        val cells = table
            .map { row -> row.map { cell -> cell.toString().lines().joinToString("\n") { it.trim() }.trim('\n', ' ') } }
            .filter { row -> row.isNotEmpty() }
        if (cells.isEmpty()) return
        val cols = cells.maxOf { it.size }
        // Una tabella di una cella sola è un riquadro per l'impaginazione, non una tabella: resta testo.
        if (cells.size == 1 && cols == 1) {
            cells[0][0].split('\n').forEach { lines += ClipLine(listOf(ClipRun(it))) }
            return
        }
        val square = cells.map { row -> row + List(cols - row.size) { "" } }
        val label = square.first().filter { it.isNotBlank() }.joinToString(" | ") { it.replace('\n', ' ') }.ifBlank { "Table" }
        lines += ClipLine(listOf(ClipRun(label)), BlockType.TABLE, 0, square)
    }

    private fun frameOf(tag: String, attrs: String): Frame {
        val style = Regex("style\\s*=\\s*(\"[^\"]*\"|'[^']*')", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.lowercase().orEmpty()
        val weight = Regex("font-weight\\s*:\\s*([a-z0-9]+)").find(style)?.groupValues?.get(1)
        val boldStyle = weight == "bold" || weight == "bolder" || (weight?.toIntOrNull() ?: 0) >= 600
        val normal = weight == "normal" || weight == "400" || weight == "lighter"
        val decoration = Regex("text-decoration(-line)?\\s*:\\s*([^;\"']*)").find(style)?.groupValues?.get(2).orEmpty()
        val parentPt = sizePt
        val heading = HEADING_SCALE[tag]
        val size = cssSize(style, parentPt)
            ?: (if (tag == "font") fontTagSize(attrs) else null)
            ?: heading?.let { (parentPt ?: DEFAULT_SOURCE_PT) * it }
        val href = if (tag == "a") {
            Regex("href\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE).find(attrs)?.let { m ->
                decodeEntities(m.groupValues[2].ifEmpty { m.groupValues[3] }.ifEmpty { m.groupValues[4] })
            }?.takeIf { usableLink(it) }
        } else {
            null
        }
        return Frame(
            tag = tag,
            bold = when {
                normal -> false
                tag == "b" || tag == "strong" || boldStyle || heading != null -> true
                else -> null
            },
            italic = tag == "i" || tag == "em" || Regex("font-style\\s*:\\s*italic").containsMatchIn(style),
            underline = tag == "u" || "underline" in decoration,
            strike = tag == "s" || tag == "strike" || tag == "del" || "line-through" in decoration,
            sizePt = size,
            link = href
        )
    }

    /** "font-size" in punti, pixel, em, rem o percentuale, o a parole ("large"). */
    private fun cssSize(style: String, parentPt: Float?): Float? {
        val match = Regex("font-size\\s*:\\s*([0-9.]+)\\s*(pt|px|em|rem|%)?").find(style)
        if (match == null) {
            val word = Regex("font-size\\s*:\\s*([a-z-]+)").find(style)?.groupValues?.get(1) ?: return null
            return SIZE_KEYWORDS[word]
        }
        val value = match.groupValues[1].toFloatOrNull()?.takeIf { it > 0f } ?: return null
        return when (match.groupValues[2]) {
            "pt" -> value
            "em" -> (parentPt ?: DEFAULT_SOURCE_PT) * value
            "rem" -> DEFAULT_SOURCE_PT * value
            "%" -> (parentPt ?: DEFAULT_SOURCE_PT) * value / 100f
            else -> value * 0.75f
        }
    }

    /** `<font size="5">`, la grandezza dei vecchi siti. */
    private fun fontTagSize(attrs: String): Float? =
        Regex("size\\s*=\\s*[\"']?([1-7])\\b").find(attrs)?.groupValues?.get(1)?.toIntOrNull()?.let { FONT_TAG_SIZES[it] }
}
