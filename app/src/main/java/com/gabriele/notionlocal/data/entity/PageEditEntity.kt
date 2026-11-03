package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Una modifica fatta a una pagina, come la racconta la voce "Updates":
 * quando, cosa c'era scritto e cosa c'è scritto adesso.
 *
 * **Non è una riga per tasto premuto.** Si registra una voce per
 * *sessione di scrittura*: finché si continua a scrivere nello stesso
 * blocco è una modifica sola, e si chiude quando il cursore passa a un
 * altro blocco, quando cambia la struttura della pagina o quando si
 * esce. È lo stesso confine che l'Annulla usa già per decidere cosa
 * togliere in un colpo — e per la stessa ragione: "Esmepoi → Esempio" è
 * una correzione, non sette.
 *
 * `blockId` non ha un vincolo verso i blocchi di proposito: la
 * cronologia deve sopravvivere al blocco che racconta, altrimenti
 * cancellare un paragrafo cancellerebbe anche la prova che c'era.
 * Verso la pagina invece il vincolo c'è ed è a cascata: la cronologia
 * di una pagina che non esiste più non la leggerà mai nessuno.
 *
 * `TITLE_BLOCK_ID` è il finto blocco del titolo: il titolo non è un
 * blocco ma si modifica come loro, e dargli un id riservato evita di
 * dover trattare ovunque un `null` come caso a sé.
 */
@Entity(
    tableName = "page_edits",
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
data class PageEditEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pageId: String,
    val blockId: String,
    /** Quando la sessione di scrittura si è chiusa, in millisecondi. */
    val at: Long = System.currentTimeMillis(),
    /** Il testo semplice prima e dopo: alla cronologia serve cosa si legge, non come era scritto. */
    val before: String,
    val after: String
) {
    companion object {
        const val TITLE_BLOCK_ID = "__title__"
    }
}
