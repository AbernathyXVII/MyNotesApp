package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.webclip.WebClipMode
import com.gabriele.notionlocal.data.webclip.WebClipRequest
import com.gabriele.notionlocal.ui.screen.WebClipperSheet
import com.gabriele.notionlocal.ui.screen.isTallImage
import com.gabriele.notionlocal.ui.screen.sampleFor
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Il pannello del Web Clipper** (20/10/2026): le scelte di OneNote, senza
 * "Location"; "Clip" si accende solo con un link vero.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class WebClipperSheetTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun theOptionsAreThoseOfOneNoteAndClipNeedsALink() {
        var clipped: WebClipRequest? = null
        compose.setContent {
            NotionLocalTheme {
                WebClipperSheet(onDismiss = {}, onClip = { clipped = it })
            }
        }
        compose.onNodeWithText("What do you want to capture?").assertExists()
        listOf("Full Page", "Region", "Bookmark", "Article", "Text and Images", "Title", "Note", "Source").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("Location").assertDoesNotExist()
        compose.onNodeWithText("Clip").assertIsNotEnabled()

        val fields = compose.onAllNodes(hasSetTextAction())
        // Non un link: "Clip" resta spento e lo dice.
        fields[0].performTextInput("ciao")
        compose.onNodeWithText("That doesn't look like a link").assertExists()
        compose.onNodeWithText("Clip").assertIsNotEnabled()
    }

    @Test
    fun clipSendsTheLinkTheModeAndTheNote() {
        var clipped: WebClipRequest? = null
        compose.setContent {
            NotionLocalTheme {
                WebClipperSheet(onDismiss = {}, onClip = { clipped = it })
            }
        }
        val fields = compose.onAllNodes(hasSetTextAction())
        // Un indirizzo che non esiste: il titolo del sito non arriva, e la prova resta veloce.
        fields[0].performTextInput("clip-test.invalid/page")
        compose.onNodeWithText("Article").performClick()
        fields[1].performTextInput("Prologue")
        fields[2].performTextInput("Da rileggere")
        compose.onNodeWithText("Clip").assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(WebClipRequest("https://clip-test.invalid/page", WebClipMode.ARTICLE, "Prologue", "Da rileggere"), clipped)
        compose.onAllNodes(hasText("https://clip-test.invalid/page")).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun aClipIsTallAndIsReadAtTheRightSize() {
        // La foto di game8: alta, si legge a pezzi.
        assertTrue(isTallImage(2160, 19023))
        // Le foto della fotocamera no, anche grandi e in verticale.
        assertFalse(isTallImage(4000, 3000))
        assertFalse(isTallImage(3000, 4000))
        assertFalse(isTallImage(1080, 1920))
        // Mostrata più piccola della metà, si legge rimpicciolita; ingrandita, intera.
        assertEquals(1, sampleFor(0.67f))
        assertEquals(1, sampleFor(3f))
        assertEquals(2, sampleFor(0.4f))
        assertEquals(4, sampleFor(0.2f))
    }
}
