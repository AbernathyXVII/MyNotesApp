package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.plainText
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
 * **Tutta la pagina selezionata, come su Notion** (20/10/2026): la copia ha
 * ogni riga nell'ordine in cui si legge, col suo tipo; "Cut" e "Delete" tolgono
 * tutto tranne sottopagine e database, e si possono annullare.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageSelectAllTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Libro")
    private val subpage = PageEntity(title = "Sottopagina")
    private val database = PageEntity(title = "Parole", isDatabase = true)

    private fun block(type: BlockType, text: String, at: Int, parent: String? = null, checked: Boolean = false, indent: Int = 0, linked: String? = null) =
        BlockEntity(
            pageId = page.id, parentBlockId = parent, type = type, isChecked = checked, indentLevel = indent,
            textJson = """[{"text":"$text"}]""", orderIndex = at, linkedPageId = linked
        )

    private val title = block(BlockType.PARAGRAPH, "Capitolo", 0)
    private val bullet = block(BlockType.BULLET_LIST_ITEM, "punto", 1, indent = 1)
    private val box = block(BlockType.CHECKBOX, "fatto", 2, checked = true)
    private val callout = block(BlockType.CALLOUT, "uno\\ndue", 3)
    private val table = block(BlockType.TABLE, "", 4).copy(tableRows = 2, tableCols = 2)
    private val divider = block(BlockType.DIVIDER, "", 5)
    private val toggle = block(BlockType.TOGGLE, "Apri", 6)
    private val inside = block(BlockType.PARAGRAPH, "dentro", 0, parent = toggle.id)
    private val pageLink = block(BlockType.PAGE_LINK, "", 7, linked = subpage.id)
    private val databaseLink = block(BlockType.DATABASE_LINK, "", 8, linked = database.id)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(page)
        pages.createPage(subpage.copy(parentId = page.id))
        pages.createPage(database.copy(parentId = page.id))
        db.blockDao().insertAll(listOf(title, bullet, box, callout, table, divider, toggle, inside, pageLink, databaseLink))
        pages.setTableCellText(table.id, 0, 0, "A")
        pages.setTableCellText(table.id, 0, 1, "B")
        pages.setTableCellText(table.id, 1, 1, "D")
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 10 }
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

    private fun savedTypes() = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.map { it.type }.toSet()

    @Test
    fun theCopyHasEveryLineInReadingOrderWithItsType() {
        val lines = runBlocking { viewModel.pageCopyLines() }
        assertEquals(
            listOf("Capitolo", "punto", "fatto", "uno", "due", "A\tB", "\tD", "---", "Apri", "dentro"),
            lines.map { it.spans.plainText() }
        )
        assertEquals(
            listOf(
                CopiedLine(BlockType.PARAGRAPH),
                CopiedLine(BlockType.BULLET_LIST_ITEM, indent = 1),
                CopiedLine(BlockType.CHECKBOX, checked = true),
                null, null, null, null, null, null,
                // Dentro il toggle: un rientro in più.
                CopiedLine(BlockType.PARAGRAPH, indent = 1)
            ),
            lines.map { it.kind }
        )
    }

    @Test
    fun deleteLeavesSubpagesAndDatabasesAndCanBeUndone() {
        viewModel.deletePageContent()
        waitFor("tutto tolto") { savedTypes() == setOf(BlockType.PAGE_LINK, BlockType.DATABASE_LINK) }
        // Le pagine che vivono altrove restano.
        assertEquals(setOf("Sottopagina", "Parole"), runBlocking { listOfNotNull(pages.getPage(subpage.id), pages.getPage(database.id)) }.map { it.title }.toSet())

        viewModel.undo()
        waitFor("tutto tornato") { runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.size == 10 }
    }
}
