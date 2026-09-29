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
    fun backspaceUnderADividerRemovesTheDividerAndKeepsTheLine() {
        val sopra = line("Sopra", 0)
        val divider = BlockEntity(pageId = page.id, type = BlockType.DIVIDER, orderIndex = 1)
        val sotto = line("Sotto", 2)
        val ultima = line("Ultima", 3)
        open(sopra, divider, sotto, ultima)
        val outcome = viewModel.backspaceAtLineStartDetailed(listOf(sotto.id, ultima.id), 0)
        assertEquals(BackspaceOutcome.MERGED_INTO_ISLAND, outcome)
        waitFor("il divisore tolto") { saved().map { it.id } == listOf(sopra.id, sotto.id, ultima.id) }
        // Il testo resta, e il cursore torna all'inizio della riga.
        assertEquals("Sotto", viewModel.plainTextOf(saved()[1]))
        waitFor("il cursore sulla riga") { viewModel.focusRequestBlockId.value == sotto.id }
        assertEquals(sotto.id to 0, viewModel.pendingCaret.value)
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
