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
import com.gabriele.notionlocal.data.settings.withFavoriteColorToggled
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
 * **I colori preferiti** (20/10/2026): la stella accanto all'anteprima del
 * selettore aggiunge il colore scelto, o lo toglie; la loro fila sta sotto
 * quella dei recenti e toccandone uno il selettore va su quel colore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class FavoriteColorsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    /** Il campo Hex: fra quelli in cui si scrive (anche Rosso, Verde e Blu), quello col "#". */
    private fun hexField() = compose.onNode(hasSetTextAction() and hasText("#", substring = true))

    @Test
    fun theStarAddsAtTheEndAndRemoves() {
        var list = emptyList<String>()
        list = list.withFavoriteColorToggled("#112233")
        // In minuscolo o senza # è lo stesso colore, e va in fondo.
        list = list.withFavoriteColorToggled("aabbcc")
        assertEquals(listOf("#112233", "#AABBCC"), list)
        // Toccata di nuovo, la stella lo toglie.
        assertEquals(listOf("#AABBCC"), list.withFavoriteColorToggled("#112233"))
        // Quello che non è un colore non entra.
        assertEquals(list, list.withFavoriteColorToggled("rosso"))
        // Piena, non ne entra un altro (e nessuno se ne va da solo).
        val full = (1..3).fold(emptyList<String>()) { acc, i -> acc.withFavoriteColorToggled("#0000%02X".format(i), max = 3) }
        assertEquals(full, full.withFavoriteColorToggled("#FFFFFF", max = 3))
        assertEquals(listOf("#000001", "#000003"), full.withFavoriteColorToggled("#000002", max = 3))
    }

    @Test
    fun aFavoriteIsKeptAndCanBePickedAgain() {
        compose.setContent {
            NotionLocalTheme {
                ColorPickerSheet(
                    initialHex = "#336699",
                    background = false,
                    onDismiss = {},
                    onSwitchTarget = {},
                    onPick = {}
                )
            }
        }
        // Senza preferiti, la riga dice come metterne uno.
        compose.onNodeWithText("Favorite colors").assertExists()
        compose.onNodeWithText("Tap the star to keep the current color here").assertExists()

        // La stella: il colore scelto va fra i preferiti, e la stella si accende.
        compose.onNodeWithContentDescription("Add to favorite colors").performClick()
        compose.waitForIdle()
        assertEquals(listOf("#336699"), AppSettings.favoriteColors)
        compose.onNodeWithContentDescription("Remove from favorite colors").assertExists()
        compose.onNodeWithText("Tap the star to keep the current color here").assertDoesNotExist()

        // Su un altro colore la stella è spenta; il tondo del preferito lo rimette.
        hexField().performTextReplacement("#000000")
        compose.onNodeWithContentDescription("Add to favorite colors").assertExists()
        compose.onNodeWithContentDescription("#336699").performClick()
        compose.waitForIdle()
        hexField().assert(hasText("#336699"))

        // I preferiti si ricordano: rilette le impostazioni, ci sono ancora.
        AppSettings.init(ApplicationProvider.getApplicationContext())
        assertEquals(listOf("#336699"), AppSettings.favoriteColors)

        // Toccata di nuovo, la stella lo toglie.
        compose.onNodeWithContentDescription("Remove from favorite colors").performClick()
        compose.waitForIdle()
        assertEquals(emptyList<String>(), AppSettings.favoriteColors)
        compose.onNodeWithText("Tap the star to keep the current color here").assertExists()
    }
}
