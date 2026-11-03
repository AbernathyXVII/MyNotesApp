package com.gabriele.notionlocal.viewmodel

import org.junit.Assert.assertTrue
import com.gabriele.notionlocal.data.hasChosenIcon
import com.gabriele.notionlocal.data.IconSymbol
import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.DEFAULT_PAGE_ICON
import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.RowIconRef
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
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

/**
 * **Le icone scelte col selettore** (07/10/2026): un'emoji, un'immagine o
 * niente, per le pagine, i database e le pagine delle righe. Il 📄 di
 * riserva vuol dire "niente scelto".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageIconTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private val page = PageEntity(title = "Viaggi", iconImage = "vecchia.png")
    private val games = PageEntity(title = "Giochi", isDatabase = true)
    private val zelda = DatabaseRowEntity(pageId = games.id, title = "Zelda", orderIndex = 0)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(page)
        pages.createPage(games)
        db.databaseDao().insertRow(zelda)
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

    private fun stored(id: String) = runBlocking { db.pageDao().getById(id)!! }

    @Test
    fun anEmojiTakesThePlaceOfTheImage_andRemoveLeavesTheSpareSheet() {
        val viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("la pagina") { viewModel.page.value != null }

        // L'immagine di prima va cancellata da chi chiama: gliela si restituisce.
        assertEquals("vecchia.png", viewModel.setIcon(IconChoice.Emoji("🧳")))
        waitFor("l'emoji salvata") { stored(page.id).icon == "🧳" && viewModel.page.value?.icon == "🧳" }
        assertNull(stored(page.id).iconImage)

        assertNull(viewModel.setIcon(IconChoice.Image("nuova.png")))
        waitFor("l'immagine salvata") {
            stored(page.id).iconImage == "nuova.png" && viewModel.page.value?.iconImage == "nuova.png"
        }
        assertEquals(DEFAULT_PAGE_ICON, stored(page.id).icon)

        assertEquals("nuova.png", viewModel.setIcon(IconChoice.None))
        waitFor("niente") { stored(page.id).iconImage == null }
        assertEquals(DEFAULT_PAGE_ICON, stored(page.id).icon)
    }

    @Test
    fun aDatabaseAndItsRowsCanHaveAnEmoji() {
        val viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(games.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.rows.isNotEmpty() }

        viewModel.setIcon(IconChoice.Emoji("🎮"))
        waitFor("l'emoji del database") { stored(games.id).icon == "🎮" }

        // La riga non ha ancora una pagina: nasce adesso, con l'emoji.
        assertEquals(emptyMap<String, RowIconRef>(), viewModel.rowIcons.value)
        viewModel.setRowIcon(zelda, IconChoice.Emoji("🗡️"))
        waitFor("la pagina della riga") { runBlocking { db.databaseDao().getRowById(zelda.id)!!.linkedPageId } != null }
        val rowPage = runBlocking { db.databaseDao().getRowById(zelda.id)!!.linkedPageId!! }
        waitFor("l'emoji della riga") { stored(rowPage).icon == "🗡️" }
        // Il database la vede dalla query che segue righe e pagine insieme.
        waitFor("l'icona nel database") { viewModel.rowIcons.value[zelda.id] == RowIconRef(emoji = "🗡️", image = null) }

        viewModel.setRowIcon(zelda, IconChoice.None)
        waitFor("tolta") { viewModel.rowIcons.value[zelda.id] == null && stored(rowPage).icon == DEFAULT_PAGE_ICON }
    }

    @Test
    fun aMaterialIconWithItsColorIsSaved_andAnEmojiTakesItsPlace() {
        val viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("la pagina") { viewModel.page.value != null }

        assertEquals("vecchia.png", viewModel.setIcon(IconChoice.Symbol(IconSymbol("Home", "#E03E3E"))))
        waitFor("l'icona salvata") { stored(page.id).iconSymbol == "Home|#E03E3E" && viewModel.page.value?.iconSymbol != null }
        assertNull(stored(page.id).iconImage)
        assertEquals(DEFAULT_PAGE_ICON, stored(page.id).icon)
        assertTrue(stored(page.id).hasChosenIcon)

        viewModel.setIcon(IconChoice.Emoji("🏠"))
        waitFor("l'emoji") { stored(page.id).icon == "🏠" }
        assertNull(stored(page.id).iconSymbol)
    }

    @Test
    fun theRowsOfADatabaseShowTheirMaterialIcon() {
        val viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(games.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.rows.isNotEmpty() }
        viewModel.setRowIcon(zelda, IconChoice.Symbol(IconSymbol("SportsEsports")))
        waitFor("l'icona nel database") {
            viewModel.rowIcons.value[zelda.id] == RowIconRef(emoji = null, image = null, symbol = "SportsEsports")
        }
    }
}
