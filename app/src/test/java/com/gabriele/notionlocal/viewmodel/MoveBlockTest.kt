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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Le frecce della barra.** Spostare una riga subito dopo averci scritto o
 * dopo un Invio riportava indietro il testo e le posizioni (l'utente: "sembra
 * che cancelli una riga in basso"); sopra un database il cursore restava
 * sotto ("sembra che non sposta niente"). Trovati il 28/09/2026.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MoveBlockTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova")
    private val database = PageEntity(title = "Spesa", isDatabase = true)

    private fun line(text: String, at: Int) =
        BlockEntity(pageId = page.id, textJson = """[{"text":"$text"}]""", orderIndex = at)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        pages.createPage(database)
        viewModel = PageEditorViewModel(pages)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun open(vararg blocks: BlockEntity) {
        runBlocking { db.blockDao().insertAll(blocks.toList()) }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == blocks.size }
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

    /** Un attimo in più: niente arriva dopo a rimettere le cose come prima. */
    private fun settle() = repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }

    private fun saved() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }

    private fun texts() = saved().map { viewModel.plainTextOf(it) }

    private fun shown(id: String) = viewModel.blocks.value.first { it.id == id }

    @Test
    fun theTextJustWrittenMovesWithTheLine() {
        val uno = line("Uno", 0)
        val due = line("Due", 1)
        val tre = line("Tre", 2)
        open(uno, due, tre)
        val ids = listOf(uno.id, due.id, tre.id)
        // Una lettera e subito la freccia: il database non ha ancora "Tre!".
        viewModel.updateRun(ids, listOf("Uno", "Due", "Tre!"), listOf("Uno", "Due", "Tre"), caretLine = 2)
        viewModel.moveBlockUp(shown(tre.id))
        waitFor("Tre! sopra Due") { texts() == listOf("Uno", "Tre!", "Due") }
        settle()
        assertEquals(listOf("Uno", "Tre!", "Due"), texts())
    }

    @Test
    fun afterAnEnterTheRightLineMoves() {
        val uno = line("Uno", 0)
        val due = line("Due", 1)
        val tre = line("Tre", 2)
        open(uno, due, tre)
        val ids = listOf(uno.id, due.id, tre.id)
        // Invio in fondo a "Uno" e subito la freccia su "Tre", con le posizioni di prima.
        val stale = shown(tre.id)
        viewModel.updateRun(ids, listOf("Uno", "", "Due", "Tre"), listOf("Uno", "Due", "Tre"), caretLine = 1)
        viewModel.moveBlockUp(stale)
        waitFor("quattro righe, Tre sopra Due") { texts() == listOf("Uno", "", "Tre", "Due") }
        settle()
        assertEquals(listOf("Uno", "", "Tre", "Due"), texts())
        assertEquals(saved().size, saved().map { it.orderIndex }.distinct().size)
    }

    @Test
    fun aboveTheDatabaseTheCursorGoesWithTheLine() {
        val sopra = line("Sopra", 0)
        val table = BlockEntity(pageId = page.id, type = BlockType.DATABASE_LINK, linkedPageId = database.id, orderIndex = 1)
        val sotto = line("Sotto", 2)
        val ultima = line("Ultima", 3)
        open(sopra, table, sotto, ultima)
        // Il cursore dopo "Sot" (il campo di sotto: righe "Sotto" e "Ultima").
        viewModel.reportSelection(listOf(sotto.id, ultima.id), 3, 3)
        viewModel.moveBlockUp(shown(sotto.id))
        waitFor("Sotto sopra il database") { saved().map { it.id } == listOf(sopra.id, sotto.id, table.id, ultima.id) }
        waitFor("il cursore su Sotto") { viewModel.focusRequestBlockId.value == sotto.id }
        assertEquals(sotto.id to 3, viewModel.pendingCaret.value)
        settle()
        assertEquals(listOf(sopra.id, sotto.id, table.id, ultima.id), saved().map { it.id })
    }

    @Test
    fun downAndBackUpIsWhereItWas() {
        val uno = line("Uno", 0)
        val due = line("Due", 1)
        open(uno, due)
        viewModel.moveBlockDown(shown(uno.id))
        waitFor("Due sopra Uno") { texts() == listOf("Due", "Uno") }
        waitFor("la lista mostrata") { viewModel.blocks.value.map { it.id } == listOf(due.id, uno.id) }
        viewModel.moveBlockUp(shown(uno.id))
        waitFor("di nuovo Uno sopra Due") { texts() == listOf("Uno", "Due") }
    }

    @Test
    fun twoLinesWithTheSameIndexStillMove() {
        val uno = line("Uno", 0)
        val due = line("Due", 1)
        val tre = line("Tre", 1)
        open(uno, due, tre)
        val order = viewModel.blocks.value.map { it.id }
        val last = viewModel.blocks.value.last()
        viewModel.moveBlockUp(last)
        val expected = order.toMutableList().apply { add(1, removeAt(2)) }
        waitFor("l'ultima sale di uno") { saved().map { it.id } == expected }
        assertEquals(listOf(0, 1, 2), saved().map { it.orderIndex })
    }
}
