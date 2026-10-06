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
import com.gabriele.notionlocal.data.repository.PageRepository
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

/**
 * **Riquadro e citazione** (06/10/2026): si scrivono su più righe dentro
 * un blocco solo; tornando testo, le righe diventano blocchi; il riquadro
 * ha un'icona e uno sfondo suoi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CalloutQuoteTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PageRepository(db)
        repository.createPage(page)
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

    private fun stored() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }

    private fun open(vararg blocks: BlockEntity) {
        runBlocking { db.blockDao().insertAll(blocks.toList()) }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == blocks.size }
    }

    @Test
    fun aCalloutWithThreeLinesBecomesThreeLinesOfText() {
        val callout = BlockEntity(
            pageId = page.id,
            type = BlockType.CALLOUT,
            textJson = """[{"text":"Uno\n"},{"text":"Due","bold":true},{"text":"\nTre"}]""",
            orderIndex = 0
        )
        val sotto = BlockEntity(pageId = page.id, textJson = """[{"text":"Sotto"}]""", orderIndex = 1)
        open(callout, sotto)

        viewModel.updateBlockType(viewModel.blocks.value.first { it.id == callout.id }, BlockType.BULLET_LIST_ITEM)
        waitFor("le tre righe") { stored().size == 4 }

        val blocks = stored()
        assertEquals(listOf("Uno", "Due", "Tre", "Sotto"), blocks.map { viewModel.plainTextOf(it) })
        assertEquals(callout.id, blocks[0].id)
        assertEquals(List(3) { BlockType.BULLET_LIST_ITEM } + BlockType.PARAGRAPH, blocks.map { it.type })
        // La formattazione resta con la sua riga.
        assertEquals(listOf(RichTextSpan(text = "Due", bold = true)), viewModel.spansOf(blocks[1]))
    }

    @Test
    fun backspaceAtTheStartOfAQuoteMakesItText() {
        val quote = BlockEntity(pageId = page.id, type = BlockType.QUOTE, textJson = """[{"text":"Prima\nSeconda"}]""", orderIndex = 0)
        open(quote)

        assertTrue(viewModel.backspaceAtIslandStart(viewModel.blocks.value.first()))
        waitFor("le due righe") { stored().size == 2 }
        assertEquals(listOf("Prima", "Seconda"), stored().map { viewModel.plainTextOf(it) })
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.PARAGRAPH), stored().map { it.type })
    }

    @Test
    fun turningTextIntoACalloutKeepsTheTextAndStartsWithTheBulb() {
        val riga = BlockEntity(pageId = page.id, textJson = """[{"text":"Attenzione"}]""", orderIndex = 0)
        open(riga)

        viewModel.updateBlockType(viewModel.blocks.value.first(), BlockType.CALLOUT)
        waitFor("il riquadro") { stored().first().type == BlockType.CALLOUT }
        val callout = stored().first()
        assertEquals("Attenzione", viewModel.plainTextOf(callout))
        // Nessuna voce in più: l'icona di riserva è la lampadina, lo sfondo il grigio.
        assertEquals(BlockAttrs(), callout.attrs)
    }

    @Test
    fun theIconAndTheColorOfACalloutDoNotEraseEachOther() {
        val callout = BlockEntity(pageId = page.id, type = BlockType.CALLOUT, textJson = """[{"text":"Nota"}]""", orderIndex = 0)
        open(callout)

        // Due ritocchi di fila: ognuno legge le voci dal database, non dalla copia di prima.
        viewModel.updateBlockAttrs(callout.id) { it.copy(icon = "🔥") }
        waitFor("l'icona sulla schermata") { viewModel.blocks.value.first().attrs.icon == "🔥" }
        viewModel.updateBlockAttrs(callout.id) { it.copy(color = "#1F3A5F") }
        waitFor("icona e colore") { stored().first().attrs == BlockAttrs(icon = "🔥", color = "#1F3A5F") }

        viewModel.undo()
        waitFor("il colore annullato") { stored().first().attrs == BlockAttrs(icon = "🔥") }
    }

    @Test
    fun enterOnTheEmptyLastLineLeavesTheCallout() {
        val callout = BlockEntity(pageId = page.id, type = BlockType.CALLOUT, textJson = """[{"text":"Riga\n"}]""", orderIndex = 0)
        open(callout)

        viewModel.leaveMultilineBlock(viewModel.blocks.value.first(), listOf(RichTextSpan(text = "Riga")))
        waitFor("la riga sotto") { stored().size == 2 }
        assertEquals(listOf("Riga", ""), stored().map { viewModel.plainTextOf(it) })
        assertEquals(listOf(BlockType.CALLOUT, BlockType.PARAGRAPH), stored().map { it.type })
    }
}
