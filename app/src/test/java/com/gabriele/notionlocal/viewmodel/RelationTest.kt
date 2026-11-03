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
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.RollupCalc
import com.gabriele.notionlocal.data.entity.RollupConfig
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import com.gabriele.notionlocal.data.entity.encodeRollupConfig
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationIds
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
 * **"Relation" e "Rollup"** (21/10/2026) con il database vero: le due metà
 * di una Relation nei due sensi restano allineate, il Rollup segue le righe
 * collegate anche a due passi, e la copia di una riga o di un database si
 * porta dietro i collegamenti giusti. L'esempio è quello dell'utente:
 * gruppi K-pop, canzoni, vittorie ai music show.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RelationTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var repository: DatabaseRepository
    private lateinit var viewModel: DatabaseViewModel

    private val groups = PageEntity(title = "Groups", isDatabase = true)
    private val songs = PageEntity(title = "Songs", isDatabase = true)
    private val wins = PageEntity(title = "KPOP Music Shows", isDatabase = true)
    private val ive = DatabaseRowEntity(pageId = groups.id, title = "IVE", orderIndex = 0)
    private val aespa = DatabaseRowEntity(pageId = groups.id, title = "aespa", orderIndex = 1)
    private val iAm = DatabaseRowEntity(pageId = songs.id, title = "I AM", orderIndex = 0)
    private val supernova = DatabaseRowEntity(pageId = songs.id, title = "Supernova", orderIndex = 1)
    private val musicBank = DatabaseRowEntity(pageId = wins.id, title = "Music Bank", orderIndex = 0)
    private val inkigayo = DatabaseRowEntity(pageId = wins.id, title = "Inkigayo", orderIndex = 1)

    private val songGroup = DatabaseColumnEntity(
        pageId = songs.id, name = "Group", type = ColumnType.RELATION,
        optionsJson = encodeRelationConfig(RelationConfig(database = groups.id))
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        repository = DatabaseRepository(db)
        listOf(groups, songs, wins).forEach { pages.createPage(it) }
        listOf(ive, aespa, iAm, supernova, musicBank, inkigayo).forEach { db.databaseDao().insertRow(it) }
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

    private suspend fun column(id: String) = db.databaseDao().getColumnById(id)!!
    private suspend fun cell(rowId: String, columnId: String) = db.databaseDao().getCell(rowId, columnId)?.value.orEmpty()
    private suspend fun twinOf(column: DatabaseColumnEntity) = column(column.id).relationConfig()?.reverse?.let { db.databaseDao().getColumnById(it) }

    @Test
    fun aTwoWayRelationHasItsTwinAndBothHalvesStayInSync() = runBlocking<Unit> {
        repository.addRelationColumn(songGroup, twoWayName = "Songs")
        val twin = twinOf(songGroup)!!
        assertEquals(groups.id, twin.pageId)
        assertEquals("Songs", twin.name)
        assertEquals(RelationConfig(database = songs.id, reverse = songGroup.id), twin.relationConfig())

        repository.setRelation(iAm.id, column(songGroup.id), ive.id)
        repository.setRelation(supernova.id, column(songGroup.id), aespa.id)
        assertEquals(iAm.id, cell(ive.id, twin.id))
        assertEquals(supernova.id, cell(aespa.id, twin.id))

        // I AM passa ad aespa: IVE la perde, aespa la guadagna.
        repository.setRelation(iAm.id, column(songGroup.id), aespa.id)
        assertEquals("", cell(ive.id, twin.id))
        assertEquals(listOf(supernova.id, iAm.id), relationIds(cell(aespa.id, twin.id)))

        // E dall'altra parte vale lo stesso: IVE riprende I AM.
        repository.setRelation(ive.id, twin, iAm.id)
        assertEquals(listOf(aespa.id, ive.id), relationIds(cell(iAm.id, songGroup.id)))
    }

    @Test
    fun twoWayCanBeTurnedOnRenamedAndOff() = runBlocking<Unit> {
        repository.addRelationColumn(songGroup, twoWayName = null)
        assertNull(twinOf(songGroup))
        repository.setRelation(iAm.id, column(songGroup.id), ive.id)
        repository.setRelation(supernova.id, column(songGroup.id), aespa.id)

        // Accesa: la gemella nasce già con le canzoni di ogni gruppo.
        repository.updateRelationColumn(column(songGroup.id), twoWayName = "Songs")
        val twin = twinOf(songGroup)!!
        assertEquals(iAm.id, cell(ive.id, twin.id))
        assertEquals(supernova.id, cell(aespa.id, twin.id))

        // Col nome nuovo, la stessa gemella rinominata.
        repository.updateRelationColumn(column(songGroup.id).copy(name = "Artist"), twoWayName = "Brani")
        assertEquals("Brani", twinOf(songGroup)!!.name)
        assertEquals(twin.id, twinOf(songGroup)!!.id)
        assertEquals("Artist", column(songGroup.id).name)

        // Spenta: la gemella se ne va, le celle di qua restano.
        repository.updateRelationColumn(column(songGroup.id), twoWayName = null)
        assertNull(twinOf(songGroup))
        assertNull(db.databaseDao().getColumnById(twin.id))
        assertEquals(ive.id, cell(iAm.id, songGroup.id))
    }

    @Test
    fun deletingARelationTakesItsTwin() = runBlocking<Unit> {
        repository.addRelationColumn(songGroup, twoWayName = "Songs")
        val twin = twinOf(songGroup)!!
        repository.deleteColumnWithTwin(column(songGroup.id))
        assertNull(db.databaseDao().getColumnById(songGroup.id))
        assertNull(db.databaseDao().getColumnById(twin.id))
    }

    @Test
    fun aDuplicatedRowIsLinkedFromTheOtherSideToo() = runBlocking<Unit> {
        repository.addRelationColumn(songGroup, twoWayName = "Songs")
        repository.setRelation(iAm.id, column(songGroup.id), ive.id)
        val rowPage = pages.ensureRowPage(iAm.id)!!
        pages.duplicatePage(rowPage, DuplicateTarget.NextToOriginal, "copy") { it }!!
        val copy = db.databaseDao().getRowsForPageOnce(songs.id).single { it.title == "I AM (copy)" }
        assertEquals(ive.id, cell(copy.id, songGroup.id))
        assertEquals(listOf(iAm.id, copy.id), relationIds(cell(ive.id, twinOf(songGroup)!!.id)))
    }

    @Test
    fun aCopiedDatabaseLinksItsOwnCopiedRows() = runBlocking<Unit> {
        // "Next single": le canzoni fra di loro, nei due sensi ("Previous single").
        val next = DatabaseColumnEntity(
            pageId = songs.id, name = "Next single", type = ColumnType.RELATION,
            optionsJson = encodeRelationConfig(RelationConfig(database = songs.id))
        )
        repository.addRelationColumn(next, twoWayName = "Previous single")
        repository.addRelationColumn(songGroup, twoWayName = "Songs")
        repository.setRelation(iAm.id, column(next.id), supernova.id)
        repository.setRelation(iAm.id, column(songGroup.id), ive.id)
        // Il database sta in una pagina: la copia gli nasce accanto.
        val home = PageEntity(title = "KPOP")
        pages.createPage(home)
        db.blockDao().insertAll(
            listOf(BlockEntity(pageId = home.id, type = BlockType.DATABASE_LINK, linkedPageId = songs.id, orderIndex = 0))
        )

        val copyId = pages.duplicatePage(songs.id, DuplicateTarget.NextToOriginal, "copy") { it }!!
        val columns = db.databaseDao().getColumnsForPageOnce(copyId)
        val rows = db.databaseDao().getRowsForPageOnce(copyId)
        val copiedNext = columns.single { it.name == "Next single" }
        val copiedPrevious = columns.single { it.name == "Previous single" }
        val copiedGroup = columns.single { it.name == "Group" }
        val copiedIAm = rows.single { it.title == "I AM" }
        val copiedSupernova = rows.single { it.title == "Supernova" }

        // Fra le righe della copia, con la gemella della copia.
        assertEquals(RelationConfig(database = copyId, reverse = copiedPrevious.id), copiedNext.relationConfig())
        assertEquals(copiedSupernova.id, cell(copiedIAm.id, copiedNext.id))
        assertEquals(copiedIAm.id, cell(copiedSupernova.id, copiedPrevious.id))
        // Verso i gruppi resta collegata, ma in un senso solo.
        assertEquals(RelationConfig(database = groups.id), copiedGroup.relationConfig())
        assertEquals(ive.id, cell(copiedIAm.id, copiedGroup.id))
        assertEquals(iAm.id, cell(ive.id, twinOf(songGroup)!!.id))
    }

    /** Le vittorie: la canzone (una Relation) e l'artista (un Rollup del gruppo della canzone). */
    private suspend fun winsWithArtist(): Triple<DatabaseColumnEntity, DatabaseColumnEntity, DatabaseColumnEntity> {
        repository.addRelationColumn(songGroup, twoWayName = null)
        repository.setRelation(iAm.id, column(songGroup.id), ive.id)
        repository.setRelation(supernova.id, column(songGroup.id), aespa.id)
        val song = DatabaseColumnEntity(
            pageId = wins.id, name = "Song", type = ColumnType.RELATION, orderIndex = 0,
            optionsJson = encodeRelationConfig(RelationConfig(database = songs.id))
        )
        val artist = DatabaseColumnEntity(
            pageId = wins.id, name = "Artist", type = ColumnType.ROLLUP, orderIndex = 1,
            optionsJson = encodeRollupConfig(RollupConfig(relation = song.id, property = songGroup.id))
        )
        val count = DatabaseColumnEntity(
            pageId = wins.id, name = "Songs", type = ColumnType.ROLLUP, orderIndex = 2,
            optionsJson = encodeRollupConfig(RollupConfig(relation = song.id, property = SORT_BY_NAME, calc = RollupCalc.COUNT_ALL))
        )
        repository.addRelationColumn(song, twoWayName = null)
        repository.addColumn(artist)
        repository.addColumn(count)
        return Triple(song, artist, count)
    }

    @Test
    fun theRollupReadsTwoStepsAwayAndFollowsTheChanges() = runBlocking<Unit> {
        val (song, artist, _) = winsWithArtist()
        viewModel.load(wins.id)
        waitFor("le righe") { viewModel.tableState.value.rows.size == 2 }
        val row = viewModel.tableState.value.rows.single { it.id == musicBank.id }
        viewModel.setCellValue(row, viewModel.tableState.value.columns.single { it.id == song.id }, supernova.id)
        waitFor("l'artista") { viewModel.tableState.value.cellValues[musicBank.id to artist.id] == "aespa" }
        // Supernova passa a IVE, in un altro database: il Rollup se ne accorge.
        repository.setRelation(supernova.id, column(songGroup.id), ive.id)
        waitFor("l'artista nuovo") { viewModel.tableState.value.cellValues[musicBank.id to artist.id] == "IVE" }
        // Rinominato il gruppo, anche il nome.
        db.databaseDao().updateRow(ive.copy(title = "IVE (아이브)"))
        waitFor("il nome nuovo") { viewModel.tableState.value.cellValues[musicBank.id to artist.id] == "IVE (아이브)" }
    }

    @Test
    fun rowsSortByTheLinkedNamesAndByTheRollupNumbers() = runBlocking<Unit> {
        val (song, _, count) = winsWithArtist()
        repository.setRelation(musicBank.id, column(song.id), supernova.id)
        repository.setRelation(inkigayo.id, column(song.id), "${iAm.id}|${supernova.id}")
        viewModel.load(wins.id)
        waitFor("i conti") { viewModel.tableState.value.cellValues[inkigayo.id to count.id] == "2" }
        // "I AM, Supernova" prima di "Supernova".
        viewModel.setSort(song.id, descending = false)
        waitFor("per canzone") { viewModel.tableState.value.rows.map { it.title } == listOf("Inkigayo", "Music Bank") }
        // 1 prima di 2.
        viewModel.setSort(count.id, descending = false)
        waitFor("per numero") { viewModel.tableState.value.rows.map { it.title } == listOf("Music Bank", "Inkigayo") }
    }

    @Test
    fun theSearchFindsTheLinkedNamesAndTheRollups() = runBlocking<Unit> {
        val (song, _, _) = winsWithArtist()
        repository.setRelation(musicBank.id, column(song.id), supernova.id)
        repository.setRelation(inkigayo.id, column(song.id), iAm.id)
        viewModel.load(wins.id)
        waitFor("le righe") { viewModel.tableState.value.rows.size == 2 }
        viewModel.setSearchQuery("supernova")
        waitFor("la canzone") { viewModel.tableState.value.rows.map { it.title } == listOf("Music Bank") }
        // "IVE" è scritto solo nel Rollup dell'artista.
        viewModel.setSearchQuery("ive")
        waitFor("il gruppo") { viewModel.tableState.value.rows.map { it.title } == listOf("Inkigayo") }
    }

    @Test
    fun fromTheScreenATwoWayRelationIsBornAndChanged() = runBlocking<Unit> {
        viewModel.load(songs.id)
        waitFor("le righe") { viewModel.tableState.value.rows.size == 2 }
        viewModel.addColumn("Group", ColumnType.RELATION, encodeRelationConfig(RelationConfig(database = groups.id)), twoWayName = "Songs")
        waitFor("la colonna") { viewModel.tableState.value.columns.any { it.type == ColumnType.RELATION } }
        val group = viewModel.tableState.value.columns.single { it.type == ColumnType.RELATION }
        val twinId = group.relationConfig()!!.reverse!!
        assertEquals("Songs", db.databaseDao().getColumnById(twinId)!!.name)

        val row = viewModel.tableState.value.rows.single { it.id == iAm.id }
        viewModel.setCellValue(row, group, ive.id)
        waitFor("la gemella") { runBlocking { cell(ive.id, twinId) } == iAm.id }
        // I nomi delle righe collegate arrivano alla schermata.
        waitFor("i gruppi") { viewModel.related.value[groups.id]?.rows?.map { it.title } == listOf("IVE", "aespa") }

        viewModel.updateRelationColumn(group.id, "Group", group.optionsJson, twoWayName = null)
        waitFor("senza gemella") { runBlocking { db.databaseDao().getColumnById(twinId) } == null }
        assertTrue(viewModel.tableState.value.columns.none { it.id == twinId })
    }
}
