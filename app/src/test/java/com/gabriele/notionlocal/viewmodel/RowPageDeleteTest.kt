package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Le pagine delle righe cancellate** (08/10/2026): cancellando una riga se
 * ne va anche la sua pagina, coi suoi blocchi e i suoi file; quelle rimaste
 * da prima vanno nel cestino all'avvio, e ripristinate tornano pagine normali.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RowPageDeleteTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private val root = PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space")
    private val games = PageEntity(title = "Giochi", isDatabase = true)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(root)
        pages.createPage(games)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    private fun page(id: String) = runBlocking { db.pageDao().getById(id) }

    @Test
    fun deletingARowDeletesItsPage_itsBlocksAndItsFiles() {
        val store = PageImageStore(ApplicationProvider.getApplicationContext())
        val file = "foto-riga.png"
        store.fileFor(file).apply { parentFile?.mkdirs() }.writeBytes(byteArrayOf(1, 2, 3))
        val rowPage = PageEntity(title = "Zelda", isRowPage = true, iconImage = null)
        val zelda = DatabaseRowEntity(pageId = games.id, title = "Zelda", orderIndex = 0, linkedPageId = rowPage.id)
        val halo = DatabaseRowEntity(pageId = games.id, title = "Halo", orderIndex = 1)
        runBlocking {
            db.pageDao().insert(rowPage)
            db.databaseDao().insertRow(zelda)
            db.databaseDao().insertRow(halo)
            db.blockDao().insert(
                BlockEntity(pageId = rowPage.id, type = BlockType.IMAGE, attrsJson = BlockAttrs.encode(BlockAttrs(file = file)), orderIndex = 0)
            )
        }

        val viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(games.id)
        waitFor("le righe") { viewModel.tableState.value.rows.size == 2 }
        viewModel.deleteRow(zelda, store)
        waitFor("la pagina della riga cancellata") { page(rowPage.id) == null }
        assertTrue(runBlocking { db.blockDao().getBlocksForPageOnce(rowPage.id) }.isEmpty())
        waitFor("il file") { !store.fileFor(file).exists() }

        // Una riga senza pagina si cancella come prima.
        viewModel.deleteRow(halo, store)
        waitFor("l'altra riga") { runBlocking { db.databaseDao().getRowById(halo.id) } == null }
        assertNotNull(page(games.id))
    }

    @Test
    fun theLeftoverPagesGoToTheTrash_andComeBackAsNormalPages() = runBlocking<Unit> {
        val leftover = PageEntity(title = "Rimasta", isRowPage = true)
        val linkedElsewhere = PageEntity(title = "Collegata", isRowPage = true)
        val stillInRow = PageEntity(title = "Nella riga", isRowPage = true)
        val template = PageEntity(title = "", isRowPage = true, isTemplate = true)
        listOf(leftover, linkedElsewhere, stillInRow, template).forEach { db.pageDao().insert(it) }
        db.databaseDao().insertRow(DatabaseRowEntity(pageId = games.id, title = "Nella riga", orderIndex = 0, linkedPageId = stillInRow.id))
        db.blockDao().insert(BlockEntity(pageId = root.id, type = BlockType.PAGE_LINK, linkedPageId = linkedElsewhere.id, orderIndex = 5))

        assertEquals(1, pages.tidyOrphanRowPages())
        assertNotNull(page(leftover.id)!!.trashedAt)
        // Un'altra pagina la richiama: resta dov'è, come pagina normale.
        assertNull(page(linkedElsewhere.id)!!.trashedAt)
        assertFalse(page(linkedElsewhere.id)!!.isRowPage)
        assertNull(page(stillInRow.id)!!.trashedAt)
        assertTrue(page(stillInRow.id)!!.isRowPage)
        assertNull(page(template.id)!!.trashedAt)
        // Una seconda volta non c'è più niente da fare.
        assertEquals(0, pages.tidyOrphanRowPages())

        pages.restoreFromTrash(leftover.id)
        val restored = page(leftover.id)!!
        assertNull(restored.trashedAt)
        assertFalse(restored.isRowPage)
        assertTrue(db.blockDao().getBlocksLinkingTo(leftover.id).any { it.pageId == PageEntity.ROOT_PAGE_ID })
    }
}
