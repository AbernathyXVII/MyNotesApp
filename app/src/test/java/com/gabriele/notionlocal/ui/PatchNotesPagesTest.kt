package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gabriele.notionlocal.data.patchnotes.NoteBlock
import com.gabriele.notionlocal.data.patchnotes.PatchNotesAsset
import com.gabriele.notionlocal.data.patchnotes.PatchVersion
import com.gabriele.notionlocal.ui.screen.PatchNoteScreen
import com.gabriele.notionlocal.ui.screen.PatchNotesScreen
import com.gabriele.notionlocal.ui.screen.inlineMarkdown
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Una pagina per ogni versione** (30/09/2026): l'elenco delle Patch Notes
 * ha solo i titoli — emoji, numero, giorno e ora, titolo — uno sotto
 * l'altro, e toccandone uno si apre la sua pagina col suo testo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-xhdpi")
class PatchNotesPagesTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val notes get() = PatchNotesAsset.load(compose.activity)

    private fun waitForText(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    /** Il testo di un blocco come lo mostra la schermata, senza asterischi e accenti gravi. */
    private fun shownText(block: NoteBlock) = inlineMarkdown(
        when (block) {
            is NoteBlock.Paragraph -> block.text
            is NoteBlock.Bullet -> block.text
            is NoteBlock.Quote -> block.text
            is NoteBlock.Table -> block.rows.first().first()
        }
    ).text

    /**
     * Un testo che è solo di quella versione: la richiesta parola per parola, o,
     * dove manca (v0.116.0.1, riassunta), la prima modifica. Non il primo
     * paragrafo: la riga in corsivo sotto il titolo si ripete uguale.
     */
    private fun bodyOf(version: PatchVersion) =
        shownText(version.blocks.firstOrNull { it is NoteBlock.Quote } ?: version.blocks.first { it is NoteBlock.Bullet })

    @Test
    fun theListHasOnlyTheTitlesAndOpensTheTappedOne() {
        val (newest, previous) = notes.versions
        var opened: String? = null
        compose.setContent {
            NotionLocalTheme {
                PatchNotesScreen(onBack = {}, onOpenVersion = { opened = it })
            }
        }
        waitForText("v${newest.version}")
        // Le due più recenti una sotto l'altra: il testo della prima non c'è.
        compose.onNodeWithText("v${newest.version}").assertIsDisplayed()
        compose.onNodeWithText("${newest.date} · ${newest.time}").assertIsDisplayed()
        compose.onNodeWithText("v${previous.version}").assertIsDisplayed()
        val newestBody = bodyOf(newest)
        assertTrue(compose.onAllNodesWithText(newestBody.take(40), substring = true).fetchSemanticsNodes().isEmpty())

        compose.onNodeWithText("v${previous.version}").performClick()
        assertEquals(previous.version, opened)
    }

    @Test
    fun aVersionPageShowsItsText() {
        val version = notes.versions[1]
        compose.setContent {
            NotionLocalTheme {
                PatchNoteScreen(version = version.version, onBack = {})
            }
        }
        waitForText(version.time)
        compose.onNodeWithText("${version.date} · ${version.time}").assertIsDisplayed()
        compose.onNodeWithText(shownText(version.blocks.first()).take(30), substring = true).assertIsDisplayed()
        // Il testo delle altre versioni non c'è.
        val other = bodyOf(notes.versions[0])
        assertTrue(compose.onAllNodesWithText(other.take(40), substring = true).fetchSemanticsNodes().isEmpty())
    }
}
