package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.gabriele.notionlocal.data.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow

/** Le automazioni dei database (dal 03/11/2026): vedi `AutomationEntity`. */
@Dao
interface AutomationDao {

    @Query("SELECT * FROM automations WHERE databaseId = :databaseId ORDER BY orderIndex, createdAt")
    fun observeForDatabase(databaseId: String): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE databaseId = :databaseId AND enabled = 1 ORDER BY orderIndex, createdAt")
    suspend fun enabledFor(databaseId: String): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE enabled = 1")
    suspend fun allEnabled(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: String): AutomationEntity?

    @Upsert
    suspend fun upsert(automation: AutomationEntity)

    @Delete
    suspend fun delete(automation: AutomationEntity)

    @Query("UPDATE automations SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE automations SET lastRunAt = :at WHERE id = :id")
    suspend fun setLastRun(id: String, at: Long)
}
