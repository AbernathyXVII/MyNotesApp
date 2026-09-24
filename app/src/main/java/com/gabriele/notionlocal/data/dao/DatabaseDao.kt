package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import kotlinx.coroutines.flow.Flow

/** L'immagine-icona della pagina di una riga: vedi `DatabaseDao.observeRowIcons`. */
data class RowIcon(val rowId: String, val iconImage: String)

@Dao
interface DatabaseDao {

    // --- Colonne ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumn(column: DatabaseColumnEntity)

    @Update
    suspend fun updateColumn(column: DatabaseColumnEntity)

    @Delete
    suspend fun deleteColumn(column: DatabaseColumnEntity)

    @Query("SELECT * FROM database_columns WHERE pageId = :pageId ORDER BY orderIndex ASC")
    fun getColumnsForPage(pageId: String): Flow<List<DatabaseColumnEntity>>

    // --- Righe ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRow(row: DatabaseRowEntity)

    @Update
    suspend fun updateRow(row: DatabaseRowEntity)

    @Delete
    suspend fun deleteRow(row: DatabaseRowEntity)

    @Query("SELECT * FROM database_rows WHERE pageId = :pageId ORDER BY orderIndex ASC")
    fun getRowsForPage(pageId: String): Flow<List<DatabaseRowEntity>>

    /**
     * Cambia il titolo senza riscrivere il resto della riga. Salvare
     * l'intera entità userebbe la copia che il chiamante ha in mano, che
     * può essere più vecchia: si rischierebbe di azzerare `linkedPageId`
     * scrivendo mentre la pagina collegata viene creata, e la riga
     * perderebbe la sua pagina.
     */
    @Query("UPDATE database_rows SET title = :title, updatedAt = :timestamp WHERE id = :rowId")
    suspend fun setRowTitle(rowId: String, title: String, timestamp: Long)

    @Query("SELECT * FROM database_rows WHERE id = :rowId LIMIT 1")
    suspend fun getRowById(rowId: String): DatabaseRowEntity?

    /**
     * Rinomina la riga collegata a una pagina. Il titolo di una
     * pagina-riga e il nome della riga nella tabella sono la stessa
     * cosa, come su Notion: rinominare da un lato deve cambiare
     * l'altro, altrimenti si vedono due nomi diversi per lo stesso
     * oggetto.
     */
    @Query("UPDATE database_rows SET title = :title, updatedAt = :timestamp WHERE linkedPageId = :pageId")
    suspend fun setRowTitleByLinkedPage(pageId: String, title: String, timestamp: Long)

    /**
     * Fa posto fra le righe: sposta avanti di `by` tutte quelle da
     * `fromIndex` in poi. Serve a mettere una riga duplicata subito sotto
     * l'originale invece che in fondo.
     */
    @Query(
        "UPDATE database_rows SET orderIndex = orderIndex + :by " +
            "WHERE pageId = :pageId AND orderIndex >= :fromIndex"
    )
    suspend fun shiftRowOrder(pageId: String, fromIndex: Int, by: Int)

    /** Le celle di una riga, una volta sola: servono a duplicarla. */
    @Query("SELECT * FROM database_cells WHERE rowId = :rowId")
    suspend fun getCellsForRowOnce(rowId: String): List<DatabaseCellEntity>

    /** Segna la riga come modificata adesso (per "Last edited time"). */
    @Query("UPDATE database_rows SET updatedAt = :timestamp WHERE id = :rowId")
    suspend fun touchRow(rowId: String, timestamp: Long)

    // --- Celle ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: DatabaseCellEntity)

    @Update
    suspend fun updateCell(cell: DatabaseCellEntity)

    @Query("SELECT * FROM database_cells WHERE rowId = :rowId")
    fun getCellsForRow(rowId: String): Flow<List<DatabaseCellEntity>>

    @Query("SELECT * FROM database_cells WHERE rowId = :rowId AND columnId = :columnId LIMIT 1")
    suspend fun getCell(rowId: String, columnId: String): DatabaseCellEntity?

    /**
     * Tutte le celle di tutte le righe di una pagina-database in un colpo
     * solo. Molto più efficiente che fare una query per riga quando si
     * carica l'intera tabella (evita N+1 query).
     */
    @Query(
        """
        SELECT database_cells.* FROM database_cells
        INNER JOIN database_rows ON database_cells.rowId = database_rows.id
        WHERE database_rows.pageId = :pageId
        """
    )
    fun getAllCellsForPage(pageId: String): Flow<List<DatabaseCellEntity>>

    // Le stesse tre letture, ma **una volta sola** invece che seguite
    // nel tempo: servono a duplicare un database, dove il contenuto va
    // letto e ricopiato, non osservato.

    @Query("SELECT * FROM database_columns WHERE pageId = :pageId ORDER BY orderIndex ASC")
    suspend fun getColumnsForPageOnce(pageId: String): List<DatabaseColumnEntity>

    @Query("SELECT * FROM database_rows WHERE pageId = :pageId ORDER BY orderIndex ASC")
    suspend fun getRowsForPageOnce(pageId: String): List<DatabaseRowEntity>

    @Query("SELECT COUNT(*) FROM database_rows WHERE pageId = :pageId")
    suspend fun countRows(pageId: String): Int

    /**
     * Le icone delle pagine delle righe: per ogni riga la cui pagina ha
     * un'immagine, quale. Seguita nel tempo e su **tutte e due le
     * tabelle**: l'icona cambiata dentro la pagina si vede subito anche
     * nel database, senza riaprirlo.
     */
    @Query(
        "SELECT database_rows.id AS rowId, pages.iconImage AS iconImage " +
            "FROM database_rows JOIN pages ON pages.id = database_rows.linkedPageId " +
            "WHERE database_rows.pageId = :pageId AND pages.iconImage IS NOT NULL"
    )
    fun observeRowIcons(pageId: String): Flow<List<RowIcon>>

    /** Le righe il cui nome contiene la parola cercata: anche quelle mai aperte come pagina. */
    @Query("SELECT * FROM database_rows WHERE title LIKE '%' || :query || '%'")
    suspend fun searchRows(query: String): List<DatabaseRowEntity>

    /** Le righe che aprono una certa pagina: di solito una, a volte nessuna. */
    @Query("SELECT * FROM database_rows WHERE linkedPageId = :pageId")
    suspend fun getRowsLinkingTo(pageId: String): List<DatabaseRowEntity>

    @Query(
        """
        SELECT database_cells.* FROM database_cells
        INNER JOIN database_rows ON database_cells.rowId = database_rows.id
        WHERE database_rows.pageId = :pageId
        """
    )
    suspend fun getAllCellsForPageOnce(pageId: String): List<DatabaseCellEntity>
}
