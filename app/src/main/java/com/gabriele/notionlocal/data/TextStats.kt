package com.gabriele.notionlocal.data

/**
 * Quanto testo c'è in una pagina: la voce "X words" in fondo al menu dei
 * tre puntini, e i numeri che si aprono toccandola.
 *
 * Il modello è la finestra "Word Count" di Word, che è quella che
 * l'utente ha mostrato come esempio. Da lì vengono le regole che contano
 * di più:
 *  - **una parola è un pezzo di testo fra due spazi**, qualunque cosa
 *    contenga: anche "=" o "[]" da soli contano, come in Word;
 *  - **nelle lingue che non separano le parole con gli spazi** —
 *    cinese e giapponese — **ogni carattere conta come una parola**,
 *    ancora come Word. Il coreano invece gli spazi li usa, quindi le sue
 *    parole si contano come quelle italiane;
 *  - **i caratteri sono tutti i segni scritti**, punteggiatura compresa,
 *    non solo lettere e cifre: altrimenti "caratteri senza spazi" sarebbe
 *    solo la somma di lettere e numeri, che si vedono già a parte.
 *
 * Tutto si conta per **punti di codice**, non per `Char`: un ideogramma
 * raro o un'emoji occupano due `Char` in Java, ma sono un segno solo.
 */
data class TextStats(
    val words: Int = 0,
    /** Lettere degli alfabeti — latino, greco, cirillico... — senza quelle di cinese, giapponese e coreano, che hanno una voce loro. */
    val letters: Int = 0,
    /** Cifre: "2026" sono quattro numeri, come si contano i caratteri. */
    val digits: Int = 0,
    val charactersNoSpaces: Int = 0,
    val charactersWithSpaces: Int = 0,
    /** I blocchi che hanno del testo: una riga della pagina, un elemento di elenco, una tabella. */
    val lines: Int = 0,
    /** Hiragana, katakana, e gli ideogrammi dei blocchi che contengono anche kana. */
    val japanese: Int = 0,
    /** Gli ideogrammi dei blocchi senza kana. */
    val chinese: Int = 0,
    /** Hangul: sillabe e lettere singole. */
    val korean: Int = 0
) {
    /**
     * Lettere e numeri insieme, senza nient'altro: una voce a sé chiesta
     * dall'utente, distinta dai caratteri, che contano anche punti,
     * virgole, parentesi e ogni altro simbolo.
     */
    val lettersAndDigits: Int get() = letters + digits

    operator fun plus(other: TextStats) = TextStats(
        words = words + other.words,
        letters = letters + other.letters,
        digits = digits + other.digits,
        charactersNoSpaces = charactersNoSpaces + other.charactersNoSpaces,
        charactersWithSpaces = charactersWithSpaces + other.charactersWithSpaces,
        lines = lines + other.lines,
        japanese = japanese + other.japanese,
        chinese = chinese + other.chinese,
        korean = korean + other.korean
    )
}

/**
 * Conta un pezzo di testo scritto dall'utente: un blocco, o una cella di
 * una tabella. `lines` resta a zero — cosa sia una riga lo decide chi
 * mette insieme i pezzi, perché una tabella intera vale una riga sola.
 *
 * **Giapponese o cinese?** Gli ideogrammi sono gli stessi nelle due
 * lingue (i kanji giapponesi *sono* caratteri cinesi), e da un carattere
 * solo non si può capire in che lingua è scritto. Si guarda allora il
 * pezzo intero: se contiene anche hiragana o katakana, che esistono solo
 * in giapponese, i suoi ideogrammi sono giapponesi; altrimenti cinesi.
 * Una frase giapponese scritta tutta in kanji verrebbe presa per cinese,
 * ma è un caso raro, mentre una riga di appunti di giapponese ha quasi
 * sempre almeno una particella in hiragana.
 */
fun textStatsOf(text: String): TextStats {
    if (text.isEmpty()) return TextStats()

    var hasKana = false
    forEachCodePoint(text) { cp -> if (isKana(cp)) hasKana = true }

    var words = 0
    var letters = 0
    var digits = 0
    var noSpaces = 0
    var withSpaces = 0
    var japanese = 0
    var chinese = 0
    var korean = 0
    var inWord = false

    forEachCodePoint(text) { cp ->
        // Gli a-capo dentro un blocco separano le parole ma non sono
        // caratteri: Word non conta i segni di fine paragrafo.
        if (cp == '\n'.code || cp == '\r'.code) {
            inWord = false
            return@forEachCodePoint
        }
        withSpaces++
        if (isSpace(cp)) {
            inWord = false
            return@forEachCodePoint
        }
        noSpaces++

        val script = runCatching { Character.UnicodeScript.of(cp) }.getOrNull()
        when {
            script == Character.UnicodeScript.HAN -> {
                if (hasKana) japanese++ else chinese++
            }
            isKana(cp) -> japanese++
            script == Character.UnicodeScript.HANGUL -> korean++
            Character.isDigit(cp) -> digits++
            Character.isLetter(cp) -> letters++
        }

        // Un ideogramma o un kana è una parola a sé e chiude quella che
        // c'era prima; tutto il resto allunga la parola in corso, o ne
        // comincia una.
        if (script == Character.UnicodeScript.HAN || isKana(cp)) {
            words++
            inWord = false
        } else if (!inWord) {
            words++
            inWord = true
        }
    }

    return TextStats(
        words = words,
        letters = letters,
        digits = digits,
        charactersNoSpaces = noSpaces,
        charactersWithSpaces = withSpaces,
        japanese = japanese,
        chinese = chinese,
        korean = korean
    )
}

private inline fun forEachCodePoint(text: String, action: (Int) -> Unit) {
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        action(cp)
        i += Character.charCount(cp)
    }
}

/**
 * Spazio è anche quello "che non va a capo" (U+00A0) e quello largo
 * delle lingue asiatiche (U+3000): per `isWhitespace` il primo non lo è,
 * ma in una riga di testo separa le parole come gli altri.
 */
private fun isSpace(cp: Int): Boolean = Character.isWhitespace(cp) || Character.isSpaceChar(cp)

/**
 * Hiragana e katakana, compreso il trattino che allunga le vocali (ー),
 * che Unicode non assegna a nessuna delle due scritture perché lo usano
 * entrambe, ma che esiste solo in giapponese.
 */
private fun isKana(cp: Int): Boolean {
    if (cp == 0x30FC) return true
    val script = runCatching { Character.UnicodeScript.of(cp) }.getOrNull()
    return script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA
}
