package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gabriele.notionlocal.data.entity.PersonalWordEntity
import kotlinx.coroutines.flow.Flow

/** Il dizionario dell'utente: vedi `PersonalWordEntity`. */
@Dao
interface PersonalWordDao {

    @Query("SELECT * FROM personal_words ORDER BY word COLLATE NOCASE")
    fun observeAll(): Flow<List<PersonalWordEntity>>

    /** Una parola già aggiunta resta com'era. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: PersonalWordEntity)

    @Query("DELETE FROM personal_words WHERE word = :word")
    suspend fun delete(word: String)
}
