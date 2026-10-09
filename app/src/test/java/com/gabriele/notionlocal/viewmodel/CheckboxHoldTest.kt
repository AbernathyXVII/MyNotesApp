package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
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
import java.time.Duration

/**
 * **Le righe ferme dopo una spunta** (08/10/2026): in una lista ordinata
 * (o filtrata) per la sua casella, spuntare una riga non la fa saltare via
 * da sotto il dito; va al suo posto un secondo dopo l'ultima spunta, o
 * uscendo dalla pagina. Vedi `DatabaseViewModel.checkboxHold`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CheckboxHoldTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val food = PageEntity(title = "Food", isDatabase = true)
    private val bought = DatabaseColumnEntity(pageId = food.id, name = "Checkbox", type = ColumnType.CHECKBOX, orderIndex = 0)
    private val names = listOf("Shampoo", "Dentifricio", "Spazzolino", "Pasta")
    private lateinit var rows: List<DatabaseRowEntity>

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(food)
        db.databaseDao().insertColumn(bought)
        rows = names.mapIndexed { i, name ->
            DatabaseRowEntity(pageId = food.id, title = name, orderIndex = i, createdAt = 1_000L + i).also { row ->
                db.databaseDao().insertRow(row)
                // Solo la pasta è già presa: come in Food, le spuntate stanno sotto.
                db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = bought.id, value = (name == "Pasta").toString()))
            }
        }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(food.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.rows.size == names.size }
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
        fail("Non è successo: $what — righe: ${titles()}")
    }

    private fun titles() = viewModel.tableState.value.rows.map { it.title }

    /** Fa passare il tempo dell'app (non quello vero): i conti alla rovescia vanno avanti. */
    private fun elapse(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun checked(name: String) =
        viewModel.tableState.value.cellValues[rows.first { it.title == name }.id to bought.id] == "true"

    private fun tick(name: String, value: Boolean) {
        viewModel.setCellValue(rows.first { it.title == name }, bought, value.toString())
        waitFor("$name ${if (value) "spuntata" else "tolta"}") { checked(name) == value }
    }

    @Test
    fun aTickedRowStaysUnderTheFingerUntilReleased() {
        viewModel.setSort(bought.id, descending = false)
        waitFor("l'ordine per casella") { titles() == listOf("Shampoo", "Dentifricio", "Spazzolino", "Pasta") }

        tick("Shampoo", true)
        // Presa, ma ancora in cima: sotto il dito c'è sempre la stessa riga, e
        // un secondo tocco lì la toglie invece di spuntarne un'altra.
        assertEquals(listOf("Shampoo", "Dentifricio", "Spazzolino", "Pasta"), titles())
        tick("Shampoo", false)
        tick("Shampoo", true)
        assertEquals(listOf("Shampoo", "Dentifricio", "Spazzolino", "Pasta"), titles())

        viewModel.releaseCheckboxHold()
        waitFor("le prese in fondo") { titles().take(2) == listOf("Dentifricio", "Spazzolino") }
        assertEquals(setOf("Shampoo", "Pasta"), titles().drop(2).toSet())
    }

    @Test
    fun aSecondAfterTheLastTickTheRowsGoToTheirPlace() {
        viewModel.setSort(bought.id, descending = false)
        waitFor("l'ordine per casella") { titles() == listOf("Shampoo", "Dentifricio", "Spazzolino", "Pasta") }

        tick("Shampoo", true)
        elapse(600)
        assertEquals(listOf("Shampoo", "Dentifricio", "Spazzolino", "Pasta"), titles())
        // Un'altra spunta prima che passi il secondo: il conto riparte, e
        // scenderanno insieme.
        tick("Dentifricio", true)
        elapse(600)
        assertEquals(listOf("Shampoo", "Dentifricio", "Spazzolino", "Pasta"), titles())
        elapse(500)
        waitFor("le prese in fondo") { titles().first() == "Spazzolino" }
        assertEquals(setOf("Shampoo", "Dentifricio", "Pasta"), titles().drop(1).toSet())
    }

    @Test
    fun aTickedRowStaysVisibleUnderAFilterOnTheCheckbox() {
        viewModel.setFilter(bought.id, "false")
        waitFor("solo le non prese") { titles() == listOf("Shampoo", "Dentifricio", "Spazzolino") }

        tick("Dentifricio", true)
        assertEquals(listOf("Shampoo", "Dentifricio", "Spazzolino"), titles())

        viewModel.releaseCheckboxHold()
        waitFor("il filtro di nuovo") { titles() == listOf("Shampoo", "Spazzolino") }
    }

    @Test
    fun withoutSortOrFilterOnTheCheckboxNothingIsHeld() {
        tick("Dentifricio", true)
        viewModel.setSort(com.gabriele.notionlocal.data.entity.SORT_BY_NAME, descending = false)
        waitFor("l'ordine per nome") { titles() == listOf("Dentifricio", "Pasta", "Shampoo", "Spazzolino") }
    }

    @Test
    fun changingTheSortReleasesTheRows() {
        viewModel.setSort(bought.id, descending = false)
        waitFor("l'ordine per casella") { titles().last() == "Pasta" }
        tick("Shampoo", true)
        assertEquals("Shampoo", titles().first())
        // Cambiando ordine, le righe vanno subito dove dice quello nuovo.
        viewModel.setSort(bought.id, descending = true)
        waitFor("le prese in cima") { titles().take(2).toSet() == setOf("Shampoo", "Pasta") }
    }
}
