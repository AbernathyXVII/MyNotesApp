package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Font e corpo sul testo selezionato: solo lì, senza toccare il resto né la formattazione che c'era. */
class RichTextFontTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun fontOnlyOnTheSelection() {
        val spans = setFontInRange(listOf(RichTextSpan("uno due tre")), 4, 7, "GARAMOND")
        assertEquals(
            listOf(RichTextSpan("uno "), RichTextSpan("due", font = "GARAMOND"), RichTextSpan(" tre")),
            spans
        )
    }

    @Test
    fun twoSelectionsTwoFonts() {
        // La richiesta dell'utente: un pezzo in un font, un altro pezzo in un altro.
        val first = setFontInRange(listOf(RichTextSpan("uno due tre")), 0, 3, "GARAMOND")
        val second = setFontInRange(first, 8, 11, "CONSOLAS")
        assertEquals(
            listOf(
                RichTextSpan("uno", font = "GARAMOND"),
                RichTextSpan(" due "),
                RichTextSpan("tre", font = "CONSOLAS")
            ),
            second
        )
    }

    @Test
    fun fontKeepsBoldAndColor() {
        val spans = setFontInRange(
            listOf(RichTextSpan("ciao", bold = true, color = "#E03E3E")),
            0,
            4,
            "ARIAL"
        )
        assertEquals(listOf(RichTextSpan("ciao", bold = true, color = "#E03E3E", font = "ARIAL")), spans)
    }

    @Test
    fun backToTheSystemFontMergesThePieces() {
        val on = setFontInRange(listOf(RichTextSpan("abc")), 1, 2, "GEORGIA")
        assertEquals(listOf(RichTextSpan("abc")), setFontInRange(on, 1, 2, null))
    }

    @Test
    fun sizeOnlyOnTheSelectionAnd16IsNoSize() {
        val big = setSizeInRange(listOf(RichTextSpan("abcd")), 2, 4, 32, defaultSize = 16)
        assertEquals(listOf(RichTextSpan("ab"), RichTextSpan("cd", size = 32)), big)
        // Il 16 di sempre non si scrive: i pezzi tornano uno solo.
        assertEquals(listOf(RichTextSpan("abcd")), setSizeInRange(big, 2, 4, 16, defaultSize = 16))
    }

    @Test
    fun emptySelectionChangesNothing() {
        val spans = listOf(RichTextSpan("abc"))
        assertEquals(spans, setFontInRange(spans, 1, 1, "GARAMOND"))
        assertEquals(spans, setSizeInRange(spans, 2, 2, 40, defaultSize = 16))
    }

    @Test
    fun spanAtFindsTheCharacter() {
        val spans = listOf(RichTextSpan("ab"), RichTextSpan("cd", font = "CALIBRI"))
        assertEquals("ab", spanAt(spans, 1)?.text)
        assertEquals("CALIBRI", spanAt(spans, 2)?.font)
        assertNull(spanAt(spans, 4))
        assertNull(spanAt(spans, -1))
    }

    @Test
    fun oldTextWithoutFontStillReads() {
        // Salvato prima del 25/09/2026: nessun font né corpo nel testo.
        val spans = json.decodeFromString<List<RichTextSpan>>("""[{"text":"vecchio","italic":true}]""")
        assertEquals(listOf(RichTextSpan("vecchio", italic = true)), spans)
        // E un testo con font e corpo va e torna uguale.
        val styled = listOf(RichTextSpan("nuovo", font = "FUTURA", size = 24))
        assertEquals(styled, json.decodeFromString<List<RichTextSpan>>(json.encodeToString(styled)))
    }
}
