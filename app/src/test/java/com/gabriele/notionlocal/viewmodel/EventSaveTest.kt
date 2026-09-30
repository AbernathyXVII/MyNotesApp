package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.EventDraft
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * **Il time-blocking come Google Calendar** (30/09/2026): un evento salvato
 * dalla finestra diventa una riga col nome, la data e il colore; "Edit
 * event" scrive solo quello che è cambiato; la modalità Day si salva.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EventSaveTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val zone = ZoneId.of("Europe/Rome")
    private val sunday = LocalDate.of(2026, 10, 11)

    private fun open(page: PageEntity, vararg columns: DatabaseColumnEntity) = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        columns.forEach { db.databaseDao().insertColumn(it) }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(page.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.columns.size == columns.size }
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

    private fun rows(pageId: String) = runBlocking { db.databaseDao().getRowsForPageOnce(pageId) }
    private fun cell(rowId: String, columnId: String) = runBlocking { db.databaseDao().getCell(rowId, columnId)?.value }

    @Test
    fun aNewEventIsARowWithItsNameDateAndColor() {
        val agenda = PageEntity(title = "Agenda", isDatabase = true)
        val date = DatabaseColumnEntity(pageId = agenda.id, name = "Date", type = ColumnType.DATE)
        open(agenda, date)

        val draft = EventDraft.at(sunday.atTime(9, 0)).withEnd(sunday.atTime(10, 0)).copy(title = "Corsetta", color = "#33B679")
        viewModel.saveEvent(null, draft.title, draft.cellValue(zone), draft.color)
        waitFor("la riga") { rows(agenda.id).size == 1 }

        val row = rows(agenda.id).single()
        assertEquals("Corsetta", row.title)
        assertEquals("#33B679", row.eventColor)
        waitFor("la data") { cell(row.id, date.id) != null }
        val range = parseDateRange(cell(row.id, date.id)!!)!!
        assertTrue(range.hasTime)
        assertEquals(sunday.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), range.start)
        assertEquals(sunday.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(), range.end)
    }

    @Test
    fun withoutADatePropertyItIsCreatedOnTheFly() {
        val agenda = PageEntity(title = "Agenda", isDatabase = true)
        open(agenda)

        val draft = EventDraft.at(sunday.atTime(0, 0))
        viewModel.saveEvent(null, "", draft.cellValue(zone), null)
        waitFor("la proprietà data") { viewModel.tableState.value.columns.any { it.type == ColumnType.DATE } }
        val date = viewModel.tableState.value.columns.single { it.type == ColumnType.DATE }
        // Il calendario usa proprio lei.
        waitFor("scelta") { runBlocking { db.pageDao().getById(agenda.id)!!.calendarDateColumnId } == date.id }
        waitFor("la riga") { rows(agenda.id).size == 1 }
        val row = rows(agenda.id).single()
        assertNull(row.eventColor)
        waitFor("la data") { cell(row.id, date.id) == draft.cellValue(zone) }
    }

    @Test
    fun editingWritesOnlyWhatChanged() {
        val agenda = PageEntity(title = "Agenda", isDatabase = true)
        val date = DatabaseColumnEntity(pageId = agenda.id, name = "Date", type = ColumnType.DATE)
        val existing = DatabaseRowEntity(pageId = agenda.id, title = "Coreano", updatedAt = 1_000L)
        open(agenda, date)
        runBlocking { db.databaseDao().insertRow(existing) }
        val start = sunday.atTime(10, 0)
        val value = EventDraft.at(start).withEnd(sunday.atTime(13, 0)).cellValue(zone)
        runBlocking { db.databaseDao().insertCell(com.gabriele.notionlocal.data.entity.DatabaseCellEntity(rowId = existing.id, columnId = date.id, value = value)) }
        waitFor("la riga nello stato") {
            viewModel.tableState.value.rows.any { it.id == existing.id } &&
                viewModel.tableState.value.cellValues[existing.id to date.id] == value
        }

        // Salvata così com'è: niente cambia, nemmeno "Last edited".
        val row = viewModel.tableState.value.rows.single { it.id == existing.id }
        val draft = EventDraft.of(row, parseDateRange(value), zone, start)
        viewModel.saveEvent(draft.rowId, draft.title, draft.cellValue(zone), draft.color)
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(200)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1_000L, runBlocking { db.databaseDao().getRowById(existing.id)!!.updatedAt })

        // Solo il colore.
        viewModel.saveEvent(draft.rowId, draft.title, draft.cellValue(zone), "#039BE5")
        waitFor("il colore") { runBlocking { db.databaseDao().getRowById(existing.id)!!.eventColor } == "#039BE5" }
        assertEquals("Coreano", runBlocking { db.databaseDao().getRowById(existing.id)!!.title })
        assertEquals(value, cell(existing.id, date.id))

        // Il nome e le ore.
        val moved = draft.withStart(sunday.atTime(11, 0)).copy(title = "Coreano 2")
        viewModel.saveEvent(moved.rowId, moved.title, moved.cellValue(zone), "#039BE5")
        waitFor("il nome") { runBlocking { db.databaseDao().getRowById(existing.id)!!.title } == "Coreano 2" }
        waitFor("le ore") { cell(existing.id, date.id) == moved.cellValue(zone) }
        assertEquals(1, rows(agenda.id).size)
    }

    @Test
    fun theDayModeIsSavedWithTheView() {
        val agenda = PageEntity(title = "Agenda", isDatabase = true)
        open(agenda)
        viewModel.setCalendarMode(CalendarMode.DAY)
        waitFor("Day") { runBlocking { db.pageDao().getById(agenda.id)!!.calendarMode } == CalendarMode.DAY }
        assertEquals(listOf(CalendarMode.YEAR, CalendarMode.MONTH, CalendarMode.WEEK, CalendarMode.DAY), CalendarMode.entries)
    }
}
