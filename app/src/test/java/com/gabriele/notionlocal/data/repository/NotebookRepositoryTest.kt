package com.gabriele.notionlocal.data.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.NotebookColors
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.security.AppLock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * I quaderni come in OneNote: quaderni, gruppi, sezioni e pagine, e il fatto
 * che stiano **per conto loro**, fuori dal menu principale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NotebookRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var notebooks: NotebookRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        context.getDatabasePath("notion_local.db").delete()
        db = AppDatabase.getInstance(context)
        pages = PageRepository(db)
        notebooks = NotebookRepository(db, pages)
        AppLock.keepOnlyUnlocked(null)
        db.pageDao().insert(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
    }

    @After
    fun tearDown() {
        AppLock.keepOnlyUnlocked(null)
        AppDatabase.resetForTests()
    }

    @Test
    fun aNewNotebookComesWithItsFirstSection() = runBlocking<Unit> {
        val notebook = notebooks.createNotebook("SuduS", NotebookColors.DEFAULT_NOTEBOOK, "New Section 1")
        val sections = db.notebookDao().getSectionsOfNotebook(notebook.id)
        assertEquals(listOf("New Section 1"), sections.map { it.title })
        assertNull(sections.single().parentId)
    }

    @Test
    fun sectionsTakeTheNextColorAndGoAtTheEnd() = runBlocking<Unit> {
        val notebook = notebooks.createNotebook("SuduS", NotebookColors.DEFAULT_NOTEBOOK, "General notes")
        val songs = notebooks.createSection(notebook.id, null, "Songs", isGroup = false)
        val languages = notebooks.createSection(notebook.id, null, "Languages", isGroup = true)
        val deutsch = notebooks.createSection(notebook.id, languages.id, "Deutsch", isGroup = true)
        val general = notebooks.createSection(notebook.id, deutsch.id, "General", isGroup = false)

        val first = db.notebookDao().getSectionsOfNotebook(notebook.id).first { it.title == "General notes" }
        assertNotEquals(first.color, songs.color)
        assertEquals(NotebookColors.after(first.color), songs.color)
        assertEquals(listOf("General notes", "Songs", "Languages"), topLevelTitles(notebook.id))
        assertEquals(listOf("SuduS", "Languages", "Deutsch"), notebooks.pathOf(general.id))

        notebooks.moveSection(languages.id, up = true)
        assertEquals(listOf("General notes", "Languages", "Songs"), topLevelTitles(notebook.id))
        // Il primo non sale oltre la cima.
        notebooks.moveSection(first.id, up = true)
        assertEquals(listOf("General notes", "Languages", "Songs"), topLevelTitles(notebook.id))
    }

    private suspend fun topLevelTitles(notebookId: String) =
        db.notebookDao().getSectionsOfNotebook(notebookId).filter { it.parentId == null }.map { it.title }

    @Test
    fun pagesStayInTheirSectionAndOutOfTheMainMenu() = runBlocking<Unit> {
        val notebook = notebooks.createNotebook("SuduS", NotebookColors.DEFAULT_NOTEBOOK, "Songs")
        val section = db.notebookDao().getSectionsOfNotebook(notebook.id).single()
        val first = notebooks.createPage(section.id)
        val second = notebooks.createPage(section.id)
        db.pageDao().setTitle(first, "Esempio", 0)
        db.blockDao().insert(BlockEntity(id = "b1", pageId = first, orderIndex = 0, textJson = """[{"text":"Chill Lab Music"}]"""))
        assertEquals(listOf(first, second), db.notebookDao().getPagesInSection(section.id).map { it.id })

        notebooks.movePage(second, up = true)
        assertEquals(listOf(second, first), db.notebookDao().getPagesInSection(section.id).map { it.id })

        // Il menu principale non le vede: né la ricerca, né le pagine da cui partire.
        assertTrue(pages.search("Esempio", inContent = true, inTrash = false).isEmpty())
        assertTrue(pages.search("Chill", inContent = true, inTrash = false).isEmpty())
        assertTrue(pages.startupCandidates().none { it.notebookSectionId != null })
    }

    @Test
    fun deletingAGroupTakesItsSectionsAndPagesWithIt() = runBlocking<Unit> {
        val notebook = notebooks.createNotebook("SuduS", NotebookColors.DEFAULT_NOTEBOOK, "General notes")
        val keep = db.notebookDao().getSectionsOfNotebook(notebook.id).single()
        val languages = notebooks.createSection(notebook.id, null, "Languages", isGroup = true)
        val english = notebooks.createSection(notebook.id, languages.id, "English", isGroup = false)
        val kept = notebooks.createPage(keep.id)
        val gone = notebooks.createPage(english.id)
        assertEquals(1, notebooks.countPagesUnderSection(languages.id))

        notebooks.deleteSection(languages.id)
        assertEquals(listOf(keep.id), db.notebookDao().getSectionsOfNotebook(notebook.id).map { it.id })
        assertNull(db.pageDao().getById(gone))
        assertEquals(kept, db.pageDao().getById(kept)?.id)

        notebooks.deleteNotebook(notebook.id)
        assertTrue(db.notebookDao().getNotebooksOnce().isEmpty())
        assertNull(db.pageDao().getById(kept))
        // Il menu principale non si tocca.
        assertEquals("My Space", db.pageDao().getById(PageEntity.ROOT_PAGE_ID)?.title)
    }

    @Test
    fun aProtectedSectionClosesItsPages() = runBlocking<Unit> {
        val notebook = notebooks.createNotebook("SuduS", NotebookColors.DEFAULT_NOTEBOOK, "Diario")
        val section = db.notebookDao().getSectionsOfNotebook(notebook.id).single()
        val page = notebooks.createPage(section.id)
        assertNull(pages.lockingPageOf(page))

        notebooks.setSectionProtected(section.id, true)
        assertEquals(section.id, pages.lockingPageOf(section.id))
        assertEquals(section.id, pages.lockingPageOf(page))
        assertTrue(pages.isLockedNow(page))
        // Senza togliere prima la protezione, la password non si toglie.
        assertEquals(1, pages.countProtectedPages())

        AppLock.unlock(section.id)
        assertTrue(!pages.isLockedNow(page))
    }
}
