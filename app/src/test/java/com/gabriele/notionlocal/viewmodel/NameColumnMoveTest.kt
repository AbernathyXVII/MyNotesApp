package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.namePosition
import com.gabriele.notionlocal.data.entity.tableKeys
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **La colonna Name si sposta** (29/09/2026): a sinistra e a destra come le
 * altre, fra quelle che si vedono, e resta dov'è per tutte le viste.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NameColumnMoveTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: DatabaseViewModel

    private val home = PageEntity(title = "Pagina")
    private val games = PageEntity(title = "Giochi", isDatabase = true)
    private val platform = DatabaseColumnEntity(pageId = games.id, name = "Platform", type = ColumnType.SELECT, orderIndex = 0)
    private val year = DatabaseColumnEntity(pageId = games.id, name = "Year", type = ColumnType.NUMBER, orderIndex = 1)
    private val notes = DatabaseColumnEntity(pageId = games.id, name = "Notes", type = ColumnType.TEXT, orderIndex = 2)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(home)
        pages.createPage(games)
        db.blockDao().insertAll(
            listOf(BlockEntity(pageId = home.id, type = BlockType.DATABASE_LINK, linkedPageId = games.id, orderIndex = 0))
        )
        listOf(platform, year, notes).forEach { db.databaseDao().insertColumn(it) }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(games.id)
        waitFor("il database") { viewModel.dataPage.value != null && viewModel.tableState.value.columns.size == 3 }
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

    /** La tabella come si vede, da sinistra a destra, per nome. */
    private fun shown(): List<String> {
        val state = viewModel.tableState.value
        val nameOrder = viewModel.dataPage.value!!.nameColumnOrder
        return tableKeys(state.visibleColumns, nameOrder).map { key ->
            if (key == NAME_COLUMN_KEY) "Name" else state.columns.first { it.id == key }.name
        }
    }

    /** Spostata, salvata e tornata al ViewModel: il passo dopo parte da lì, come col dito. */
    private fun move(key: String, delta: Int, expected: List<String>) {
        viewModel.moveColumn(key, delta)
        waitFor("$expected") { shown() == expected }
    }

    @Test
    fun byDefaultNameIsFirst() {
        assertEquals(0, namePosition(listOf(platform, year, notes), -1))
        assertEquals(listOf("Name", "Platform", "Year", "Notes"), shown())
    }

    @Test
    fun whereNameSits() {
        val columns = listOf(platform, year, notes)
        // Subito prima della prima colonna col numero uguale o più alto.
        assertEquals(1, namePosition(columns, 1))
        assertEquals(3, namePosition(columns, 3))
        assertEquals(3, namePosition(columns, 99))
        // Con un buco nei numeri (una colonna eliminata) resta fra le stesse due.
        assertEquals(1, namePosition(listOf(platform, notes), 1))
    }

    @Test
    fun nameGoesRightAllTheWayAndBack() {
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Name", "Year", "Notes"))
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Year", "Name", "Notes"))
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Year", "Notes", "Name"))
        // In fondo non va più in là.
        viewModel.moveColumn(NAME_COLUMN_KEY, 1)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("Platform", "Year", "Notes", "Name"), shown())
        move(NAME_COLUMN_KEY, -1, listOf("Platform", "Year", "Name", "Notes"))
        move(NAME_COLUMN_KEY, -1, listOf("Platform", "Name", "Year", "Notes"))
        move(NAME_COLUMN_KEY, -1, listOf("Name", "Platform", "Year", "Notes"))
        // Tornata in testa è come se non si fosse mai mossa, e le colonne hanno i loro numeri di sempre.
        val saved = runBlocking { db.pageDao().getById(games.id)!! }
        assertEquals(-1, saved.nameColumnOrder)
        assertEquals(listOf(0, 1, 2), runBlocking { db.databaseDao().getColumnsForPageOnce(games.id) }.map { it.orderIndex })
    }

    @Test
    fun theColumnsGoPastName() {
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Name", "Year", "Notes"))
        move(year.id, -1, listOf("Platform", "Year", "Name", "Notes"))
        move(platform.id, 1, listOf("Year", "Platform", "Name", "Notes"))
        move(notes.id, -1, listOf("Year", "Platform", "Notes", "Name"))
    }

    @Test
    fun aHiddenColumnIsSkipped() {
        viewModel.setColumnHidden(platform.id, true)
        waitFor("nascosta") { viewModel.tableState.value.visibleColumns.size == 2 }
        // Name salta la nascosta: si vede subito spostarsi dopo Year.
        move(NAME_COLUMN_KEY, 1, listOf("Year", "Name", "Notes"))
        viewModel.setColumnHidden(platform.id, false)
        waitFor("di nuovo visibile") { shown() == listOf("Platform", "Year", "Name", "Notes") }
    }

    @Test
    fun aNewPropertyStillArrivesAtTheFarRight() {
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Name", "Year", "Notes"))
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Year", "Name", "Notes"))
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Year", "Notes", "Name"))
        viewModel.addColumn("Genre", ColumnType.TEXT)
        waitFor("la nuova in fondo") { shown() == listOf("Platform", "Year", "Notes", "Name", "Genre") }
    }

    @Test
    fun aLinkedViewSeesAndMovesTheSameName() = runBlocking<Unit> {
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Name", "Year", "Notes"))
        val view = PageEntity(title = "Vista", isDatabase = true, sourceDatabaseId = games.id)
        pages.createPage(view)
        val other = DatabaseViewModel(DatabaseRepository(db), pages)
        other.load(view.id)
        waitFor("la vista") { other.dataPage.value?.id == games.id && other.tableState.value.columns.size == 3 }
        assertEquals(1, namePosition(other.tableState.value.visibleColumns, other.dataPage.value!!.nameColumnOrder))
        // Spostata dalla vista, si sposta nel database; la vista resta com'era.
        other.moveColumn(NAME_COLUMN_KEY, 1)
        waitFor("spostata dalla vista") { shown() == listOf("Platform", "Year", "Name", "Notes") }
        assertEquals(-1, db.pageDao().getById(view.id)!!.nameColumnOrder)
    }

    @Test
    fun aCopiedDatabaseKeepsNameWhereItWas() = runBlocking<Unit> {
        move(NAME_COLUMN_KEY, 1, listOf("Platform", "Name", "Year", "Notes"))
        val copyId = pages.duplicatePage(games.id, DuplicateTarget.NextToOriginal, "copy") { it }!!
        val copy = db.pageDao().getById(copyId)!!
        val columns = db.databaseDao().getColumnsForPageOnce(copyId)
        assertEquals(listOf("Platform", "Year", "Notes"), columns.map { it.name })
        assertEquals(1, namePosition(columns, copy.nameColumnOrder))
    }

    @Test
    fun aHiddenColumnMovesAmongAll() {
        viewModel.setColumnHidden(year.id, true)
        waitFor("nascosta") { viewModel.tableState.value.visibleColumns.size == 2 }
        val order = viewModel.tableState.value.orderAfterMove(year.id, -1, -1)
        assertEquals(listOf(NAME_COLUMN_KEY, year.id, platform.id, notes.id), order)
        // A sinistra di Name, in testa, non c'è niente.
        assertNull(viewModel.tableState.value.orderAfterMove(NAME_COLUMN_KEY, -1, -1))
    }
}
