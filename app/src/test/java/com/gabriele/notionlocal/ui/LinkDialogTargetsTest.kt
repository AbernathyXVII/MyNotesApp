package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.LinkTarget
import com.gabriele.notionlocal.data.entity.blockLink
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.LinkDialog
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"In this page"** nella finestra del collegamento (20/10/2026): le righe a
 * cui collegarsi, e un tocco su una di loro fa il collegamento.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
class LinkDialogTargetsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val targets = listOf(
        LinkTarget("b", "Capitolo 1", pinned = true, level = 0),
        LinkTarget("c", "Capitolo 2", pinned = false, level = 0)
    )
    private var saved: String? = null

    private fun show(current: String?, targets: List<LinkTarget> = this.targets) {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        compose.setContent {
            NotionLocalTheme {
                LinkDialog(current = current, targets = targets, onDismiss = {}, onSave = { url, _ -> saved = url }, onRemove = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun aTapOnALineOfThePageMakesTheLink() {
        show(current = null)
        // Si parte dal sito: l'elenco non c'è.
        compose.onNodeWithText("Capitolo 2").assertDoesNotExist()
        compose.onNodeWithText("In this page").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Capitolo 1").assertExists()
        compose.onNodeWithText("Capitolo 2").performClick()
        compose.waitForIdle()
        assertEquals(blockLink("c"), saved)
    }

    @Test
    fun aLinkToALineOpensOnThePage() {
        show(current = blockLink("b"))
        compose.onNodeWithText("Capitolo 1").assertExists()
        assertNull(saved)
    }

    @Test
    fun withNothingToLinkToItSaysHow() {
        show(current = null, targets = emptyList())
        compose.onNodeWithText("In this page").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Pin a line from its six-dot menu", substring = true).assertExists()
    }
}
