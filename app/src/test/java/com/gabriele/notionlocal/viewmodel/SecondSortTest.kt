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
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
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

/**
 * **La seconda regola di ordinamento** (05/10/2026): prima conta la
 * prima, e fra le righe pari per lei conta la seconda.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SecondSortTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val games = PageEntity(title = "Videogiochi", isDatabase = true)
    private val platform = DatabaseColumnEntity(pageId = games.id, name = "Platform", type = ColumnType.SELECT, orderIndex = 0)
    private val score = DatabaseColumnEntity(pageId = games.id, name = "Score", type = ColumnType.NUMBER, orderIndex = 1)

    /** nome, piattaforma, voto: nell'ordine in cui sono state create. */
    private val data = listOf(
        Triple("Bloodborne", "PS5", "9"),
        Triple("Hades", "PC", "8"),
        Triple("Astro Bot", "PS5", "10"),
        Triple("Celeste", "PC", "10"),
        Triple("Returnal", "PS5", "8")
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(games)
        db.databaseDao().insertColumn(platform)
        db.databaseDao().insertColumn(score)
        data.forEachIndexed { i, (name, platformValue, scoreValue) ->
            val row = DatabaseRowEntity(pageId = games.id, title = name, orderIndex = i, createdAt = 1_000L + i)
            db.databaseDao().insertRow(row)
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = platform.id, value = platformValue))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = score.id, value = scoreValue))
        }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(games.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.rows.size == data.size }
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
        fail("Non è successo: $what — righe: ${titles()}")
    }

    private fun titles() = viewModel.tableState.value.rows.map { it.title }

    private fun savedPage() = runBlocking { db.pageDao().getById(games.id)!! }

    @Test
    fun theSecondRuleOrdersTheRowsThatAreEqualForTheFirst() {
        viewModel.setSort(platform.id, descending = false)
        // Solo la prima: dentro ogni piattaforma, l'ordine di creazione.
        waitFor("per piattaforma") { titles() == listOf("Hades", "Celeste", "Bloodborne", "Astro Bot", "Returnal") }
        viewModel.setThenSort(score.id, descending = true)
        // PC prima di PS5 resta; dentro ognuna, dal voto più alto.
        waitFor("poi per voto") { titles() == listOf("Celeste", "Hades", "Astro Bot", "Bloodborne", "Returnal") }
        waitFor("salvata") { savedPage().thenSortColumnId == score.id }
        val saved = savedPage()
        assertEquals(score.id, saved.thenSortColumnId)
        assertEquals(true, saved.thenSortDescending)
    }

    @Test
    fun theFirstRuleComesFirst() {
        viewModel.setSort(score.id, descending = true)
        viewModel.setThenSort(SORT_BY_NAME, descending = false)
        // Prima il voto; a parità di voto, il nome.
        waitFor("voto, poi nome") { titles() == listOf("Astro Bot", "Celeste", "Bloodborne", "Hades", "Returnal") }
        // Cambiando verso alla prima, la seconda resta com'è.
        viewModel.setSort(score.id, descending = false)
        waitFor("voto al contrario, poi nome") { titles() == listOf("Hades", "Returnal", "Bloodborne", "Astro Bot", "Celeste") }
    }

    @Test
    fun removingTheFirstRulePromotesTheSecond() {
        viewModel.setSort(platform.id, descending = false)
        viewModel.setThenSort(score.id, descending = true)
        viewModel.setSort(null, descending = false)
        waitFor("solo per voto") { titles().take(2).toSet() == setOf("Astro Bot", "Celeste") && titles().last() in setOf("Hades", "Returnal") }
        // Il salvataggio arriva in sottofondo.
        waitFor("salvata") { savedPage().sortColumnId == score.id }
        val saved = savedPage()
        assertEquals(score.id, saved.sortColumnId)
        assertEquals(true, saved.sortDescending)
        assertNull(saved.thenSortColumnId)
    }

    @Test
    fun theSecondRuleNeedsAFirstAndADifferentProperty() {
        // Senza la prima non si mette.
        viewModel.setThenSort(score.id, descending = false)
        assertNull(viewModel.page.value!!.thenSortColumnId)
        // Sulla stessa proprietà della prima nemmeno.
        viewModel.setSort(score.id, descending = false)
        viewModel.setThenSort(score.id, descending = true)
        assertNull(viewModel.page.value!!.thenSortColumnId)
        // E se la prima diventa quella della seconda, la seconda se ne va.
        viewModel.setThenSort(platform.id, descending = false)
        assertEquals(platform.id, viewModel.page.value!!.thenSortColumnId)
        viewModel.setSort(platform.id, descending = false)
        assertNull(viewModel.page.value!!.thenSortColumnId)
    }
}
