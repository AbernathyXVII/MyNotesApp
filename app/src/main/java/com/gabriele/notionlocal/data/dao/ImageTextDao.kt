package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gabriele.notionlocal.data.entity.ImageTextEntity

/** Il testo letto nelle foto (dal 03/11/2026): vedi `ImageTextEntity`. */
@Dao
interface ImageTextDao {

    @Upsert
    suspend fun upsert(entry: ImageTextEntity)

    @Query("SELECT * FROM image_texts WHERE file = :file")
    suspend fun get(file: String): ImageTextEntity?

    /** I file letti per intero: gli altri (e quelli letti a metà, `readAt` 0) sono da leggere. */
    @Query("SELECT file FROM image_texts WHERE readAt > 0")
    suspend fun readFiles(): List<String>

    /** Le foto che contengono la parola cercata. */
    @Query("SELECT * FROM image_texts WHERE text LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<ImageTextEntity>

    /** Tutte da rileggere, col testo che hanno già intanto (vedi `ImageTexts.init`). */
    @Query("UPDATE image_texts SET readAt = 0")
    suspend fun markAllToReread()

    @Query("DELETE FROM image_texts WHERE file IN (:files)")
    suspend fun delete(files: List<String>)
}
