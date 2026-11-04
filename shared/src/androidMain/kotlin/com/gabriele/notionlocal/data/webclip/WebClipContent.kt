package com.gabriele.notionlocal.data.webclip

import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.data.entity.RichTextSpan
import org.json.JSONArray
import org.json.JSONObject

/**
 * **"Text and Images"** (dal 20/10/2026, chiesto dall'utente: "prendere tutto
 * ciò che riguarda la pagina specifica ed incollare solo testo e immagini
 * rispettando l'ordine", come OneNote): l'articolo letto da
 * `assets/web_clipper_reader.js` (`noteReader.serialize()`), un blocco alla
 * volta, e come diventa righe di notE.
 */
data class ClipContent(val title: String, val site: String, val blocks: List<ClipPiece>)

sealed class ClipPiece {
    /** Un paragrafo; `caption` per la didascalia di un'immagine. */
    data class Paragraph(val spans: List<RichTextSpan>, val caption: Boolean = false) : ClipPiece()
    data class Heading(val level: Int, val spans: List<RichTextSpan>) : ClipPiece()
    data class ListItem(val ordered: Boolean, val depth: Int, val spans: List<RichTextSpan>) : ClipPiece()
    data class Quote(val text: String) : ClipPiece()
    data class Code(val text: String) : ClipPiece()
    data class Table(val rows: List<List<String>>) : ClipPiece()
    object Divider : ClipPiece()
    /** Un'immagine: il suo indirizzo, il testo alternativo, e quanto era larga rispetto all'articolo. */
    data class Image(val src: String, val alt: String, val width: Float) : ClipPiece()
}

/** Il JSON di `noteReader.serialize()` letto; null se non è quello. */
fun parseClipContent(json: String): ClipContent? = runCatching {
    val root = JSONObject(json)
    val blocks = root.optJSONArray("blocks") ?: JSONArray()
    val pieces = (0 until blocks.length()).mapNotNull { i -> blocks.optJSONObject(i)?.let(::pieceOf) }
    ClipContent(root.optString("title").trim(), root.optString("site").trim(), pieces)
}.getOrNull()

private fun pieceOf(block: JSONObject): ClipPiece? = when (block.optString("k")) {
    "p" -> spansOf(block).takeIf { it.isNotEmpty() }?.let { ClipPiece.Paragraph(it) }
    "cap" -> spansOf(block).takeIf { it.isNotEmpty() }?.let { ClipPiece.Paragraph(it, caption = true) }
    "h" -> spansOf(block).takeIf { it.isNotEmpty() }?.let { ClipPiece.Heading(block.optInt("l", 2).coerceIn(1, 6), it) }
    "ul", "ol" -> spansOf(block).takeIf { it.isNotEmpty() }?.let {
        ClipPiece.ListItem(block.optString("k") == "ol", block.optInt("d", 0).coerceIn(0, MAX_LIST_DEPTH), it)
    }
    "quote" -> block.optString("text").trim().takeIf { it.isNotEmpty() }?.let { ClipPiece.Quote(it) }
    "code" -> block.optString("text").trimEnd().takeIf { it.isNotBlank() }?.let { ClipPiece.Code(it) }
    "table" -> block.optJSONArray("rows")?.let { rows ->
        (0 until rows.length()).mapNotNull { r ->
            rows.optJSONArray(r)?.let { row -> (0 until row.length()).map { c -> row.optString(c).trim() } }
        }.takeIf { it.isNotEmpty() }?.let { ClipPiece.Table(it) }
    }
    "hr" -> ClipPiece.Divider
    "img" -> block.optString("src").takeIf { it.startsWith("http") }?.let {
        ClipPiece.Image(it, block.optString("alt").trim(), block.optDouble("w", 1.0).toFloat().coerceIn(0f, 1f))
    }
    else -> null
}

private fun spansOf(block: JSONObject): List<RichTextSpan> {
    val runs = block.optJSONArray("runs") ?: return emptyList()
    return (0 until runs.length()).mapNotNull { i ->
        val run = runs.optJSONObject(i) ?: return@mapNotNull null
        val text = run.optString("t").replace('\n', ' ')
        if (text.isEmpty()) return@mapNotNull null
        RichTextSpan(
            text = text,
            bold = run.optBoolean("b"),
            italic = run.optBoolean("i"),
            underline = run.optBoolean("u"),
            strikethrough = run.optBoolean("s"),
            link = run.optString("h").takeIf { it.startsWith("http") }
        )
    }
}

/**
 * **Le righe di notE** di un articolo: i paragrafi coi loro stili, i
 * titoletti in grassetto e più grandi (quelli da 20 punti in su diventano
 * anche voci dell'indice, vedi `isLargeLine`), gli elenchi puntati e
 * numerati col loro rientro, citazioni, codice, tabelle semplici, divisori, e
 * le immagini già scaricate (`images`: indirizzo → file nell'app; quelle che
 * non si sono scaricate restano fuori), larghe come nella pagina.
 */
fun ClipContent.toNewBlocks(images: Map<String, String>): List<NewBlock> = blocks.mapNotNull { piece ->
    when (piece) {
        is ClipPiece.Paragraph -> NewBlock(spans = piece.spans)
        is ClipPiece.Heading -> NewBlock(spans = piece.spans.map { it.copy(bold = true, size = headingSize(piece.level)) })
        is ClipPiece.ListItem -> NewBlock(
            type = if (piece.ordered) BlockType.NUMBERED_LIST_ITEM else BlockType.BULLET_LIST_ITEM,
            spans = piece.spans,
            indentLevel = piece.depth
        )
        is ClipPiece.Quote -> NewBlock(type = BlockType.QUOTE, spans = listOf(RichTextSpan(text = piece.text)))
        is ClipPiece.Code -> NewBlock(type = BlockType.CODE, spans = listOf(RichTextSpan(text = piece.text)))
        is ClipPiece.Table -> NewBlock(type = BlockType.TABLE, cells = piece.rows)
        ClipPiece.Divider -> NewBlock(type = BlockType.DIVIDER)
        is ClipPiece.Image -> images[piece.src]?.let { file ->
            NewBlock(
                type = BlockType.IMAGE,
                attrs = BlockAttrs(
                    file = file,
                    name = piece.alt.takeIf { it.isNotEmpty() },
                    url = piece.src,
                    // Come nella pagina (non meno di un quarto); quasi a tutta larghezza, a tutta larghezza.
                    width = piece.width.coerceAtLeast(MIN_IMAGE_WIDTH).takeIf { it < 0.95f }
                )
            )
        }
    }
}

/** Le immagini dell'articolo, nell'ordine, ognuna una volta. */
val ClipContent.imageSources: List<String>
    get() = blocks.filterIsInstance<ClipPiece.Image>().map { it.src }.distinct()

/** Quanto è grande un titoletto, in punti (il testo normale è 16). */
fun headingSize(level: Int): Int = when (level) {
    1, 2 -> 24
    3 -> 20
    else -> 18
}

private const val MAX_LIST_DEPTH = 5
private const val MIN_IMAGE_WIDTH = 0.25f
