package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Tipo di blocco di contenuto. Modellare il rich text a blocchi (come fa
 * davvero Notion) invece che come un'unica stringona HTML rende la ricerca,
 * il riordino e le estensioni future molto più semplici.
 */
enum class BlockType {
    PARAGRAPH,
    // HEADING_1, HEADING_2 e HEADING_3 c'erano fino al 24/09/2026: tolti
    // su richiesta dell'utente, che ora sceglie il corpo del testo per
    // pagina dalla barra Aa e coi titoli a misura fissa non ci faceva
    // più niente. La migrazione 26→27 ha trasformato quelli già scritti
    // in paragrafi. **Non rimetterli senza una migrazione**: un blocco
    // salvato con un tipo che l'enum non conosce fa chiudere l'app.
    BULLET_LIST_ITEM,
    NUMBERED_LIST_ITEM,
    CHECKBOX,
    TOGGLE,
    TABLE,
    PAGE_LINK,
    DATABASE_LINK,
    DIVIDER
}

/**
 * Un Blocco è una singola unità di contenuto dentro una pagina.
 * textJson contiene il testo con eventuale formattazione inline
 * (grassetto/corsivo) serializzata come JSON semplice — vedi
 * RichText.kt per la struttura.
 *
 * I blocchi "di testo scorrevole" (PARAGRAPH,
 * BULLET_LIST_ITEM, NUMBERED_LIST_ITEM) restano righe separate qui nel
 * database, ma a livello di editor vengono uniti in un unico campo di
 * testo continuo per ogni sequenza consecutiva — è così che selezione
 * e backspace-tra-paragrafi funzionano in modo nativo, esattamente
 * come farebbero in un editor di testo normale. Per questi tipi,
 * indentLevel sostituisce parentBlockId come indicatore di rientro: un
 * semplice numero di livello (come Tab in Word), non una vera
 * struttura ad albero.
 *
 * CHECKBOX, TOGGLE, TABLE, DIVIDER, PAGE_LINK, DATABASE_LINK restano
 * "isole" separate fuori dal flusso di testo continuo, ciascuna il
 * proprio elemento a sé — per questi, parentBlockId conserva il
 * vecchio significato (nidificazione vera, usata dai TOGGLE per i
 * blocchi al loro interno).
 *
 * linkedPageId valorizzato solo per type == PAGE_LINK o DATABASE_LINK:
 * punta alla PageEntity vera e propria che il blocco rappresenta (la
 * "pagina figlia" a cui si naviga toccandolo) — stesso meccanismo già
 * usato da DatabaseRowEntity.linkedPageId per le righe aperte come
 * pagina. onDelete = SET_NULL: se la pagina collegata viene cancellata
 * altrove, il blocco resta ma perde il collegamento invece di sparire
 * di colpo — eliminare il blocco stesso NON cancella la pagina
 * collegata, per evitare di perdere contenuto per sbaglio con un tap.
 */
@Entity(
    tableName = "blocks",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentBlockId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedPageId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("pageId"), Index("parentBlockId"), Index("linkedPageId")]
)
data class BlockEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pageId: String,
    var parentBlockId: String? = null,
    var type: BlockType = BlockType.PARAGRAPH,
    var textJson: String = "[]", // lista di RichTextSpan serializzata
    var isChecked: Boolean = false, // usato solo se type == CHECKBOX
    var isExpanded: Boolean = true, // usato solo se type == TOGGLE
    var tableRows: Int = 2, // usato solo se type == TABLE
    var tableCols: Int = 2, // usato solo se type == TABLE
    var linkedPageId: String? = null, // usato solo se type == PAGE_LINK/DATABASE_LINK
    // Eredità: sostituito da numberStartsAt, che sa dire da QUALE numero
    // ripartire e non solo "riparti da 1". La migrazione 5→6 ha travasato
    // i valori esistenti. Non viene più né letto né scritto, ma la colonna
    // resta perché toglierla richiederebbe di ricreare la tabella.
    var numberResetHere: Boolean = false,
    // Usato solo se type == NUMBERED_LIST_ITEM. null = prosegui il
    // conteggio dall'elemento precedente; un numero = la numerazione
    // riparte da lì (scrivendo "5." a inizio blocco, o toccando il numero
    // per farla ripartire da 1).
    var numberStartsAt: Int? = null,
    var indentLevel: Int = 0, // usato solo dai tipi di testo scorrevole: livello di rientro visivo, come Tab in Word
    var orderIndex: Int = 0
)
