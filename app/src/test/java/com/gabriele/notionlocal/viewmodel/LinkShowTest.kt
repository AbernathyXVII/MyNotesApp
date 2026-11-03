package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * **Quello che si vede di un link nel testo** (27/10/2026, chiesto
 * dall'utente: "il link per intero", "il titolo della pagina web" o "una
 * scritta che io scriverò manualmente"). La finestrella dà il testo nuovo;
 * qui si controlla che vada al link intero — anche toccandone una parola
 * sola, e anche quando il sito era scritto come testo semplice — con la sua
 * formattazione, e che il resto della riga non cambi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LinkShowTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val natures = "https://pokemondb.net/mechanics/natures"
    private val page = PageEntity(title = "Prova")
    private val linked = BlockEntity(
        pageId = page.id, type = BlockType.PARAGRAPH, orderIndex = 0,
        textJson = """[{"text":"Vedi "},{"text":"$natures","bold":true,"link":"$natures"},{"text":" ora"}]"""
    )
    private val bare = BlockEntity(
        pageId = page.id, type = BlockType.PARAGRAPH, orderIndex = 1,
        textJson = """[{"text":"Anche (www.pokemondb.net/natures), qui"}]"""
    )
    private val run = listOf(linked.id, bare.id)
    /** Dove comincia la seconda riga nel testo del campo: la prima e il suo a-capo. */
    private val secondLine = "Vedi $natures ora".length + 1

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(linked, bare))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 2 }
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

    private fun saved(block: BlockEntity) =
        viewModel.spansOf(runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.single { it.id == block.id })

    @Test
    fun aWordOfTheLinkTakesTheWholeLinkAndTheTitleReplacesIt() {
        // Tenendo premuto si seleziona una parola sola, "pokemondb".
        viewModel.reportSelection(run, 13, 22)
        viewModel.captureSelection()
        assertEquals(natures, viewModel.capturedLink())
        assertEquals(natures, viewModel.capturedLinkText())

        val changes = mutableListOf<PageEditorViewModel.LinkTextChange>()
        val listening = CoroutineScope(Dispatchers.Main.immediate).launch { viewModel.linkTextChanges.collect { changes += it } }
        assertTrue(viewModel.applyLinkToCapturedSelection(natures, "Natures | Pokémon Database"))
        waitFor("il titolo al posto del link") {
            saved(linked) == listOf(
                RichTextSpan(text = "Vedi "),
                RichTextSpan(text = "Natures | Pokémon Database", bold = true, link = natures),
                RichTextSpan(text = " ora")
            )
        }
        // Il campo lo sa subito, col cursore in fondo al link.
        assertEquals(5 + "Natures | Pokémon Database".length, changes.single().caret)
        listening.cancel()
        // La riga sotto non cambia.
        assertEquals(listOf(RichTextSpan(text = "Anche (www.pokemondb.net/natures), qui")), saved(bare))

        // E si torna al link per intero, quando la pagina ha il testo nuovo.
        waitFor("la pagina col titolo") {
            viewModel.blocks.value.any { block -> block.id == linked.id && viewModel.spansOf(block).any { it.link == natures && it.text != natures } }
        }
        viewModel.reportSelection(run, 10, 10)
        viewModel.captureSelection()
        assertEquals("Natures | Pokémon Database", viewModel.capturedLinkText())
        assertTrue(viewModel.applyLinkToCapturedSelection(natures, natures))
        waitFor("il link per intero") {
            saved(linked) == listOf(
                RichTextSpan(text = "Vedi "),
                RichTextSpan(text = natures, bold = true, link = natures),
                RichTextSpan(text = " ora")
            )
        }
    }

    @Test
    fun aSiteWrittenAsTextBecomesALinkWithYourText() {
        // Il solo cursore dentro "www.pokemondb.net/natures": l'icona del link c'è.
        viewModel.reportSelection(run, secondLine + 12, secondLine + 12)
        assertFalse(viewModel.selectionLink.value.hasText)
        assertNull(viewModel.selectionLink.value.link)
        assertTrue(viewModel.selectionLink.value.onSite)
        viewModel.captureSelection()
        assertNull(viewModel.capturedLink())
        // Senza le parentesi e la virgola intorno.
        assertEquals("www.pokemondb.net/natures", viewModel.capturedLinkText())

        val url = "https://www.pokemondb.net/natures"
        assertTrue(viewModel.applyLinkToCapturedSelection(url, "le nature"))
        waitFor("la scritta col link") {
            saved(bare) == listOf(
                RichTextSpan(text = "Anche ("),
                RichTextSpan(text = "le nature", link = url),
                RichTextSpan(text = "), qui")
            )
        }
    }

    @Test
    fun keepingTheTextOnlyAddsTheLink() {
        viewModel.reportSelection(run, secondLine + 7, secondLine + 7)
        viewModel.captureSelection()
        val url = "https://www.pokemondb.net/natures"
        assertTrue(viewModel.applyLinkToCapturedSelection(url, null))
        waitFor("il link sul sito scritto") {
            saved(bare) == listOf(
                RichTextSpan(text = "Anche ("),
                RichTextSpan(text = "www.pokemondb.net/natures", link = url),
                RichTextSpan(text = "), qui")
            )
        }
    }

    @Test
    fun aSelectionOverTwoLinesChangesOnlyTheAddress() {
        viewModel.reportSelection(run, 2, secondLine + 4)
        viewModel.captureSelection()
        assertNull(viewModel.capturedLinkText())
    }

    @Test
    fun plainWordsAreNotASite() {
        // "Vedi": niente icona col solo cursore.
        viewModel.reportSelection(run, 2, 2)
        assertFalse(viewModel.selectionLink.value.onSite)
        assertNull(viewModel.selectionLink.value.link)
    }
}
