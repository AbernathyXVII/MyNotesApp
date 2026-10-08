package com.gabriele.notionlocal.data

import org.junit.Assert.assertFalse
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.ColumnType
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Le viste di un database** (29/09/2026): il "+" accanto al nome della
 * vista ne aggiunge una, toccando il nome si sceglie quale vedere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseViewsTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private val page = PageEntity(title = "Pagina")
    private val database = PageEntity(title = "Videogiochi", isDatabase = true, databaseLayout = DatabaseLayout.BOARD)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(page)
        pages.createPage(database)
        db.blockDao().insertAll(
            listOf(BlockEntity(pageId = page.id, type = BlockType.DATABASE_LINK, linkedPageId = database.id, orderIndex = 0))
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun page(id: String) = db.pageDao().getById(id)

    @Test
    fun aNewViewShowsTheSameDataWithItsOwnSettingsAndComesToTheFront() = runBlocking<Unit> {
        val id = pages.createView(database.id, "  Per console ", DatabaseLayout.TABLE)!!
        val view = page(id)!!
        assertEquals(database.id, view.viewHostId)
        assertEquals(database.id, view.sourceDatabaseId)
        assertEquals("Per console", view.viewName)
        assertEquals(DatabaseLayout.TABLE, view.databaseLayout)
        assertTrue(view.isDatabase)
        // Da zero: niente filtro, ordinamento o gruppi del database.
        assertNull(view.filterColumnId)
        assertNull(view.sortColumnId)
        assertEquals(id, page(database.id)!!.activeViewId)
        // Il database resta com'era.
        assertEquals(DatabaseLayout.BOARD, page(database.id)!!.databaseLayout)
        assertEquals(listOf(id), db.pageDao().getViewsOnce(database.id).map { it.id })
    }

    @Test
    fun viewsKeepTheOrderTheyWereMadeIn() = runBlocking<Unit> {
        val first = pages.createView(database.id, "Uno", DatabaseLayout.LIST)!!
        val second = pages.createView(database.id, "", DatabaseLayout.GALLERY)!!
        assertEquals(listOf(first, second), db.pageDao().getViewsOnce(database.id).map { it.id })
        // Senza nome: quello del layout, deciso dallo schermo.
        assertNull(page(second)!!.viewName)
        assertEquals(second, page(database.id)!!.activeViewId)
    }

    @Test
    fun aViewOfALinkedViewShowsTheRealDatabase() = runBlocking<Unit> {
        val linked = pages.createLinkedView(database.id)!!
        val id = pages.createView(linked, null, DatabaseLayout.TABLE)!!
        assertEquals(linked, page(id)!!.viewHostId)
        assertEquals(database.id, page(id)!!.sourceDatabaseId)
        // Le viste della vista collegata sono sue, non del database.
        assertEquals(emptyList<String>(), db.pageDao().getViewsOnce(database.id).map { it.id })
    }

    @Test
    fun pickingTheDatabaseItselfClearsTheActiveView() = runBlocking<Unit> {
        val id = pages.createView(database.id, "Altra", DatabaseLayout.TABLE)!!
        pages.setActiveView(database.id, database.id)
        assertNull(page(database.id)!!.activeViewId)
        pages.setActiveView(database.id, id)
        assertEquals(id, page(database.id)!!.activeViewId)
    }

    @Test
    fun renamingToNothingGivesBackTheLayoutName() = runBlocking<Unit> {
        val id = pages.createView(database.id, "Altra", DatabaseLayout.TABLE)!!
        pages.renameView(id, " Nuovo nome ")
        assertEquals("Nuovo nome", page(id)!!.viewName)
        pages.renameView(id, "   ")
        assertNull(page(id)!!.viewName)
        // Anche la vista di partenza si rinomina.
        pages.renameView(database.id, "Tutti")
        assertEquals("Tutti", page(database.id)!!.viewName)
    }

    @Test
    fun deletingTheActiveViewGoesBackToTheDatabaseOne() = runBlocking<Unit> {
        val other = pages.createView(database.id, "Altra", DatabaseLayout.TABLE)!!
        val active = pages.createView(database.id, "Davanti", DatabaseLayout.LIST)!!
        pages.deleteView(active)
        assertNull(page(active))
        assertNull(page(database.id)!!.activeViewId)
        assertNotNull(page(other))
        // Il database stesso non si toglie da qui.
        pages.deleteView(database.id)
        assertNotNull(page(database.id))
    }

    @Test
    fun theViewsGoAwayWithTheirDatabase() = runBlocking<Unit> {
        val view = pages.createView(database.id, "Altra", DatabaseLayout.TABLE)!!
        pages.moveToTrash(database.id)
        pages.deletePermanently(database.id)
        assertNull(page(database.id))
        assertNull(page(view))
    }

    @Test
    fun theViewsGoAwayWithThePageThatHoldsTheDatabase() = runBlocking<Unit> {
        val view = pages.createView(database.id, "Altra", DatabaseLayout.TABLE)!!
        pages.moveToTrash(page.id)
        pages.deletePermanently(page.id)
        assertNull(page(database.id))
        assertNull(page(view))
    }

    @Test
    fun viewsAreNeverAStartPage() = runBlocking<Unit> {
        val view = pages.createView(database.id, "Altra", DatabaseLayout.TABLE)!!
        assertTrue(pages.startupCandidates().none { it.id == view })
    }

    @Test
    fun aCopiedDatabaseShowsItsOwnView() = runBlocking<Unit> {
        pages.createView(database.id, "Altra", DatabaseLayout.TABLE)!!
        val copy = pages.duplicatePage(database.id, DuplicateTarget.NextToOriginal, "copy") { it }!!
        assertNull(page(copy)!!.activeViewId)
        assertEquals(emptyList<String>(), db.pageDao().getViewsOnce(copy).map { it.id })
    }

    @Test
    fun deletingTheStartingViewGivesItTheSettingsOfTheNextOne() = runBlocking<Unit> {
        // "Foreign Languages" (08/10/2026): la Table di partenza se ne va, resta la List.
        val title = DatabaseColumnEntity(pageId = database.id, name = "Note", type = ColumnType.TEXT, orderIndex = 0)
        val level = DatabaseColumnEntity(pageId = database.id, name = "Level", type = ColumnType.SELECT, orderIndex = 1, hidden = true)
        db.databaseDao().insertColumn(title)
        db.databaseDao().insertColumn(level)
        val list = pages.createView(database.id, "Lista", DatabaseLayout.LIST)!!
        db.pageDao().update(
            page(list)!!.copy(
                sortColumnId = level.id, sortDescending = true, filterColumnId = level.id, filterValue = "B2",
                viewHiddenColumnIds = title.id, freezeFirstColumn = true, freezeFirstRow = true
            )
        )
        val gallery = pages.createView(database.id, null, DatabaseLayout.GALLERY)!!
        pages.setActiveView(database.id, list)

        assertTrue(pages.deleteStartingView(database.id))
        val host = page(database.id)!!
        assertEquals(DatabaseLayout.LIST, host.databaseLayout)
        assertEquals("Lista", host.viewName)
        assertEquals(level.id, host.sortColumnId)
        assertTrue(host.sortDescending)
        assertEquals("B2", host.filterValue)
        assertTrue(host.freezeFirstColumn)
        assertTrue(host.freezeFirstRow)
        // Quella davanti se n'è andata: ora la mostra il database.
        assertNull(host.activeViewId)
        assertNull(page(list))
        assertEquals(listOf(gallery), db.pageDao().getViewsOnce(database.id).map { it.id })
        // Le colonne nascoste della List ora sono quelle del database.
        val columns = db.databaseDao().getColumnsForPageOnce(database.id).associateBy { it.id }
        assertTrue(columns[title.id]!!.hidden)
        assertFalse(columns[level.id]!!.hidden)
        // Il nome e il resto del database restano suoi.
        assertEquals("Videogiochi", host.title)
        assertTrue(host.isDatabase)
    }

    @Test
    fun theLastViewCannotBeDeleted() = runBlocking<Unit> {
        assertFalse(pages.deleteStartingView(database.id))
        assertEquals(DatabaseLayout.BOARD, page(database.id)!!.databaseLayout)
    }
}
