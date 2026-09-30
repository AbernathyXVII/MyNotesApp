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
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * **Un tocco nel vuoto di una pagina vuota** (30/09/2026): "in un campo
 * vuoto o nuovo, se premo in un punto a caso della pagina il text cursor mi
 * vada al primissimo blocco". Sulle righe vuote in fondo alla pagina il
 * cursore va sulla prima di loro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BlankTapTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova")

    private fun line(text: String, at: Int, type: BlockType = BlockType.PARAGRAPH) =
        BlockEntity(pageId = page.id, type = type, textJson = """[{"text":"$text"}]""", orderIndex = at)

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
            // Il cursore arriva dopo `IME_SETTLE_DELAY_MS`: il tempo del test va fatto andare avanti.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    @Test
    fun onAnEmptyPageATapBelowGoesToTheFirstLine() {
        val lines = (0 until 5).map { line("", it) }
        open(*lines.toTypedArray())
        assertEquals(lines.map { it.id }, viewModel.trailingBlankLines().map { it.id })
        viewModel.focusLastBlock()
        waitFor("il cursore sulla prima riga") { viewModel.focusRequestBlockId.value == lines[0].id }
    }

    @Test
    fun onAnEmptyPageATapOnALowerLineGoesToTheFirst() {
        val lines = (0 until 5).map { line("", it) }
        open(*lines.toTypedArray())
        val ids = lines.map { it.id }
        assertEquals(0, viewModel.trailingBlankRedirect(ids, List(5) { "" }, 3))
        // Già sulla prima: resta.
        assertNull(viewModel.trailingBlankRedirect(ids, List(5) { "" }, 0))
    }

    @Test
    fun afterTheTextItIsTheFirstEmptyLineBelowIt() {
        val lines = listOf(line("Testo", 0), line("", 1), line("", 2), line("", 3))
        open(*lines.toTypedArray())
        val ids = lines.map { it.id }
        assertEquals(1, viewModel.trailingBlankRedirect(ids, listOf("Testo", "", "", ""), 3))
        viewModel.focusLastBlock()
        waitFor("il cursore sotto il testo") { viewModel.focusRequestBlockId.value == lines[1].id }
        // Appena scritto nella prima riga vuota (il database non lo sa ancora): si va a quella dopo.
        assertEquals(2, viewModel.trailingBlankRedirect(ids, listOf("Testo", "Ciao", "", ""), 3))
    }

    @Test
    fun anEmptyLineBetweenTheTextStaysTappable() {
        val lines = listOf(line("A", 0), line("", 1), line("B", 2), line("", 3))
        open(*lines.toTypedArray())
        val ids = lines.map { it.id }
        assertNull(viewModel.trailingBlankRedirect(ids, listOf("A", "", "B", ""), 1))
        assertNull(viewModel.trailingBlankRedirect(ids, listOf("A", "", "B", ""), 2))
        // Una sola riga vuota in fondo: niente da spostare.
        assertNull(viewModel.trailingBlankRedirect(ids, listOf("A", "", "B", ""), 3))
    }

    @Test
    fun anEmptyBulletIsNotEmptySpace() {
        val lines = listOf(line("", 0, BlockType.BULLET_LIST_ITEM), line("", 1), line("", 2))
        open(*lines.toTypedArray())
        assertEquals(listOf(lines[1].id, lines[2].id), viewModel.trailingBlankLines().map { it.id })
    }
}
