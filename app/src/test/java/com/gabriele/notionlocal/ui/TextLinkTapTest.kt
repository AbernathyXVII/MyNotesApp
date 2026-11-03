package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.linkStyleOf
import com.gabriele.notionlocal.ui.screen.linkTaps
import com.gabriele.notionlocal.ui.screen.markLink
import com.gabriele.notionlocal.ui.screen.textLinkTarget
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **Toccando un testo con un collegamento si apre il link** (17/10/2026), e
 * il campo non prende il cursore; toccando il resto del testo si scrive come
 * sempre. E come si legge l'indirizzo scritto nella finestrella.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextLinkTapTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val google = "https://google.com"
    private val spans = listOf(
        RichTextSpan(text = "Cerca su "),
        RichTextSpan(text = "Google", link = google),
        RichTextSpan(text = " adesso")
    )

    /** Come i campi dell'editor: gli span disegnati, col link segnato. */
    private val linked = VisualTransformation { text ->
        val builder = AnnotatedString.Builder()
        var pos = 0
        for (span in spans) {
            builder.append(span.text)
            linkStyleOf(span)?.let { builder.addStyle(it, pos, pos + span.text.length) }
            builder.markLink(span, pos, pos + span.text.length)
            pos += span.text.length
        }
        check(builder.length == text.length)
        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    @Test
    fun aTapOnTheLinkOpensItAndTheRestStillWrites() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        val opened = mutableListOf<String>()
        var focused = false
        var lastLayout: TextLayoutResult? = null
        compose.setContent {
            NotionLocalTheme {
                var value by remember { mutableStateOf("Cerca su Google adesso") }
                var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                Box(modifier = Modifier.width(400.dp)) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        visualTransformation = linked,
                        onTextLayout = {
                            layout = it
                            lastLayout = it
                        },
                        modifier = Modifier
                            .testTag("field")
                            .linkTaps(layout = { layout }) { opened += it }
                            .onFocusChanged { focused = it.isFocused }
                    )
                }
            }
        }
        compose.waitForIdle()

        // Un tocco su "Google": si apre, e il campo resta senza cursore.
        val onGoogle = lastLayout!!.getBoundingBox(11).center
        compose.onNodeWithTag("field").performTouchInput { click(onGoogle) }
        compose.waitForIdle()
        assertEquals(listOf(google), opened)
        assertFalse(focused)

        // Un tocco su "Cerca": si scrive, niente link.
        val onCerca = lastLayout!!.getBoundingBox(2).center
        compose.onNodeWithTag("field").performTouchInput { click(onCerca) }
        compose.waitForIdle()
        assertEquals(listOf(google), opened)
        assertTrue(focused)
    }

    @Test
    fun whatTheDialogAccepts() {
        assertEquals("https://google.com", textLinkTarget(" google.com "))
        assertEquals("https://it.wikipedia.org/wiki/Roma", textLinkTarget("https://it.wikipedia.org/wiki/Roma"))
        assertEquals("http://example.com", textLinkTarget("http://example.com"))
        assertEquals("mailto:mario.rossi@gmail.com", textLinkTarget("mario.rossi@gmail.com"))
        assertEquals("tel:+390612345678", textLinkTarget("tel:+390612345678"))
        assertNull(textLinkTarget("ciao"))
        assertNull(textLinkTarget("due parole.com"))
        assertNull(textLinkTarget(""))
    }
}
