package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DEFAULT_DURATION_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.defaultColumnWidth
import com.gabriele.notionlocal.data.entity.encodeDateRange
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
 * **"Duration"** (29/09/2026) nel database: il formato si salva con la
 * colonna, e le righe si ordinano per quanto durano.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DurationColumnTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val trips = PageEntity(title = "Viaggi", isDatabase = true)
    private val length = DatabaseColumnEntity(pageId = trips.id, name = "Length", type = ColumnType.DURATION)
    private val long = DatabaseRowEntity(pageId = trips.id, title = "Lungo", orderIndex = 0)
    private val short = DatabaseRowEntity(pageId = trips.id, title = "Corto", orderIndex = 1)
    private val open = DatabaseRowEntity(pageId = trips.id, title = "Senza fine", orderIndex = 2)
    private val day = 86_400_000L

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(trips)
        db.databaseDao().insertColumn(length)
        listOf(long, short, open).forEach { db.databaseDao().insertRow(it) }
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = long.id, columnId = length.id, value = encodeDateRange(0L, 30 * day, hasTime = true)))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = short.id, columnId = length.id, value = encodeDateRange(100 * day, 102 * day, hasTime = true)))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = open.id, columnId = length.id, value = encodeDateRange(5 * day, null, hasTime = true)))
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(trips.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.rows.size == 3 }
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

    private fun savedFormat() = runBlocking { db.databaseDao().getColumnsForPageOnce(trips.id) }.single().durationFormat

    @Test
    fun theFormatIsSavedWithTheColumn() {
        viewModel.updateColumn(length.id, "Length", ColumnType.DURATION, "[]", duration = DurationUnit.MONTHS)
        waitFor("anni e mesi") { savedFormat() == "MONTHS" }
        // Fino ai minuti è com'è di suo: non resta scritto niente.
        viewModel.updateColumn(length.id, "Length", ColumnType.DURATION, "[]", duration = DurationUnit.MINUTES)
        waitFor("di nuovo tutto") { savedFormat() == null }
    }

    @Test
    fun rowsSortByHowLongTheyLast() {
        viewModel.setSort(length.id, descending = false)
        // La più corta prima; quella senza fine, vuota, in fondo.
        waitFor("dal più corto") { viewModel.tableState.value.rows.map { it.title } == listOf("Corto", "Lungo", "Senza fine") }
        viewModel.setSort(length.id, descending = true)
        waitFor("dal più lungo") { viewModel.tableState.value.rows.map { it.title }.take(2) == listOf("Lungo", "Corto") }
    }

    @Test
    fun aDurationColumnStartsWide() {
        assertEquals(DEFAULT_DURATION_COLUMN_WIDTH_DP, defaultColumnWidth(length.id, ColumnType.DURATION))
    }
}
