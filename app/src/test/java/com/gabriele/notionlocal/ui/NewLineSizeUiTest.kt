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
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * **Sotto un titolo grande si scrive a 16, e lo si vede** (27/10/2026,
 * chiesto dall'utente: "se dovessi modificare il font size di un blocco, per
 * esempio un titolo a 50, e poi vado a capo per scrivere altro, fai sì che il
 * font size venga resettato a 16 per tutti i nuovi blocchi").
 *
 * Nel database le righe nuove erano già a 16; quello che sembrava 50 era il
 * campo: la riga nuova stava in quello del titolo, alta come lui e col
 * cursore alto come lui, finché la tastiera era aperta. Qui si scrive come
 * sul telefono — Invio in fondo al titolo e poi le lettere — e dopo la pausa
 * il titolo ha un campo suo, mentre il campo col cursore resta quello e il
 * cursore resta in fondo a quello che si è scritto.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class NewLineSizeUiTest {
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
            db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"Titolo","size":50}]""", orderIndex = 0))
            // Una riga sotto, come nella pagina vera: uscito il titolo, il campo
            // col cursore si unisce anche a lei (prima, nello stesso giro, e il
            // cursore finiva in questa riga).
            db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"Fine"}]""", orderIndex = 1))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun stored() = runBlocking {
        AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("p").sortedBy { it.orderIndex }
    }

    /** I campi di testo della pagina (il titolo della pagina escluso), col loro testo. */
    private fun fields() = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        .map { it to it.config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty() }
        .filter { (_, text) -> text.isNotEmpty() && text != "Prova" }

    private fun settle(realMs: Long = 0) {
        if (realMs > 0) Thread.sleep(realMs)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }

    @Test
    fun underABigTitleTheNewLinesGetTheirOwnFieldAndKeepTheCursor() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Titolo", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val field = compose.onNode(hasSetTextAction() and hasText("Titolo", substring = true))
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        // In fondo al titolo (il testo del campo comincia con l'a-capo nascosto).
        field.performTextInputSelection(TextRange(7))
        field.performTextInput("\nAltro")
        // Il database scrive un attimo dopo, fuori dal filo principale.
        repeat(20) {
            if (stored().size < 3) {
                compose.mainClock.advanceTimeBy(200)
                Thread.sleep(100)
                compose.waitForIdle()
            }
        }
        settle()

        // Nel database: il titolo a 50, la riga nuova senza corpo (16).
        val (title, line) = stored().take(2)
        assertTrue(title.textJson.contains("\"size\":50"))
        assertEquals("""[{"text":"Altro"}]""", line.textJson)

        // Dopo la pausa (`TYPING_QUIET_MS`, in tempo vero) il titolo esce dal
        // campo col cursore.
        compose.waitUntil(PageEditorViewModel.TYPING_QUIET_MS + 8_000) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
            fields().map { it.second } == listOf("Titolo", "Altro\nFine")
        }
        settle()
        val shown = fields()
        assertEquals(listOf("Titolo", "Altro\nFine"), shown.map { it.second })
        val altro = shown.single { it.second.startsWith("Altro") }.first
        assertTrue("il cursore non è più nel campo", altro.config.getOrNull(SemanticsProperties.Focused) == true)
        // In fondo ad "Altro": il testo del campo comincia con l'a-capo nascosto.
        assertEquals(TextRange(1 + "Altro".length), altro.config.getOrNull(SemanticsProperties.TextSelectionRange))
        assertNull(shown.single { it.second == "Titolo" }.first.config.getOrNull(SemanticsProperties.Focused)?.takeIf { it })
    }
}
