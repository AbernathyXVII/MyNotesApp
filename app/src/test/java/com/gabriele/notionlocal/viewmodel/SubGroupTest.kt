package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
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

/** **I sottogruppi** della tabella: gruppi dentro i gruppi (29/09/2026). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SubGroupTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel

    private val shopping = PageEntity(title = "Spesa", isDatabase = true)
    private val shop = DatabaseColumnEntity(pageId = shopping.id, name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0)
    private val kind = DatabaseColumnEntity(pageId = shopping.id, name = "Tipo", type = ColumnType.SELECT, orderIndex = 1)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(shopping)
        db.databaseDao().insertColumn(shop)
        db.databaseDao().insertColumn(kind)
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(shopping.id)
        waitFor("le colonne") { viewModel.tableState.value.columns.size == 2 && viewModel.page.value != null }
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

    private fun saved() = runBlocking { db.pageDao().getById(shopping.id)!! }

    @Test
    fun subGroupsNeedAGroupAndAreSaved() {
        // Senza raggruppamento i sottogruppi non valgono.
        viewModel.setTableSubGroupColumn(kind.id)
        assertNull(viewModel.tableSubGroupColumn())
        viewModel.setTableSubGroupColumn(null)
        viewModel.setTableGroupColumn(shop.id)
        assertNull(viewModel.tableSubGroupColumn())
        viewModel.setTableSubGroupColumn(kind.id)
        assertEquals(kind.id, viewModel.tableSubGroupColumn()?.id)
        waitFor("salvati") { saved().tableSubGroupColumnId == kind.id }
    }

    @Test
    fun removingTheGroupRemovesTheSubGroups() {
        viewModel.setTableGroupColumn(shop.id)
        viewModel.setTableSubGroupColumn(kind.id)
        viewModel.setTableGroupColumn(null)
        assertNull(viewModel.tableSubGroupColumn())
        waitFor("tolti anche nel database") { saved().let { it.tableGroupColumnId == null && it.tableSubGroupColumnId == null } }
    }

    @Test
    fun groupingByTheSubGroupPropertyRemovesTheSubGroups() {
        viewModel.setTableGroupColumn(shop.id)
        viewModel.setTableSubGroupColumn(kind.id)
        viewModel.setTableGroupColumn(kind.id)
        assertNull(viewModel.tableSubGroupColumn())
    }

    @Test
    fun theTwoEmptySwitchesAreSeparate() {
        viewModel.setTableGroupColumn(shop.id)
        viewModel.setTableSubGroupColumn(kind.id)
        viewModel.setHideEmptySubGroups(false)
        waitFor("salvato") { saved().let { !it.hideEmptySubGroups && it.hideEmptyGroups } }
    }

    @Test
    fun aRowAddedInASubGroupGetsBothValues() {
        viewModel.setTableGroupColumn(shop.id)
        viewModel.setTableSubGroupColumn(kind.id)
        viewModel.addRowInSubGroup(shop.id, "IPERPOLI", kind.id, "Cibo")
        waitFor("la riga nuova coi due valori") {
            val state = viewModel.tableState.value
            val row = state.rows.singleOrNull() ?: return@waitFor false
            state.cellValues[row.id to shop.id] == "IPERPOLI" && state.cellValues[row.id to kind.id] == "Cibo"
        }
    }
}
