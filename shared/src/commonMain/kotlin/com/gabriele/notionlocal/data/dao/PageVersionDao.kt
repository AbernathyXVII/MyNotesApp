package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gabriele.notionlocal.data.entity.PageVersionEntity
import com.gabriele.notionlocal.data.entity.PageVersionInfo
import com.gabriele.notionlocal.data.entity.VersionStamp
import kotlinx.coroutines.flow.Flow

/** Le versioni delle pagine: vedi `PageVersionEntity`. */
@Dao
interface PageVersionDao {

    @Insert
    suspend fun insert(version: PageVersionEntity)

    /** L'elenco di una pagina, dalla più recente, senza il contenuto: si legge solo aprendone una. */
    @Query("SELECT id, pageId, at, reason, title, lines, words FROM page_versions WHERE pageId = :pageId ORDER BY at DESC")
    fun observeForPage(pageId: String): Flow<List<PageVersionInfo>>

    @Query("SELECT id, at FROM page_versions WHERE pageId = :pageId")
    suspend fun stampsFor(pageId: String): List<VersionStamp>

    /** L'impronta dell'ultima: se la pagina è uguale, una versione nuova non serve. */
    @Query("SELECT hash FROM page_versions WHERE pageId = :pageId ORDER BY at DESC LIMIT 1")
    suspend fun latestHash(pageId: String): String?

    @Query("SELECT content FROM page_versions WHERE id = :id")
    suspend fun contentOf(id: String): ByteArray?

    @Query("DELETE FROM page_versions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
