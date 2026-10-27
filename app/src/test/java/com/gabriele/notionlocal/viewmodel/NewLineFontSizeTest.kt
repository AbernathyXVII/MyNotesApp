package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.applyTextEdit
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.data.entity.splitLines
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Andando a capo il corpo torna normale** (27/10/2026, chiesto
 * dall'utente: "se dovessi modificare il font size di un blocco, per esempio
 * un titolo a 50, e poi vado a capo per scrivere altro, fai sì che il font
 * size venga resettato a 16 per tutti i nuovi blocchi").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NewLineFontSizeTest {

    private val title = listOf(RichTextSpan(text = "Titolo", bold = true, size = 50))

    @Test
    fun whatIsWrittenAfterEnterStartsAtTheNormalSize() {
        val lines = applyTextEdit(title, "Titolo", "Titolo\nAltro\nAncora").splitLines()
        assertEquals(listOf("Titolo", "Altro", "Ancora"), lines.map { it.plainText() })
        assertEquals(50, lines[0].single().size)
        assertNull(lines[1].single().size)
        assertNull(lines[2].single().size)
        // Il corpo sì, il resto della formattazione continua come prima.
        assertTrue(lines[1].single().bold)
    }

    @Test
    fun enterInTheMiddleOfTheTitleKeepsItsSize() {
        // La metà che scende è testo che c'era già.
        val lines = applyTextEdit(title, "Titolo", "Tit\nolo").splitLines()
        assertEquals(50, lines[1].single().size)
    }

    @Test
    fun writingOnTheSameLineKeepsTheSize() {
        assertEquals(50, applyTextEdit(title, "Titolo", "Titolone").single().size)
    }

    // --- Lo stesso nella pagina: Invio e lettere arrivati insieme ---

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova del corpo")
    private val heading = BlockEntity(pageId = page.id, textJson = """[{"text":"Titolo","size":50}]""", orderIndex = 0)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(heading))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("il titolo") { viewModel.blocks.value.isNotEmpty() }
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

    @Test
    fun aNewLineUnderABigTitleIsWrittenAtTheNormalSize() {
        // Scrivendo subito dopo l'Invio la riga nuova non è ancora nel database:
        // il campo manda insieme l'a-capo e le lettere.
        viewModel.updateRun(listOf(heading.id), listOf("Titolo", "Altro"), listOf("Titolo"), caretLine = 1)
        waitFor("la riga nuova") { viewModel.blocks.value.size == 2 }
        val (first, second) = viewModel.blocks.value.sortedBy { it.orderIndex }
        assertEquals(50, viewModel.spansOf(first).single().size)
        assertEquals("Altro", viewModel.plainTextOf(second))
        assertNull(viewModel.spansOf(second).single().size)
    }

    @Test
    fun enterThenWritingInTheEmptyLine() {
        viewModel.updateRun(listOf(heading.id), listOf("Titolo", ""), listOf("Titolo"), caretLine = 1)
        waitFor("la riga vuota") { viewModel.blocks.value.size == 2 }
        val newId = viewModel.blocks.value.sortedBy { it.orderIndex }[1].id
        println("PROVA vuota: " + viewModel.blocks.value.sortedBy { it.orderIndex }.map { it.textJson })
        viewModel.updateRun(listOf(heading.id, newId), listOf("Titolo", "A"), listOf("Titolo", ""), caretLine = 1)
        waitFor("la lettera") { viewModel.blocks.value.any { viewModel.plainTextOf(it) == "A" } }
        println("PROVA dopo: " + viewModel.blocks.value.sortedBy { it.orderIndex }.map { it.textJson })
        val second = viewModel.blocks.value.single { viewModel.plainTextOf(it) == "A" }
        assertNull(viewModel.spansOf(second).single().size)
    }
}
