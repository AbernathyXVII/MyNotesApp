package com.gabriele.notionlocal.viewmodel

import com.gabriele.notionlocal.data.symbolField
import com.gabriele.notionlocal.data.replacedImage
import com.gabriele.notionlocal.data.imageField
import com.gabriele.notionlocal.data.iconField
import com.gabriele.notionlocal.data.chosenEmoji
import com.gabriele.notionlocal.data.RowIconRef
import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.nameOrderAt
import com.gabriele.notionlocal.data.entity.tableKeys
import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.entity.durationMillisOfCell
import com.gabriele.notionlocal.data.entity.minColumnWidth
import com.gabriele.notionlocal.data.entity.CellOverflow
import com.gabriele.notionlocal.data.entity.defaultColumnWidth
import com.gabriele.notionlocal.data.entity.snapColumnWidth
import com.gabriele.notionlocal.data.entity.encodeColumnWidths
import com.gabriele.notionlocal.data.entity.encodeFitColumns
import com.gabriele.notionlocal.data.entity.parseColumnWidths
import com.gabriele.notionlocal.data.entity.parseFitColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.dao.RowCover
import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.canGroup
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.ChartSettings
import com.gabriele.notionlocal.data.entity.encodeChartSettings
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.GALLERY_DEFAULT_PREVIEW
import com.gabriele.notionlocal.data.entity.GalleryCardPreview
import com.gabriele.notionlocal.data.entity.GalleryCardSize
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.parseColorCell
import com.gabriele.notionlocal.data.entity.Calculation
import com.gabriele.notionlocal.data.entity.NumberSettings
import com.gabriele.notionlocal.data.entity.encodeViewCalculations
import com.gabriele.notionlocal.data.entity.parseViewCalculations
import com.gabriele.notionlocal.data.entity.parseCalculationAlignments
import com.gabriele.notionlocal.data.entity.parseCalculationSubRows
import com.gabriele.notionlocal.data.entity.CellAlignment
import com.gabriele.notionlocal.data.entity.withNumberSettings
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.TAG_COLORS
import com.gabriele.notionlocal.data.entity.TimelineZoom
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.RowPreviewLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Quante righe di testo di una pagina si leggono per l'anteprima di una
 * scheda della galleria. Più di quante ne stiano nella scheda più alta:
 * quelle in eccesso le taglia la scheda, mentre leggerne di meno la
 * lascerebbe mezza vuota su una pagina che ha testo da mostrare.
 */
private const val GALLERY_PREVIEW_MAX_LINES = 12

/**
 * Quale anteprima mostra la galleria di questo database, o null se la
 * vista scelta non è la galleria — e allora non ne serve nessuna.
 */
private fun PageEntity.galleryPreviewShown(simple: Boolean = isSimpleDatabase): GalleryCardPreview? =
    // Un database semplice non ha pagine di riga, quindi niente copertine
    // né testo da leggere: nessuna query. `simple` è quello del database
    // che ha i dati: per una vista collegata, quello di origine.
    if (databaseLayout == DatabaseLayout.GALLERY && !simple) {
        galleryCardPreview ?: GALLERY_DEFAULT_PREVIEW
    } else {
        null
    }

/**
 * Il valore di una cella ridotto a quello che serve per ordinarla: un
 * numero quando ce n'è uno, e il testo per tutto il resto.
 */
private data class SortValue(val number: Double?, val text: String) {
    val isEmpty: Boolean get() = number == null && text.isBlank()
}

/**
 * Stato di una tabella database completa: colonne, righe, e una mappa
 * rapida (rowId, columnId) -> valore per popolare le celle senza dover
 * cercare ogni volta nella lista.
 */
/**
 * La ricerca in corso dentro un database (la lente accanto a Sort), per
 * chi disegna le righe: la parola cercata, per illuminarla nei nomi, e le
 * righe trovate **per quello che hanno dentro** — nelle proprietà o nella
 * pagina — e non nel nome, che si illuminano per intero.
 */
data class RowSearch(val query: String, val foundInside: Set<String>)

/**
 * Quanto restano ferme le righe dopo che l'ultima casella spuntata si è
 * riempita, prima di andare al loro posto: vedi
 * `DatabaseViewModel.checkboxHold`. Era un secondo, "estremamente troppo"
 * per l'utente (09/10/2026).
 */
internal const val CHECKBOX_SETTLE_MS = 200L

data class DatabaseTableState(
    val columns: List<DatabaseColumnEntity> = emptyList(),
    val rows: List<DatabaseRowEntity> = emptyList(),
    val cellValues: Map<Pair<String, String>, String> = emptyMap(),
    /** Null quando non si sta cercando niente. */
    val search: RowSearch? = null
) {
    /**
     * Le colonne da disegnare. `columns` resta l'elenco completo, perché
     * una proprietà nascosta continua a esistere ovunque non sia la
     * griglia: si ordina, si raggruppa e si scrive lo stesso.
     */
    val visibleColumns: List<DatabaseColumnEntity> get() = columns.filterNot { it.hidden }

    /**
     * L'ordine della tabella, da sinistra a destra, dopo aver spostato `key`
     * — una colonna, o Name con [NAME_COLUMN_KEY] (dal 29/09/2026) — di un
     * posto a sinistra (`delta` -1) o a destra (+1), oltre la vicina. Null
     * se da quella parte non c'è niente. `nameOrder` è dove sta Name adesso
     * (vedi `namePosition`).
     *
     * La vicina si cerca **fra quelle che si vedono**: nella tabella è la
     * colonna accanto, e scambiarsi con una nascosta in mezzo non
     * sposterebbe niente di visibile. Name si vede sempre. Una colonna
     * nascosta invece si muove fra tutte, perché fra le visibili non c'è.
     */
    fun orderAfterMove(key: String, delta: Int, nameOrder: Int): List<String>? {
        val keys = tableKeys(columns, nameOrder).toMutableList()
        if (key !in keys) return null
        val hidden = columns.filter { it.hidden }.map { it.id }.toSet()
        val lane = if (key in hidden) keys else keys.filterNot { it in hidden }
        val neighbour = lane.getOrNull(lane.indexOf(key) + delta) ?: return null
        keys.remove(key)
        val at = keys.indexOf(neighbour)
        keys.add(if (delta < 0) at else at + 1, key)
        return keys
    }
}

/** Le proprietà in cui cerca la lente: quelle il cui valore salvato è il testo che si legge. */
private val SEARCHABLE_COLUMN_TYPES = setOf(
    ColumnType.TEXT,
    ColumnType.COLOR,
    ColumnType.NUMBER,
    ColumnType.SELECT,
    ColumnType.MULTI_SELECT,
    ColumnType.URL,
    ColumnType.EMAIL,
    ColumnType.PHONE
)

/**
 * **La ricerca**: restano le righe in cui la parola c'è — nel nome,
 * nel valore di una proprietà scritta (testo, numero, selezione,
 * collegamento, email, telefono), o dentro la loro pagina. Come su
 * Notion, le altre spariscono finché si cerca. Maiuscole e minuscole
 * non contano.
 *
 * Date, caselle e date automatiche non si cercano: il loro valore
 * salvato ("2026-09-24", "true") non è quello che si legge sullo
 * schermo, e "true" troverebbe ogni casella spuntata.
 */
internal fun searchRows(
    rows: List<DatabaseRowEntity>,
    columns: List<DatabaseColumnEntity>,
    cellValues: Map<Pair<String, String>, String>,
    query: String,
    index: Map<String, String>,
    /**
     * Le righe create mentre si cerca: restano, anche se la parola non ce
     * l'hanno. Una riga nuova è vuota, e sparendo subito sembrerebbe che
     * "Nuova pagina" non abbia fatto niente.
     */
    addedWhileSearching: Set<String> = emptySet()
): Pair<List<DatabaseRowEntity>, RowSearch?> {
    val wanted = query.trim()
    if (wanted.isEmpty()) return rows to null
    val searchable = columns.filter { it.type in SEARCHABLE_COLUMN_TYPES }
    val foundInside = mutableSetOf<String>()
    val kept = rows.filter { row ->
        if (row.id in addedWhileSearching) return@filter true
        if (row.title.contains(wanted, ignoreCase = true)) return@filter true
        val inProperty = searchable.any { column ->
            val raw = cellValues[row.id to column.id].orEmpty()
            // Di una cella colorata si cerca il testo, non il codice del colore.
            val readable = if (column.type == ColumnType.COLOR) parseColorCell(raw).text else raw
            readable.replace(MULTI_VALUE_SEPARATOR, " ").contains(wanted, ignoreCase = true)
        }
        val inPage = index[row.id]?.contains(wanted, ignoreCase = true) == true
        (inProperty || inPage).also { found -> if (found) foundInside += row.id }
    }
    return kept to RowSearch(wanted, foundInside)
}

/** Le proprietà nascoste di una vista collegata (vedi `PageEntity.viewHiddenColumnIds`). */
internal fun PageEntity.viewHiddenColumns(): Set<String> =
    viewHiddenColumnIds.orEmpty().split(",").filter { it.isNotBlank() }.toSet()

