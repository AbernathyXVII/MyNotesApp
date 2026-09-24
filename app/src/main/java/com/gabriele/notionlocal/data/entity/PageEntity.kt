package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Come una pagina-database mostra le sue righe. Ricalca i "layout" di
 * Notion. Qui ci sono solo quelli costruiti davvero: gli altri (oggi
 * manca solo il grafico) si aggiungono uno alla volta, perché ognuno è
 * una schermata a sé e offrirne una che non disegna niente sarebbe
 * peggio che non offrirla.
 *
 * Si salva **per nome**, quindi l'ordine qui si può cambiare senza
 * toccare i database già salvati. È anche l'ordine dei riquadri nel
 * selettore della vista.
 */
enum class DatabaseLayout {
    TABLE,
    BOARD,
    CALENDAR,
    TIMELINE,
    LIST,
    GALLERY
}

/**
 * Cosa mostra la parte alta di una scheda della galleria: niente, la
 * copertina della pagina, o l'inizio del suo testo. Sono le tre voci di
 * "Card preview" su Notion che si possono fare qui — la quarta, una
 * proprietà "Files & media", richiede un tipo di proprietà che l'app
 * non ha.
 */
enum class GalleryCardPreview {
    NONE,
    PAGE_COVER,
    PAGE_CONTENT
}

/** Quanto sono grandi le schede della galleria, come "Card size" su Notion. */
enum class GalleryCardSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Cosa mostra una galleria finché non si sceglie altro: **la
 * copertina**. È la ragione per cui la galleria è arrivata dopo le
 * copertine delle pagine — una griglia di schede che si riconoscono a
 * colpo d'occhio dall'immagine è la cosa che la distingue dall'elenco.
 * Chi ha pagine senza copertina passa a "Page content" dalle
 * impostazioni della vista.
 */
val GALLERY_DEFAULT_PREVIEW = GalleryCardPreview.PAGE_COVER

/** Schede medie finché non si sceglie altro: due per riga su un telefono. */
val GALLERY_DEFAULT_SIZE = GalleryCardSize.MEDIUM

/**
 * Il font del testo di una pagina, fra quelli chiesti dall'utente. Si
 * salva per nome, come le altre scelte. Nomi, sosia liberi e da dove
 * arrivano stanno in `ui/theme/PageFonts.kt`; l'ordine qui è quello del
 * menu: prima gli occidentali, poi i cinesi, poi i giapponesi.
 */
enum class PageFont {
    HELVETICA,
    GARAMOND,
    ARIAL,
    VERDANA,
    GEORGIA,
    CALIBRI,
    FUTURA,
    TIMES_NEW_ROMAN,
    CAMBRIA,
    CONSOLAS,
    SONGTI,
    KAITI,
    MS_YAHEI,
    MINCHO,
    GOTHIC,
    KAISEI
}

/**
 * Quanto tempo mostra il calendario in una schermata: un anno, un mese
 * o una settimana. Sostituisce il vecchio `calendarWeekView`, che
 * essendo un sì/no non poteva reggere una terza scelta.
 */
enum class CalendarMode {
    YEAR,
    MONTH,
    WEEK
}

/**
 * Quanto tempo sta in una schermata della linea del tempo. Sono gli
 * stessi ingrandimenti di Notion, meno "5 anni": a quella scala un
 * mese è largo due millimetri e non si distingue più niente.
 */
/**
 * Il valore di `sortColumnId` che vuol dire "ordina per nome della
 * pagina". Il nome è una colonna della tabella ma non una proprietà
 * del database, quindi non ha un id a cui riferirsi: gliene diamo uno
 * che nessun id vero può avere.
 */
const val SORT_BY_NAME = "__name__"

enum class TimelineZoom {
    HOURS,
    DAY,
    WEEK,
    BI_WEEK,
    MONTH,
    QUARTER,
    YEAR
}

/**
 * Una Pagina è il nodo base dell'albero di navigazione. Ogni pagina può
 * contenere:
 *  - contenuto rich text (salvato come blocchi, vedi BlockEntity)
 *  - OPPURE essere di tipo "database" e contenere righe (vedi
 *    DatabaseRowEntity), se isDatabase = true.
 *
 * parentId nullable = pagina di primo livello (radice dell'albero).
 * parentId valorizzato = sottopagina di un'altra pagina. Questo campo è
 * rimasto nello schema ma non è più popolato dalla UI: la gerarchia di
 * navigazione oggi vive nei blocchi PAGE_LINK/DATABASE_LINK dentro il
 * contenuto delle pagine, non più in un puntatore diretto pagina-pagina.
 *
 * isRowPage = true per le pagine create aprendo una riga di un database
 * come pagina vera (vedi DatabaseRowEntity.linkedPageId) — queste
 * pagine non devono comparire tra le pagine radice della Home.
 *
 * onDelete = CASCADE sul parent: cancellare una pagina cancella
 * automaticamente tutte le sue sottopagine, esattamente come in Notion.
 */
