package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * **Il collegamento sul testo selezionato** (17/10/2026): l'icona della
 * barra mette da parte la selezione, la finestrella ci mette l'indirizzo.
 * Col solo cursore dentro un link si cambia o si toglie tutto il link.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TextLinkTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova")
    private val first = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"Cerca su Google"}]""", orderIndex = 0)
    private val second = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"oppure Bing"}]""", orderIndex = 1)
    private val run = listOf(first.id, second.id)
    private val google = "https://google.com"

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(first, second))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 2 }
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

    private fun saved(block: BlockEntity) =
        viewModel.spansOf(runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.single { it.id == block.id })

    @Test
    fun aLinkOnTheSelectedWord() {
        // "Google" selezionato: l'icona compare.
        viewModel.reportSelection(run, 9, 15)
        assertTrue(viewModel.selectionLink.value.hasText)
        assertNull(viewModel.selectionLink.value.link)
        viewModel.captureSelection()
        assertNull(viewModel.capturedLink())
        assertTrue(viewModel.applyLinkToCapturedSelection(google))
        waitFor("il link salvato") {
            saved(first) == listOf(RichTextSpan(text = "Cerca su "), RichTextSpan(text = "Google", link = google))
        }
        assertEquals(listOf(RichTextSpan(text = "oppure Bing")), saved(second))
    }

    @Test
    fun aLinkAcrossTwoLines() {
        // Da "Google" a "oppure": ogni riga ha la sua parte.
        viewModel.reportSelection(run, 9, 22)
        viewModel.captureSelection()
        assertTrue(viewModel.applyLinkToCapturedSelection(google))
        waitFor("tutte e due le righe") {
            saved(first) == listOf(RichTextSpan(text = "Cerca su "), RichTextSpan(text = "Google", link = google)) &&
                saved(second) == listOf(RichTextSpan(text = "oppure", link = google), RichTextSpan(text = " Bing"))
        }
    }

    @Test
    fun theCaretInsideALinkChangesOrRemovesAllOfIt() {
        viewModel.reportSelection(run, 9, 15)
        viewModel.captureSelection()
        viewModel.applyLinkToCapturedSelection(google)
        waitFor("il link") { viewModel.spansOf(viewModel.blocks.value.first()).any { it.link == google } }

        // Il solo cursore in mezzo a "Google": l'icona c'è, col link di adesso.
        viewModel.reportSelection(run, 12, 12)
        assertFalse(viewModel.selectionLink.value.hasText)
        assertEquals(google, viewModel.selectionLink.value.link)
        viewModel.captureSelection()
        assertEquals(google, viewModel.capturedLink())

        // "Remove link" lo toglie tutto, non solo dov'è il cursore.
        assertTrue(viewModel.applyLinkToCapturedSelection(null))
        waitFor("il link tolto") { saved(first) == listOf(RichTextSpan(text = "Cerca su Google")) }
    }

    @Test
    fun nothingSelectedNothingToLink() {
        viewModel.reportSelection(run, 3, 3)
        assertFalse(viewModel.selectionLink.value.hasText)
        assertNull(viewModel.selectionLink.value.link)
        viewModel.captureSelection()
        assertFalse(viewModel.applyLinkToCapturedSelection(google))
    }
}
