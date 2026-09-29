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
 * **Sotto un divisore** (29/09/2026): il backspace a inizio riga non faceva
 * niente ("sotto un divider non posso cancellare nulla"), e una riga vuota
 * spostata oltre il divisore sembrava sparire.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DividerBackspaceTest {

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

    private fun saved() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }

    @Test
    fun underADividerTheFirstBackspaceTakesItAndTheSecondRemovesIt() {
        val sopra = line("Sopra", 0)
        val divider = BlockEntity(pageId = page.id, type = BlockType.DIVIDER, orderIndex = 1)
        val sotto = line("Sotto", 2)
        val ultima = line("Ultima", 3)
        open(sopra, divider, sotto, ultima)
        // Il primo lo "prende" (si accende) e non toglie niente.
        assertEquals(BackspaceOutcome.STEP_UNDONE, viewModel.backspaceAtLineStartDetailed(listOf(sotto.id, ultima.id), 0))
        assertEquals(divider.id, viewModel.armedDivider.value)
        repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertEquals(4, saved().size)
        // Il secondo lo toglie: la riga resta col suo testo, il cursore all'inizio.
        assertEquals(BackspaceOutcome.MERGED_INTO_ISLAND, viewModel.backspaceAtLineStartDetailed(listOf(sotto.id, ultima.id), 0))
        waitFor("il divisore tolto") { saved().map { it.id } == listOf(sopra.id, sotto.id, ultima.id) }
        assertEquals("Sotto", viewModel.plainTextOf(saved()[1]))
        waitFor("il cursore sulla riga") { viewModel.focusRequestBlockId.value == sotto.id }
        assertEquals(sotto.id to 0, viewModel.pendingCaret.value)
        assertEquals(null, viewModel.armedDivider.value)
    }

    /**
     * **Il bug del 29/09/2026**: cancellando le righe vuote fino al divisore,
     * all'ultima spariva il divisore e la riga vuota restava.
     */
    @Test
    fun anEmptyLineUnderADividerGoesAwayAndTheDividerStays() {
        val sopra = line("Sopra", 0)
        val divider = BlockEntity(pageId = page.id, type = BlockType.DIVIDER, orderIndex = 1)
        val vuota = line("", 2)
        val titolo = line("Titolo uno H1", 3)
        open(sopra, divider, vuota, titolo)
        assertEquals(BackspaceOutcome.MERGED_INTO_ISLAND, viewModel.backspaceAtLineStartDetailed(listOf(vuota.id, titolo.id), 0))
        waitFor("la riga vuota tolta") { saved().map { it.id } == listOf(sopra.id, divider.id, titolo.id) }
        // Il cursore all'inizio della riga salita al suo posto.
        waitFor("il cursore sul titolo") { viewModel.focusRequestBlockId.value == titolo.id }
        assertEquals(titolo.id to 0, viewModel.pendingCaret.value)
        // E il backspace dopo non lo toglie subito: prima lo prende.
        assertEquals(BackspaceOutcome.STEP_UNDONE, viewModel.backspaceAtLineStartDetailed(listOf(titolo.id), 0))
        assertEquals(3, saved().size)
    }

    @Test
    fun anEmptyLastLineUnderADividerStays() {
        val divider = BlockEntity(pageId = page.id, type = BlockType.DIVIDER, orderIndex = 0)
        val vuota = line("", 1)
        open(divider, vuota)
        assertEquals(BackspaceOutcome.NOTHING, viewModel.backspaceAtLineStartDetailed(listOf(vuota.id), 0))
        repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertEquals(listOf(divider.id, vuota.id), saved().map { it.id })
    }

    @Test
    fun aTakenDividerLetsGoByItself() {
        val divider = BlockEntity(pageId = page.id, type = BlockType.DIVIDER, orderIndex = 0)
        val sotto = line("Sotto", 1)
        open(divider, sotto)
        viewModel.backspaceAtLineStartDetailed(listOf(sotto.id), 0)
        assertEquals(divider.id, viewModel.armedDivider.value)
        // Il tempo del test va avanti solo a mano.
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(PageEditorViewModel.ARMED_DIVIDER_MS + 100))
        waitFor("si spegne da solo") { viewModel.armedDivider.value == null }
        assertEquals(2, saved().size)
    }

    @Test
    fun backspaceUnderADatabaseDoesNothing() {
        val table = BlockEntity(pageId = page.id, type = BlockType.DATABASE_LINK, linkedPageId = database.id, orderIndex = 0)
        val sotto = line("Sotto", 1)
        open(table, sotto)
        assertEquals(BackspaceOutcome.NOTHING, viewModel.backspaceAtLineStartDetailed(listOf(sotto.id), 0))
        repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertEquals(listOf(table.id, sotto.id), saved().map { it.id })
    }

    @Test
    fun anEmptyLineMovedPastTheDividerIsMarkedToLightUp() {
        val vuota = line("", 0)
        val divider = BlockEntity(pageId = page.id, type = BlockType.DIVIDER, orderIndex = 1)
        val sotto = line("", 2)
        open(vuota, divider, sotto)
        viewModel.moveBlockDown(viewModel.blocks.value.first { it.id == vuota.id })
        waitFor("la riga sotto il divisore") { saved().map { it.id } == listOf(divider.id, vuota.id, sotto.id) }
        waitFor("segnata per il velo") { viewModel.movedBlock.value?.blockId == vuota.id }
    }
}
