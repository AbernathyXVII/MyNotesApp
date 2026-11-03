package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.ui.screen.TextLikeCell
import com.gabriele.notionlocal.ui.screen.linkUri
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Le celle URL** (05/10/2026): con un indirizzo dentro, il tocco lo apre
 * e il tocco lungo dà "Edit link", "Copy link" e "Paste link"; vuote si
 * scrivono come prima.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
class CellLinkTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val clipboard get() = app.getSystemService(ClipboardManager::class.java)

    private val bulbapedia = "https://bulbapedia.bulbagarden.net/wiki/Bulbasaur_(Pok%C3%A9mon)"

    private fun cell(initial: String, onChange: (String) -> Unit = {}) {
        compose.setContent {
            NotionLocalTheme {
                var value by remember { mutableStateOf(initial) }
                Box(modifier = Modifier.width(380.dp)) {
                    TextLikeCell(
                        value = value,
                        onValueChange = {
                            value = it
                            onChange(it)
                        },
                        keyboardOptions = KeyboardOptions.Default,
                        isLink = true,
                        cellKey = "r:c"
                    )
                }
            }
        }
    }

    /** L'ultima attività fatta partire, dalla finestra di prova o dall'app. */
    private fun startedIntent(): Intent? =
        shadowOf(compose.activity).nextStartedActivity ?: shadowOf(app).nextStartedActivity

    @Test
    fun theAddressGetsHttpsOnlyWhenItHasNoBeginning() {
        assertEquals(bulbapedia, linkUri(bulbapedia))
        assertEquals("https://example.com/a", linkUri("  example.com/a "))
        assertEquals("http://example.com", linkUri("http://example.com"))
        assertEquals("mailto:someone@example.com", linkUri("mailto:someone@example.com"))
    }

    @Test
    fun aTapOpensTheLink() {
        cell(bulbapedia)
        compose.onNodeWithText(bulbapedia).performClick()
        compose.waitForIdle()
        val intent = startedIntent()
        assertEquals(Intent.ACTION_VIEW, intent?.action)
        assertEquals(bulbapedia, intent?.dataString)
    }

    @Test
    fun aLongPressGivesEditCopyAndPaste() {
        var last: String? = null
        cell(bulbapedia) { last = it }
        clipboard.setPrimaryClip(ClipData.newPlainText("x", "https://example.com/other\n"))

        compose.onNodeWithText(bulbapedia).performTouchInput { longClick() }
        compose.waitForIdle()
        // Nessun link aperto: il tocco lungo non è un tocco.
        assertNull(startedIntent())
        compose.onNodeWithText("Edit link").assertExists()

        compose.onNodeWithText("Copy link").performClick()
        compose.waitForIdle()
        assertEquals(bulbapedia, clipboard.primaryClip?.getItemAt(0)?.text.toString())

        // "Paste link": al suo posto quello copiato, senza l'a capo.
        clipboard.setPrimaryClip(ClipData.newPlainText("x", "https://example.com/other\n"))
        compose.onNodeWithText(bulbapedia).performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Paste link").performClick()
        compose.waitForIdle()
        assertEquals("https://example.com/other", last)
        compose.onNodeWithText("https://example.com/other").assertExists()
    }

    @Test
    fun editLinkLetsYouWriteInTheCell() {
        var last: String? = null
        cell("example.com") { last = it }
        compose.onNodeWithText("example.com").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Edit link").performClick()
        compose.waitForIdle()
        // Ora la cella è un campo di scrittura, col cursore in fondo.
        compose.onNode(hasSetTextAction()).performTextInput("/x")
        assertEquals("example.com/x", last)
    }

    @Test
    fun anEmptyLinkCellIsWrittenAsBefore() {
        var last: String? = null
        cell("") { last = it }
        // Vuota: il tocco non apre niente, fa scrivere.
        compose.onRoot().performClick()
        compose.waitForIdle()
        assertNull(startedIntent())
        compose.onNode(hasSetTextAction()).performTextInput("example.com")
        assertEquals("example.com", last)
    }
}
