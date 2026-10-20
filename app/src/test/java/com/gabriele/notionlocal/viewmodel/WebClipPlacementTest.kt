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
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.entity.inReadingOrder
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Dove va la foto del Web Clipper, e "Move to line…"** (20/10/2026): al
 * posto della prima riga vuota, con le righe della didascalia sotto; se non
 * ce n'è, in fondo. Poi il blocco si mette sotto qualsiasi riga.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WebClipPlacementTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Guide")
    private val title = BlockEntity(pageId = page.id, textJson = """[{"text":"Shin Megami Tensei"}]""", orderIndex = 0)
    private val empty = BlockEntity(pageId = page.id, textJson = "[]", orderIndex = 1)
    private val toggle = BlockEntity(pageId = page.id, type = BlockType.TOGGLE, textJson = """[{"text":"Capitoli"}]""", orderIndex = 2)
    private val chapter = BlockEntity(pageId = page.id, parentBlockId = toggle.id, textJson = """[{"text":"Prologo"}]""", orderIndex = 0)
    private val last = BlockEntity(pageId = page.id, textJson = """[{"text":"Fine"}]""", orderIndex = 3)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(title, empty, toggle, chapter, last))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 5 }
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

    private fun saved() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }

    /** La pagina come si legge: id, oppure il testo per le righe nuove. */
    private fun reading(): List<String> = saved().inReadingOrder().map { (block, _) ->
        when {
            block.id == title.id -> "title"
            block.id == empty.id -> "empty"
            block.id == toggle.id -> "toggle"
            block.id == chapter.id -> "chapter"
            block.id == last.id -> "last"
            else -> viewModel.plainTextOf(block)
        }
    }

    private val captions = listOf(
        listOf(RichTextSpan(text = "Prologue", bold = true)),
        listOf(RichTextSpan(text = "Clipped from "), RichTextSpan(text = "https://game8.co/x", link = "https://game8.co/x"))
    )

    @Test
    fun theClipTakesTheFirstEmptyLineAndTheCaptionsGoUnderIt() {
        viewModel.insertWebClip(BlockType.IMAGE, BlockAttrs(file = "clip.jpg", name = "Prologue"), captions)
        waitFor("la foto") { saved().any { it.id == empty.id && it.type == BlockType.IMAGE } }
        val clip = saved().single { it.id == empty.id }
        assertEquals("clip.jpg", clip.attrs.file)
        assertEquals(
            listOf("title", "empty", "Prologue", "Clipped from https://game8.co/x", "toggle", "chapter", "last"),
            reading()
        )
        // La foto si porta in vista.
        waitFor("la richiesta di mostrarla") { viewModel.revealBlock.value == empty.id }
    }

    @Test
    fun withoutAnEmptyLineTheClipGoesAtTheEnd() = runBlocking<Unit> {
        db.blockDao().delete(empty)
        waitFor("senza riga vuota") { viewModel.blocks.value.size == 4 }
        viewModel.insertWebClip(BlockType.BOOKMARK, BlockAttrs(url = "https://game8.co/x"), emptyList())
        waitFor("il segnalibro") { saved().any { it.type == BlockType.BOOKMARK } }
        val bookmark = saved().single { it.type == BlockType.BOOKMARK }
        assertEquals(null, bookmark.parentBlockId)
        assertEquals("https://game8.co/x", bookmark.attrs.url)
        assertEquals(listOf("title", "toggle", "chapter", "last", ""), reading())
    }

    @Test
    fun moveToLinePutsTheBlockUnderTheChosenLine() {
        val image = { saved().single { it.id == title.id } }
        // Il titolo sotto "Prologo", dentro il toggle.
        viewModel.moveBlockAfter(viewModel.blocks.value.single { it.id == title.id }, chapter.id)
        waitFor("dentro il toggle") { image().parentBlockId == toggle.id }
        assertEquals(listOf("empty", "toggle", "chapter", "title", "last"), reading())
        // Poi in cima alla pagina.
        viewModel.moveBlockAfter(viewModel.blocks.value.single { it.id == title.id }, null)
        waitFor("in cima") { image().parentBlockId == null && reading().first() == "title" }
        assertEquals(listOf("title", "empty", "toggle", "chapter", "last"), reading())
        // Sotto l'ultima riga.
        viewModel.moveBlockAfter(viewModel.blocks.value.single { it.id == title.id }, last.id)
        waitFor("in fondo") { reading().last() == "title" }
        assertEquals(listOf("empty", "toggle", "chapter", "last", "title"), reading())
        // Gli indici restano in fila, senza buchi né doppioni.
        val top = saved().filter { it.parentBlockId == null }.map { it.orderIndex }.sorted()
        assertEquals(top.indices.toList(), top)
    }

    @Test
    fun aToggleCannotGoInsideItself() = runBlocking<Unit> {
        assertFalse(pages.moveBlockAfter(toggle.id, chapter.id))
        assertFalse(pages.moveBlockAfter(toggle.id, toggle.id))
        assertEquals(listOf("title", "empty", "toggle", "chapter", "last"), reading())
    }
}
