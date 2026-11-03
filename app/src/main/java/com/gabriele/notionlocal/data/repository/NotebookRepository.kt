package com.gabriele.notionlocal.data.repository

import androidx.room.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.NotebookColors
import com.gabriele.notionlocal.data.entity.NotebookEntity
import com.gabriele.notionlocal.data.entity.NotebookSectionEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.security.AppLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * **I quaderni** (dal 26/09/2026): quaderni, gruppi di sezioni, sezioni e le
 * pagine delle sezioni, come in OneNote. Vedi `NotebookEntity`.
 *
 * Le pagine sono pagine vere (`PageEntity` con `notebookSectionId`), e per
 * cancellarle passa da `PageRepository.deletePermanently`, che sa già
 * togliere blocchi, disegni e immagini.
 */
class NotebookRepository(private val db: AppDatabase, private val pages: PageRepository) {

    private val dao = db.notebookDao()
    private val pageDao = db.pageDao()

    fun observeNotebooks(): Flow<List<NotebookEntity>> = dao.observeNotebooks()
    fun observeSections(): Flow<List<NotebookSectionEntity>> = dao.observeSections()
    fun observeSection(id: String): Flow<NotebookSectionEntity?> = dao.observeSection(id)
    fun observePages(sectionId: String): Flow<List<PageEntity>> = dao.observePagesInSection(sectionId)

    /** Sezione → quante pagine ha (quelle vuote non ci sono). */
    fun observePageCounts(): Flow<Map<String, Int>> =
        dao.observePageCounts().map { counts -> counts.associate { it.sectionId to it.pages } }

    suspend fun getNotebook(id: String): NotebookEntity? = dao.getNotebook(id)
    suspend fun getSection(id: String): NotebookSectionEntity? = dao.getSection(id)

    // --- Quaderni ---

    /**
     * Un quaderno nuovo, in fondo agli altri, **con dentro la sua prima
     * sezione**, come in OneNote: un quaderno vuoto non servirebbe a niente
     * finché non gli si aggiunge una sezione.
     */
    suspend fun createNotebook(title: String, color: Int, firstSectionTitle: String): NotebookEntity =
        db.withTransaction {
            val order = (dao.getNotebooksOnce().maxOfOrNull { it.orderIndex } ?: -1) + 1
            val notebook = NotebookEntity(title = title.trim(), color = color, orderIndex = order)
            dao.insertNotebook(notebook)
            dao.insertSection(
                NotebookSectionEntity(
                    notebookId = notebook.id,
                    title = firstSectionTitle,
                    color = NotebookColors.palette.first()
                )
            )
            notebook
        }

    suspend fun renameNotebook(id: String, title: String) {
        val notebook = dao.getNotebook(id) ?: return
        dao.updateNotebook(notebook.copy(title = title.trim()))
    }

    suspend fun setNotebookColor(id: String, color: Int) {
        val notebook = dao.getNotebook(id) ?: return
        dao.updateNotebook(notebook.copy(color = color))
    }

    /** L'immagine al posto del quaderno disegnato (null: di nuovo quello disegnato). Torna il file di prima. */
    suspend fun setNotebookIcon(id: String, fileName: String?): String? {
        val notebook = dao.getNotebook(id) ?: return fileName
        dao.updateNotebook(notebook.copy(iconImage = fileName))
        return notebook.iconImage
    }

    suspend fun setNotebookExpanded(id: String, expanded: Boolean) {
        val notebook = dao.getNotebook(id) ?: return
        if (notebook.isExpanded != expanded) dao.updateNotebook(notebook.copy(isExpanded = expanded))
    }

    /** Su o giù di un posto fra i quaderni. */
    suspend fun moveNotebook(id: String, up: Boolean) = db.withTransaction {
        val list = dao.getNotebooksOnce()
        val index = list.indexOfFirst { it.id == id }
        val other = if (up) index - 1 else index + 1
        if (index < 0 || other !in list.indices) return@withTransaction
        val reordered = list.toMutableList().apply { add(other, removeAt(index)) }
        reordered.forEachIndexed { order, notebook ->
            if (notebook.orderIndex != order) dao.updateNotebook(notebook.copy(orderIndex = order))
        }
    }

    /**
     * Il quaderno se ne va **per sempre**, con sezioni e pagine. Torna i file
     * delle immagini da cancellare, come `PageRepository.deletePermanently`.
     */
    suspend fun deleteNotebook(id: String): List<String> {
        val notebook = dao.getNotebook(id) ?: return emptyList()
        val sectionIds = dao.getSectionsOfNotebook(id).map { it.id }
        val files = deletePagesIn(sectionIds)
        dao.deleteNotebook(notebook)
        return files + listOfNotNull(notebook.iconImage)
    }

    // --- Sezioni e gruppi ---

    /**
     * Una sezione (o un gruppo) nuova **in fondo** a `parentId` — un gruppo,
     * o il quaderno se null. Una sezione prende il colore dopo quello
     * dell'ultima creata nel quaderno. Il posto dove nasce si apre, così la
     * si vede subito.
     */
    suspend fun createSection(
        notebookId: String,
        parentId: String?,
        title: String,
        isGroup: Boolean
    ): NotebookSectionEntity = db.withTransaction {
        val all = dao.getSectionsOfNotebook(notebookId)
        val lastColor = all.filter { !it.isGroup }.maxByOrNull { it.createdAt }?.color
        val section = NotebookSectionEntity(
            notebookId = notebookId,
            parentId = parentId,
            isGroup = isGroup,
            title = title.trim(),
            color = NotebookColors.after(lastColor),
            orderIndex = (all.filter { it.parentId == parentId }.maxOfOrNull { it.orderIndex } ?: -1) + 1
        )
        dao.insertSection(section)
        setNotebookExpanded(notebookId, true)
        parentId?.let { setSectionExpanded(it, true) }
        section
    }

