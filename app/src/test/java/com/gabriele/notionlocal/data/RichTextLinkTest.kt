package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.applyTextEdit
import com.gabriele.notionlocal.data.entity.linkInRange
import com.gabriele.notionlocal.data.entity.linkRunAt
import com.gabriele.notionlocal.data.entity.setLinkInRange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **I collegamenti sul testo** (17/10/2026): l'indirizzo sta nel pezzo di
 * testo, si mette e si toglie su un intervallo, e scrivendo ai bordi di un
 * link il testo nuovo non ci finisce dentro.
 */
class RichTextLinkTest {

    private val google = "https://google.com"

    @Test
    fun oldTextReadsWithoutLinks() {
        val spans = Json.decodeFromString<List<RichTextSpan>>("""[{"text":"Ciao","bold":true}]""")
        assertEquals(listOf(RichTextSpan(text = "Ciao", bold = true)), spans)
        val saved = Json.encodeToString(listOf(RichTextSpan(text = "Cerca", link = google)))
        assertEquals(listOf(RichTextSpan(text = "Cerca", link = google)), Json.decodeFromString<List<RichTextSpan>>(saved))
    }

    @Test
    fun aLinkOnARangeAndOff() {
        val spans = listOf(RichTextSpan(text = "Cerca su Google adesso"))
        val linked = setLinkInRange(spans, 9, 15, google)
        assertEquals(
            listOf(RichTextSpan(text = "Cerca su "), RichTextSpan(text = "Google", link = google), RichTextSpan(text = " adesso")),
            linked
        )
        assertEquals(google, linkInRange(linked, 0, 10))
        assertNull(linkInRange(linked, 0, 9))
        assertEquals(spans, setLinkInRange(linked, 9, 15, null))
    }

    @Test
    fun typingAtTheEdgesDoesNotStretchTheLink() {
        val linked = listOf(RichTextSpan(text = "Vai su "), RichTextSpan(text = "Google", link = google))
        // In fondo al testo, subito dopo il link: fuori.
        assertEquals(
            listOf(RichTextSpan(text = "Vai su "), RichTextSpan(text = "Google", link = google), RichTextSpan(text = " ora")),
            applyTextEdit(linked, "Vai su Google", "Vai su Google ora")
        )
        // Subito prima: fuori.
        assertEquals(
            listOf(RichTextSpan(text = "Vai su il "), RichTextSpan(text = "Google", link = google)),
            applyTextEdit(linked, "Vai su Google", "Vai su il Google")
        )
        // Dentro: è il testo del link che si corregge.
        assertEquals(
            listOf(RichTextSpan(text = "Vai su "), RichTextSpan(text = "Gooogle", link = google)),
            applyTextEdit(linked, "Vai su Google", "Vai su Gooogle")
        )
    }

    @Test
    fun theLinkUnderTheCaret() {
        // Un link con dentro una parola in grassetto è un link solo.
        val spans = listOf(
            RichTextSpan(text = "Leggi "),
            RichTextSpan(text = "la ", link = google),
            RichTextSpan(text = "guida", link = google, bold = true),
            RichTextSpan(text = " qui")
        )
        assertEquals(6 until 14, linkRunAt(spans, 8))
        assertEquals(6 until 14, linkRunAt(spans, 14))
        assertEquals(6 until 14, linkRunAt(spans, 6))
        assertNull(linkRunAt(spans, 3))
        assertNull(linkRunAt(spans, 16))
    }
}
