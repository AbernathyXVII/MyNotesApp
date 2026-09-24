package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.dao.RowCover
import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.GALLERY_DEFAULT_PREVIEW
import com.gabriele.notionlocal.data.entity.GalleryCardPreview
import com.gabriele.notionlocal.data.entity.GalleryCardSize
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.TAG_COLORS
import com.gabriele.notionlocal.data.entity.TimelineZoom
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.RowPreviewLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
private fun PageEntity.galleryPreviewShown(): GalleryCardPreview? =
    // Un database semplice non ha pagine di riga, quindi niente copertine
    // né testo da leggere: nessuna query.
    if (databaseLayout == DatabaseLayout.GALLERY && !isSimpleDatabase) {
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
data class DatabaseTableState(
    val columns: List<DatabaseColumnEntity> = emptyList(),
    val rows: List<DatabaseRowEntity> = emptyList(),
    val cellValues: Map<Pair<String, String>, String> = emptyMap()
) {
    /**
     * Le colonne da disegnare. `columns` resta l'elenco completo, perché
     * una proprietà nascosta continua a esistere ovunque non sia la
     * griglia: si ordina, si raggruppa e si scrive lo stesso.
     */
    val visibleColumns: List<DatabaseColumnEntity> get() = columns.filterNot { it.hidden }

    /**
     * La colonna con cui si scambierebbe il posto spostandone una a
     * sinistra (`delta` -1) o a destra (+1), o null se da quella parte non
     * c'è niente.
     *
     * Si conta **fra quelle che si vedono**: nella tabella la vicina è la
     * colonna accanto, e scambiarsi con una nascosta in mezzo non
     * sposterebbe niente di visibile. Una colonna nascosta invece si
     * muove fra tutte, perché fra le visibili non c'è. A sinistra della
     * prima non si va mai: lì c'è Name, che non è una proprietà e resta
     * sempre la prima.
     */
    fun neighbourForMove(columnId: String, delta: Int): DatabaseColumnEntity? {
        val column = columns.find { it.id == columnId } ?: return null
        val lane = if (column.hidden) columns else visibleColumns
        val position = lane.indexOfFirst { it.id == columnId }
        return lane.getOrNull(position + delta)
    }
}

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

    /** Riga → immagine dell'icona della sua pagina, per le righe che ne hanno una. */
    private val _rowIcons = MutableStateFlow<Map<String, String>>(emptyMap())
    val rowIcons: StateFlow<Map<String, String>> = _rowIcons

    /** Riga → copertina della sua pagina: le schede della galleria. */
    private val _rowCovers = MutableStateFlow<Map<String, RowCover>>(emptyMap())
    val rowCovers: StateFlow<Map<String, RowCover>> = _rowCovers

    /** Riga → prime righe di testo della sua pagina: l'anteprima "Page content" della galleria. */
    private val _rowPreviews = MutableStateFlow<Map<String, List<RowPreviewLine>>>(emptyMap())
    val rowPreviews: StateFlow<Map<String, List<RowPreviewLine>>> = _rowPreviews

    @OptIn(ExperimentalCoroutinesApi::class)
    fun load(pageId: String) {
        if (currentPageId == pageId) return
        currentPageId = pageId

        viewModelScope.launch {
            repository.observeRowIcons(pageId).collect { _rowIcons.value = it }
        }

        // Copertine e testo delle pagine si seguono **solo mentre la
        // galleria li mostra**. Il testo soprattutto: la query guarda la
        // tabella dei blocchi, che cambia ad ogni tasto battuto in
        // qualunque pagina, e questo ViewModel resta vivo anche quando
        // il database è rimasto indietro nella pila di navigazione.
        // Seguirlo sempre vorrebbe dire rifare la query ad ogni lettera
        // scritta altrove, per una vista che magari non è nemmeno quella
        // scelta.
        viewModelScope.launch {
            _page.map { it?.galleryPreviewShown() }
                .distinctUntilChanged()
                .flatMapLatest { shown ->
                    if (shown == GalleryCardPreview.PAGE_COVER) {
                        repository.observeRowCovers(pageId)
                    } else {
                        flowOf(emptyMap<String, RowCover>())
                    }
                }
                .collect { _rowCovers.value = it }
        }
        viewModelScope.launch {
            _page.map { it?.galleryPreviewShown() }
                .distinctUntilChanged()
                .flatMapLatest { shown ->
                    if (shown == GalleryCardPreview.PAGE_CONTENT) {
                        repository.observeRowContentPreviews(pageId, GALLERY_PREVIEW_MAX_LINES)
                    } else {
                        flowOf(emptyMap<String, List<RowPreviewLine>>())
                    }
                }
                .distinctUntilChanged()
                .collect { _rowPreviews.value = it }
        }

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

        viewModelScope.launch {
            combine(
                repository.getColumns(pageId),
                repository.getRows(pageId),
                repository.getAllCells(pageId),
                // La pagina entra nel calcolo perché è lì che sta
                // l'ordinamento: senza, cambiarlo non farebbe
                // ricalcolare niente e la tabella resterebbe com'era.
                _page
            ) { columns, rows, cells, page ->
                val cellValues = cells.associate { (it.rowId to it.columnId) to it.value }
                // Prima si filtra e poi si ordina: ordinare righe che
                // stanno per sparire è lavoro buttato, e il risultato
                // sarebbe lo stesso.
                val kept = filterRows(rows, columns, cellValues, page)
                DatabaseTableState(
                    columns = columns,
                    rows = sortRows(kept, columns, cellValues, page),
                    cellValues = cellValues
                )
            }.collect { state ->
                _tableState.value = state
            }
        }
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
    private fun sortRows(
        rows: List<DatabaseRowEntity>,
        columns: List<DatabaseColumnEntity>,
        cellValues: Map<Pair<String, String>, String>,
        page: PageEntity?
    ): List<DatabaseRowEntity> {
        val sortId = page?.sortColumnId ?: return rows
        val descending = page.sortDescending
        val column = columns.find { it.id == sortId }
        if (sortId != SORT_BY_NAME && column == null) return rows

        val keyed = rows.map { row ->
            row to sortValueOf(row, column, cellValues)
        }
        return keyed.sortedWith { a, b ->
            val (_, first) = a
            val (_, second) = b
            when {
                first.isEmpty && second.isEmpty -> 0
                first.isEmpty -> 1
                second.isEmpty -> -1
                else -> {
                    val cmp = compareSortValues(first, second)
                    if (descending) -cmp else cmp
                }
            }
        }.map { it.first }
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
     * scelta **come collegamento a pagina** (vedi
     * `PageRepository.movePageTo`). `onDone` solo se lo spostamento c'è
     * stato.
     */
    fun movePageTo(destination: PageRepository.PageTreeNode, onDone: () -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch {
            pageRepository.movePageTo(current.id, destination)?.let { change ->
                PendingPageUndo.offer(change)
                onDone()
            }
        }
    }

    /**
     * L'icona del database: un'immagine scelta dall'utente, o null per
     * **nessuna icona**.
     *
     * Per un database "nessuna icona" vuol dire proprio niente accanto
     * al nome, non il 📄 delle pagine: l'emoji di riserva che le pagine
     * hanno scritto nel campo `icon` qui non si usa, altrimenti ogni
     * database avrebbe avuto un foglietto davanti senza che nessuno
     * l'avesse chiesto. Conta solo `iconImage`.
     *
     * Restituisce il nome del file che è stato sostituito, perché chi
     * chiama possa cancellarlo: lasciarlo lì riempirebbe la cartella di
     * immagini che nessuno può più raggiungere. Stessa regola delle
     * pagine (`PageEditorViewModel.setIconImage`).
     */
    fun setIconImage(fileName: String?): String? {
        val current = _page.value ?: return null
        val previous = current.iconImage
        val updated = current.copy(iconImage = fileName)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
        return previous?.takeIf { it != fileName }
    }

    /**
     * L'icona della pagina di una riga, o null per toglierla.
     *
     * La riga potrebbe non avere ancora una pagina — nasce la prima volta
     * che la si apre — e allora la si crea qui: l'icona sta nella pagina,
     * non nella riga. Si vede subito, prima ancora che il database la
     * confermi. Restituisce il file dell'icona di prima, che chi chiama
     * toglie dalla cartella (come `setIconImage`).
     */
    fun setRowIcon(row: DatabaseRowEntity, fileName: String?): String? {
        val previous = _rowIcons.value[row.id]
        _rowIcons.value = if (fileName == null) {
            _rowIcons.value - row.id
        } else {
            _rowIcons.value + (row.id to fileName)
        }
        viewModelScope.launch {
            val pageId = pageRepository.ensureRowPage(row.id) ?: return@launch
            pageRepository.setIconImage(pageId, fileName)
        }
        return previous?.takeIf { it != fileName }
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

    /** Sceglie la proprietà su cui ordinare, o null per tornare all'ordine di creazione. */
    fun setSort(columnId: String?, descending: Boolean) {
        val current = _page.value ?: return
        val updated = current.copy(sortColumnId = columnId, sortDescending = descending)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    /**
     * La proprietà che fa da colonna nella vista a bacheca. Se non è
     * stata scelta, o se quella scelta è sparita o non è più a selezione
     * singola, si ripiega sulla prima adatta: meglio una bacheca
     * ragionevole che una schermata vuota.
     *
     * Solo SELECT: con la selezione multipla una riga apparterrebbe a
     * più colonne insieme, e non è ovvio cosa dovrebbe succedere
     * spostandola.
     */
    fun boardGroupColumn(): DatabaseColumnEntity? {
        val columns = _tableState.value.columns
        val chosen = _page.value?.boardGroupColumnId?.let { id ->
            columns.find { it.id == id }
        }
        return chosen?.takeIf { it.type == ColumnType.SELECT }
            ?: columns.firstOrNull { it.type == ColumnType.SELECT }
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

    /** Sceglie la proprietà su cui raggruppare, o null per togliere i gruppi. */
    fun setTableGroupColumn(columnId: String?) {
        val current = _page.value ?: return
        val updated = current.copy(tableGroupColumnId = columnId)
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
        val pageId = currentPageId ?: return
        val existing = calendarDateColumn()
        if (existing != null) {
            addRowOnDate(existing.id, dateMillis)
            return
        }
        // Stesso criterio di addColumn per l'indice: uno più del
        // massimo, non il numero di colonne.
        val nextIndex = (_tableState.value.columns.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val column = DatabaseColumnEntity(
            pageId = pageId,
            name = "Date",
            type = ColumnType.DATE,
            orderIndex = nextIndex
        )
        viewModelScope.launch {
            repository.addColumn(column)
            val row = DatabaseRowEntity(pageId = pageId, orderIndex = nextRowIndex())
            repository.addRow(row)
            repository.setCellValue(row.id, column.id, dateMillis.toString())
        }
        // La vista deve usare proprio questa, non "la prima data che
        // trova": se domani se ne aggiunge un'altra, il calendario non
        // deve cambiare colonna sotto i piedi.
        setCalendarDateColumn(column.id)
    }

    fun addRowOnDate(columnId: String, dateMillis: Long) {
        val pageId = currentPageId ?: return
        viewModelScope.launch {
            val row = DatabaseRowEntity(pageId = pageId, orderIndex = nextRowIndex())
            repository.addRow(row)
            repository.setCellValue(row.id, columnId, dateMillis.toString())
        }
    }

    fun setBoardGroupColumn(columnId: String) {
        val current = _page.value ?: return
        val updated = current.copy(boardGroupColumnId = columnId)
        _page.value = updated
        viewModelScope.launch { pageRepository.updatePage(updated) }
    }

    fun setLayout(layout: DatabaseLayout) {
        val current = _page.value ?: return
        val updated = current.copy(databaseLayout = layout)
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
        val pageId = currentPageId ?: return
        // Uno più del massimo, non il numero di colonne: dopo aver
        // eliminato una colonna in mezzo, contarle darebbe un indice già
        // occupato, e due colonne con lo stesso indice non si possono
        // riordinare (lo scambio non cambierebbe nulla).
        val nextIndex = (_tableState.value.columns.maxOfOrNull { it.orderIndex } ?: -1) + 1
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
        val column = _tableState.value.columns.find { it.id == columnId } ?: return
        if (column.hidden == hidden) return
        viewModelScope.launch { repository.updateColumn(column.copy(hidden = hidden)) }
    }

    /** Mette il contenuto della colonna al centro, o lo rimanda a sinistra. */
    fun setColumnCentered(columnId: String, centered: Boolean) {
        val column = _tableState.value.columns.find { it.id == columnId } ?: return
        if (column.centerContent == centered) return
        viewModelScope.launch { repository.updateColumn(column.copy(centerContent = centered)) }
    }

    /** Nasconde o rimostra tutte le colonne in un colpo solo. */
    fun setAllColumnsHidden(hidden: Boolean) {
        val columns = _tableState.value.columns.filter { it.hidden != hidden }
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
    fun updateColumn(columnId: String, name: String, type: ColumnType, optionsJson: String) {
        val column = _tableState.value.columns.find { it.id == columnId } ?: return
        viewModelScope.launch {
            repository.updateColumn(
                column.copy(name = name, type = type, optionsJson = optionsJson)
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
        val column = _tableState.value.columns.find { it.id == columnId } ?: return
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
        val column = _tableState.value.columns.find { it.id == columnId } ?: return
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
     * (delta +1), oltre la vicina che si vede (`neighbourForMove`).
     *
     * Le colonne si rinumerano tutte da capo invece di scambiare i due
     * indici: con una colonna nascosta in mezzo lo scambio farebbe
     * saltare la vicina dall'altra parte di quella, e due colonne con lo stesso
     * numero si scambierebbero il posto senza muoversi. Il salvataggio è
     * uno solo, così la tabella si ridisegna una volta e non a metà strada.
     */
    fun moveColumn(columnId: String, delta: Int) {
        val state = _tableState.value
        val column = state.columns.find { it.id == columnId } ?: return
        val neighbour = state.neighbourForMove(columnId, delta) ?: return
        val reordered = state.columns.filterNot { it.id == columnId }.toMutableList()
        val at = reordered.indexOfFirst { it.id == neighbour.id }
        reordered.add(if (delta < 0) at else at + 1, column)
        val changed = reordered.mapIndexedNotNull { index, c ->
            if (c.orderIndex != index) c.copy(orderIndex = index) else null
        }
        viewModelScope.launch { repository.updateColumns(changed) }
    }

    fun addRow(title: String = "") {
        val pageId = currentPageId ?: return
        viewModelScope.launch {
            repository.addRow(
                DatabaseRowEntity(pageId = pageId, title = title, orderIndex = nextRowIndex())
            )
        }
    }

    /**
     * Aggiunge una riga già assegnata a una colonna della bacheca:
     * creandola dentro "In corso" ci si aspetta che ci resti, non che
     * compaia fra quelle senza valore e vada spostata a mano.
     */
    fun addRowInGroup(columnId: String, value: String) {
        val pageId = currentPageId ?: return
        viewModelScope.launch {
            val row = DatabaseRowEntity(pageId = pageId, orderIndex = nextRowIndex())
            repository.addRow(row)
            if (value.isNotEmpty()) {
                repository.setCellValue(row.id, columnId, value)
            }
        }
    }

    // Uno più del massimo, non il numero di righe: dopo un'eliminazione
    // contarle darebbe un indice già occupato (stesso motivo spiegato
    // in addColumn).
    private fun nextRowIndex(): Int =
        (_tableState.value.rows.maxOfOrNull { it.orderIndex } ?: -1) + 1

    fun updateRowTitle(row: DatabaseRowEntity, newTitle: String) {
        viewModelScope.launch { repository.setRowTitle(row.id, newTitle) }
    }

    fun deleteRow(row: DatabaseRowEntity) {
        viewModelScope.launch { repository.deleteRow(row) }
    }

    fun setCellValue(row: DatabaseRowEntity, column: DatabaseColumnEntity, value: String) {
        viewModelScope.launch {
            repository.setCellValue(row.id, column.id, value)
        }
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
