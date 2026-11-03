package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DEFAULT_IMAGE_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.defaultColumnWidth
import com.gabriele.notionlocal.data.entity.parseColumnWidths
import com.gabriele.notionlocal.data.entity.snapColumnWidth
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import com.gabriele.notionlocal.ui.screen.cellFitWidth
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
 * **"Image"** (29/09/2026): una cella quadrata con un'immagine intera. La
 * colonna nasce quadrata; ogni riga ha il suo file, anche quando la si copia.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ImageColumnTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private val home = PageEntity(title = "Casa")
    private val dex = PageEntity(title = "Pokédex", isDatabase = true)
    private val sprite = DatabaseColumnEntity(pageId = dex.id, name = "Sprite", type = ColumnType.IMAGE)
    private val bulbasaur = DatabaseRowEntity(pageId = dex.id, title = "Bulbasaur", orderIndex = 0)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(home)
        pages.createPage(dex)
        db.blockDao().insertAll(
            listOf(BlockEntity(pageId = home.id, type = BlockType.DATABASE_LINK, linkedPageId = dex.id, orderIndex = 0))
        )
        db.databaseDao().insertColumn(sprite)
        db.databaseDao().insertRow(bulbasaur)
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = bulbasaur.id, columnId = sprite.id, value = "bulbasaur.img"))
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

    @Test
    fun anImageColumnStartsSquare() {
        assertEquals(DEFAULT_IMAGE_COLUMN_WIDTH_DP, defaultColumnWidth(sprite.id, ColumnType.IMAGE))
        assertEquals(150, defaultColumnWidth("altra", ColumnType.TEXT))
        // Trascinata fin quasi al quadrato, torna quadrata.
        assertEquals(44, snapColumnWidth(sprite.id, 47, 44))
        assertEquals(60, snapColumnWidth(sprite.id, 60, 44))
    }

    @Test
    fun backToSquareNothingIsKept() {
        val viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(dex.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.columns.isNotEmpty() }
        viewModel.setColumnWidth(sprite.id, 90)
        waitFor("larga 90") { parseColumnWidths(runBlocking { db.pageDao().getById(dex.id) }!!.columnWidths)[sprite.id] == 90 }
        viewModel.setColumnWidth(sprite.id, 46)
        waitFor("di nuovo quadrata") { runBlocking { db.pageDao().getById(dex.id) }!!.columnWidths == null }
    }

    @Test
    fun aCopiedDatabaseHasItsOwnImageFiles() = runBlocking<Unit> {
        val copyId = pages.duplicatePage(dex.id, DuplicateTarget.NextToOriginal, "copy") { "copia-di-$it" }!!
        val cell = db.databaseDao().getAllCellsForPageOnce(copyId).single()
        assertEquals("copia-di-bulbasaur.img", cell.value)
    }

    @Test
    fun aDuplicatedRowHasItsOwnImageFile() = runBlocking<Unit> {
        val rowPage = pages.ensureRowPage(bulbasaur.id)!!
        pages.duplicatePage(rowPage, DuplicateTarget.NextToOriginal, "copy") { "copia-di-$it" }!!
        val values = db.databaseDao().getAllCellsForPageOnce(dex.id).map { it.value }.sorted()
        assertEquals(listOf("bulbasaur.img", "copia-di-bulbasaur.img"), values)
    }

    @Test
    fun fitToContentKeepsTheSquare() {
        val measure: (String, androidx.compose.ui.text.TextStyle) -> androidx.compose.ui.unit.Dp = { text, _ -> (text.length * 7).dp }
        // Più i 12 punti per lato che la misura aggiunge: 44, il quadrato.
        assertEquals(20.dp, cellFitWidth(sprite, bulbasaur, "bulbasaur.img", Typography(), measure))
    }
}
