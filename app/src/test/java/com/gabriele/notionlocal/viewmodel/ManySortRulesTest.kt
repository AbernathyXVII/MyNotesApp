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
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.sortRules
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
 * **Le regole di ordinamento, quante se ne vogliono** (17/10/2026): ognuna
 * conta solo fra le righe pari per quelle sopra; si aggiungono in fondo, si
 * spostano, si tolgono, e restano salvate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ManySortRulesTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: DatabaseViewModel
    private val games = PageEntity(title = "Videogiochi", isDatabase = true)
    private val platform = DatabaseColumnEntity(pageId = games.id, name = "Platform", type = ColumnType.SELECT, orderIndex = 0)
    private val genre = DatabaseColumnEntity(pageId = games.id, name = "Genre", type = ColumnType.SELECT, orderIndex = 1)
    private val score = DatabaseColumnEntity(pageId = games.id, name = "Score", type = ColumnType.NUMBER, orderIndex = 2)

    /** nome, piattaforma, genere, voto: nell'ordine in cui sono state create. */
    private val data = listOf(
        listOf("Bloodborne", "PS5", "Action", "9"),
        listOf("Hades", "PC", "Action", "9"),
        listOf("Astro Bot", "PS5", "Platform", "10"),
        listOf("Celeste", "PC", "Platform", "10"),
        listOf("Returnal", "PS5", "Action", "8"),
        listOf("Dead Cells", "PC", "Action", "9")
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(games)
        listOf(platform, genre, score).forEach { db.databaseDao().insertColumn(it) }
        data.forEachIndexed { i, (name, platformValue, genreValue, scoreValue) ->
            val row = DatabaseRowEntity(pageId = games.id, title = name, orderIndex = i, createdAt = 1_000L + i)
            db.databaseDao().insertRow(row)
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = platform.id, value = platformValue))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = genre.id, value = genreValue))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = score.id, value = scoreValue))
        }
        viewModel = load()
    }

    private fun load(): DatabaseViewModel {
        val model = DatabaseViewModel(DatabaseRepository(db), pages)
        model.load(games.id)
        waitFor("il database") { model.page.value != null && model.tableState.value.rows.size == data.size }
        return model
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

    /**
     * Una modifica, e poi si aspetta che sia salvata. Il ViewModel rilegge
     * la pagina dal database dopo ogni salvataggio: senza aspettare, una
     * rilettura arrivata tardi rimetterebbe le regole di prima sotto la
     * modifica dopo. Col dito, fra un tocco e l'altro, il tempo c'è sempre.
     */
    private fun step(change: DatabaseViewModel.() -> Unit) {
        viewModel.change()
        val wanted = viewModel.page.value!!.sortRules()
        waitFor("salvato: $wanted") { savedPage().sortRules() == wanted && viewModel.page.value!!.sortRules() == wanted }
        // Una rilettura partita prima del salvataggio può ancora arrivare
        // (succedeva nella suite intera, col PC carico): si aspetta che la
        // pagina resti ferma un momento, e che sia ancora quella giusta.
        val quietUntil = System.currentTimeMillis() + 300
        while (System.currentTimeMillis() < quietUntil) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        waitFor("ancora salvato: $wanted") { viewModel.page.value!!.sortRules() == wanted }
    }

    /** Piattaforma, genere, voto dal più alto, nome. */
    private fun addFourRules() {
        step { addSortRule(platform.id) }
        step { addSortRule(genre.id) }
        step { addSortRule(score.id) }
        step { toggleSortRule(score.id) }
        step { addSortRule(SORT_BY_NAME) }
    }

    @Test
    fun everyRuleCountsAfterTheOnesAbove() {
        step { addSortRule(platform.id) }
        step { addSortRule(genre.id) }
        step { addSortRule(score.id) }
        step { toggleSortRule(score.id) }
        // Tre regole: Hades e Dead Cells sono pari per tutte e tre, e restano
        // nell'ordine in cui sono state create.
        waitFor("tre regole") {
            titles() == listOf("Hades", "Dead Cells", "Celeste", "Bloodborne", "Returnal", "Astro Bot")
        }
        // La quarta, per nome, mette in ordine anche loro.
        step { addSortRule(SORT_BY_NAME) }
        waitFor("quattro regole") {
            titles() == listOf("Dead Cells", "Hades", "Celeste", "Bloodborne", "Returnal", "Astro Bot")
        }
        // Tutte e quattro nell'elenco, nell'ordine in cui contano.
        assertEquals(
            listOf(SortRuleSpec(platform.id), SortRuleSpec(genre.id), SortRuleSpec(score.id, true), SortRuleSpec(SORT_BY_NAME)),
            savedPage().sortRules()
        )
        // E riaprendo il database ci sono ancora tutte.
        viewModel = load()
        waitFor("riaperto") {
            titles() == listOf("Dead Cells", "Hades", "Celeste", "Bloodborne", "Returnal", "Astro Bot")
        }
    }

    @Test
    fun movingAndRemovingRules() {
        addFourRules()
        // Il voto sale in cima: prima i 10, poi i 9 (per piattaforma, genere, nome), poi l'8.
        step { moveSortRule(score.id, -1) }
        step { moveSortRule(score.id, -1) }
        assertEquals(listOf(score.id, platform.id, genre.id, SORT_BY_NAME), viewModel.page.value!!.sortRules().map { it.column })
        waitFor("voto prima di tutto") {
            titles() == listOf("Celeste", "Astro Bot", "Dead Cells", "Hades", "Bloodborne", "Returnal")
        }
        // Più su della cima non va.
        viewModel.moveSortRule(score.id, -1)
        assertEquals(score.id, viewModel.page.value!!.sortRules().first().column)
        // Tolta la piattaforma, le altre salgono.
        step { removeSortRule(platform.id) }
        assertEquals(listOf(score.id, genre.id, SORT_BY_NAME), viewModel.page.value!!.sortRules().map { it.column })
        waitFor("voto, genere, nome") {
            titles() == listOf("Astro Bot", "Celeste", "Bloodborne", "Dead Cells", "Hades", "Returnal")
        }
    }

    @Test
    fun theFirstRuleFromTheListAndClearingAll() {
        addFourRules()
        // Dall'elenco delle proprietà: il nome prende il posto della prima
        // regola (la piattaforma) e se ne va dal fondo; le altre restano.
        step { setSort(SORT_BY_NAME, descending = false) }
        assertEquals(listOf(SORT_BY_NAME, genre.id, score.id), viewModel.page.value!!.sortRules().map { it.column })
        waitFor("per nome") {
            titles() == listOf("Astro Bot", "Bloodborne", "Celeste", "Dead Cells", "Hades", "Returnal")
        }
        // "Remove all sorts": l'ordine di creazione, e niente salvato.
        step { clearSortRules() }
        waitFor("ordine di creazione") { titles() == data.map { it[0] } }
        assertNull(savedPage().sortRulesJson)
    }

    @Test
    fun aPropertyIsInOneRuleOnly() {
        addFourRules()
        viewModel.addSortRule(genre.id)
        assertEquals(4, viewModel.page.value!!.sortRules().size)
    }
}
