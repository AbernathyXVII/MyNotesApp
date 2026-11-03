package com.gabriele.notionlocal.data.proofing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Il correttore sui dizionari veri dell'app, con frasi vere in ogni
 * lingua: le parole giuste passano, quelle sbagliate no, e il primo
 * suggerimento è quello che si voleva scrivere.
 */
class SpellDictionaryTest {

    companion object {
        private fun load(dir: String, locale: Locale): SpellDictionary {
            val base = File("src/main/assets/proofing/$dir")
            return File(base, "index.aff").bufferedReader().use { aff ->
                File(base, "index.dic").bufferedReader().use { dic -> SpellDictionary.read(aff, dic, locale) }
            }
        }

        val en by lazy { load("en_US", Locale.US) }
        val gb by lazy { load("en_GB", Locale.UK) }
        val it by lazy { load("it", Locale.ITALIAN) }
        val de by lazy { load("de", Locale.GERMAN) }
        val fr by lazy { load("fr", Locale.FRENCH) }
        val es by lazy { load("es", Locale("es")) }
    }

    private fun assertAllCorrect(d: SpellDictionary, text: String) {
        val wrong = text.split(Regex("[\\s,.;:!?()\"«»¿¡]+")).filter { it.isNotEmpty() && !d.check(it) }
        assertEquals("parole giuste segnate come sbagliate", emptyList<String>(), wrong)
    }

    private fun assertAllWrong(d: SpellDictionary, vararg words: String) {
        val passed = words.filter { d.check(it) }
        assertEquals("parole sbagliate che passano", emptyList<String>(), passed)
    }

    private fun assertFirst(d: SpellDictionary, wrong: String, expected: String) {
        val s = d.suggest(wrong)
        assertEquals("primo suggerimento per \"$wrong\" (tutti: $s)", expected, s.firstOrNull())
    }

    @Test
    fun englishUs() {
        assertAllCorrect(
            en,
            "The quick brown fox jumps over the lazy dog. John's children were running happily; " +
                "unhappiness, well-known, reread, color, I'm, don't, it's, they'll, organize, center"
        )
        assertAllWrong(en, "teh", "recieve", "definately", "seperate", "colour", "untill")
        assertFirst(en, "teh", "the")
        assertFirst(en, "recieve", "receive")
        assertFirst(en, "definately", "definitely")
        assertFirst(en, "becuase", "because")
        assertFirst(en, "wierd", "weird")
        assertFirst(en, "helo", "hello")
        assertFirst(en, "untill", "until")
        assertFirst(en, "alot", "a lot")
        assertFirst(en, "inthe", "in the")
        assertFirst(en, "Teh", "The")
        assertFirst(en, "Gemrany", "Germany")
    }

    @Test
    fun englishUk() {
        assertAllCorrect(gb, "The colour of the centre; organise, travelled, behaviour")
        assertAllWrong(gb, "color", "center")
        assertFirst(gb, "color", "colour")
        assertFirst(gb, "center", "centre")
    }

    @Test
    fun italian() {
        assertAllCorrect(
            it,
            "Questa è una frase scritta correttamente: dell'anno, l'albero, un'altra, c'è, po', " +
                "parliamo, perché, città, Andiamo, nell'ultimo, sull'isola, quell'uomo, gli amici, " +
                "mangiarlo, dimmelo, facendolo, qual è"
        )
        assertAllWrong(it, "perche", "aqua", "squola", "propio", "daccordo", "cè", "pò", "citta")
        assertFirst(it, "perche", "perché")
        assertFirst(it, "Perche", "Perché")
        assertFirst(it, "citta", "città")
        assertFirst(it, "propio", "proprio")
        assertFirst(it, "aqua", "acqua")
        assertFirst(it, "squola", "scuola")
        assertFirst(it, "cè", "c'è")
        assertFirst(it, "pò", "po'")
        assertFirst(it, "dellacasa", "della casa")
        assertTrue(it.suggest("qual'è").contains("qual è"))
    }

    @Test
    fun german() {
        assertAllCorrect(
            de,
            "Die Haustür ist offen. Häuser, Straße, Arbeitszimmer, Fußballweltmeisterschaft, " +
                "Donaudampfschifffahrt, E-Mail, Kinder, schönen, gegangen, Bundesregierung"
        )
        // Le parti dei composti vanno minuscole: "HausTür" no.
        assertAllWrong(de, "Hauss", "Strase", "Arbeitzimmer", "HausTür", "haustür")
        assertFirst(de, "Hauss", "Haus")
        assertFirst(de, "Strase", "Straße")
        assertFirst(de, "Arbeitzimmer", "Arbeitszimmer")
        assertFirst(de, "Kindergartenn", "Kindergarten")
        assertFirst(de, "haustür", "Haustür")
        assertFirst(de, "schoen", "schön")
        assertFirst(de, "Mädchn", "Mädchen")
    }

    @Test
    fun germanSharpSInCapitals() {
        assertTrue(de.check("STRASSE"))
        assertTrue(de.check("HAUSTÜR"))
    }

    @Test
    fun french() {
        assertAllCorrect(
            fr,
            "L'homme qu'il a vu aujourd'hui; les enfants mangeaient des pommes. C'est l'été, j'ai, " +
                "n'est-ce pas, l’homme, événement, très, être"
        )
        assertAllWrong(fr, "évenement", "tres", "etre", "lhomme")
        assertFirst(fr, "évenement", "événement")
        assertFirst(fr, "evenement", "événement")
        assertFirst(fr, "tres", "très")
        assertFirst(fr, "etre", "être")
        assertFirst(fr, "lhomme", "l'homme")
    }

    @Test
    fun spanish() {
        assertAllCorrect(
            es,
            "Dámelo ahora, la canción de los niños; ¿Qué estás haciendo? diciéndoselo, comiéndolo, España, árboles"
        )
        assertAllWrong(es, "cancion", "ninos", "arbol", "aqui", "tambien")
        assertFirst(es, "cancion", "canción")
        assertFirst(es, "ninos", "niños")
        assertFirst(es, "arbol", "árbol")
        assertFirst(es, "aqui", "aquí")
        assertFirst(es, "tambien", "también")
    }

    @Test
    fun capitals() {
        // Una parola minuscola va bene anche con l'iniziale grande o tutta maiuscola; un nome no al contrario.
        assertTrue(it.check("Casa"))
        assertTrue(it.check("CASA"))
        assertTrue(en.check("London"))
        assertFalse(en.check("london"))
        assertFirst(en, "london", "London")
    }

    @Test
    fun curlyApostrophes() {
        assertTrue(it.check("dell’anno"))
        assertTrue(fr.check("l’homme"))
        // Il suggerimento tiene l'apostrofo com'era scritto.
        val s = it.suggest("l’albro")
        assertTrue(s.toString(), "l’albero" in s && s.all { "'" !in it })
    }

    @Test
    fun correctWordsHaveNoSuggestions() {
        assertEquals(emptyList<String>(), it.suggest("casa"))
    }

    @Test
    fun suggestionsAreQuick() {
        // Una parola lunga e sbagliata in ogni lingua: anche passando per le parole simili, ben sotto il limite.
        for ((d, w) in listOf(en to "xqzwlkjhgfd", it to "precipitevolissimevolmenter", de to "Donaudampfschiffahrtsgesellschaftskapitänn", fr to "anticonstitutionnelemant")) {
            val start = System.nanoTime()
            d.suggest(w)
            val ms = (System.nanoTime() - start) / 1_000_000
            assertTrue("\"$w\" ci ha messo $ms ms", ms < 1500)
        }
    }
}
