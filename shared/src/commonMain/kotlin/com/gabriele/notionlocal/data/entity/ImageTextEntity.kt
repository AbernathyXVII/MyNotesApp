package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * **Il testo letto in una foto** (dal 03/11/2026): quello che c'è scritto
 * nell'immagine di un blocco, una foto o un documento scansionato, per
 * trovarla con la ricerca. Si legge una volta per file (`ImageTexts`), e il
 * file è la chiave: le copie di un blocco sincronizzato, che hanno lo stesso
 * file, si leggono una volta sola. [text] vuoto = letta, e niente scritto.
 * [readAt] 0 = letta a metà (un alfabeto il cui modello non era ancora
 * arrivato): il testo c'è già, e la si rilegge la volta dopo.
 */
@Entity(tableName = "image_texts")
data class ImageTextEntity(
    @PrimaryKey val file: String,
    val text: String,
    val readAt: Long = System.currentTimeMillis()
)
