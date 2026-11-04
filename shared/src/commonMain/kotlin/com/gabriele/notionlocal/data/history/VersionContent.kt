package com.gabriele.notionlocal.data.history

import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.plainText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * **Il contenuto di una versione di una pagina** (dal 02/11/2026, vedi
 * `PageVersionEntity`): tutto quello che serve per rimetterla com'era — il
 * titolo, i blocchi con le loro impostazioni, il testo delle tabelle e i
 * disegni. Non ci sono l'icona e la copertina (cambiandole, il file di
 * prima se ne va) né quello che c'è dentro i database e le sottopagine:
 * sono pagine a sé, qui c'è solo il blocco che le mostra.
 *
 * Le classi sono copie delle entità e non le entità stesse: così una
 * versione salvata oggi si legge anche quando le entità cambiano (un campo
 * nuovo prende il suo valore di sempre). `VersionContentTest` controlla
 * che nessun campo delle entità resti fuori.
 */
@Serializable
data class VersionContent(
    val title: String,
    val blocks: List<VersionBlock>,
    val cells: List<VersionCell> = emptyList(),
    val ink: List<VersionStroke> = emptyList()
) {
    /** Le righe della pagina: i blocchi, anche quelli dentro i toggle. */
    val lines: Int get() = blocks.size

    /** Le parole scritte nei blocchi e nelle tabelle. */
    val words: Int
        get() = blocks.sumOf { countWords(plainTextOf(it)) } + cells.sumOf { countWords(it.text) }

    companion object {
        /**
         * La pagina di adesso. Sempre nello stesso ordine, qualunque sia
         * quello in cui il database le dà: due pagine uguali devono avere la
         * stessa impronta.
         */
        fun of(title: String, blocks: List<BlockEntity>, cells: List<TableCellEntity>, ink: List<InkStrokeEntity>) = VersionContent(
            title = title,
            blocks = blocks.map { it.toVersion() }.sortedWith(compareBy({ it.parentBlockId.orEmpty() }, { it.orderIndex }, { it.id })),
            cells = cells.map { it.toVersion() }.sortedWith(compareBy({ it.blockId }, { it.rowIndex }, { it.colIndex }, { it.id })),
            ink = ink.map { it.toVersion() }.sortedWith(compareBy({ it.createdAt }, { it.id }))
        )
    }
}

@Serializable
data class VersionBlock(
    val id: String,
    val parentBlockId: String? = null,
    val type: BlockType = BlockType.PARAGRAPH,
    val textJson: String = "[]",
    val isChecked: Boolean = false,
    val isExpanded: Boolean = true,
    val tableRows: Int = 2,
    val tableCols: Int = 2,
    val linkedPageId: String? = null,
    val numberResetHere: Boolean = false,
    val numberStartsAt: Int? = null,
    val indentLevel: Int = 0,
    val orderIndex: Int = 0,
    val notifyAt: Long? = null,
    val attrsJson: String = "{}"
)

@Serializable
data class VersionCell(
    val id: String,
    val blockId: String,
    val rowIndex: Int,
    val colIndex: Int,
    val text: String = ""
)

@Serializable
data class VersionStroke(
    val id: String,
    val anchor: String,
    val tool: InkTool,
    val color: Long,
    val width: Float,
    val points: String,
    val createdAt: Long
)

fun BlockEntity.toVersion() = VersionBlock(
    id = id, parentBlockId = parentBlockId, type = type, textJson = textJson, isChecked = isChecked,
    isExpanded = isExpanded, tableRows = tableRows, tableCols = tableCols, linkedPageId = linkedPageId,
    numberResetHere = numberResetHere, numberStartsAt = numberStartsAt, indentLevel = indentLevel,
    orderIndex = orderIndex, notifyAt = notifyAt, attrsJson = attrsJson
)

fun VersionBlock.toEntity(pageId: String) = BlockEntity(
    id = id, pageId = pageId, parentBlockId = parentBlockId, type = type, textJson = textJson, isChecked = isChecked,
    isExpanded = isExpanded, tableRows = tableRows, tableCols = tableCols, linkedPageId = linkedPageId,
    numberResetHere = numberResetHere, numberStartsAt = numberStartsAt, indentLevel = indentLevel,
    orderIndex = orderIndex, notifyAt = notifyAt, attrsJson = attrsJson
)

fun TableCellEntity.toVersion() = VersionCell(id = id, blockId = blockId, rowIndex = rowIndex, colIndex = colIndex, text = text)

fun VersionCell.toEntity() = TableCellEntity(id = id, blockId = blockId, rowIndex = rowIndex, colIndex = colIndex, text = text)

fun InkStrokeEntity.toVersion() = VersionStroke(
    id = id, anchor = anchor, tool = tool, color = color, width = width, points = points, createdAt = createdAt
)

fun VersionStroke.toEntity(pageId: String) = InkStrokeEntity(
    id = id, pageId = pageId, anchor = anchor, tool = tool, color = color, width = width, points = points, createdAt = createdAt
)

private val spansJson = Json { ignoreUnknownKeys = true }

/** Il testo semplice di un blocco della versione. */
fun plainTextOf(block: VersionBlock): String =
    runCatching { spansJson.decodeFromString<List<RichTextSpan>>(block.textJson).plainText() }.getOrDefault("")

private fun countWords(text: String): Int = text.split(Regex("\\s+")).count { it.isNotEmpty() }

/** Da contenuto a byte e ritorno: JSON compresso. */
object VersionCodec {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(content: VersionContent): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(json.encodeToString(content).toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): VersionContent =
        json.decodeFromString(GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }.toString(Charsets.UTF_8))

    /**
     * L'impronta del contenuto, per non salvare due volte la stessa pagina.
     * Un toggle aperto o chiuso non conta: non è una modifica.
     */
    fun hash(content: VersionContent): String {
        val normalized = content.copy(blocks = content.blocks.map { if (it.isExpanded) it else it.copy(isExpanded = true) })
        val digest = MessageDigest.getInstance("SHA-256").digest(json.encodeToString(normalized).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
