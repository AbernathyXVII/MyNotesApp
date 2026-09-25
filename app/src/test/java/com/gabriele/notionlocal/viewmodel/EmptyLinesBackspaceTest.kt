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
 * **Il backspace fra righe vuote toglie la riga giusta.** Tutte vuote,
 * le righe sono indistinguibili per il testo, ma non per il campo che le
 * mostra: il testo condiviso è legato al suo primo blocco, e togliendo
 * quello la tastiera spariva (trovato dall'utente il 25/09/2026).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EmptyLinesBackspaceTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel

    private val page = PageEntity(title = "Righe vuote")
    private val lines = (0 until 4).map { i ->
        BlockEntity(
            pageId = page.id,
            type = BlockType.PARAGRAPH,
            textJson = """[{"text":""}]""",
            orderIndex = i
        )
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
        waitFor("quattro righe") { viewModel.blocks.value.size == 4 }
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

    private fun ids() = viewModel.blocks.value.sortedBy { it.orderIndex }.map { it.id }

    @Test
    fun backspaceOnTheSecondEmptyLineRemovesTheSecond() {
        // Backspace a inizio della seconda riga: il cursore finisce in
        // fondo alla prima, e le righe da quattro diventano tre.
        viewModel.updateRun(ids(), listOf("", "", ""), listOf("", "", "", ""), caretLine = 0)
        waitFor("tre righe") { viewModel.blocks.value.size == 3 }
        assertEquals(listOf(lines[0].id, lines[2].id, lines[3].id), ids())
    }

    @Test
    fun backspaceOnTheThirdEmptyLineRemovesTheThird() {
        viewModel.updateRun(ids(), listOf("", "", ""), listOf("", "", "", ""), caretLine = 1)
        waitFor("tre righe") { viewModel.blocks.value.size == 3 }
        assertEquals(listOf(lines[0].id, lines[1].id, lines[3].id), ids())
    }

    @Test
    fun enterAtTheStartOfALineKeepsThatLineItsOwnBlock() {
        // L'Invio a inizio riga non deve cambiare chi è la riga che scende.
        viewModel.updateRun(ids(), listOf("", "", "", "", ""), listOf("", "", "", ""), caretLine = 2)
        waitFor("cinque righe") { viewModel.blocks.value.size == 5 }
        assertEquals(lines.map { it.id }, ids().filter { id -> lines.any { it.id == id } })
    }
}
