package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gabriele.notionlocal.data.entity.InkStrokeEntity

/** I disegni a mano delle pagine: vedi `InkStrokeEntity`. */
@Dao
interface InkDao {

    /** Nell'ordine in cui sono stati disegnati: quello dopo sta sopra. */
    @Query("SELECT * FROM ink_strokes WHERE pageId = :pageId ORDER BY createdAt")
    suspend fun getForPageOnce(pageId: String): List<InkStrokeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(strokes: List<InkStrokeEntity>)

    @Query("DELETE FROM ink_strokes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    /**
     * Una modifica sola, tutta o niente: una passata di gomma toglie dei
     * tratti e ne rimette i pezzi rimasti, e a metà non deve restare.
     */
    @Transaction
    suspend fun replace(removedIds: List<String>, added: List<InkStrokeEntity>) {
        if (removedIds.isNotEmpty()) deleteByIds(removedIds)
        if (added.isNotEmpty()) insertAll(added)
    }
}
