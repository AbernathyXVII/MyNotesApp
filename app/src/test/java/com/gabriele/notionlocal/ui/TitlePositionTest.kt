package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.TitlePosition
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
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
 * **"Title position"** (05/10/2026): in Settings, sopra "Animations", per
 * tutte le pagine; di partenza il titolo resta sotto l'icona.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class TitlePositionTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        AppSettings.titlePosition = TitlePosition.BELOW_ICON
    }

    @Test
    fun itSitsAboveAnimationsAndTheChoiceIsSaved() {
        assertEquals(TitlePosition.BELOW_ICON, AppSettings.titlePosition)
        compose.setContent { NotionLocalTheme { SettingsScreen(onBack = {}) } }
        val title = compose.onNodeWithText("Title position").getUnclippedBoundsInRoot()
        val animations = compose.onNodeWithText("Animations").getUnclippedBoundsInRoot()
        assertTrue(title.top < animations.top)
        compose.onNodeWithText("Below the icon").assertExists()

        compose.onNodeWithText("Title position").performClick()
        compose.onNodeWithText("Next to the icon").performClick()
        compose.waitForIdle()
        assertEquals(TitlePosition.BESIDE_ICON, AppSettings.titlePosition)
    }
}
