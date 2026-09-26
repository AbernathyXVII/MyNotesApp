package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.PageTreeNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * La barra laterale: l'albero delle pagine.
 *
 * L'albero si carica **un ramo alla volta**, quando lo si apre: leggerlo
 * tutto a ogni apertura della barra vorrebbe dire attraversare ogni
 * pagina e ogni database dell'archivio per mostrarne una manciata. Si
 * ricarica (i soli rami aperti) ogni volta che la barra si apre, così
 * una pagina appena creata o rinominata c'è già.
 */
class SidebarViewModel(private val repository: PageRepository) : ViewModel() {

    private val _root = MutableStateFlow<PageTreeNode?>(null)
    val root: StateFlow<PageTreeNode?> = _root

    /** I figli già letti, per chiave del nodo (`PageTreeNode.key`). */
    private val _children = MutableStateFlow<Map<String, List<PageTreeNode>>>(emptyMap())
    val children: StateFlow<Map<String, List<PageTreeNode>>> = _children

    /** I rami aperti. La pagina principale parte aperta: è da lì che si comincia. */
    private val _expanded = MutableStateFlow(setOf(PageEntity.ROOT_PAGE_ID))
    val expanded: StateFlow<Set<String>> = _expanded

    fun refresh() {
        viewModelScope.launch {
            _root.value = repository.treeRoot()
            val loaded = mutableMapOf<String, List<PageTreeNode>>()
            for (key in _expanded.value) {
                if (key.startsWith("row:")) continue
                loaded[key] = repository.treeChildren(key)
            }
            _children.value = loaded
        }
    }

    fun toggle(node: PageTreeNode) {
        val pageId = node.pageId ?: return
        if (pageId in _expanded.value) {
            _expanded.value = _expanded.value - pageId
            return
        }
        _expanded.value = _expanded.value + pageId
        viewModelScope.launch {
            _children.value = _children.value + (pageId to repository.treeChildren(pageId))
        }
    }

    /** La pagina di una riga mai aperta nasce qui, al primo tocco. */
    fun openRow(rowId: String, onReady: (String) -> Unit) {
        viewModelScope.launch { repository.ensureRowPage(rowId)?.let(onReady) }
    }
}

/**
 * La scelta della pagina in cui mettere una copia: lo stesso albero della
 * barra laterale, coi rami aperti per conto suo, più una ricerca per
 * titolo che arriva anche alle righe dei database sepolte in fondo.
 */
class PagePickerViewModel(private val repository: PageRepository) : ViewModel() {

    private val _root = MutableStateFlow<PageTreeNode?>(null)
    val root: StateFlow<PageTreeNode?> = _root

    private val _children = MutableStateFlow<Map<String, List<PageTreeNode>>>(emptyMap())
    val children: StateFlow<Map<String, List<PageTreeNode>>> = _children

    private val _expanded = MutableStateFlow(setOf(PageEntity.ROOT_PAGE_ID))
    val expanded: StateFlow<Set<String>> = _expanded

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<PageRepository.DestinationHit>>(emptyList())
    val results: StateFlow<List<PageRepository.DestinationHit>> = _results

    private var searchJob: Job? = null

    /** Si riparte da capo a ogni apertura: l'albero può essere cambiato, e la ricerca di prima non serve più. */
    fun reset() {
        _expanded.value = setOf(PageEntity.ROOT_PAGE_ID)
        setQuery("")
        viewModelScope.launch {
            _root.value = repository.treeRoot()
            _children.value = mapOf(PageEntity.ROOT_PAGE_ID to repository.treeChildren(PageEntity.ROOT_PAGE_ID))
        }
    }

    fun toggle(node: PageTreeNode) {
        val pageId = node.pageId ?: return
        if (pageId in _expanded.value) {
            _expanded.value = _expanded.value - pageId
            return
        }
        _expanded.value = _expanded.value + pageId
        viewModelScope.launch {
            _children.value = _children.value + (pageId to repository.treeChildren(pageId))
        }
    }

    fun setQuery(text: String) {
        _query.value = text
        searchJob?.cancel()
        if (text.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch { _results.value = repository.searchDestinations(text) }
    }
}

/** L'elenco dei preferiti. L'ordine lo decide la schermata, dalle impostazioni. */
class FavoritesViewModel(repository: PageRepository) : ViewModel() {
    val favorites: StateFlow<List<PageEntity>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Il cestino: l'elenco, il ripristino, la cancellazione definitiva.
 *
 * Tiene il magazzino delle immagini perché cancellare una pagina per
 * davvero vuol dire anche togliere dalla cartella la sua icona e la sua
 * copertina: altrimenti resterebbero lì per sempre, senza nessuno che le
 * mostri.
 */
class TrashViewModel(
    private val repository: PageRepository,
    private val imageStore: PageImageStore
) : ViewModel() {

    val trash: StateFlow<List<PageEntity>> = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(pageId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreFromTrash(pageId)
            onDone()
        }
    }

    fun deletePermanently(pageId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePermanently(pageId).forEach { imageStore.delete(it) }
            onDone()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash().forEach { imageStore.delete(it) }
        }
    }
}

/** La finestra di avvio: serve l'elenco delle pagine fra cui sceglierne una. */
class StartupViewModel(private val repository: PageRepository) : ViewModel() {

    private val _pages = MutableStateFlow<List<PageEntity>>(emptyList())
    val pages: StateFlow<List<PageEntity>> = _pages

    private val _chosen = MutableStateFlow<PageEntity?>(null)
    val chosen: StateFlow<PageEntity?> = _chosen

    fun load(chosenId: String?) {
        viewModelScope.launch {
            _pages.value = repository.startupCandidates()
            _chosen.value = chosenId?.let { repository.getPage(it) }
        }
    }
}
