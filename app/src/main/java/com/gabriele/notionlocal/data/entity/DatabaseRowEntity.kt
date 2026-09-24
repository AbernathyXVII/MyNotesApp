package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Una riga di un database. Ogni riga ha un titolo (come il "Name" di
 * Notion, sempre presente) più valori per le colonne custom, salvati
 * in DatabaseCellEntity (una cella per colonna per riga).
 *
 * linkedPageId valorizzato quando la riga è stata aperta come pagina
 * vera e propria (vedi DatabaseViewModel.openRow) — la pagina viene
 * creata al volo la prima volta e poi riusata. onDelete = SET_NULL: se
 * la pagina collegata viene cancellata, la riga resta ma perde il
 * collegamento invece di sparire.
 */
@Entity(
    tableName = "database_rows",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedPageId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("pageId"), Index("linkedPageId")]
)
data class DatabaseRowEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pageId: String,
    var title: String = "",
    var linkedPageId: String? = null,
    var orderIndex: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    // Momento dell'ultima modifica al titolo o a una qualsiasi cella
    // della riga. Ammette null perché le righe già esistenti quando la
    // colonna è stata introdotta (migrazione 6→7) non hanno una storia
    // da cui ricavarlo: per quelle si mostra la data di creazione.
    var updatedAt: Long? = null
)

/**
 * Il valore di una singola cella (riga x colonna). value è sempre una
 * stringa: per NUMBER/CHECKBOX/DATE la convertiamo a/da tipo nella UI,
 * così lo schema resta semplice e non serve una tabella per tipo.
 */
@Entity(
    tableName = "database_cells",
    foreignKeys = [
        ForeignKey(
            entity = DatabaseRowEntity::class,
            parentColumns = ["id"],
            childColumns = ["rowId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DatabaseColumnEntity::class,
            parentColumns = ["id"],
            childColumns = ["columnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rowId"), Index("columnId")]
)
data class DatabaseCellEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rowId: String,
    val columnId: String,
    var value: String = ""
)
