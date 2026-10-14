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
    /**
     * **"Duration"** (dal 29/09/2026): quanto passa fra un inizio e una fine
     * scelti a mano, in anni, mesi, giorni, ore e minuti. Il valore è quello
     * di una data col periodo e l'ora; vedi `Duration.kt` e `DurationCell.kt`.
     */
    DURATION,
    CHECKBOX,
    /**
     * "Color cell" (dal 26/09/2026): la cella si riempie di un colore
     * scelto, con sopra del testo se si vuole. Vedi `ColorCellValue`.
     */
    COLOR,
    SELECT,
    MULTI_SELECT,
    URL,
    EMAIL,
    PHONE,
    /**
     * **"Page link"** (dal 29/09/2026): il collegamento a un'altra pagina
     * del Workspace, anche quella di un'altra riga dello stesso database.
     * Il valore è l'id della pagina. Vedi `PageLinkCell.kt`.
     */
    PAGE_LINK,
    /**
     * **"Image"** (dal 29/09/2026): una cella quadrata con un'immagine
     * intera, scelta come l'icona di una pagina. Il valore è il nome del
     * file (`PageImageStore`). Vedi `ImageCell.kt`.
     */
    IMAGE,
    CREATED_TIME,
    LAST_EDITED_TIME
}

/**
 * Se si può raggruppare per una proprietà di questo tipo, nella tabella e
 * nella bacheca: quello che conta è che il valore stia in una cella e che
 * si legga. "Creata il" e "Modificata il" non ci sono perché non sono celle
 * ma proprietà della riga, e un gruppo per istante esatto sarebbe un gruppo
 * per riga. Un gruppo per colore avrebbe per nome un codice ("#E03E3E"), e
 * "Page link" e "Image" salvano un id o il nome di un file: non si leggono.
 * Una durata ha per valore due istanti: anche lì, un gruppo per riga.
 */
val ColumnType.canGroup: Boolean
    get() = this != ColumnType.CREATED_TIME &&
        this != ColumnType.LAST_EDITED_TIME &&
        this != ColumnType.COLOR &&
        this != ColumnType.PAGE_LINK &&
        this != ColumnType.IMAGE &&
        this != ColumnType.DURATION

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
    var centerContent: Boolean = false,
    // --- Solo per le colonne Number (dal 26/09/2026), "Edit property" ---
    // Valgono in tutte le viste che mostrano la colonna, come su Notion.
    // Salvati per nome e letti con `NumberFormatKind.of` e `NumberDisplay.of`:
    // un nome sconosciuto torna al valore di sempre invece di far chiudere
    // l'app. Vedi `NumberFormat.kt`.
    /** Number, Percent o una valuta; null = Number. */
    var numberFormat: String? = null,
    /** Quante cifre dopo la virgola si vedono (0–5); null = quelle scritte. */
    var decimalPlaces: Int? = null,
    /** Number, Bar o Ring; null = Number. */
    var numberDisplay: String? = null,
    /** Il colore della barra o dell'anello, "#RRGGBB"; null = il verde di sempre. */
    var numberColor: String? = null,
    /** Accanto alla barra o all'anello si vede anche il numero. */
    var showNumber: Boolean = true,
    /** L'anello in mezzo alla cella invece che a sinistra. */
    var ringCentered: Boolean = false,
    /**
     * **Dove sta il contenuto delle celle** (dal 26/09/2026): a sinistra, al
     * centro o a destra, per ogni tipo; le intestazioni restano sempre a
     * sinistra. Null = come prima: al centro se `centerContent`, se no a
     * sinistra. Letto con `cellAlignment`.
     */
    var contentAlignment: String? = null,
    // --- Solo per le colonne Date (dal 26/09/2026), "Edit property" ---
    /**
     * Come si scrive la data (il nome di uno stile `DateStyle`, gli stessi
     * che prima si sceglievano per tutta l'app nelle impostazioni). Null =
     * quello scelto allora nelle impostazioni.
     */
    var dateFormat: String? = null,
    /** "H12" a 12 ore (9:30 PM), "HIDDEN" senza ora; null = a 24 ore (21:30). Vedi `TimeFormatKind`. */
    var timeFormat: String? = null,
    /**
     * **Solo per le colonne Duration** (dal 29/09/2026), "Edit property":
     * fino a quale unità si scrive la durata (il nome di un `DurationUnit`,
     * da YEARS a MINUTES). Null = tutto, fino ai minuti.
     */
    var durationFormat: String? = null
)

/** A sinistra, al centro o a destra: le celle di una colonna, e i calcoli sotto la tabella. */
enum class CellAlignment {
    LEFT,
    CENTER,
    RIGHT;

    companion object {
        fun of(name: String?): CellAlignment? = entries.find { it.name == name }
    }
}

/** Dove sta il contenuto delle celle di questa colonna. */
val DatabaseColumnEntity.cellAlignment: CellAlignment
    get() = CellAlignment.of(contentAlignment) ?: if (centerContent) CellAlignment.CENTER else CellAlignment.LEFT
