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
 * **Invio premuto in fretta**: ogni Invio è una riga, anche quando arriva
 * prima che il database abbia scritto quello di prima. Tre Invio di fila in
 * cima alla pagina principale ne facevano quattro, e da lì le righe coi
 * font e i pedici si vedevano senza (trovato dall'utente il 26/09/2026).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FastEnterTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Home")

    private val styled = listOf(
        """[{"text":"Titolo uno H1"}]""",
        """[{"text":"Paragrafo","font":"FUTURA"},{"text":" di prova"}]""",
        """[{"text":"acqua H"},{"text":"2","subscript":true},{"text":"O"}]"""
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        val empties = (0 until 2).map { BlockEntity(pageId = page.id, textJson = """[{"text":""}]""", orderIndex = it) }
        val texts = styled.mapIndexed { i, json -> BlockEntity(pageId = page.id, textJson = json, orderIndex = 10 + i) }
        db.blockDao().insertAll(empties + texts)
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("cinque righe") { viewModel.blocks.value.size == 5 }
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

    @Test
    fun threeQuickEntersOnTheFirstLineMakeThreeLines() {
        // Gli id che il campo ha in mano: quelli di prima, per tutti e tre.
        val ids = viewModel.blocks.value.sortedBy { it.orderIndex }.map { it.id }
        val texts = listOf("", "", "Titolo uno H1", "Paragrafo di prova", "acqua H2O")
        var before = texts
        repeat(3) { n ->
            val after = listOf("") + before
            // Nessuna attesa fra un Invio e l'altro: il database è indietro.
            viewModel.updateRun(ids, after, before, caretLine = n + 1)
            before = after
        }
        waitFor("otto righe") { saved().size == 8 }
        // Un attimo in più: nessuna riga di troppo arriva dopo.
        repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        val blocks = saved()
        assertEquals(8, blocks.size)
        assertEquals(styled, blocks.takeLast(3).map { it.textJson })
    }

    @Test
    fun aTypeChangedRightAfterTheTextStays() {
        // Il menu "/": prima si salva il testo, subito dopo si cambia il tipo.
        val ordered = viewModel.blocks.value.sortedBy { it.orderIndex }
        val first = ordered.first()
        viewModel.updateRun(ordered.map { it.id }, listOf("ciao", "", "Titolo uno H1", "Paragrafo di prova", "acqua H2O"),
            listOf("", "", "Titolo uno H1", "Paragrafo di prova", "acqua H2O"), caretLine = 0)
        viewModel.updateBlockType(first, BlockType.CHECKBOX)
        waitFor("la casella scritta") {
            saved().first().let { it.type == BlockType.CHECKBOX && it.textJson.contains("ciao") }
        }
        repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertEquals(BlockType.CHECKBOX, saved().first().type)
    }
}
