package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * **"---" in fondo alla pagina** (27/10/2026, trovato dall'utente in
 * "Paragraph – prova"): Invio in fondo all'ultima riga e tre trattini, uno
 * alla volta come dalla tastiera. La riga diventa un divisore, i trattini
 * non restano scritti da nessuna parte e sopra il divisore non resta una
 * riga vuota in più.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class DividerShortcutUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "p", title = "Prova"))
            db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"Primo paragrafo"}]""", orderIndex = 0))
            db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"Ultimo paragrafo"}]""", orderIndex = 1))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun stored() = runBlocking {
        AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("p").sortedBy { it.orderIndex }
            .map { it.type to it.textJson }
    }

    /** I campi di testo della pagina (il titolo escluso), col loro testo. */
    private fun fields() = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        .map { it.config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty() }
        .filter { it != "Prova" }

    private fun settle() {
        repeat(10) {
            compose.mainClock.advanceTimeBy(200)
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            compose.waitForIdle()
        }
    }

    @Test
    fun threeDashesAtTheEndBecomeADividerAndNothingElse() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ultimo paragrafo", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val field = compose.onNode(hasSetTextAction() and hasText("Ultimo paragrafo", substring = true))
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        // In fondo (il testo del campo comincia con l'a-capo nascosto).
        val end = 1 + "Primo paragrafo\nUltimo paragrafo".length
        field.performTextInputSelection(TextRange(end))
        field.performTextInput("\n")
        settle()
        // Di fila, come scrivendo: il database non fa in tempo a rispondere fra un trattino e l'altro.
        repeat(3) {
            compose.onNode(hasSetTextAction() and hasText("Ultimo paragrafo", substring = true)).performTextInput("-")
        }
        settle()

        assertEquals(
            listOf(
                BlockType.PARAGRAPH to """[{"text":"Primo paragrafo"}]""",
                BlockType.PARAGRAPH to """[{"text":"Ultimo paragrafo"}]""",
                BlockType.DIVIDER to """[{"text":""}]""",
                BlockType.PARAGRAPH to "[]"
            ),
            stored()
        )
        // Nessun campo mostra i trattini, e sopra il divisore non c'è una riga
        // vuota: il campo di sopra ha le sue due righe, quello sotto è vuoto.
        assertEquals(listOf("Primo paragrafo\nUltimo paragrafo"), fields().filter { it.isNotEmpty() })
    }

    @Test
    fun threeDashesInTheMiddleOfAListSplitIt() = runBlocking {
        // Come l'utente il 27/10/2026: un elenco numerato, alla terza voce
        // tolto il numero (la riga torna testo) e scritti i tre trattini.
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.blockDao().getBlocksForPageOnce("p").forEach { db.blockDao().delete(it) }
            listOf("A", "A", "", "B", "B").forEachIndexed { i, text ->
                db.blockDao().insert(
                    BlockEntity(
                        id = "l$i", pageId = "p", orderIndex = i,
                        type = if (i == 2) BlockType.PARAGRAPH else BlockType.NUMBERED_LIST_ITEM,
                        textJson = """[{"text":"$text"}]"""
                    )
                )
            }
        }
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction() and hasText("B", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        val field = compose.onNode(hasSetTextAction() and hasText("B", substring = true))
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        // Sulla riga vuota, la terza: dopo l'a-capo nascosto, "A\nA\n".
        field.performTextInputSelection(TextRange(1 + "A\nA\n".length))
        repeat(3) {
            compose.onNode(hasSetTextAction() and hasText("B", substring = true)).performTextInput("-")
        }
        settle()

        assertEquals(
            listOf(
                BlockType.NUMBERED_LIST_ITEM to """[{"text":"A"}]""",
                BlockType.NUMBERED_LIST_ITEM to """[{"text":"A"}]""",
                BlockType.DIVIDER to """[{"text":""}]""",
                BlockType.PARAGRAPH to "[]",
                BlockType.NUMBERED_LIST_ITEM to """[{"text":"B"}]""",
                BlockType.NUMBERED_LIST_ITEM to """[{"text":"B"}]"""
            ),
            stored()
        )
        // Sopra il divisore le due A, sotto la riga nuova e le due B: ognuna una
        // volta sola, niente trattini. I numeri continuano dopo il divisore
        // (un blocco in mezzo non interrompe l'elenco: `numberedOrdinals`).
        assertEquals(listOf("1.  A\n2.  A", "\n3.  B\n4.  B"), fields().filter { it.isNotEmpty() })
    }
}
