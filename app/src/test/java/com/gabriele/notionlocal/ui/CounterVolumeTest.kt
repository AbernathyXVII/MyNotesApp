package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CounterWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.screen.WidgetsSection
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La barra del volume dei click** (18/10/2026, chiesta dall'utente), nel
 * pannello "Sounds" del contatore: si sposta, si salva col contatore, e coi
 * suoni spenti non si tocca.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2400dp-xhdpi")
class CounterVolumeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var counterId: String

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSettings.init(context)
        WidgetStore.init(context)
        WidgetStore.add(WidgetKind.COUNTER)
        counterId = WidgetStore.state.widgets.filterIsInstance<CounterWidget>().last().id
        WidgetStore.state.widgets.filter { it.id != counterId }.forEach { WidgetStore.remove(it.id) }
    }

    @After
    fun tearDown() {
        WidgetStore.remove(counterId)
    }

    private fun counter() = WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == counterId }

    @Test
    fun theVolumeBarInTheSoundsSheet() {
        compose.setContent {
            NotionLocalTheme {
                Column(modifier = Modifier.width(300.dp).background(SidebarBackground)) { WidgetsSection(active = false) }
            }
        }
        compose.onNodeWithContentDescription(WidgetStrings.widgetOptions).performClick()
        compose.onNodeWithText(WidgetStrings.sounds).performClick()
        compose.waitForIdle()
        // Un contatore nuovo parte da tutto il volume.
        compose.onNodeWithText("100%").assertExists()
        compose.onNodeWithTag("counter-volume").assertIsEnabled()

        // Spostata la barra, il volume si salva col contatore.
        compose.onNodeWithTag("counter-volume").performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        compose.waitForIdle()
        assertEquals(30, counter().volume)
        compose.onNodeWithText("30%").assertExists()

        // Coi suoni spenti la barra non si tocca, e il volume resta.
        compose.onNodeWithText(WidgetStrings.soundsOn).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("counter-volume").assertIsNotEnabled()
        assertEquals(30, counter().volume)
    }
}
