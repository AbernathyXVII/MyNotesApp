package com.gabriele.notionlocal.data.repository

import androidx.room.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.dao.RowCover
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Una riga di testo dell'anteprima di una scheda della galleria: il tipo
 * del blocco serve a disegnarla come nella pagina (un titolo più
 * marcato, un pallino davanti a un elemento di elenco, la casella).
 *
 * `ordinal` è il numero di un elemento di elenco numerato, contato come
 * lo conta la pagina (`PageEditorViewModel.numberedListOrdinal`); zero
 * per tutti gli altri tipi.
 */
data class RowPreviewLine(
    val type: BlockType,
    val text: String,
    val isChecked: Boolean,
    val indentLevel: Int,
    val ordinal: Int
)

/** Lo stesso tetto ai rientri dell'editor: oltre, i numeri si contano come all'ultimo livello. */
private const val PREVIEW_MAX_INDENT = 10

/**
 * Il testo di un blocco come va mostrato nell'anteprima: semplice, ma
 * **con i pezzi coperti ancora coperti**. Uno spoiler nella pagina è un
 * testo che l'utente ha scelto di non far leggere a colpo d'occhio, e
 * l'anteprima di una scheda è proprio un colpo d'occhio: ogni lettera
 * coperta diventa un quadratino, gli spazi restano, così si vede quanto
 * è lungo senza leggerlo.
 */
private fun List<RichTextSpan>.previewText(): String = joinToString("") { span ->
    if (span.spoiler) {
        span.text.map { if (it.isWhitespace()) it else '▒' }.joinToString("")
    } else {
        span.text
    }
}

/** I blocchi che hanno un testo da mostrare nell'anteprima. */
private val PREVIEW_TYPES = setOf(
    BlockType.PARAGRAPH,
    BlockType.BULLET_LIST_ITEM,
    BlockType.NUMBERED_LIST_ITEM,
    BlockType.CHECKBOX,
    BlockType.TOGGLE
)

/**
 * Punto unico di accesso ai dati delle pagine-database (tabelle con
 * campi personalizzati, tipo le Notion database view).
 */
class DatabaseRepository(private val db: AppDatabase) {

    private val dao = db.databaseDao()

    private val json = Json { ignoreUnknownKeys = true }

    fun getColumns(pageId: String): Flow<List<DatabaseColumnEntity>> =
        dao.getColumnsForPage(pageId)

    suspend fun addColumn(column: DatabaseColumnEntity) = dao.insertColumn(column)

    suspend fun updateColumn(column: DatabaseColumnEntity) = dao.updateColumn(column)

    /** Più colonne insieme, in una transazione: chi osserva la tabella riceve un avviso solo. */
    suspend fun updateColumns(columns: List<DatabaseColumnEntity>) {
        if (columns.isEmpty()) return
        db.withTransaction { columns.forEach { dao.updateColumn(it) } }
    }

    suspend fun deleteColumn(column: DatabaseColumnEntity) = dao.deleteColumn(column)

    fun getRows(pageId: String): Flow<List<DatabaseRowEntity>> =
        dao.getRowsForPage(pageId)

    /** Riga → immagine dell'icona della sua pagina, per le righe che ne hanno una. */
    fun observeRowIcons(pageId: String): Flow<Map<String, String>> =
        dao.observeRowIcons(pageId).map { icons -> icons.associate { it.rowId to it.iconImage } }

    /** Riga → copertina della sua pagina, con l'inquadratura, per le righe che ne hanno una. */
    fun observeRowCovers(pageId: String): Flow<Map<String, RowCover>> =
        dao.observeRowCovers(pageId).map { covers -> covers.associateBy { it.rowId } }

