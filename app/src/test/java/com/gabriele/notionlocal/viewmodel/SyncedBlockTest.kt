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
import com.gabriele.notionlocal.data.sync.SyncedBlocks
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
import java.time.Duration

/**
 * **I blocchi sincronizzati nell'editor** (03/11/2026): "Synced block" dal
 * menu "/" mette il cursore dentro, e quello che si scrive in una copia
 * arriva da solo alle altre, anche a due copie nella stessa pagina, senza
 * che si rimandino la modifica all'infinito.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SyncedBlockTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(id = "qui", title = "Qui")
    private val altrove = PageEntity(id = "altrove", title = "Altrove")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PageRepository(db)
        repository.createPage(page)
        repository.createPage(altrove)
        SyncedBlocks.debounceMs = 50
        viewModel = PageEditorViewModel(repository)
    }

    @After
    fun tearDown() {
        runBlocking { SyncedBlocks.flush() }
        SyncedBlocks.debounceMs = 600
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    private fun text(t: String) = """[{"text":"$t"}]"""

    private fun stored(pageId: String) =
        runBlocking { db.blockDao().getBlocksForPageOnce(pageId) }

    private fun linesOf(blockId: String, pageId: String) =
        stored(pageId).filter { it.parentBlockId == blockId }.sortedBy { it.orderIndex }.map { it.textJson }

    @Test
    fun theSlashMenuPutsTheCursorInsideTheBlock() {
        runBlocking { db.blockDao().insert(BlockEntity(id = "riga", pageId = page.id, textJson = text("/synced"), orderIndex = 0)) }
        viewModel.load(page.id)
        waitFor("la riga") { viewModel.blocks.value.size == 1 }
        viewModel.makeSyncedBlock(viewModel.blocks.value.single())
        waitFor("il blocco") { stored(page.id).any { it.type == BlockType.SYNCED } }
        val child = stored(page.id).single { it.parentBlockId == "riga" }
        waitFor("il cursore") { viewModel.focusRequestBlockId.value == child.id }
    }

    @Test
    fun writingInOneCopyChangesTheOthers() {
        val copyId = runBlocking {
            db.blockDao().insert(BlockEntity(id = "s", pageId = page.id, textJson = text("Uno"), orderIndex = 0))
            repository.makeSynced("s")
            repository.addSyncedCopy("s", altrove.id)!!
        }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 2 }
        val line = viewModel.blocks.value.single { it.parentBlockId == "s" }
        viewModel.updateBlockText(line, "Uno e due")
        waitFor("la copia riscritta") { linesOf(copyId, altrove.id) == listOf(text("Uno e due")) }
        // Di nuovo: la seconda modifica arriva anche lei.
        waitFor("il testo mostrato") { viewModel.blocks.value.any { it.textJson == text("Uno e due") } }
        viewModel.updateBlockText(viewModel.blocks.value.single { it.parentBlockId == "s" }, "Tre")
        waitFor("la copia riscritta ancora") { linesOf(copyId, altrove.id) == listOf(text("Tre")) }
    }

    @Test
    fun twoCopiesInTheSamePageDoNotBounce() {
        val copyId = runBlocking {
            db.blockDao().insert(BlockEntity(id = "s", pageId = page.id, textJson = text("Uno"), orderIndex = 0))
            repository.makeSynced("s")
            repository.addSyncedCopy("s", page.id)!!
        }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 4 }
        viewModel.updateBlockText(viewModel.blocks.value.single { it.parentBlockId == "s" }, "Nuovo")
        waitFor("l'altra copia") { linesOf(copyId, page.id) == listOf(text("Nuovo")) }
        // Passata la pausa, nessuna rimanda indietro niente: tutte e due restano uguali.
        Thread.sleep(300)
        runBlocking { SyncedBlocks.flush() }
        waitFor("le copie mostrate") { viewModel.blocks.value.count { it.textJson == text("Nuovo") } == 2 }
        assertEquals(listOf(text("Nuovo")), linesOf("s", page.id))
        assertEquals(listOf(text("Nuovo")), linesOf(copyId, page.id))
        assertEquals(0, runBlocking { repository.pushSynced("s") })
    }
}
