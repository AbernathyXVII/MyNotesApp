package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.FULL_LINE_DEFAULT
import com.gabriele.notionlocal.data.entity.FULL_LINE_OFF
import com.gabriele.notionlocal.data.entity.FULL_LINE_ON
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.PageFullLineSheet
import com.gabriele.notionlocal.ui.screen.fullLineSummary
import com.gabriele.notionlocal.ui.screen.fullLinesOf
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **"Full or page line"** (27/10/2026, chiesto dall'utente): ogni pagina
 * sceglie dai tre puntini se le righe lunghe restano intere (da scorrere di
 * lato) o vanno a capo, oppure fa come Settings ("Default"). Qui una riga
 * lunga nella pagina vera: si contano le righe in cui il campo la disegna.
 */
@RunWith(RobolectricTestRunner::class)
// Il testo misurato davvero: serve a sapere dove va a capo.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class FullLineChoiceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val long = "Una riga molto lunga che non sta nello schermo del telefono ".repeat(4).trim()

    @Before
    fun setUp() {
        AppSettings.init(context)
    }

    @After
    fun tearDown() {
        AppSettings.fullLineView = true
        AppDatabase.resetForTests()
    }

    private fun page(mode: Int) = runBlocking {
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "p", title = "Prova", fullLineMode = mode))
            db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"$long"}]""", orderIndex = 0))
        }
    }

    private fun open() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Una riga molto lunga", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    /** In quante righe il campo disegna la riga lunga. */
    private fun shownLines(): Int {
        compose.waitForIdle()
        val node = compose.onNode(hasSetTextAction() and hasText("Una riga molto lunga", substring = true)).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        // Dalla prima all'ultima lettera della riga lunga (prima c'è l'a-capo nascosto del campo).
        val layout = layouts.single()
        val text = layout.layoutInput.text.text
        val start = text.indexOf("Una riga molto lunga")
        return layout.getLineForOffset(start + long.length - 1) - layout.getLineForOffset(start) + 1
    }

    @Test
    fun pageLineWrapsEvenWithFullLineViewInSettings() {
        AppSettings.fullLineView = true
        page(FULL_LINE_OFF)
        open()
        assertTrue(shownLines() > 1)
    }

    @Test
    fun fullLineKeepsTheLineWholeEvenWithFullLineViewOffInSettings() {
        AppSettings.fullLineView = false
        page(FULL_LINE_ON)
        open()
        assertEquals(1, shownLines())
    }

    @Test
    fun defaultFollowsSettingsEvenWhenItChanges() {
        AppSettings.fullLineView = true
        page(FULL_LINE_DEFAULT)
        open()
        assertEquals(1, shownLines())
        // Cambiato Settings, la pagina su "Default" lo segue.
        compose.runOnUiThread { AppSettings.fullLineView = false }
        assertTrue(shownLines() > 1)
    }

    @Test
    fun whatTheMenuSaysAndTheThreeChoices() {
        AppSettings.fullLineView = true
        assertTrue(fullLinesOf(PageEntity(title = "x")))
        assertEquals("Default (Full line)", fullLineSummary(PageEntity(title = "x")))
        assertEquals("Page line", fullLineSummary(PageEntity(title = "x", fullLineMode = FULL_LINE_OFF)))
        AppSettings.fullLineView = false
        assertFalse(fullLinesOf(PageEntity(title = "x")))
        assertEquals("Default (Page line)", fullLineSummary(PageEntity(title = "x")))
        assertEquals("Full line", fullLineSummary(PageEntity(title = "x", fullLineMode = FULL_LINE_ON)))

        var chosen: Int? = null
        compose.setContent {
            NotionLocalTheme {
                PageFullLineSheet(page = PageEntity(title = "x"), onSet = { chosen = it }, onDismiss = {})
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Full or page line").assertExists()
        compose.onNodeWithText("Default (Page line)").assertExists()
        compose.onNodeWithText("Page line").performClick()
        compose.waitForIdle()
        assertEquals(FULL_LINE_OFF, chosen)
        compose.onNodeWithText("Full line").performClick()
        compose.waitForIdle()
        assertEquals(FULL_LINE_ON, chosen)
    }
}
