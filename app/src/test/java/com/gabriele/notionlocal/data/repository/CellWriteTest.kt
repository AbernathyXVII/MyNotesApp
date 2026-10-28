package com.gabriele.notionlocal.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.data.entity.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Una cella sola per riga e proprietà** (28/10/2026). Sul telefono, scrivendo
 * "Cibo" molto in fretta in due celle vuote, ne erano nate due ciascuna e si
 * vedevano "Ci" e "Cib": ogni lettera salva la cella, e due salvataggi vicini
 * la trovavano vuota tutti e due.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CellWriteTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DatabaseRepository

    private val food = PageEntity(id = "food", title = "Spesa", isDatabase = true)
    private val kind = DatabaseColumnEntity(id = "kind", pageId = "food", name = "Tipo", type = ColumnType.TEXT)
    private val milk = DatabaseRowEntity(id = "latte", pageId = "food", title = "Latte")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        PageRepository(db).createPage(food)
        db.databaseDao().insertColumn(kind)
        db.databaseDao().insertRow(milk)
        repository = DatabaseRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun cells() = db.databaseDao().getCellsForRowOnce(milk.id).filter { it.columnId == kind.id }

    @Test
    fun typingFastInAnEmptyCellLeavesOneCellWithTheWholeWord() = runBlocking<Unit> {
        // Come la schermata: un salvataggio per lettera, partiti uno dopo l'altro senza aspettarsi.
        val word = "Cibo"
        val jobs = (1..word.length).map { n -> launch { repository.setCellValue(milk.id, kind.id, word.take(n)) } }
        jobs.forEach { it.join() }
        assertEquals(listOf(word), cells().map { it.value })
    }

    @Test
    fun manyWritesAtOnceStillMakeOneCell() = runBlocking<Unit> {
        // Anche da più thread insieme: alla fine una cella sola.
        (1..30).map { n -> async(Dispatchers.IO) { repository.setCellValue(milk.id, kind.id, "v$n") } }.awaitAll()
        assertEquals(1, cells().size)
    }

    @Test
    fun aSecondCellForTheSameRowAndPropertyTakesThePlaceOfTheFirst() = runBlocking<Unit> {
        db.databaseDao().insertCell(DatabaseCellEntity(id = "a", rowId = milk.id, columnId = kind.id, value = "Ci"))
        db.databaseDao().insertCell(DatabaseCellEntity(id = "b", rowId = milk.id, columnId = kind.id, value = "Cibo"))
        assertEquals(listOf("b" to "Cibo"), cells().map { it.id to it.value })
    }

    @Test
    fun theLinkLabelIsWrittenOnTheOnlyCell() = runBlocking<Unit> {
        repository.setCellValue(milk.id, kind.id, "https://example.com")
        repository.setCellLinkLabel(milk.id, kind.id, LinkLabel("Esempio", isTitle = false))
        assertEquals(listOf(LinkLabel("Esempio", isTitle = false)), cells().map { it.label })
        // Con un link diverso da quello atteso non si scrive.
        repository.setCellLinkLabel(milk.id, kind.id, LinkLabel("Altro", isTitle = false), expectedValue = "https://altro.it")
        assertEquals(listOf(LinkLabel("Esempio", isTitle = false)), cells().map { it.label })
    }
}
