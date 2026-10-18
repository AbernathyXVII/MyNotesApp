package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.DbStrings
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
 * **Il nome della proprietà principale** (18/10/2026, chiesto dall'utente):
 * ogni database può chiamare "Aa Name" come vuole. Il nome sta nel database,
 * e una vista collegata lo cambia lì.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MainPropertyNameTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository

    private val books = PageEntity(title = "Books", isDatabase = true, databaseLayout = DatabaseLayout.TABLE)
    private val other = PageEntity(title = "Food", isDatabase = true, databaseLayout = DatabaseLayout.TABLE)
    /** Una vista collegata di Books, in un'altra pagina. */
    private val linked = PageEntity(title = "Books", isDatabase = true, sourceDatabaseId = books.id, databaseLayout = DatabaseLayout.GALLERY)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        listOf(books, other, linked).forEach { pages.createPage(it) }
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

    private fun stored(id: String) = runBlocking { db.pageDao().getById(id) }!!

    private fun open(id: String): DatabaseViewModel {
        val viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(id)
        waitFor("la pagina") { viewModel.dataPage.value != null }
        return viewModel
    }

    @Test
    fun renamedInTheDatabaseOnly() {
        val viewModel = open(books.id)
        viewModel.renameNameColumn("  Titolo del libro \n")
        waitFor("il nome nuovo") { stored(books.id).nameColumnTitle != null }
        // Senza spazi e a capo.
        assertEquals("Titolo del libro", stored(books.id).nameColumnTitle)
        waitFor("la tabella lo vede") { viewModel.dataPage.value?.nameColumnTitle == "Titolo del libro" }
        // Gli altri database restano "Name".
        assertNull(stored(other.id).nameColumnTitle)
        // Vuoto torna "Name".
        viewModel.renameNameColumn("   ")
        waitFor("di nuovo Name") { stored(books.id).nameColumnTitle == null }
    }

    @Test
    fun aLinkedViewRenamesItsSource() {
        val viewModel = open(linked.id)
        waitFor("l'origine") { viewModel.dataPage.value?.id == books.id }
        viewModel.renameNameColumn("Titolo")
        waitFor("il nome nell'origine") { stored(books.id).nameColumnTitle == "Titolo" }
        // La vista non ha un nome suo: mostra quello dell'origine.
        assertNull(stored(linked.id).nameColumnTitle)
        waitFor("la vista lo vede") { viewModel.dataPage.value?.nameColumnTitle == "Titolo" }
    }

    @Test
    fun theNameShown() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        AppSettings.language = AppLanguage.ENGLISH
        assertEquals("Name", DbStrings.nameColumn(null))
        assertEquals("Name", DbStrings.nameColumn("  "))
        assertEquals("Titolo", DbStrings.nameColumn("Titolo"))
        AppSettings.language = AppLanguage.ITALIAN
        // Senza un nome scelto, "Name" nella lingua dell'app; uno scelto resta com'è.
        assertEquals("Nome", DbStrings.nameColumn(null))
        assertEquals("Titolo", DbStrings.nameColumn("Titolo"))
        AppSettings.language = AppLanguage.ENGLISH
    }
}
