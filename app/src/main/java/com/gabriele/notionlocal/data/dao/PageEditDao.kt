package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gabriele.notionlocal.data.entity.PageEditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageEditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(edit: PageEditEntity)

    /**
     * La cronologia di una pagina, dalla più recente.
     *
     * Il tetto di duecento voci è nella query e non in una pulizia a
     * parte: una pagina scritta per mesi accumula migliaia di
     * correzioni, e caricarle tutte per mostrarne una schermata
     * sarebbe sprecato. Le più vecchie restano nel database — non si
     * cancella niente — semplicemente non si leggono.
     */
    @Query("SELECT * FROM page_edits WHERE pageId = :pageId ORDER BY at DESC LIMIT 200")
    fun observeForPage(pageId: String): Flow<List<PageEditEntity>>

    @Query("DELETE FROM page_edits WHERE pageId = :pageId")
    suspend fun clearForPage(pageId: String)
}
