package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Il contenuto di una singola cella di un blocco TABLE (tabella semplice
 * dentro una pagina — diversa dalle Database view, che hanno colonne
 * tipizzate). Solo testo libero, nessun tipo di colonna: la tabella
 * semplice è deliberatamente più basilare di un database.
 *
 * onDelete = CASCADE: cancellare il blocco TABLE cancella tutte le sue
 * celle.
 */
@Entity(
    tableName = "table_cells",
    foreignKeys = [
        ForeignKey(
            entity = BlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("blockId")]
)
data class TableCellEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val blockId: String,
    val rowIndex: Int,
    val colIndex: Int,
    var text: String = ""
)
