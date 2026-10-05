package com.gabriele.notionlocal.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Move to database"** (05/10/2026): una pagina diventa una pagina nuova
 * di un database, col suo contenuto; un database finisce in una pagina
 * nuova del database scelto, aperto dentro di lei. Con Annulla e
 * Ripristina.
 *
 * Il menu principale ha "Notes" (una pagina con del testo), "Games" (un
 * database con una riga) e "Movies" (un altro database, mostrato aperto).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MoveToDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository

    private val root = PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "Root")
    private val notes = PageEntity(title = "Notes", icon = "📝")
    private val games = PageEntity(title = "Games", isDatabase = true)
    private val movies = PageEntity(title = "Movies", icon = "🎬", isDatabase = true)
    private val zelda = PageEntity(title = "Zelda", isRowPage = true)
    private lateinit var zeldaRow: DatabaseRowEntity
    private lateinit var zeldaCell: DatabaseCellEntity
    private lateinit var notesLink: BlockEntity
    private lateinit var moviesLink: BlockEntity

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PageRepository(db)
        listOf(root, notes, games, movies, zelda).forEach { repo.createPage(it) }
        notesLink = link(root.id, notes.id, 0)
        link(root.id, games.id, 1)
        moviesLink = link(root.id, movies.id, 2, BlockType.DATABASE_LINK)
        repo.saveBlock(BlockEntity(pageId = notes.id, textJson = """[{"text":"testo"}]""", orderIndex = 0))
        val status = DatabaseColumnEntity(pageId = games.id, name = "Status")
        db.databaseDao().insertColumn(status)
        zeldaRow = DatabaseRowEntity(pageId = games.id, title = "Zelda", linkedPageId = zelda.id, orderIndex = 0)
        db.databaseDao().insertRow(zeldaRow)
        zeldaCell = DatabaseCellEntity(rowId = zeldaRow.id, columnId = status.id, value = "Done")
        db.databaseDao().insertCell(zeldaCell)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun link(inPage: String, to: String, order: Int, type: BlockType = BlockType.PAGE_LINK): BlockEntity {
        val block = BlockEntity(pageId = inPage, type = type, linkedPageId = to, orderIndex = order)
        repo.saveBlock(block)
        return block
    }

    private suspend fun rowsOf(databaseId: String) = db.databaseDao().getRowsForPageOnce(databaseId).sortedBy { it.orderIndex }
    private suspend fun page(id: String) = db.pageDao().getById(id)
    private suspend fun linksTo(id: String) = db.blockDao().getBlocksLinkingTo(id)

    @Test
    fun aPageBecomesANewPageOfTheDatabase_andUndoPutsItBack() = runBlocking<Unit> {
        val moved = repo.moveIntoDatabase(notes.id, games.id)
        assertNotNull(moved)
        // Non sta più nel menu principale: è l'ultima riga di Games, col suo nome.
        assertTrue(linksTo(notes.id).isEmpty())
        val last = rowsOf(games.id).last()
        assertEquals(notes.id, last.linkedPageId)
        assertEquals("Notes", last.title)
        assertTrue(page(notes.id)!!.isRowPage)
        // Il contenuto è quello di prima.
        assertEquals(1, db.blockDao().getBlocksForPageOnce(notes.id).size)
        assertNull(moved!!.wrapperPageId)

        assertNotNull(repo.undoPageChange(moved))
        assertEquals(listOf(zeldaRow.id), rowsOf(games.id).map { it.id })
        assertFalse(page(notes.id)!!.isRowPage)
        assertEquals(listOf(notesLink.id), linksTo(notes.id).map { it.id })

        // Ripristina: di nuovo nel database.
        assertNotNull(repo.redoPageChange(moved))
        assertEquals(notes.id, rowsOf(games.id).last().linkedPageId)
    }

    @Test
    fun aDatabaseGoesIntoANewPageOfTheDatabase_openInside() = runBlocking<Unit> {
        val moved = repo.moveIntoDatabase(movies.id, games.id)
        assertNotNull(moved)
        val wrapperId = moved!!.wrapperPageId!!
        val wrapper = page(wrapperId)!!
        assertEquals("Movies", wrapper.title)
        assertEquals("🎬", wrapper.icon)
        assertTrue(wrapper.isRowPage)
        assertEquals(wrapperId, rowsOf(games.id).last().linkedPageId)
        // Dentro, il database stesso, aperto.
        val inside = linksTo(movies.id).single()
        assertEquals(wrapperId, inside.pageId)
        assertEquals(BlockType.DATABASE_LINK, inside.type)
        assertFalse(page(movies.id)!!.isRowPage)

        // Annulla: la pagina nata per lui se ne va, e lui torna al suo posto.
        assertNotNull(repo.undoPageChange(moved))
        assertNull(page(wrapperId))
        assertEquals(listOf(moviesLink.id), linksTo(movies.id).map { it.id })
        assertEquals(1, rowsOf(games.id).size)
    }

    @Test
    fun aRowPageLeavesItsDatabaseForTheOther() = runBlocking<Unit> {
        val moved = repo.moveIntoDatabase(zelda.id, movies.id)
        assertNotNull(moved)
        assertTrue(rowsOf(games.id).isEmpty())
        assertEquals(zelda.id, rowsOf(movies.id).single().linkedPageId)
        assertTrue(page(zelda.id)!!.isRowPage)

        // Annulla: torna in Games, con il valore della sua proprietà.
        assertNotNull(repo.undoPageChange(moved!!))
        assertTrue(rowsOf(movies.id).isEmpty())
        assertEquals(listOf(zeldaRow.id), rowsOf(games.id).map { it.id })
        assertEquals("Done", db.databaseDao().getCellsForRowOnce(zeldaRow.id).single().value)
    }

    @Test
    fun notIntoASimpleDatabaseNorIntoItselfNorAPage() = runBlocking<Unit> {
        val simple = PageEntity(title = "List", isDatabase = true, isSimpleDatabase = true)
        repo.createPage(simple)
        link(root.id, simple.id, 3, BlockType.DATABASE_LINK)
        assertNull(repo.moveIntoDatabase(notes.id, simple.id))
        // Un database dentro sé stesso no.
        assertNull(repo.moveIntoDatabase(games.id, games.id))
        // Una pagina che non è un database no.
        assertNull(repo.moveIntoDatabase(movies.id, notes.id))
        // La pagina principale no.
        assertNull(repo.moveIntoDatabase(root.id, games.id))
        // Niente è cambiato.
        assertEquals(listOf(notesLink.id), linksTo(notes.id).map { it.id })
        assertEquals(1, rowsOf(games.id).size)
    }

    @Test
    fun aLinkedViewStandsForItsDatabase() = runBlocking<Unit> {
        val view = PageEntity(title = "Games", isDatabase = true, sourceDatabaseId = games.id)
        repo.createPage(view)
        link(notes.id, view.id, 1, BlockType.DATABASE_LINK)
        val moved = repo.moveIntoDatabase(movies.id, view.id)
        assertEquals(games.id, moved!!.databaseId)
        assertEquals(moved.wrapperPageId, rowsOf(games.id).last().linkedPageId)
    }

    @Test
    fun searchFindsOnlyRealDatabases() = runBlocking<Unit> {
        val simple = PageEntity(title = "Games list", isDatabase = true, isSimpleDatabase = true)
        repo.createPage(simple)
        link(root.id, simple.id, 3, BlockType.DATABASE_LINK)
        val hits = repo.searchDatabaseDestinations("gam")
        assertEquals(listOf(games.id), hits.map { it.node.pageId })
        assertEquals("Root", hits.single().place)
    }
}
