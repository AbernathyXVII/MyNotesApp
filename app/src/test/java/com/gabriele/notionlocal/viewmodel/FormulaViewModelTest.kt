package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.Calculation
import com.gabriele.notionlocal.data.entity.CalculationGroup
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.calculationsFor
import com.gabriele.notionlocal.data.entity.canGroup
import com.gabriele.notionlocal.data.formula.FormulaConfig
import com.gabriele.notionlocal.data.formula.FormulaError
import com.gabriele.notionlocal.data.formula.FormulaValue
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **La proprietà Formula** (28/10/2026) col database vero: ogni riga ha il
 * suo risultato, che cambia quando cambiano le celle che legge; si ordina
 * per numero, si cerca, segue una proprietà rinominata e non si scrive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FormulaViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var repository: DatabaseRepository
    private lateinit var viewModel: DatabaseViewModel

    private val food = PageEntity(title = "Food", isDatabase = true)
    private val price = DatabaseColumnEntity(id = "prezzo", pageId = food.id, name = "Prezzo", type = ColumnType.NUMBER, orderIndex = 0)
    private val quantity = DatabaseColumnEntity(id = "quanti", pageId = food.id, name = "Quantità", type = ColumnType.NUMBER, orderIndex = 1)
    private val total = DatabaseColumnEntity(
        id = "totale", pageId = food.id, name = "Totale", type = ColumnType.FORMULA, orderIndex = 2,
        optionsJson = FormulaConfig.encode(FormulaConfig("prop(\"Prezzo\") * prop(\"Quantità\")"))
    )
    private val milk = DatabaseRowEntity(id = "latte", pageId = food.id, title = "Latte", orderIndex = 0)
    private val bread = DatabaseRowEntity(id = "pane", pageId = food.id, title = "Pane", orderIndex = 1)
    private val eggs = DatabaseRowEntity(id = "uova", pageId = food.id, title = "Uova", orderIndex = 2)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        repository = DatabaseRepository(db)
        pages.createPage(food)
        listOf(price, quantity, total).forEach { db.databaseDao().insertColumn(it) }
        listOf(milk, bread, eggs).forEach { db.databaseDao().insertRow(it) }
        // Latte 1,5 × 4 = 6; pane 2 × 1 = 2; uova 3,2 × 10 = 32.
        listOf(
            milk to "1.5" to "4", bread to "2" to "1", eggs to "3.2" to "10"
        ).forEach { (rowPrice, q) ->
            val (row, p) = rowPrice
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = price.id, value = p))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = quantity.id, value = q))
        }
        viewModel = DatabaseViewModel(repository, pages)
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

    private val state get() = viewModel.tableState.value
    private fun shown(rowId: String, columnId: String = total.id) = state.cellValues[rowId to columnId]
    private fun columnNow(id: String) = state.columns.single { it.id == id }
    private fun rowNow(id: String) = state.rows.single { it.id == id }

    @Test
    fun everyRowHasItsResult() {
        viewModel.load(food.id)
        waitFor("i totali") { shown(eggs.id) == "32" }
        assertEquals("6", shown(milk.id))
        assertEquals("2", shown(bread.id))
        assertEquals(FormulaValue.Num(6.0), state.formulas[milk.id to total.id])
    }

    @Test
    fun theResultFollowsTheCellsItReads() {
        viewModel.load(food.id)
        waitFor("i totali") { shown(milk.id) == "6" }
        viewModel.setCellValue(rowNow(milk.id), columnNow(quantity.id), "10")
        waitFor("il totale nuovo") { shown(milk.id) == "15" }
        // Svuotata, la quantità conta zero.
        viewModel.setCellValue(rowNow(milk.id), columnNow(quantity.id), "")
        waitFor("il totale a zero") { shown(milk.id) == "0" }
    }

    @Test
    fun aFormulaCannotBeWrittenByHand() = runBlocking<Unit> {
        viewModel.load(food.id)
        waitFor("i totali") { shown(milk.id) == "6" }
        viewModel.setCellValue(rowNow(milk.id), columnNow(total.id), "999")
        Thread.sleep(200)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(null, db.databaseDao().getCell(milk.id, total.id))
        assertEquals("6", shown(milk.id))
    }

    @Test
    fun aColumnThatBecomesAFormulaHidesWhatItHadWritten() = runBlocking<Unit> {
        val note = DatabaseColumnEntity(id = "nota", pageId = food.id, name = "Nota", type = ColumnType.TEXT, orderIndex = 3)
        db.databaseDao().insertColumn(note)
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = milk.id, columnId = note.id, value = "scritto a mano"))
        viewModel.load(food.id)
        waitFor("la nota") { shown(milk.id, note.id) == "scritto a mano" }
        repository.updateColumn(note.copy(type = ColumnType.FORMULA, optionsJson = FormulaConfig.encode(FormulaConfig("upper(prop(\"Name\"))"))))
        waitFor("la formula") { shown(milk.id, note.id) == "LATTE" }
    }

    @Test
    fun sortsByTheNumberNotByTheText() {
        viewModel.load(food.id)
        waitFor("i totali") { shown(eggs.id) == "32" }
        viewModel.setSort(total.id, descending = false)
        // Come testo "32" verrebbe prima di "6".
        waitFor("dal più piccolo") { state.rows.map { it.title } == listOf("Pane", "Latte", "Uova") }
        viewModel.setSort(total.id, descending = true)
        waitFor("dal più grande") { state.rows.map { it.title } == listOf("Uova", "Latte", "Pane") }
    }

    @Test
    fun theSearchFindsTheResult() {
        viewModel.load(food.id)
        waitFor("i totali") { shown(eggs.id) == "32" }
        viewModel.setSearchQuery("32")
        waitFor("la riga trovata") { state.rows.map { it.title } == listOf("Uova") }
    }

    @Test
    fun renamingAPropertyRewritesTheFormula() = runBlocking<Unit> {
        viewModel.load(food.id)
        waitFor("i totali") { shown(milk.id) == "6" }
        viewModel.renameColumn(price.id, "Costo")
        waitFor("la formula riscritta") {
            runBlocking { FormulaConfig.parse(db.databaseDao().getColumnById(total.id)!!.optionsJson).expression } ==
                "prop(\"Costo\") * prop(\"Quantità\")"
        }
        waitFor("il nome nuovo") { columnNow(price.id).name == "Costo" }
        // E il totale c'è ancora.
        waitFor("il totale") { shown(milk.id) == "6" }
    }

    @Test
    fun setFormulaChangesEveryRow() {
        viewModel.load(food.id)
        waitFor("i totali") { shown(milk.id) == "6" }
        viewModel.setFormula(total.id, "if(prop(\"Prezzo\") > 2, \"caro\", \"ok\")")
        waitFor("le scritte") { shown(eggs.id) == "caro" && shown(milk.id) == "ok" && shown(bread.id) == "ok" }
        // Scritta male: un errore in ogni riga, e la cella non ha testo.
        viewModel.setFormula(total.id, "prop(\"Boh\")")
        waitFor("l'errore") { state.formulas[milk.id to total.id] is FormulaValue.Error }
        assertEquals("", shown(milk.id))
    }

    @Test
    fun thePreviewUsesARealRow() {
        viewModel.load(food.id)
        waitFor("i totali") { shown(eggs.id) == "32" }
        val onEggs = viewModel.previewFormula(total.id, "prop(\"Prezzo\") + 1", eggs.id)!!
        assertEquals(eggs.id, onEggs.row.id)
        assertEquals(4.2, (onEggs.value as FormulaValue.Num).value, 1e-9)
        // Senza una riga, la prima; per una proprietà che non c'è ancora, lo stesso.
        val first = viewModel.previewFormula(null, "prop(\"Name\")", null)!!
        assertEquals(milk.id, first.row.id)
        assertEquals(FormulaValue.Text("Latte"), first.value)
        assertEquals(
            FormulaError.Kind.UNKNOWN_PROPERTY,
            (viewModel.previewFormula(total.id, "prop(\"Boh\")", null)!!.value as FormulaValue.Error).error.kind
        )
        // L'anteprima non cambia niente.
        assertEquals("6", shown(milk.id))
    }

    @Test
    fun aFormulaReadsTheNameUnderItsOwnTitle() = runBlocking<Unit> {
        db.pageDao().update(db.pageDao().getById(food.id)!!.copy(nameColumnTitle = "Prodotto"))
        val label = DatabaseColumnEntity(
            id = "etichetta", pageId = food.id, name = "Etichetta", type = ColumnType.FORMULA, orderIndex = 3,
            optionsJson = FormulaConfig.encode(FormulaConfig("prop(\"Prodotto\") + \" · \" + prop(\"Name\")"))
        )
        db.databaseDao().insertColumn(label)
        viewModel.load(food.id)
        waitFor("l'etichetta") { shown(milk.id, label.id) == "Latte · Latte" }
    }

    @Test
    fun aFormulaIsCalculatedLikeANumberOrADateButNotGrouped() {
        val groups = calculationsFor(ColumnType.FORMULA)
        assertTrue(Calculation.SUM in groups[CalculationGroup.MORE].orEmpty())
        assertTrue(Calculation.LATEST_DATE in groups[CalculationGroup.DATE].orEmpty())
        assertFalse(ColumnType.FORMULA.canGroup)
    }
}
