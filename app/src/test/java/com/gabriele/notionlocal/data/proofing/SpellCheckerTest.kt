package com.gabriele.notionlocal.data.proofing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellCheckerTest {

    private val it get() = SpellDictionaryTest.it
    private val en get() = SpellDictionaryTest.en

    private fun misspelled(checker: SpellChecker, text: String) =
        checker.misspelled(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun findsTheWrongWordsOfASentence() {
        val checker = SpellChecker(it)
        assertEquals(listOf("squola", "perche"), misspelled(checker, "Domani vado a squola perche c'è il compito di Mario."))
    }

    @Test
    fun personalWordsFollowTheCapitalRules() {
        val checker = SpellChecker(it, personal = setOf("notE", "gabriele", "Ferrandina"))
        assertTrue(checker.isCorrect("notE"))
        assertTrue(checker.isCorrect("gabriele"))
        assertTrue(checker.isCorrect("Gabriele"))
        assertTrue(checker.isCorrect("GABRIELE"))
        assertTrue(checker.isCorrect("Ferrandina"))
        assertTrue(checker.isCorrect("FERRANDINA"))
        assertFalse(checker.isCorrect("ferrandina"))
    }

    @Test
    fun personalWordsComeFirstInSuggestions() {
        val checker = SpellChecker(it, personal = setOf("Ferrandina"))
        assertEquals("Ferrandina", checker.suggestions("Ferandina").firstOrNull())
    }

    @Test
    fun ignoredWordsPass() {
        val checker = SpellChecker(it, ignored = setOf("blablabla"))
        assertEquals(emptyList<String>(), misspelled(checker, "Un blablabla qualunque"))
    }

    @Test
    fun abbreviationsWithTheirDot() {
        val checker = SpellChecker(en)
        assertEquals(emptyList<String>(), misspelled(checker, "apples, pears, etc. and so on"))
    }

    @Test
    fun italianTruncationWithApostropheAtTheEnd() {
        val checker = SpellChecker(it)
        assertEquals(emptyList<String>(), misspelled(checker, "Aspetta un po' qui"))
    }
}
