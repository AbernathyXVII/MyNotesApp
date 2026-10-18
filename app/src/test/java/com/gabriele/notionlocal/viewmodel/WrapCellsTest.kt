package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.CellOverflow
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DEFAULT_IMAGE_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.MIN_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.MIN_NAME_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.MIN_WRITABLE_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.fitMinColumnWidth
import com.gabriele.notionlocal.data.entity.minColumnWidth
import com.gabriele.notionlocal.data.entity.parseColumnWidths
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.screen.shownColumnWidth
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
 * **Il testo lungo nelle celle e la larghezza minima** (30/09/2026): i tre
 * modi di ogni tabella, e le colonne in cui si scrive che non vanno sotto un minimo.
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
    fun theModeIsSavedWithTheView() {
        assertEquals(CellOverflow.TRUNCATE, CellOverflow.of(saved().cellOverflow))
        viewModel.setCellOverflow(CellOverflow.WIDEN)
        waitFor("allarga") { CellOverflow.of(saved().cellOverflow) == CellOverflow.WIDEN }
        viewModel.setCellOverflow(CellOverflow.WRAP)
        waitFor("a capo") { CellOverflow.of(saved().cellOverflow) == CellOverflow.WRAP }
        viewModel.setCellOverflow(CellOverflow.TRUNCATE)
        waitFor("i puntini") { saved().cellOverflow == 0 }
    }

    @Test
    fun colorCardsIsSavedWithTheView() {
        // "Color cards" della bacheca (30/09/2026): di suo spento.
        assertEquals(false, saved().colorBoardCards)
        viewModel.setColorBoardCards(true)
        waitFor("acceso") { saved().colorBoardCards }
        viewModel.setColorBoardCards(false)
        waitFor("spento") { !saved().colorBoardCards }
    }

    @Test
    fun theModesKeepTheNumbersOfTheOldSwitch() {
        // La colonna era sì/no: 0 "no" (i puntini), 1 "sì" (a capo).
        assertEquals(CellOverflow.TRUNCATE, CellOverflow.of(0))
        assertEquals(CellOverflow.WRAP, CellOverflow.of(1))
        assertEquals(CellOverflow.WIDEN, CellOverflow.of(2))
        assertEquals(CellOverflow.TRUNCATE, CellOverflow.of(99))
        // Nell'ordine chiesto: puntini, allarga, a capo.
        assertEquals(listOf(CellOverflow.TRUNCATE, CellOverflow.WIDEN, CellOverflow.WRAP), CellOverflow.entries)
    }

    @Test
    fun widenGrowsTheColumnButNeverShrinksIt() {
        // Il contenuto più largo: la colonna lo segue.
        assertEquals(230.dp, shownColumnWidth(150.dp, 230.dp, widen = true, min = 80.dp))
        // Il contenuto più stretto: resta com'era.
        assertEquals(150.dp, shownColumnWidth(150.dp, 60.dp, widen = true, min = 80.dp))
        // Negli altri modi non conta, e il minimo vale sempre.
        assertEquals(150.dp, shownColumnWidth(150.dp, 230.dp, widen = false, min = 80.dp))
        assertEquals(80.dp, shownColumnWidth(48.dp, null, widen = false, min = 80.dp))
    }

    @Test
    fun theMinimumWidthsOfFitToContent() {
        // A mano si stringe fino a niente (dal 18/10/2026); i minimi restano per "Fit to content".
        assertEquals(0, minColumnWidth(NAME_COLUMN_KEY))
        assertEquals(0, minColumnWidth("c", ColumnType.TEXT))
        assertEquals(MIN_NAME_COLUMN_WIDTH_DP, fitMinColumnWidth(NAME_COLUMN_KEY))
        listOf(ColumnType.TEXT, ColumnType.NUMBER, ColumnType.URL, ColumnType.EMAIL, ColumnType.PHONE, ColumnType.COLOR).forEach {
            assertEquals(it.name, MIN_WRITABLE_COLUMN_WIDTH_DP, fitMinColumnWidth("c", it))
        }
        assertEquals(MIN_COLUMN_WIDTH_DP, fitMinColumnWidth("c", ColumnType.CHECKBOX))
        // "Image" resta quadrata: il minimo non la allarga.
        assertEquals(DEFAULT_IMAGE_COLUMN_WIDTH_DP, fitMinColumnWidth("c", ColumnType.IMAGE))
    }

    @Test
    fun anyColumnShrinksAsMuchAsYouWant() {
        // Dal 18/10/2026, chiesto dall'utente: niente minimo trascinando.
        viewModel.setColumnWidth(notes.id, 10)
        waitFor("il testo a 10") { parseColumnWidths(saved().columnWidths)[notes.id] == 10 }
        viewModel.setColumnWidth(NAME_COLUMN_KEY, 0)
        waitFor("Name a niente") { parseColumnWidths(saved().columnWidths)[NAME_COLUMN_KEY] == 0 }
        viewModel.setColumnWidth(done.id, 3)
        waitFor("la casella a 3") { parseColumnWidths(saved().columnWidths)[done.id] == 3 }
    }
}
