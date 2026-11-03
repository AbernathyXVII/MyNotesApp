package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.VersionReason
import com.gabriele.notionlocal.data.history.VersionContent
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.OutlineKind
import com.gabriele.notionlocal.ui.screen.OutlineLine
import com.gabriele.notionlocal.ui.screen.PAGE_HISTORY_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.VERSION_ROW_TAG
import com.gabriele.notionlocal.ui.screen.versionOutline
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.first
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
 * **"Page history"** (02/11/2026, chiesto dall'utente: "Cronologia con
 * ripristino"): aprendo la pagina nasce la prima versione; dai tre puntini,
 * sotto "Updates", l'elenco; toccandone una si legge com'era la pagina, e
 * "Restore this version" la rimette.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class PageHistoryScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    private fun line(id: String, words: String, order: Int, type: BlockType = BlockType.PARAGRAPH, parent: String? = null) =
        BlockEntity(id = id, pageId = "diario", parentBlockId = parent, type = type, textJson = """[{"text":"$words"}]""", orderIndex = order)

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "diario", title = "Diario"))
            db.blockDao().insertAll(
                listOf(
                    line("a", "Primo paragrafo", 0),
                    line("b", "Comprare il latte", 1, BlockType.CHECKBOX),
                    line("c", "Ultima riga", 2)
                )
            )
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun open(locked: Boolean = false) {
        // Con la sua query: riscrivere la pagina intera (REPLACE) porterebbe via i blocchi a cascata.
        if (locked) runBlocking { PageRepository(db).setLocked("diario", true) }
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "diario", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Primo paragrafo", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun versions() = runBlocking { db.pageVersionDao().observeForPage("diario").first() }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            Thread.sleep(20)
            condition()
        }
        compose.waitForIdle()
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun openHistory() {
        compose.onNodeWithContentDescription("Page options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Page history").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithTag(PAGE_HISTORY_TAG).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun openingThePageSavesTheFirstVersionAndTheMenuShowsIt() {
        open()
        waitFor { versions().size == 1 }
        assertEquals(VersionReason.OPENED, versions().single().reason)
        openHistory()
        assertTrue(shown("Today"))
        assertTrue(shown("3 lines · 7 words"))
    }

    @Test
    fun aVersionIsReadAndRestored() {
        open()
        waitFor { versions().size == 1 }
        // La pagina si rovina (come coi bug delle liste): due righe se ne vanno.
        runBlocking {
            db.blockDao().delete(db.blockDao().getBlocksForPageOnce("diario").single { it.id == "b" })
            db.blockDao().delete(db.blockDao().getBlocksForPageOnce("diario").single { it.id == "c" })
        }
        waitFor { !shown("Ultima riga") }

        openHistory()
        compose.onAllNodesWithTag(VERSION_ROW_TAG).onFirst().performClick()
        // L'anteprima: com'era, con la casella davanti alla sua riga.
        waitFor { shown("Comprare il latte") }
        assertTrue(shown("☐"))
        compose.onNodeWithText("Restore this version").performClick()
        compose.waitForIdle()
        assertTrue(shown("Restore this version?"))
        compose.onNodeWithText("Restore").performClick()

        waitFor { runBlocking { db.blockDao().getBlocksForPageOnce("diario").size } == 3 }
        waitFor { compose.onAllNodesWithTag(PAGE_HISTORY_TAG).fetchSemanticsNodes().isEmpty() }
        waitFor { shown("Ultima riga") }
        // Com'era prima del ripristino è nella cronologia.
        waitFor { versions().any { it.reason == VersionReason.BEFORE_RESTORE && it.lines == 1 } }
    }

    @Test
    fun aLockedPageShowsItsVersionsButDoesNotRestoreThem() {
        open(locked = true)
        waitFor { versions().size == 1 }
        openHistory()
        compose.onAllNodesWithTag(VERSION_ROW_TAG).onFirst().performClick()
        waitFor { shown("Comprare il latte") }
        assertTrue(shown("The page is locked"))
        compose.onNodeWithText("Restore this version").assertIsNotEnabled()
    }

    @Test
    fun theOutlineReadsLikeThePage() {
        val blocks = listOf(
            line("n1", "Uno", 0, BlockType.NUMBERED_LIST_ITEM),
            line("n2", "Due", 1, BlockType.NUMBERED_LIST_ITEM),
            line("t", "Toggle", 2, BlockType.TOGGLE),
            line("dentro", "Dentro", 0, BlockType.BULLET_LIST_ITEM, parent = "t"),
            line("n3", "Di nuovo uno", 3, BlockType.NUMBERED_LIST_ITEM),
            BlockEntity(id = "div", pageId = "diario", type = BlockType.DIVIDER, orderIndex = 4),
            BlockEntity(id = "tab", pageId = "diario", type = BlockType.TABLE, tableRows = 1, tableCols = 2, orderIndex = 5),
            BlockEntity(id = "link", pageId = "diario", type = BlockType.PAGE_LINK, linkedPageId = "sotto", orderIndex = 6)
        )
        val cells = listOf(
            TableCellEntity(blockId = "tab", rowIndex = 0, colIndex = 0, text = "A"),
            TableCellEntity(blockId = "tab", rowIndex = 0, colIndex = 1, text = "B")
        )
        val outline = versionOutline(VersionContent.of("Diario", blocks.shuffled(), cells, emptyList()), mapOf("sotto" to "Sottopagina"))
        assertEquals(
            listOf(
                OutlineLine(0, "1.", "Uno"),
                OutlineLine(0, "2.", "Due"),
                OutlineLine(0, "▸", "Toggle"),
                OutlineLine(1, "•", "Dentro"),
                // Dopo il toggle l'elenco riparte.
                OutlineLine(0, "1.", "Di nuovo uno"),
                OutlineLine(0, "", "", OutlineKind.DIVIDER),
                OutlineLine(0, "", "A  │  B"),
                OutlineLine(0, "📄", "Sottopagina")
            ),
            outline
        )
    }
}
