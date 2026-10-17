package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.applyLinks
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import com.gabriele.notionlocal.ui.screen.matchAnchors
import com.gabriele.notionlocal.ui.screen.pastedLinks
import com.gabriele.notionlocal.ui.screen.withoutPastedLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Incollare coi link** (17/10/2026, chiesto dall'utente): il testo copiato
 * da OneNote o da un browser arriva negli appunti in chiaro e in HTML; i
 * link dell'HTML vanno sulle stesse parole del testo incollato.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PasteLinksTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Gli appunti di Compose che non sanno niente: tutto viene da quelli del telefono. */
    private val emptyBase = object : ClipboardManager {
        override fun setText(annotatedString: AnnotatedString) = Unit
        override fun getText(): AnnotatedString? = null
    }

    private fun copy(clip: ClipData) {
        context.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(clip)
    }

    private fun linksOf(text: AnnotatedString) = TextFieldValue(text).pastedLinks(0)

    @Test
    fun linksFromTheHtmlOfOneNote() {
        // Come OneNote: il testo semplice, e accanto lo stesso in HTML coi link.
        copy(
            ClipData.newHtmlText(
                "OneNote",
                "Vai su Google e poi su Bing.\nFine",
                "<p>Vai su <a href=\"https://www.google.com/\">Google</a> e poi su " +
                    "<a href='https://www.bing.com/'>Bing</a>.</p><p>Fine</p>"
            )
        )
        val pasted = LinkClipboard(context, emptyBase).getText()!!
        // Il testo è quello semplice, come prima.
        assertEquals("Vai su Google e poi su Bing.\nFine", pasted.text)
        assertEquals(
            listOf(LinkRange(7, 13, "https://www.google.com/"), LinkRange(23, 27, "https://www.bing.com/")),
            linksOf(pasted)
        )
    }

    @Test
    fun linksInsideTheText() {
        // Un'app che mette negli appunti il testo coi link dentro.
        val text = SpannableString("leggi qui e anche qui").apply {
            setSpan(URLSpan("https://a.example/"), 6, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(URLSpan("https://b.example/"), 18, 21, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        copy(ClipData.newPlainText("browser", text))
        val pasted = LinkClipboard(context, emptyBase).getText()!!
        assertEquals("leggi qui e anche qui", pasted.text)
        assertEquals(listOf(LinkRange(6, 9, "https://a.example/"), LinkRange(18, 21, "https://b.example/")), linksOf(pasted))
    }

    @Test
    fun plainTextStaysPlain() {
        copy(ClipData.newPlainText("note", "niente link"))
        val pasted = LinkClipboard(context, emptyBase).getText()!!
        assertEquals("niente link", pasted.text)
        assertEquals(emptyList<LinkRange>(), linksOf(pasted))
    }

    @Test
    fun theWordsOfTheLinksInOrder() {
        // La stessa parola due volte: ogni link al suo posto. Lo spazio che
        // non va a capo dell'HTML vale come uno spazio.
        val plain = "qui e qui, poi San Siro"
        val found = matchAnchors(
            listOf("qui" to "https://1/", "qui" to "https://2/", "San Siro" to "https://3/", "assente" to "https://4/"),
            plain
        )
        assertEquals(listOf(LinkRange(0, 3, "https://1/"), LinkRange(6, 9, "https://2/"), LinkRange(15, 23, "https://3/")), found)
    }

    @Test
    fun pastedIntoAFieldAndThenForgotten() {
        // Quello che fa l'incolla di Compose: il testo prima, l'incollato, il testo dopo.
        val pasted = AnnotatedString.Builder("Google").apply { addStringAnnotation("pasted-link", "https://g/", 0, 6) }.toAnnotatedString()
        val field = TextFieldValue(AnnotatedString("\nCerca su ") + pasted + AnnotatedString(" adesso"), TextRange(16))
        // Nelle posizioni del testo vero, senza l'a-capo nascosto in testa.
        assertEquals(listOf(LinkRange(9, 15, "https://g/")), field.pastedLinks(1))
        val clean = field.withoutPastedLinks()
        assertEquals(field.text, clean.text)
        assertEquals(field.selection, clean.selection)
        assertEquals(emptyList<LinkRange>(), clean.pastedLinks(1))
        // Senza link il valore resta lo stesso (la tastiera non se ne accorge).
        assertSame(clean, clean.withoutPastedLinks())
    }

    @Test
    fun linksOnTheRichText() {
        val spans = listOf(RichTextSpan(text = "Cerca su "), RichTextSpan(text = "Google adesso", bold = true))
        val linked = applyLinks(spans, listOf(LinkRange(9, 15, "https://g/"), LinkRange(20, 40, "https://fuori/")))
        assertEquals(spans.plainText(), linked.plainText())
        val link = linked.single { it.link == "https://g/" }
        assertEquals("Google", link.text)
        // Il grassetto resta.
        assertEquals(true, link.bold)
        // Il secondo cade fuori dal testo ("adesso" finisce a 22): resta solo il pezzo dentro.
        assertEquals("so", linked.single { it.link == "https://fuori/" }.text)
        assertNull(linked.first().link)
    }
}
