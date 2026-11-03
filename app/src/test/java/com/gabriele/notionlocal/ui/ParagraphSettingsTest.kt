package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ListGaps
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.ParagraphSettingsDialog
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Settings → "Paragraph"** (21/10/2026): lo spazio fra i blocchi, e quello
 * dopo i marcatori uguale per tutti gli elenchi o uno per ognuno.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class ParagraphSettingsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        AppSettings.blockSpacing = 0
        AppSettings.listGaps = ListGaps()
    }

    @After
    fun tearDown() {
        AppSettings.blockSpacing = 0
        AppSettings.listGaps = ListGaps()
    }

    @Test
    fun theSpaceBetweenBlocksAndAfterTheMarkersAreChosenHere() {
        compose.setContent { NotionLocalTheme { ParagraphSettingsDialog(onDismiss = {}) } }
        compose.onNodeWithText("Space between blocks").assertExists()
        compose.onNodeWithText("0 pt").assertExists()
        compose.onNodeWithText("All lists").assertExists()

        // "+" dello spazio fra i blocchi: un passo.
        compose.onAllNodesWithContentDescription("More space")[0].performClick()
        compose.waitForIdle()
        assertEquals(2, AppSettings.blockSpacing)

        // Uno per ogni elenco: tre righe, e tutti partono da Default.
        compose.onNodeWithText("Same for numbered, bulleted and to-do lists").performClick()
        compose.waitForIdle()
        assertFalse(AppSettings.listGaps.same)
        listOf("Numbered list", "Bulleted list", "To-do list").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("All lists").assertDoesNotExist()

        // "+" del to-do: da Default a 0 pt, solo per lui.
        compose.onAllNodesWithContentDescription("More space")[3].performClick()
        compose.waitForIdle()
        assertEquals(0, AppSettings.listGaps.of(BlockType.CHECKBOX))
        assertNull(AppSettings.listGaps.of(BlockType.NUMBERED_LIST_ITEM))
    }
}
