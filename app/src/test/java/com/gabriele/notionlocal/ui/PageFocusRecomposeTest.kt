package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
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
 * **La tastiera non fa ridisegnare i database della pagina** (27/10/2026,
 * l'utente: "nella home continua a laggare la tastiera quando la apro e
 * quando la chiudo"). Aprendo e chiudendo la tastiera la pagina si rifà un
 * paio di volte — il cursore che entra e che esce, la tastiera che c'è o non
 * c'è — e i database dentro si rifacevano con lei: nella home sono dieci,
 * tutti tenuti pronti anche fuori dallo schermo, e sul telefono costava fino
 * a 69 ms per fotogramma su 8.
 *
 * Il cursore che entra nel titolo fa rifare la pagina come la tastiera; qui
 * si conta, coi segni che Compose lascia per i profilatori
 * (`Composer.setTracer`), quante volte riparte il resto.
 */
@OptIn(InternalComposeTracingApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class PageFocusRecomposeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Quante volte è ripartita ogni funzione (per nome, come nella traccia) da quando si conta. */
    private val started = mutableListOf<String>()
    private var counting = false

    private fun count(name: String) = started.count { name in it }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        // Un test di prima può aver lasciato aperto il suo database, che qui
        // non vale più: nella suite intera, senza questo, la prova falliva
        // (o si fermava) già scrivendo la pagina.
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
            db.pageDao().insert(PageEntity(id = "spesa", parentId = "pagina", title = "Spesa", isDatabase = true, isSimpleDatabase = true))
            repeat(5) { i ->
                db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "spesa", title = "Cosa $i", orderIndex = i))
            }
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "spesa", orderIndex = 0))
        }
        Composer.setTracer(object : CompositionTracer {
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                if (counting) started += info
            }
            override fun traceEventEnd() = Unit
            override fun isTraceInProgress() = true
        })
    }

    @After
    fun tearDown() {
        counting = false
        Composer.setTracer(object : CompositionTracer {
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) = Unit
            override fun traceEventEnd() = Unit
            override fun isTraceInProgress() = false
        })
        AppDatabase.resetForTests()
    }

    @Test
    fun theCursorInAndOutOfTheTitleLeavesTheDatabaseAlone() {
        val factory = ViewModelFactory(context)
        lateinit var focusManager: FocusManager
        compose.setContent {
            focusManager = LocalFocusManager.current
            NotionLocalTheme {
                PageEditorScreen(
                    pageId = "pagina",
                    factory = factory,
                    onBack = {},
                    onOpenSearch = {},
                    onOpenUpdates = {},
                    onNavigateToPage = {},
                    onNavigateToDatabase = {}
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Cosa 0").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()

        counting = true
        compose.onNode(hasSetTextAction() and hasText("Pagina di prova")).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.runOnIdle { focusManager.clearFocus() }
        compose.waitForIdle()
        counting = false

        // La pagina si è rifatta davvero, se no la prova non direbbe niente...
        assertTrue("la pagina non si è rifatta", count("PageEditorContent") > 0)
        // ...ma il database dentro no, né la sua voce della lista.
        assertEquals(0, count("DatabaseViewBody"))
        assertEquals(0, count("IslandItem"))
        // E la lista non è ripartita con tutte le sue voci.
        assertEquals(0, count("LazyLayoutItemContentFactory"))
    }
}
