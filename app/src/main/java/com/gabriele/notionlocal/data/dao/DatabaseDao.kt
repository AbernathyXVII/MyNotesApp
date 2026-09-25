package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import kotlinx.coroutines.flow.Flow

/** L'immagine-icona della pagina di una riga: vedi `DatabaseDao.observeRowIcons`. */
data class RowIcon(val rowId: String, val iconImage: String)

/**
 * La copertina della pagina di una riga, con la sua inquadratura: vedi
 * `DatabaseDao.observeRowCovers`.
 */
data class RowCover(
    val rowId: String,
    val coverImage: String,
    val coverScale: Float,
    val coverOffsetX: Float,
    val coverOffsetY: Float,
    /** L'inquadratura sua della scheda, se l'utente ne ha scelta una: vedi `PageEntity.cardCoverScale`. */
    val cardCoverScale: Float? = null,
    val cardCoverOffsetX: Float? = null,
    val cardCoverOffsetY: Float? = null
)

/**
 * Un blocco in cima alla pagina di una riga, per l'anteprima del testo
 * nella galleria: vedi `DatabaseDao.observeRowPreviewBlocks`.
 */
data class RowPreviewBlock(
    val rowId: String,
    val type: BlockType,
    val textJson: String,
    val isChecked: Boolean,
    val indentLevel: Int,
    val numberStartsAt: Int?,
    val orderIndex: Int
)

/**
 * Un pezzo di testo che sta dentro la pagina di una riga — un blocco, la
 * cella di una tabella, il nome di una sottopagina — per la ricerca dentro
 * un database: vedi `DatabaseRepository.searchIndex`.
 */
data class RowPageText(val rowId: String, val text: String)

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

    /**
     * Stacca le righe di un database dalle loro pagine: serve a "Turn
     * into simple database", dopo aver cancellato quelle pagine. Il
     * collegamento si toglierebbe da solo (`SET_NULL`), ma detto qui non
     * dipende da come è configurato il database.
     */
    @Query("UPDATE database_rows SET linkedPageId = NULL WHERE pageId = :pageId")
    suspend fun unlinkRowPages(pageId: String)

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

    /**
     * Le copertine delle pagine delle righe, per la galleria. Come le
     * icone: seguite su tutte e due le tabelle, così una copertina
     * cambiata o inquadrata dentro la pagina si vede subito anche nelle
     * schede.
     */
    @Query(
        "SELECT database_rows.id AS rowId, pages.coverImage AS coverImage, " +
            "pages.coverScale AS coverScale, pages.coverOffsetX AS coverOffsetX, " +
            "pages.coverOffsetY AS coverOffsetY, pages.cardCoverScale AS cardCoverScale, " +
            "pages.cardCoverOffsetX AS cardCoverOffsetX, pages.cardCoverOffsetY AS cardCoverOffsetY " +
            "FROM database_rows JOIN pages ON pages.id = database_rows.linkedPageId " +
            "WHERE database_rows.pageId = :pageId AND pages.coverImage IS NOT NULL"
    )
    fun observeRowCovers(pageId: String): Flow<List<RowCover>>

    /**
     * I blocchi **di primo livello** delle pagine delle righe, nell'ordine
     * in cui stanno nella pagina: da qui la galleria prende le prime righe
     * di testo da mostrare nelle schede. Quelli dentro un toggle restano
     * fuori — sono nascosti anche nella pagina finché il toggle è chiuso.
     *
     * Torna tutti i blocchi e non solo i primi: "i primi N di ogni pagina"
     * in SQLite vorrebbe le funzioni finestra, che arrivano solo con
     * Android 11, sotto il minimo che l'app sostiene. Il taglio lo fa il
     * repository.
     */
    @Query(
        "SELECT database_rows.id AS rowId, blocks.type AS type, blocks.textJson AS textJson, " +
            "blocks.isChecked AS isChecked, blocks.indentLevel AS indentLevel, " +
            "blocks.numberStartsAt AS numberStartsAt, blocks.orderIndex AS orderIndex " +
            "FROM database_rows JOIN blocks ON blocks.pageId = database_rows.linkedPageId " +
            "WHERE database_rows.pageId = :pageId AND blocks.parentBlockId IS NULL " +
            "ORDER BY database_rows.id, blocks.orderIndex"
    )
    fun observeRowPreviewBlocks(pageId: String): Flow<List<RowPreviewBlock>>

    /** Le righe il cui nome contiene la parola cercata: anche quelle mai aperte come pagina. */
    @Query("SELECT * FROM database_rows WHERE title LIKE '%' || :query || '%'")
    suspend fun searchRows(query: String): List<DatabaseRowEntity>

    // --- Ricerca dentro un database (la lente accanto a Sort) ---

    /** Il testo dei blocchi delle pagine delle righe di un database, come JSON. */
    @Query(
        """
        SELECT database_rows.id AS rowId, blocks.textJson AS text FROM database_rows
        INNER JOIN blocks ON blocks.pageId = database_rows.linkedPageId
        WHERE database_rows.pageId = :databasePageId
        """
    )
    suspend fun getRowPageBlockTexts(databasePageId: String): List<RowPageText>

    /** Le celle delle tabelle dentro le pagine delle righe di un database. */
    @Query(
        """
        SELECT database_rows.id AS rowId, table_cells.text AS text FROM database_rows
        INNER JOIN blocks ON blocks.pageId = database_rows.linkedPageId
        INNER JOIN table_cells ON table_cells.blockId = blocks.id
        WHERE database_rows.pageId = :databasePageId
        """
    )
    suspend fun getRowPageTableTexts(databasePageId: String): List<RowPageText>

    /** I nomi delle sottopagine richiamate dentro le pagine delle righe: si leggono nella pagina come righe sue. */
    @Query(
        """
        SELECT database_rows.id AS rowId, pages.title AS text FROM database_rows
        INNER JOIN blocks ON blocks.pageId = database_rows.linkedPageId
        INNER JOIN pages ON pages.id = blocks.linkedPageId
        WHERE database_rows.pageId = :databasePageId AND pages.trashedAt IS NULL
        """
    )
    suspend fun getRowPageLinkTitles(databasePageId: String): List<RowPageText>

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
