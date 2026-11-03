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
 * **"Turn into page" e si resta dove si è** (17/10/2026, chiesto
 * dall'utente): il database dentro la pagina diventa un collegamento, e
 * basta — nessun'altra pagina si apre, e il resto della pagina non cambia.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TurnDatabaseIntoPageTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Home")
    private val database = PageEntity(title = "Libri", isDatabase = true)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        pages.createPage(database.copy(parentId = page.id))
        db.blockDao().insertAll(
            listOf(
                BlockEntity(pageId = page.id, textJson = """[{"text":"Sopra"}]""", orderIndex = 0),
                BlockEntity(pageId = page.id, type = BlockType.DATABASE_LINK, linkedPageId = database.id, orderIndex = 1),
                BlockEntity(pageId = page.id, textJson = """[{"text":"Sotto"}]""", orderIndex = 2)
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

    @Test
    fun theDatabaseBecomesALinkAndNothingElse() {
        val before = saved()
        val block = viewModel.blocks.value.single { it.type == BlockType.DATABASE_LINK }
        // Senza nient'altro da fare dopo: la voce non apre niente.
        viewModel.turnDatabaseIntoPage(block)
        waitFor("il collegamento") { saved().any { it.type == BlockType.PAGE_LINK } }
        val after = saved()
        assertEquals(BlockType.PAGE_LINK, after[1].type)
        assertEquals(database.id, after[1].linkedPageId)
        // Le altre righe e il posto restano quelli.
        assertEquals(before.map { it.id }, after.map { it.id })
        assertEquals(listOf(before[0], before[2]), listOf(after[0], after[2]))
        // Il database non viene toccato.
        assertEquals(true, runBlocking { db.pageDao().getById(database.id) }?.isDatabase)
    }
}
