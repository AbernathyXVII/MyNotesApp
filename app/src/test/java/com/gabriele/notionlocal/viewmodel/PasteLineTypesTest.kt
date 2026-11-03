package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PasteFormat
import com.gabriele.notionlocal.data.entity.PastedLine
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
 * **Le caselle copiate tornano caselle** (19/10/2026, chiesto dall'utente:
 * "le checkbox non vengono incollate e rimane solo testo normale"): la copia
 * dice il tipo di ogni riga presa intera, e l'incolla lo rimette.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PasteLineTypesTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Spesa")
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
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

    private fun line(text: String, at: Int, type: BlockType = BlockType.PARAGRAPH, checked: Boolean = false, indent: Int = 0) =
        BlockEntity(pageId = page.id, type = type, isChecked = checked, indentLevel = indent, textJson = """[{"text":"$text"}]""", orderIndex = at)

    private fun saved() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }

    private fun text(block: BlockEntity) = json.decodeFromString<List<RichTextSpan>>(block.textJson).joinToString("") { it.text }

    /** Come si vede una riga: testo, tipo, spuntata, rientro. */
    private fun shape(block: BlockEntity) = "${text(block)}|${block.type}|${block.isChecked}|${block.indentLevel}"

    @Test
    fun theSelectionSaysTheTypeOfWholeLines() {
        val milk = line("Latte", 0, BlockType.CHECKBOX, checked = true)
        val bread = line("Pane", 1, BlockType.CHECKBOX, indent = 1)
        val eggs = line("Uova", 2, BlockType.BULLET_LIST_ITEM)
        open(milk, bread, eggs)
        val ids = listOf(milk.id, bread.id, eggs.id)
        // Tutte e tre intere: "Latte\nPane\nUova".
        viewModel.reportSelection(ids, 0, 15)
        assertEquals(
            listOf(
                CopiedLine(BlockType.CHECKBOX, checked = true),
                CopiedLine(BlockType.CHECKBOX, indent = 1),
                CopiedLine(BlockType.BULLET_LIST_ITEM)
            ),
            viewModel.selectedLines()
        )
        // Da metà di "Latte" a metà di "Uova": solo "Pane" è intera.
        viewModel.reportSelection(ids, 2, 13)
        assertEquals(listOf(null, CopiedLine(BlockType.CHECKBOX, indent = 1), null), viewModel.selectedLines())
        // Senza testo selezionato, niente.
        viewModel.reportSelection(ids, 3, 3)
        assertEquals(null, viewModel.selectedLines())
    }

    @Test
    fun pastedIntoAnEmptyLineTheyAreCheckboxesAgain() {
        val empty = line("", 0)
        val after = line("Dopo", 1)
        open(empty, after)
        // "Latte\nPane" incollato nella riga vuota: due caselle, la seconda rientrata.
        viewModel.updateRun(
            listOf(empty.id, after.id),
            listOf("Latte", "Pane", "Dopo"),
            listOf("", "Dopo"),
            caretLine = 1,
            paste = PasteFormat(
                lines = listOf(
                    PastedLine(0, 5, BlockType.CHECKBOX, checked = true),
                    PastedLine(6, 10, BlockType.CHECKBOX, indent = 1)
                )
            )
        )
        waitFor("le righe incollate") { saved().size == 3 }
        assertEquals(
            listOf("Latte|CHECKBOX|true|0", "Pane|CHECKBOX|false|1", "Dopo|PARAGRAPH|false|0"),
            saved().map { shape(it) }
        )
        // La riga vuota in cui si è incollato è diventata la prima casella.
        assertEquals(empty.id, saved().first().id)
    }

    @Test
    fun pastedAfterTextTheFirstLineStaysText() {
        val hello = line("Ciao ", 0)
        open(hello)
        // Incollato dopo "Ciao ": la prima riga si unisce al testo, la seconda è una casella.
        viewModel.updateRun(
            listOf(hello.id),
            listOf("Ciao Latte", "Pane"),
            listOf("Ciao "),
            caretLine = 1,
            paste = PasteFormat(
                lines = listOf(
                    PastedLine(5, 10, BlockType.CHECKBOX, checked = true),
                    PastedLine(11, 15, BlockType.NUMBERED_LIST_ITEM, indent = 3)
                )
            )
        )
        waitFor("la riga nuova") { saved().size == 2 }
        // Il rientro si conta da quello della riga in cui si incolla: il più piccolo copiato vale zero.
        assertEquals(listOf("Ciao Latte|PARAGRAPH|false|0", "Pane|NUMBERED_LIST_ITEM|false|0"), saved().map { shape(it) })
    }

    @Test
    fun withoutTypesTheNewLinesFollowTheLineAbove() {
        val box = line("Comprare", 0, BlockType.CHECKBOX)
        open(box)
        // Testo da un'altra app, senza tipi: come prima, le righe nuove sono caselle come quella sopra.
        viewModel.updateRun(listOf(box.id), listOf("Comprare latte", "pane"), listOf("Comprare"), caretLine = 1)
        waitFor("la riga nuova") { saved().size == 2 }
        assertEquals(listOf("Comprare latte|CHECKBOX|false|0", "pane|CHECKBOX|false|0"), saved().map { shape(it) })
    }

    @Test
    fun insideAToggleTheNewLinesTakeTheirType() {
        val toggle = BlockEntity(pageId = page.id, type = BlockType.TOGGLE, textJson = """[{"text":"Lista"}]""", orderIndex = 0)
        val child = BlockEntity(pageId = page.id, parentBlockId = toggle.id, textJson = """[{"text":""}]""", orderIndex = 0)
        open(toggle, child)
        viewModel.splitBlockAtMultiple(
            child,
            listOf("Latte", "Pane", "Uova"),
            lineKinds = listOf(
                PastedLine(0, 5, BlockType.CHECKBOX, checked = true),
                PastedLine(6, 10, BlockType.CHECKBOX),
                null
            )
        )
        waitFor("le righe nel toggle") { saved().count { it.parentBlockId == toggle.id } == 3 }
        val inside = saved().filter { it.parentBlockId == toggle.id }
        // Anche la riga vuota in cui si è incollato prende il suo tipo; l'ultima, presa in parte, quello di sempre.
        assertEquals(listOf("Latte|CHECKBOX|true|0", "Pane|CHECKBOX|false|0", "Uova|PARAGRAPH|false|0"), inside.map { shape(it) })
        assertEquals(child.id, inside.first().id)
    }
}
