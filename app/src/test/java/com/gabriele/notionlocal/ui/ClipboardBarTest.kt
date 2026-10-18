package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.PasteFormat
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.StyleRange
import com.gabriele.notionlocal.data.entity.applyPaste
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.data.entity.sliceSpans
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import com.gabriele.notionlocal.ui.screen.PasteMode
import com.gabriele.notionlocal.ui.screen.htmlStyleRuns
import com.gabriele.notionlocal.ui.screen.leadOutOfSelection
import com.gabriele.notionlocal.ui.screen.pastedFormat
import com.gabriele.notionlocal.ui.screen.sendCtrlShortcut
import com.gabriele.notionlocal.ui.screen.spansToHtml
import com.gabriele.notionlocal.ui.screen.withoutPastedLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Copia, incolla, taglia e seleziona tutto dalla barra** (18/10/2026,
 * chiesti dall'utente) e i tre modi di incollare di Word: i pulsanti mandano
 * Ctrl+C/V/X/A al campo che ha il fuoco, e l'incolla porta con sé lo stile
 * che il modo chiede.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ClipboardBarTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val system get() = context.getSystemService(android.content.ClipboardManager::class.java)

    private val emptyBase = object : ClipboardManager {
        override fun setText(annotatedString: AnnotatedString) {
            system.setPrimaryClip(ClipData.newPlainText("test", annotatedString.text))
        }
        override fun getText(): AnnotatedString? = system.primaryClip?.getItemAt(0)?.text?.let { AnnotatedString(it.toString()) }
    }

    /** Il campo di prova, col fuoco: [value] è quello che ha adesso. */
    private var value by mutableStateOf(TextFieldValue(""))
    private lateinit var clipboard: LinkClipboard

    private fun showField(
        start: String,
        rich: List<RichTextSpan>? = null,
        adjust: (TextFieldValue) -> TextFieldValue = { it }
    ) {
        value = TextFieldValue(start, TextRange(start.length))
        clipboard = LinkClipboard(context, emptyBase) { rich }
        compose.setContent {
            val focus = remember { FocusRequester() }
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                BasicTextField(value = value, onValueChange = { value = adjust(it) }, modifier = Modifier.focusRequester(focus))
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        compose.waitForIdle()
    }

    /** Svuota il campo, che resta lo stesso e col fuoco (`setContent` si chiama una volta sola). */
    private fun clear() {
        compose.runOnUiThread { value = TextFieldValue("", TextRange(0)) }
        compose.waitForIdle()
    }

    private fun press(keyCode: Int) {
        compose.runOnUiThread { compose.activity.window.decorView.sendCtrlShortcut(keyCode) }
        compose.waitForIdle()
    }

    @Test
    fun selectAllCopyCutAndPasteFromTheBar() {
        showField("Ciao mondo")
        press(KeyEvent.KEYCODE_A)
        assertEquals(TextRange(0, 10), value.selection)
        press(KeyEvent.KEYCODE_C)
        assertEquals("Ciao mondo", system.primaryClip!!.getItemAt(0).text.toString())
        press(KeyEvent.KEYCODE_X)
        assertEquals("", value.text)
        clipboard.nextPasteMode = PasteMode.MATCH_DESTINATION
        press(KeyEvent.KEYCODE_V)
        assertEquals("Ciao mondo", value.text)
        // Dopo un incolla si torna al modo di sempre.
        assertEquals(PasteMode.LINKS_ONLY, clipboard.nextPasteMode)
    }

    @Test
    fun selectAllLeavesTheHiddenLineBreakAlone() {
        // Come i campi dell'editor, con l'a-capo nascosto in testa. Sul
        // telefono "Select all" lo prendeva: copiato e incollato in fondo
        // faceva una riga nuova.
        showField("\nCiao mondo", adjust = { it.leadOutOfSelection("\n") })
        press(KeyEvent.KEYCODE_A)
        assertEquals(TextRange(1, 11), value.selection)
        press(KeyEvent.KEYCODE_C)
        assertEquals("Ciao mondo", system.primaryClip!!.getItemAt(0).text.toString())
        // Tagliato tutto, l'a-capo resta: non è un backspace a inizio riga.
        press(KeyEvent.KEYCODE_X)
        assertEquals("\n", value.text)
        // Il cursore da solo non si tocca, nemmeno a zero.
        val atZero = TextFieldValue("\nx", TextRange(0))
        assertEquals(atZero, atZero.leadOutOfSelection("\n"))
        // Una selezione all'indietro resta all'indietro.
        assertEquals(TextRange(3, 1), TextFieldValue("\nxy", TextRange(3, 0)).leadOutOfSelection("\n").selection)
    }

    @Test
    fun copyingFormattedTextPutsItsHtmlInTheClipboard() {
        val rich = listOf(RichTextSpan("Ciao "), RichTextSpan("mondo", bold = true, link = "https://m/"))
        showField("Ciao mondo", rich)
        press(KeyEvent.KEYCODE_A)
        press(KeyEvent.KEYCODE_C)
        val item = system.primaryClip!!.getItemAt(0)
        assertEquals("Ciao mondo", item.text.toString())
        assertEquals("Ciao <a href=\"https://m/\"><b>mondo</b></a>", item.htmlText)
    }

    @Test
    fun theThreeWaysToPaste() {
        // Come Word, Google Docs o OneNote: il testo e accanto l'HTML.
        system.setPrimaryClip(
            ClipData.newHtmlText(
                "src",
                "Uno due tre",
                "<p>Uno <b>due</b> <a href='https://t/'><i>tre</i></a></p>"
            )
        )
        showField("")
        // Keep Source Formatting: tutto ripulito, poi lo stile e il link di chi l'ha copiato.
        clipboard.nextPasteMode = PasteMode.KEEP_SOURCE
        press(KeyEvent.KEYCODE_V)
        val source = value.pastedFormat(0)
        assertEquals(listOf(0 until 11), source.plain)
        assertEquals(listOf(StyleRange(4, 7, bold = true), StyleRange(8, 11, italic = true)), source.styles)
        assertEquals(listOf(LinkRange(8, 11, "https://t/")), source.links)
        // Match Destination Formatting: testo e basta, che prende lo stile di dove arriva.
        clear()
        clipboard.nextPasteMode = PasteMode.MATCH_DESTINATION
        press(KeyEvent.KEYCODE_V)
        assertTrue(value.pastedFormat(0).isEmpty)
        // Keep Text Only: ripulito, senza stile né link.
        clear()
        clipboard.nextPasteMode = PasteMode.TEXT_ONLY
        press(KeyEvent.KEYCODE_V)
        assertEquals(PasteFormat(plain = listOf(0 until 11)), value.pastedFormat(0))
        // Il "Paste" di sempre: i link sì, lo stile no.
        clear()
        press(KeyEvent.KEYCODE_V)
        assertEquals(PasteFormat(links = listOf(LinkRange(8, 11, "https://t/"))), value.pastedFormat(0))
        assertTrue(value.withoutPastedLinks().pastedFormat(0).isEmpty)
    }

    @Test
    fun theFormattingLandsOnTheRichText() {
        // "Ciao " in grassetto (la destinazione), poi "due tre" incollato in mezzo.
        val destination = listOf(RichTextSpan("Ciao due tre", bold = true))
        // Keep Source: "due" perde il grassetto di dove arriva, "tre" diventa corsivo.
        val source = applyPaste(
            destination,
            PasteFormat(plain = listOf(5 until 12), styles = listOf(StyleRange(9, 12, italic = true)))
        )
        assertEquals(listOf(RichTextSpan("Ciao ", bold = true), RichTextSpan("due "), RichTextSpan("tre", italic = true)), source)
        // Keep Text Only: niente.
        assertEquals(
            listOf(RichTextSpan("Ciao ", bold = true), RichTextSpan("due tre")),
            applyPaste(destination, PasteFormat(plain = listOf(5 until 12)))
        )
        assertEquals(listOf(RichTextSpan("o d", bold = true)), sliceSpans(destination, 3, 6))
        assertEquals("Ciao due tre", source.plainText())
    }

    @Test
    fun stylesAsOneNoteWordAndGoogleDocsWriteThem() {
        // Google Docs: tutto dentro un <b> "normale", il grassetto nello stile degli span.
        val docs = htmlStyleRuns(
            "<meta charset='utf-8'><b style=\"font-weight:normal;\" id=\"docs-internal-guid\">" +
                "<span style=\"font-weight:400\">Uno </span><span style=\"font-weight:700\">due</span>" +
                "<span style=\"font-style:italic\"> tre</span></b>"
        )
        assertEquals(listOf("due" to "b", "tre" to "i"), docs.map { it.text to code(it) })
        // OneNote e Word: lo stile negli span, dentro il frammento copiato.
        val oneNote = htmlStyleRuns(
            "<html><body><!--StartFragment--><p><span style='font-weight:bold'>Lista</span> della " +
                "<span style='text-decoration:underline'>spesa</span> &amp; " +
                "<span style='text-decoration:line-through'>vecchia</span></p><!--EndFragment--></body></html>"
        )
        assertEquals(listOf("Lista" to "b", "spesa" to "u", "vecchia" to "s"), oneNote.map { it.text to code(it) })
        assertEquals(emptyList<Any>(), htmlStyleRuns("<p>solo testo</p>"))
        assertEquals("a &amp; <b>b</b><br>c", spansToHtml(listOf(RichTextSpan("a & "), RichTextSpan("b", bold = true), RichTextSpan("\nc"))))
        assertFalse(spansToHtml(listOf(RichTextSpan("x"))).contains("<"))
    }

    private fun code(run: com.gabriele.notionlocal.ui.screen.StyledRun) = buildString {
        if (run.bold) append('b')
        if (run.italic) append('i')
        if (run.underline) append('u')
        if (run.strikethrough) append('s')
    }
}
