package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.data.entity.bareLinkAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **Un sito scritto come testo semplice** (27/10/2026): la parola che
 * comincia con "http://", "https://" o "www.", senza la punteggiatura
 * attaccata ai bordi. È quella che l'icona del link prende tutta.
 */
class BareLinkTest {

    private fun word(text: String, at: Int, end: Int = at): String? =
        bareLinkAt(text, at, end)?.let { text.substring(it.first, it.last + 1) }

    @Test
    fun theWholeAddressAroundTheCaret() {
        val text = "Vedi https://pokemondb.net/mechanics/natures ora"
        assertEquals("https://pokemondb.net/mechanics/natures", word(text, 20))
        // Subito prima e subito dopo conta ancora.
        assertEquals("https://pokemondb.net/mechanics/natures", word(text, 5))
        assertEquals("https://pokemondb.net/mechanics/natures", word(text, 44))
        // Una parola selezionata dentro l'indirizzo.
        assertEquals("https://pokemondb.net/mechanics/natures", word(text, 22, 31))
    }

    @Test
    fun punctuationAroundIsNotPartOfIt() {
        assertEquals("www.google.com", word("(vedi www.google.com).", 10))
        assertEquals("https://example.com/a", word("\"https://example.com/a\", poi", 5))
        // La parentesi dell'indirizzo resta: chiude la sua.
        assertEquals(
            "https://bulbapedia.bulbagarden.net/wiki/Bulbasaur_(Pokémon)",
            word("Vedi https://bulbapedia.bulbagarden.net/wiki/Bulbasaur_(Pokémon).", 20)
        )
    }

    @Test
    fun notASite() {
        assertNull(word("Cerca su Google", 11))
        assertNull(word("google.com", 3))
        assertNull(word("https:// da solo", 3))
        assertNull(word("www.", 2))
        // Una selezione con uno spazio dentro non è una parola.
        assertNull(word("www.google.com e altro", 5, 17))
    }
}
