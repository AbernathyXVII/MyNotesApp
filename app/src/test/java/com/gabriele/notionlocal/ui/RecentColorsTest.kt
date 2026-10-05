package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.withRecentColor
import com.gabriele.notionlocal.ui.screen.ColorPickerSheet
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Gli ultimi colori usati** (05/10/2026): sotto il campo Hex del
 * selettore, i dieci colori applicati più di recente; toccandone uno il
 * selettore va su quel colore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class RecentColorsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    /** Il campo Hex: fra quelli in cui si scrive (anche Rosso, Verde e Blu), quello col "#". */
    private fun hexField() = compose.onNode(hasSetTextAction() and hasText("#", substring = true))

    @Test
    fun theListKeepsTheLastTenWithoutDuplicates() {
        var list = emptyList<String>()
        (1..12).forEach { list = list.withRecentColor("#0000%02X".format(it)) }
        assertEquals(10, list.size)
        assertEquals("#00000C", list.first())
        assertEquals("#000003", list.last())
        // Un colore già usato sale in cima, una volta sola; in minuscolo o senza # è lo stesso.
        list = list.withRecentColor("00000a")
        assertEquals("#00000A", list.first())
        assertEquals(10, list.size)
        assertEquals(1, list.count { it == "#00000A" })
        // Quello che non è un colore non entra.
        assertEquals(list, list.withRecentColor("rosso"))
    }

    @Test
    fun applyRemembersTheColor_andARecentColorCanBePickedAgain() {
        var picked: String? = null
        var open = true
        compose.setContent {
            NotionLocalTheme {
                if (open) {
                    ColorPickerSheet(
                        initialHex = "#336699",
                        background = false,
                        onDismiss = {},
                        onSwitchTarget = {},
                        onPick = { picked = it }
                    )
                }
            }
        }
        // Un colore scritto in Hex, poi "Apply": va in cima ai recenti.
        hexField().performTextReplacement("#12AB34")
        compose.onNodeWithText("Apply").performClick()
        compose.waitForIdle()
        assertEquals("#12AB34", picked)
        assertEquals("#12AB34", AppSettings.recentColors.first())

        // Riaperto su un altro colore, il tondo recente lo rimette.
        hexField().performTextReplacement("#000000")
        compose.onNodeWithText("Recent colors").assertExists()
        compose.onNodeWithContentDescription("#12AB34").performClick()
        compose.waitForIdle()
        hexField().assert(hasText("#12AB34"))
    }
}