class DatabaseViewModel(
    private val repository: DatabaseRepository,
    private val pageRepository: PageRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private var currentPageId: String? = null

    private val _tableState = MutableStateFlow(DatabaseTableState())
    val tableState: StateFlow<DatabaseTableState> = _tableState

    // La pagina che contiene la tabella: serve per mostrarne (e
    // cambiarne) il nome. Senza, ogni database resterebbe "Untitled" per
    // sempre, perché questa schermata è l'unico posto da cui lo si
    // raggiunge.
    private val _page = MutableStateFlow<PageEntity?>(null)
    val page: StateFlow<PageEntity?> = _page

    // **Vista collegata** (`PageEntity.sourceDatabaseId`): `_page` è la
    // vista, con le sue impostazioni; righe, colonne e celle stanno nel
    // database di origine, che si segue qui. Per un database vero le due
    // cose coincidono.
    private class SourceRead(val page: PageEntity?)
    private val _source = MutableStateFlow<SourceRead?>(null)

    /** La pagina che ha i dati: l'origine per una vista collegata, la pagina stessa per un database. */
    private val _dataPage = MutableStateFlow<PageEntity?>(null)
    val dataPage: StateFlow<PageEntity?> = _dataPage

    // --- "Page link" (dal 29/09/2026) ---

    private val _linkedPages = MutableStateFlow<Map<String, PageEntity>>(emptyMap())

    /** Le pagine collegate dalle celle "Page link", per id: il titolo e l'icona di adesso. */
    val linkedPages: StateFlow<Map<String, PageEntity>> = _linkedPages

    // --- Le viste del database (dal 29/09/2026, come su Notion) ---

    private val _views = MutableStateFlow<List<PageEntity>>(emptyList())

    /** Le viste in più di questo database; la sua è `page`. */
    val views: StateFlow<List<PageEntity>> = _views

    /** Una vista nuova, subito davanti; `onCreated` riceve il suo id. */
    fun createView(name: String?, layout: DatabaseLayout, onCreated: (String) -> Unit = {}) {
        val hostId = currentPageId ?: return
        viewModelScope.launch { pageRepository.createView(hostId, name, layout)?.let(onCreated) }
    }

    /** La vista da mostrare: `viewId` uguale al database, la sua. */
    fun setActiveView(viewId: String) {
        val hostId = currentPageId ?: return
        viewModelScope.launch { pageRepository.setActiveView(hostId, viewId) }
    }

    fun renameView(viewId: String, name: String?) {
        viewModelScope.launch { pageRepository.renameView(viewId, name) }
    }

    fun deleteView(viewId: String) {
        viewModelScope.launch { pageRepository.deleteView(viewId) }
    }

    /** Toglie la vista di partenza: vedi `PageRepository.deleteStartingView`. */
    fun deleteStartingView() {
        val hostId = currentPageId ?: return
        viewModelScope.launch { pageRepository.deleteStartingView(hostId) }
    }

    /** Una vista collegata il cui database non c'è più, o è nel cestino. */
    private val _sourceMissing = MutableStateFlow(false)
    val sourceMissing: StateFlow<Boolean> = _sourceMissing

    /** L'id del database che ha i dati: vedi `dataPage`. */
    private val dataPageId: String?
        get() = _page.value?.let { it.sourceDatabaseId ?: it.id }

    /**
     * Le colonne **come sono nel database**. Quelle dello stato, per una
     * vista collegata, hanno `hidden` preso dalla vista: salvare una di
     * quelle scriverebbe il "nascosto" della vista dentro l'origine, dove
     * lo vedrebbero tutti. Chi modifica una colonna parte da qui.
     */
    private var rawColumns: List<DatabaseColumnEntity> = emptyList()

    /** Il numero d'ordine più alto fra **tutte** le righe, anche quelle che il filtro o la ricerca nascondono. */
    private var lastRowOrderIndex: Int = -1
    private fun rawColumn(id: String): DatabaseColumnEntity? = rawColumns.find { it.id == id }

    /** Riga → immagine dell'icona della sua pagina, per le righe che ne hanno una. */
    private val _rowIcons = MutableStateFlow<Map<String, RowIconRef>>(emptyMap())
    val rowIcons: StateFlow<Map<String, RowIconRef>> = _rowIcons

    /** Riga → copertina della sua pagina: le schede della galleria. */
    private val _rowCovers = MutableStateFlow<Map<String, RowCover>>(emptyMap())
    val rowCovers: StateFlow<Map<String, RowCover>> = _rowCovers

    /** Riga → prime righe di testo della sua pagina: l'anteprima "Page content" della galleria. */
    private val _rowPreviews = MutableStateFlow<Map<String, List<RowPreviewLine>>>(emptyMap())
    val rowPreviews: StateFlow<Map<String, List<RowPreviewLine>>> = _rowPreviews

    /** Le righe con la pagina bloccata dalla password: nella galleria, il lucchetto al posto dell'anteprima. */
    private val _lockedRows = MutableStateFlow<Set<String>>(emptySet())
    val lockedRows: StateFlow<Set<String>> = _lockedRows

    // --- La ricerca dentro il database (la lente accanto a Sort) ---
    //
    // Sta qui e non nella schermata: aprendo una pagina trovata e
    // tornando indietro la ricerca deve essere ancora lì, com'era, come
    // su Notion. Il ViewModel del database resta vivo mentre si è dentro
    // la pagina di una riga; la schermata no.

    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** Riga → testo della sua pagina: vedi `DatabaseRepository.searchIndex`. */
    private val _searchIndex = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Le righe create mentre si cerca: vedi `searchRows`. */
    private val _addedWhileSearching = MutableStateFlow<Set<String>>(emptySet())

    /**
     * **L'ordine delle righe mentre se ne scrive il nome** (dal 26/09/2026):
     * con un ordinamento acceso, cancellando o cambiando il nome la riga
     * sarebbe saltata al suo nuovo posto a ogni lettera — con l'ordine per
     * nome, un nome vuoto va in fondo. Così resta dov'è finché non si
     * finisce di scrivere, e solo allora va al suo posto.
     */
    private val _frozenOrder = MutableStateFlow<List<String>?>(null)

    /**
     * **Le righe ferme dopo una spunta** (dal 08/10/2026, trovato
     * dall'utente in Food, ordinato per la sua casella). Spuntando una
     * casella su cui la vista ordina (o filtra), la riga saltava subito al
     * suo nuovo posto: le righe in mezzo scalavano di uno, e sotto il dito
     * arrivava un'altra riga, con la sua casella che si ridisegnava —
     * sembrava che scattassero caselle mai toccate, e un secondo tocco nello
     * stesso punto spuntava una riga diversa. Ora la casella si riempie
     * dov'è, e l'ordine e le righe che si vedono restano quelli del momento
     * della spunta finché la casella non è piena, e poi per altri
     * [CHECKBOX_SETTLE_MS] dopo **l'ultima** spunta: allora le righe vanno al
     * loro posto, ognuna con la sua casella (09/10/2026: la prima versione le
     * teneva ferme finché si restava sulla pagina, e all'utente mancava
     * vedere la presa scendere in fondo). Il tempo si conta da quando il
     * valore nuovo è arrivato dal database, non dal tocco: con un attimo così
     * corto un salvataggio lento farebbe partire la riga prima che la sua
     * casella si sia riempita, e si tornerebbe allo scatto. Spuntando più
     * righe di fila restano tutte ferme e scendono insieme. Anche uscendo
     * dalla pagina
     * ([releaseCheckboxHold], dalla schermata) o cambiando ordine e filtro
     * vanno subito al loro posto.
     */
    private var checkboxHold = false

    /** Il conto alla rovescia che rimette a posto le righe: vedi [checkboxHold]. */
    private var holdRelease: Job? = null

    /** Le righe che si vedevano alla spunta: restano anche se il filtro ora le nasconderebbe. */
    private val _heldVisible = MutableStateFlow<Set<String>?>(null)

    private fun keepVisibleWhileSearching(rowId: String) {
        if (_searchQuery.value.isNotBlank()) _addedWhileSearching.value = _addedWhileSearching.value + rowId
    }

    /**
     * **Una riga nuova nasce già col valore del filtro**, come su Notion:
     * creata nella vista "solo PS5", è un gioco PS5. Senza, sparirebbe
     * nell'istante in cui nasce — il filtro non la fa passare — e
     * sembrerebbe che "Nuova pagina" non abbia fatto niente. Vale per le
     * selezioni (singola e multipla: basta il primo valore scelto) e per
     * le caselle filtrate solo sulle spuntate; per date e numeri non c'è
     * un valore solo da scegliere. `skipColumnId`: la proprietà a cui la
     * riga ha già avuto un valore (il gruppo della bacheca, il giorno del
     * calendario), che non va riscritta.
     */
    private suspend fun fillFilterValue(rowId: String, skipColumnId: String? = null) {
        val page = _page.value ?: return
        val columnId = page.filterColumnId ?: return
        if (columnId == skipColumnId) return
        val wanted = page.filterValue.orEmpty().split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
        if (wanted.isEmpty()) return
        val column = rawColumn(columnId) ?: return
        val value = when (column.type) {
            ColumnType.SELECT, ColumnType.MULTI_SELECT -> wanted.first()
            ColumnType.CHECKBOX -> if (wanted == listOf("true")) "true" else return
            else -> return
        }
        repository.setCellValue(rowId, columnId, value)
    }

    /**
     * `light`: per le proprietà in cima alla pagina di una riga (dal
     * 26/09/2026), a cui servono colonne, righe e celle e basta. Lì non si
     * seguono mai le copertine e il testo delle pagine per la galleria, che
     * cambierebbero a ogni tasto battuto proprio in quella pagina.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun load(pageId: String, light: Boolean = false) {
        if (currentPageId == pageId) return
        currentPageId = pageId

        // Seguita nel tempo, non letta una volta sola. Lo stesso
        // database può stare aperto due volte insieme — dentro una
        // pagina e a schermo intero — e sono due ViewModel diversi.
        // Chi resta indietro poi riscrive quello che ha in mano (le
        // modifiche qui sotto salvano la pagina intera), e la rinomina
        // fatta nell'altra schermata sparisce.
        viewModelScope.launch {
            pageRepository.observePage(pageId)
                .distinctUntilChanged()
                .collect { fromDb -> if (fromDb != null) _page.value = fromDb }
        }

        // Le pagine collegate dalle celle "Page link", seguite nel tempo.
        viewModelScope.launch {
            _tableState
                .map { state ->
                    val linkColumns = state.columns.filter { it.type == ColumnType.PAGE_LINK }.mapTo(HashSet()) { it.id }
                    if (linkColumns.isEmpty()) {
                        emptySet()
                    } else {
                        state.cellValues.filter { (key, value) -> key.second in linkColumns && value.isNotEmpty() }.values.toSet()
                    }
                }
                .distinctUntilChanged()
                .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(emptyList()) else pageRepository.observePages(ids.toList()) }
                .collect { pages -> _linkedPages.value = pages.associateBy { it.id } }
        }

        // Le viste in più di questo database (vedi `PageEntity.viewHostId`).
        if (!light) {
            viewModelScope.launch {
                pageRepository.observeViews(pageId).distinctUntilChanged().collect { _views.value = it }
            }
        }

        // L'origine di una vista collegata, seguita anche lei: rinominata o
        // bloccata dall'altra parte, qui si vede subito.
        viewModelScope.launch {
            _page.map { it?.sourceDatabaseId }
                .distinctUntilChanged()
                .flatMapLatest { sourceId ->
                    if (sourceId == null) flowOf(null) else pageRepository.observePage(sourceId).map { SourceRead(it) }
                }
                .collect { _source.value = it }
        }
        viewModelScope.launch {
            combine(_page, _source) { page, source ->
                if (page?.sourceDatabaseId == null) page else source?.page
            }.collect { _dataPage.value = it }
        }
        viewModelScope.launch {
            combine(_page, _source) { page, source ->
                page?.sourceDatabaseId != null && source != null &&
                    (source.page == null || source.page.trashedAt != null)
            }.collect { _sourceMissing.value = it }
        }

        // L'id del database che ha i dati. Aspetta che la pagina sia letta:
        // prima non si sa se è una vista collegata.
        val dataIds = _page
            .map { it?.let { page -> page.sourceDatabaseId ?: page.id } }
            .filterNotNull()
            .distinctUntilChanged()

        viewModelScope.launch {
            dataIds.flatMapLatest { repository.observeRowIcons(it) }.collect { _rowIcons.value = it }
        }

        // Copertine e testo delle pagine si seguono **solo mentre la
        // galleria li mostra**. Il testo soprattutto: la query guarda la
        // tabella dei blocchi, che cambia ad ogni tasto battuto in
        // qualunque pagina, e questo ViewModel resta vivo anche quando
        // il database è rimasto indietro nella pila di navigazione.
        // Seguirlo sempre vorrebbe dire rifare la query ad ogni lettera
        // scritta altrove, per una vista che magari non è nemmeno quella
        // scelta.
        val previewShown = combine(_page, _dataPage) { page, data ->
            if (light) null else page?.galleryPreviewShown(simple = data?.isSimpleDatabase == true)
        }.distinctUntilChanged()
        viewModelScope.launch {
            combine(dataIds, previewShown) { id, shown -> id to shown }
                .distinctUntilChanged()
                .flatMapLatest { (id, shown) ->
                    if (shown == GalleryCardPreview.PAGE_COVER) {
                        repository.observeRowCovers(id)
                    } else {
                        flowOf(emptyMap<String, RowCover>())
                    }
                }
                .collect { _rowCovers.value = it }
        }
        viewModelScope.launch {
            combine(dataIds, previewShown) { id, shown -> id to shown }
                .distinctUntilChanged()
                .flatMapLatest { (id, shown) ->
                    if (shown == GalleryCardPreview.PAGE_CONTENT) {
                        repository.observeRowContentPreviews(id, GALLERY_PREVIEW_MAX_LINES)
                    } else {
                        flowOf(emptyMap<String, List<RowPreviewLine>>())
                    }
                }
                .distinctUntilChanged()
                .collect { _rowPreviews.value = it }
        }
        viewModelScope.launch {
            combine(dataIds, previewShown) { id, shown -> id to shown }
                .distinctUntilChanged()
                .flatMapLatest { (id, shown) ->
                    if (shown != null && shown != GalleryCardPreview.NONE) {
                        repository.observeLockedRowIds(id)
                    } else {
                        flowOf(emptySet())
                    }
                }
                .collect { _lockedRows.value = it }
        }

        viewModelScope.launch {
            combine(
                dataIds.flatMapLatest { id ->
                    combine(
                        repository.getColumns(id),
                        repository.getRows(id),
                        repository.getAllCells(id)
                    ) { columns, rows, cells -> Triple(columns, rows, cells) }
                },
                // La pagina entra nel calcolo perché è lì che sta
                // l'ordinamento: senza, cambiarlo non farebbe
                // ricalcolare niente e la tabella resterebbe com'era.
                _page,
                combine(_searchQuery, _searchIndex, _addedWhileSearching, _frozenOrder, _heldVisible) { query, index, added, frozen, held ->
                    SearchAndOrder(query, index, added, frozen, held)
                }
            ) { (sourceColumns, rows, cells), page, (query, index, added, frozen, held) ->
                rawColumns = sourceColumns
                lastRowOrderIndex = rows.maxOfOrNull { it.orderIndex } ?: -1
                // In una vista collegata si vede quello che nasconde la
                // vista, non quello che nasconde l'origine.
                val columns = if (page?.sourceDatabaseId != null) {
                    val hidden = page.viewHiddenColumns()
                    sourceColumns.map { it.copy(hidden = it.id in hidden) }
                } else {
                    sourceColumns
                }
                val cellValues = cells.associate { (it.rowId to it.columnId) to it.value }
                // Prima si filtra e poi si ordina: ordinare righe che
                // stanno per sparire è lavoro buttato, e il risultato
                // sarebbe lo stesso. La ricerca è un filtro in più, sopra
                // a quello scelto nelle impostazioni.
                val filtered = filterRows(rows, columns, cellValues, page).let { shown ->
                    if (held == null) shown else {
                        val ids = shown.mapTo(HashSet()) { it.id }
                        rows.filter { it.id in ids || it.id in held }
                    }
                }
                val (kept, search) = searchRows(filtered, columns, cellValues, query, index, added)
                val sorted = sortRows(kept, columns, cellValues, page)
                DatabaseTableState(
                    columns = columns,
                    rows = frozen?.let { order -> keepOrder(sorted, order) } ?: sorted,
                    cellValues = cellValues,
                    search = search
                )
            }.collect { state ->
                _tableState.value = state
            }
        }
    }

    /** Apre la barra della ricerca e legge il testo delle pagine delle righe. */
    fun openSearch() {
        _searchOpen.value = true
        refreshSearchIndex()
    }

    /** Chiude la barra: la parola si cancella e tornano tutte le righe. */
    fun closeSearch() {
        _searchOpen.value = false
        _searchQuery.value = ""
        _searchIndex.value = emptyMap()
        _addedWhileSearching.value = emptySet()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Rilegge il testo delle pagine delle righe. Si fa aprendo la ricerca
     * e tornando sulla schermata con la ricerca aperta: dentro una pagina
     * trovata si può aver scritto, e la ricerca deve saperlo.
     */
    fun refreshSearchIndex() {
        val pageId = dataPageId ?: return
        if (!_searchOpen.value) return
        viewModelScope.launch { _searchIndex.value = repository.searchIndex(pageId) }
    }

    /**
     * Solo le righe che passano il filtro.
     *
     * Il filtro vale per **tutte le viste**, non solo per la tabella:
     * la domanda "fammi vedere solo queste pagine" non cambia senso
     * passando alla bacheca o al calendario, e un filtro che si spegne
     * cambiando vista sarebbe un filtro di cui non ci si può fidare.
     *
     * Se la proprietà su cui si filtrava è stata cancellata non si
     * filtra più niente: nascondere tutto per una proprietà che non
     * esiste più sembrerebbe che le pagine siano sparite.
     */
    private fun filterRows(
        rows: List<DatabaseRowEntity>,
        columns: List<DatabaseColumnEntity>,
        cellValues: Map<Pair<String, String>, String>,
        page: PageEntity?
    ): List<DatabaseRowEntity> {
        val columnId = page?.filterColumnId ?: return rows
        val wanted = page.filterValue.orEmpty()
        // Proprietà scelta ma nessun valore ancora spuntato: il filtro
        // c'è a metà e non deve svuotare la tabella mentre lo si sta
        // componendo.
        if (wanted.isBlank()) return rows
        val column = columns.find { it.id == columnId } ?: return rows
        return rows.filter { row ->
            matchesFilter(column, cellValues[row.id to column.id].orEmpty(), wanted)
        }
    }

    private fun matchesFilter(
        column: DatabaseColumnEntity,
        cellValue: String,
        filterValue: String
    ): Boolean {
        if (column.type == ColumnType.DATE) return matchesDateFilter(cellValue, filterValue)
        if (column.type == ColumnType.NUMBER) return matchesNumberFilter(cellValue, filterValue)
        if (column.type == ColumnType.CHECKBOX) {
            // Una casella mai toccata non ha nemmeno una cella nel
            // database: "" vuol dire "non spuntata", non "non lo so".
            // Confrontarla come testo la farebbe sparire da entrambi i
            // filtri, che è il modo peggiore di sbagliare.
            val checked = if (cellValue == "true") "true" else "false"
            return checked in filterValue.split(MULTI_VALUE_SEPARATOR)
        }

        val wanted = filterValue.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.toSet()
        if (wanted.isEmpty()) return true
        // Scegliendo più valori passa chi ne ha **almeno uno**, come il
        // "contains any of" di Notion: chiedere che li abbia tutti
        // renderebbe inutile sceglierne più d'uno su una proprietà a
        // selezione singola, dove nessuna riga potrebbe mai passare.
        return cellValue.split(MULTI_VALUE_SEPARATOR)
            .filter { it.isNotBlank() }
            .any { it in wanted }
    }

    /**
     * Una riga passa se il suo numero sta **fra** i due estremi,
     * compresi.
     *
     * I due estremi sono facoltativi uno per volta: solo il minimo vuol
     * dire "da x in su", solo il massimo "fino a y". Una cella vuota o
     * non numerica non passa mai — non è zero, è "non c'è un numero", e
     * farla entrare in "da 0 a 10" mostrerebbe righe che non hanno
     * niente a che fare con la domanda.
     */
    private fun matchesNumberFilter(cellValue: String, filterValue: String): Boolean {
        val parts = filterValue.split(MULTI_VALUE_SEPARATOR)
        val min = parts.getOrNull(0)?.toDoubleOrNull()
        val max = parts.getOrNull(1)?.toDoubleOrNull()
        if (min == null && max == null) return true
        val number = cellValue.toDoubleOrNull() ?: return false
        if (min != null && number < min) return false
        if (max != null && number > max) return false
        return true
    }

    /**
     * Una riga datata passa se i suoi giorni **toccano** quelli scelti,
     * non se coincidono: un impegno che va dal 3 al 7 deve comparire
     * filtrando il 5, altrimenti filtrare un giorno solo non mostrerebbe
     * quasi mai niente.
     *
     * Le date che si ripetono contano tutte le loro volte, non solo la
     * prima: una cosa che capita ogni domenica deve comparire filtrando
     * una domenica qualsiasi, che è tutto il senso di averla fatta
     * ripetere.
     */
    private fun matchesDateFilter(cellValue: String, filterValue: String): Boolean {
        val wanted = parseDateRange(filterValue) ?: return true
        val cell = parseDateRange(cellValue) ?: return false

        val from = wanted.start.toLocalDate()
        val to = wanted.end?.toLocalDate() ?: from
        val start = cell.start.toLocalDate()
        val end = cell.end?.toLocalDate() ?: start

        val rule = cell.recurrence
            ?: return !start.isAfter(to) && !end.isBefore(from)

        // La finestra si allarga all'indietro di quanto dura
        // l'impegno: una ripetizione cominciata prima del giorno
        // filtrato può arrivarci comunque.
        val duration = ChronoUnit.DAYS.between(start, end)
        return rule.occurrences(start, from.minusDays(duration), to).isNotEmpty()
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(com.gabriele.notionlocal.data.settings.AppSettings.zoneId).toLocalDate()

    /**
     * La proprietà su cui si sta filtrando, o null se non si filtra o
     * se quella scelta è stata cancellata.
     */
    fun filterColumn(): DatabaseColumnEntity? {
        val id = _page.value?.filterColumnId ?: return null
        if (_page.value?.filterValue.isNullOrBlank()) return null
        return _tableState.value.columns.find { it.id == id }
    }

    /** Il valore su cui si filtra, letto come lo scrive la schermata. */
    fun filterValue(): String = _page.value?.filterValue.orEmpty()

    /** Sceglie proprietà e valore del filtro. */
    fun setFilter(columnId: String?, value: String?) {
        releaseCheckboxHold()
        val current = _page.value ?: return
        val updated = current.copy(filterColumnId = columnId, filterValue = value)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Toglie il filtro: tornano tutte le pagine. */
    fun clearFilter() = setFilter(null, null)

    /**
     * Le righe nell'ordine chiesto dall'utente.
     *
     * Il confronto dipende dal tipo: i numeri si confrontano come
     * numeri (altrimenti "10" verrebbe prima di "9"), le date come
     * istanti, tutto il resto alfabeticamente e senza distinguere
     * maiuscole e minuscole.
     *
     * **Le celle vuote stanno in fondo in tutti e due i versi.** Non è
     * una svista: invertendo l'ordine ci si aspetta di vedere in cima
     * l'ultimo valore, non una fila di righe senza niente scritto.
     */
    /**
     * Le righe nell'ordine di `order`; quelle che lì non c'erano (nate nel
     * frattempo) vengono dopo, nell'ordine in cui sono arrivate.
     */
    private fun keepOrder(rows: List<DatabaseRowEntity>, order: List<String>): List<DatabaseRowEntity> {
        val position = order.withIndex().associate { it.value to it.index }
        return rows.sortedBy { position[it.id] ?: Int.MAX_VALUE }
    }

    private data class SearchAndOrder(
        val query: String,
        val index: Map<String, String>,
        val added: Set<String>,
        val frozen: List<String>?,
        val held: Set<String>?
    )

    private fun sortRows(
        rows: List<DatabaseRowEntity>,
        columns: List<DatabaseColumnEntity>,
        cellValues: Map<Pair<String, String>, String>,
        page: PageEntity?
    ): List<DatabaseRowEntity> {
        val rules = sortRulesOf(page, columns)
        if (rules.isEmpty()) return rows

        val keyed = rows.map { row ->
            row to rules.map { rule -> sortValueOf(row, rule.column, cellValues) }
        }
        // **Prima la prima regola, poi la seconda** (dal 05/10/2026): la
        // seconda conta solo fra le righe che per la prima sono pari. Fra
        // quelle pari anche per lei resta l'ordine di prima (`sortedWith`
        // non le rimescola).
        return keyed.sortedWith { a, b ->
            rules.indices.firstNotNullOfOrNull { i ->
                compareForSort(a.second[i], b.second[i], rules[i].descending).takeIf { it != 0 }
            } ?: 0
        }.map { it.first }
    }

    /** Una regola di ordinamento: la proprietà (null = il nome della riga) e il verso. */
    private class SortRule(val column: DatabaseColumnEntity?, val descending: Boolean)

    /**
     * Le regole che valgono, nell'ordine: la prima e, se c'è, la seconda.
     * Una regola su una proprietà che non c'è più non conta; senza la
     * prima non conta nemmeno la seconda, come prima di lei.
     */
    private fun sortRulesOf(page: PageEntity?, columns: List<DatabaseColumnEntity>): List<SortRule> {
        fun ruleOf(id: String?, descending: Boolean): SortRule? = when (id) {
            null -> null
            SORT_BY_NAME -> SortRule(null, descending)
            else -> columns.find { it.id == id }?.let { SortRule(it, descending) }
        }
        val first = ruleOf(page?.sortColumnId, page?.sortDescending == true) ?: return emptyList()
        val then = page?.thenSortColumnId
            ?.takeIf { it != page.sortColumnId }
            ?.let { ruleOf(it, page.thenSortDescending) }
        return listOfNotNull(first, then)
    }

    /**
     * Due valori della stessa regola. **Le celle vuote stanno in fondo in
     * tutti e due i versi**: invertendo l'ordine ci si aspetta di vedere in
     * cima l'ultimo valore, non una fila di righe senza niente scritto.
     */
    private fun compareForSort(first: SortValue, second: SortValue, descending: Boolean): Int = when {
        first.isEmpty && second.isEmpty -> 0
        first.isEmpty -> 1
        second.isEmpty -> -1
        else -> {
            val cmp = compareSortValues(first, second)
            if (descending) -cmp else cmp
        }
    }

    private fun sortValueOf(
        row: DatabaseRowEntity,
        column: DatabaseColumnEntity?,
        cellValues: Map<Pair<String, String>, String>
    ): SortValue {
        if (column == null) return SortValue(null, row.title)
        val raw = cellValues[row.id to column.id].orEmpty()
        return when (column.type) {
            ColumnType.NUMBER -> SortValue(raw.toDoubleOrNull(), raw)
            // Il primo campo del valore di una data è l'istante di
            // inizio in millisecondi (il formato completo sta in
            // `parseDateRange`, nella schermata).
            ColumnType.DATE -> SortValue(
                raw.substringBefore(MULTI_VALUE_SEPARATOR).toLongOrNull()?.toDouble(),
                raw
            )
            ColumnType.CHECKBOX -> SortValue(if (raw == "true") 1.0 else 0.0, raw)
            // Per quanto dura, non per quando comincia; senza la fine è vuota.
            ColumnType.DURATION -> durationMillisOfCell(raw)?.let { SortValue(it.toDouble(), raw) } ?: SortValue(null, "")
            // Per il titolo della pagina collegata, non per il suo id.
            ColumnType.PAGE_LINK -> SortValue(null, if (raw.isEmpty()) "" else _linkedPages.value[raw]?.title ?: raw)
            // Per il testo che c'è scritto sopra, non per il colore.
            ColumnType.COLOR -> SortValue(null, parseColorCell(raw).text)
            ColumnType.CREATED_TIME -> SortValue(row.createdAt.toDouble(), "x")
            ColumnType.LAST_EDITED_TIME ->
                SortValue((row.updatedAt ?: row.createdAt).toDouble(), "x")
            else -> SortValue(null, raw)
        }
    }

    private fun compareSortValues(a: SortValue, b: SortValue): Int =
        if (a.number != null && b.number != null) {
            a.number.compareTo(b.number)
        } else {
            String.CASE_INSENSITIVE_ORDER.compare(a.text, b.text)
        }

    /**
     * Mette o toglie questo database dai preferiti.
     *
     * Scrive **solo quella colonna** invece di salvare la pagina intera:
     * lo stesso database può stare aperto in due schermate insieme —
     * dentro una pagina e a schermo intero — e riscriverlo tutto da qui
     * rimetterebbe indietro quello che è cambiato nell'altra.
     */
    fun setFavorite(favorite: Boolean) {
        val current = _page.value ?: return
        _page.value = current.copy(isFavorite = favorite)
        viewModelScope.launch { pageRepository.setFavorite(current.id, favorite) }
    }

    // --- Le voci del menu "..." ---
    //
    // Le stesse di `PageEditorViewModel`, perché fanno le stesse cose a
    // una pagina: il lavoro vero sta tutto nel repository, qui c'è solo
    // l'aggiornamento immediato di quello che la UI ha in mano, per non
    // far aspettare l'interruttore il giro dal database.

    fun toggleLocked() {
        val current = _page.value ?: return
        val wanted = !current.isLocked
        _page.value = current.copy(isLocked = wanted)
        viewModelScope.launch { pageRepository.setLocked(current.id, wanted) }
    }

    /**
     * Blocca o sblocca il database con la password. Bloccato, dove lo si
     * vedeva dentro una pagina diventa un collegamento, come con "Turn into
     * page" (vedi `PageRepository.setProtected`), e si richiude subito.
     */
    fun setPasswordLocked(wanted: Boolean) {
        val current = _page.value ?: return
        _page.value = current.copy(isProtected = wanted)
        if (wanted) AppLock.lock(current.id)
        viewModelScope.launch { pageRepository.setProtected(current.id, wanted) }
    }

    fun toggleViewLocked() {
        val current = _page.value ?: return
        val wanted = !current.isViewLocked
        _page.value = current.copy(isViewLocked = wanted)
        viewModelScope.launch { pageRepository.setViewLocked(current.id, wanted) }
    }

    fun duplicatePage(
        target: PageRepository.DuplicateTarget,
        titleSuffix: String,
        imageStore: PageImageStore,
        onDone: (String) -> Unit
    ) {
        val current = _page.value ?: return
        viewModelScope.launch {
            pageRepository.duplicatePage(current.id, target, titleSuffix) { imageStore.copy(it) }?.let { copyId ->
                // L'Annulla va alla prossima pagina che si apre: vedi `PendingPageUndo`.
                PendingPageUndo.offer(pageRepository.describeCopy(copyId))
                onDone(copyId)
            }
        }
    }

    /** Dove questo database non si può spostare: lui stesso e le pagine delle sue righe, giù fino in fondo. */
    fun loadMoveExclusions(onReady: (PageRepository.MoveExclusions) -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch { onReady(pageRepository.moveExclusions(current.id)) }
    }

    /**
     * "Move to" dai tre puntini: il database arriva in fondo alla pagina
     * scelta come collegamento a pagina, o aperto dentro di lei con "Move as
     * inline" (`asInline`, vedi `PageRepository.movePageTo`). `onDone` solo
     * se lo spostamento c'è stato.
     */
    fun movePageTo(destination: PageRepository.PageTreeNode, asInline: Boolean = false, onDone: () -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch {
            pageRepository.movePageTo(current.id, destination, asInline)?.let { change ->
                PendingPageUndo.offer(change)
                onDone()
            }
        }
    }

    /**
     * **"Move to database"** dai tre puntini (dal 05/10/2026): il database
     * finisce in una pagina nuova del database scelto, aperto dentro di lei
     * (vedi `PageRepository.moveIntoDatabase`). `onDone` solo se lo
     * spostamento c'è stato.
     */
    fun movePageToDatabase(destination: PageRepository.PageTreeNode, rowTitle: String, onDone: () -> Unit) {
        val current = _page.value ?: return
        val databaseId = destination.pageId ?: return
        viewModelScope.launch {
            pageRepository.moveIntoDatabase(current.id, databaseId, rowTitle)?.let { change ->
                PendingPageUndo.offer(change)
                onDone()
            }
        }
    }

    /**
     * L'icona del database: un'immagine, un'emoji (dal 07/10/2026, il
     * selettore delle emoji) o **nessuna icona**.
     *
     * Per un database "nessuna icona" vuol dire proprio niente accanto
     * al nome, non il 📄 delle pagine: l'emoji di riserva che le pagine
     * hanno scritto nel campo `icon` qui non conta, altrimenti ogni
     * database avrebbe avuto un foglietto davanti senza che nessuno
     * l'avesse chiesto. Conta un'emoji scelta davvero (`chosenEmoji`).
     *
     * Restituisce il nome del file che è stato sostituito, perché chi
     * chiama possa cancellarlo: lasciarlo lì riempirebbe la cartella di
     * immagini che nessuno può più raggiungere. Stessa regola delle
     * pagine (`PageEditorViewModel.setIcon`).
     */
    fun setIcon(choice: IconChoice): String? {
        // In una vista collegata l'icona è quella del database di origine:
        // è quella che la vista mostra, e cambiarla vuol dire cambiarla là.
        if (_page.value?.sourceDatabaseId != null) {
            val source = _dataPage.value ?: return null
            viewModelScope.launch { pageRepository.setIcon(source.id, choice) }
            return replacedImage(source.iconImage, choice)
        }
        val current = _page.value ?: return null
        _page.value = current.copy(icon = choice.iconField, iconImage = choice.imageField, iconSymbol = choice.symbolField)
        viewModelScope.launch { pageRepository.setIcon(current.id, choice) }
        return replacedImage(current.iconImage, choice)
    }

    /**
     * L'icona della pagina di una riga: un'emoji, un'immagine o niente.
     *
     * La riga potrebbe non avere ancora una pagina — nasce la prima volta
     * che la si apre — e allora la si crea qui: l'icona sta nella pagina,
     * non nella riga. Si vede subito, prima ancora che il database la
     * confermi. Restituisce il file dell'icona di prima, che chi chiama
     * toglie dalla cartella (come `setIcon`).
     */
    fun setRowIcon(row: DatabaseRowEntity, choice: IconChoice): String? {
        val previous = _rowIcons.value[row.id]?.image
        _rowIcons.value = when (choice) {
            IconChoice.None -> _rowIcons.value - row.id
            else -> _rowIcons.value + (
                row.id to RowIconRef(emoji = chosenEmoji(choice.iconField), image = choice.imageField, symbol = choice.symbolField)
            )
        }
        viewModelScope.launch {
            val pageId = pageRepository.ensureRowPage(row.id) ?: return@launch
            pageRepository.setIcon(pageId, choice)
        }
        return replacedImage(previous, choice)
    }

    /**
     * "Reposition cover" dalla galleria: l'inquadratura della copertina
     * **nella scheda** di questa riga, indipendente da quella in cima alla
     * pagina. Una scheda con la copertina ha per forza la sua pagina.
     */
    fun setRowCardCover(row: DatabaseRowEntity, scale: Float, offsetX: Float, offsetY: Float) {
        val pageId = row.linkedPageId ?: return
        viewModelScope.launch { pageRepository.setCardCoverTransform(pageId, scale, offsetX, offsetY) }
    }

    /** Se da qualche parte questo database è mostrato come pagina: solo allora ha senso "Turn into database". */
    suspend fun isShownAsPage(): Boolean {
        val current = _page.value ?: return false
        return pageRepository.isShownAsPage(current.id)
    }

    /**
     * "Turn into database": il database torna a vedersi dentro la pagina
     * che lo richiama. `onDone` serve a tornare lì, perché è lì che lo si
     * vuole rivedere — restare a schermo intero non mostrerebbe niente di
     * cambiato.
     */
    fun turnIntoDatabase(onDone: () -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch {
            pageRepository.turnPageLinksIntoDatabase(current.id)
            onDone()
        }
    }

    fun moveToTrash(onDone: () -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch {
            pageRepository.moveToTrash(current.id)?.let { PendingPageUndo.offer(it) }
            onDone()
        }
    }

    /** Dalla barra del cestino: il database torna in fondo al menu principale. */
    fun restoreFromTrash() {
        val current = _page.value ?: return
        viewModelScope.launch { pageRepository.restoreFromTrash(current.id) }
    }

    /** Dalla barra del cestino: il database se ne va per sempre, righe e loro pagine comprese. */
    fun deletePermanently(imageStore: PageImageStore, onDone: () -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch {
            pageRepository.deletePermanently(current.id).forEach { imageStore.delete(it) }
            onDone()
        }
    }

    /** Mostra o nasconde il titolo quando il database è dentro una pagina. */
    fun setShowEmbeddedTitle(show: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(showEmbeddedTitle = show)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * Sceglie la proprietà su cui ordinare, o null per togliere la regola.
     *
     * Con la seconda regola (dal 05/10/2026): togliendo la prima, **la
     * seconda sale al suo posto**, col suo verso; senza nessuna delle due si
     * torna all'ordine di creazione. Scegliendo per la prima la stessa
     * proprietà della seconda, la seconda se ne va: ordinerebbe due volte
     * per la stessa cosa.
     */
    fun setSort(columnId: String?, descending: Boolean) {
        releaseCheckboxHold()
        val current = _page.value ?: return
        val updated = when {
            columnId == null -> current.copy(
                sortColumnId = current.thenSortColumnId,
                sortDescending = current.thenSortColumnId != null && current.thenSortDescending,
                thenSortColumnId = null,
                thenSortDescending = false
            )
            columnId == current.thenSortColumnId -> current.copy(
                sortColumnId = columnId,
                sortDescending = descending,
                thenSortColumnId = null,
                thenSortDescending = false
            )
            else -> current.copy(sortColumnId = columnId, sortDescending = descending)
        }
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * **La seconda regola** (dal 05/10/2026): fra le righe che per la prima
     * sono pari, l'ordine lo dà questa. null la toglie. Senza una prima
     * regola, o sulla sua stessa proprietà, non si mette.
     */
    fun setThenSort(columnId: String?, descending: Boolean) {
        releaseCheckboxHold()
        val current = _page.value ?: return
        if (columnId != null && (current.sortColumnId == null || columnId == current.sortColumnId)) return
        val updated = current.copy(
            thenSortColumnId = columnId,
            thenSortDescending = columnId != null && descending
        )
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * La proprietà che fa da colonna nella vista a bacheca. Se non è
     * stata scelta, o se quella scelta è sparita o non si può più usare
     * per raggruppare, si ripiega sulla prima a selezione singola: meglio
     * una bacheca ragionevole che una schermata vuota.
     *
     * **Dal 14/10/2026 si sceglie fra quasi tutte le proprietà**, come per
     * la tabella ([canGroup]): prima solo SELECT, e in Food all'utente veniva
     * offerto solo "Supermercato". Con la selezione multipla una
     * scheda sta nella colonna di ognuno dei suoi tag; nella bacheca le
     * schede non si trascinano, quindi non c'è da decidere cosa vorrebbe
     * dire spostarla.
     */
    fun boardGroupColumn(): DatabaseColumnEntity? {
        val columns = _tableState.value.columns
        val chosen = _page.value?.boardGroupColumnId?.let { id ->
            columns.find { it.id == id }
        }
        return chosen?.takeIf { it.type.canGroup }
            ?: columns.firstOrNull { it.type == ColumnType.SELECT }
    }

    /**
     * **Le fasce della bacheca** (dal 14/10/2026), o null se non ce ne sono:
     * valgono solo quando la bacheca ha le sue colonne, e mai sulla stessa
     * proprietà delle colonne.
     */
    fun boardSubGroupColumn(): DatabaseColumnEntity? {
        val group = boardGroupColumn() ?: return null
        val id = _page.value?.boardSubGroupColumnId?.takeIf { it != group.id } ?: return null
        return _tableState.value.columns.find { it.id == id }?.takeIf { it.type.canGroup }
    }

    /** Sceglie la proprietà delle fasce della bacheca, o null per toglierle. */
    fun setBoardSubGroupColumn(columnId: String?) {
        val current = _page.value ?: return
        val updated = current.copy(boardSubGroupColumnId = columnId)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Se le fasce della bacheca senza nemmeno una pagina si nascondono. */
    fun setBoardHideEmptySubGroups(hide: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(boardHideEmptySubGroups = hide)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * La proprietà in base a cui la tabella raggruppa le righe, o null
     * se non si raggruppa.
     *
     * Qui **non c'è ripiego**, a differenza della bacheca: senza
     * raggruppamento la tabella è la tabella di sempre, e sceglierne
     * uno al posto dell'utente vorrebbe dire rimescolargli le righe da
     * solo. Se la proprietà scelta viene cancellata, il raggruppamento
     * semplicemente non c'è più.
     */
    fun tableGroupColumn(): DatabaseColumnEntity? {
        val id = _page.value?.tableGroupColumnId ?: return null
        return _tableState.value.columns.find { it.id == id }
    }

    /**
     * Sceglie la proprietà su cui raggruppare, o null per togliere i gruppi.
     * Senza gruppi i sottogruppi se ne vanno con loro; e se il gruppo diventa
     * la proprietà dei sottogruppi, quelli non avrebbero più niente da dividere.
     */
    fun setTableGroupColumn(columnId: String?) {
        val current = _page.value ?: return
        val sub = current.tableSubGroupColumnId?.takeIf { columnId != null && it != columnId }
        val updated = current.copy(tableGroupColumnId = columnId, tableSubGroupColumnId = sub)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * **La proprietà dei sottogruppi** (dal 29/09/2026), o null se non ce ne
     * sono: vale solo con un raggruppamento, e mai uguale al gruppo.
     */
    fun tableSubGroupColumn(): DatabaseColumnEntity? {
        val group = tableGroupColumn() ?: return null
        val id = _page.value?.tableSubGroupColumnId?.takeIf { it != group.id } ?: return null
        return _tableState.value.columns.find { it.id == id }
    }

    /** Sceglie la proprietà dei sottogruppi, o null per toglierli. */
    fun setTableSubGroupColumn(columnId: String?) {
        val current = _page.value ?: return
        val updated = current.copy(tableSubGroupColumnId = columnId)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * **Il testo lungo nelle celle** (dal 30/09/2026) di questa vista: tre
     * puntini, colonna allargata o a capo (vedi `CellOverflow`).
     */
    fun setCellOverflow(mode: CellOverflow) {
        val current = _page.value ?: return
        val updated = current.copy(cellOverflow = mode.code)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** **"Freeze first column"** della tabella (dal 08/10/2026), di questa vista. */
    fun setFreezeFirstColumn(freeze: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(freezeFirstColumn = freeze)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** **"Freeze first row"**: la riga dei titoli resta in cima, di questa vista. */
    fun setFreezeFirstRow(freeze: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(freezeFirstRow = freeze)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** **"Color columns"** della bacheca (dal 30/09/2026): ogni colonna del colore del suo tag. */
    fun setColorBoardCards(color: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(colorBoardCards = color)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Se i gruppi senza nemmeno una pagina si nascondono. */
    fun setHideEmptyGroups(hide: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(hideEmptyGroups = hide)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Se i sottogruppi senza nemmeno una pagina si nascondono, a sé dai gruppi. */
    fun setHideEmptySubGroups(hide: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(hideEmptySubGroups = hide)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * La proprietà data che colloca le righe nel calendario. Stessa
     * logica di ripiego della bacheca: se non è stata scelta, o se
     * quella scelta è sparita o non è più una data, si usa la prima
     * adatta.
     */
    fun calendarDateColumn(): DatabaseColumnEntity? {
        val columns = _tableState.value.columns
        val chosen = _page.value?.calendarDateColumnId?.let { id ->
            columns.find { it.id == id }
        }
        return chosen?.takeIf { it.type == ColumnType.DATE }
            ?: columns.firstOrNull { it.type == ColumnType.DATE }
    }

    fun setTimelineZoom(zoom: TimelineZoom) {
        val current = _page.value ?: return
        val updated = current.copy(timelineZoom = zoom)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    fun setCalendarMode(mode: CalendarMode) {
        val current = _page.value ?: return
        val updated = current.copy(calendarMode = mode)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    fun setCalendarDateColumn(columnId: String) {
        val current = _page.value ?: return
        val updated = current.copy(calendarDateColumnId = columnId)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Aggiunge una riga già datata: creandola su un giorno ci si aspetta che ci resti. */
    /**
     * Mette una pagina su un giorno del calendario, **creando al volo
     * la proprietà data se non c'è**.
     *
     * Un database appena creato non ha una proprietà data, e prima il
     * calendario si rifiutava di disegnarsi finché non la si creava a
     * mano. Ma chi tocca un giorno ha già detto tutto quello che
     * serve: vuole una pagina, in quel giorno. La proprietà è
     * l'impalcatura che serve a noi, non una decisione da chiedergli.
     */
    fun addRowOnDateCreatingColumn(dateMillis: Long) {
        val pageId = dataPageId ?: return
        val existing = calendarDateColumn()
        if (existing != null) {
            addRowOnDate(existing.id, dateMillis)
            return
        }
        // Stesso criterio di addColumn per l'indice: uno più del
        // massimo, non il numero di colonne.
        val nextIndex = nextColumnIndex()
        val column = DatabaseColumnEntity(
            pageId = pageId,
            name = "Date",
            type = ColumnType.DATE,
            orderIndex = nextIndex
        )
        viewModelScope.launch {
            repository.addColumn(column)
            val row = DatabaseRowEntity(pageId = pageId, orderIndex = nextRowIndex())
            keepVisibleWhileSearching(row.id)
            repository.addRow(row)
            repository.setCellValue(row.id, column.id, dateMillis.toString())
        }
        // La vista deve usare proprio questa, non "la prima data che
        // trova": se domani se ne aggiunge un'altra, il calendario non
        // deve cambiare colonna sotto i piedi.
        setCalendarDateColumn(column.id)
    }

    fun addRowOnDate(columnId: String, dateMillis: Long) {
        val pageId = dataPageId ?: return
        viewModelScope.launch {
            val row = DatabaseRowEntity(pageId = pageId, orderIndex = nextRowIndex())
            keepVisibleWhileSearching(row.id)
            repository.addRow(row)
            repository.setCellValue(row.id, columnId, dateMillis.toString())
            fillFilterValue(row.id, skipColumnId = columnId)
        }
    }

    /**
     * **Salva un evento del calendario** (dal 30/09/2026, il time-blocking
     * come Google Calendar): la finestra rapida o "More options".
     *
     * Nuovo (`rowId` null): una riga col nome, la data (`dateValue`, già nel
     * formato della cella) e il colore; la proprietà data si crea al volo se
     * non c'è, come toccando un giorno. Una riga che c'è già ("Edit event"):
     * si scrive solo quello che è cambiato, così una riga lasciata com'era non
     * risulta modificata adesso.
     */
    fun saveEvent(rowId: String?, title: String, dateValue: String, color: String?) {
        val pageId = dataPageId ?: return
        val existingColumn = calendarDateColumn()
        val column = existingColumn ?: DatabaseColumnEntity(
            pageId = pageId,
            name = "Date",
            type = ColumnType.DATE,
            orderIndex = nextColumnIndex()
        )
        val state = _tableState.value
        viewModelScope.launch {
            if (existingColumn == null) repository.addColumn(column)
            if (rowId != null) {
                val row = state.rows.find { it.id == rowId }
                val oldValue = state.cellValues[rowId to column.id].orEmpty()
                if (row == null || row.title != title) repository.setRowTitle(rowId, title)
                if (oldValue != dateValue) repository.setCellValue(rowId, column.id, dateValue)
                if (row == null || row.eventColor != color) repository.setRowEventColor(rowId, color)
            } else {
                val row = DatabaseRowEntity(pageId = pageId, title = title, orderIndex = nextRowIndex(), eventColor = color)
                keepVisibleWhileSearching(row.id)
                repository.addRow(row)
                repository.setCellValue(row.id, column.id, dateValue)
                fillFilterValue(row.id, skipColumnId = column.id)
            }
        }
        // Come toccando un giorno: la vista usa proprio la proprietà appena nata.
        if (existingColumn == null) setCalendarDateColumn(column.id)
    }

    /**
     * Sceglie la proprietà delle colonne della bacheca. Se è quella delle
     * fasce, le fasce se ne vanno: non avrebbero più niente da dividere.
     */
    fun setBoardGroupColumn(columnId: String) {
        val current = _page.value ?: return
        val sub = current.boardSubGroupColumnId?.takeIf { it != columnId }
        val updated = current.copy(boardGroupColumnId = columnId, boardSubGroupColumnId = sub)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    fun setLayout(layout: DatabaseLayout) {
        val current = _page.value ?: return
        val updated = current.copy(databaseLayout = layout)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Le impostazioni del grafico di questa vista (la vista Chart, dal 28/09/2026). */
    fun setChartSettings(settings: ChartSettings) {
        val current = _page.value ?: return
        val updated = current.copy(chartSettings = encodeChartSettings(settings))
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Cosa mostrano le schede della galleria: niente, la copertina o il testo. */
    fun setGalleryCardPreview(preview: GalleryCardPreview) {
        val current = _page.value ?: return
        val updated = current.copy(galleryCardPreview = preview)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Quanto sono grandi le schede della galleria. */
    fun setGalleryCardSize(size: GalleryCardSize) {
        val current = _page.value ?: return
        val updated = current.copy(galleryCardSize = size)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    fun updateTitle(newTitle: String) {
        val current = _page.value ?: return
        val updated = current.copy(title = newTitle)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    fun addColumn(name: String, type: ColumnType, optionsJson: String = "[]") {
        val pageId = dataPageId ?: return
        val nextIndex = nextColumnIndex()
        viewModelScope.launch {
            repository.addColumn(
                DatabaseColumnEntity(
                    pageId = pageId,
                    name = name,
                    type = type,
                    optionsJson = optionsJson,
                    orderIndex = nextIndex
                )
            )
        }
    }

    /**
     * Il numero d'ordine di una colonna nuova. Uno più del massimo, non il
     * numero di colonne: dopo aver eliminato una colonna in mezzo, contarle
     * darebbe un indice già occupato, e due colonne con lo stesso indice non
     * si possono riordinare (lo scambio non cambierebbe nulla).
     *
     * Conta anche Name (dal 29/09/2026): spostata in fondo, la proprietà
     * nuova nasce lo stesso all'estrema destra, accanto al "+" che si è
     * toccato.
     */
    private fun nextColumnIndex(): Int =
        maxOf(
            _tableState.value.columns.maxOfOrNull { it.orderIndex } ?: -1,
            _dataPage.value?.nameColumnOrder ?: -1
        ) + 1

    fun deleteColumn(column: DatabaseColumnEntity) {
        viewModelScope.launch { repository.deleteColumn(column) }
    }

    /**
     * Nasconde o rimostra una colonna nella tabella.
     *
     * È l'alternativa mite a cancellarla: la colonna sparisce dalla
     * griglia ma le celle restano scritte, quindi rimostrandola si
     * ritrova tutto. Su un telefono conta parecchio — la tabella scorre
     * in orizzontale, e togliere di mezzo due colonne che non servono
     * ora è l'unico modo per vedere quelle che servono senza trascinare.
     */
    fun setColumnHidden(columnId: String, hidden: Boolean) {
        val page = _page.value
        if (page?.sourceDatabaseId != null) {
            // Vista collegata: nascosta **qui**, non nel database.
            val now = page.viewHiddenColumns()
            setViewHidden(if (hidden) now + columnId else now - columnId)
            return
        }
        val column = rawColumn(columnId) ?: return
        if (column.hidden == hidden) return
        viewModelScope.launch { repository.updateColumn(column.copy(hidden = hidden)) }
    }

    /** Le proprietà nascoste di una vista collegata: la vista si ridisegna subito. */
    private fun setViewHidden(ids: Set<String>) {
        val page = _page.value ?: return
        _page.value = page.copy(viewHiddenColumnIds = ids.joinToString(",").ifEmpty { null })
        viewModelScope.launch { pageRepository.setViewHiddenColumns(page.id, ids) }
    }

    /** Nasconde o rimostra tutte le colonne in un colpo solo. */
    fun setAllColumnsHidden(hidden: Boolean) {
        if (_page.value?.sourceDatabaseId != null) {
            setViewHidden(if (hidden) rawColumns.map { it.id }.toSet() else emptySet())
            return
        }
        val columns = rawColumns.filter { it.hidden != hidden }
        if (columns.isEmpty()) return
        viewModelScope.launch {
            columns.forEach { repository.updateColumn(it.copy(hidden = hidden)) }
        }
    }

    /**
     * Rinomina una colonna e/o ne cambia il tipo (e le opzioni, se è una
     * colonna a selezione).
     *
     * I valori già scritti nelle celle **non** vengono toccati: sono
     * salvati come testo, quindi cambiare tipo li reinterpreta invece di
     * cancellarli. Chi passa da Testo a Numero si ritrova le celle non
     * numeriche così com'erano, e tornando indietro le ritrova intatte —
     * meglio che perderle per un tipo sbagliato scelto per errore.
     */
    fun updateColumn(
        columnId: String,
        name: String,
        type: ColumnType,
        optionsJson: String,
        /** Solo per una colonna Number: formato, cifre, barra o anello (vedi `NumberSettings`). */
        number: NumberSettings? = null,
        /** Solo per una colonna Duration: fino a quale unità si scrive (vedi `DurationUnit`). */
        duration: DurationUnit? = null
    ) {
        val column = rawColumn(columnId) ?: return
        viewModelScope.launch {
            val renamed = column.copy(name = name, type = type, optionsJson = optionsJson)
            val withNumber = number?.let { renamed.withNumberSettings(it) } ?: renamed
            // Le colonne Date non hanno più un formato loro (dal 16/10/2026):
            // quello che restava della scelta di prima se ne va.
            val withDate = if (type == ColumnType.DATE) withNumber.copy(dateFormat = null, timeFormat = null) else withNumber
            // Fino ai minuti è com'è di suo: non serve scriverlo.
            repository.updateColumn(
                duration?.let { withDate.copy(durationFormat = it.takeIf { unit -> unit != DurationUnit.MINUTES }?.name) } ?: withDate
            )
        }
    }

    /**
     * **Il calcolo in fondo a una colonna** di questa vista ("Calculate"), o
     * null per toglierlo. `key` è l'id della colonna, o `NAME_CALCULATION_KEY`.
     */
    fun setCalculation(key: String, calculation: Calculation?) {
        val current = _page.value ?: return
        val calcs = parseViewCalculations(current.viewCalculations).toMutableMap()
        if (calculation == null) calcs.remove(key) else calcs[key] = calculation
        val alignments = parseCalculationAlignments(current.viewCalculations)
        val subRows = parseCalculationSubRows(current.viewCalculations)
        val updated = current.copy(viewCalculations = encodeViewCalculations(calcs, alignments, subRows))
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * "Include sub rows" nella finestra di "Calculate" (dal 29/09/2026): se il
     * calcolo di questa colonna conta anche le sotto-righe. Di suo no.
     */
    fun setCalculationSubRows(key: String, include: Boolean) {
        val current = _page.value ?: return
        val calcs = parseViewCalculations(current.viewCalculations)
        val alignments = parseCalculationAlignments(current.viewCalculations)
        val subRows = parseCalculationSubRows(current.viewCalculations).let { if (include) it + key else it - key }
        val updated = current.copy(viewCalculations = encodeViewCalculations(calcs, alignments, subRows))
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /** Dove sta il risultato del calcolo di una colonna (dal 26/09/2026): tenendo premuto il calcolo. */
    /**
     * **La larghezza di una colonna** (dal 29/09/2026), trascinando il suo
     * segnalino: vale per questa vista. Allargarla o stringerla a mano
     * spegne "Fit to content" su quella colonna — si è scelta una misura.
     */
    fun setColumnWidth(key: String, widthDp: Int) {
        val current = _page.value ?: return
        // Tornata alla larghezza di sempre, non c'è niente da ricordare.
        val type = rawColumn(key)?.type
        val default = defaultColumnWidth(key, type)
        val width = snapColumnWidth(key, widthDp, default, minColumnWidth(key, type))
        val widths = parseColumnWidths(current.columnWidths).let {
            if (width == default) it - key else it + (key to width)
        }
        val fit = parseFitColumns(current.fitColumns) - key
        val updated = current.copy(columnWidths = encodeColumnWidths(widths), fitColumns = encodeFitColumns(fit))
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * "Fit to content" acceso o spento su una colonna: acceso, la colonna è
     * larga quanto quello che c'è scritto; spento, torna la sua larghezza.
     */
    fun setFitToContent(key: String, fit: Boolean) {
        val current = _page.value ?: return
        val keys = parseFitColumns(current.fitColumns).let { if (fit) it + key else it - key }
        val updated = current.copy(fitColumns = encodeFitColumns(keys))
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    fun setCalculationAlignment(key: String, alignment: CellAlignment) {
        val current = _page.value ?: return
        val calcs = parseViewCalculations(current.viewCalculations)
        val alignments = parseCalculationAlignments(current.viewCalculations).toMutableMap()
        // A destra è come stanno di suo: non serve scriverlo.
        if (alignment == CellAlignment.RIGHT) alignments.remove(key) else alignments[key] = alignment
        val subRows = parseCalculationSubRows(current.viewCalculations)
        val updated = current.copy(viewCalculations = encodeViewCalculations(calcs, alignments, subRows))
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * **Dove sta il contenuto delle celle di una colonna** (dal 26/09/2026): a
     * sinistra, al centro o a destra. `centerContent` segue, così le versioni
     * di prima della 37 leggono ancora il centro.
     */
    fun setColumnAlignment(columnId: String, alignment: CellAlignment) {
        val column = rawColumn(columnId) ?: return
        viewModelScope.launch {
            repository.updateColumn(
                column.copy(
                    contentAlignment = alignment.name,
                    centerContent = alignment == CellAlignment.CENTER
                )
            )
        }
    }

    /**
     * Aggiunge un'opzione a una colonna a selezione, se non c'è già.
     *
     * Serve per poterle creare direttamente dalla cella, come su
     * Notion: altrimenti una colonna a selezione appena creata resta
     * inutilizzabile finché non si torna nelle sue impostazioni, e la
     * cella mostra solo un trattino senza spiegare perché.
     */
    fun addSelectOption(columnId: String, label: String) {
        val column = rawColumn(columnId) ?: return
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return

        val existing = try {
            json.decodeFromString<List<SelectOption>>(column.optionsJson)
        } catch (e: Exception) {
            emptyList()
        }
        if (existing.any { it.label.equals(trimmed, ignoreCase = true) }) return

        // Colori assegnati a giro, così due tag vicini non sono mai
        // dello stesso colore.
        val color = TAG_COLORS[existing.size % TAG_COLORS.size].hex
        val updated = existing + SelectOption(label = trimmed, color = color)
        viewModelScope.launch {
            repository.updateColumn(column.copy(optionsJson = json.encodeToString(updated)))
        }
    }

    /** Cambia il colore di un tag. Le celle non ne sono toccate: il valore resta l'etichetta. */
    fun setSelectOptionColor(columnId: String, label: String, colorHex: String) {
        updateOptions(columnId) { options ->
            options.map { if (it.label == label) it.copy(color = colorHex) else it }
        }
    }

    /**
     * Rinomina un tag **e aggiorna tutte le righe che lo usavano**.
     *
     * Le celle contengono l'etichetta, non un riferimento all'opzione:
     * cambiarla senza toccarle lascerebbe le righe con un valore che non
     * corrisponde più a nessun tag, cioè invisibili nella bacheca e
     * impossibili da ritrovare.
     */
    fun renameSelectOption(columnId: String, oldLabel: String, newLabel: String) {
        val trimmed = newLabel.trim()
        if (trimmed.isEmpty() || trimmed == oldLabel) return
        updateOptions(columnId) { options ->
            options.map { if (it.label == oldLabel) it.copy(label = trimmed) else it }
        }
        rewriteCellsOfColumn(columnId) { labels -> labels.map { if (it == oldLabel) trimmed else it } }
    }

    /** Elimina un tag e lo toglie dalle righe che lo usavano, per lo stesso motivo. */
    fun deleteSelectOption(columnId: String, label: String) {
        updateOptions(columnId) { options -> options.filterNot { it.label == label } }
        rewriteCellsOfColumn(columnId) { labels -> labels.filterNot { it == label } }
    }

    private fun updateOptions(
        columnId: String,
        transform: (List<SelectOption>) -> List<SelectOption>
    ) {
        val column = rawColumn(columnId) ?: return
        val current = try {
            json.decodeFromString<List<SelectOption>>(column.optionsJson)
        } catch (e: Exception) {
            emptyList()
        }
        viewModelScope.launch {
            repository.updateColumn(column.copy(optionsJson = json.encodeToString(transform(current))))
        }
    }

    /**
     * Applica una trasformazione alle etichette salvate in ogni cella di
     * una colonna. Vale sia per la selezione singola sia per quella
     * multipla, che tiene più etichette nella stessa stringa.
     */
    private fun rewriteCellsOfColumn(columnId: String, transform: (List<String>) -> List<String>) {
        val state = _tableState.value
        viewModelScope.launch {
            state.rows.forEach { row ->
                val value = state.cellValues[row.id to columnId].orEmpty()
                if (value.isEmpty()) return@forEach
                val labels = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
                val updated = transform(labels)
                if (updated != labels) {
                    repository.setCellValue(row.id, columnId, updated.joinToString(MULTI_VALUE_SEPARATOR))
                }
            }
        }
    }

    /**
     * Sposta una colonna di un posto a sinistra (delta -1) o a destra
     * (delta +1), oltre la vicina che si vede (`orderAfterMove`). Anche
     * Name, con [NAME_COLUMN_KEY] (dal 29/09/2026: "permettimi di spostare
     * ovunque io voglia anche la colonna Name").
     *
     * Le colonne si rinumerano tutte da capo invece di scambiare i due
     * indici: con una colonna nascosta in mezzo lo scambio farebbe
     * saltare la vicina dall'altra parte di quella, e due colonne con lo stesso
     * numero si scambierebbero il posto senza muoversi. Name prende il
     * numero della colonna che le resta a destra (vedi `namePosition`). Il
     * salvataggio è uno solo, così la tabella si ridisegna una volta e non a
     * metà strada.
     */
    fun moveColumn(key: String, delta: Int) {
        val state = _tableState.value
        val data = _dataPage.value ?: return
        val order = state.orderAfterMove(key, delta, data.nameColumnOrder) ?: return
        val columnIds = order.filterNot { it == NAME_COLUMN_KEY }
        // Dalle colonne vere: quelle dello stato, in una vista collegata,
        // hanno il "nascosto" della vista (vedi `rawColumns`).
        val changed = columnIds.mapIndexedNotNull { index, id ->
            rawColumn(id)?.takeIf { it.orderIndex != index }?.copy(orderIndex = index)
        }
        val nameOrder = nameOrderAt(order.indexOf(NAME_COLUMN_KEY)).takeIf { it != data.nameColumnOrder }
        // Qui non si tocca la copia della pagina: la tabella si ridisegna da
        // quello che è salvato, colonne e Name insieme. Aggiornarla prima
        // faceva tornare indietro Name quando arrivava la risposta di uno
        // spostamento precedente.
        viewModelScope.launch { repository.reorderColumns(changed, data.id, nameOrder) }
    }

    fun addRow(title: String = "") {
        val pageId = dataPageId ?: return
        viewModelScope.launch {
            val row = DatabaseRowEntity(pageId = pageId, title = title, orderIndex = nextRowIndex())
            keepVisibleWhileSearching(row.id)
            repository.addRow(row)
            fillFilterValue(row.id)
        }
    }

    /**
     * Aggiunge una riga già assegnata a una colonna della bacheca:
     * creandola dentro "In corso" ci si aspetta che ci resti, non che
     * compaia fra quelle senza valore e vada spostata a mano.
     */
    fun addRowInGroup(columnId: String, value: String) {
        val pageId = dataPageId ?: return
        viewModelScope.launch {
            val row = DatabaseRowEntity(pageId = pageId, orderIndex = nextRowIndex())
            keepVisibleWhileSearching(row.id)
            repository.addRow(row)
            if (value.isNotEmpty()) {
                repository.setCellValue(row.id, columnId, groupCellValue(columnId, value))
            }
            fillFilterValue(row.id, skipColumnId = columnId)
        }
    }

    /**
     * Una riga nata dentro un sottogruppo: col valore del gruppo **e** quello
     * del sottogruppo, così compare lì dove la si è chiesta.
     */
    fun addRowInSubGroup(groupColumnId: String, groupValue: String, subColumnId: String, subValue: String) {
        val pageId = dataPageId ?: return
        viewModelScope.launch {
            val row = DatabaseRowEntity(pageId = pageId, orderIndex = nextRowIndex())
            keepVisibleWhileSearching(row.id)
            repository.addRow(row)
            if (groupValue.isNotEmpty()) repository.setCellValue(row.id, groupColumnId, groupCellValue(groupColumnId, groupValue))
            if (subValue.isNotEmpty()) repository.setCellValue(row.id, subColumnId, groupCellValue(subColumnId, subValue))
            // Il valore del filtro non deve coprire quello del gruppo o del sottogruppo.
            val skip = if (_page.value?.filterColumnId == subColumnId) subColumnId else groupColumnId
            fillFilterValue(row.id, skipColumnId = skip)
        }
    }

    /**
     * Il valore da scrivere nella cella di una riga nata dentro il gruppo
     * `key`: quasi sempre è la chiave stessa, ma i gruppi per data sono
     * giorni (`toEpochDay`) e la cella vuole l'istante della mezzanotte.
     * Fino al 14/10/2026 ci finiva il numero del giorno così com'era, e la
     * riga nuova cadeva al 1° gennaio 1970, in un gruppo tutto suo.
     */
    private fun groupCellValue(columnId: String, key: String): String {
        if (rawColumn(columnId)?.type != ColumnType.DATE) return key
        val day = key.toLongOrNull() ?: return key
        return LocalDate.ofEpochDay(day)
            .atStartOfDay(com.gabriele.notionlocal.data.settings.AppSettings.zoneId)
            .toInstant().toEpochMilli().toString()
    }

    // Uno più del massimo, non il numero di righe: dopo un'eliminazione
    // contarle darebbe un indice già occupato (stesso motivo spiegato
    // in addColumn).
    // **Fra tutte le righe, non solo quelle che si vedono.** Prima si
    // contava su quelle dello stato, che sono già filtrate: con un filtro
    // (o, dal 24/09/2026, una vista collegata filtrata, o una ricerca) la
    // riga nuova poteva prendere il numero di una nascosta, e due righe
    // con lo stesso numero non hanno un ordine sicuro fra loro.
    private fun nextRowIndex(): Int = lastRowOrderIndex + 1

    /** Si comincia a scrivere il nome di una riga: l'ordine si ferma com'è adesso. */
    fun startTitleEdit() {
        if (_frozenOrder.value == null) _frozenOrder.value = _tableState.value.rows.map { it.id }
    }

    /** Finito di scrivere: le righe tornano al loro ordine (se una spunta non le tiene ferme). */
    fun endTitleEdit() {
        if (!checkboxHold) _frozenOrder.value = null
    }

    /**
     * La tabella esce dalla composizione (si lascia la pagina), o cambiano
     * ordine e filtro: le righe ferme dopo una spunta vanno al loro posto.
     * Vedi [checkboxHold].
     */
    fun releaseCheckboxHold() {
        holdRelease?.cancel()
        holdRelease = null
        if (!checkboxHold) return
        checkboxHold = false
        _frozenOrder.value = null
        _heldVisible.value = null
    }

    private fun holdAfterCheckbox(row: DatabaseRowEntity, column: DatabaseColumnEntity, value: String) {
        val page = _page.value
        val sorted = sortRulesOf(page, rawColumns).any { it.column?.id == column.id }
        val filtered = page?.filterColumnId == column.id && !page.filterValue.isNullOrBlank()
        if (!sorted && !filtered) return
        val shown = _tableState.value.rows.map { it.id }
        if (_frozenOrder.value == null) _frozenOrder.value = shown
        if (_heldVisible.value == null) _heldVisible.value = shown.toSet()
        checkboxHold = true
        holdRelease?.cancel()
        holdRelease = viewModelScope.launch {
            // Prima la casella si riempie dov'è (il valore arriva dal
            // database), poi un attimo, poi le righe al loro posto.
            withTimeoutOrNull(2_000) { _tableState.first { it.cellValues[row.id to column.id] == value } }
            delay(CHECKBOX_SETTLE_MS)
            holdRelease = null
            releaseCheckboxHold()
        }
    }

    fun updateRowTitle(row: DatabaseRowEntity, newTitle: String) {
        viewModelScope.launch { repository.setRowTitle(row.id, newTitle) }
    }

    /**
     * Cancella una riga **con la sua pagina** (dal 08/10/2026): prima la
     * pagina restava nel database, senza più un posto. I suoi file (icone,
     * copertine, immagini e file dei blocchi) se ne vanno da `imageStore`.
     */
    fun deleteRow(row: DatabaseRowEntity, imageStore: PageImageStore? = null) {
        viewModelScope.launch {
            val pageId = repository.deleteRow(row) ?: return@launch
            pageRepository.deleteRowPage(pageId).forEach { imageStore?.delete(it) }
        }
    }

    // --- Le sotto-righe (dal 29/09/2026) ---

    private val _editTitleOf = MutableStateFlow<String?>(null)

    /** Una riga appena nata il cui nome va scritto subito: la cella del nome si apre da sola. */
    val editTitleOf: StateFlow<String?> = _editTitleOf

    fun consumeEditTitle() {
        _editTitleOf.value = null
    }

    /**
     * **"Create sub row"**: una riga nuova sotto `parent`, in fondo alle sue
     * sotto-righe, che si aprono se erano chiuse. Prende da lei i valori di
     * `inherit` — le proprietà dei gruppi della tabella —, così compare lì
     * sotto e non in un altro gruppo; e il valore del filtro, come ogni riga
     * nuova. Poi il suo nome si apre per scriverlo.
     */
    fun addSubRow(parent: DatabaseRowEntity, inherit: List<String> = emptyList()) {
        val pageId = dataPageId ?: return
        val values = _tableState.value.cellValues
        viewModelScope.launch {
            val row = DatabaseRowEntity(pageId = pageId, parentRowId = parent.id, orderIndex = nextRowIndex())
            keepVisibleWhileSearching(row.id)
            repository.addRow(row)
            if (parent.subRowsCollapsed) repository.setSubRowsCollapsed(parent.id, false)
            inherit.forEach { columnId ->
                values[parent.id to columnId]?.takeIf { it.isNotEmpty() }?.let { repository.setCellValue(row.id, columnId, it) }
            }
            if (_page.value?.filterColumnId !in inherit) fillFilterValue(row.id)
            _editTitleOf.value = row.id
        }
    }

    /** La freccia sul nome: apre o chiude le sotto-righe. */
    fun setSubRowsCollapsed(row: DatabaseRowEntity, collapsed: Boolean) {
        viewModelScope.launch { repository.setSubRowsCollapsed(row.id, collapsed) }
    }

    /** "Move up" / "Move down": la riga subito prima o subito dopo `target`. Vedi `DatabaseRepository.moveRowNextTo`. */
    fun moveRowNextTo(row: DatabaseRowEntity, target: DatabaseRowEntity, before: Boolean) {
        val pageId = dataPageId ?: return
        viewModelScope.launch { repository.moveRowNextTo(pageId, row.id, target.id, before) }
    }

    fun setCellValue(row: DatabaseRowEntity, column: DatabaseColumnEntity, value: String) {
        if (column.type == ColumnType.CHECKBOX) holdAfterCheckbox(row, column, value)
        viewModelScope.launch {
            repository.setCellValue(row.id, column.id, value)
        }
    }

    /**
     * "Activate all" / "Deactivate all" di una colonna Checkbox: le righe che
     * la vista mostra adesso (filtro e ricerca compresi), quelle che si
     * vedono sotto l'intestazione tenuta premuta.
     */
    fun setAllChecked(column: DatabaseColumnEntity, checked: Boolean) {
        val rowIds = _tableState.value.rows.map { it.id }
        viewModelScope.launch { repository.setAllChecked(rowIds, column.id, checked) }
    }

    /**
     * Apre una riga come pagina vera e propria: crea la pagina collegata
     * al volo se non esiste ancora (isRowPage = true, così non compare
     * mai come pagina radice indipendente nella Home), altrimenti riusa
     * quella già collegata. onReady riceve l'id della pagina per
     * navigarci subito.
     */
    fun openRow(row: DatabaseRowEntity, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val targetPageId = row.linkedPageId ?: run {
                val newPage = PageEntity(
                    title = row.title.ifBlank { "Untitled" },
                    isRowPage = true
                )
                pageRepository.createPage(newPage)
                repository.updateRow(row.copy(linkedPageId = newPage.id))
                newPage.id
            }
            onReady(targetPageId)
        }
    }
}
