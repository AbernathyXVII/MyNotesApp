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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
 * **Il corpo del titolo di un toggle** (30/09/2026): "Non mi modifica la
 * grandezza del font di un titolo del toggle". Il corpo andava nel database
 * ma il campo del titolo, che ha il fuoco, non lo sapeva: ora il ViewModel
 * glielo dice (`externalSpans`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ToggleTitleSizeTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova")
    private val toggle = BlockEntity(pageId = page.id, type = BlockType.TOGGLE, textJson = """[{"text":"Prova"}]""", orderIndex = 0)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(toggle))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("il toggle") { viewModel.blocks.value.size == 1 }
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

    private fun saved() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.single()

    @Test
    fun theTitleHearsTheNewSizeRightAway() {
        val heard = mutableListOf<Pair<String, List<RichTextSpan>>>()
        @Suppress("OPT_IN_USAGE")
        val listening = GlobalScope.launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            viewModel.externalSpans.collect { heard += it }
        }
        // "Prova" selezionata nel titolo, poi il 30 dalla finestrella.
        viewModel.reportSelection(listOf(toggle.id), 0, 5)
        viewModel.captureSelection()
        assertTrue(viewModel.applySizeToCapturedSelection(30))
        assertEquals(listOf(toggle.id to listOf(RichTextSpan(text = "Prova", size = 30))), heard)
        waitFor("salvato") { viewModel.spansOf(saved()) == listOf(RichTextSpan(text = "Prova", size = 30)) }
        listening.cancel()
    }

    @Test
    fun theColourIsHeardToo() {
        val heard = mutableListOf<Pair<String, List<RichTextSpan>>>()
        @Suppress("OPT_IN_USAGE")
        val listening = GlobalScope.launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            viewModel.externalSpans.collect { heard += it }
        }
        viewModel.applyColorToRun(listOf(toggle.id), 0, 3, background = false, hex = "#E03E3E")
        assertEquals(listOf(RichTextSpan(text = "Pro", color = "#E03E3E"), RichTextSpan(text = "va")), heard.single().second)
        listening.cancel()
    }
}
