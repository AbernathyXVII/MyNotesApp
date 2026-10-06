package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
 * **"Link to page" e "Database - Full page"** (06/10/2026): il rimando a una
 * pagina che sta altrove, e il database che si apre a pagina intera.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LinkToPageTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Qui")
    private val altrove = PageEntity(title = "Altrove")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PageRepository(db)
        repository.createPage(page)
        repository.createPage(altrove)
        viewModel = PageEditorViewModel(repository)
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

    private fun stored() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }

    private fun openWith(text: String): BlockEntity {
        val riga = BlockEntity(pageId = page.id, textJson = """[{"text":"$text"}]""", orderIndex = 0)
        runBlocking { db.blockDao().insert(riga) }
        viewModel.load(page.id)
        waitFor("la riga") { viewModel.blocks.value.size == 1 }
        return viewModel.blocks.value.first()
    }

    @Test
    fun aFullPageDatabaseIsALinkToANewDatabaseAndTheCommandIsGone() {
        val riga = openWith("/database")
        var opened: String? = null
        viewModel.convertToFullPageDatabase(riga) { opened = it }
        waitFor("il database") { opened != null }

        val block = stored().single()
        assertEquals(BlockType.PAGE_LINK, block.type)
        assertEquals(opened, block.linkedPageId)
        assertEquals("[]", block.textJson)
        val database = runBlocking { db.pageDao().getById(opened!!) }!!
        assertTrue(database.isDatabase)
        assertEquals("Untitled", database.title)
    }

    @Test
    fun turnedIntoAFullPageDatabaseTheTextBecomesItsName() {
        val riga = openWith("Libri")
        var opened: String? = null
        viewModel.convertToFullPageDatabase(riga, title = "Libri") { opened = it }
        waitFor("il database") { opened != null }
        assertEquals("Libri", runBlocking { db.pageDao().getById(opened!!) }!!.title)
    }

    @Test
    fun aLinkToAPageDoesNotPutThePageHere() {
        val riga = openWith("/link")
        viewModel.convertToPageMention(riga, altrove.id)
        waitFor("il rimando") { stored().single().type == BlockType.PAGE_MENTION }

        val block = stored().single()
        assertEquals(BlockAttrs(pageId = altrove.id), block.attrs)
        assertEquals("[]", block.textJson)
        // Non è un collegamento dell'albero: la pagina resta dov'era.
        assertNull(block.linkedPageId)
        assertEquals(0, runBlocking { db.blockDao().countLinksInPage(page.id) })
    }

    @Test
    fun theLinkStaysWhenThePageGoesToTheTrash() = runBlocking<Unit> {
        val riga = openWith("/link")
        viewModel.convertToPageMention(riga, altrove.id)
        waitFor("il rimando") { stored().single().type == BlockType.PAGE_MENTION }

        repository.moveToTrash(altrove.id)
        val block = stored().single()
        assertEquals(BlockType.PAGE_MENTION, block.type)
        assertNotNull(db.pageDao().getById(altrove.id)!!.trashedAt)
    }
}
