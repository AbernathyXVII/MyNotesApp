package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageFont
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.applyTextEdit
import com.gabriele.notionlocal.data.entity.joinLines
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.data.entity.setColorInRange
import com.gabriele.notionlocal.data.entity.setFormatInRange
import com.gabriele.notionlocal.data.entity.splitLines
import com.gabriele.notionlocal.data.entity.toggleFormatInRange
import com.gabriele.notionlocal.data.TextStats
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.theme.DEFAULT_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.MAX_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.MIN_PAGE_FONT_SIZE
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** I tipi di formattazione inline selezionabili dalla barra "Aa". */
enum class FormatType { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, SPOILER }

/**
 * Gestisce il contenuto di una singola pagina di testo: titolo, icona,
 * l'albero di blocchi (paragrafi, titoli, liste, checkbox, toggle,
 * tabelle, link a pagina/database...), il focus tra i blocchi, e la
 * cronologia annulla/ripristina.
 */
/**
 * Cosa ha fatto `splitBlockAt`. Serve a chi chiama, non per curiosità:
 * `BECAME_PARAGRAPH` vuol dire che il blocco ha **cambiato specie**, da
 * riga a sé a testo scorrevole, e che quindi il campo di testo da cui
 * arriva la chiamata sta per essere distrutto. Chi lo sa deve staccare
 * la tastiera prima che succeda — vedi il commento in `BlockRow`.
 *
 * `SPLIT_ABOVE` vuol dire l'opposto: la riga nuova è stata messa
 * **sopra** e il campo che chiama resta vivo, col fuoco, a tenere il
 * testo dopo il cursore. Deve quindi mostrare quello, non la prima
 * riga, col cursore all'inizio.
 */
enum class SplitOutcome { SPLIT, SPLIT_ABOVE, BECAME_PARAGRAPH, NOTHING }

/**
 * Cosa ha fatto il backspace a inizio riga. `MERGED_INTO_ISLAND` vuol
 * dire che questa riga è finita dentro il blocco di sopra e quindi non
 * esiste più: il campo che ha chiamato sta per essere distrutto.
 */
enum class BackspaceOutcome { NOTHING, STEP_UNDONE, MERGED_INTO_ISLAND }

class PageEditorViewModel(private val repository: PageRepository) : ViewModel() {

    companion object {
        // Piccolo ritardo prima di spostare il focus su un blocco diverso
        // da quello su cui l'utente sta scrivendo. Mitigazione
        // sperimentale per un bug documentato della tastiera Samsung con
        // Jetpack Compose: se il campo di testo attivo viene sostituito
        // "da sotto" troppo a ridosso di un carattere appena digitato
        // (mentre la tastiera sta ancora finalizzando quel carattere),
        // quel carattere può finire duplicato nel campo vecchio invece
        // che nel nuovo. Non è garantito risolva del tutto — la causa
        // vive nell'implementazione della tastiera stessa, non nel
        // nostro codice — ma dà alla tastiera un momento in più per
        // "assestarsi" prima che il focus si sposti.
        private const val IME_SETTLE_DELAY_MS = 60L

        // Quanto aspettare che una riga prenda il fuoco prima di rinunciare
        // a togliere quella che lo sta cedendo. Di solito basta un
        // fotogramma; mezzo secondo è largo apposta.
        private const val FOCUS_HANDOFF_TIMEOUT_MS = 500L

        // Tetto ai passaggi di annulla ricordati, per non far crescere
        // la memoria senza limite su una sessione di modifica lunga.
        private const val MAX_UNDO_STEPS = 50

        // Livello massimo di rientro per i blocchi di testo scorrevole.
        //
        // Dieci e non cinque perché i segni degli elenchi puntati fanno
        // un giro di otto (vedi `BULLET_MARKERS`): con cinque livelli il
        // giro non si sarebbe mai potuto vedere finire, né tantomeno
        // ricominciare.
        private const val MAX_INDENT_LEVEL = 10

        // Quanto si aspetta, chiusa una sessione di scrittura, prima di
        // leggere il testo "dopo" da mettere nella cronologia: il tempo
        // che l'ultima battuta faccia il giro fino al database e torni
        // indietro. Vedi `PageRepository.recordEditAfterSettling`.
        private const val EDIT_SETTLE_MS = 400L
    }

    /**
     * Le divisioni di blocco si mettono in fila.
     *
     * Ognuna rilegge dal database l'indice del blocco e sposta di uno
     * tutto quello che sta sotto: due che girano insieme leggono lo
     * stesso indice e finiscono per occupare lo stesso posto. È successo
     * davvero, andando a capo in fretta.
     */
    private val structuralEdits = Mutex()

    private val json = Json { ignoreUnknownKeys = true }

    private var currentPageId: String? = null

    private val _page = MutableStateFlow<PageEntity?>(null)
    val page: StateFlow<PageEntity?> = _page

    private var blocksFlowJob: Job? = null
    private var pageFlowJob: Job? = null
    private var editsFlowJob: Job? = null

    private val _blocks = MutableStateFlow<List<BlockEntity>>(emptyList())
    val blocks: StateFlow<List<BlockEntity>> = _blocks

    // --- Le righe nuove si vedono subito, non al ritorno dal database ---
    //
    // **Andando a capo dentro un toggle la riga appena scritta spariva
    // per un fotogramma.** Il campo col cursore passa all'istante al testo
    // dopo il cursore, ma la riga nuova sopra, col testo di prima, arrivava
    // solo quando il database aveva finito di scriverla e la rimandava
    // indietro: 29 ms misurati, tre o quattro fotogrammi. Registrando lo
    // schermo si vede la lettera appena battuta scomparire e ricomparire
    // una riga più su, e il cursore scendere di colpo. Il testo condiviso
    // non ha il problema perché lì le righe sono tutte dentro un campo solo.
    //
    // Qui la riga nuova entra nella lista **nello stesso istante** in cui
    // il campo cambia testo, e il database la conferma dopo.
    //
    // Non basta però aggiungerla una volta: una lettura del database
    // partita un attimo prima dell'Invio può arrivare dopo, senza la riga,
    // e toglierla di nuovo — e se quella riga ha già il cursore, togliere
    // la sua casella di scrittura fa chiudere l'app. Quindi la modifica
    // resta in sospeso e si **riapplica a ogni lettura** che non contiene
    // ancora la riga nuova; la prima che la contiene la chiude.

    private class PendingInsert(
        val newBlockId: String,
        val applyTo: (List<BlockEntity>) -> List<BlockEntity>
    )

    private val pendingInserts = mutableListOf<PendingInsert>()
    private var lastLoadedBlocks: List<BlockEntity> = emptyList()

    /** Mostra subito una riga nuova che il database deve ancora scrivere. */
    private fun showBeforeSaved(newBlockId: String, applyTo: (List<BlockEntity>) -> List<BlockEntity>) {
        pendingInserts += PendingInsert(newBlockId, applyTo)
        _blocks.value = applyTo(_blocks.value)
    }

    /** Il database non l'ha scritta: la riga mostrata in anticipo se ne va. */
    private fun forgetPendingInsert(newBlockId: String) {
        if (pendingInserts.removeAll { it.newBlockId == newBlockId }) publishBlocks(lastLoadedBlocks)
    }

    private fun publishBlocks(loaded: List<BlockEntity>) {
        pendingInserts.removeAll { pending -> loaded.any { it.id == pending.newBlockId } }
        var shown = loaded
        for (pending in pendingInserts) shown = pending.applyTo(shown)
        // Se la pagina non ha ancora nessun blocco (appena creata),
        // partiamo con un paragrafo vuoto pronto per scrivere,
        // così l'utente non vede una schermata completamente bianca
        // senza cursore.
        _blocks.value = shown.ifEmpty {
            listOf(BlockEntity(pageId = currentPageId ?: return, orderIndex = 0))
        }
    }

    /** `splitBlockAbove` del repository, fatto sulla lista in memoria. */
    private fun splitAboveInList(
        list: List<BlockEntity>,
        blockId: String,
        newBlockId: String,
        newBlockType: BlockType,
        textBeforeJson: String,
        textAfterJson: String
    ): List<BlockEntity> {
        val current = list.find { it.id == blockId } ?: return list
        val shifted = list.map { b ->
            when {
                b.id == current.id -> b.copy(
                    orderIndex = current.orderIndex + 1,
                    textJson = textAfterJson,
                    isChecked = false,
                    numberStartsAt = null
                )
                b.parentBlockId == current.parentBlockId && b.orderIndex >= current.orderIndex ->
                    b.copy(orderIndex = b.orderIndex + 1)
                else -> b
            }
        }
        val above = BlockEntity(
            id = newBlockId,
            pageId = current.pageId,
            parentBlockId = current.parentBlockId,
            orderIndex = current.orderIndex,
            type = newBlockType,
            textJson = textBeforeJson,
            isChecked = current.isChecked,
            numberStartsAt = current.numberStartsAt,
            indentLevel = current.indentLevel
        )
        return (shifted + above).sortedBy { it.orderIndex }
    }

    /** `insertFirstChild` del repository, fatto sulla lista in memoria. */
    private fun firstChildInList(
        list: List<BlockEntity>,
        parentId: String,
        newBlockId: String,
        textJson: String
    ): List<BlockEntity> {
        val parent = list.find { it.id == parentId } ?: return list
        val shifted = list.map { b ->
            if (b.parentBlockId == parent.id) b.copy(orderIndex = b.orderIndex + 1) else b
        }
        val child = BlockEntity(
            id = newBlockId,
            pageId = parent.pageId,
            parentBlockId = parent.id,
            orderIndex = 0,
            textJson = textJson
        )
        return (shifted + child).sortedBy { it.orderIndex }
    }

    /** `insertSiblingBelow` del repository, fatto sulla lista in memoria. */
    private fun siblingBelowInList(
        list: List<BlockEntity>,
        blockId: String,
        newBlockId: String,
        type: BlockType,
        textJson: String
    ): List<BlockEntity> {
        val current = list.find { it.id == blockId } ?: return list
        val shifted = list.map { b ->
            if (b.parentBlockId == current.parentBlockId && b.orderIndex >= current.orderIndex + 1) {
                b.copy(orderIndex = b.orderIndex + 1)
            } else b
        }
        val sibling = BlockEntity(
            id = newBlockId,
            pageId = current.pageId,
            parentBlockId = current.parentBlockId,
            orderIndex = current.orderIndex + 1,
            type = type,
            textJson = textJson,
            isExpanded = current.isExpanded
        )
        return (shifted + sibling).sortedBy { it.orderIndex }
    }

    // Blocco su cui la UI deve richiedere il focus adesso — impostato da
    // splitBlockAt/splitBlockAtMultiple/addChildBlock/focusLastBlock/
    // deleteBlockAndFocusPrevious, consumato una volta applicato.
    private val _focusRequestBlockId = MutableStateFlow<String?>(null)
    val focusRequestBlockId: StateFlow<String?> = _focusRequestBlockId

