package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.Calculation
import com.gabriele.notionlocal.data.entity.CellAlignment
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.encodeViewCalculations
import com.gabriele.notionlocal.data.entity.hasSubRows
import com.gabriele.notionlocal.data.entity.parseCalculationAlignments
import com.gabriele.notionlocal.data.entity.parseCalculationSubRows
import com.gabriele.notionlocal.data.entity.parseViewCalculations
import com.gabriele.notionlocal.data.entity.rowTree
import com.gabriele.notionlocal.data.entity.rowsForCalculation
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * **Le sotto-righe** (29/09/2026): "Marmellata" con sotto "Albicocca",
 * "Pesca", "Fragola" — righe vere, con la loro pagina e le loro proprietà,
 * che si aprono e si chiudono con la freccia sulla riga principale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SubRowsTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var rows: DatabaseRepository
    private lateinit var viewModel: DatabaseViewModel

    private val home = PageEntity(title = "Spesa")
    private val food = PageEntity(title = "Dispensa", isDatabase = true)
    private val shelf = DatabaseColumnEntity(pageId = food.id, name = "Scaffale", type = ColumnType.SELECT)

    private fun row(title: String, order: Int, parent: String? = null, collapsed: Boolean = false) =
        DatabaseRowEntity(pageId = food.id, title = title, orderIndex = order, parentRowId = parent, subRowsCollapsed = collapsed)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        rows = DatabaseRepository(db)
        pages.createPage(home)
        pages.createPage(food)
        db.blockDao().insertAll(
            listOf(BlockEntity(pageId = home.id, type = BlockType.DATABASE_LINK, linkedPageId = food.id, orderIndex = 0))
        )
        db.databaseDao().insertColumn(shelf)
        viewModel = DatabaseViewModel(rows, pages)
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

    private fun saved() = runBlocking { db.databaseDao().getRowsForPageOnce(food.id) }

    // --- L'albero ---

    @Test
    fun subRowsSitUnderTheirRowIndented() {
        val jam = row("Marmellata", 0)
        val pasta = row("Pasta", 1)
        val apricot = row("Albicocca", 2, jam.id)
        val peach = row("Pesca", 3, jam.id)
        val tree = rowTree(listOf(jam, pasta, apricot, peach))
        assertEquals(listOf("Marmellata", "Albicocca", "Pesca", "Pasta"), tree.map { it.row.title })
        assertEquals(listOf(0, 1, 1, 0), tree.map { it.depth })
        assertEquals(listOf(true, false, false, false), tree.map { it.hasChildren })
        assertTrue(hasSubRows(listOf(jam, apricot)))
        assertFalse(hasSubRows(listOf(jam, pasta)))
    }

    @Test
    fun aClosedRowHidesItsSubRowsButKeepsTheArrow() {
        val jam = row("Marmellata", 0, collapsed = true)
        val apricot = row("Albicocca", 1, jam.id)
        val tree = rowTree(listOf(jam, apricot))
        assertEquals(listOf("Marmellata"), tree.map { it.row.title })
        assertTrue(tree.single().hasChildren)
    }

    @Test
    fun deeperLevelsAndHiddenParents() {
        val jam = row("Marmellata", 0)
        val apricot = row("Albicocca", 1, jam.id)
        val organic = row("Bio", 2, apricot.id)
        assertEquals(listOf(0, 1, 2), rowTree(listOf(jam, apricot, organic)).map { it.depth })
        // La riga principale filtrata via: la sotto-riga resta, in cima.
        assertEquals(listOf("Albicocca" to 0, "Bio" to 1), rowTree(listOf(apricot, organic)).map { it.row.title to it.depth })
    }

    @Test
    fun rowsInALoopDoNotDisappear() {
        val a = row("A", 0).copy(id = "a", parentRowId = "b")
        val b = row("B", 1).copy(id = "b", parentRowId = "a")
        assertEquals(setOf("A", "B"), rowTree(listOf(a, b)).map { it.row.title }.toSet())
    }

    // --- I calcoli ---

    @Test
    fun calculationsCountOnlyMainRowsUnlessAsked() {
        val jam = row("Marmellata", 0)
        val all = listOf(jam, row("Albicocca", 1, jam.id), row("Pesca", 2, jam.id))
        assertEquals(listOf("Marmellata"), rowsForCalculation(all, includeSubRows = false).map { it.title })
        assertEquals(3, rowsForCalculation(all, includeSubRows = true).size)
    }

    @Test
    fun includeSubRowsIsKeptWithTheOtherCalculationSettings() {
        val text = encodeViewCalculations(
            mapOf("c" to Calculation.COUNT_ALL),
            mapOf("c" to CellAlignment.LEFT),
            setOf("c")
        )
        assertEquals(mapOf("c" to Calculation.COUNT_ALL), parseViewCalculations(text))
        assertEquals(mapOf("c" to CellAlignment.LEFT), parseCalculationAlignments(text))
        assertEquals(setOf("c"), parseCalculationSubRows(text))
        assertEquals(emptySet<String>(), parseCalculationSubRows(encodeViewCalculations(mapOf("c" to Calculation.COUNT_ALL))))
    }

    @Test
    fun theSwitchSurvivesChangingTheCalculation() {
        viewModel.load(food.id)
        waitFor("il database") { viewModel.page.value != null }
        viewModel.setCalculationSubRows(shelf.id, true)
        waitFor("acceso") { parseCalculationSubRows(viewModel.page.value?.viewCalculations) == setOf(shelf.id) }
        viewModel.setCalculation(shelf.id, Calculation.COUNT_ALL)
        viewModel.setCalculationAlignment(shelf.id, CellAlignment.CENTER)
        assertEquals(setOf(shelf.id), parseCalculationSubRows(viewModel.page.value?.viewCalculations))
    }

    // --- Nel database ---

    @Test
    fun deletingARowMovesItsSubRowsOneLevelUp() = runBlocking<Unit> {
        val jam = row("Marmellata", 0)
        val apricot = row("Albicocca", 1, jam.id)
        val organic = row("Bio", 2, apricot.id)
        listOf(jam, apricot, organic).forEach { db.databaseDao().insertRow(it) }
        rows.deleteRow(apricot)
        assertEquals(jam.id, saved().single { it.title == "Bio" }.parentRowId)
        rows.deleteRow(jam)
        assertNull(saved().single().parentRowId)
    }

    @Test
    fun aCopiedDatabaseKeepsItsSubRowsUnderTheCopiedRows() = runBlocking<Unit> {
        val jam = row("Marmellata", 0)
        listOf(jam, row("Albicocca", 1, jam.id)).forEach { db.databaseDao().insertRow(it) }
        val copyId = pages.duplicatePage(food.id, DuplicateTarget.NextToOriginal, "copy") { it }!!
        val copied = db.databaseDao().getRowsForPageOnce(copyId)
        val copiedJam = copied.single { it.title == "Marmellata" }
        assertEquals(copiedJam.id, copied.single { it.title == "Albicocca" }.parentRowId)
    }

    @Test
    fun createSubRowOpensTheParentTakesItsGroupAndAsksForAName() = runBlocking<Unit> {
        val jam = row("Marmellata", 0, collapsed = true)
        db.databaseDao().insertRow(jam)
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = jam.id, columnId = shelf.id, value = "Dolci"))
        viewModel.load(food.id)
        waitFor("la riga") { viewModel.tableState.value.cellValues[jam.id to shelf.id] == "Dolci" }
        viewModel.addSubRow(viewModel.tableState.value.rows.single(), inherit = listOf(shelf.id))
        waitFor("la sotto-riga") { saved().size == 2 && viewModel.editTitleOf.value != null }
        val sub = saved().single { it.id != jam.id }
        assertEquals(jam.id, sub.parentRowId)
        assertEquals(sub.id, viewModel.editTitleOf.value)
        assertFalse(saved().single { it.id == jam.id }.subRowsCollapsed)
        waitFor("il gruppo della riga") { viewModel.tableState.value.cellValues[sub.id to shelf.id] == "Dolci" }
        viewModel.consumeEditTitle()
        assertNull(viewModel.editTitleOf.value)
    }

    @Test
    fun theArrowOpensAndClosesWithoutTouchingLastEdited() = runBlocking<Unit> {
        val jam = row("Marmellata", 0)
        db.databaseDao().insertRow(jam)
        val before = saved().single().updatedAt
        rows.setSubRowsCollapsed(jam.id, true)
        assertTrue(saved().single().subRowsCollapsed)
        assertEquals(before, saved().single().updatedAt)
    }
}
