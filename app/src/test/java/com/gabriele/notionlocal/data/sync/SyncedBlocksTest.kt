package com.gabriele.notionlocal.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I blocchi sincronizzati** (03/11/2026, chiesti dall'utente: "lo stesso
 * pezzo di testo in più pagine, che cambia ovunque insieme"): come nascono,
 * come si copiano in un'altra pagina e come una modifica arriva alle copie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SyncedBlocksTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository
    private val qui = PageEntity(id = "qui", title = "Qui")
    private val altrove = PageEntity(id = "altrove", title = "Altrove")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PageRepository(db)
        repository.createPage(qui)
        repository.createPage(altrove)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun text(t: String) = """[{"text":"$t"}]"""

    private fun stored(pageId: String) = runBlocking { db.blockDao().getBlocksForPageOnce(pageId) }

    private fun childrenOf(blockId: String, pageId: String) =
        stored(pageId).filter { it.parentBlockId == blockId }.sortedBy { it.orderIndex }

    /** Un blocco sincronizzato in "Qui" con [lines] dentro, e la sua copia in fondo ad "Altrove". */
    private fun syncedPair(vararg lines: String): Pair<BlockEntity, BlockEntity> = runBlocking {
        db.blockDao().insert(BlockEntity(id = "s", pageId = qui.id, textJson = text(lines.first()), orderIndex = 0))
        repository.makeSynced("s")
        lines.drop(1).forEachIndexed { i, t ->
            db.blockDao().insert(BlockEntity(pageId = qui.id, parentBlockId = "s", textJson = text(t), orderIndex = i + 1))
        }
        db.blockDao().insert(BlockEntity(id = "prima", pageId = altrove.id, textJson = text("Già qui"), orderIndex = 0))
        val copyId = repository.addSyncedCopy("s", altrove.id)!!
        db.blockDao().getById("s")!! to db.blockDao().getById(copyId)!!
    }

    @Test
    fun turnIntoKeepsTheTextAsTheFirstLine() = runBlocking {
        db.blockDao().insert(BlockEntity(id = "riga", pageId = qui.id, type = BlockType.BULLET_LIST_ITEM, textJson = text("Ciao"), orderIndex = 0))
        val first = repository.makeSynced("riga")!!
        val block = db.blockDao().getById("riga")!!
        assertEquals(BlockType.SYNCED, block.type)
        assertNotNull(block.attrs.syncId)
        assertEquals("[]", block.textJson)
        val child = childrenOf("riga", qui.id).single()
        assertEquals(first, child.id)
        // Un elemento di elenco resta un elemento di elenco, dentro.
        assertEquals(BlockType.BULLET_LIST_ITEM, child.type)
        assertEquals(text("Ciao"), child.textJson)
    }

    @Test
    fun theCopyGoesAtTheBottomWithTheSameLines() {
        val (source, copy) = syncedPair("Uno", "Due")
        assertEquals(source.attrs.syncId, copy.attrs.syncId)
        assertEquals(BlockType.SYNCED, copy.type)
        // In fondo alla pagina, dopo la riga che c'era.
        assertEquals("prima", stored(altrove.id).filter { it.parentBlockId == null }.minBy { it.orderIndex }.id)
        assertEquals(listOf(text("Uno"), text("Due")), childrenOf(copy.id, altrove.id).map { it.textJson })
        // Righe sue, nella sua pagina.
        assertTrue(childrenOf(copy.id, altrove.id).none { child -> childrenOf(source.id, qui.id).any { it.id == child.id } })
        assertEquals(2, runBlocking { repository.syncedPageCount(source.attrs.syncId!!) })
    }

    @Test
    fun aChangedLineIsRewrittenInPlaceInTheOtherCopies() = runBlocking {
        val (source, copy) = syncedPair("Uno", "Due")
        val before = childrenOf(copy.id, altrove.id).map { it.id }
        db.blockDao().setText(childrenOf(source.id, qui.id)[1].id, text("Due e mezzo"))
        assertEquals(1, repository.pushSynced(source.id))
        val after = childrenOf(copy.id, altrove.id)
        assertEquals(listOf(text("Uno"), text("Due e mezzo")), after.map { it.textJson })
        // Le stesse righe, riscritte: un campo aperto su una di loro resta suo.
        assertEquals(before, after.map { it.id })
        // Già uguali: niente da riscrivere.
        assertEquals(0, repository.pushSynced(source.id))
    }

    @Test
    fun aNewLineOrATableRebuildTheCopy() = runBlocking {
        val (source, copy) = syncedPair("Uno")
        db.blockDao().insert(BlockEntity(id = "t", pageId = qui.id, parentBlockId = source.id, type = BlockType.TABLE, orderIndex = 1))
        db.tableCellDao().insertCell(TableCellEntity(blockId = "t", rowIndex = 0, colIndex = 1, text = "B1"))
        db.blockDao().insert(BlockEntity(pageId = qui.id, parentBlockId = source.id, textJson = text("Tre"), orderIndex = 2))
        assertEquals(1, repository.pushSynced(source.id))
        val lines = childrenOf(copy.id, altrove.id)
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.TABLE, BlockType.PARAGRAPH), lines.map { it.type })
        assertEquals(listOf("B1"), db.tableCellDao().getCellsForBlockOnce(lines[1].id).map { it.text })
        // Una cella scritta arriva anche lei, al suo posto.
        db.tableCellDao().insertCell(TableCellEntity(blockId = "t", rowIndex = 1, colIndex = 0, text = "A2"))
        assertEquals(1, repository.pushSynced(source.id))
        assertEquals(lines[1].id, childrenOf(copy.id, altrove.id)[1].id)
        assertEquals(setOf("B1", "A2"), db.tableCellDao().getCellsForBlockOnce(lines[1].id).map { it.text }.toSet())
    }

    @Test
    fun aSubpageStaysInItsPageAndIsALinkInTheOthers() = runBlocking {
        val (source, copy) = syncedPair("Uno")
        repository.createPage(PageEntity(id = "sotto", title = "Sotto", parentId = qui.id))
        db.blockDao().insert(BlockEntity(pageId = qui.id, parentBlockId = source.id, type = BlockType.PAGE_LINK, linkedPageId = "sotto", orderIndex = 1))
        repository.pushSynced(source.id)
        val link = childrenOf(copy.id, altrove.id)[1]
        assertEquals(BlockType.PAGE_MENTION, link.type)
        assertEquals("sotto", link.attrs.pageId)
        // Una riga nuova scritta nella copia: nella pagina di casa la sottopagina resta tale.
        db.blockDao().insert(BlockEntity(pageId = altrove.id, parentBlockId = copy.id, textJson = text("Tre"), orderIndex = 2))
        assertEquals(1, repository.pushSynced(copy.id))
        val home = childrenOf(source.id, qui.id)
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.PAGE_LINK, BlockType.PARAGRAPH), home.map { it.type })
        assertEquals("sotto", home[1].linkedPageId)
        assertEquals(text("Tre"), home[2].textJson)
    }

    @Test
    fun unsyncLeavesTheLinesWhereTheBlockWas() = runBlocking {
        val (source, copy) = syncedPair("Uno", "Due")
        db.blockDao().insert(BlockEntity(id = "dopo", pageId = qui.id, textJson = text("Dopo"), orderIndex = 1))
        assertTrue(repository.unsync(source.id))
        val page = stored(qui.id).filter { it.parentBlockId == null }.sortedBy { it.orderIndex }
        assertEquals(listOf(text("Uno"), text("Due"), text("Dopo")), page.map { it.textJson })
        assertTrue(page.none { it.type == BlockType.SYNCED })
        // L'altra copia resta com'era.
        assertEquals(listOf(text("Uno"), text("Due")), childrenOf(copy.id, altrove.id).map { it.textJson })
        assertEquals(1, repository.syncedPageCount(copy.attrs.syncId!!))
        assertFalse(repository.unsync(source.id))
    }

    @Test
    fun aFileStillUsedByAnotherCopyIsNotDeleted() = runBlocking {
        val (source, copy) = syncedPair("Uno")
        db.blockDao().insert(BlockEntity(pageId = qui.id, parentBlockId = source.id, type = BlockType.IMAGE, attrsJson = BlockAttrs.encode(BlockAttrs(file = "foto.jpg")), orderIndex = 1))
        repository.pushSynced(source.id)
        assertEquals("foto.jpg", childrenOf(copy.id, altrove.id)[1].attrs.file)
        assertFalse("foto.jpg" in repository.deletePermanently(altrove.id))
        assertTrue("foto.jpg" in repository.deletePermanently(qui.id))
    }

    @Test
    fun theKeyIgnoresWhereTheLinesAre() {
        val (source, copy) = syncedPair("Uno", "Due")
        val here = SyncedBlocks.keyOf(SyncedBlocks.linesOf(source.id, stored(qui.id)))
        val there = SyncedBlocks.keyOf(SyncedBlocks.linesOf(copy.id, stored(altrove.id)))
        assertEquals(here, there)
        runBlocking { db.blockDao().setText(childrenOf(copy.id, altrove.id)[0].id, text("Altro")) }
        assertNotEquals(here, SyncedBlocks.keyOf(SyncedBlocks.linesOf(copy.id, stored(altrove.id))))
    }
}