@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class PageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null,
    var title: String = "Senza titolo",
    var icon: String = "📄", // emoji come icona, semplice ed efficace
    // Se valorizzati, al posto dell'emoji e sopra al titolo si vede
    // un'immagine scelta dall'utente. È il **nome del file** dentro la
    // cartella delle immagini dell'app, non un percorso intero: la
    // cartella dell'app può cambiare posto fra un'installazione e
    // l'altra, il nome no.
    var iconImage: String? = null,
    var coverImage: String? = null,
    // Come è inquadrata la copertina dentro la sua striscia: quanto è
    // ingrandita (1 = riempie la striscia e basta) e di quanto è
    // spostata rispetto al centro. Lo spostamento è in **frazioni di
    // striscia**, non in pixel: così la stessa inquadratura vale su
    // qualunque schermo. Il file dell'immagine non viene mai toccato —
    // qui c'è solo come guardarla.
    var coverScale: Float = 1f,
    var coverOffsetX: Float = 0f,
    var coverOffsetY: Float = 0f,
    var isDatabase: Boolean = false,
    // **Database semplice** (solo con isDatabase = true): un database vero
    // — viste, filtri, ordinamento, raggruppamento, proprietà — in cui
    // però **le righe sono solo testo e non diventano mai pagine**. È la
    // cosa che l'utente ha sempre voluto e che Notion non ha: nessun OPEN,
    // nessuna icona o copertina di riga, e toccando una riga si apre la
    // sua scheda (nome e proprietà) invece di una pagina. Si sceglie
    // quando lo si crea ("Simple database" nei menu "/" e "+").
    var isSimpleDatabase: Boolean = false,
    var isRowPage: Boolean = false,
    // Solo per le pagine con isDatabase = true: come vengono mostrate le
    // righe (tabella, elenco, bacheca...). Ammette null perché i
    // database creati prima che questa scelta esistesse non ne hanno
    // una, e null vale "tabella", che è quello che stavano già usando.
    var databaseLayout: DatabaseLayout? = null,
    // Quale proprietà fa da colonna nella vista a bacheca. null = non
    // scelta: si usa la prima proprietà a selezione singola che c'è.
    var boardGroupColumnId: String? = null,
    // In base a quale proprietà la **tabella** raggruppa le righe.
    // null = nessun raggruppamento, cioè l'elenco piatto di sempre. È
    // un campo diverso da `boardGroupColumnId` di proposito: la
    // bacheca è fatta di colonne e senza raggruppamento non esiste,
    // la tabella invece funziona benissimo senza, e chi cambia vista
    // non si ritrova l'altra rimescolata.
    var tableGroupColumnId: String? = null,
    // Se i gruppi senza nemmeno una pagina si nascondono. Acceso come
    // su Notion: un elenco di intestazioni vuote è rumore.
    var hideEmptyGroups: Boolean = true,
    // Quale proprietà data colloca le righe nel calendario. null = non
    // scelta: si usa la prima proprietà di tipo data che c'è.
    var calendarDateColumnId: String? = null,
    // Da non usare: sostituito da calendarMode. Resta nell'entità solo
    // perché la colonna esiste ancora nel database — toglierla vorrebbe
    // dire ricreare la tabella, e SQLite sa cancellare una colonna solo
    // dalla 3.35, cioè su telefoni più nuovi del minimo che l'app
    // sostiene. Il suo valore è già stato travasato in calendarMode
    // dalla migrazione 11→12.
    var calendarWeekView: Boolean? = null,
    // Se il calendario mostra un anno, un mese o una settimana. null
    // vale "mese", che è come si apriva prima.
    var calendarMode: CalendarMode? = null,
    // Quanto tempo sta in una schermata della linea del tempo. null
    // vale "giorno", che è come si apriva prima.
    var timelineZoom: TimelineZoom? = null,
    // Cosa mostra la parte alta delle schede della galleria, e quanto
    // sono grandi. null vale "copertina" e "media": vedi
    // `GALLERY_DEFAULT_PREVIEW` e `GALLERY_DEFAULT_SIZE` qui sopra, dove
    // è spiegato il perché della scelta.
    var galleryCardPreview: GalleryCardPreview? = null,
    var galleryCardSize: GalleryCardSize? = null,
    // Il font e il corpo del testo della pagina, scelti dalla barra Aa.
    // Valgono per **tutta la pagina**, non per un pezzo di testo. null
    // vale "come prima": il font di sistema e il corpo 16.
    var pageFont: PageFont? = null,
    var pageFontSize: Int? = null,
    // In base a quale proprietà sono ordinate le righe, e in che verso.
    // null = nell'ordine in cui sono state create, che è come stavano
    // prima che l'ordinamento esistesse. Il valore speciale
    // SORT_BY_NAME ordina per nome della pagina, che è una colonna
    // della tabella ma non una proprietà nel database.
    var sortColumnId: String? = null,
    var sortDescending: Boolean = false,
    // In base a quale proprietà sono filtrate le righe, e su quale
    // valore. null = nessun filtro, cioè tutte le pagine — che è come
    // stavano prima che il filtro esistesse.
    //
    // Come si legge `filterValue` dipende dal tipo della proprietà: per
    // i tag sono le etichette scelte separate da MULTI_VALUE_SEPARATOR,
    // per le date è lo stesso formato di una cella data (inizio, e la
    // fine se il filtro copre più giorni). Riusare quel formato invece
    // di inventarne un altro vuol dire che a leggerlo è la stessa
    // funzione che legge le celle, quindi le due cose non possono
    // separarsi.
    var filterColumnId: String? = null,
    var filterValue: String? = null,
    // Se il titolo del database si vede quando è dentro una pagina.
    // Vale solo lì: aperto a schermo intero il titolo c'è sempre,
    // perché è l'unica cosa che dice dove si è finiti.
    var showEmbeddedTitle: Boolean = true,
    // Se la pagina sta fra i preferiti.
    //
    // **Una colonna sola per pagine e database**: qui un database è una
    // pagina con `isDatabase = true`, quindi mettere fra i preferiti
    // l'uno o l'altro è esattamente la stessa operazione. Un secondo
    // elenco per i database avrebbe voluto dire due posti da tenere in
    // fila e due modi di sbagliare.
    //
    // La pagina radice non si può mettere fra i preferiti: è quella da
    // cui si parte sempre, un collegamento a lei non porterebbe da
    // nessuna parte. Il divieto lo applica la UI, che nasconde
    // l'interruttore, e lo ribadisce `PageRepository.setFavorite`.
    var isFavorite: Boolean = false,
    // Quando è stata messa fra i preferiti: l'elenco dei preferiti la
    // mostra e ci si ordina sopra. Null per le pagine che lo erano già
    // prima che la data si salvasse — per quelle la data vera non c'è, e
    // inventarne una sarebbe peggio che dire che non si sa.
    var favoritedAt: Long? = null,
    // Pagina bloccata: il contenuto si legge ma non si modifica. È il
    // "Lock page" di Notion, e vale per qualsiasi pagina.
    var isLocked: Boolean = false,
    // Vista bloccata: le righe di un database si modificano ancora, ma
    // **come** sono mostrate no — niente cambio di vista, ordinamento,
    // filtro, raggruppamento o colonne. È il "Lock view", una cosa
    // diversa dal blocco del contenuto: si blocca l'impaginazione
    // proprio per poter continuare a lavorare sui dati senza
    // scombinarla per sbaglio.
    var isViewLocked: Boolean = false,
    // Quando è stata buttata nel cestino, o null se non ci sta. La
    // pagina **resta nel database** con tutto il suo contenuto: qui si
    // cancella per davvero solo il collegamento che la mostrava, così
    // sparisce da dov'era senza che niente vada perso.
    var trashedAt: Long? = null,
    var orderIndex: Int = 0, // per riordinare manualmente le pagine nella sidebar
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        // ID fisso invece di un UUID casuale: è l'unica vera pagina
        // radice dell'app (quella che prima era la schermata Home
        // separata), creata al volo al primo avvio se non esiste
        // ancora. Un ID fisso e conosciuto evita di dover cercare "qual
        // è la pagina radice" con una query o un flag dedicato.
        const val ROOT_PAGE_ID = "root"
    }
}
