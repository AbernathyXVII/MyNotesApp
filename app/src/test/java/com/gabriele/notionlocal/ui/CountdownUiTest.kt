package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CountdownEntry
import com.gabriele.notionlocal.data.widgets.CountdownWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.ui.screen.FlipTile
import com.gabriele.notionlocal.ui.screen.WidgetsSection
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * **Il widget Countdown disegnato** (27/10/2026): i foglietti coi nomi delle
 * unità, il countdown che si cambia tenendolo premuto, e quelli nuovi dai tre
 * puntini, uno sotto l'altro. L'immagine dei foglietti la fa `WidgetsRenderTest`
 * (disegnare la finestra a mano a metà prova la lasciava ferma).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CountdownUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        AppSettings.init(context)
        WidgetStore.init(context)
        // Solo il Countdown, per trovarne i tre puntini.
        WidgetStore.state.widgets.forEach { WidgetStore.remove(it.id) }
    }

    private fun countdowns() = WidgetStore.state.widgets.filterIsInstance<CountdownWidget>().single().countdowns

    @Test
    fun aLongPressEditsAndTheMenuAddsOneBelow() {
        WidgetStore.add(WidgetKind.COUNTDOWN, CountdownEntry(name = "Vacanze", target = "2030-08-01T00:00:00"))
        compose.setContent {
            NotionLocalTheme {
                Column(modifier = Modifier.width(300.dp).background(SidebarBackground)) {
                    WidgetsSection(active = false)
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Vacanze").assertExists()
        listOf("Months", "Weeks", "Days", "Hours", "Minutes", "Seconds").forEach {
            assertTrue(it, compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty())
        }

        // Tenendolo premuto si cambia: l'anno, anche nel passato.
        compose.onNodeWithText("Vacanze").performTouchInput { longClick() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Edit countdown").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction() and hasText("2030")).performTextReplacement("2020")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals(LocalDateTime.parse("2020-08-01T00:00:00"), countdowns().single().targetTime)

        // Dai tre puntini uno nuovo, che va sotto.
        compose.onNodeWithContentDescription("Widget options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Add countdown").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Esame")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Vacanze", "Esame"), countdowns().map { it.name })
        val first = compose.onNodeWithText("Vacanze").fetchSemanticsNode().positionInRoot.y
        val second = compose.onNodeWithText("Esame").fetchSemanticsNode().positionInRoot.y
        assertTrue("uno sotto l'altro: $first, $second", second > first + 40f)

        // Tenendolo premuto si può anche togliere.
        compose.onNodeWithText("Esame").performTouchInput { longClick() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Delete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Vacanze"), countdowns().map { it.name })
    }

    @Test
    fun aTileFlipsToTheNewNumber() {
        var value by mutableStateOf("40")
        compose.setContent {
            NotionLocalTheme {
                FlipTile(text = value, modifier = Modifier.width(48.dp).height(36.dp))
            }
        }
        compose.waitForIdle()
        // Fermo: il numero sulle due metà del foglietto.
        assertEquals(2, compose.onAllNodesWithText("40").fetchSemanticsNodes().size)
        assertTrue(compose.onAllNodesWithText("39").fetchSemanticsNodes().isEmpty())
        // Cambiato il numero, a metà corsa si vedono tutti e due: il vecchio
        // che cade, il nuovo che c'è già sotto.
        compose.mainClock.autoAdvance = false
        value = "39"
        // Senza far andare l'orologio: il numero nuovo arriva e il foglietto parte.
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(150)
        assertTrue(compose.onAllNodesWithText("40").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("39").fetchSemanticsNodes().isNotEmpty())
        // Alla fine solo il nuovo, sulle due metà.
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("40").fetchSemanticsNodes().isEmpty())
        assertEquals(2, compose.onAllNodesWithText("39").fetchSemanticsNodes().size)
    }
}
