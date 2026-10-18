package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.RenderItem
import com.gabriele.notionlocal.ui.screen.groupIntoRenderItems
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * **Le pagine lunghe divise in pezzi** (18/10/2026, chiesto dall'utente:
 * "l'app deve reggere tante scritte"): ogni pezzo è un campo di testo a sé,
 * e i confini non si spostano sotto il campo in cui si scrive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageChunksTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Lunga")
    private val lines = (0 until 120).map { i ->
        BlockEntity(pageId = page.id, textJson = """[{"text":"riga $i"}]""", orderIndex = i)
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(lines)
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == lines.size }
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

    private fun top() = viewModel.blocks.value.filter { it.parentBlockId == null }

    /** I gruppi di righe come li disegna la pagina. */
    private fun runs() = groupIntoRenderItems(top(), viewModel.chunkStartsOf(top())).filterIsInstance<RenderItem.Run>()

    private fun text(id: String) = viewModel.plainTextOf(viewModel.blocks.value.single { it.id == id })

    @Test
    fun aLongPageBecomesSeveralFields() {
        val runs = runs()
        assertEquals(listOf(50, 50, 20), runs.map { it.blocks.size })
        assertEquals("run:top:" + page.id, runs[0].key)
        assertEquals("chunk:" + lines[50].id, runs[1].key)
        assertEquals("chunk:" + lines[100].id, runs[2].key)
        assertTrue(runs.all { it.blocks.size <= PageEditorViewModel.CHUNK_MAX_LINES })
    }

    @Test
    fun writingInAPieceTouchesOnlyThatPiece() {
        val second = runs()[1].blocks
        val before = second.map { viewModel.plainTextOf(it) }
        viewModel.updateRun(second.map { it.id }, listOf("riga 50 scritta") + before.drop(1), before, caretLine = 0)
        waitFor("la riga scritta") { text(lines[50].id) == "riga 50 scritta" }
        assertEquals("riga 49", text(lines[49].id))
        assertEquals("riga 51", text(lines[51].id))
        assertEquals(listOf(50, 50, 20), runs().map { it.blocks.size })
    }

    @Test
    fun enterAtTheStartOfAPieceKeepsThePieceWhole() {
        val second = runs()[1]
        val before = second.blocks.map { viewModel.plainTextOf(it) }
        // Invio a inizio della prima riga del pezzo: la riga vuota nasce sopra.
        viewModel.updateRun(second.blocks.map { it.id }, listOf("") + before, before, caretLine = 1)
        waitFor("la riga nuova") { viewModel.blocks.value.size == lines.size + 1 }
        val after = runs()
        // Il pezzo resta lo stesso (stessa chiave) e la riga nuova è la sua prima:
        // il campo in cui si scrive ha ancora tutte le sue righe.
        assertEquals(second.key, after[1].key)
        assertEquals("", viewModel.plainTextOf(after[1].blocks.first()))
        assertEquals(lines[50].id, after[1].blocks[1].id)
        assertEquals(listOf(50, 51, 20), after.map { it.blocks.size })
    }

    @Test
    fun backspaceAtTheStartOfAPieceJoinsTheLineAbove() {
        val second = runs()[1]
        val outcome = viewModel.backspaceAtLineStartDetailed(second.blocks.map { it.id }, 0)
        assertEquals(BackspaceOutcome.MERGED_INTO_ISLAND, outcome)
        waitFor("le righe unite") { viewModel.blocks.value.none { it.id == lines[50].id } }
        assertEquals("riga 49riga 50", text(lines[49].id))
        // Il cursore va dove si sono unite, nella riga di sopra.
        waitFor("il cursore") { viewModel.focusRequestBlockId.value == lines[49].id }
        assertEquals(lines[49].id to "riga 49".length, viewModel.pendingCaret.value)
        // Il pezzo continua dalla sua seconda riga, con la stessa chiave.
        val after = runs()
        assertEquals(second.key, after[1].key)
        assertEquals(lines[51].id, after[1].blocks.first().id)
    }

    @Test
    fun piecesAreDividedAgainOnlyWhenNotWriting() {
        viewModel.setEditing(true)
        val first = runs()[0]
        val before = first.blocks.map { viewModel.plainTextOf(it) }
        // Trenta righe nuove in fondo al primo pezzo, scrivendo.
        val added = (0 until 30).map { "nuova $it" }
        viewModel.updateRun(first.blocks.map { it.id }, before + added, before, caretLine = before.size + added.size - 1)
        waitFor("le righe nuove") { viewModel.blocks.value.size == lines.size + 30 }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        // Mentre si scrive il pezzo si allunga e basta.
        assertEquals(80, runs()[0].blocks.size)
        // Smesso di scrivere, dopo un attimo si ridivide.
        viewModel.setEditing(false)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        waitFor("i pezzi rifatti") { runs()[0].blocks.size == 50 }
        assertTrue(runs().all { it.blocks.size <= PageEditorViewModel.CHUNK_MAX_LINES })
        assertEquals(lines.size + 30, runs().sumOf { it.blocks.size })
    }

    @Test
    fun theNumbersInOneGoAreTheSameAsBefore() {
        val parent = "p"
        val mixed = listOf(
            BlockEntity(pageId = page.id, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 0),
            BlockEntity(pageId = page.id, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 1, indentLevel = 1),
            BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, orderIndex = 2),
            BlockEntity(pageId = page.id, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 3, indentLevel = 1),
            BlockEntity(pageId = page.id, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 4),
            BlockEntity(pageId = page.id, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 5, numberStartsAt = 7),
            BlockEntity(pageId = page.id, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 6),
            BlockEntity(pageId = page.id, parentBlockId = parent, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 0),
            BlockEntity(pageId = page.id, parentBlockId = parent, type = BlockType.NUMBERED_LIST_ITEM, orderIndex = 1)
        )
        val inOneGo = viewModel.numberedOrdinals(mixed)
        mixed.filter { it.type == BlockType.NUMBERED_LIST_ITEM }.forEach { block ->
            assertEquals(block.toString(), viewModel.numberedListOrdinal(mixed, block), inOneGo[block.id])
        }
        assertEquals(listOf(1, 1, 2, 2, 7, 8, 1, 2), mixed.filter { it.type == BlockType.NUMBERED_LIST_ITEM }.map { inOneGo[it.id] })
    }
}
