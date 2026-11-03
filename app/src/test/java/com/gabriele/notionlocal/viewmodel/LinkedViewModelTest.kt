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
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Una **vista collegata** vista dal ViewModel del database, su un database
 * vero: mostra le righe dell'origine con il suo filtro, nasconde le
 * proprietà solo per sé, e scrive le righe nuove nell'origine — già col
 * valore del filtro, così non spariscono appena nate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LinkedViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: DatabaseViewModel

    private val games = PageEntity(title = "Giochi", isDatabase = true)
    private val platform = DatabaseColumnEntity(pageId = games.id, name = "Platform", type = ColumnType.SELECT)
    private val zelda = DatabaseRowEntity(pageId = games.id, title = "Zelda", orderIndex = 0)
    private val halo = DatabaseRowEntity(pageId = games.id, title = "Halo", orderIndex = 1)
    private lateinit var viewId: String

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(games)
        db.databaseDao().insertColumn(platform)
        db.databaseDao().insertRow(zelda)
        db.databaseDao().insertRow(halo)
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = zelda.id, columnId = platform.id, value = "PS5"))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = halo.id, columnId = platform.id, value = "XBOX"))

        viewId = pages.createLinkedView(games.id)!!
        // "Nella pagina PS5 vedrò solo i giochi della PS5".
        db.pageDao().update(db.pageDao().getById(viewId)!!.copy(filterColumnId = platform.id, filterValue = "PS5"))

        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(viewId)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Fa girare il thread principale finché la condizione non è vera: i dati di Room arrivano da un altro thread. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    private fun titles() = viewModel.tableState.value.rows.map { it.title }

    @Test
    fun showsTheSourceRowsThroughItsOwnFilter() {
        waitFor("solo Zelda") { titles() == listOf("Zelda") }
        assertEquals(games.id, viewModel.dataPage.value?.id)
        assertEquals(viewId, viewModel.page.value?.id)
    }

    @Test
    fun hidingAPropertyHidesItOnlyInTheView() = runBlocking<Unit> {
        waitFor("righe caricate") { titles() == listOf("Zelda") }
        viewModel.setColumnHidden(platform.id, true)
        waitFor("colonna nascosta nella vista") { viewModel.tableState.value.visibleColumns.isEmpty() }
        assertFalse(db.databaseDao().getColumnsForPageOnce(games.id).single().hidden)

        // E spostarla o rinominarla non porta il "nascosto" della vista nell'origine.
        viewModel.updateColumn(platform.id, "Piattaforma", ColumnType.SELECT, platform.optionsJson)
        waitFor("rinominata") { viewModel.tableState.value.columns.single().name == "Piattaforma" }
        assertFalse(db.databaseDao().getColumnsForPageOnce(games.id).single().hidden)
    }

    @Test
    fun aNewRowGoesToTheSourceWithTheFilterValue() = runBlocking<Unit> {
        waitFor("righe caricate") { titles() == listOf("Zelda") }
        viewModel.addRow("Bloodborne")
        waitFor("la riga nuova si vede") { "Bloodborne" in titles() }

        val created = db.databaseDao().getRowsForPageOnce(games.id).single { it.title == "Bloodborne" }
        assertEquals("PS5", db.databaseDao().getCellsForRowOnce(created.id).single().value)
        // Nessuna riga nella vista: stanno tutte nell'origine.
        assertEquals(0, db.databaseDao().getRowsForPageOnce(viewId).size)
        // Numero d'ordine dopo **tutte** le righe, anche quella che il filtro nasconde.
        assertEquals(2, created.orderIndex)
    }

    @Test
    fun theSourceBeingTrashedIsNoticed() = runBlocking<Unit> {
        waitFor("righe caricate") { titles() == listOf("Zelda") }
        pages.moveToTrash(games.id)
        waitFor("origine nel cestino") { viewModel.sourceMissing.value }
    }
}
