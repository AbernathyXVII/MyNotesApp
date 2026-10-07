package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
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
 * **Il blocco "Image"** (06/10/2026): nasce vuoto e chiede la galleria, si
 * sposta in fondo a un'altra pagina, e il suo file va e viene con le pagine.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ImageBlockTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Qui")
    private val altrove = PageEntity(title = "Altrove")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PageRepository(db)
        repository.createPage(page)
        repository.createPage(altrove)
        viewModel = PageEditorViewModel(repository)
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

    private fun blocksOf(pageId: String) = runBlocking { db.blockDao().getBlocksForPageOnce(pageId) }.sortedBy { it.orderIndex }

    private fun line(pageId: String, text: String, at: Int, type: BlockType = BlockType.PARAGRAPH, attrs: BlockAttrs = BlockAttrs()) =
        BlockEntity(pageId = pageId, type = type, textJson = """[{"text":"$text"}]""", orderIndex = at, attrsJson = BlockAttrs.encode(attrs))

    private fun open(vararg blocks: BlockEntity) {
        runBlocking { db.blockDao().insertAll(blocks.toList()) }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == blocks.size }
    }

    @Test
    fun anImageFromTheMenuStartsEmptyAndAsksForTheGallery() {
        val riga = line(page.id, "/image", 0)
        open(riga)

        viewModel.convertToMedia(viewModel.blocks.value.first(), BlockType.IMAGE)
        waitFor("la richiesta") { viewModel.mediaRequest.value != null }
        assertEquals(MediaRequest(riga.id, MediaRequestKind.PICK), viewModel.mediaRequest.value)
        val image = blocksOf(page.id).single()
        assertEquals(BlockType.IMAGE, image.type)
        assertEquals("[]", image.textJson)
        assertEquals(BlockAttrs(), image.attrs)
    }

    @Test
    fun movedToAnotherPageTheImageGoesToTheBottom() {
        val sopra = line(page.id, "Sopra", 0)
        val image = line(page.id, "", 1, BlockType.IMAGE, BlockAttrs(file = "foto.img"))
        val sotto = line(page.id, "Sotto", 2)
        runBlocking {
            db.blockDao().insertAll(listOf(line(altrove.id, "Primo", 0), line(altrove.id, "Secondo", 1)))
        }
        open(sopra, image, sotto)

        var moved = false
        viewModel.moveBlockToPage(viewModel.blocks.value.first { it.id == image.id }, altrove.id) { moved = true }
        waitFor("lo spostamento") { moved }

        assertEquals(listOf("Sopra", "Sotto"), blocksOf(page.id).map { viewModel.plainTextOf(it) })
        assertEquals(listOf(0, 1), blocksOf(page.id).map { it.orderIndex })
        val there = blocksOf(altrove.id)
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.PARAGRAPH, BlockType.IMAGE), there.map { it.type })
        assertEquals("foto.img", there.last().attrs.file)
    }

    @Test
    fun aDuplicatedImageHasItsOwnFileAndTheFilesLeaveWithThePage() = runBlocking<Unit> {
        val image = line(page.id, "", 0, BlockType.IMAGE, BlockAttrs(file = "foto.img", name = "Mare.jpg"))
        db.blockDao().insert(image)

        val copyId = repository.duplicateBlock(image.id) { "copia-di-$it" }!!
        val copy = db.blockDao().getById(copyId)!!
        assertEquals(BlockAttrs(file = "copia-di-foto.img", name = "Mare.jpg"), copy.attrs)

        val files = repository.deletePermanently(page.id)
        assertTrue(files.containsAll(listOf("foto.img", "copia-di-foto.img")))
    }

    @Test
    fun backspaceUnderACalloutJoinsItsText() {
        val callout = line(page.id, "Nota", 0, BlockType.CALLOUT)
        val riga = line(page.id, " in più", 1)
        open(callout, riga)

        assertTrue(viewModel.backspaceAtLineStart(listOf(riga.id), 0))
        waitFor("la fusione") { blocksOf(page.id).size == 1 }
        val merged = blocksOf(page.id).single()
        assertEquals(BlockType.CALLOUT, merged.type)
        assertEquals("Nota in più", viewModel.plainTextOf(merged))
    }
}
