package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Looper
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import com.gabriele.notionlocal.ui.screen.PasteMode
import com.gabriele.notionlocal.ui.screen.pastedFormat
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
 * **"Keep Source Formatting" da OneNote** (20/10/2026): dagli appunti con
 * l'HTML arrivano le righe col loro tipo, la grandezza in punti di notE e la
 * tabella, che nella pagina diventa una tabella semplice con le sue celle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PasteSourceStructureTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Appunti")
    private val json = Json { ignoreUnknownKeys = true }
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    private val html = """
        <body style='font-family:Calibri;font-size:11.0pt'><!--StartFragment-->
        <p style='font-size:20.0pt'>Titolo</p>
        <ul><li><span style='font-size:11.0pt'>Primo</span></li><li><span style='font-size:11.0pt'>Secondo</span></li></ul>
        <table><tr><td>A</td><td>B</td></tr><tr><td>C</td><td>D</td></tr></table>
        <!--EndFragment--></body>
    """.trimIndent()

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val pages = PageRepository(db)
        pages.createPage(page)
        viewModel = PageEditorViewModel(pages)
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

    /** "Keep Source Formatting" su quello che c'è negli appunti. */
    private fun pasteKeepingSource(): AnnotatedString {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newHtmlText("OneNote", "Titolo\n• Primo\n• Secondo\nA\tB\nC\tD", html))
        val base = object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText(): AnnotatedString? = null
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        val clipboard = LinkClipboard(context, base)
        clipboard.nextPasteMode = PasteMode.KEEP_SOURCE
        return clipboard.getText()!!
    }

    @Test
    fun theClipboardGivesLinesSizesAndTheTable() {
        val pasted = pasteKeepingSource()
        // Il testo è quello delle righe, senza pallini scritti; la tabella ha un segnaposto.
        assertEquals("Titolo\nPrimo\nSecondo\nA | B", pasted.text)
        val format = TextFieldValue(pasted).pastedFormat(0)
        assertEquals(
            listOf(BlockType.PARAGRAPH, BlockType.BULLET_LIST_ITEM, BlockType.BULLET_LIST_ITEM, BlockType.TABLE),
            format.lines.map { it.type }
        )
        assertEquals(listOf(listOf("A", "B"), listOf("C", "D")), format.lines.last().table)
        // Il titolo a 20 punti col testo a 11: in notE 29.
        assertEquals(listOf(29), format.styles.mapNotNull { it.size })
    }

    @Test
    fun pastedIntoAnEmptyLineTheTableIsATable() {
        val empty = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":""}]""", orderIndex = 0)
        runBlocking { db.blockDao().insertAll(listOf(empty)) }
        viewModel.load(page.id)
        waitFor("la riga") { viewModel.blocks.value.size == 1 }

        val pasted = pasteKeepingSource()
        val lines = pasted.text.split('\n')
        viewModel.updateRun(listOf(empty.id), lines, listOf(""), caretLine = lines.lastIndex, paste = TextFieldValue(pasted).pastedFormat(0))
        waitFor("le righe incollate") { viewModel.blocks.value.size == 4 }

        val saved = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.BULLET_LIST_ITEM, BlockType.BULLET_LIST_ITEM, BlockType.TABLE), saved.map { it.type })
        assertEquals(listOf(RichTextSpan(text = "Titolo", size = 29)), json.decodeFromString<List<RichTextSpan>>(saved[0].textJson))
        val table = saved.last()
        assertEquals(2 to 2, table.tableRows to table.tableCols)
        waitFor("le celle") {
            runBlocking { db.tableCellDao().getCellsForBlockOnce(table.id) }.associate { (it.rowIndex to it.colIndex) to it.text } ==
                mapOf((0 to 0) to "A", (0 to 1) to "B", (1 to 0) to "C", (1 to 1) to "D")
        }
        // La tabella non tiene il segnaposto come testo.
        assertEquals("", json.decodeFromString<List<RichTextSpan>>(table.textJson).joinToString("") { it.text })
    }
}
