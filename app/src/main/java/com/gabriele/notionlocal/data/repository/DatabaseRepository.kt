package com.gabriele.notionlocal.data.repository

import androidx.room.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Punto unico di accesso ai dati delle pagine-database (tabelle con
 * campi personalizzati, tipo le Notion database view).
 */
class DatabaseRepository(private val db: AppDatabase) {

    private val dao = db.databaseDao()

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
