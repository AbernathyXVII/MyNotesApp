package com.gabriele.notionlocal.data.proofing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProofingTextTest {

    private fun words(text: String) = ProofingText.words(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun plainSentence() {
        assertEquals(listOf("Questa", "frase", "scritta", "bene"), words("Questa è una frase scritta bene.").filter { it.length > 3 })
        assertEquals(listOf("Ciao", "come", "stai"), words("Ciao, come stai?"))
    }

    @Test
    fun apostrophesAndHyphensStayInside() {
        assertEquals(listOf("dell'anno", "l’homme", "well-known"), words("dell'anno l’homme well-known"))
        // Fuori dalla parola non contano: virgolette e trattini lunghi.
        assertEquals(listOf("ciao", "mondo"), words("'ciao' -mondo-"))
    }

    @Test
    fun skipsAddressesFilesAndCode() {
        assertEquals(
            listOf("scrivi", "apri"),
            words("scrivi a mario@example.com, apri https://notion.so www.google.com notes.txt z.B. a=b C++ #todo snake_case")
        )
    }

    @Test
    fun skipsNumbersCapsSingleLettersAndOtherAlphabets() {
        assertEquals(listOf("parola"), words("mp3 2nd NASA OK x parola 漢字 한국어 Москва"))
    }

    @Test
    fun rangesPointIntoTheText() {
        val text = "  doppio  spazio"
        val ranges = ProofingText.words(text)
        assertEquals(2, ranges.size)
        assertEquals("doppio", text.substring(ranges[0].first, ranges[0].last + 1))
        assertTrue(ranges[1].first > ranges[0].last)
    }
}
