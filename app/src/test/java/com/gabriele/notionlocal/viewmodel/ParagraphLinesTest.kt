package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ListGaps
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.entity.paragraph
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.RenderItem
import com.gabriele.notionlocal.ui.screen.groupIntoRenderItems
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **"Paragraph" di un gruppo di righe** (21/10/2026): le righe scelte hanno
 * il loro spazio e diventano un campo di testo a sé; scrivendoci, le righe
 * nuove lo tengono e quelle degli altri gruppi non si toccano.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ParagraphLinesTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Spazi")

    private fun line(text: String, at: Int, type: BlockType = BlockType.PARAGRAPH) =
        BlockEntity(pageId = page.id, type = type, textJson = """[{"text":"$text"}]""", orderIndex = at)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        viewModel = PageEditorViewModel(pages)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
        db.close()
    }

    private fun open(blocks: List<BlockEntity>) {
        runBlocking { db.blockDao().insertAll(blocks) }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == blocks.size && viewModel.page.value != null }
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

    private fun shown() = viewModel.blocks.value.filter { it.parentBlockId == null }.sortedBy { it.orderIndex }

    private fun runs() = shown().let { top ->
        groupIntoRenderItems(top, viewModel.spacingStartsOf(top) + viewModel.chunkStartsOf(top)).filterIsInstance<RenderItem.Run>()
    }

    @Test
    fun theChosenLinesBecomeAFieldOfTheirOwnAndNewLinesKeepTheSpace() {
        val lines = listOf(line("a", 0), line("b", 1), line("c", 2, BlockType.BULLET_LIST_ITEM), line("d", 3, BlockType.BULLET_LIST_ITEM))
        open(lines)
        assertEquals(1, runs().size)

        // "Only these lines" sulle ultime due: 8 pt fra i blocchi, 12 dopo il pallino.
        viewModel.setLinesParagraph(lines.drop(2), spacing = 8) { type -> if (type == BlockType.BULLET_LIST_ITEM) 12 else null }
        waitFor("lo spazio delle righe") { shown().count { it.attrs.spacing == 8 } == 2 }
        assertEquals(12, shown()[2].attrs.markerGap)
        assertNull(shown()[0].attrs.spacing)
        val split = runs()
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), split.map { r -> r.blocks.map { viewModel.plainTextOf(it) } })

        // Invio in fondo a "c", nel campo del gruppo: la riga nuova ha il suo spazio.
        val group = split[1].blocks.map { it.id }
        viewModel.updateRun(group, listOf("c", "", "d"), listOf("c", "d"), caretLine = 1)
        waitFor("la riga nuova") { shown().size == 5 }
        val born = shown()[3]
        assertEquals("", viewModel.plainTextOf(born))
        assertEquals(BlockAttrs(spacing = 8, markerGap = 12), born.attrs)
        // Il campo di sopra non si è toccato, e quello del gruppo ha tre righe.
        assertEquals(listOf(listOf("a", "b"), listOf("c", "", "d")), runs().map { r -> r.blocks.map { viewModel.plainTextOf(it) } })

        // Tornate come la pagina, i due campi tornano uno.
        viewModel.setLinesParagraph(shown().drop(2), spacing = null) { null }
        waitFor("come la pagina") { shown().none { it.attrs.spacing != null } }
        assertEquals(1, runs().size)
    }

    @Test
    fun thePageCanFollowSettingsOrHaveItsOwn() {
        open(listOf(line("a", 0), line("b", 1, BlockType.CHECKBOX)))
        val first = shown()[0]
        viewModel.setPageParagraph(PageParagraph(blockSpacing = 6, gaps = ListGaps(same = false, todo = 20)))
        waitFor("salvato") { runBlocking { db.pageDao().getById(page.id) }?.paragraphJson != null }
        assertEquals(6, viewModel.paragraphLook().spacingOf(first))
        assertEquals(20, viewModel.paragraphLook().gapOf(shown()[1], BlockType.CHECKBOX))
        assertEquals(PageParagraph(blockSpacing = 6, gaps = ListGaps(same = false, todo = 20)), viewModel.page.value!!.paragraph)

        // "As in Settings" per tutte e due: la colonna torna vuota.
        viewModel.setPageParagraph(PageParagraph())
        waitFor("come Settings") { runBlocking { db.pageDao().getById(page.id) }?.paragraphJson == null }
        // La pagina riletta dal database arriva un attimo dopo.
        waitFor("la pagina riletta") { viewModel.paragraphLook().spacingOf(first) == 0 }
    }
}
