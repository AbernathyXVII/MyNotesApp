package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.EmojiCatalogs
import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.IconPickerSheet
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Il selettore delle icone** (07/10/2026): le categorie di Notion con la
 * loro barra, la ricerca, il colore della pelle, "Remove".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class IconPickerTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var picked: IconChoice? = null

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        AppSettings.skinTone = 0
        // Nei test non c'è il carattere delle emoji del telefono: si fa finta che le sappia tutte.
        EmojiCatalogs.glyphCheck = { true }
    }

    @After
    fun tearDown() {
        AppSettings.skinTone = 0
    }

    private fun show(hasIcon: Boolean) {
        compose.setContent {
            NotionLocalTheme {
                IconPickerSheet(
                    store = PageImageStore(ApplicationProvider.getApplicationContext()),
                    hasIcon = hasIcon,
                    onPick = {
                        picked = it
                        null
                    },
                    onDismiss = {}
                )
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("People").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theCategoriesAndTheirBarAreThere_andASearchPicksAnEmoji() {
        show(hasIcon = false)
        compose.onNodeWithText("People").assertExists()
        compose.onNodeWithContentDescription("Food and drink").assertExists()
        compose.onNodeWithContentDescription("Flags").assertExists()
        compose.onNodeWithContentDescription("Custom").assertExists()
        // Senza icona non c'è niente da togliere.
        compose.onNodeWithText("Remove").assertDoesNotExist()

        compose.onNodeWithTag("emojiFilter").performTextInput("cat face")
        compose.onNodeWithText("🐱").performClick()
        assertEquals(IconChoice.Emoji("🐱"), picked)
        assertEquals("🐱", AppSettings.recentEmojis.first())
    }

    @Test
    fun theSkinToneColorsTheHands() {
        AppSettings.skinTone = 3
        show(hasIcon = false)
        compose.onNodeWithTag("emojiFilter").performTextInput("waving hand")
        compose.onNodeWithText("👋🏽").performClick()
        assertEquals(IconChoice.Emoji("👋🏽"), picked)
    }

    @Test
    fun removeIsThereWhenThereIsAnIcon() {
        show(hasIcon = true)
        compose.onNodeWithText("Remove").performClick()
        assertEquals(IconChoice.None, picked)
    }

    @Test
    fun nothingFoundSaysSo() {
        show(hasIcon = false)
        compose.onNodeWithTag("emojiFilter").performTextInput("zzzqqq")
        compose.onNodeWithText("No results").assertExists()
    }
}
