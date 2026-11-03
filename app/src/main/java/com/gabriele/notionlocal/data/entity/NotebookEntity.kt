package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * **Un quaderno**, come quelli di OneNote (dal 26/09/2026). I quaderni sono
 * un mondo **a parte** rispetto al menu principale: niente collegamenti fra
 * i due, niente ricerca in comune, ognuno sta per conto suo — l'ha chiesto
 * così l'utente.
 *
 * Dentro un quaderno ci sono sezioni e gruppi di sezioni
 * (`NotebookSectionEntity`), e dentro le sezioni le pagine: pagine vere
 * (`PageEntity`), con lo stesso editor, che però sanno di stare in una
 * sezione (`PageEntity.notebookSectionId`) e non compaiono mai nel menu
 * principale.
 */
@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    /** Il colore del quaderno disegnato accanto al nome, in ARGB. */
    val color: Int = NotebookColors.palette.first(),
    /**
     * Un'immagine scelta dall'utente al posto del quaderno disegnato: il
     * **nome del file** nella cartella delle immagini, come l'icona delle
     * pagine (`PageEntity.iconImage`).
     */
    val iconImage: String? = null,
    val orderIndex: Int = 0,
    /** Aperto nella barra laterale: si ricorda, come in OneNote. */
    val isExpanded: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * **Una sezione o un gruppo di sezioni** di un quaderno. Una tabella sola
 * per tutti e due perché formano un unico albero: un gruppo può contenere
 * sezioni e altri gruppi (in OneNote "Languages" → "Deutsch" → "General"),
 * e l'ordine fra fratelli è uno solo, gruppi e sezioni mescolati.
 *
 * - `parentId` null: sta direttamente nel quaderno; altrimenti l'id del
 *   gruppo che la contiene.
 * - Le **sezioni** hanno un colore (la linguetta accanto al nome) e
 *   contengono le pagine; si possono proteggere con la password.
 * - I **gruppi** si aprono e si chiudono come i toggle, e basta.
 */
@Entity(
    tableName = "notebook_sections",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notebookId")]
)
data class NotebookSectionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val notebookId: String,
    val parentId: String? = null,
    val isGroup: Boolean = false,
    val title: String,
    val color: Int = NotebookColors.palette.first(),
    val orderIndex: Int = 0,
    val isExpanded: Boolean = true,
    /** Protetta con la password dell'app (vedi `AppLock`): solo le sezioni, come in OneNote. */
    val isProtected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * I colori di quaderni e sezioni, quelli di OneNote. Una sezione nuova
 * prende il colore dopo quello dell'ultima, così due sezioni vicine non si
 * confondono.
 */
object NotebookColors {
    val palette: List<Int> = listOf(
        0xFF1582E8.toInt(), // blu
        0xFFE5534B.toInt(), // rosso
        0xFF10A04A.toInt(), // verde
        0xFFF2B61B.toInt(), // giallo
        0xFFB07CE8.toInt(), // viola
        0xFFE85D8C.toInt(), // rosa
        0xFFF46A1F.toInt(), // arancione
        0xFF1CC7B6.toInt(), // verde acqua
        0xFFC4A07A.toInt(), // sabbia
        0xFF3AB0FF.toInt(), // azzurro
        0xFF8BC34A.toInt(), // verde chiaro
        0xFF9E9E9E.toInt() // grigio
    )

    /** Il blu di OneNote, il primo della tavolozza: così nella scelta del colore ha la spunta. */
    val DEFAULT_NOTEBOOK: Int get() = palette.first()

    /** Il colore dopo `previous` nella tavolozza (il primo, se non c'è). */
    fun after(previous: Int?): Int {
        val index = previous?.let { palette.indexOf(it) } ?: -1
        return palette[(index + 1).mod(palette.size)]
    }
}
