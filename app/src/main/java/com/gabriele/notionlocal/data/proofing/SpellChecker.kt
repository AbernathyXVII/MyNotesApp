package com.gabriele.notionlocal.data.proofing

/**
 * Il correttore come lo usa una pagina: un [dictionary], le parole che
 * l'utente ha aggiunto al suo dizionario ([personal], le stesse per tutte
 * le lingue) e quelle che ha detto di ignorare in quella pagina
 * ([ignored]).
 *
 * Le parole aggiunte seguono le regole delle maiuscole del dizionario:
 * "gabriele" aggiunta minuscola va bene anche come "Gabriele" e
 * "GABRIELE"; "Gabriele" aggiunta con la maiuscola non va bene scritta
 * "gabriele", come i nomi del dizionario.
 *
 * Ricorda le ultime parole controllate: mentre si scrive, le parole della
 * pagina si ricontrollano a ogni lettera, ma sono quasi sempre le stesse.
 */
class SpellChecker(
    val dictionary: SpellDictionary,
    private val personal: Set<String> = emptySet(),
    private val ignored: Set<String> = emptySet()
) {
    private val cache = object : LinkedHashMap<String, Boolean>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > CACHE_SIZE
    }

    /** Se [word] va bene: per il dizionario, per l'utente o per la pagina. */
    fun isCorrect(word: String): Boolean {
        synchronized(cache) { cache[word] }?.let { return it }
        val ok = word in ignored || personalMatch(word) || dictionary.check(word)
        synchronized(cache) { cache[word] = ok }
        return ok
    }

    /**
     * Le parole sbagliate di [text]. Una parola seguita da un punto va
     * bene anche se va bene col punto ("etc."); una seguita da un
     * apostrofo, anche con l'apostrofo ("po'").
     */
    fun misspelled(text: String): List<IntRange> = ProofingText.words(text).filter { r ->
        val w = text.substring(r.first, r.last + 1)
        if (isCorrect(w)) return@filter false
        val next = text.getOrNull(r.last + 1)
        if (next == '.' && isCorrect("$w.")) return@filter false
        if ((next == '\'' || next == '’') && isCorrect("$w$next")) return@filter false
        true
    }

    /**
     * Fino a [max] parole al posto di [word], la migliore per prima: le
     * parole aggiunte dall'utente che le somigliano molto, poi quelle del
     * dizionario.
     */
    fun suggestions(word: String, max: Int = 5): List<String> {
        val mine = personal.filter { closeTo(word, it) }.sortedBy { distance(word.lowercase(), it.lowercase()) }
        return (mine + dictionary.suggest(word, max)).distinct().take(max)
    }

    private fun personalMatch(word: String): Boolean {
        if (personal.isEmpty()) return false
        if (word in personal) return true
        val normalized = dictionary.normalize(word)
        if (normalized in personal) return true
        val cap = dictionary.capType(normalized)
        if (cap == SpellDictionary.CAP_INIT || cap == SpellDictionary.CAP_ALL) {
            val low = dictionary.lower(normalized)
            if (low in personal) return true
            if (cap == SpellDictionary.CAP_ALL && dictionary.initCap(low) in personal) return true
        }
        return false
    }

    private fun closeTo(word: String, candidate: String): Boolean {
        val a = word.lowercase()
        val b = candidate.lowercase()
        if (a == b) return false
        val limit = if (a.length >= 8) 2 else 1
        return kotlin.math.abs(a.length - b.length) <= limit && distance(a, b) <= limit
    }

    /** Lettere da cambiare, aggiungere, togliere o scambiare per passare da [a] a [b]. */
    private fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }

    private companion object {
        const val CACHE_SIZE = 4096
    }
}
