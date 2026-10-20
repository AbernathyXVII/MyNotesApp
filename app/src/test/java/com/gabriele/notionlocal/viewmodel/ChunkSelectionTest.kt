package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.RenderItem
import com.gabriele.notionlocal.ui.screen.groupIntoRenderItems
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * **La selezione passa da un pezzo all'altro** (19/10/2026, trovato
 * dall'utente in Permanent notes: trascinando, la selezione si fermava a
 * "Kor", l'ultima riga di un pezzo di 50 righe). Una pagina di 120 righe: tre
 * pezzi, 50 + 50 + 20.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChunkSelectionTest {

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

    @Test
    fun atTheLastLineTheNextPieceComesIn() {
        assertEquals(listOf(50, 50, 20), runs().map { it.blocks.size })
        // La selezione è arrivata a "riga 49", l'ultima del primo pezzo.
        assertTrue(viewModel.joinChunkAfter(lines[49].id))
        assertEquals(listOf(100, 20), runs().map { it.blocks.size })
        // Il campo che resta è lo stesso: le righe gli arrivano in fondo.
        assertEquals("run:top:" + page.id, runs()[0].key)
        // E così via, fino all'ultimo pezzo; dopo non c'è più niente da unire.
        assertTrue(viewModel.joinChunkAfter(lines[99].id))
        assertEquals(listOf(120), runs().map { it.blocks.size })
        assertFalse(viewModel.joinChunkAfter(lines[119].id))
    }

    @Test
    fun aPieceInTheMiddleTakesTheNextOne() {
        assertTrue(viewModel.joinChunkAfter(lines[99].id))
        assertEquals(listOf(50, 70), runs().map { it.blocks.size })
        assertEquals("chunk:" + lines[50].id, runs()[1].key)
    }

    /** Le righe da "riga 45" a "riga 55": il confine fra il primo e il secondo pezzo si vede. */
    private val onScreen get() = lines.subList(45, 56).map { it.id }

    @Test
    fun theLinesOnScreenBecomeOneField() {
        assertFalse(viewModel.keepVisibleTogether(onScreen, focusedId = null))
        assertEquals(listOf(100, 20), runs().map { it.blocks.size })
        // Senza fuoco resta il campo di sopra, e le righe di sotto gli arrivano in fondo.
        assertEquals("run:top:" + page.id, runs()[0].key)
        // Niente da fare se il confine non si vede.
        assertFalse(viewModel.keepVisibleTogether(lines.subList(60, 70).map { it.id }, focusedId = null))
        assertEquals(listOf(100, 20), runs().map { it.blocks.size })
    }

    @Test
    fun theFocusedFieldStaysAndTakesTheLinesAbove() {
        // Il fuoco è su "riga 52", nel secondo pezzo: il suo campo non si butta via.
        assertFalse(viewModel.keepVisibleTogether(onScreen, focusedId = lines[52].id))
        assertEquals(listOf(100, 20), runs().map { it.blocks.size })
        assertEquals("chunk:" + lines[50].id, runs()[0].key)
    }

    @Test
    fun whileWritingTheFocusedFieldIsLeftAlone() {
        viewModel.updateRun(listOf(lines[52].id), listOf("riga 52 e altro"), listOf("riga 52"), caretLine = 0)
        // Ci si sta scrivendo: il campo col fuoco non si tocca, e la pagina riproverà.
        assertTrue(viewModel.keepVisibleTogether(onScreen, focusedId = lines[52].id))
        assertEquals(listOf(50, 50, 20), runs().map { it.blocks.size })
    }

    @Test
    fun aNewSplitDoesNotFallBetweenLinesOnScreen() {
        viewModel.keepVisibleTogether(onScreen, focusedId = null)
        // Quando non si scrive i pezzi si ridividono, ma non fra due righe sullo schermo.
        viewModel.setEditing(true)
        viewModel.setEditing(false)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        val first = runs()[0].blocks.map { it.id }
        assertTrue(first.containsAll(onScreen))
        assertTrue(runs().all { it.blocks.size <= PageEditorViewModel.CHUNK_MAX_LINES + onScreen.size })
    }

    @Test
    fun whenTheWritingIsOverThePiecesSplitAgain() {
        viewModel.setEditing(true)
        assertTrue(viewModel.joinChunkAfter(lines[49].id))
        assertTrue(viewModel.joinChunkAfter(lines[99].id))
        assertEquals(listOf(120), runs().map { it.blocks.size })
        // Mentre si scrive i pezzi restano uniti...
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(listOf(120), runs().map { it.blocks.size })
        // ...poi tornano da 50 righe.
        viewModel.setEditing(false)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(listOf(50, 50, 20), runs().map { it.blocks.size })
    }
}