    suspend fun renameSection(id: String, title: String) {
        val section = dao.getSection(id) ?: return
        dao.updateSection(section.copy(title = title.trim()))
    }

    suspend fun setSectionColor(id: String, color: Int) {
        val section = dao.getSection(id) ?: return
        dao.updateSection(section.copy(color = color))
    }

    suspend fun setSectionExpanded(id: String, expanded: Boolean) {
        val section = dao.getSection(id) ?: return
        if (section.isExpanded != expanded) dao.updateSection(section.copy(isExpanded = expanded))
    }

    /** Protetta con la password (vedi `AppLock`); protetta, si richiude subito. */
    suspend fun setSectionProtected(id: String, locked: Boolean) {
        val section = dao.getSection(id) ?: return
        if (section.isGroup) return
        if (locked) AppLock.lock(id)
        dao.updateSection(section.copy(isProtected = locked))
        AppLock.protectionChanged()
    }

    /** Su o giù di un posto fra le sezioni e i gruppi che stanno insieme a lei. */
    suspend fun moveSection(id: String, up: Boolean) = db.withTransaction {
        val section = dao.getSection(id) ?: return@withTransaction
        val siblings = dao.getSectionsOfNotebook(section.notebookId).filter { it.parentId == section.parentId }
        val index = siblings.indexOfFirst { it.id == id }
        val other = if (up) index - 1 else index + 1
        if (index < 0 || other !in siblings.indices) return@withTransaction
        val reordered = siblings.toMutableList().apply { add(other, removeAt(index)) }
        reordered.forEachIndexed { order, sibling ->
            if (sibling.orderIndex != order) dao.updateSection(sibling.copy(orderIndex = order))
        }
    }

    /** La sezione o il gruppo, con tutto quello che c'è dentro: prima lei, poi il resto. */
    private suspend fun subtreeOf(section: NotebookSectionEntity): List<NotebookSectionEntity> {
        val all = dao.getSectionsOfNotebook(section.notebookId)
        val result = mutableListOf(section)
        var frontier = listOf(section.id)
        while (frontier.isNotEmpty()) {
            val children = all.filter { it.parentId in frontier }
            result += children
            frontier = children.map { it.id }
        }
        return result
    }

    /** Quante pagine se ne andrebbero con questa sezione (o gruppo), per la domanda prima di cancellarla. */
    suspend fun countPagesUnderSection(id: String): Int {
        val section = dao.getSection(id) ?: return 0
        return dao.getPageIdsInSections(subtreeOf(section).map { it.id }).size
    }

    /** Quante pagine ha il quaderno, in tutte le sue sezioni. */
    suspend fun countPagesInNotebook(id: String): Int =
        dao.getPageIdsInSections(dao.getSectionsOfNotebook(id).map { it.id }).size

    /** La sezione o il gruppo se ne va **per sempre**, con quello che contiene. Torna i file da cancellare. */
    suspend fun deleteSection(id: String): List<String> {
        val section = dao.getSection(id) ?: return emptyList()
        val subtree = subtreeOf(section)
        val files = deletePagesIn(subtree.map { it.id })
        dao.deleteSections(subtree)
        return files
    }

    private suspend fun deletePagesIn(sectionIds: List<String>): List<String> {
        if (sectionIds.isEmpty()) return emptyList()
        return dao.getPageIdsInSections(sectionIds).flatMap { pages.deletePermanently(it) }
    }

    /** Il quaderno e i gruppi sopra la sezione, dal più esterno: "SuduS › Languages › Deutsch". */
    suspend fun pathOf(sectionId: String): List<String> {
        val section = dao.getSection(sectionId) ?: return emptyList()
        val all = dao.getSectionsOfNotebook(section.notebookId).associateBy { it.id }
        val groups = mutableListOf<String>()
        var parent = section.parentId?.let { all[it] }
        while (parent != null && groups.size < all.size) {
            groups.add(0, parent.title)
            parent = parent.parentId?.let { all[it] }
        }
        val notebook = dao.getNotebook(section.notebookId)?.title
        return listOfNotNull(notebook) + groups
    }

    // --- Pagine ---

    /** Una pagina nuova, vuota, in fondo alla sezione. Torna il suo id. */
    suspend fun createPage(sectionId: String): String = db.withTransaction {
        val order = (dao.getPagesInSection(sectionId).maxOfOrNull { it.orderIndex } ?: -1) + 1
        val page = PageEntity(title = "", icon = "", notebookSectionId = sectionId, orderIndex = order)
        pageDao.insert(page)
        page.id
    }

    /** Su o giù di un posto nella sezione. */
    suspend fun movePage(pageId: String, up: Boolean) = db.withTransaction {
        val sectionId = pageDao.getById(pageId)?.notebookSectionId ?: return@withTransaction
        val list = dao.getPagesInSection(sectionId)
        val index = list.indexOfFirst { it.id == pageId }
        val other = if (up) index - 1 else index + 1
        if (index < 0 || other !in list.indices) return@withTransaction
        val reordered = list.toMutableList().apply { add(other, removeAt(index)) }
        reordered.forEachIndexed { order, page ->
            if (page.orderIndex != order) dao.setPageOrder(page.id, order)
        }
    }

    /** La pagina se ne va **per sempre**. Torna i file da cancellare. */
    suspend fun deletePage(pageId: String): List<String> = pages.deletePermanently(pageId)
}
