package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
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
 * **La riga vuota di una pagina senza righe sopravvive al primo Invio.**
 * Si mostra senza essere nel database; l'Invio salvava solo la riga
 * nuova, e quella di prima — col campo di testo e il cursore — spariva
 * (trovato sul telefono il 25/09/2026).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EmptyPageTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Vuota")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("la riga vuota") { viewModel.blocks.value.size == 1 }
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
    fun enterOnTheEmptyLineKeepsIt() = runBlocking<Unit> {
        val first = viewModel.blocks.value.single().id
        // Nel database ancora niente: la riga è solo sullo schermo.
        assertEquals(0, db.blockDao().getBlocksForPageOnce(page.id).size)

        viewModel.updateRun(listOf(first), listOf("", ""), listOf(""), caretLine = 1)
        waitFor("due righe") { viewModel.blocks.value.size == 2 }

        val saved = db.blockDao().getBlocksForPageOnce(page.id).sortedBy { it.orderIndex }
        assertEquals(2, saved.size)
        assertEquals(first, saved.first().id)
    }

    @Test
    fun typingOnTheEmptyLineSavesThatLine() {
        val first = viewModel.blocks.value.single().id
        viewModel.updateRun(listOf(first), listOf("Ciao"), listOf(""), caretLine = 0)
        waitFor("la riga scritta") { viewModel.blocks.value.singleOrNull()?.textJson?.contains("Ciao") == true }
        assertEquals(first, viewModel.blocks.value.single().id)
    }

    @Test
    fun theEmptyLineTurnsIntoAToDoFromThePlusMenu() {
        // "To-do list" dal "+" prima di scrivere niente: la riga non era nel
        // database, il cambio di tipo non la trovava e si perdeva (trovato
        // dall'utente il 26/09/2026, in un template nuovo).
        val first = viewModel.blocks.value.single()
        viewModel.updateBlockType(first, BlockType.CHECKBOX)
        waitFor("la casella") { viewModel.blocks.value.singleOrNull()?.type == BlockType.CHECKBOX }

        viewModel.updateRun(listOf(first.id), listOf("ciao"), listOf(""), caretLine = 0)
        waitFor("la casella scritta") { viewModel.blocks.value.singleOrNull()?.textJson?.contains("ciao") == true }
        val saved = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.single()
        assertEquals(first.id, saved.id)
        assertEquals(BlockType.CHECKBOX, saved.type)
    }
}