    // Dove mettere il cursore dentro il blocco che sta per ricevere il
    // fuoco, quando non basta "dall'inizio" o "in fondo": per ora lo
    // usa la fusione di una riga dentro la casella sopra, che vuole il
    // cursore nel punto esatto in cui i due testi si sono uniti.
    private val _pendingCaret = MutableStateFlow<Pair<String, Int>?>(null)
    val pendingCaret: StateFlow<Pair<String, Int>?> = _pendingCaret

    fun consumePendingCaret() {
        _pendingCaret.value = null
    }

    // Richiesta "applica questa formattazione alla selezione corrente
    // del blocco X" — impostata dalla barra Aa, consumata dal BlockRow
    // del blocco interessato una volta applicata. Stesso schema di
    // _focusRequestBlockId: chi mostra la barra (PageEditorScreen) non
    // conosce la posizione esatta della selezione (vive dentro il
    // singolo BlockRow), quindi manda solo "quale blocco, quale
    // formattazione" e lascia che sia il blocco giusto ad applicarla
    // alla propria selezione attuale.
    private val _formatRequest = MutableStateFlow<Pair<String, FormatType>?>(null)
    val formatRequest: StateFlow<Pair<String, FormatType>?> = _formatRequest

    fun requestFormat(blockId: String, type: FormatType) {
        _formatRequest.value = blockId to type
    }

    fun consumeFormatRequest() {
        _formatRequest.value = null
    }

    // --- Il pennello: quale testo colorare ---
    //
    // **La selezione va messa da parte prima di aprire la finestra.**
    // Il pennello apre un pannello che sale dal basso, e il campo di
    // scrittura, perdendo il fuoco, chiude la selezione riducendola a
    // un cursore: al momento di applicare il colore non ci sarebbe più
    // niente di selezionato. I comandi della barra Aa non hanno questo
    // problema perché non aprono niente e agiscono all'istante.
    //
    // Quindi i campi dicono qui, mentre si usa, dove sta la selezione;
    // il pennello ne fa una copia nell'istante in cui lo si tocca —
    // prima che il fuoco se ne vada — e il colore si applica a quella.

    data class SelectionSnapshot(val blockIds: List<String>, val start: Int, val end: Int)

    private var liveSelection: SelectionSnapshot? = null
    private var capturedSelection: SelectionSnapshot? = null

    /** Posizioni **nel testo dei blocchi**, senza l'a-capo nascosto del campo. */
    fun reportSelection(blockIds: List<String>, start: Int, end: Int) {
        liveSelection = SelectionSnapshot(blockIds, start, end)
    }

    fun captureSelectionForColor() {
        capturedSelection = liveSelection
    }

    /** Vero se c'è davvero del testo selezionato da colorare. */
    fun hasCapturedSelection(): Boolean =
        capturedSelection?.let { it.start < it.end } == true

    fun applyCapturedColor(background: Boolean, hex: String?) {
        val selection = capturedSelection ?: return
        applyColorToRun(selection.blockIds, selection.start, selection.end, background, hex)
    }

    /**
     * Come `applyFormatToRun`: la selezione dell'utente è una sola ma
     * può attraversare più blocchi, e ognuno tiene i propri span.
     */
    fun applyColorToRun(
        runBlockIds: List<String>,
        selStart: Int,
        selEnd: Int,
        background: Boolean,
        hex: String?
    ) {
        if (selStart >= selEnd) return
        val all = _blocks.value
        val runBlocks = runBlockIds.mapNotNull { id -> all.find { it.id == id } }
        if (runBlocks.isEmpty()) return

        snapshotForStructuralChange()

        var pos = 0
        val updates = mutableListOf<BlockEntity>()
        for (block in runBlocks) {
            val text = plainTextOf(block)
            val blockStart = pos
            val blockEnd = pos + text.length
            pos = blockEnd + 1 // +1 per l'a-capo che separa i blocchi

            val overlapStart = maxOf(blockStart, selStart) - blockStart
            val overlapEnd = minOf(blockEnd, selEnd) - blockStart
            if (overlapStart >= overlapEnd) continue

            val newSpans = setColorInRange(
                spansOf(block),
                overlapStart,
                overlapEnd,
                background,
                hex
            )
            updates.add(block.copy(textJson = json.encodeToString(newSpans)))
        }

        viewModelScope.launch { updates.forEach { repository.saveBlock(it) } }
    }

