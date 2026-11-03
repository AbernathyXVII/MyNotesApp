package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.RollupCalc
import com.gabriele.notionlocal.data.entity.RollupConfig
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import com.gabriele.notionlocal.data.entity.encodeRollupConfig
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.databaseViews
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
 * **Le viste con le righe di un altro database** (21/10/2026, chiesto
 * dall'utente: i tre database K-pop in uno, con tre viste). Come le "data
 * sources" di Notion: la vista sta in un database ma mostra le righe, le
 * proprietà e i calcoli di un altro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ViewSourceTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var repository: DatabaseRepository

    private val wins = PageEntity(title = "KPOP Music Shows", isDatabase = true, isSimpleDatabase = true)
    private val groups = PageEntity(title = "KPOP Groups", isDatabase = true, isSimpleDatabase = true)
    private val ive = DatabaseRowEntity(pageId = groups.id, title = "IVE", orderIndex = 0)
    private val win = DatabaseRowEntity(pageId = wins.id, title = "I AM", orderIndex = 0)
    private val winGroup = DatabaseColumnEntity(
        pageId = wins.id, name = "Group", type = ColumnType.RELATION,
        optionsJson = encodeRelationConfig(RelationConfig(database = groups.id))
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        repository = DatabaseRepository(db)
        pages.createPage(wins)
        pages.createPage(groups)
        db.databaseDao().insertRow(ive)
        db.databaseDao().insertRow(win)
        repository.addRelationColumn(winGroup, twoWayName = "Wins")
        repository.setRelation(win.id, db.databaseDao().getColumnById(winGroup.id)!!, ive.id)
        db.databaseDao().insertColumn(
            DatabaseColumnEntity(
                pageId = groups.id, name = "Number of wins", type = ColumnType.ROLLUP, orderIndex = 1,
                optionsJson = encodeRollupConfig(
                    RollupConfig(relation = db.databaseDao().getColumnById(winGroup.id)!!.relationConfig()!!.reverse!!, property = SORT_BY_NAME, calc = RollupCalc.COUNT_ALL)
                )
            )
        )
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

    @Test
    fun aViewShowsTheRowsPropertiesAndRollupsOfItsSource() = runBlocking<Unit> {
        val viewId = pages.createView(wins.id, null, DatabaseLayout.TABLE, groups.id)!!
        val view = db.pageDao().getById(viewId)!!
        // Sta nel database delle vittorie, mostra i gruppi, e senza nome prende il loro.
        assertEquals(wins.id, view.viewHostId)
        assertEquals(groups.id, view.sourceDatabaseId)
        assertEquals("KPOP Groups", view.viewName)
        val viewModel = DatabaseViewModel(repository, pages)
        viewModel.load(viewId)
        val count = db.databaseDao().getColumnsForPageOnce(groups.id).single { it.type == ColumnType.ROLLUP }
        waitFor("i gruppi col loro conto") {
            val state = viewModel.tableState.value
            state.rows.map { it.title } == listOf("IVE") && state.cellValues[ive.id to count.id] == "1"
        }
        // I collegamenti della vista sono quelli dei gruppi: le vittorie.
        waitFor("le vittorie collegate") { viewModel.related.value[wins.id]?.rows?.map { it.title } == listOf("I AM") }
        // Una riga nuova da questa vista è un gruppo.
        viewModel.addRow()
        waitFor("il gruppo nuovo") { runBlocking { db.databaseDao().getRowsForPageOnce(groups.id).size } == 2 }
        assertEquals(1, db.databaseDao().getRowsForPageOnce(wins.id).size)
    }

    @Test
    fun theStartingViewGivesWayOnlyToAViewWithTheSameRows() = runBlocking<Unit> {
        pages.createView(wins.id, "Groups", DatabaseLayout.TABLE, groups.id)
        // Solo una vista di un altro database: la vista di partenza non si toglie.
        assertFalse(databaseViews(db.pageDao().getById(wins.id)!!, db.pageDao().getViewsOnce(wins.id)).first().deletable)
        assertFalse(pages.deleteStartingView(wins.id))
        val board = pages.createView(wins.id, "Board", DatabaseLayout.BOARD)!!
        db.pageDao().update(db.pageDao().getById(board)!!.copy(boardGroupRulesJson = """[{"column":"x","dateBy":{"unit":"WEEK"}}]"""))
        assertTrue(databaseViews(db.pageDao().getById(wins.id)!!, db.pageDao().getViewsOnce(wins.id)).first().deletable)
        // Con una vista delle stesse righe: il database prende la sua bacheca, anche le settimane.
        assertTrue(pages.deleteStartingView(wins.id))
        val host = db.pageDao().getById(wins.id)!!
        assertEquals(DatabaseLayout.BOARD, host.databaseLayout)
        assertEquals("""[{"column":"x","dateBy":{"unit":"WEEK"}}]""", host.boardGroupRulesJson)
        assertEquals(listOf("Groups"), db.pageDao().getViewsOnce(wins.id).map { it.viewName })
        assertNull(db.pageDao().getById(board))
    }

    @Test
    fun aLinkedViewOrATrashedDatabaseIsNotASource() = runBlocking<Unit> {
        val linked = PageEntity(title = "Vista collegata", isDatabase = true, sourceDatabaseId = groups.id)
        pages.createPage(linked)
        assertNull(pages.createView(wins.id, null, DatabaseLayout.TABLE, linked.id))
        db.pageDao().update(groups.copy(trashedAt = 1L))
        assertNull(pages.createView(wins.id, null, DatabaseLayout.TABLE, groups.id))
        // La propria sorgente è come non sceglierne una.
        val own = pages.createView(wins.id, null, DatabaseLayout.TABLE, wins.id)!!
        assertEquals(wins.id, db.pageDao().getById(own)!!.sourceDatabaseId)
        assertNull(db.pageDao().getById(own)!!.viewName)
    }
}
