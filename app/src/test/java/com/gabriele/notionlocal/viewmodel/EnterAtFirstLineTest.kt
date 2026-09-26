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
 * **L'Invio a inizio della prima riga di un gruppo** mette la riga vuota
 * subito sopra di lei, non in cima alla pagina: sopra il gruppo può
 * esserci un'isola (qui un link a una pagina), e la riga ci saltava sopra.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EnterAtFirstLineTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel

    private val page = PageEntity(title = "Invio in cima")
    private val link = BlockEntity(pageId = page.id, type = BlockType.PAGE_LINK, textJson = """[{"text":""}]""", orderIndex = 0)
    private val prova = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"prova"}]""", orderIndex = 1)
    private val x = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"x"}]""", orderIndex = 2)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(link, prova, x))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("tre blocchi") { viewModel.blocks.value.size == 3 }
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

    private fun ordered() = viewModel.blocks.value.sortedBy { it.orderIndex }

    @Test
    fun theEmptyLineGoesRightAboveTheFirstLineNotAboveTheLink() {
        viewModel.updateRun(listOf(prova.id, x.id), listOf("", "prova", "x"), listOf("prova", "x"), caretLine = 1)
        waitFor("quattro blocchi") { viewModel.blocks.value.size == 4 }
        val blocks = ordered()
        assertEquals(link.id, blocks[0].id)
        assertEquals("", viewModel.plainTextOf(blocks[1]))
        // Le due righe restano loro, solo un posto più giù.
        assertEquals(listOf(prova.id, x.id), blocks.drop(2).map { it.id })
    }
}
