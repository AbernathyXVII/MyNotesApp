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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Il "+" su più righe selezionate** (14/10/2026): "Se seleziono del testo,
 * e poi premo To do list, mi fa la checkbox solo nel campo di testo dove è
 * presente il text cursor". Ora cambiano tutte le righe toccate dalla
 * selezione, nell'ordine, col testo com'era.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TurnIntoManyTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova")

    // "Uno" in un altro font, una riga vuota, "Tre" con un pallino, "Quattro" grande:
    // nel testo condiviso Uno = 0..3, vuota = 4, Tre = 5..8, Quattro = 9..16.
    private val one = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"Uno","font":"SERIF"}]""", orderIndex = 0)
    private val empty = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":""}]""", orderIndex = 1)
    private val three = BlockEntity(pageId = page.id, type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Tre"}]""", orderIndex = 2)
    private val four = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"Quattro","size":30}]""", orderIndex = 3)
    private val run = listOf(one, empty, three, four).map { it.id }

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(one, empty, three, four))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 4 }
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
        throw AssertionError("Non è successo: $what")
    }

    private fun saved() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }

    private fun selected(start: Int, end: Int): List<String> {
        viewModel.reportSelection(run, start, end)
        viewModel.captureSelection()
        return viewModel.capturedSelectionBlocks().map { it.id }
    }

    @Test
    fun theSelectionTouchesTheRightRows() {
        // Da "U|no" a "Tr|e": le prime tre, compresa quella vuota in mezzo.
        assertEquals(listOf(one.id, empty.id, three.id), selected(1, 7))
        // Fino all'inizio di "Quattro" (l'a-capo prima compreso): lei no.
        assertEquals(listOf(one.id, empty.id, three.id), selected(0, 9))
        // Dalla fine di "Uno": la sua parte è solo l'a-capo, non conta.
        assertEquals(listOf(empty.id, three.id), selected(3, 8))
        // Dentro una riga sola: solo lei.
        assertEquals(listOf(four.id), selected(10, 12))
        // Solo il cursore: nessuna, vale la riga del cursore.
        assertEquals(emptyList<String>(), selected(6, 6))
    }

    @Test
    fun allTheSelectedRowsBecomeNumberedInOrderAndUndoTakesThemBack() {
        selected(1, 7)
        viewModel.updateBlockTypes(viewModel.capturedSelectionBlocks(), BlockType.NUMBERED_LIST_ITEM)
        waitFor("tre righe numerate") {
            saved().map { it.type } == listOf(
                BlockType.NUMBERED_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.PARAGRAPH
            )
        }
        // Il testo resta com'era, font e corpo compresi.
        assertEquals(listOf(RichTextSpan(text = "Uno", font = "SERIF")), viewModel.spansOf(saved()[0]))
        assertEquals(listOf(RichTextSpan(text = "Quattro", size = 30)), viewModel.spansOf(saved()[3]))
        // Annulla, una volta: tutte com'erano.
        viewModel.undo()
        waitFor("rimesse come prima") {
            saved().map { it.type } == listOf(
                BlockType.PARAGRAPH, BlockType.PARAGRAPH, BlockType.BULLET_LIST_ITEM, BlockType.PARAGRAPH
            )
        }
    }

    @Test
    fun allTheSelectedRowsBecomeCheckboxes() {
        selected(0, 16)
        viewModel.updateBlockTypes(viewModel.capturedSelectionBlocks(), BlockType.CHECKBOX)
        waitFor("quattro caselle") { saved().all { it.type == BlockType.CHECKBOX } }
    }
}
