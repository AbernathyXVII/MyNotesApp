package com.gabriele.notionlocal.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I template delle pagine**: stanno a parte, e il loro contenuto si copia
 * nelle pagine nuove dei database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TemplateTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun line(pageId: String, text: String, order: Int, type: BlockType = BlockType.PARAGRAPH, parent: String? = null) =
        BlockEntity(pageId = pageId, type = type, textJson = """[{"text":"$text"}]""", orderIndex = order, parentBlockId = parent)

    private suspend fun template(title: String): String =
        pages.createTemplate().also { pages.renamePage(it, title) }

    private suspend fun content(pageId: String): List<Pair<BlockType, String>> =
        db.blockDao().getBlocksForPageOnce(pageId)
            .filter { it.parentBlockId == null }
            .sortedBy { it.orderIndex }
            .map { it.type to it.textJson }

    @Test
    fun aTemplateIsListedButIsNeitherAStartPageNorASearchResult() = runBlocking<Unit> {
        val id = template("SaaS")
        assertEquals(listOf(id), pages.observeTemplates().first().map { it.id })
        assertTrue(pages.startupCandidates().none { it.id == id })
        assertTrue(pages.search("SaaS", inContent = true, inTrash = false).isEmpty())
    }

    @Test
    fun theTemplateTakesThePlaceOfTheEmptyLinesOfANewPage() = runBlocking<Unit> {
        val saas = template("SaaS")
        val toggle = line(saas, "Dettagli", 2, BlockType.TOGGLE)
        db.blockDao().insertAll(
            listOf(
                line(saas, "1", 0, BlockType.CHECKBOX),
                line(saas, "2", 1, BlockType.CHECKBOX),
                toggle,
                line(saas, "Dentro", 0, parent = toggle.id)
            )
        )
        val page = PageEntity(title = "2", isRowPage = true)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(line(page.id, "", 0), line(page.id, "", 1)))

        assertTrue(pages.applyTemplate(saas, page.id) { it })

        assertEquals(content(saas), content(page.id))
        val copied = db.blockDao().getBlocksForPageOnce(page.id)
        val copiedToggle = copied.single { it.type == BlockType.TOGGLE }
        // Il figlio sta sotto il toggle copiato, non sotto quello del template.
        assertEquals(copiedToggle.id, copied.single { it.parentBlockId != null }.parentBlockId)
        // Il template resta com'era.
        assertEquals(4, db.blockDao().getBlocksForPageOnce(saas).size)
    }

    @Test
    fun onAPageWithTextTheTemplateGoesAtTheEnd() = runBlocking<Unit> {
        val saas = template("SaaS")
        db.blockDao().insertAll(listOf(line(saas, "1", 0, BlockType.CHECKBOX)))
        val page = PageEntity(title = "Scritta", isRowPage = true)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(line(page.id, "Ciao", 0)))

        pages.applyTemplate(saas, page.id) { it }

        assertEquals(
            listOf(BlockType.PARAGRAPH to """[{"text":"Ciao"}]""", BlockType.CHECKBOX to """[{"text":"1"}]"""),
            content(page.id)
        )
    }

    @Test
    fun aTemplateBackFromTheTrashIsATemplateAgainNotAPageOfTheMainMenu() = runBlocking<Unit> {
        pages.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
        val id = template("SaaS")
        pages.moveToTrash(id)
        assertTrue(pages.observeTemplates().first().isEmpty())

        pages.restoreFromTrash(id)

        assertEquals(listOf(id), pages.observeTemplates().first().map { it.id })
        assertTrue(db.blockDao().getBlocksLinkingTo(id).isEmpty())
    }
}
