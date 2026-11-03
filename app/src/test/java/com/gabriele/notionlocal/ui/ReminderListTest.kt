package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.ReminderList
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Le notifiche di una data sulla schermata** (26/10/2026): una riga per
 * ognuna, la X che la toglie, "Add notification" con le scelte rapide e
 * "Custom…" per "10 ore prima".
 */
@RunWith(RobolectricTestRunner::class)
// Uno schermo da telefono: in quello piccolo di Robolectric la finestra delle
// scelte scorre e "Custom…" resta sotto.
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp")
class ReminderListTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var reminders by mutableStateOf(listOf(0))

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext<Context>())
        compose.setContent {
            NotionLocalTheme {
                Column {
                    ReminderList(
                        reminders = reminders,
                        atStartLabel = "At the chosen time",
                        onChange = { reminders = it },
                        indent = 16.dp
                    )
                }
            }
        }
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun addAPresetAndACustomOneThenRemove() {
        assertEquals(true, shows("At the chosen time"))
        // Un giorno prima, dalle scelte rapide.
        compose.onNodeWithText("Add notification").performClick()
        compose.onNodeWithText("1 day before").performClick()
        compose.waitForIdle()
        assertEquals(listOf(0, 24 * 60), reminders)
        assertEquals(true, shows("1 day before"))
        // "Custom…": 10 ore prima.
        compose.onNodeWithText("Add notification").performClick()
        compose.onNodeWithText("Custom…").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextReplacement("10")
        compose.onNodeWithText("hours").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals(listOf(0, 600, 24 * 60), reminders)
        assertEquals(true, shows("10 hours before"))
        // La X toglie quella della sua riga: la seconda è "10 hours before".
        compose.onAllNodesWithContentDescription("Remove notification")[1].performClick()
        compose.waitForIdle()
        assertEquals(listOf(0, 24 * 60), reminders)
    }

    @Test
    fun theOnesAlreadyThereAreNotOffered() {
        reminders = listOf(0, 10)
        compose.onNodeWithText("Add notification").performClick()
        compose.waitForIdle()
        // "10 minutes before" c'è già: solo nella lista, non fra le scelte.
        assertEquals(1, compose.onAllNodesWithText("10 minutes before").fetchSemanticsNodes().size)
        assertEquals(true, shows("30 minutes before"))
    }

    @Test
    fun moreThanAYearIsNotAccepted() {
        compose.onNodeWithText("Add notification").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Custom…").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextReplacement("53")
        compose.onNodeWithText("weeks").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Done").assertIsNotEnabled()
        assertEquals(true, shows("At most one year before"))
    }
}
