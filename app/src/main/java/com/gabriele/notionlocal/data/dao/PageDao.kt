package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageFont
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity)

    @Update
    suspend fun update(page: PageEntity)

    @Delete
    suspend fun delete(page: PageEntity)

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun getById(pageId: String): PageEntity?

    /**
     * La stessa pagina, ma **seguita nel tempo**: ogni volta che la riga
     * cambia arriva il valore nuovo.
     *
     * Serve perché la stessa pagina può stare aperta in due schermate
     * insieme — un database dentro una pagina e lo stesso database a
     * schermo intero — e ciascuna ha il suo ViewModel. Con una lettura
     * una tantum, quella rimasta indietro non solo mostrava il titolo
     * vecchio: alla prima modifica lo riscriveva nel database, e la
     * rinomina fatta nell'altra schermata spariva.
     */
    @Query("SELECT * FROM pages WHERE id = :pageId")
    fun observeById(pageId: String): Flow<PageEntity?>

    /**
     * Pagine di primo livello (radici dell'albero), ordinate manualmente.
     * Esclude le pagine create aprendo una riga di database come pagina
     * (isRowPage = 1): quelle non devono comparire come pagine radice
     * indipendenti nella Home.
     */
    @Query("SELECT * FROM pages WHERE parentId IS NULL AND isRowPage = 0 ORDER BY orderIndex ASC")
    fun getRootPages(): Flow<List<PageEntity>>

    /**
     * Sottopagine dirette di una pagina data (meccanismo legacy, non più
     * popolato dalla UI attuale ma mantenuto nello schema).
     */
    @Query("SELECT * FROM pages WHERE parentId = :parentId ORDER BY orderIndex ASC")
    fun getChildPages(parentId: String): Flow<List<PageEntity>>

    /**
     * Le pagine messe fra i preferiti, database compresi — qui un
     * database è una pagina con `isDatabase = 1`.
     *
     * **Comprese le pagine-riga**, al contrario di `getRootPages`. Là
     * l'esclusione serve a non riempire la Home di pagine che nessuno
     * ha creato apposta; qui invece nell'elenco ci finisce solo quello
     * che è stato scelto uno per uno, e una riga di database aperta
     * come pagina è una pagina come le altre.
     *
     * Quelle nel cestino no: sono state buttate, e un preferito che porta
     * a una pagina buttata sarebbe una trappola. Tornano fra i preferiti
     * da sole se vengono ripristinate, perché il segno resta.
     *
     * L'ordine lo sceglie l'elenco dei preferiti (data o titolo, nei due
     * versi), quindi qui non se ne impone nessuno.
     */
    @Query("SELECT * FROM pages WHERE isFavorite = 1 AND trashedAt IS NULL")
    fun observeFavorites(): Flow<List<PageEntity>>

    /**
     * Accende o spegne il preferito **senza riscrivere il resto della
     * riga**: la stessa pagina può stare aperta in due schermate, e
     * salvarla intera da qui rimetterebbe indietro un titolo o
     * un'impostazione cambiati nell'altra. Insieme si scrive quando è
     * successo, o si cancella togliendolo.
     */
    @Query("UPDATE pages SET isFavorite = :favorite, favoritedAt = :at WHERE id = :pageId")
    suspend fun setFavorite(pageId: String, favorite: Boolean, at: Long?)

    /** Le pagine nel cestino, dall'ultima buttata. */
    @Query("SELECT * FROM pages WHERE trashedAt IS NOT NULL ORDER BY trashedAt DESC")
    fun observeTrash(): Flow<List<PageEntity>>

    /** Le pagine nel cestino da prima di un certo momento: quelle da togliere per sempre. */
    @Query("SELECT id FROM pages WHERE trashedAt IS NOT NULL AND trashedAt < :before")
    suspend fun getTrashedBefore(before: Long): List<String>

    /** Tutte le pagine nel cestino. */
    @Query("SELECT id FROM pages WHERE trashedAt IS NOT NULL")
    suspend fun getAllTrashedIds(): List<String>

    /** Come `setFavorite`, e per la stessa ragione: una colonna sola per volta. */
    @Query("UPDATE pages SET isLocked = :locked WHERE id = :pageId")
    suspend fun setLocked(pageId: String, locked: Boolean)

    @Query("UPDATE pages SET isViewLocked = :locked WHERE id = :pageId")
    suspend fun setViewLocked(pageId: String, locked: Boolean)

    /** Il font del testo della pagina, o null per quello di sistema. Una colonna sola, come sopra. */
    @Query("UPDATE pages SET pageFont = :font WHERE id = :pageId")
    suspend fun setPageFont(pageId: String, font: PageFont?)

    /** Il corpo del testo della pagina, o null per quello di partenza (16). */
    @Query("UPDATE pages SET pageFontSize = :size WHERE id = :pageId")
    suspend fun setPageFontSize(pageId: String, size: Int?)

    @Query("UPDATE pages SET trashedAt = :at WHERE id = :pageId")
    suspend fun setTrashedAt(pageId: String, at: Long?)

    /** Solo l'immagine dell'icona, per la stessa ragione di `setFavorite`. */
    @Query("UPDATE pages SET iconImage = :fileName WHERE id = :pageId")
    suspend fun setIconImage(pageId: String, fileName: String?)

    /**
     * Le pagine dentro cui se ne può spostare un'altra.
     *
     * Niente database: il loro contenuto sono righe, non blocchi, e un
     * collegamento a pagina lì dentro non avrebbe dove stare. Niente
     * pagine-riga per lo stesso motivo pratico — sono il dettaglio di
     * una riga, non un contenitore — e niente pagine nel cestino, che
     * sarebbe spostare una cosa in un posto che non si vede.
     */
    @Query(
        "SELECT * FROM pages WHERE isDatabase = 0 AND isRowPage = 0 " +
            "AND trashedAt IS NULL ORDER BY title COLLATE NOCASE ASC"
    )
    suspend fun getMoveDestinations(): List<PageEntity>

    /**
     * Ricerca full-text sul titolo delle pagine. Per la ricerca dentro
     * il contenuto dei blocchi vedi BlockDao.searchInBlocks — le due
     * vengono unite nel Repository.
     */
    @Query("SELECT * FROM pages WHERE title LIKE '%' || :query || '%'")
    suspend fun searchByTitle(query: String): List<PageEntity>

    /** Tutte le pagine fuori dal cestino, tranne quella principale, in ordine di titolo. */
    @Query(
        "SELECT * FROM pages WHERE trashedAt IS NULL AND id != 'root' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    suspend fun getAllLivePages(): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<PageEntity>
}
