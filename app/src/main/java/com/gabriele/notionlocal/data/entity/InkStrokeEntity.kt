package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Con cosa è fatto un tratto: cambia come si disegna (vedi `InkLayer`). */
enum class InkTool {
    /** Penna: piena, più larga quanto più si preme con la S Pen. */
    PEN,

    /** Matita: come la penna ma più sottile e un po' trasparente. */
    PENCIL,

    /** Pennarello: pieno, sempre della stessa larghezza. */
    MARKER,

    /** Evidenziatore: largo, trasparente, a punta piatta; sta sotto gli altri tratti. */
    HIGHLIGHTER
}

/**
 * **Un tratto disegnato a mano su una pagina** (dal 25/09/2026, versione 33
 * del database): la matita nei tre puntini della pagina.
 *
 * **Non sta a una distanza fissa dalla cima della pagina**, ma attaccato a
 * quello su cui lo si è disegnato (`anchor`): il gruppo di righe fra due
 * divisori, un database, un toggle, o la testata della pagina. Così, se
 * sopra si scrivono altre righe, il disegno scende con quello che
 * accompagna invece di finire sopra a un'altra cosa. Le coordinate dei
 * punti sono in dp, contate dall'angolo in alto a sinistra di quello a
 * cui è attaccato (vedi `data/ink/InkGeometry.kt`).
 *
 * `anchor` è un elenco di id di blocchi separati da virgole — tutti quelli
 * del gruppo di righe su cui si è disegnato, nell'ordine — oppure
 * `HEADER_ANCHOR`. Il disegno segue il primo che c'è ancora: cancellata la
 * prima riga del gruppo, resta attaccato al gruppo. Se non ce n'è più
 * nessuno (cancellato il database che si era cerchiato) non si vede, ma
 * resta: se la cancellazione si annulla, torna con lui.
 *
 * `color` è un colore ARGB, oppure `AUTO_COLOR`: il colore del testo, che
 * cambia col tema (nero sul chiaro, bianco sullo scuro).
 */
@Entity(
    tableName = "ink_strokes",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pageId")]
)
data class InkStrokeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pageId: String,
    val anchor: String,
    val tool: InkTool,
    val color: Long,
    /** La larghezza scelta, in dp (la penna la allarga e la stringe con la pressione). */
    val width: Float,
    /** I punti, vedi `encodeInkPoints`. */
    val points: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Attaccato alla testata della pagina (icona e titolo), che sta sempre in cima. */
        const val HEADER_ANCHOR = "__header__"

        /** Il colore del testo, che segue il tema. */
        const val AUTO_COLOR = 0L
    }
}
