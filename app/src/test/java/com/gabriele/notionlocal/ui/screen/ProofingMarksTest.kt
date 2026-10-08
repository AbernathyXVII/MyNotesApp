package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.text.AnnotatedString
import com.gabriele.notionlocal.data.entity.RichTextSpan
import org.junit.Assert.assertEquals
import org.junit.Test

/** I segni del correttore nel testo mostrato: vedi `ProofingUi.kt`. */
class ProofingMarksTest {

    private fun marks(builder: AnnotatedString.Builder) =
        builder.toAnnotatedString().let { text -> text.getStringAnnotations(PROOFING_TAG, 0, text.length) }

    @Test
    fun aWordIsMarkedWithItsLineAndPlace() {
        // Una riga con davanti un prefisso di tre caratteri (il pallino e gli spazi).
        val b = AnnotatedString.Builder("•  vado a squola")
        b.markMisspellings(line = 4, ranges = listOf(7..12), at = 3, spans = null)
        val m = marks(b).single()
        assertEquals(10, m.start)
        assertEquals(16, m.end)
        assertEquals(MisspelledWord(4, 7, 13), MisspelledWord.parse(m.item))
    }

    @Test
    fun spoilersAndTheWordBeingTypedAreNotMarked() {
        val text = "squola perche aqua"
        val spans = listOf(RichTextSpan("squola "), RichTextSpan("perche", spoiler = true), RichTextSpan(" aqua"))
        val b = AnnotatedString.Builder(text)
        // Il cursore è appena dopo "aqua": la si sta scrivendo.
        b.markMisspellings(0, listOf(0..5, 7..12, 14..17), 0, spans, typingEnd = 18)
        assertEquals(listOf("0:0:6"), marks(b).map { it.item })
    }

    @Test
    fun theTitleKeepsItsTextAndGetsTheMarks() {
        val shown = ProofingTransformation(listOf(0..5), typingEnd = null).filter(AnnotatedString("squola nuova"))
        assertEquals("squola nuova", shown.text.text)
        assertEquals(listOf("0:0:6"), shown.text.getStringAnnotations(PROOFING_TAG, 0, 12).map { it.item })
    }
}
