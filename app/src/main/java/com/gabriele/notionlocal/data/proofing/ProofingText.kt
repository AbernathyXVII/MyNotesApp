package com.gabriele.notionlocal.data.proofing

/**
 * Le parole di un testo da dare al correttore: dove stanno, e quali
 * saltare.
 *
 * Una parola è fatta di lettere (e dei segni che si attaccano alle
 * lettere, come gli accenti scomposti), con dentro al più apostrofi e
 * trattini fra due lettere: "dell'anno", "l’homme", "well-known" sono una
 * parola sola, che il dizionario sa spezzare se serve.
 *
 * **Non si controllano**, come fanno OneNote e Word:
 *  - gli indirizzi, le email e i nomi di file — tutto il pezzo fra due
 *    spazi che contiene "@", "://", "www.", una barra rovescia, un "_", o
 *    un punto fra due lettere ("notes.txt", ma anche "z.B." ed "e.g.");
 *  - i pezzi con simboli da codice o da formula ("a=b", "C++", "#todo");
 *  - le parole con dei numeri ("mp3", "2nd");
 *  - le parole tutte maiuscole ("NASA", "OK"): quasi sempre sigle;
 *  - le parole di una lettera sola: iniziali, elenchi, abbreviazioni;
 *  - le parole in un altro alfabeto (greco, cirillico, cinese, coreano,
 *    giapponese...): i dizionari dell'app sono tutti in alfabeto latino.
 */
object ProofingText {

    private const val CODE_SYMBOLS = "@\\_#`=<>{}|~^$%&+*"

    /** Le parole da controllare in [text], come intervalli di posizioni. */
    fun words(text: String): List<IntRange> {
        val out = ArrayList<IntRange>()
        var i = 0
        val n = text.length
        while (i < n) {
            while (i < n && text[i].isWhitespace()) i++
            val chunkStart = i
            while (i < n && !text[i].isWhitespace()) i++
            if (i > chunkStart) chunkWords(text, chunkStart, i, out)
        }
        return out
    }

    private fun chunkWords(text: String, start: Int, end: Int, out: MutableList<IntRange>) {
        val chunk = text.substring(start, end)
        if (skipChunk(chunk)) return
        var i = start
        while (i < end) {
            if (!isWordChar(text, i)) {
                i++
                continue
            }
            val wordStart = i
            while (i < end) {
                if (isWordChar(text, i)) {
                    i += Character.charCount(text.codePointAt(i))
                } else if (isJoiner(text[i]) && i + 1 < end && isWordChar(text, i + 1) && i > wordStart) {
                    i++
                } else break
            }
            if (worthChecking(text, wordStart, i)) out += wordStart until i
        }
    }

    private fun skipChunk(chunk: String): Boolean {
        if (chunk.contains("://") || chunk.startsWith("www.", ignoreCase = true)) return true
        if (chunk.any { it in CODE_SYMBOLS }) return true
        for (k in 1 until chunk.length - 1) {
            if (chunk[k] == '.' && chunk[k - 1].isLetter() && chunk[k + 1].isLetter()) return true
        }
        return false
    }

    private fun isWordChar(text: String, i: Int): Boolean {
        val cp = text.codePointAt(i)
        if (Character.isLetterOrDigit(cp)) return true
        return when (Character.getType(cp)) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt() -> i > 0
            else -> false
        }
    }

    /** Apostrofi e trattini: uniscono due pezzi di parola. */
    private fun isJoiner(c: Char) = c == '\'' || c == '’' || c == 'ʼ' || c == '-' || c == '‐'

    private fun worthChecking(text: String, start: Int, end: Int): Boolean {
        var letters = 0
        var upper = 0
        var i = start
        while (i < end) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (Character.isDigit(cp)) return false
            if (!Character.isLetter(cp)) continue
            if (Character.UnicodeScript.of(cp) != Character.UnicodeScript.LATIN) return false
            letters++
            if (Character.isUpperCase(cp)) upper++
        }
        if (letters < 2) return false
        return upper < letters
    }
}
