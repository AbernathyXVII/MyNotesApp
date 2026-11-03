package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Pedice e apice: si accendono su un pezzo, si escludono, e non rompono il testo già salvato. */
class RichTextScriptTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sub = { s: RichTextSpan, v: Boolean -> s.withSubscript(v) }
    private val sup = { s: RichTextSpan, v: Boolean -> s.withSuperscript(v) }

    @Test
    fun subscriptOnlyOnTheSelection() {
        val spans = toggleFormatInRange(listOf(RichTextSpan("H2O")), 1, 2, { it.subscript }, sub)
        assertEquals(
            listOf(RichTextSpan("H"), RichTextSpan("2", subscript = true), RichTextSpan("O")),
            spans
        )
    }

    @Test
    fun superscriptReplacesSubscript() {
        val subbed = toggleFormatInRange(listOf(RichTextSpan("x2")), 1, 2, { it.subscript }, sub)
        val supped = toggleFormatInRange(subbed, 1, 2, { it.superscript }, sup)
        assertEquals(listOf(RichTextSpan("x"), RichTextSpan("2", superscript = true)), supped)
        // E al contrario.
        val back = toggleFormatInRange(supped, 1, 2, { it.subscript }, sub)
        assertEquals(listOf(RichTextSpan("x"), RichTextSpan("2", subscript = true)), back)
    }

    @Test
    fun secondTapRemovesIt() {
        val on = toggleFormatInRange(listOf(RichTextSpan("mc2")), 2, 3, { it.superscript }, sup)
        val off = toggleFormatInRange(on, 2, 3, { it.superscript }, sup)
        assertEquals(listOf(RichTextSpan("mc2")), off)
    }

    @Test
    fun keepsTheOtherFormatting() {
        val spans = toggleFormatInRange(listOf(RichTextSpan("a2", bold = true, color = "#FF0000")), 1, 2, { it.superscript }, sup)
        assertEquals(RichTextSpan("2", bold = true, color = "#FF0000", superscript = true), spans.last())
    }

    @Test
    fun typingInsideContinuesIt() {
        val spans = listOf(RichTextSpan("x"), RichTextSpan("2", superscript = true))
        val edited = applyTextEdit(spans, "x2", "x23")
        assertEquals(listOf(RichTextSpan("x"), RichTextSpan("23", superscript = true)), edited)
    }

    @Test
    fun oldSavedTextStillReads() {
        // Com'era salvato prima: senza le due chiavi.
        val old = json.decodeFromString<List<RichTextSpan>>("""[{"text":"Ciao","bold":true}]""")
        assertFalse(old.single().subscript)
        assertFalse(old.single().superscript)
        // E il testo senza pedice né apice si salva come prima, senza chiavi nuove.
        assertEquals("""[{"text":"Ciao","bold":true}]""", json.encodeToString(old))
        val withSup = listOf(RichTextSpan("2", superscript = true))
        assertEquals(withSup, json.decodeFromString<List<RichTextSpan>>(json.encodeToString(withSup)))
    }
}