    // --- Annulla / Ripristina ---
    //
    // Un intero "scatto" della lista blocchi della pagina viene salvato
    // prima di ogni modifica strutturale (cambio tipo, divisione,
    // indentazione, spostamento, eliminazione...) e prima di ogni nuova
    // "sessione" di digitazione (la prima lettera scritta in un blocco
    // diverso da quello dell'ultimo scatto salvato) — non ad ogni
    // singolo tasto premuto, altrimenti Annulla toglierebbe una lettera
    // alla volta invece di annullare "quello che hai appena scritto" in
    // un colpo solo, come in un editor vero. Copre i blocchi (testo,
    // tipo, struttura); non copre il contenuto delle celle di tabelle o
    // database, che vivono in tabelle del database separate.
    private val undoStack = ArrayDeque<List<BlockEntity>>()
    private val redoStack = ArrayDeque<List<BlockEntity>>()
    private var lastTextEditSnapshotBlockId: String? = null

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private fun updateUndoRedoAvailability() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun pushUndoSnapshot() {
        undoStack.addLast(_blocks.value)
        if (undoStack.size > MAX_UNDO_STEPS) undoStack.removeFirst()
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    /** Da chiamare prima di una modifica di testo/formattazione (updateBlockText, updateBlockSpans). */
    private fun snapshotForTextEdit(blockId: String) {
        beginEditSession(blockId)
        if (lastTextEditSnapshotBlockId != blockId) {
            pushUndoSnapshot()
            lastTextEditSnapshotBlockId = blockId
        }
    }

    /** Da chiamare prima di qualsiasi altra modifica (tipo, struttura, posizione, eliminazione...). */
    private fun snapshotForStructuralChange() {
        closeEditSession()
        pushUndoSnapshot()
        lastTextEditSnapshotBlockId = null
    }

    // --- Cronologia delle modifiche (la voce "Updates") ---
    //
    // **Una voce per sessione di scrittura, non per tasto premuto.**
    // Finché si continua a scrivere nello stesso blocco è una modifica
    // sola; si chiude quando il cursore passa a un altro blocco, quando
    // cambia la struttura della pagina o quando si esce dalla pagina.
    // È lo stesso confine che l'Annulla usa già, ed è quello giusto
    // anche qui: "Esmepoi → Esempio" è una correzione, non sette.

    private data class EditSession(val blockId: String, val before: String)

    private var editSession: EditSession? = null

    /** Il testo semplice di un blocco, o del titolo se l'id è quello riservato. */
    private fun textOfEditable(blockId: String): String =
        if (blockId == PageEditEntity.TITLE_BLOCK_ID) {
            _page.value?.title.orEmpty()
        } else {
            _blocks.value.find { it.id == blockId }?.let { plainTextOf(it) }.orEmpty()
        }

    private fun beginEditSession(blockId: String) {
        if (editSession?.blockId == blockId) return
        closeEditSession()
        editSession = EditSession(blockId, textOfEditable(blockId))
    }

    /**
     * Chiude la sessione aperta e, se qualcosa è cambiato davvero, ne
     * scrive la voce.
     *
     * **L'attesa prima di leggere il "dopo" è voluta.** Quello che si è
     * appena scritto viaggia verso il database e torna indietro dal
     * flusso dei blocchi: leggendo subito si prenderebbe il testo di un
     * attimo prima, e la cronologia racconterebbe una correzione
     * troncata all'ultima lettera. Qui un ritardo non costa niente —
     * nessuno sta aspettando questa riga — mentre sbagliarla la
     * renderebbe inservibile.
     */
    fun closeEditSession() {
        val session = editSession ?: return
        editSession = null
        val pageId = currentPageId ?: return
        // **Non su `viewModelScope`.** L'ultima sessione si chiude
        // proprio mentre si esce dalla pagina, e uscendo quello scope
        // viene annullato: la riga non verrebbe mai scritta. Vedi
        // `PageRepository.recordEditAfterSettling`.
        repository.recordEditAfterSettling(EDIT_SETTLE_MS) {
            val after = textOfEditable(session.blockId)
            if (after == session.before) {
                null
            } else {
                PageEditEntity(
                    pageId = pageId,
                    blockId = session.blockId,
                    before = session.before,
                    after = after
                )
            }
        }
    }

    /** Le modifiche fatte a questa pagina, dalla più recente. */
    private val _edits = MutableStateFlow<List<PageEditEntity>>(emptyList())
    val edits: StateFlow<List<PageEditEntity>> = _edits

    override fun onCleared() {
        closeEditSession()
        super.onCleared()
    }

    // Incrementato ad ogni annulla/ripristina. La UI lo osserva per
    // sapere che il prossimo contenuto in arrivo dal database va
    // accettato anche se ha una propria modifica in sospeso — è un
    // cambiamento voluto proveniente da fuori, non il database rimasto
    // indietro.
    private val _externalChangeTick = MutableStateFlow(0)
    val externalChangeTick: StateFlow<Int> = _externalChangeTick

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_blocks.value)
        // Annullare vuol dire tornare alla foto di prima: una riga nuova
        // ancora in sospeso non deve restare appiccicata sopra.
        pendingInserts.clear()
        lastTextEditSnapshotBlockId = null
        updateUndoRedoAvailability()
        _externalChangeTick.value += 1
        viewModelScope.launch { applySnapshot(previous) }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_blocks.value)
        pendingInserts.clear()
        lastTextEditSnapshotBlockId = null
        updateUndoRedoAvailability()
        _externalChangeTick.value += 1
        viewModelScope.launch { applySnapshot(next) }
    }

    private suspend fun applySnapshot(snapshot: List<BlockEntity>) {
        val pageId = currentPageId ?: return
        repository.replaceAllBlocks(pageId, snapshot)
    }

    fun load(pageId: String) {
        if (currentPageId == pageId) return
        currentPageId = pageId
        undoStack.clear()
        redoStack.clear()
        lastTextEditSnapshotBlockId = null
        updateUndoRedoAvailability()

        pageFlowJob?.cancel()
        pageFlowJob = viewModelScope.launch {
            if (repository.getPage(pageId) == null && pageId == PageEntity.ROOT_PAGE_ID) {
                // Primo avvio dell'app: la pagina radice (quella che
                // prima era la schermata Home separata) non esiste
                // ancora — la creiamo al volo con l'ID fisso, così
                // esiste sempre esattamente una volta.
                repository.createPage(
                    PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space", icon = "🏠")
                )
            }
            // Seguita nel tempo, non letta una volta sola: la stessa
            // pagina può stare aperta in due punti della pila di
            // navigazione, e le modifiche qui sotto salvano la pagina
            // intera. Chi resta indietro riscriverebbe il titolo (o
            // l'icona, o la copertina) vecchio sopra a quello nuovo.
            repository.observePage(pageId)
                .distinctUntilChanged()
                .collect { fromDb -> if (fromDb != null) _page.value = fromDb }
        }

        editsFlowJob?.cancel()
        editsFlowJob = viewModelScope.launch {
            repository.observeEdits(pageId).collect { _edits.value = it }
        }

        blocksFlowJob?.cancel()
        pendingInserts.clear()
        blocksFlowJob = viewModelScope.launch {
            repository.getBlocks(pageId).collect { loaded ->
                lastLoadedBlocks = loaded
                publishBlocks(loaded)
            }
        }
    }

    fun updateTitle(newTitle: String) {
        val current = _page.value ?: return
        // Il titolo non è un blocco ma si modifica come loro, quindi
        // entra nella cronologia con l'id riservato.
        beginEditSession(PageEditEntity.TITLE_BLOCK_ID)
        val updated = current.copy(title = newTitle)
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
    }

    // --- Le voci del menu "..." della pagina ---

    /** Blocca o sblocca il contenuto: da bloccata la pagina si legge ma non si scrive. */
    fun toggleLocked() {
        val current = _page.value ?: return
        val wanted = !current.isLocked
        _page.value = current.copy(isLocked = wanted)
        viewModelScope.launch { repository.setLocked(current.id, wanted) }
    }

    /** Blocca o sblocca l'impaginazione di un database (vista, ordine, filtro, colonne). */
    fun toggleViewLocked() {
        val current = _page.value ?: return
        val wanted = !current.isViewLocked
        _page.value = current.copy(isViewLocked = wanted)
        viewModelScope.launch { repository.setViewLocked(current.id, wanted) }
    }

    /**
     * Fa una copia della pagina, sottopagine comprese, e la mette dove si
     * è scelto. `onDone` riceve l'id della copia, per poterci andare subito.
     */
    fun duplicatePage(
        target: PageRepository.DuplicateTarget,
        titleSuffix: String,
        imageStore: PageImageStore,
        onDone: (String) -> Unit
    ) {
        val current = _page.value ?: return
        viewModelScope.launch {
            repository.duplicatePage(current.id, target, titleSuffix) { imageStore.copy(it) }?.let(onDone)
        }
    }

    /** Le pagine dentro cui questa si può spostare. */
    fun loadMoveDestinations(onReady: (List<PageEntity>) -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch { onReady(repository.moveDestinations(current.id)) }
    }

    fun movePageTo(destinationPageId: String, onDone: () -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch {
            repository.movePageTo(current.id, destinationPageId)
            onDone()
        }
    }

    /**
     * Butta la pagina nel cestino. La pagina resta nel database: quello
     * che sparisce è il collegamento che la mostrava. `onDone` serve a
     * tornare indietro, perché restare su una pagina che non è più
     * raggiungibile da nessuna parte non avrebbe senso.
     */
    fun moveToTrash(onDone: () -> Unit) {
        val current = _page.value ?: return
        closeEditSession()
        viewModelScope.launch {
            repository.moveToTrash(current.id)
            onDone()
        }
    }

    /** Dalla barra del cestino: la pagina torna in fondo al menu principale. */
    fun restoreFromTrash() {
        val current = _page.value ?: return
        viewModelScope.launch { repository.restoreFromTrash(current.id) }
    }

    /**
     * Dalla barra del cestino: la pagina se ne va per sempre, con le sue
     * immagini. `onDone` arriva a cancellazione finita — chi chiama torna
     * indietro, e questa pagina da lì in poi non esiste più.
     */
    fun deletePermanently(imageStore: PageImageStore, onDone: () -> Unit) {
        val current = _page.value ?: return
        closeEditSession()
        viewModelScope.launch {
            repository.deletePermanently(current.id).forEach { imageStore.delete(it) }
            onDone()
        }
    }

    /**
     * Il font del testo della pagina, dalla barra Aa (null = quello di
     * sistema). Scrive solo quella colonna, come `toggleLocked`; il valore
     * in mano alla UI cambia subito, così il testo si ridisegna senza
     * aspettare il giro dal database.
     */
    fun setPageFont(font: PageFont?) {
        val current = _page.value ?: return
        if (current.pageFont == font) return
        _page.value = current.copy(pageFont = font)
        viewModelScope.launch { repository.setPageFont(current.id, font) }
    }

    /**
     * Il corpo del testo della pagina, da 5 a 72. Il 16 di partenza si
     * salva come null, cioè "come prima": una pagina rimessa a 16 torna
     * uguale a una che non l'ha mai cambiato.
     */
    fun setPageFontSize(size: Int) {
        val current = _page.value ?: return
        val clamped = size.coerceIn(MIN_PAGE_FONT_SIZE, MAX_PAGE_FONT_SIZE)
        val stored = clamped.takeIf { it != DEFAULT_PAGE_FONT_SIZE }
        if (current.pageFontSize == stored) return
        _page.value = current.copy(pageFontSize = stored)
        viewModelScope.launch { repository.setPageFontSize(current.id, stored) }
    }

    /**
     * Il conteggio del testo della pagina, per la voce "X words" del menu
     * dei tre puntini. Letto dal database ogni volta che si apre il menu:
     * il testo si salva a ogni tasto, quindi lì è già tutto.
     */
    fun loadTextStats(onReady: (TextStats) -> Unit) {
        val pageId = currentPageId ?: return
        viewModelScope.launch { onReady(repository.textStats(pageId)) }
    }

    /**
     * Mette o toglie questa pagina dai preferiti.
     *
     * Scrive **solo quella colonna**: la stessa pagina può stare aperta
     * in due schermate, e riscriverla intera da qui rimetterebbe
     * indietro un titolo cambiato nell'altra. Il valore in mano alla UI
     * si aggiorna subito, così la stellina non aspetta il giro dal
     * database.
     */
    fun toggleFavorite() {
        val current = _page.value ?: return
        if (current.id == PageEntity.ROOT_PAGE_ID) return
        val wanted = !current.isFavorite
        _page.value = current.copy(isFavorite = wanted)
        viewModelScope.launch { repository.setFavorite(current.id, wanted) }
    }

    fun updateIcon(newIcon: String) {
        val current = _page.value ?: return
        val updated = current.copy(icon = newIcon)
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
    }

    /**
     * L'immagine dell'icona, o null per tornare all'emoji. Il nome del
     * file vecchio viene restituito a chi chiama, che è l'unico a
     * sapere dove stanno i file e può cancellarlo: lasciarlo lì
     * vorrebbe dire riempire la cartella di immagini irraggiungibili.
     */
    fun setIconImage(fileName: String?): String? {
        val current = _page.value ?: return null
        val previous = current.iconImage
        val updated = current.copy(iconImage = fileName)
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
        return previous?.takeIf { it != fileName }
    }

    /**
     * La copertina, o null per toglierla. Vedi `setIconImage`.
     *
     * L'inquadratura torna a zero: era stata scelta guardando l'altra
     * immagine, e su una nuova darebbe un ritaglio deciso a caso.
     */
    fun setCoverImage(fileName: String?): String? {
        val current = _page.value ?: return null
        val previous = current.coverImage
        val updated = current.copy(
            coverImage = fileName,
            coverScale = 1f,
            coverOffsetX = 0f,
            coverOffsetY = 0f
        )
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
        return previous?.takeIf { it != fileName }
    }

    /** Come è inquadrata la copertina: ingrandimento e spostamento. */
    fun setCoverTransform(scale: Float, offsetX: Float, offsetY: Float) {
        val current = _page.value ?: return
        val updated = current.copy(
            coverScale = scale,
            coverOffsetX = offsetX,
            coverOffsetY = offsetY
        )
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
    }

    /** Aggiorna il testo semplice di un blocco (senza formattazione inline). */
    fun updateBlockText(block: BlockEntity, newText: String) {
        updateBlockSpans(block, listOf(RichTextSpan(text = newText)))
    }

    /**
     * Aggiorna gli span (testo + formattazione inline) di un blocco.
     *
     * Scrive **solo la colonna del testo**, non l'entità intera. La
     * copia del blocco che ha in mano la schermata può essere di un
     * istante fa: risalvandola per intero si rimandavano indietro anche
     * il posto e la spunta di allora, e dopo un a-capo quel posto è
     * quello che nel frattempo ha preso la riga nuova.
     */
    fun updateBlockSpans(block: BlockEntity, spans: List<RichTextSpan>) {
        snapshotForTextEdit(block.id)
        val textJson = json.encodeToString(spans)
        viewModelScope.launch { repository.setBlockText(block.id, textJson) }
    }

    /**
     * Cambia il tipo di un blocco **scrivendo solo quella colonna**.
     *
     * Prima salvava il blocco intero, e il blocco che arriva qui è la
     * copia che la UI aveva in mano quando si è toccato il menu. Col
     * menu "/" questo voleva dire: si scrive `/toggle`, il campo toglie
     * la barra e il comando dal testo e lo salva, e subito dopo il cambio
     * di tipo **riscriveva il testo vecchio**, barra compresa. Era la "/"
     * che restava appesa dentro ogni toggle creato così. Scrivendo solo
     * il tipo, il testo resta quello che il campo ha appena salvato.
     */
    fun updateBlockType(block: BlockEntity, newType: BlockType) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.setBlockType(block.id, newType)
            // **Un toggle nuovo nasce aperto.** Lo stato aperto/chiuso sta
            // nel blocco e ci resta anche quando il blocco cambia tipo:
            // una riga che era stata un toggle chiuso, rifatta toggle,
            // rinasceva chiusa — e l'Invio nel titolo, invece di scendere
            // dentro, creava un secondo toggle sotto. Chi crea un toggle
            // ci vuole scrivere dentro subito.
            if (newType == BlockType.TOGGLE && block.type != BlockType.TOGGLE) {
                repository.setBlockExpanded(block.id, true)
            }
            // **Diventato toggle, si continua a scriverne il titolo.** Il
            // toggle è una riga a sé, fuori dal testo condiviso: il campo
            // in cui si stava scrivendo non è più il suo, e senza questa
            // richiesta la tastiera si chiudeva e bisognava ritoccare la
            // riga per cominciare. Su Notion invece si scrive subito.
            if (newType == BlockType.TOGGLE && block.type != BlockType.TOGGLE) {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = block.id
            }
        }
    }

    /**
     * Invio nel titolo di un toggle.
     *
     * Non è una divisione come per un paragrafo, e trattarlo così era
     * il guaio: la riga nuova nasceva **sopra** col testo prima del
     * cursore — cioè col titolo — e il toggle restava vuoto sotto. Il
     * titolo "usciva" dal toggle a ogni Invio.
     *
     * Come su Notion: a toggle **aperto** la riga nuova è il **primo
     * figlio**, dentro; a toggle **chiuso** è un **altro toggle** subito
     * sotto, perché dentro non si vedrebbe. Il testo dopo il cursore, se
     * c'è, passa alla riga nuova; il titolo tiene quello prima.
     */
    fun enterInToggle(toggle: BlockEntity, titleSpans: List<RichTextSpan>, textAfterCursor: String) {
        val current = _blocks.value.find { it.id == toggle.id } ?: toggle
        snapshotForStructuralChange()
        val newBlockId = java.util.UUID.randomUUID().toString()
        val newSpansJson = json.encodeToString(listOf(RichTextSpan(text = textAfterCursor)))
        val titleJson = json.encodeToString(titleSpans)
        // La riga nuova si vede subito, come per l'Invio dentro il toggle:
        // vedi `showBeforeSaved`. Il titolo va aggiornato insieme, perché
        // una lettura in ritardo lo riporterebbe col testo di prima
        // dell'Invio.
        showBeforeSaved(newBlockId) { list ->
            val titled = list.map { if (it.id == current.id) it.copy(textJson = titleJson) else it }
            if (current.isExpanded) {
                firstChildInList(titled, current.id, newBlockId, newSpansJson)
            } else {
                siblingBelowInList(titled, current.id, newBlockId, BlockType.TOGGLE, newSpansJson)
            }
        }
        viewModelScope.launch {
            val inserted = structuralEdits.withLock {
                repository.setBlockText(current.id, titleJson)
                if (current.isExpanded) {
                    repository.insertFirstChild(current, newBlockId, newSpansJson)
                    true
                } else {
                    repository.insertSiblingBelow(current, newBlockId, BlockType.TOGGLE, newSpansJson)
                }
            }
            if (!inserted) {
                forgetPendingInsert(newBlockId)
                return@launch
            }
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    /**
     * "Turn into page" dalle impostazioni di un database dentro questa
     * pagina: il blocco diventa un collegamento, e `onDone` apre il
     * database a schermo intero — è il senso della voce, vederlo come
     * una pagina a sé.
     */
    fun turnDatabaseIntoPage(block: BlockEntity, onDone: () -> Unit) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.turnDatabaseBlockIntoPage(block.id)
            onDone()
        }
    }

    /** Il primo figlio di un toggle aperto e vuoto, dal segnaposto "Empty toggle". */
    fun addFirstChild(toggle: BlockEntity) {
        snapshotForStructuralChange()
        val newBlockId = java.util.UUID.randomUUID().toString()
        val emptyJson = json.encodeToString(listOf(RichTextSpan(text = "")))
        showBeforeSaved(newBlockId) { list -> firstChildInList(list, toggle.id, newBlockId, emptyJson) }
        viewModelScope.launch {
            structuralEdits.withLock {
                repository.insertFirstChild(toggle, newBlockId, emptyJson)
            }
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    fun toggleCheckbox(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(block.copy(isChecked = !block.isChecked))
        }
    }

    /**
     * Le quattro cose che si possono fare a un elemento di elenco
     * numerato tenendo premuto sul suo numero, come su OneNote.
     *
     * Prendono l'id e non il blocco di proposito. Chi chiama — un dito
     * sullo schermo — può avere in mano una copia vecchia dell'entità,
     * catturata quando la zona toccabile è stata creata: scrivere
     * quella riporterebbe indietro anche il testo, cancellando ciò che
     * nel frattempo è stato digitato. Per lo stesso motivo i
     * salvataggi toccano **solo la colonna che cambia**.
     */
    fun beginNewListHere(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { repository.setNumberStart(blockId, 1) }
    }

    /** Toglie il "riparti da qui": il conteggio riprende da quello di prima. */
    fun continuePreviousList(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { repository.setNumberStart(blockId, null) }
    }

    /** Toglie il numero: la riga resta, ma come paragrafo normale. */
    fun removeNumber(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { repository.setBlockType(blockId, BlockType.PARAGRAPH) }
    }

    /**
     * La vista di un database appena creato dal menu "/". "Calendar
     * view" deve dare un calendario, non una tabella da cambiare a
     * mano: è l'unica differenza fra quelle voci.
     */
    fun setDatabaseLayout(databasePageId: String, layout: DatabaseLayout) {
        viewModelScope.launch {
            val page = repository.getPage(databasePageId) ?: return@launch
            repository.updatePage(page.copy(databaseLayout = layout))
        }
    }

    /** Da numero a pallino, testo compreso. */
    fun changeToBullet(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { repository.setBlockType(blockId, BlockType.BULLET_LIST_ITEM) }
    }

    /**
     * **A che numero è arrivato** un elemento di elenco numerato dentro
     * il suo livello: il primo del livello è 1, il secondo 2, e così
     * via. Non l'etichetta da mostrare — quella dipende dal livello e la
     * compone la schermata, che sa come si scrivono i numeri romani, le
     * lettere e "First".
     *
     * Ogni livello conta per conto suo e **riparte da capo** sotto ogni
     * elemento del livello sopra, come su OneNote: `indentLevel` decide
     * la profondità, quindi spostare un elemento a destra o a sinistra
     * lo rinumera da solo, e con lui tutti quelli che seguono.
     *
     * Il conteggio scorre i fratelli dello stesso gruppo in ordine,
     * tenendo un contatore per livello: scendendo di un rientro se ne
     * apre uno nuovo, risalendo quelli più profondi vengono dimenticati
     * (è così che il primo figlio riparte da uno sotto ogni padre).
     * `numberStartsAt` impone il valore all'elemento che lo porta. I
     * blocchi di altro tipo intercalati (un paragrafo in mezzo
     * all'elenco) non interrompono il conteggio — come in Notion.
     */
    fun numberedListOrdinal(allBlocks: List<BlockEntity>, block: BlockEntity): Int {
        val siblings = allBlocks
            .filter { it.parentBlockId == block.parentBlockId && it.type == BlockType.NUMBERED_LIST_ITEM }
            .sortedBy { it.orderIndex }

        val counters = mutableListOf<Int>()
        for (sibling in siblings) {
            val level = sibling.indentLevel.coerceIn(0, MAX_INDENT_LEVEL)
            // Se qualcuno salta un livello (due rientri in un colpo),
            // riempiamo i livelli intermedi con 1: meglio contare da uno
            // che lasciare un buco.
            while (counters.size < level) counters.add(1)
            if (counters.size > level) {
                while (counters.size > level + 1) counters.removeAt(counters.size - 1)
                counters[level] = sibling.numberStartsAt ?: (counters[level] + 1)
            } else {
                counters.add(sibling.numberStartsAt ?: 1)
            }

            if (sibling.id == block.id) return counters[level]
        }
        return 1
    }

    fun toggleExpanded(block: BlockEntity) {
        // Espandere/comprimere un toggle è solo uno stato di
        // visualizzazione, non contenuto — non entra nella cronologia
        // annulla/ripristina.
        //
        // **Solo la colonna dello stato**, e letta dall'ultima versione
        // in memoria: il blocco che arriva qui è quello che la freccia
        // aveva in mano quando è stata disegnata, e salvarlo intero
        // riporterebbe indietro il titolo scritto nel frattempo.
        val current = _blocks.value.find { it.id == block.id } ?: block
        viewModelScope.launch {
            repository.setBlockExpanded(current.id, !current.isExpanded)
        }
    }

    /**
     * Gestisce la pressione di Invio dentro un blocco. Se il blocco è un
     * elemento di lista (puntata, numerata, checkbox) vuoto, Invio esce
     * dalla lista trasformandolo in un paragrafo — come in qualsiasi
     * editor di note. Altrimenti, divide gli span nel punto del cursore:
     * la parte prima (con la sua formattazione intatta) resta in questo
     * blocco, la parte dopo il cursore diventa il testo iniziale
     * (semplice, senza formattazione) di un nuovo blocco creato subito
     * sotto, che eredita lo stesso tipo e lo stesso parentBlockId se si
     * tratta di una lista.
     */
    fun splitBlockAt(
        block: BlockEntity,
        spansBeforeCursor: List<RichTextSpan>,
        textAfterCursor: String
    ): SplitOutcome {
        val isListType = block.type == BlockType.BULLET_LIST_ITEM ||
            block.type == BlockType.NUMBERED_LIST_ITEM ||
            block.type == BlockType.CHECKBOX

        val textBeforeCursor = spansBeforeCursor.plainText()

        // **Cintura di sicurezza contro la perdita di testo.**
        //
        // "Invio su una riga vuota esce dalla lista" si decide guardando
        // quello che il campo dice di avere in mano. Se per un attimo
        // quella copia è sbagliata — e con la tastiera vera, che tiene
        // le parole in composizione, succede — una riga piena verrebbe
        // scambiata per vuota e **riscritta a zero**: il testo sparisce
        // e non va nemmeno a capo. È il difetto segnalato il 22/09/2026.
        //
        // Prima di svuotare si chiede quindi al database, che è l'unico
        // a sapere davvero cosa c'è scritto: se lì c'è del testo, non si
        // svuota niente e si divide normalmente.
        val fieldThinksEmpty = textBeforeCursor.isBlank() && textAfterCursor.isBlank()
        if (fieldThinksEmpty && plainTextOf(block).isNotBlank()) {
            // Il campo si crede vuoto ma nel database c'è del testo: è
            // la sua copia a essere indietro. Non si tocca niente e si
            // dice a chi chiama di rimettere il campo com'è davvero.
            return SplitOutcome.NOTHING
        }
        if (isListType && fieldThinksEmpty) {
            snapshotForStructuralChange()
            val spans = listOf(RichTextSpan(text = ""))
            viewModelScope.launch {
                repository.saveBlock(
                    block.copy(type = BlockType.PARAGRAPH, textJson = json.encodeToString(spans))
                )
                // Il blocco cambia specie: da "isola" (una riga tutta
                // sua) a testo scorrevole (dentro il campo condiviso
                // coi vicini). Per la UI sono due posti diversi, quindi
                // il campo di prima viene distrutto e con lui se ne va
                // il cursore — e la tastiera si chiude in faccia a chi
                // stava scrivendo. Lo richiamiamo qui.
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = block.id
            }
            return SplitOutcome.BECAME_PARAGRAPH
        }

        snapshotForStructuralChange()
        val pageId = currentPageId ?: return SplitOutcome.NOTHING
        val newBlockType = if (isListType) block.type else BlockType.PARAGRAPH

        // **La riga nuova nasce sopra, non sotto.**
        //
        // Sembra al contrario ed è il punto di tutta la faccenda. Ogni
        // isola — una casella da spuntare, per esempio — è un campo di
        // testo a sé. Creando la riga nuova sotto bisogna spostarci il
        // cursore, e spostare il cursore fra due campi **stacca e
        // riattacca la tastiera**: nel frattempo i tasti già premuti
        // finiscono nel campo vecchio, la maiuscola automatica si
        // rimette a zero (si vede la lettera passare da minuscola a
        // maiuscola) e il campo appena abbandonato viene distrutto
        // mentre la tastiera gli sta ancora chiedendo dov'è il cursore,
        // che è il modo in cui l'app si chiudeva di colpo.
        //
        // Mettendo invece la riga nuova **sopra** col testo prima del
        // cursore, e lasciando a questo blocco — che il fuoco ce l'ha
        // già — il testo dopo il cursore, sullo schermo non cambia
        // niente ma **il fuoco non si muove**: nessun cambio di campo,
        // nessuna tastiera staccata, niente da distruggere.
        //
        // Vale solo per i blocchi senza figli: se ne avessero,
        // resterebbero attaccati a questo blocco, che dopo la divisione
        // è la riga *di sotto*, e finirebbero sotto la riga sbagliata.
        val hasChildren = _blocks.value.any { it.parentBlockId == block.id }
        if (!hasChildren) {
            val newBlockId = java.util.UUID.randomUUID().toString()
            val textBeforeJson = json.encodeToString(spansBeforeCursor)
            val textAfterJson = json.encodeToString(listOf(RichTextSpan(text = textAfterCursor)))
            // La riga di sopra si vede nello stesso fotogramma in cui il
            // campo passa al testo dopo il cursore: vedi `showBeforeSaved`.
            showBeforeSaved(newBlockId) { list ->
                splitAboveInList(list, block.id, newBlockId, newBlockType, textBeforeJson, textAfterJson)
            }
            viewModelScope.launch {
                // In coda alle divisioni già in corso, una per volta:
                // due a-capo ravvicinati che calcolano gli indici
                // insieme si pestano i piedi.
                val split = structuralEdits.withLock {
                    repository.splitBlockAbove(
                        blockId = block.id,
                        newBlockId = newBlockId,
                        newBlockType = newBlockType,
                        textBeforeJson = textBeforeJson,
                        textAfterJson = textAfterJson
                    )
                }
                if (!split) forgetPendingInsert(newBlockId)
            }
            return SplitOutcome.SPLIT_ABOVE
        }

        val newIndex = block.orderIndex + 1
        val newBlockId = java.util.UUID.randomUUID().toString()

        viewModelScope.launch {
            val afterSpans = listOf(RichTextSpan(text = textAfterCursor))
            repository.applyBlockChanges(
                shift = PageRepository.BlockShift(pageId, block.parentBlockId, newIndex, 1),
                saved = listOf(
                    block.copy(textJson = json.encodeToString(spansBeforeCursor)),
                    BlockEntity(
                        id = newBlockId,
                        pageId = pageId,
                        parentBlockId = block.parentBlockId,
                        orderIndex = newIndex,
                        type = newBlockType,
                        textJson = json.encodeToString(afterSpans)
                    )
                )
            )

            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
        return SplitOutcome.SPLIT
    }

    /**
     * Come splitBlockAt, ma per quando il testo arrivato in un colpo solo
     * (tipicamente un incolla) contiene più righe: la prima resta nel
     * blocco corrente, ognuna delle successive diventa un nuovo blocco
     * fratello — tutte ereditando lo stesso tipo se il blocco di partenza
     * era una lista. Incollare tre righe dentro un elenco puntato crea
     * tre punti separati, non un blocco unico con newline dentro.
     */
    fun splitBlockAtMultiple(currentBlock: BlockEntity, lines: List<String>) {
        if (lines.size <= 1) {
            updateBlockText(currentBlock, lines.firstOrNull() ?: "")
            return
        }

        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        val inheritedType = when (currentBlock.type) {
            BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.CHECKBOX ->
                currentBlock.type
            else -> BlockType.PARAGRAPH
        }
        val newBlockIds = lines.drop(1).map { java.util.UUID.randomUUID().toString() }

        viewModelScope.launch {
            val startIndex = currentBlock.orderIndex + 1
            val firstSpans = listOf(RichTextSpan(text = lines.first()))
            repository.applyBlockChanges(
                shift = PageRepository.BlockShift(
                    pageId,
                    currentBlock.parentBlockId,
                    startIndex,
                    lines.size - 1
                ),
                saved = listOf(currentBlock.copy(textJson = json.encodeToString(firstSpans))) +
                    lines.drop(1).mapIndexed { i, lineText ->
                        BlockEntity(
                            id = newBlockIds[i],
                            pageId = pageId,
                            parentBlockId = currentBlock.parentBlockId,
                            type = inheritedType,
                            textJson = json.encodeToString(listOf(RichTextSpan(text = lineText))),
                            orderIndex = startIndex + i
                        )
                    }
            )

            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockIds.last()
        }
    }

    /** Aggiunge un blocco figlio in coda dentro un toggle (es. il primo, se è ancora vuoto). */
    fun addChildBlock(parentBlock: BlockEntity, initialText: String = "") {
        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        val siblingCount = _blocks.value.count { it.parentBlockId == parentBlock.id }
        val newBlockId = java.util.UUID.randomUUID().toString()

        viewModelScope.launch {
            val spans = listOf(RichTextSpan(text = initialText))
            repository.saveBlock(
                BlockEntity(
                    id = newBlockId,
                    pageId = pageId,
                    parentBlockId = parentBlock.id,
                    orderIndex = siblingCount,
                    textJson = json.encodeToString(spans)
                )
            )
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    /**
     * Trasforma un blocco in divisore E crea subito un paragrafo vuoto
     * appena dopo, mettendoci il focus — un divisore non ha campo di
     * testo, quindi senza questo passaggio in più il blocco appena
     * convertito (specie se era l'ultimo della pagina) lascerebbe
     * l'utente senza nulla su cui continuare a scrivere.
     */
    fun convertToDividerAndFocusNext(block: BlockEntity) {
        snapshotForStructuralChange()
        val newBlockId = java.util.UUID.randomUUID().toString()

        viewModelScope.launch {
            // In coda alle altre modifiche di struttura, e senza riscrivere
            // il testo: vedi `turnIntoDividerWithLineBelow`.
            structuralEdits.withLock {
                repository.turnIntoDividerWithLineBelow(block.id, newBlockId)
            }
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    /**
     * Indenta un blocco: diventa figlio del fratello immediatamente
     * precedente, in coda ai suoi figli attuali. Se non c'è un fratello
     * precedente (il blocco è già il primo del suo gruppo), non fa nulla
     * — non c'è nulla sotto cui annidarlo.
     */
    /**
     * Indenta un blocco. Per i tipi di testo scorrevole (paragrafo,
     * titoli, elenchi), è solo un numero di livello sul blocco stesso —
     * come Tab in Word, non una vera nidificazione — dato che questi
     * tipi ora vivono in un campo di testo condiviso con i vicini, non
     * più in righe strutturalmente separate. Per le "isole" (oggi solo
     * TOGGLE) resta il vecchio comportamento: diventa figlio del
     * fratello immediatamente precedente.
     */
    fun indentBlock(block: BlockEntity) {
        if (isFlowingTextType(block.type)) {
            snapshotForStructuralChange()
            viewModelScope.launch {
                repository.saveBlock(block.copy(indentLevel = (block.indentLevel + 1).coerceAtMost(MAX_INDENT_LEVEL)))
            }
            return
        }

        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val currentPos = siblings.indexOfFirst { it.id == block.id }
        if (currentPos <= 0) return
        val newParent = siblings[currentPos - 1]

        snapshotForStructuralChange()
        val newParentChildren = _blocks.value.filter { it.parentBlockId == newParent.id }
        val newOrderIndex = (newParentChildren.maxOfOrNull { it.orderIndex } ?: -1) + 1

        viewModelScope.launch {
            repository.saveBlock(block.copy(parentBlockId = newParent.id, orderIndex = newOrderIndex))
            // Indentare sposta il blocco da "cima pagina" a "dentro un
            // altro blocco" — posizioni strutturalmente diverse nella UI,
            // quindi Compose ricrea il campo di testo da zero perdendo il
            // focus, e con esso la tastiera si chiude da sola. Richiedere
            // di nuovo il focus qui lo riporta su.
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = block.id
        }
    }

    /** Come indentBlock ma al contrario. Per i tipi di testo scorrevole, decrementa il livello (minimo 0). */
    fun outdentBlock(block: BlockEntity) {
        if (isFlowingTextType(block.type)) {
            if (block.indentLevel <= 0) return
            snapshotForStructuralChange()
            viewModelScope.launch {
                repository.saveBlock(block.copy(indentLevel = block.indentLevel - 1))
            }
            return
        }

        val currentParentId = block.parentBlockId ?: return
        val currentParent = _blocks.value.find { it.id == currentParentId } ?: return
        val newParentId = currentParent.parentBlockId
        val insertAtIndex = currentParent.orderIndex + 1

        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        viewModelScope.launch {
            // Stessa transazione unica della divisione: vedi
            // `BlockDao.shiftOrderIndexes`. Qui il blocco che si sposta
            // è "nuovo" per il gruppo che lo accoglie, quindi entra fra
            // i blocchi da inserire e non fra quelli da aggiornare.
            repository.applyBlockChanges(
                shift = PageRepository.BlockShift(pageId, newParentId, insertAtIndex, 1),
                saved = listOf(
                    block.copy(parentBlockId = newParentId, orderIndex = insertAtIndex)
                )
            )
            // Stesso motivo di indentBlock: disindentare sposta il
            // blocco tra due posizioni strutturalmente diverse nella UI,
            // il campo di testo viene ricreato senza focus e la tastiera
            // si chiude da sola. Richiediamo di nuovo il focus qui.
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = block.id
        }
    }

    /** Scambia la posizione di un blocco con il fratello immediatamente precedente. */
    fun moveBlockUp(block: BlockEntity) {
        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val currentPos = siblings.indexOfFirst { it.id == block.id }
        if (currentPos <= 0) return
        val previous = siblings[currentPos - 1]

        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(block.copy(orderIndex = previous.orderIndex))
            repository.saveBlock(previous.copy(orderIndex = block.orderIndex))
        }
    }

    /** Scambia la posizione di un blocco con il fratello immediatamente successivo. */
    fun moveBlockDown(block: BlockEntity) {
        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val currentPos = siblings.indexOfFirst { it.id == block.id }
        if (currentPos < 0 || currentPos >= siblings.size - 1) return
        val next = siblings[currentPos + 1]

        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(block.copy(orderIndex = next.orderIndex))
            repository.saveBlock(next.copy(orderIndex = block.orderIndex))
        }
    }

    fun deleteBlock(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch { repository.deleteBlock(block) }
    }

    /**
     * Elimina un database mostrato dentro la pagina: sia il blocco sia
     * la pagina-database che contiene, con le sue colonne e le sue
     * righe (se ne occupa il database, a cascata).
     *
     * Qui si cancella tutto, a differenza di un collegamento a pagina,
     * dove togliere il blocco lascia la pagina al suo posto. La
     * differenza è che lì il blocco è un rimando a qualcosa che vive
     * per conto suo, mentre un database incorporato **è** quel
     * contenuto: lasciarne i dati vorrebbe dire lasciarli in un posto
     * da cui non si può più arrivare.
     */
    fun deleteDatabaseBlock(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            block.linkedPageId?.let { linkedId ->
                repository.getPage(linkedId)?.let { repository.deletePage(it) }
            }
            repository.deleteBlock(block)
        }
    }

    /**
     * Elimina un blocco e sposta il focus sul fratello precedente, se
     * esiste — chiamato dal pulsante "elimina blocco vuoto" nella barra
     * (prima intercettava il tasto Backspace direttamente sul campo di
     * testo, ma su alcune tastiere — Samsung inclusa — quell'intercettazione
     * disturbava la connessione con l'input).
     */
    fun deleteBlockAndFocusPrevious(block: BlockEntity) {
        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val currentPos = siblings.indexOfFirst { it.id == block.id }
        // Solo tra i fratelli PRIMA di questa posizione, e solo quelli
        // con un campo di testo vero (non divisori/tabelle/link, che
        // non hanno nulla su cui il focus possa atterrare).
        val previous = siblings
            .subList(0, currentPos.coerceAtLeast(0))
            .lastOrNull { isFocusableBlockType(it.type) }

        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.deleteBlock(block)
            if (previous != null) {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = previous.id
            }
        }
    }

    /**
     * Porta il cursore in fondo alla pagina — chiamato toccando lo
     * spazio vuoto sotto l'ultimo blocco, così tutta l'area dal titolo
     * in giù è scrivibile e non solo i confini esatti dei blocchi.
     *
     * Conta l'**ultimo** blocco, non l'ultimo scrivibile: se la pagina
     * finisce con qualcosa su cui il cursore non può atterrare (un
     * database, un divisore, una tabella, un collegamento) va creato un
     * paragrafo nuovo **dopo** di lui. Cercare l'ultimo scrivibile
     * riportava invece a scrivere sopra quell'elemento, che è l'opposto
     * di quello che si intende toccando sotto.
     */
    fun focusLastBlock() {
        val topLevel = _blocks.value
            .filter { it.parentBlockId == null }
            .sortedBy { it.orderIndex }
        val last = topLevel.lastOrNull()

        if (last != null && isFocusableBlockType(last.type)) {
            viewModelScope.launch {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = last.id
            }
            return
        }

        // L'ultimo blocco non accetta il cursore (un database, un
        // divisore, una tabella, un collegamento): ne creiamo uno nuovo
        // dopo di lui, che è dove si intende scrivere toccando sotto.
        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        val newIndex = (topLevel.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val newBlockId = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.saveBlock(BlockEntity(id = newBlockId, pageId = pageId, orderIndex = newIndex))
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    fun consumeFocusRequest() {
        _focusRequestBlockId.value = null
    }

    /**
     * I tipi di blocco senza un campo di testo editabile — richiedere il
     * focus su uno di questi manderebbe in crash la UI (nessun
     * FocusRequester collegato). Usato da focusLastBlock e
     * deleteBlockAndFocusPrevious per scegliere sempre un blocco valido.
     */
    private fun isFocusableBlockType(type: BlockType): Boolean =
        type != BlockType.DIVIDER &&
            type != BlockType.TABLE &&
            type != BlockType.PAGE_LINK &&
            type != BlockType.DATABASE_LINK

    /**
     * I tipi "di testo scorrevole": vivono uniti in un unico campo di
     * testo condiviso con i vicini consecutivi dello stesso gruppo,
     * invece che ciascuno nel proprio campo separato — è quello che
     * rende selezione e backspace-tra-blocchi naturali. Tutti gli altri
     * tipi restano "isole" separate, invariate.
     */
    /**
     * I tipi che vivono dentro il campo di testo condiviso.
     *
     * **Deve restare uguale a `FLOWING_TYPES` nella schermata.** Sono
     * due elenchi perché uno decide cosa disegnare e l'altro cosa
     * salvare, ma se divergono il campo mostra righe che il ViewModel
     * non riconosce come sue e le modifiche spariscono in silenzio: è
     * successo mettendoci le caselle da spuntare e aggiornando solo
     * l'elenco della schermata.
     */
    fun isFlowingTextType(type: BlockType): Boolean =
        type == BlockType.PARAGRAPH ||
            type == BlockType.HEADING_1 ||
            type == BlockType.HEADING_2 ||
            type == BlockType.HEADING_3 ||
            type == BlockType.BULLET_LIST_ITEM ||
            type == BlockType.NUMBERED_LIST_ITEM ||
            type == BlockType.CHECKBOX

    /**
     * Applica a un gruppo di blocchi di testo scorrevole (mostrati
     * uniti in un unico campo nell'editor) la stessa modifica subita
     * dal testo combinato, passando dalle righe attuali a newLines.
     * Confronta quante righe iniziali e finali sono rimaste identiche
     * per isolare la porzione davvero cambiata (di solito pochissime
     * righe, anche in un gruppo lungo), poi:
     *  - stesso numero di righe coinvolte quanto blocchi: aggiorna
     *    solo il testo di ciascuno (digitazione normale)
     *  - più righe che blocchi: le righe in eccesso diventano nuovi
     *    blocchi (Invio premuto, o incolla multi-riga)
     *  - meno righe che blocchi: i blocchi in eccesso vengono
     *    eliminati, il loro contenuto è già confluito nelle righe
     *    superstiti (backspace che unisce due righe, o una selezione
     *    a cavallo di più blocchi cancellata) — è esattamente questo
     *    terzo caso a far funzionare backspace-tra-blocchi in modo
     *    nativo, senza bisogno di intercettare nessun tasto: è solo
     *    una conseguenza di come il testo è cambiato, rilevata allo
     *    stesso modo affidabile con cui rileviamo qualsiasi modifica.
     */
    /**
     * Applica una formattazione (grassetto/corsivo/...) a un intervallo
     * del campo di testo unito, traducendolo nei corrispondenti
     * intervalli sui singoli blocchi che lo compongono: la selezione
     * dell'utente è una sola, ma può attraversare più blocchi, ognuno
     * dei quali conserva i propri span separatamente.
     */
    fun applyFormatToRun(runBlockIds: List<String>, selStart: Int, selEnd: Int, type: FormatType) {
        if (selStart >= selEnd) return
        val all = _blocks.value
        val runBlocks = runBlockIds.mapNotNull { id -> all.find { it.id == id } }
        if (runBlocks.isEmpty()) return

        snapshotForStructuralChange()

        var pos = 0
        val updates = mutableListOf<BlockEntity>()
        for (block in runBlocks) {
            val text = plainTextOf(block)
            val blockStart = pos
            val blockEnd = pos + text.length
            pos = blockEnd + 1 // +1 per il newline che separa i blocchi

            val overlapStart = maxOf(blockStart, selStart) - blockStart
            val overlapEnd = minOf(blockEnd, selEnd) - blockStart
            if (overlapStart >= overlapEnd) continue

            val spans = spansOf(block)
            val newSpans = when (type) {
                FormatType.BOLD -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.bold }) { s, v -> s.copy(bold = v) }
                FormatType.ITALIC -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.italic }) { s, v -> s.copy(italic = v) }
                FormatType.UNDERLINE -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.underline }) { s, v -> s.copy(underline = v) }
                FormatType.STRIKETHROUGH -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.strikethrough }) { s, v -> s.copy(strikethrough = v) }
                FormatType.SPOILER -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.spoiler }) { s, v -> s.copy(spoiler = v) }
            }
            updates.add(block.copy(textJson = json.encodeToString(newSpans)))
        }

        viewModelScope.launch {
            updates.forEach { repository.saveBlock(it) }
        }
    }

    /**
     * Il gruppo di testo scorrevole reale a cui appartengono gli id
     * indicati. La lista di id arriva dalla UI, che può essere indietro
     * di un istante rispetto allo stato reale (es. si scrive subito
     * dopo un Invio, prima che la ricomposizione abbia aggiornato il
     * gruppo). Invece di fidarcene — che faceva sparire la modifica in
     * silenzio, ed è la ragione per cui l'Invio e la continuazione
     * delle liste sembravano "saltare" — ricostruiamo il gruppo dallo
     * stato corrente: la sequenza consecutiva di blocchi di testo
     * scorrevole che contiene il primo blocco ancora esistente tra
     * quelli indicati.
     */
    private fun resolveRun(runBlockIds: List<String>): List<BlockEntity>? {
        val all = _blocks.value
        val anchor = runBlockIds.firstNotNullOfOrNull { id -> all.find { it.id == id } } ?: return null
        val siblings = all
            .filter { it.parentBlockId == anchor.parentBlockId }
            .sortedBy { it.orderIndex }
        val anchorPos = siblings.indexOfFirst { it.id == anchor.id }
        if (anchorPos < 0) return null

        var start = anchorPos
        while (start > 0 && isFlowingTextType(siblings[start - 1].type)) start--
        var endExclusive = anchorPos
        while (endExclusive < siblings.size && isFlowingTextType(siblings[endExclusive].type)) endExclusive++
        return siblings.subList(start, endExclusive).ifEmpty { null }
    }

    /**
     * Backspace premuto a inizio di una riga del testo unito. Invece di
     * fondere subito la riga con quella sopra, il gesto la "smonta" un
     * gradino alla volta, come in Notion:
     *  1. se è un elemento di elenco, ne toglie il marcatore;
     *  2. altrimenti, se è rientrata, le toglie un rientro — e si può
     *     continuare, un livello per volta, fino al margine;
     *  3. quando non c'è più niente da togliere restituisce false, e la
     *     fusione con la riga sopra avviene normalmente.
     *
     * Restituisce true quando ha fatto qualcosa: è il segnale al campo
     * che la fusione non deve avvenire e che il testo va rimesso com'era.
     * La decisione sta qui e non nella UI perché solo qui il gruppo di
     * blocchi è quello aggiornato (vedi resolveRun). I salvataggi
     * toccano solo la colonna che cambia: scrivere l'intera entità
     * riporterebbe indietro anche il testo, se la copia in memoria
     * fosse più vecchia di quello che si sta scrivendo.
     */
    fun backspaceAtLineStart(runBlockIds: List<String>, lineIndex: Int): Boolean =
        backspaceAtLineStartDetailed(runBlockIds, lineIndex) != BackspaceOutcome.NOTHING

    /**
     * Come sopra, ma dice **cosa** ha fatto.
     *
     * A chi chiama serve distinguere un gradino tolto (il campo resta
     * dov'è) da una fusione con l'isola di sopra: lì questa riga sparisce
     * e con lei il campo di testo che sta chiamando. Un campo distrutto
     * mentre ha il fuoco fa chiudere l'app, quindi prima il fuoco va
     * posato altrove — vedi "il posteggio della tastiera".
     */
    fun backspaceAtLineStartDetailed(
        runBlockIds: List<String>,
        lineIndex: Int
    ): BackspaceOutcome {
        val block = resolveRun(runBlockIds)?.getOrNull(lineIndex)
            ?: return BackspaceOutcome.NOTHING
        val isListItem = block.type == BlockType.BULLET_LIST_ITEM ||
            block.type == BlockType.NUMBERED_LIST_ITEM ||
            // Backspace a inizio di una casella toglie il quadratino e
            // lascia il testo, come per gli altri marcatori. Prima era
            // un ramo a parte (`backspaceAtIslandStart`) perché la
            // casella viveva fuori dal campo condiviso.
            block.type == BlockType.CHECKBOX

        when {
            isListItem -> {
                snapshotForStructuralChange()
                viewModelScope.launch {
                    repository.setBlockType(block.id, BlockType.PARAGRAPH)
                }
            }
            block.indentLevel > 0 -> {
                snapshotForStructuralChange()
                viewModelScope.launch {
                    repository.setIndentLevel(block.id, block.indentLevel - 1)
                }
            }
            // Prima riga del gruppo, e sopra c'è un blocco che vive per
            // conto suo (una casella da spuntare, un toggle): la fusione
            // normale non lo raggiunge, perché quella sa muoversi solo
            // dentro il campo di testo condiviso. Senza questo ramo il
            // backspace lì non faceva niente — sembrava di essere in
            // cima alla pagina mentre sopra c'era ancora roba.
            lineIndex == 0 -> return if (mergeIntoIslandAbove(block)) {
                BackspaceOutcome.MERGED_INTO_ISLAND
            } else {
                BackspaceOutcome.NOTHING
            }
            else -> return BackspaceOutcome.NOTHING
        }
        return BackspaceOutcome.STEP_UNDONE
    }

    /**
     * Fonde `block` (la prima riga di un gruppo di testo) con il blocco
     * isola che ha sopra: il testo si attacca in coda a quello
     * dell'isola, e la riga sparisce.
     *
     * Dentro le caselle da spuntare il testo si può scrivere ma non si
     * poteva fondere: è la stessa separazione che impedisce la
     * selezione fra un blocco e l'altro. Qui almeno il backspace
     * attraversa il confine, così si può cancellare all'indietro senza
     * trovare muri.
     */
    private fun mergeIntoIslandAbove(block: BlockEntity): Boolean {
        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val position = siblings.indexOfFirst { it.id == block.id }
        if (position <= 0) return false
        val above = siblings[position - 1]
        // Le isole "contenitore" (tabelle, divisori, link, database) non
        // hanno un testo in cui fondersi: lì non si fa niente, come
        // prima.
        if (above.type != BlockType.CHECKBOX && above.type != BlockType.TOGGLE) return false

        val aboveSpans = spansOf(above)
        val joinAt = aboveSpans.plainText().length
        val merged = aboveSpans + spansOf(block)

        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.applyBlockChanges(
                saved = listOf(above.copy(textJson = json.encodeToString(merged))),
                deleted = listOf(block)
            )
            // Il cursore va dove i due testi si sono uniti, non in
            // fondo: è lì che l'utente stava cancellando. Senza attesa,
            // per lo stesso motivo di `backspaceAtIslandStart`: la riga
            // che si fonde sparisce e la tastiera va riagganciata
            // all'altra prima che si chiuda.
            _pendingCaret.value = above.id to joinAt
            _focusRequestBlockId.value = above.id
        }
        return true
    }

    /**
     * Backspace a inizio di una casella da spuntare: toglie la casella
     * e lascia il testo, che diventa una riga normale — lo stesso
     * gradino che il testo unito fa già per gli elenchi. Da lì in poi
     * la riga è testo come gli altri e si fonde con quella sopra.
     *
     * Restituisce true se il campo che ha chiamato **sta per essere
     * distrutto senza un altro posto dove mandare il fuoco**, e quindi
     * deve posarlo sul campo invisibile. La riga vuota dentro un toggle
     * restituisce false anche quando sparisce: lì il fuoco va sulla riga
     * di sopra prima che la riga venga tolta.
     */
    fun backspaceAtIslandStart(block: BlockEntity): Boolean {
        if (block.type == BlockType.TOGGLE) return unmakeToggle(block)
        if (block.type != BlockType.CHECKBOX) return removeEmptyNestedLine(block)
        snapshotForStructuralChange()
        viewModelScope.launch {
            // Solo la colonna del tipo: il testo resta quello che c'è
            // adesso nel database, non la copia che ha in mano la UI.
            repository.setBlockType(block.id, BlockType.PARAGRAPH)
            // **Senza attesa.** Qui il campo muore per forza: il blocco
            // passa da riga a sé a testo scorrevole, che è un'altra
            // composable. L'unica cosa che si può fare è far riprendere
            // il fuoco al campo nuovo **prima** che la tastiera faccia
            // in tempo a chiudersi — l'animazione di chiusura dura
            // circa due decimi di secondo, quindi ogni millisecondo di
            // attesa qui è un pezzo di tastiera che sparisce in faccia.
            _focusRequestBlockId.value = block.id
        }
        return true
    }

    /**
     * Backspace a inizio del titolo di un toggle: **il toggle torna testo
     * normale**, col suo titolo, e le righe che conteneva escono e
     * restano subito sotto, allo stesso livello. Come su Notion.
     *
     * Prima non succedeva niente, e per un toggle col titolo scritto non
     * c'era nessun modo di toglierlo: il cestino della barra compare solo
     * sulle righe vuote. Così invece si fa com'è naturale — un backspace
     * lo trasforma, un altro lo unisce alla riga sopra.
     *
     * Le righe interne **non si cancellano** di proposito: un backspace
     * che si porta via mezza pagina nascosta dentro un toggle chiuso
     * sarebbe il modo peggiore di perdere del testo.
     */
    private fun unmakeToggle(block: BlockEntity): Boolean {
        val current = _blocks.value.find { it.id == block.id } ?: return false
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock {
                repository.unnestAndConvert(current.id, BlockType.PARAGRAPH)
            }
            // Il cursore resta **a inizio riga**, dov'era quando si è
            // premuto: così il backspace successivo unisce la riga a
            // quella sopra, invece di mangiare l'ultima lettera del
            // titolo come farebbe col cursore in fondo.
            _pendingCaret.value = current.id to 0
            // Senza attesa, per la stessa ragione della casella da
            // spuntare qui sopra: il campo di prima è già morto, e ogni
            // istante perso è un pezzo di tastiera che si chiude.
            _focusRequestBlockId.value = current.id
        }
        return true
    }

    /**
     * Backspace a inizio di una riga **vuota dentro un toggle**: la riga
     * sparisce e il cursore sale su quella di sopra — o sul titolo del
     * toggle, se era la prima.
     *
     * Prima non succedeva niente: le righe dentro un toggle sono campi a
     * sé, fuori dal testo condiviso, e il backspace a inizio riga lì
     * veniva ignorato. Una riga vuota creata per sbaglio non si riusciva
     * più a togliere.
     *
     * Solo se è vuota: con del testo dentro bisognerebbe fonderlo con la
     * riga sopra, che è un altro campo, ed è un lavoro a parte. Restituisce
     * sempre false: il campo non deve posare il fuoco da nessuna parte,
     * perché glielo prende direttamente la riga di sopra.
     */
    private fun removeEmptyNestedLine(block: BlockEntity): Boolean {
        val parentId = block.parentBlockId ?: return false
        if (block.type == BlockType.TOGGLE) return false
        val current = _blocks.value.find { it.id == block.id } ?: return false
        if (plainTextOf(current).isNotEmpty()) return false

        val previousSibling = _blocks.value
            .filter { it.parentBlockId == parentId && it.orderIndex < current.orderIndex }
            .filter { isFocusableBlockType(it.type) }
            .maxByOrNull { it.orderIndex }
        val focusTarget = previousSibling?.id ?: parentId
        // La tastiera a volte manda lo stesso backspace due volte, a due
        // millisecondi di distanza (si vede nel registro): la seconda
        // troverebbe la riga ancora lì e la toglierebbe un'altra volta.
        if (!removingNestedLines.add(current.id)) return false

        // **Prima il cursore sale, poi la riga sparisce.**
        //
        // Prima era il contrario: il fuoco si posava sul campo invisibile,
        // la riga veniva cancellata, e 60 ms dopo il cursore andava sulla
        // riga di sopra. Per tutto quel tempo nessuna riga aveva il fuoco,
        // e la barra sopra la tastiera — che si vede solo se c'è una riga
        // a fuoco — **spariva e tornava**: misurato, cento millisecondi,
        // quattro fotogrammi del video senza barra. Era lo scatto più forte
        // del solito che si vedeva salendo col backspace.
        //
        // Adesso il cursore passa direttamente da questa riga a quella di
        // sopra, in un colpo solo: nessun istante senza fuoco, nessun campo
        // intermedio. La riga si cancella **dopo**, quando non ha più il
        // cursore — distruggere il campo che ce l'ha fa chiudere l'app.
        snapshotForStructuralChange()
        val target = _blocks.value.find { it.id == focusTarget }
        // In fondo alla riga di sopra, dove un backspace deve portare.
        _pendingCaret.value = focusTarget to (target?.let { plainTextOf(it).length } ?: 0)
        _focusRequestBlockId.value = focusTarget
        viewModelScope.launch {
            try {
                // Il `true` in fondo non è decorativo: la richiesta servita
                // diventa `null`, e restituendo quella `withTimeoutOrNull`
                // non si distinguerebbe più dal tempo scaduto — la riga
                // non veniva mai tolta.
                val moved = withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS) {
                    focusRequestBlockId.first { it != focusTarget }
                    true
                } ?: false
                if (!moved) {
                    // La riga di sopra non ha preso il fuoco: questa lo ha
                    // ancora, e cancellarla adesso sarebbe il crash. Resta
                    // dov'è; un altro backspace riproverà.
                    if (_focusRequestBlockId.value == focusTarget) _focusRequestBlockId.value = null
                    if (_pendingCaret.value?.first == focusTarget) _pendingCaret.value = null
                    return@launch
                }
                repository.deleteBlock(current)
            } finally {
                removingNestedLines.remove(current.id)
            }
        }
        // Il campo resta vivo finché il fuoco non se n'è andato: non deve
        // posarlo sul campo invisibile. Vedi `backspaceAtIslandStart`.
        return false
    }

    /** Le righe dentro un toggle che un backspace sta già togliendo. */
    private val removingNestedLines = mutableSetOf<String>()

    /**
     * L'utente ha appena scritto "5." (o "1.", o qualsiasi numero) come
     * unica cosa in un blocco: quel blocco diventa un elemento di elenco
     * numerato che parte da quel numero, e il "5." scritto a mano sparisce
     * perché da lì in poi lo disegna la lista.
     *
     * Vale solo sui paragrafi: su un blocco che è già un elenco, o su un
     * titolo, non si converte niente. Restituisce true se ha convertito,
     * così il campo sa che deve togliere il testo digitato.
     */
    fun startNumberedListAtLine(runBlockIds: List<String>, lineIndex: Int, startNumber: Int): Boolean {
        val block = resolveRun(runBlockIds)?.getOrNull(lineIndex) ?: return false
        if (block.type != BlockType.PARAGRAPH) return false
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(
                block.copy(
                    type = BlockType.NUMBERED_LIST_ITEM,
                    numberStartsAt = startNumber,
                    textJson = json.encodeToString(listOf(RichTextSpan(text = "")))
                )
            )
        }
        return true
    }

    /**
     * L'utente ha appena chiuso un `||qualcosa||` su una riga del testo
     * unito: le quattro barre spariscono — erano un comando, non roba
     * scritta — e quello che stava in mezzo resta, coperto.
     *
     * `lineText` è la riga **già ripulita** dalle barre, cioè quella che
     * il campo mostra da adesso; `spoilerStart`/`spoilerEnd` sono le
     * posizioni del testo da coprire dentro quella riga. Passarla già
     * fatta evita di rifare qui lo stesso conto che ha fatto il campo e
     * di rischiare che i due non coincidano.
     */
    fun applySpoilerAtLine(
        runBlockIds: List<String>,
        lineIndex: Int,
        lineText: String,
        spoilerStart: Int,
        spoilerEnd: Int
    ): Boolean {
        val block = resolveRun(runBlockIds)?.getOrNull(lineIndex) ?: return false
        snapshotForTextEdit(block.id)
        val spans = setFormatInRange(
            applyTextEdit(spansOf(block), plainTextOf(block), lineText),
            spoilerStart,
            spoilerEnd
        ) { s, _ -> s.copy(spoiler = true) }
        viewModelScope.launch {
            // Solo la colonna del testo: qui non cambia nient'altro del
            // blocco, e riscriverlo tutto rischierebbe di riportare
            // indietro un tipo o un rientro cambiati nel frattempo.
            repository.setBlockText(block.id, json.encodeToString(spans))
        }
        return true
    }

    /**
     * `linesBefore` è quello che c'era scritto **nel campo** un attimo
     * prima, cioè quello che l'utente vedeva. Non è un doppione dei
     * testi salvati: il salvataggio va a giro per il database e può
     * essere indietro di qualche decina di millisecondi, e in quella
     * finestra i due non coincidono. Il confronto serve a capire
     * *quale riga è cambiata*, quindi va fatto su quello che l'utente
     * aveva davanti: con i testi salvati, una riga appena scritta ma
     * non ancora salvata risultava diversa, il confronto si
     * disallineava di una posizione, e la riga nuova nata dall'Invio
     * ereditava il tipo dalla riga sbagliata — è il motivo per cui
     * "0." creava la lista ma poi l'Invio dava un paragrafo invece di
     * continuarla. Se il campo e i blocchi non hanno nemmeno lo stesso
     * numero di righe, la UI è indietro di una struttura intera e non
     * ci si può fidare: lì si torna ai testi salvati.
     */
    fun updateRun(
        runBlockIds: List<String>,
        newLines: List<String>,
        linesBefore: List<String>? = null,
        caretLine: Int? = null
    ) {
        val runBlocks = resolveRun(runBlockIds) ?: return

        val stored = runBlocks.map { plainTextOf(it) }
        val oldTexts = if (linesBefore != null && linesBefore.size == runBlocks.size) {
            linesBefore
        } else {
            stored
        }
        if (stored == newLines) return
        if (oldTexts == newLines) return

        var prefixCount = 0
        val minCount = minOf(oldTexts.size, newLines.size)
        while (prefixCount < minCount && oldTexts[prefixCount] == newLines[prefixCount]) prefixCount++
        // **La modifica non può essere dopo il cursore.** Una riga
        // vuota inserita in mezzo ad altre righe vuote è identica a
        // tutte le sue vicine: il confronto da solo non sa quale sia
        // la nuova, e finiva per attribuirla all'ultima riga del
        // gruppo. Lì il "vicino da cui ereditare" era l'ultimo blocco
        // invece della riga su cui si stava scrivendo, e l'Invio in
        // fondo a un elenco dava un paragrafo: la lista si fermava.
        // Il cursore dice senza ambiguità dove è successo.
        if (caretLine != null) {
            prefixCount = prefixCount.coerceAtMost(caretLine.coerceIn(0, minCount))
        }

        var suffixCount = 0
        val maxSuffix = minCount - prefixCount
        while (suffixCount < maxSuffix &&
            oldTexts[oldTexts.size - 1 - suffixCount] == newLines[newLines.size - 1 - suffixCount]
        ) suffixCount++

        val oldMiddleStart = prefixCount
        val oldMiddleEnd = oldTexts.size - suffixCount
        val oldMiddleBlocks = runBlocks.subList(oldMiddleStart, oldMiddleEnd)
        val newMiddleLines = newLines.subList(prefixCount, newLines.size - suffixCount)

        if (oldMiddleBlocks.isEmpty() && newMiddleLines.isEmpty()) return

        val isPureTextEdit = oldMiddleBlocks.size == 1 && newMiddleLines.size == 1
        if (isPureTextEdit) {
            snapshotForTextEdit(oldMiddleBlocks[0].id)
        } else {
            snapshotForStructuralChange()
        }

        val pageId = currentPageId ?: return
        val parentBlockId = runBlocks.first().parentBlockId

        // Il blocco da cui i nuovi ereditano tipo e rientro è sempre
        // l'ultimo della zona modificata; se la zona è vuota (puro
        // inserimento, es. Invio a fine riga senza toccare le righe
        // esistenti), è quello immediatamente prima del punto in cui si
        // inserisce.
        val referenceBlock = oldMiddleBlocks.lastOrNull()
            ?: runBlocks.getOrNull(oldMiddleStart - 1)
        val indentLevel = referenceBlock?.indentLevel ?: 0
        // I titoli non si propagano: premere Invio dopo un titolo dà un
        // paragrafo normale, come in qualsiasi editor. Gli elenchi
        // invece sì, così continuare una lista crea altri elementi.
        val inheritedType = when (referenceBlock?.type) {
            BlockType.BULLET_LIST_ITEM,
            BlockType.NUMBERED_LIST_ITEM,
            // Andando a capo su una casella se ne ottiene un'altra, come
            // su Notion: è una lista di cose da fare, non una riga sola.
            // La nuova nasce **non spuntata**, ovviamente.
            BlockType.CHECKBOX -> referenceBlock.type
            else -> BlockType.PARAGRAPH
        }

        // **La formattazione sopravvive alla scrittura.**
        //
        // Prima ogni riga toccata veniva riscritta come un unico span
        // senza formato: bastava aggiungere una lettera a un paragrafo
        // perché il grassetto di una parola lontana sparisse. Con gli
        // spoiler sarebbe stato ancora più evidente — si copre una
        // parola, si continua a scrivere sulla stessa riga e il velo
        // cade da solo.
        //
        // Qui la zona modificata si tratta come **un testo solo**,
        // a-capo compresi: la stessa modifica si applica agli span con
        // `applyTextEdit`, che tocca soltanto il pezzo davvero
        // cambiato, e poi si rimette in righe. Vale anche quando il
        // cambiamento attraversa più righe: andando a capo in mezzo a
        // una frase, la metà che scende tiene il suo formato.
        val oldMiddleSpans = oldMiddleBlocks.map { spansOf(it) }.joinLines()
        val oldMiddleText = oldMiddleBlocks.joinToString("\n") { plainTextOf(it) }
        val newMiddleSpans = applyTextEdit(
            oldMiddleSpans,
            oldMiddleText,
            newMiddleLines.joinToString("\n")
        ).splitLines()

        // Rete di sicurezza: se per qualsiasi motivo il risultato non
        // corrisponde più alla riga che deve descrivere, si scrive la
        // riga in chiaro. Meglio perdere un grassetto che scrivere nel
        // database un testo diverso da quello che l'utente vede.
        fun spansForLine(index: Int, line: String): List<RichTextSpan> =
            newMiddleSpans.getOrNull(index)?.takeIf { it.plainText() == line }
                ?: listOf(RichTextSpan(text = line))

        // Una transazione sola per tutto il cambiamento, righe
        // riscritte e righe nuove insieme: vedi
        // `PageRepository.applyBlockChanges`. Scritte una alla volta
        // facevano ridisegnare la pagina una volta per riga, proprio
        // mentre l'utente stava scrivendo.
        viewModelScope.launch {
            if (newMiddleLines.size >= oldMiddleBlocks.size) {
                val rewritten = oldMiddleBlocks.mapIndexed { i, block ->
                    block.copy(
                        textJson = json.encodeToString(spansForLine(i, newMiddleLines[i]))
                    )
                }
                val extraLines = newMiddleLines.drop(oldMiddleBlocks.size)
                val shiftFromIndex = (referenceBlock?.orderIndex ?: -1) + 1
                repository.applyBlockChanges(
                    shift = if (extraLines.isEmpty()) {
                        null
                    } else {
                        PageRepository.BlockShift(
                            pageId,
                            parentBlockId,
                            shiftFromIndex,
                            extraLines.size
                        )
                    },
                    saved = rewritten + extraLines.mapIndexed { i, line ->
                        BlockEntity(
                            pageId = pageId,
                            parentBlockId = parentBlockId,
                            type = inheritedType,
                            indentLevel = indentLevel,
                            textJson = json.encodeToString(
                                spansForLine(oldMiddleBlocks.size + i, line)
                            ),
                            orderIndex = shiftFromIndex + i
                        )
                    }
                )
            } else {
                repository.applyBlockChanges(
                    saved = newMiddleLines.mapIndexed { i, line ->
                        oldMiddleBlocks[i].copy(
                            textJson = json.encodeToString(spansForLine(i, line))
                        )
                    },
                    deleted = oldMiddleBlocks.drop(newMiddleLines.size)
                )
            }
        }
    }

    /** Gli span (testo + formattazione) di un blocco, decodificati dal JSON salvato. */
    fun spansOf(block: BlockEntity): List<RichTextSpan> {
        return try {
            json.decodeFromString<List<RichTextSpan>>(block.textJson)
        } catch (e: Exception) {
            listOf(RichTextSpan(text = ""))
        }
    }

    /** Estrae il testo semplice da un blocco, per mostrarlo nell'editor. */
    fun plainTextOf(block: BlockEntity): String = spansOf(block).plainText()

    // --- Tabelle semplici (BlockType.TABLE) ---

    fun tableCellsFlow(blockId: String): Flow<List<TableCellEntity>> =
        repository.getTableCells(blockId)

    fun setTableCell(blockId: String, row: Int, col: Int, text: String) {
        // Il contenuto delle celle vive in una tabella separata dai
        // blocchi: non è coperto dalla cronologia annulla/ripristina.
        viewModelScope.launch { repository.setTableCellText(blockId, row, col, text) }
    }

    fun resizeTable(block: BlockEntity, newRows: Int, newCols: Int) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(block.copy(tableRows = newRows, tableCols = newCols))
        }
    }

    // --- Blocchi link a pagina/database (BlockType.PAGE_LINK / DATABASE_LINK) ---

    /**
     * Converte un blocco in un link verso una pagina di testo, creandola
     * se il blocco non ne ha già una collegata (riselezionare "Page" su
     * un blocco già collegato non ne crea una seconda). onReady riceve
     * l'id della pagina per navigarci subito — scegliere "Page" dal
     * menu crea la sottopagina E ci entra, come in Notion.
     */
    fun convertToPageLink(block: BlockEntity, onReady: (String) -> Unit) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            val targetPageId = block.linkedPageId ?: run {
                val newPage = PageEntity(title = "Untitled", isDatabase = false)
                repository.createPage(newPage)
                newPage.id
            }
            if (block.type != BlockType.PAGE_LINK || block.linkedPageId != targetPageId) {
                repository.saveBlock(block.copy(type = BlockType.PAGE_LINK, linkedPageId = targetPageId))
            }
            onReady(targetPageId)
        }
    }

    /** Come convertToPageLink, ma crea una pagina di tipo database. */
    fun convertToDatabaseLink(block: BlockEntity, onReady: (String) -> Unit) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            val targetPageId = block.linkedPageId ?: run {
                val newPage = PageEntity(title = "Untitled", isDatabase = true)
                repository.createPage(newPage)
                newPage.id
            }
            if (block.type != BlockType.DATABASE_LINK || block.linkedPageId != targetPageId) {
                repository.saveBlock(block.copy(type = BlockType.DATABASE_LINK, linkedPageId = targetPageId))
            }
            onReady(targetPageId)
        }
    }

    /**
     * Lettura una tantum (non un Flow osservato in continuo) del titolo
     * e icona della pagina collegata a un blocco PAGE_LINK/DATABASE_LINK
     * — sufficiente per mostrarli nella riga del blocco: si aggiornano
     * comunque ogni volta che si riapre la pagina che li contiene.
     */
    suspend fun getPageInfo(pageId: String): PageEntity? = repository.getPage(pageId)
}
