package com.gabriele.notionlocal.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Il conteggio del testo della voce "X words". Gira sul computer, senza
 * telefono: `gradle testDebugUnitTest`.
 *
 * Gli esempi sono presi dal tipo di appunti dell'utente — liste di
 * vocaboli con "[]" e "=", e lingue asiatiche — perché è lì che le
 * regole di Word (vedi `TextStats`) fanno la differenza.
 */
class TextStatsTest {

    @Test
    fun italianSentence() {
        val s = textStatsOf("Ciao mondo")
        assertEquals(2, s.words)
        assertEquals(9, s.letters)
        assertEquals(0, s.digits)
        assertEquals(9, s.charactersNoSpaces)
        assertEquals(10, s.charactersWithSpaces)
        assertEquals(0, s.lines)
    }

    @Test
    fun symbolsAloneAreWordsAndCharactersLikeInWord() {
        // "vorschlagen [] =": tre parole per Word, anche "[]" e "=".
        val s = textStatsOf("vorschlagen [] =")
        assertEquals(3, s.words)
        assertEquals(11, s.letters)
        assertEquals(14, s.charactersNoSpaces)
        assertEquals(16, s.charactersWithSpaces)
    }

    @Test
    fun digitsAreCountedOneByOne() {
        val s = textStatsOf("Anno 2026")
        assertEquals(2, s.words)
        assertEquals(4, s.letters)
        assertEquals(4, s.digits)
        assertEquals(8, s.charactersNoSpaces)
    }

    @Test
    fun accentedLettersAreLetters() {
        val s = textStatsOf("perché è così")
        assertEquals(3, s.words)
        assertEquals(11, s.letters)
    }

    @Test
    fun japaneseWithKanaCountsKanjiAsJapanese() {
        // 私 学 生 sono ideogrammi, は で す hiragana: la riga ha kana,
        // quindi è giapponese, e ogni segno è una parola.
        val s = textStatsOf("私は学生です")
        assertEquals(6, s.japanese)
        assertEquals(0, s.chinese)
        assertEquals(6, s.words)
        assertEquals(0, s.letters)
        assertEquals(6, s.charactersNoSpaces)
    }

    @Test
    fun katakanaLongVowelMarkIsJapanese() {
        val s = textStatsOf("コーヒー")
        assertEquals(4, s.japanese)
        assertEquals(4, s.words)
    }

    @Test
    fun ideographsWithoutKanaAreChinese() {
        val s = textStatsOf("我是学生")
        assertEquals(4, s.chinese)
        assertEquals(0, s.japanese)
        assertEquals(4, s.words)
    }

    @Test
    fun koreanWordsAreSeparatedBySpaces() {
        val s = textStatsOf("안녕 하세요")
        assertEquals(5, s.korean)
        assertEquals(2, s.words)
        assertEquals(0, s.letters)
    }

    @Test
    fun mixedLatinAndChinese() {
        // "Hello" è una parola, 世 e 界 una ciascuno.
        val s = textStatsOf("Hello 世界")
        assertEquals(3, s.words)
        assertEquals(5, s.letters)
        assertEquals(2, s.chinese)
    }

    @Test
    fun ideographTouchingLatinTextStillSplitsTheWord() {
        // Nessuno spazio, ma l'ideogramma chiude "abc" e ne apre un'altra.
        val s = textStatsOf("abc中def")
        assertEquals(3, s.words)
        assertEquals(1, s.chinese)
    }

    @Test
    fun nonBreakingAndIdeographicSpacesSeparateWords() {
        val s = textStatsOf("a b　c")
        assertEquals(3, s.words)
        assertEquals(3, s.charactersNoSpaces)
        assertEquals(5, s.charactersWithSpaces)
    }

    @Test
    fun emojiIsOneCharacterNotTwo() {
        // 👍 occupa due Char in Java, ma è un segno solo.
        val s = textStatsOf("ok 👍")
        assertEquals(2, s.words)
        assertEquals(3, s.charactersNoSpaces)
        assertEquals(4, s.charactersWithSpaces)
    }

    @Test
    fun lineBreaksSeparateWordsButAreNotCharacters() {
        val s = textStatsOf("uno\ndue")
        assertEquals(2, s.words)
        assertEquals(6, s.charactersNoSpaces)
        assertEquals(6, s.charactersWithSpaces)
    }

    @Test
    fun emptyAndBlankText() {
        assertEquals(TextStats(), textStatsOf(""))
        val blank = textStatsOf("   ")
        assertEquals(0, blank.words)
        assertEquals(0, blank.charactersNoSpaces)
        assertEquals(3, blank.charactersWithSpaces)
    }

    @Test
    fun statsAddUp() {
        val sum = textStatsOf("Ciao").copy(lines = 1) + textStatsOf("我是").copy(lines = 1)
        assertEquals(3, sum.words)
        assertEquals(2, sum.lines)
        assertEquals(4, sum.letters)
        assertEquals(2, sum.chinese)
    }
}
