package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Tipo di campo per una colonna di database, ricalcando le property types
 * di Notion. SELECT usa optionsJson per l'elenco di opzioni disponibili
 * (con colore associato).
 */
enum class ColumnType {
    TEXT,
    NUMBER,
    DATE,
    CHECKBOX,
    SELECT,
    MULTI_SELECT,
    URL,
    EMAIL,
    PHONE,
    CREATED_TIME,
    LAST_EDITED_TIME
}

/**
 * Una singola opzione disponibile per una colonna SELECT. Serializzata
 * come lista JSON dentro DatabaseColumnEntity.optionsJson — usare questo
 * data class (invece di costruire il JSON a mano) evita errori di
 * escaping ed è coerente con come RichTextSpan gestisce il rich text.
 */
@Serializable
data class SelectOption(
    val label: String,
    val color: String = TAG_COLORS.first().hex
)

/**
 * I valori di una colonna a selezione multipla stanno in un'unica
 * stringa, separati da questo carattere: la tabella delle celle ha una
 * colonna `value` sola, e tenere lì una lista evita di cambiare lo
 * schema. La barra verticale non compare quasi mai nelle etichette, a
 * differenza della virgola.
 */
const val MULTI_VALUE_SEPARATOR = "|"

/** Un colore per i tag, col nome che gli dà Notion. */
data class TagColor(val name: String, val hex: String)

/**
 * I dieci colori dei tag, campionati dalla versione scura di Notion.
 *
 * Quando si crea un tag il colore viene assegnato a giro invece di
 * chiederlo: sceglierlo sarebbe una schermata in più per una decisione
 * che quasi nessuno vuole prendere subito, e lasciarli tutti grigi
 * toglierebbe il colpo d'occhio che è il motivo per cui esistono i tag.
 * Cambiarlo dopo si può, dal menu del tag.
 */
val TAG_COLORS = listOf(
    TagColor("Default", "#373735"),
    TagColor("Gray", "#686762"),
    TagColor("Brown", "#765B48"),
    TagColor("Orange", "#8E5834"),
    TagColor("Yellow", "#89692C"),
    TagColor("Green", "#3F6F55"),
    TagColor("Blue", "#3A6590"),
    TagColor("Purple", "#6E5483"),
    TagColor("Pink", "#824D67"),
    TagColor("Red", "#984F49")
)

/**
 * Una colonna definita dall'utente per una pagina di tipo "database"
 * (PageEntity.isDatabase = true). Esempio: "Stato" di tipo SELECT con
 * opzioni ["Da fare", "In corso", "Fatto"].
 */
@Entity(
    tableName = "database_columns",
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
data class DatabaseColumnEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pageId: String,
    var name: String,
    var type: ColumnType = ColumnType.TEXT,
    var optionsJson: String = "[]", // elenco opzioni per SELECT, es: [{"label":"Fatto","color":"#4CAF50"}]
    var orderIndex: Int = 0,
    /**
     * Se la colonna è nascosta nella tabella.
     *
     * **Nascosta non vuol dire cancellata**: i valori restano nelle
     * celle, la proprietà continua a comparire nelle azioni di una
     * riga, negli ordinamenti e nei raggruppamenti, e rimostrandola si
     * ritrova tutto com'era. È solo una colonna in meno da scorrere in
     * orizzontale, che su un telefono è il motivo per cui serve.
     */
    var hidden: Boolean = false,
    /**
     * Se il contenuto della cella sta al centro della colonna invece
     * che a sinistra.
     *
     * Vale per le caselle da spuntare, dove un quadratino solo
     * appoggiato al bordo sinistro di una colonna larga sembra fuori
     * posto. Essendo un allineamento e non una misura, resta al centro
     * qualunque larghezza abbia la colonna.
     */
    var centerContent: Boolean = false
)
