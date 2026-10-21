package com.gabriele.notionlocal

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DateUnit
import com.gabriele.notionlocal.data.entity.GroupSlot
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.parseDateGroupings
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationIds
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.viewmodel.DatabaseViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * **L'importazione delle vittorie K-pop** (21/10/2026, solo debug), su una
 * copia del "KPOP Music Shows" dell'utente con gli stessi id: prima di
 * farla sul telefono, qui si guarda che crei i database collegati giusti e
 * che i Rollup contino le vittorie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class KpopImportTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase

    private val kpop = PageEntity(id = "7c1561e2-ee8e-41b4-b983-110bc8f31e69", title = "KPOP")
    private val wins = PageEntity(id = "7b7d8ff1-a788-4779-8792-62ed053117b1", title = "KPOP Music Shows", isDatabase = true, isSimpleDatabase = true)
    private val board = PageEntity(
        id = "c510310e-0169-4a4f-ad03-cb04bd1fc8b1", title = "KPOP Music Shows", isDatabase = true,
        databaseLayout = DatabaseLayout.BOARD, viewHostId = wins.id, sourceDatabaseId = wins.id
    )
    private val group = DatabaseColumnEntity(
        pageId = wins.id, name = "Group", type = ColumnType.MULTI_SELECT, orderIndex = 0,
        optionsJson = """[{"label":"IVE"},{"label":"aespa","color":"#765B48"}]"""
    )
    private val show = DatabaseColumnEntity(
        pageId = wins.id, name = "Music show", type = ColumnType.SELECT, orderIndex = 1,
        optionsJson = """[{"label":"Show KPOP (Monday)"},{"label":"The Show (Tuesday)"},{"label":"Show Champion (Wednesday)"},""" +
            """{"label":"M Countdown (Thursday)"},{"label":"Music Bank (Friday)"},{"label":"Show! Music Core (Saturday)"},{"label":"Inkigayo (Sunday)"}]"""
    )
    private val winDate = DatabaseColumnEntity(pageId = wins.id, name = "Win date", type = ColumnType.DATE, orderIndex = 2)
    private val album = DatabaseColumnEntity(pageId = wins.id, name = "Album", type = ColumnType.MULTI_SELECT, orderIndex = 3)
    private val release = DatabaseColumnEntity(pageId = wins.id, name = "Release date", type = ColumnType.TEXT, orderIndex = 4)
    private val emptyRow = DatabaseRowEntity(pageId = wins.id, title = "", orderIndex = 0)

    @Before
    fun setUp() = runBlocking<Unit> {
        context.getDatabasePath("notion_local.db").delete()
        db = AppDatabase.getInstance(context)
        listOf(kpop, wins, board).forEach { db.pageDao().insert(it) }
        db.blockDao().insert(BlockEntity(pageId = kpop.id, type = BlockType.DATABASE_LINK, linkedPageId = wins.id, orderIndex = 0))
        db.blockDao().insert(BlockEntity(pageId = kpop.id, type = BlockType.PARAGRAPH, orderIndex = 1))
        listOf(group, show, winDate, album, release).forEach { db.databaseDao().insertColumn(it) }
        db.databaseDao().insertRow(emptyRow)
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = emptyRow.id, columnId = show.id, value = ""))
        File(context.filesDir, "kpop_wins.json").writeText(
            """[
            {"date":"2024-05-17","show":"Music Bank","artist":"Aespa","artistSlug":"aespa","song":"Supernova","songId":1},
            {"date":"2024-05-19","show":"Inkigayo","artist":"Aespa","artistSlug":"aespa","song":"Supernova","songId":1},
            {"date":"2024-06-01","show":"Show! Music Core","artist":"Aespa","artistSlug":"aespa","song":"Armageddon","songId":2},
            {"date":"2023-04-14","show":"Music Bank","artist":"IVE","artistSlug":"IVE","song":"I AM","songId":3},
            {"date":"2026-10-14","show":"Show Champion","artist":"KickFlip","artistSlug":"KickFlip","song":"Kick Off The Wall","songId":4},
            {"date":"2026-10-17","show":"Show! Music Core","artist":"Onew + Zico","artistSlug":"Onew-Zico","song":"Tic Tac Toe","songId":5}
            ]"""
        )
    }

    @After
    fun tearDown() {
        AppDatabase.resetForTests()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(20)
        }
        fail("Non è successo: $what")
    }

    private fun runImport(): String {
        val log = File(context.filesDir, "kpop-import-log.txt").also { it.delete() }
        Robolectric.buildActivity(KpopImportActivity::class.java).setup()
        waitFor("il resoconto") { log.exists() }
        return log.readText()
    }

    @Test
    fun theWinsTheSongsAndTheGroupsAreLinked() = runBlocking<Unit> {
        val report = runImport()
        assertTrue(report, report.startsWith("OK"))
        val dao = db.databaseDao()
        val songsDb = db.pageDao().getLinkableDatabases().single { it.title == "KPOP Songs" }
        val groupsDb = db.pageDao().getLinkableDatabases().single { it.title == "KPOP Groups" }

        // Le vittorie dopo la riga vuota che c'era, dalla più vecchia; i tag dell'utente restano i suoi.
        val winRows = dao.getRowsForPageOnce(wins.id).sortedBy { it.orderIndex }
        assertEquals(listOf("", "I AM", "Supernova", "Supernova", "Armageddon", "Kick Off The Wall", "Tic Tac Toe"), winRows.map { it.title })
        val cells = dao.getAllCellsForPageOnce(wins.id).associate { (it.rowId to it.columnId) to it.value }
        val supernova = winRows[2]
        assertEquals("aespa", cells[supernova.id to group.id])
        assertEquals("Music Bank (Friday)", cells[supernova.id to show.id])
        val tags = db.databaseDao().getColumnsForPageOnce(wins.id).single { it.id == group.id }.optionsJson
        assertTrue(tags, tags.contains("\"KickFlip\"") && tags.contains("\"Onew + Zico\"") && !tags.contains("\"Aespa\""))

        // Le Relation nei due sensi.
        val winSong = dao.getColumnsForPageOnce(wins.id).single { it.type == ColumnType.RELATION }
        assertEquals(songsDb.id, winSong.relationConfig()!!.database)
        val songRows = dao.getRowsForPageOnce(songsDb.id)
        val supernovaSong = songRows.single { it.title == "Supernova" }
        assertEquals(supernovaSong.id, cells[supernova.id to winSong.id])
        val songWins = dao.getColumnById(winSong.relationConfig()!!.reverse!!)!!
        assertEquals(listOf(winRows[2].id, winRows[3].id), relationIds(dao.getCell(supernovaSong.id, songWins.id)!!.value))
        assertEquals(listOf("aespa", "IVE", "KickFlip", "Onew + Zico"), dao.getRowsForPageOnce(groupsDb.id).sortedBy { it.orderIndex }.map { it.title }.sortedBy { it.lowercase() })

        // Una volta sola.
        assertTrue(runImport().startsWith("ERRORE"))
        assertEquals(7, dao.getRowsForPageOnce(wins.id).size)

        // La bacheca: fasce per settimana, dalla più recente; la Relation non si ripete sulle schede.
        val view = db.pageDao().getById(board.id)!!
        assertEquals(winDate.id, view.boardSubGroupColumnId)
        assertEquals(DateUnit.WEEK, parseDateGroupings(view.dateGroupingJson)[GroupSlot.BOARD_SUB].unit)
        assertTrue(parseDateGroupings(view.dateGroupingJson)[GroupSlot.BOARD_SUB].newestFirst)
        assertTrue(view.viewHiddenColumnIds!!.split(",").containsAll(listOf(winSong.id, album.id, release.id)))
    }

    @Test
    fun theRollupsCountTheWins() = runBlocking<Unit> {
        runImport()
        val pages = PageRepository(db)
        val groupsDb = db.pageDao().getLinkableDatabases().single { it.title == "KPOP Groups" }
        val songsDb = db.pageDao().getLinkableDatabases().single { it.title == "KPOP Songs" }

        // I gruppi dal più premiato: aespa 3 vittorie con 2 canzoni.
        val groups = DatabaseViewModel(DatabaseRepository(db), pages)
        groups.load(groupsDb.id)
        val wins = db.databaseDao().getColumnsForPageOnce(groupsDb.id).single { it.name == "Number of wins" }
        val songs = db.databaseDao().getColumnsForPageOnce(groupsDb.id).single { it.name == "Winning songs" }
        waitFor("i conti dei gruppi") {
            val state = groups.tableState.value
            state.rows.firstOrNull()?.title == "aespa" && state.cellValues[state.rows.first().id to wins.id] == "3"
        }
        val aespa = groups.tableState.value.rows.first()
        assertEquals("2", groups.tableState.value.cellValues[aespa.id to songs.id])

        // Le canzoni: la prima e l'ultima vittoria, e i programmi.
        val songsVm = DatabaseViewModel(DatabaseRepository(db), pages)
        songsVm.load(songsDb.id)
        val shows = db.databaseDao().getColumnsForPageOnce(songsDb.id).single { it.name == "Music shows" }
        waitFor("i programmi di Supernova") {
            val state = songsVm.tableState.value
            val supernova = state.rows.firstOrNull { it.title == "Supernova" } ?: return@waitFor false
            state.cellValues[supernova.id to shows.id] == "Music Bank (Friday), Inkigayo (Sunday)"
        }
        assertEquals("Supernova", songsVm.tableState.value.rows.first().title)
    }
}
