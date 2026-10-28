package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DateGroupBy
import com.gabriele.notionlocal.data.entity.DateGroupings
import com.gabriele.notionlocal.data.entity.DateUnit
import com.gabriele.notionlocal.data.entity.GroupSlot
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.boardGroupRules
import com.gabriele.notionlocal.data.entity.tableGroupRules
import com.gabriele.notionlocal.data.entity.encodeDateGroupings
import com.gabriele.notionlocal.data.entity.endOf
import com.gabriele.notionlocal.data.entity.parseDateGroupings
import com.gabriele.notionlocal.data.entity.startOf
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.buildRowGroups
import com.gabriele.notionlocal.ui.screen.dateGroupLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **"Date by"** (21/10/2026): raggruppando per una data, gruppi per giorno,
 * settimana, mese o anno, dal più vecchio o dal più recente. Per le
 * vittorie K-pop dell'utente: una fascia per settimana, come su Soridata.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DateGroupingTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val locale = Locale.getDefault()

    private val wins = PageEntity(title = "KPOP Music Shows", isDatabase = true, databaseLayout = DatabaseLayout.BOARD)
    private val show = DatabaseColumnEntity(pageId = wins.id, name = "Music show", type = ColumnType.SELECT, orderIndex = 0)
    private val winDate = DatabaseColumnEntity(pageId = wins.id, name = "Win date", type = ColumnType.DATE, orderIndex = 1)

    // Mercoledì e sabato della stessa settimana, il martedì dopo, l'ultimo giorno del 2025, e una senza data.
    private val days = listOf(
        "Show Champion" to LocalDate.of(2026, 10, 14),
        "Show! Music Core" to LocalDate.of(2026, 10, 17),
        "The Show" to LocalDate.of(2026, 10, 20),
        "Inkigayo" to LocalDate.of(2025, 12, 31),
        "Senza data" to null
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        // Come sul telefono dell'utente, in inglese americano: lì la settimana del
        // calendario comincia di domenica, ma quelle dei gruppi il lunedì, come su Soridata.
        Locale.setDefault(Locale.US)
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(wins)
        listOf(show, winDate).forEach { db.databaseDao().insertColumn(it) }
        days.forEachIndexed { index, (title, day) ->
            val row = DatabaseRowEntity(pageId = wins.id, title = title, orderIndex = index)
            db.databaseDao().insertRow(row)
            if (day != null) {
                val millis = day.atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli()
                db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = winDate.id, value = millis.toString()))
            }
        }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(wins.id)
        waitFor("le righe") { viewModel.tableState.value.rows.size == 5 && viewModel.page.value != null }
    }

    @After
    fun tearDown() {
        db.close()
        Locale.setDefault(locale)
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

    private fun groups(by: DateGroupBy) =
        buildRowGroups(viewModel.tableState.value, winDate, hideEmpty = true, dateBy = by)
            .map { group -> group.key to group.rows.map { it.title } }

    private fun key(day: LocalDate) = day.toEpochDay().toString()

    @Test
    fun theUnitsStartAndEndWhereTheyShould() {
        val wednesday = LocalDate.of(2026, 10, 14)
        assertEquals(wednesday, DateUnit.DAY.startOf(wednesday))
        assertEquals(LocalDate.of(2026, 10, 12), DateUnit.WEEK.startOf(wednesday))
        assertEquals(LocalDate.of(2026, 10, 11), DateUnit.WEEK.startOf(wednesday, DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 10, 18), DateUnit.WEEK.endOf(LocalDate.of(2026, 10, 12)))
        assertEquals(LocalDate.of(2026, 10, 1), DateUnit.MONTH.startOf(wednesday))
        assertEquals(LocalDate.of(2026, 2, 28), DateUnit.MONTH.endOf(LocalDate.of(2026, 2, 1)))
        assertEquals(LocalDate.of(2026, 1, 1), DateUnit.YEAR.startOf(wednesday))
        assertEquals(LocalDate.of(2026, 12, 31), DateUnit.YEAR.endOf(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun byDayItIsAsBefore() {
        assertEquals(
            listOf(
                key(LocalDate.of(2025, 12, 31)) to listOf("Inkigayo"),
                key(LocalDate.of(2026, 10, 14)) to listOf("Show Champion"),
                key(LocalDate.of(2026, 10, 17)) to listOf("Show! Music Core"),
                key(LocalDate.of(2026, 10, 20)) to listOf("The Show"),
                "" to listOf("Senza data")
            ),
            groups(DateGroupBy())
        )
    }

    @Test
    fun byWeekNewestFirstLikeSoridata() {
        assertEquals(
            listOf(
                key(LocalDate.of(2026, 10, 19)) to listOf("The Show"),
                key(LocalDate.of(2026, 10, 12)) to listOf("Show Champion", "Show! Music Core"),
                key(LocalDate.of(2025, 12, 29)) to listOf("Inkigayo"),
                // Quelle senza data restano in fondo anche così.
                "" to listOf("Senza data")
            ),
            groups(DateGroupBy(DateUnit.WEEK, newestFirst = true))
        )
        // Il nome dice il primo e l'ultimo giorno.
        assertTrue(dateGroupLabel(DateUnit.WEEK, LocalDate.of(2026, 10, 12)).contains(" – "))
    }

    @Test
    fun byMonthAndByYear() {
        assertEquals(
            listOf(key(LocalDate.of(2025, 12, 1)), key(LocalDate.of(2026, 10, 1)), ""),
            groups(DateGroupBy(DateUnit.MONTH)).map { it.first }
        )
        assertEquals(
            listOf(key(LocalDate.of(2026, 1, 1)) to listOf("Show Champion", "Show! Music Core", "The Show"), key(LocalDate.of(2025, 1, 1)) to listOf("Inkigayo")),
            groups(DateGroupBy(DateUnit.YEAR, newestFirst = true)).dropLast(1)
        )
        assertEquals("2026", dateGroupLabel(DateUnit.YEAR, LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun eachGroupingOfTheViewKeepsItsOwn() {
        // Dal 28/10/2026 "Date by" sta in ogni regola: le colonne per Music show,
        // le fasce per settimana; la tabella per mese.
        viewModel.setBoardGroupColumn(show.id)
        viewModel.addGroupRule(board = true, winDate.id)
        viewModel.setGroupRuleDateBy(board = true, winDate.id, DateGroupBy(DateUnit.WEEK, newestFirst = true))
        viewModel.addGroupRule(board = false, winDate.id)
        viewModel.setGroupRuleDateBy(board = false, winDate.id, DateGroupBy(DateUnit.MONTH))
        waitFor("salvati") {
            val page = runBlocking { db.pageDao().getById(wins.id)!! }
            page.boardGroupRules().lastOrNull()?.dateBy?.unit == DateUnit.WEEK && page.tableGroupRules().singleOrNull()?.dateBy?.unit == DateUnit.MONTH
        }
        // La pagina del ViewModel arriva anche lei dal database: si aspetta l'ultima.
        waitFor("i livelli") { viewModel.boardGroupLevels().size == 2 && viewModel.tableGroupLevels().size == 1 }
        assertEquals(DateGroupBy(DateUnit.WEEK, newestFirst = true), viewModel.boardGroupLevels()[1].dateBy)
        assertEquals(DateGroupBy(), viewModel.boardGroupLevels()[0].dateBy)
        assertEquals(DateGroupBy(DateUnit.MONTH), viewModel.tableGroupLevels()[0].dateBy)
        // Cambiando la proprietà di una regola, "Date by" riparte da capo.
        viewModel.setGroupRuleColumn(board = false, winDate.id, show.id)
        assertEquals(DateGroupBy(), viewModel.tableGroupLevels()[0].dateBy)
    }

    @Test
    fun theJsonIsSmallAndForgiving() {
        assertNull(encodeDateGroupings(DateGroupings()))
        assertEquals("""{"board":{"unit":"WEEK"}}""", encodeDateGroupings(DateGroupings().with(GroupSlot.BOARD, DateGroupBy(DateUnit.WEEK))))
        assertEquals(DateGroupings(), parseDateGroupings("non è json"))
        assertEquals(DateGroupings(), parseDateGroupings(null))
    }
}
