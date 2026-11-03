package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gabriele.notionlocal.data.entity.NotebookEntity
import com.gabriele.notionlocal.data.entity.NotebookSectionEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import kotlinx.coroutines.flow.Flow

/** Una sezione e quante pagine ha: vedi `NotebookDao.observePageCounts`. */
data class SectionPageCount(val sectionId: String, val pages: Int)

/** I quaderni, le loro sezioni e le pagine delle sezioni: vedi `NotebookEntity`. */
@Dao
interface NotebookDao {

    @Query("SELECT * FROM notebooks ORDER BY orderIndex")
    fun observeNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks ORDER BY orderIndex")
    suspend fun getNotebooksOnce(): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebook(id: String): NotebookEntity?

    @Insert
    suspend fun insertNotebook(notebook: NotebookEntity)

    @Update
    suspend fun updateNotebook(notebook: NotebookEntity)

    @Delete
    suspend fun deleteNotebook(notebook: NotebookEntity)

    /** Tutte le sezioni e i gruppi di tutti i quaderni: l'albero si costruisce in memoria. */
    @Query("SELECT * FROM notebook_sections ORDER BY orderIndex")
    fun observeSections(): Flow<List<NotebookSectionEntity>>

    @Query("SELECT * FROM notebook_sections WHERE notebookId = :notebookId ORDER BY orderIndex")
    suspend fun getSectionsOfNotebook(notebookId: String): List<NotebookSectionEntity>

    @Query("SELECT * FROM notebook_sections WHERE id = :id")
    suspend fun getSection(id: String): NotebookSectionEntity?

    @Query("SELECT * FROM notebook_sections WHERE id = :id")
    fun observeSection(id: String): Flow<NotebookSectionEntity?>

    @Insert
    suspend fun insertSection(section: NotebookSectionEntity)

    @Update
    suspend fun updateSection(section: NotebookSectionEntity)

    @Delete
    suspend fun deleteSections(sections: List<NotebookSectionEntity>)

    @Query("SELECT COUNT(*) FROM notebook_sections WHERE isProtected = 1")
    suspend fun countProtectedSections(): Int

    /** Le pagine di una sezione, nel loro ordine. */
    @Query("SELECT * FROM pages WHERE notebookSectionId = :sectionId ORDER BY orderIndex, createdAt")
    fun observePagesInSection(sectionId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE notebookSectionId = :sectionId ORDER BY orderIndex, createdAt")
    suspend fun getPagesInSection(sectionId: String): List<PageEntity>

    /** Quante pagine ha ogni sezione, per la pagina di un quaderno. */
    @Query(
        "SELECT notebookSectionId AS sectionId, COUNT(*) AS pages FROM pages " +
            "WHERE notebookSectionId IS NOT NULL GROUP BY notebookSectionId"
    )
    fun observePageCounts(): Flow<List<SectionPageCount>>

    @Query("SELECT id FROM pages WHERE notebookSectionId IN (:sectionIds)")
    suspend fun getPageIdsInSections(sectionIds: List<String>): List<String>

    @Query("UPDATE pages SET orderIndex = :orderIndex WHERE id = :pageId")
    suspend fun setPageOrder(pageId: String, orderIndex: Int)
}
