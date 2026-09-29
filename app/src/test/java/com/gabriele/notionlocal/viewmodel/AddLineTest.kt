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

/** "Add above" e "Add below" del menu dei sei puntini (29/09/2026). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AddLineTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Prova")
    private val database = PageEntity(title = "Spesa", isDatabase = true)

    private fun line(text: String, at: Int, indent: Int = 0, parent: String? = null, type: BlockType = BlockType.PARAGRAPH) =
        BlockEntity(pageId = page.id, parentBlockId = parent, type = type, textJson = """[{"text":"$text"}]""", indentLevel = indent, orderIndex = at)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        pages.createPage(database)
        viewModel = PageEditorViewModel(pages)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun open(vararg blocks: BlockEntity) {
        runBlocking { db.blockDao().insertAll(blocks.toList()) }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == maxOf(blocks.size, 1) }
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

    /** I figli di `parent` (null: il primo livello), in ordine, come testi. */
    private fun texts(parent: String? = null) = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }
        .filter { it.parentBlockId == parent }
        .sortedBy { it.orderIndex }
        .map { if (it.type == BlockType.DATABASE_LINK) "[database]" else viewModel.plainTextOf(it) }

    private fun shown(id: String) = viewModel.blocks.value.first { it.id == id }

    @Test
    fun belowAMiddleLineWithTheCursorInIt() {
        val uno = line("Uno", 0)
        val due = line("Due", 1, indent = 2, type = BlockType.BULLET_LIST_ITEM)
        val tre = line("Tre", 2)
        open(uno, due, tre)
        viewModel.addLineNextTo(shown(due.id), below = true)
        waitFor("una riga sotto Due") { texts() == listOf("Uno", "Due", "", "Tre") }
        val added = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.sortedBy { it.orderIndex }[2]
        // Un paragrafo, col rientro della riga toccata.
        assertEquals(BlockType.PARAGRAPH, added.type)
        assertEquals(2, added.indentLevel)
        waitFor("il cursore nella riga nuova") { viewModel.focusRequestBlockId.value == added.id }
    }

    @Test
    fun aboveTheFirstLine() {
        val uno = line("Uno", 0)
        val due = line("Due", 1)
        open(uno, due)
        viewModel.addLineNextTo(shown(uno.id), below = false)
        waitFor("una riga sopra Uno") { texts() == listOf("", "Uno", "Due") }
    }

    @Test
    fun aboveAndBelowADatabase() {
        val sopra = line("Sopra", 0)
        val table = BlockEntity(pageId = page.id, type = BlockType.DATABASE_LINK, linkedPageId = database.id, orderIndex = 1)
        val sotto = line("Sotto", 2)
        open(sopra, table, sotto)
        viewModel.addLineNextTo(shown(table.id), below = true)
        waitFor("una riga sotto il database") { texts() == listOf("Sopra", "[database]", "", "Sotto") }
        viewModel.addLineNextTo(shown(table.id), below = false)
        waitFor("e una sopra") { texts() == listOf("Sopra", "", "[database]", "", "Sotto") }
    }

    @Test
    fun insideAToggleTheLineStaysInside() {
        val toggle = line("Toggle", 0, type = BlockType.TOGGLE)
        val dentro = line("Dentro", 0, parent = toggle.id)
        val fuori = line("Fuori", 1)
        open(toggle, dentro, fuori)
        viewModel.addLineNextTo(shown(dentro.id), below = true)
        waitFor("una riga nel toggle") { texts(toggle.id) == listOf("Dentro", "") }
        assertEquals(listOf("Toggle", "Fuori"), texts())
    }

    @Test
    fun onTheEmptyLineOfANewPage() {
        // Nessuna riga nel database: si vede quella vuota di una pagina nuova.
        open()
        viewModel.addLineNextTo(viewModel.blocks.value.single(), below = true)
        waitFor("la riga vuota salvata e una sotto") { texts() == listOf("", "") }
    }
}
