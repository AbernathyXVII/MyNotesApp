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
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.encodeSortRules
import com.gabriele.notionlocal.data.entity.rowTree
import com.gabriele.notionlocal.data.entity.sortedAsBlocks
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
 * **Le sotto-righe con le caselle e l'ordinamento** (26/10/2026, chiesto
 * dall'utente, sulla sua lista "Food"): spuntate tutte le sotto-righe si
 * spunta la riga principale; e una riga con le sue sotto-righe si ordina
 * come un blocco, al posto del suo elemento che viene prima.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SubRowCheckboxTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel

    private val food = PageEntity(
        title = "Food", isDatabase = true,
        // Come la tabella dell'utente: prima le non spuntate, poi per "Order".
        sortRulesJson = null
    )
    private val checkbox = DatabaseColumnEntity(id = "checkbox", pageId = food.id, name = "Checkbox", type = ColumnType.CHECKBOX, orderIndex = 0)
    private val order = DatabaseColumnEntity(id = "order", pageId = food.id, name = "Order", type = ColumnType.NUMBER, orderIndex = 1)

    private val pasta = DatabaseRowEntity(id = "pasta", pageId = food.id, title = "Pasta", orderIndex = 0)
    private val rosticceria = DatabaseRowEntity(id = "rosticceria", pageId = food.id, title = "Rosticceria", orderIndex = 1)
    private val patate = DatabaseRowEntity(id = "patate", pageId = food.id, title = "Patate al forno", orderIndex = 2, parentRowId = "rosticceria")
    private val pollo = DatabaseRowEntity(id = "pollo", pageId = food.id, title = "Bocconcini di pollo", orderIndex = 3, parentRowId = "rosticceria")
    private val spazzolino = DatabaseRowEntity(id = "spazzolino", pageId = food.id, title = "Spazzolino", orderIndex = 4)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(
            food.copy(sortRulesJson = encodeSortRules(listOf(SortRuleSpec(checkbox.id), SortRuleSpec(order.id))))
        )
        listOf(checkbox, order).forEach { db.databaseDao().insertColumn(it) }
        listOf(pasta, rosticceria, patate, pollo, spazzolino).forEach { db.databaseDao().insertRow(it) }
        fun cell(row: DatabaseRowEntity, column: DatabaseColumnEntity, value: String) =
            runBlocking { db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = column.id, value = value)) }
        // Pasta da prendere; Rosticceria spuntata con dentro due cose da prendere; Spazzolino preso.
        cell(pasta, order, "1")
        cell(rosticceria, checkbox, "true")
        cell(rosticceria, order, "2")
        cell(spazzolino, checkbox, "true")
        cell(spazzolino, order, "3")
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(food.id)
        waitFor("le righe") { viewModel.tableState.value.rows.size == 5 }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            // Anche l'orologio dell'app va avanti: dopo una spunta le righe
            // restano ferme un momento (`CHECKBOX_SETTLE_MS`) prima di spostarsi.
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(10))
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what — righe: ${shown()}")
    }

    /** Le righe come si vedono nella tabella: l'albero, con le sotto-righe rientrate. */
    private fun shown() = rowTree(viewModel.tableState.value.rows).map { "  ".repeat(it.depth) + it.row.title }

    private fun checked(row: DatabaseRowEntity) = viewModel.tableState.value.cellValues[row.id to checkbox.id] == "true"

    @Test
    fun aBlockWithSomethingStillToBuyStaysWithTheUncheckedOnes() {
        // Prima la Rosticceria scendeva fra le spuntate, con dentro le cose da prendere.
        assertEquals(
            listOf("Pasta", "Rosticceria", "  Patate al forno", "  Bocconcini di pollo", "Spazzolino"),
            shown()
        )
    }

    @Test
    fun checkingEverySubRowChecksTheMainRow() {
        // Rosticceria non spuntata: si spunta da sola con l'ultima sotto-riga.
        val unchecked = viewModel.tableState.value.rows.single { it.id == rosticceria.id }
        viewModel.setCellValue(unchecked, checkbox, "false")
        waitFor("tolta la spunta") { !checked(rosticceria) }
        viewModel.setCellValue(patate, checkbox, "true")
        waitFor("patate spuntate") { checked(patate) }
        assertEquals(false, checked(rosticceria))
        viewModel.setCellValue(pollo, checkbox, "true")
        waitFor("la Rosticceria spuntata da sola") { checked(rosticceria) }
        // Ora tutto il blocco è preso: va giù con le spuntate, le sotto-righe in ordine.
        waitFor("il blocco fra le spuntate") {
            shown() == listOf("Pasta", "Rosticceria", "  Patate al forno", "  Bocconcini di pollo", "Spazzolino")
        }
        // Togliendo una spunta si toglie anche alla riga principale (26/10/2026),
        // e il blocco torna su con le cose da prendere.
        viewModel.setCellValue(pollo, checkbox, "false")
        waitFor("pollo da prendere, e la Rosticceria con lui") { !checked(pollo) && !checked(rosticceria) }
        assertEquals(true, checked(patate))
        waitFor("il blocco in alto") {
            shown() == listOf("Pasta", "Rosticceria", "  Bocconcini di pollo", "  Patate al forno", "Spazzolino")
        }
    }

    @Test
    fun aMainRowCheckedByHandFollowsItsSubRowsAtTheFirstTouch() {
        // Come "Rosticceria" in "Food": spuntata a mano, con sotto cose ancora da prendere.
        assertEquals(true, checked(rosticceria))
        viewModel.setCellValue(patate, checkbox, "true")
        waitFor("si toglie la spunta: il pollo manca ancora") { checked(patate) && !checked(rosticceria) }
    }

    @Test
    fun uncheckingGoesUpThroughEveryLevel() = runBlocking<Unit> {
        // Tutto preso: patate (con l'olio sotto), pollo e la Rosticceria.
        val olio = DatabaseRowEntity(id = "olio", pageId = food.id, title = "Olio", orderIndex = 5, parentRowId = patate.id)
        db.databaseDao().insertRow(olio)
        listOf(olio, patate, pollo).forEach {
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = it.id, columnId = checkbox.id, value = "true"))
        }
        waitFor("tutto spuntato") { viewModel.tableState.value.rows.any { it.id == olio.id } && checked(olio) && checked(patate) && checked(pollo) }
        // Tolto l'olio: niente più patate né Rosticceria; il pollo resta.
        viewModel.setCellValue(olio, checkbox, "false")
        waitFor("su fino in cima") { !checked(olio) && !checked(patate) && !checked(rosticceria) }
        assertEquals(true, checked(pollo))
    }

    @Test
    fun theCheckGoesUpThroughEveryLevel() = runBlocking<Unit> {
        // Una sotto-riga delle patate: spuntata lei, si spuntano le patate e poi, col pollo, la Rosticceria.
        val olio = DatabaseRowEntity(id = "olio", pageId = food.id, title = "Olio", orderIndex = 5, parentRowId = patate.id)
        db.databaseDao().insertRow(olio)
        db.databaseDao().getCell(rosticceria.id, checkbox.id)?.let { db.databaseDao().updateCell(it.copy(value = "false")) }
        waitFor("l'olio") { viewModel.tableState.value.rows.any { it.id == olio.id } && !checked(rosticceria) }
        viewModel.setCellValue(pollo, checkbox, "true")
        waitFor("pollo") { checked(pollo) }
        viewModel.setCellValue(olio, checkbox, "true")
        waitFor("su fino in cima") { checked(patate) && checked(rosticceria) }
    }

    @Test
    fun blocksTakeThePlaceOfTheirFirstRow() {
        // Già in ordine dalle regole: c, b1 (sotto b), a, b, d.
        val a = DatabaseRowEntity(id = "a", pageId = "x", title = "a")
        val b = DatabaseRowEntity(id = "b", pageId = "x", title = "b")
        val b1 = DatabaseRowEntity(id = "b1", pageId = "x", title = "b1", parentRowId = "b")
        val b2 = DatabaseRowEntity(id = "b2", pageId = "x", title = "b2", parentRowId = "b")
        val c = DatabaseRowEntity(id = "c", pageId = "x", title = "c")
        assertEquals(listOf("c", "b", "b1", "b2", "a"), sortedAsBlocks(listOf(c, b1, a, b, b2)).map { it.title })
        // Senza sotto-righe non cambia niente.
        assertEquals(listOf(c, a, b), sortedAsBlocks(listOf(c, a, b)))
        // Una sotto-riga la cui riga principale non c'è (filtrata) resta dov'è.
        assertEquals(listOf("b1", "c", "a"), sortedAsBlocks(listOf(b1, c, a)).map { it.title })
    }
}
