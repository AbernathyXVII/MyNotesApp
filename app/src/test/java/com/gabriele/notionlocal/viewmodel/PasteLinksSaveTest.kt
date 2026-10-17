package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.PageEntity
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
 * **I link incollati finiscono nel database** (17/10/2026): nel testo
 * condiviso dei paragrafi, anche quando l'incolla fa più righe, e nelle
 * righe nuove di una casella.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PasteLinksSaveTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Home")
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(
            listOf(
                BlockEntity(pageId = page.id, textJson = """[{"text":"Prima "}]""", orderIndex = 0),
                BlockEntity(pageId = page.id, textJson = """[{"text":"Dopo"}]""", orderIndex = 1),
                BlockEntity(pageId = page.id, type = BlockType.CHECKBOX, textJson = """[{"text":"Comprare "}]""", orderIndex = 2)
            )
        )
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("tre righe") { viewModel.blocks.value.size == 3 }
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

    private fun saved() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }

    private fun spans(block: BlockEntity) = json.decodeFromString<List<RichTextSpan>>(block.textJson)

    /** Le parole coi link di un blocco, e dove portano. */
    private fun links(block: BlockEntity) = spans(block).filter { it.link != null }.map { it.text to it.link }

    @Test
    fun aPasteOfTwoLinesInTheSharedText() {
        // Il testo condiviso ha tutte e tre le righe: anche la casella ci sta dentro.
        val run = viewModel.blocks.value.sortedBy { it.orderIndex }
        // Incollato dopo "Prima ": "vedi Google\ne Bing qui" — due righe, due link.
        val before = listOf("Prima ", "Dopo", "Comprare ")
        val after = listOf("Prima vedi Google", "e Bing qui", "Dopo", "Comprare ")
        viewModel.updateRun(
            run.map { it.id },
            after,
            before,
            caretLine = 1,
            links = listOf(LinkRange(11, 17, "https://www.google.com/"), LinkRange(20, 24, "https://www.bing.com/"))
        )
        waitFor("la riga nuova") { saved().size == 4 }
        val blocks = saved()
        assertEquals(listOf("Prima vedi Google", "e Bing qui", "Dopo", "Comprare "), blocks.map { spans(it).joinToString("") { s -> s.text } })
        assertEquals(listOf("Google" to "https://www.google.com/"), links(blocks[0]))
        assertEquals(listOf("Bing" to "https://www.bing.com/"), links(blocks[1]))
        assertEquals(emptyList<Pair<String, String?>>(), links(blocks[2]))
    }

    @Test
    fun aPasteOfThreeLinesInACheckbox() {
        val box = viewModel.blocks.value.single { it.type == BlockType.CHECKBOX }
        val lines = listOf("Comprare latte", "pane da Esselunga", "uova")
        val lineSpans = listOf(
            listOf(RichTextSpan(text = "Comprare latte")),
            listOf(RichTextSpan(text = "pane da "), RichTextSpan(text = "Esselunga", link = "https://www.esselunga.it/")),
            listOf(RichTextSpan(text = "uova"))
        )
        viewModel.splitBlockAtMultiple(box, lines, lineSpans)
        waitFor("tre caselle") { saved().count { it.type == BlockType.CHECKBOX } == 3 }
        val boxes = saved().filter { it.type == BlockType.CHECKBOX }
        assertEquals(lines, boxes.map { spans(it).joinToString("") { s -> s.text } })
        assertEquals(listOf("Esselunga" to "https://www.esselunga.it/"), links(boxes[1]))
    }
}
