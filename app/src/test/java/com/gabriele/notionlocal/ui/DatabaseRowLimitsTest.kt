package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.BOARD_BAND_LIMIT
import com.gabriele.notionlocal.ui.screen.DATABASE_ROW_LIMIT
import com.gabriele.notionlocal.ui.screen.DatabaseContent
import com.gabriele.notionlocal.ui.screen.RowLimits
import com.gabriele.notionlocal.ui.screen.limit
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I database con tante righe** (18/10/2026, chiesto dall'utente per il suo
 * dizionario, scelto "come Notion"): dentro una pagina le prime 50 righe e
 * "Load more"; a pagina intera tutte, ma si fanno solo quelle sullo schermo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class DatabaseRowLimitsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val words = 1_200
    private val dictionary = PageEntity(id = "dizionario", title = "Dizionario", isDatabase = true, isSimpleDatabase = true)

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(dictionary)
            repeat(words) { i ->
                db.databaseDao().insertRow(DatabaseRowEntity(id = "w$i", pageId = dictionary.id, title = "Parola $i", orderIndex = i))
            }
        }
    }

    @After
    fun tearDown() {
        AppDatabase.resetForTests()
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun waitForRows() = compose.waitUntil(10_000) { shows("Parola 0") }

    @Test
    fun insideAPageTheFirstRowsAndLoadMore() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                // Come nella pagina: il database non scorre per conto suo.
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    DatabaseContent(pageId = dictionary.id, factory = factory, embedded = true, onOpenRowPage = {}, onOpenFullPage = {})
                }
            }
        }
        waitForRows()
        assertTrue(shows("Parola ${DATABASE_ROW_LIMIT - 1}"))
        assertTrue(!shows("Parola $DATABASE_ROW_LIMIT"))
        // Quante restano fuori.
        compose.onNodeWithText("${words - DATABASE_ROW_LIMIT}").assertExists()
        // Sta sotto lo schermo: lo si preme come fa TalkBack.
        compose.onNodeWithTag("load-more").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(5_000) { shows("Parola ${2 * DATABASE_ROW_LIMIT - 1}") }
        assertTrue(!shows("Parola ${2 * DATABASE_ROW_LIMIT}"))
        compose.onNodeWithText("${words - 2 * DATABASE_ROW_LIMIT}").assertExists()
    }

    @Test
    fun fullPageMakesOnlyTheRowsOnScreen() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                DatabaseContent(
                    pageId = dictionary.id, factory = factory, embedded = false, onOpenRowPage = {}, onOpenFullPage = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        waitForRows()
        // Niente "Load more": scorrono tutte...
        assertTrue(compose.onAllNodesWithText("Load more").fetchSemanticsNodes().isEmpty())
        // ...ma di 1.200 se ne fanno solo quelle che stanno negli 800dp dello schermo.
        val made = (0 until words).count { shows("Parola $it") }
        assertTrue("fatte $made", made in 5..40)
    }

    @Test
    fun theOtherViewsToo() {
        runBlocking { AppDatabase.getInstance(context).pageDao().update(dictionary.copy(databaseLayout = DatabaseLayout.LIST)) }
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                DatabaseContent(
                    pageId = dictionary.id, factory = factory, embedded = false, onOpenRowPage = {}, onOpenFullPage = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        waitForRows()
        val made = (0 until words).count { shows("Parola $it") }
        assertTrue("fatte $made", made in 5..40)
    }

    @Test
    fun rowsCreatedHereAreAlwaysShown() {
        val limits = RowLimits(step = 3)
        val rows = (0 until 10).toList()
        assertEquals(listOf(0, 1, 2) to 7, limits.take("t", rows) { "$it" }.let { it.items to it.hidden })
        // Una riga nuova in fondo si vede lo stesso.
        limits.created = setOf("9")
        assertEquals(listOf(0, 1, 2, 9) to 6, limits.take("t", rows) { "$it" }.let { it.items to it.hidden })
        // "Load more" solo per quell'elenco.
        limits.more("t")
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 9) to 3, limits.take("t", rows) { "$it" }.let { it.items to it.hidden })
        assertEquals(listOf(0, 1, 2, 9), limits.take("altro", rows) { "$it" }.items)
        // Meno righe del limite: tutte, e niente "Load more".
        assertEquals(0, limits.take("t", listOf(1, 2)) { "$it" }.hidden)
    }

    @Test
    fun theBandsOfTheBoardComeTwelveAtATime() {
        // Mille settimane di vittorie (21/10/2026): le prime 12, poi altre 12.
        val limits = RowLimits()
        val weeks = (0 until 1000).toList()
        assertEquals(12 to 988, limits.limit("board-bands", weeks, BOARD_BAND_LIMIT) { "$it" }.let { it.items.size to it.hidden })
        limits.more("board-bands", BOARD_BAND_LIMIT)
        assertEquals(24, limits.limit("board-bands", weeks, BOARD_BAND_LIMIT) { "$it" }.items.size)
        // Le schede delle colonne restano a 50.
        assertEquals(50, limits.limit("board:x", weeks) { "$it" }.items.size)
        // Senza limiti (un database che li mostra tutti), tutte.
        assertEquals(1000, (null as RowLimits?).limit("board-bands", weeks, BOARD_BAND_LIMIT) { "$it" }.items.size)
    }
}
