package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.NotebookEntity
import com.gabriele.notionlocal.data.entity.NotebookSectionEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.NotebookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * **I quaderni nella barra laterale** (dal 26/09/2026): l'albero di quaderni,
 * gruppi e sezioni, e tutto quello che si fa tenendo premuta una voce.
 * Legge il database dal vivo: una sezione rinominata o spostata si vede
 * subito, senza ricaricare.
 */
class NotebooksViewModel(
    private val repository: NotebookRepository,
    private val imageStore: PageImageStore
) : ViewModel() {

    val notebooks: StateFlow<List<NotebookEntity>> =
        repository.observeNotebooks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sections: StateFlow<List<NotebookSectionEntity>> =
        repository.observeSections().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Quante pagine ha ogni sezione: solo mentre qualcuno le guarda (la
     * pagina di un quaderno). La tabella delle pagine cambia a ogni tasto,
     * e la barra laterale non ne ha bisogno.
     */
    val pageCounts: StateFlow<Map<String, Int>> =
        repository.observePageCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun createNotebook(title: String, color: Int, firstSectionTitle: String) {
        viewModelScope.launch { repository.createNotebook(title, color, firstSectionTitle) }
    }

    fun renameNotebook(id: String, title: String) {
        viewModelScope.launch { repository.renameNotebook(id, title) }
    }

    fun setNotebookColor(id: String, color: Int) {
        viewModelScope.launch { repository.setNotebookColor(id, color) }
    }

    /** L'immagine nuova (o null, il quaderno disegnato); quella di prima si cancella. */
    fun setNotebookIcon(id: String, fileName: String?) {
        viewModelScope.launch {
            val old = repository.setNotebookIcon(id, fileName)
            if (old != null && old != fileName) imageStore.delete(old)
        }
    }

    fun toggleNotebook(notebook: NotebookEntity) {
        viewModelScope.launch { repository.setNotebookExpanded(notebook.id, !notebook.isExpanded) }
    }

    fun moveNotebook(id: String, up: Boolean) {
        viewModelScope.launch { repository.moveNotebook(id, up) }
    }

    fun deleteNotebook(id: String) {
        viewModelScope.launch { repository.deleteNotebook(id).forEach { imageStore.delete(it) } }
    }

    fun createSection(
        notebookId: String,
        parentId: String?,
        title: String,
        isGroup: Boolean,
        onCreated: (NotebookSectionEntity) -> Unit = {}
    ) {
        viewModelScope.launch { onCreated(repository.createSection(notebookId, parentId, title, isGroup)) }
    }

    fun renameSection(id: String, title: String) {
        viewModelScope.launch { repository.renameSection(id, title) }
    }

    fun setSectionColor(id: String, color: Int) {
        viewModelScope.launch { repository.setSectionColor(id, color) }
    }

    fun toggleGroup(section: NotebookSectionEntity) {
        viewModelScope.launch { repository.setSectionExpanded(section.id, !section.isExpanded) }
    }

    fun setSectionProtected(id: String, locked: Boolean) {
        viewModelScope.launch { repository.setSectionProtected(id, locked) }
    }

    fun moveSection(id: String, up: Boolean) {
        viewModelScope.launch { repository.moveSection(id, up) }
    }

    fun deleteSection(id: String) {
        viewModelScope.launch { repository.deleteSection(id).forEach { imageStore.delete(it) } }
    }

    /** Quante pagine se ne andrebbero, per la domanda prima di cancellare. */
    fun countPages(notebookId: String?, sectionId: String?, onReady: (Int) -> Unit) {
        viewModelScope.launch {
            onReady(
                when {
                    sectionId != null -> repository.countPagesUnderSection(sectionId)
                    notebookId != null -> repository.countPagesInNotebook(notebookId)
                    else -> 0
                }
            )
        }
    }
}

/** **Una sezione aperta**: le sue pagine, come la colonna delle pagine di OneNote. */
@OptIn(ExperimentalCoroutinesApi::class)
class NotebookSectionViewModel(
    private val repository: NotebookRepository,
    private val imageStore: PageImageStore
) : ViewModel() {

    private val sectionId = MutableStateFlow<String?>(null)

    val section: StateFlow<NotebookSectionEntity?> = sectionId.filterNotNull()
        .flatMapLatest { repository.observeSection(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pages: StateFlow<List<PageEntity>> = sectionId.filterNotNull()
        .flatMapLatest { repository.observePages(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Il quaderno e i gruppi sopra la sezione: si rilegge quando cambia un nome qualsiasi. */
    val path: StateFlow<List<String>> = sectionId.filterNotNull()
        .flatMapLatest { id ->
            combine(repository.observeNotebooks(), repository.observeSections()) { _, _ -> repository.pathOf(id) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun load(id: String) {
        sectionId.value = id
    }

    fun addPage(onCreated: (String) -> Unit) {
        val id = sectionId.value ?: return
        viewModelScope.launch { onCreated(repository.createPage(id)) }
    }

    fun movePage(pageId: String, up: Boolean) {
        viewModelScope.launch { repository.movePage(pageId, up) }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch { repository.deletePage(pageId).forEach { imageStore.delete(it) } }
    }
}