    /**
     * Riga → le prime righe di testo della sua pagina, per l'anteprima
     * delle schede della galleria.
     *
     * Si prendono i blocchi che hanno del testo — paragrafi, titoli,
     * elenchi, caselle, toggle — e si saltano quelli che non ne hanno
     * (divisori, tabelle, collegamenti a pagine), fino a
     * `maxLinesPerRow`. Le righe vuote in testa si saltano anche loro:
     * una pagina che comincia con due a-capo avrebbe un'anteprima che
     * sembra vuota. Quelle in mezzo invece restano, perché separano i
     * paragrafi come nella pagina.
     *
     * Decodifica il testo solo dei blocchi che servono: gli altri si
     * scartano prima, e una pagina lunga non costa più di una corta.
     */
    fun observeRowContentPreviews(
        pageId: String,
        maxLinesPerRow: Int
    ): Flow<Map<String, List<RowPreviewLine>>> =
        dao.observeRowPreviewBlocks(pageId).map { blocks ->
            blocks.groupBy { it.rowId }.mapValues { (_, rowBlocks) ->
                val lines = mutableListOf<RowPreviewLine>()
                // I numeri degli elenchi si contano come nella pagina: un
                // contatore per livello, e i blocchi di altro tipo in
                // mezzo non interrompono il conteggio. Si conta anche
                // sulle righe che poi l'anteprima salta, altrimenti il
                // primo numero mostrato ripartirebbe da uno.
                val counters = mutableListOf<Int>()
                for (block in rowBlocks) {
                    if (lines.size >= maxLinesPerRow) break
                    if (block.type !in PREVIEW_TYPES) continue
                    val level = block.indentLevel.coerceIn(0, PREVIEW_MAX_INDENT)
                    val ordinal = if (block.type == BlockType.NUMBERED_LIST_ITEM) {
                        while (counters.size < level) counters.add(1)
                        if (counters.size > level) {
                            while (counters.size > level + 1) counters.removeAt(counters.size - 1)
                            counters[level] = block.numberStartsAt ?: (counters[level] + 1)
                        } else {
                            counters.add(block.numberStartsAt ?: 1)
                        }
                        counters[level]
                    } else {
                        0
                    }
                    val text = runCatching {
                        json.decodeFromString<List<RichTextSpan>>(block.textJson)
                    }.getOrNull()?.previewText().orEmpty()
                    if (lines.isEmpty() && text.isBlank()) continue
                    lines += RowPreviewLine(block.type, text, block.isChecked, level, ordinal)
                }
                // Le righe vuote in fondo non dicono niente e rubano
                // posto a una scheda che ha già poco spazio.
                lines.dropLastWhile { it.text.isBlank() }
            }.filterValues { it.isNotEmpty() }
        }

    suspend fun addRow(row: DatabaseRowEntity) = dao.insertRow(row)

    suspend fun updateRow(row: DatabaseRowEntity) = dao.updateRow(row)

    /**
     * Cambia il titolo di una riga, segnandola come modificata adesso.
     * Se la riga è già stata aperta come pagina, rinomina anche quella:
     * per l'utente è un oggetto solo, e vedere due nomi diversi a
     * seconda di dove guarda sarebbe incomprensibile.
     */
    suspend fun setRowTitle(rowId: String, title: String) {
        dao.setRowTitle(rowId, title, System.currentTimeMillis())
        val linkedPageId = dao.getRowById(rowId)?.linkedPageId ?: return
        val page = db.pageDao().getById(linkedPageId) ?: return
        db.pageDao().update(page.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRow(row: DatabaseRowEntity) = dao.deleteRow(row)

    fun getAllCells(pageId: String): Flow<List<DatabaseCellEntity>> =
        dao.getAllCellsForPage(pageId)

    /**
     * Salva il valore di una cella. Se la cella non esiste ancora per
     * quella combinazione riga/colonna, la crea; altrimenti aggiorna
     * quella esistente. Questo evita che il chiamante (la UI) debba
     * sapere se sta creando o modificando.
     */
    suspend fun setCellValue(rowId: String, columnId: String, value: String) {
        val existing = dao.getCell(rowId, columnId)
        if (existing != null) {
            dao.updateCell(existing.copy(value = value))
        } else {
            dao.insertCell(
                DatabaseCellEntity(rowId = rowId, columnId = columnId, value = value)
            )
        }
        // Modificare una cella è modificare la riga: è quello che
        // "Last edited time" deve rispecchiare.
        dao.touchRow(rowId, System.currentTimeMillis())
    }
}
