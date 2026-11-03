package com.gabriele.notionlocal.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository.IncomingBlock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Condividi con notE" e le note al volo** (02/11/2026) sul database: la
 * pagina Inbox, quello che arriva come pagina nuova o in fondo a una pagina,
 * e la nota al volo che, lasciata vuota, se ne va.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ShareRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PageRepository(db)
        repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
        repo.createPage(PageEntity(id = "spesa", title = "Spesa"))
        db.blockDao().insertAll(
            listOf(
                BlockEntity(pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "spesa", orderIndex = 0),
                BlockEntity(id = "vecchia", pageId = "spesa", textJson = """[{"text":"Uova"}]""", orderIndex = 0)
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private fun blocks(pageId: String) = runBlocking { db.blockDao().getBlocksForPageOnce(pageId).sortedBy { it.orderIndex } }

    private fun text(block: BlockEntity) = Regex(""""text":"([^"]*)"""").find(block.textJson)?.groupValues?.get(1).orEmpty()

    @Test
    fun theInboxIsBornOnceAtTheBottomOfTheMainMenu() = runBlocking<Unit> {
        val inbox = repo.inboxPageId(null, "Inbox")
        val page = db.pageDao().getById(inbox)!!
        assertEquals("Inbox", page.title)
        assertEquals("📥", page.icon)
        assertEquals(listOf("spesa", inbox), blocks(PageEntity.ROOT_PAGE_ID).map { it.linkedPageId })
        // Una volta scelta, resta quella.
        assertEquals(inbox, repo.inboxPageId(inbox, "Inbox"))
        assertEquals("spesa", repo.inboxPageId("spesa", "Inbox"))
        // Nel cestino non va bene: ne nasce un'altra.
        repo.moveToTrash("spesa")
        assertNotEquals("spesa", repo.inboxPageId("spesa", "Inbox"))
    }

    @Test
    fun whatArrivesBecomesANewPageInTheChosenOne() = runBlocking<Unit> {
        val pageId = repo.saveIncoming(
            "spesa", "Un articolo",
            listOf(
                IncomingBlock(BlockType.PARAGRAPH, "Da leggere"),
                IncomingBlock(BlockType.BOOKMARK, attrs = BlockAttrs(url = "https://example.com")),
                IncomingBlock(BlockType.IMAGE, attrs = BlockAttrs(file = "foto.img", name = "foto.jpg"))
            ),
            asNewPage = true
        )
        assertEquals("Un articolo", db.pageDao().getById(pageId)!!.title)
        // Il collegamento in fondo alla pagina scelta, dopo quello che c'era.
        assertEquals(listOf(null, pageId), blocks("spesa").map { it.linkedPageId })
        val inside = blocks(pageId)
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.BOOKMARK, BlockType.IMAGE), inside.map { it.type })
        assertEquals("Da leggere", text(inside[0]))
        assertEquals("https://example.com", inside[1].attrs.url)
        assertEquals("foto.img", inside[2].attrs.file)
    }

    @Test
    fun orAtTheEndOfThePage() = runBlocking<Unit> {
        val pageId = repo.saveIncoming("spesa", "", listOf(IncomingBlock(BlockType.PARAGRAPH, "Latte"), IncomingBlock(BlockType.PARAGRAPH, "Pane")), asNewPage = false)
        assertEquals("spesa", pageId)
        assertEquals(listOf("Uova", "Latte", "Pane"), blocks("spesa").map(::text))
    }

    @Test
    fun aQuickNoteLeftEmptyGoesAway() = runBlocking<Unit> {
        val (pageId, lineId) = repo.createQuickNote("spesa")
        assertEquals("", db.pageDao().getById(pageId)!!.title)
        assertEquals(listOf(lineId), blocks(pageId).map { it.id })
        assertEquals(listOf(null, pageId), blocks("spesa").map { it.linkedPageId })

        repo.finishQuickNote(pageId)
        assertNull(db.pageDao().getById(pageId))
        // Niente collegamento orfano nella pagina che la conteneva.
        assertEquals(listOf("vecchia"), blocks("spesa").map { it.id })
    }

    @Test
    fun aQuickNoteWithoutTitleTakesItsFirstLine() = runBlocking<Unit> {
        val (pageId, lineId) = repo.createQuickNote("spesa")
        db.blockDao().insertAll(
            listOf(
                blocks(pageId).single().copy(textJson = """[{"text":""}]"""),
                BlockEntity(pageId = pageId, textJson = """[{"text":"  Chiamare il dentista  "}]""", orderIndex = 1),
                BlockEntity(pageId = pageId, textJson = """[{"text":"domani"}]""", orderIndex = 2)
            )
        )
        repo.finishQuickNote(pageId)
        assertEquals("Chiamare il dentista", db.pageDao().getById(pageId)!!.title)
        assertTrue(blocks(pageId).any { it.id == lineId })
    }

    @Test
    fun aQuickNoteWithItsTitleOrAnImageStays() = runBlocking<Unit> {
        val (titled, _) = repo.createQuickNote("spesa")
        db.pageDao().setTitle(titled, "Idee", 1L)
        repo.finishQuickNote(titled)
        assertEquals("Idee", db.pageDao().getById(titled)!!.title)

        val (withImage, line) = repo.createQuickNote("spesa")
        db.blockDao().insertAll(listOf(blocks(withImage).single { it.id == line }.copy(type = BlockType.IMAGE)))
        repo.finishQuickNote(withImage)
        assertEquals("", db.pageDao().getById(withImage)!!.title)
    }
}
