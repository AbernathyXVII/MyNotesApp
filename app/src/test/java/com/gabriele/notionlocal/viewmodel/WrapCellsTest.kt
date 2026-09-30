package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DEFAULT_IMAGE_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.MIN_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.MIN_NAME_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.MIN_WRITABLE_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.minColumnWidth
import com.gabriele.notionlocal.data.entity.parseColumnWidths
import com.gabriele.notionlocal.data.repository.DatabaseRepository
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
 * **"Wrap cells" e la larghezza minima** (30/09/2026): l'interruttore di
 * ogni tabella, e le colonne in cui si scrive che non vanno sotto un minimo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WrapCellsTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val games = PageEntity(title = "Giochi", isDatabase = true)
    private val notes = DatabaseColumnEntity(pageId = games.id, name = "Notes", type = ColumnType.TEXT)
    private val done = DatabaseColumnEntity(pageId = games.id, name = "Done", type = ColumnType.CHECKBOX, orderIndex = 1)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(games)
        db.databaseDao().insertColumn(notes)
        db.databaseDao().insertColumn(done)
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(games.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.columns.size == 2 }
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

    private fun saved() = runBlocking { db.pageDao().getById(games.id)!! }

    @Test
    fun theSwitchIsSavedWithTheView() {
        assertEquals(false, saved().wrapCells)
        viewModel.setWrapCells(true)
        waitFor("acceso") { saved().wrapCells }
        viewModel.setWrapCells(false)
        waitFor("spento") { !saved().wrapCells }
    }

    @Test
    fun theMinimumWidths() {
        assertEquals(MIN_NAME_COLUMN_WIDTH_DP, minColumnWidth(NAME_COLUMN_KEY))
        listOf(ColumnType.TEXT, ColumnType.NUMBER, ColumnType.URL, ColumnType.EMAIL, ColumnType.PHONE, ColumnType.COLOR).forEach {
            assertEquals(it.name, MIN_WRITABLE_COLUMN_WIDTH_DP, minColumnWidth("c", it))
        }
        assertEquals(MIN_COLUMN_WIDTH_DP, minColumnWidth("c", ColumnType.CHECKBOX))
        // "Image" resta quadrata: il minimo non la allarga.
        assertEquals(DEFAULT_IMAGE_COLUMN_WIDTH_DP, minColumnWidth("c", ColumnType.IMAGE))
    }

    @Test
    fun aWritableColumnStopsAtItsMinimum() {
        viewModel.setColumnWidth(notes.id, 10)
        waitFor("il minimo del testo") { parseColumnWidths(saved().columnWidths)[notes.id] == MIN_WRITABLE_COLUMN_WIDTH_DP }
        viewModel.setColumnWidth(NAME_COLUMN_KEY, 10)
        waitFor("il minimo di Name") { parseColumnWidths(saved().columnWidths)[NAME_COLUMN_KEY] == MIN_NAME_COLUMN_WIDTH_DP }
        // Una casella da spuntare può restare stretta come prima.
        viewModel.setColumnWidth(done.id, 10)
        waitFor("il minimo di sempre") { parseColumnWidths(saved().columnWidths)[done.id] == MIN_COLUMN_WIDTH_DP }
    }
}
