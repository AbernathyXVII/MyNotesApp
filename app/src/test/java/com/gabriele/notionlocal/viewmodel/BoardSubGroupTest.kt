package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.boardGroupRules
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import java.time.Instant
import java.time.LocalDate
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
 * **La bacheca raggruppata per quasi ogni proprietà, e le sue fasce**
 * (14/10/2026): prima si sceglieva solo fra le proprietà a selezione
 * singola, e i sottogruppi c'erano solo nella tabella. Dal 28/10/2026 le
 * fasce sono le regole dopo la prima, quante se ne vogliono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BoardSubGroupTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel

    private val food = PageEntity(title = "Food", isDatabase = true, databaseLayout = DatabaseLayout.BOARD)
    private val shop = DatabaseColumnEntity(pageId = food.id, name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0)
    private val kind = DatabaseColumnEntity(pageId = food.id, name = "Tipologia prodotto", type = ColumnType.SELECT, orderIndex = 1)
    private val bought = DatabaseColumnEntity(pageId = food.id, name = "Checkbox", type = ColumnType.CHECKBOX, orderIndex = 2)
    private val day = DatabaseColumnEntity(pageId = food.id, name = "Giorno", type = ColumnType.DATE, orderIndex = 3)
    private val color = DatabaseColumnEntity(pageId = food.id, name = "Colore", type = ColumnType.COLOR, orderIndex = 4)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(food)
        listOf(shop, kind, bought, day, color).forEach { db.databaseDao().insertColumn(it) }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(food.id)
        waitFor("le colonne") { viewModel.tableState.value.columns.size == 5 && viewModel.page.value != null }
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

    private fun saved() = runBlocking { db.pageDao().getById(food.id)!! }

    @Test
    fun theBoardCanBeGroupedByTheCheckboxNowButNotByAColor() {
        // Senza scelta, la prima a selezione singola, come sempre.
        assertEquals(shop.id, viewModel.boardGroupColumn()?.id)
        viewModel.setBoardGroupColumn(bought.id)
        assertEquals(bought.id, viewModel.boardGroupColumn()?.id)
        viewModel.setBoardGroupColumn(kind.id)
        assertEquals(kind.id, viewModel.boardGroupColumn()?.id)
        // Un colore non si legge come nome di colonna: si ripiega.
        viewModel.setBoardGroupColumn(color.id)
        assertEquals(shop.id, viewModel.boardGroupColumn()?.id)
    }

    @Test
    fun theBandsAreTheRulesAfterTheFirstAndNeverOnTheColumnsProperty() {
        // Senza regole, le colonne di ripiego compaiono come prima regola.
        assertEquals(listOf(shop.id), viewModel.groupRulesShown(board = true).map { it.column })
        viewModel.addGroupRule(board = true, kind.id)
        viewModel.addGroupRule(board = true, day.id)
        assertEquals(listOf(shop.id, kind.id, day.id), viewModel.boardGroupLevels().map { it.column.id })
        waitFor("salvate") { saved().boardGroupRules().map { it.column } == listOf(shop.id, kind.id, day.id) }
        // Le colonne mostrano anche quelle vuote; le fasce di suo no.
        assertEquals(listOf(false, true, true), viewModel.boardGroupLevels().map { it.hideEmpty })
        // La proprietà delle colonne non fa anche una fascia.
        viewModel.addGroupRule(board = true, shop.id)
        assertEquals(3, viewModel.boardGroupLevels().size)
        // Un colore non si legge: anche in una regola, si salta.
        viewModel.addGroupRule(board = true, color.id)
        assertEquals(listOf(shop.id, kind.id, day.id), viewModel.boardGroupLevels().map { it.column.id })
    }

    @Test
    fun choosingABandPropertyForTheColumnsTakesItOutOfTheBands() {
        viewModel.setBoardGroupColumn(shop.id)
        viewModel.addGroupRule(board = true, kind.id)
        viewModel.addGroupRule(board = true, bought.id)
        viewModel.setBoardGroupColumn(kind.id)
        assertEquals(listOf(kind.id, bought.id), viewModel.boardGroupLevels().map { it.column.id })
        waitFor("anche nel database") { saved().boardGroupRules().map { it.column } == listOf(kind.id, bought.id) }
    }

    @Test
    fun removingAllTheRulesGoesBackToTheFirstSelect() {
        viewModel.setBoardGroupColumn(bought.id)
        viewModel.addGroupRule(board = true, kind.id)
        viewModel.removeAllGroupRules(board = true)
        assertEquals(listOf(shop.id), viewModel.boardGroupLevels().map { it.column.id })
        waitFor("tolte") { saved().boardGroupRulesJson == null }
    }

    @Test
    fun aCardAddedInABandGetsTheColumnAndEveryBandValue() {
        viewModel.setBoardGroupColumn(bought.id)
        viewModel.addGroupRule(board = true, shop.id)
        viewModel.addGroupRule(board = true, kind.id)
        viewModel.addRowInGroups(listOf(bought.id to "true", shop.id to "IPERPOLI", kind.id to "Cibo"))
        waitFor("la scheda nuova coi tre valori") {
            val state = viewModel.tableState.value
            val row = state.rows.singleOrNull() ?: return@waitFor false
            state.cellValues[row.id to bought.id] == "true" &&
                state.cellValues[row.id to shop.id] == "IPERPOLI" &&
                state.cellValues[row.id to kind.id] == "Cibo"
        }
    }

    @Test
    fun aRowAddedInADateGroupFallsOnThatDay() {
        // I gruppi per data sono giorni (`toEpochDay`): prima la cella
        // riceveva quel numero così com'era, cioè il 1° gennaio 1970.
        val october14 = LocalDate.of(2026, 10, 14)
        viewModel.addRowInGroups(listOf(day.id to october14.toEpochDay().toString()))
        waitFor("la riga del 14 ottobre") {
            val state = viewModel.tableState.value
            val row = state.rows.singleOrNull() ?: return@waitFor false
            val start = state.cellValues[row.id to day.id]?.let { parseDateRange(it) }?.start ?: return@waitFor false
            Instant.ofEpochMilli(start).atZone(AppSettings.zoneId).toLocalDate() == october14
        }
    }
}
