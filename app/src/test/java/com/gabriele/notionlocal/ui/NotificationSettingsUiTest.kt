package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.ui.screen.SidebarActions
import com.gabriele.notionlocal.ui.screen.SidebarContent
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Le notifiche in Settings** (05/10/2026): le scelte della vecchia
 * pagina "Notifications" stanno tutte in Settings, la barra laterale non
 * ha più la voce, e fra i suoni ci sono anche le suonerie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class NotificationSettingsUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun settingsHasEveryNotificationChoice() {
        compose.setContent { NotionLocalTheme { SettingsScreen(onBack = {}) } }
        listOf(
            "Notification sound",
            "Vibration",
            "Priority",
            "App icon badge",
            "Send a test notification",
            "Turn off notifications",
            "Turn off for a while"
        ).forEach { compose.onNodeWithText(it).performScrollTo().assertExists() }
        // La sezione si chiama come la pagina di prima, non più "App sounds".
        compose.onNodeWithText("Notifications").assertExists()
        compose.onNodeWithText("App sounds").assertDoesNotExist()
    }

    @Test
    fun theSoundCanBeARingtone() {
        compose.setContent { NotionLocalTheme { SettingsScreen(onBack = {}) } }
        compose.onNodeWithText("Notification sound").performScrollTo().performClick()
        compose.onNodeWithText("Notification sounds (default and downloaded)").assertExists()
        compose.onNodeWithText("Ringtones (default and downloaded)").performClick()
        compose.waitForIdle()
        val intent: Intent = shadowOf(compose.activity).nextStartedActivityForResult.intent
        assertEquals(RingtoneManager.ACTION_RINGTONE_PICKER, intent.action)
        assertEquals(RingtoneManager.TYPE_RINGTONE, intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1))
    }

    @Test
    fun theSidebarHasNoNotificationsItem() {
        val nothing = {}
        compose.setContent {
            NotionLocalTheme {
                SidebarContent(
                    root = null,
                    children = emptyMap(),
                    expanded = emptySet(),
                    currentPageId = null,
                    actions = SidebarActions(
                        onMainMenu = nothing,
                        onFavorites = nothing,
                        onOpenNode = {},
                        onToggleNode = {},
                        onSearch = nothing,
                        onStartup = nothing,
                        onTemplates = nothing,
                        onTrash = nothing,
                        onSettings = nothing
                    ),
                    widgetsActive = false
                )
            }
        }
        compose.onNodeWithText("Settings").performScrollTo().assertExists()
        compose.onNodeWithText("Trash").assertExists()
        compose.onNodeWithText("Notifications").assertDoesNotExist()
    }
}
