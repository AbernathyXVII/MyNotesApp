package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una parola del dizionario dell'utente (dal 08/10/2026): quella che ha
 * aggiunto toccandola nella pagina, "Add to dictionary". Vale per tutte le
 * lingue del correttore, scritta com'è stata scritta — vedi `SpellChecker`
 * per come contano le maiuscole.
 *
 * Sta nel database e non nelle impostazioni del telefono: è contenuto
 * dell'utente, e va dove va il resto delle note.
 */
@Entity(tableName = "personal_words")
data class PersonalWordEntity(
    @PrimaryKey val word: String,
    val addedAt: Long = System.currentTimeMillis()
)
