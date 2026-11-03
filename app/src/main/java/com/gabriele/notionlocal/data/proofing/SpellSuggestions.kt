package com.gabriele.notionlocal.data.proofing

import com.gabriele.notionlocal.data.proofing.SpellDictionary.Companion.CAP_ALL
import com.gabriele.notionlocal.data.proofing.SpellDictionary.Companion.CAP_INIT
import com.gabriele.notionlocal.data.proofing.SpellDictionary.Companion.CAP_MIXED_INIT
import com.gabriele.notionlocal.data.proofing.SpellDictionary.Companion.CAP_NONE
import com.gabriele.notionlocal.data.proofing.SpellDictionary.Companion.CPD_NOT
import kotlin.math.abs

/**
 * Le parole giuste più vicine a [word], la migliore per prima: al più
 * [max]. Una parola già giusta non ne ha.
 *
 * È il metodo di Hunspell (suggestmgr.cxx), in due tempi:
 *  1. **gli errori tipici**, provati uno per uno sulla parola e tenuti se
 *     danno una parola del dizionario: le sostituzioni `REP` del
 *     dizionario ("ph" ↔ "f"), le lettere della stessa famiglia `MAP`
 *     ("perche" → "perché"), due lettere scambiate, un tasto vicino sulla
 *     tastiera, una lettera in più, una in meno, una spostata, una
 *     sbagliata, due parole attaccate ("dellacasa" → "della casa");
 *  2. **se non trova niente**, le parole che si somigliano di più: cerca
 *     fra tutte le radici quelle con più pezzi di tre lettere in comune,
 *     ne fa le forme con prefissi e desinenze, e le mette in fila con un
 *     punteggio che premia l'inizio uguale e le lettere al posto giusto.
 *
 * Si ferma comunque dopo poco più di un secondo, con quello che ha.
 */
fun SpellDictionary.suggest(word: String, max: Int = 5): List<String> {
    if (check(word)) return emptyList()
    val curly = word.contains('’')
    return Suggester(this).run(word)
        .map { if (curly) it.replace('\'', '’') else it }
        .take(max)
}

internal class Suggester(private val d: SpellDictionary) {

    private val aff = d.aff
    private val deadline = System.nanoTime() + TIME_LIMIT_NS
    private val keyboard = aff.keyboard
    private val german = d.locale.language == "de"
    private val shortWords = SHORT_WORDS[d.locale.language].orEmpty()
    private val repRules = aff.rep + EXTRA_REP[d.locale.language].orEmpty()

    /**
     * L'ordine dei suggerimenti: prima il tipo di errore — gli errori
     * tipici (accenti, `REP`, maiuscole) davanti a due lettere scambiate,
     * poi una lettera in più o in meno, un tasto vicino, una lettera
     * qualunque, due parole — e a parità la passata (minuscola prima, per
     * una parola a inizio frase) e l'ordine in cui sono venuti.
     */
    private val rank = HashMap<String, Int>()
    private var pass = 0
    private var cat = 0

    private fun record(s: String) {
        rank.putIfAbsent(s, cat * 10 + pass)
    }

    private fun timeUp() = System.nanoTime() > deadline

    fun run(input: String): List<String> {
        val word = d.normalize(input)
        if (word.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        var good = false
        var capWords = false
        val cap = d.capType(word)
        when (cap) {
            CAP_NONE -> good = suggestInto(out, word)
            CAP_INIT -> {
                // Prima minuscola: una parola a inizio frase è quasi sempre una parola qualunque, non un nome.
                capWords = true
                good = suggestInto(out, d.lower(word))
                if (!timeUp()) good = suggestInto(out, word) || good
            }
            CAP_ALL -> {
                val low = d.lower(word)
                good = suggestInto(out, low)
                if (aff.keepCase != NONE && d.spell(low, 0)) { out.add(0, low); rank[low] = -1 }
                if (!timeUp()) good = suggestInto(out, d.initCap(low)) || good
                sortByRank(out)
                for (i in out.indices) {
                    out[i] = out[i].uppercase(d.locale).let { if (aff.checkSharps) it.replace("ß", "SS") else it }
                }
            }
            else -> {
                if (cap == CAP_MIXED_INIT) capWords = true
                good = suggestInto(out, word)
                val low = d.lower(word)
                if (d.spell(low, 0)) { out.add(0, low); rank[low] = -1 }
                if (!timeUp()) good = suggestInto(out, low) || good
            }
        }
        if (cap != CAP_ALL) sortByRank(out)
        if (!good && out.isEmpty() && !timeUp()) {
            ngramSuggest(out, if (cap == CAP_NONE) word else d.lower(word), cap)
        }
        if (!good && !timeUp()) dashSuggest(out, word)
        if (capWords) for (i in out.indices) out[i] = d.initCap(out[i])
        // Con l'iniziale grande la forma potrebbe non andare (KEEPCASE): allora com'è nel dizionario.
        if ((cap == CAP_INIT || cap == CAP_ALL) && (aff.keepCase != NONE || aff.forbidden != NONE)) {
            val kept = ArrayList<String>()
            for (s in out) {
                if (s.contains(' ') || d.spell(s, 0)) { kept += s; continue }
                val low = d.lower(s)
                if (d.spell(low, 0)) kept += low
                else if (d.spell(d.initCap(low), 0)) kept += d.initCap(low)
            }
            out.clear()
            out += kept
        }
        // Una lettera sola, o un trattino in cima o in fondo ("Kindergarten-", che il tedesco accetta per
        // "Kindergarten- und Schulzeit"), sono parole giuste ma non quello che si voleva scrivere.
        return out.distinct().filter { s ->
            (s.length > 1 || word.length < 2) &&
                (word.startsWith("-") || !s.startsWith("-")) &&
                (word.endsWith("-") || !s.endsWith("-"))
        }
    }

    private fun sortByRank(out: MutableList<String>) {
        val sorted = out.withIndex().sortedWith(compareBy({ rank[it.value] ?: 50 }, { it.index })).map { it.value }
        out.clear()
        out += sorted
    }

    /** "Afo-American" → "Afro-American": se una parte col trattino è sbagliata, si corregge solo quella. */
    private fun dashSuggest(out: MutableList<String>, word: String) {
        var dashPos = word.indexOf('-')
        if (dashPos < 0 || out.any { it.contains('-') }) return
        var prev = 0
        var last = false
        while (!last) {
            if (dashPos == word.length) last = true
            val chunk = word.substring(prev, dashPos)
            if (chunk != word && chunk.isNotEmpty() && !d.spell(chunk, 0)) {
                val fixes = Suggester(d).run(chunk)
                for (s in fixes.asReversed()) {
                    var w = word.substring(0, prev) + s
                    if (!last) w += "-" + word.substring(dashPos + 1)
                    val info = SpellDictionary.CheckInfo()
                    d.checkWord(w, info)
                    if (!info.forbidden) out.add(0, w)
                }
                return
            }
            if (!last) {
                prev = dashPos + 1
                dashPos = word.indexOf('-', prev)
            }
            if (dashPos < 0) dashPos = word.length
        }
    }

    // ---- 1. Gli errori tipici ------------------------------------------

    /** `SuggestMgr::suggest`. Vero se ha trovato un suggerimento "buono" (maiuscole, REP, due parole del dizionario). */
    private fun suggestInto(out: MutableList<String>, word: String): Boolean {
        var good = false
        var noCompoundTwoWords = false
        val start = out.size
        for (pass in 0..1) {
            if (pass == 1 && (noCompoundTwoWords || good || !aff.compounds)) break
            val compound = pass == 1
            val oldSug = out.size
            fun room() = out.size < MAX_SUG && (!compound || out.size < oldSug + MAX_COMPOUND_SUGS)

            cat = 0
            if (out.size < MAX_SUG) {
                val i = out.size
                testSug(out, word.uppercase(d.locale), compound)
                if (out.size > i) good = true
            }
            cat = 1
            if (room()) {
                val i = out.size
                replChars(out, word, compound)
                if (out.size > i) good = true
            }
            if (timeUp()) break
            if (room()) mapChars(out, word, compound)
            if (timeUp()) break
            if (!compound && out.size > start) noCompoundTwoWords = true
            cat = 2
            if (room()) swapChar(out, word, compound)
            cat = 5
            if (room()) longSwapChar(out, word, compound)
            cat = 4
            if (room()) badCharKey(out, word, compound)
            if (timeUp()) break
            cat = 3
            if (room()) extraChar(out, word, compound)
            if (room()) forgotChar(out, word, compound)
            if (timeUp()) break
            cat = 5
            if (room()) moveChar(out, word, compound)
            if (room()) badChar(out, word, compound)
            if (timeUp()) break
            cat = 3
            if (room()) doubleTwoChars(out, word, compound)
            cat = 6
            if (!compound || out.size < oldSug + MAX_COMPOUND_SUGS) twoWords(out, word, compound)
            if (timeUp()) break
        }
        this.pass++
        return good
    }

    private fun testSug(out: MutableList<String>, candidate: String, compound: Boolean) {
        if (out.size >= MAX_SUG || candidate in out) return
        if (goodSug(candidate, compound)) {
            out += candidate
            record(candidate)
        }
    }

    /**
     * `SuggestMgr::checkword`: se [word] si può proporre — esatta, senza
     * cambiare maiuscole, e senza le parole `NOSUGGEST` (le parolacce dei
     * dizionari inglesi) o vietate.
     */
    internal fun goodSug(word: String, compound: Boolean): Boolean {
        if (compound) {
            if (!aff.compounds || !d.checkCompound(word)) return false
            val entry = d.words[word] ?: return true
            return d.anyHomonym(entry) { has(it, aff.forbidden) || has(it, aff.noSuggest) } == null
        }
        var rv: String? = null
        val entry = d.words[word]
        if (entry != null) {
            if (d.anyHomonym(entry) { has(it, aff.forbidden) || has(it, aff.noSuggest) } != null) return false
            rv = d.anyHomonym(entry) { !has(it, aff.needAffix) && !has(it, aff.onlyInCompound) }
        }
        if (rv == null) rv = d.prefixCheck(word, CPD_NOT, NONE)
        if (rv == null) rv = d.suffixCheck(word, false, null, NONE, NONE, CPD_NOT)
        if (rv == null && aff.contFlags.isNotEmpty()) {
            rv = d.suffixCheckTwoSfx(word, false, null, NONE) ?: d.prefixCheckTwoSfx(word, CPD_NOT, NONE)
        }
        return rv != null && !has(rv, aff.forbidden) && !has(rv, aff.noSuggest) && !has(rv, aff.onlyInCompound)
    }

    /** Le sostituzioni `REP` del dizionario (e [EXTRA_REP]): gli errori di chi scrive, non di chi batte i tasti. */
    private fun replChars(out: MutableList<String>, word: String, compound: Boolean) {
        if (word.length < 2) return
        for (r in repRules) {
            if (r.from.isEmpty()) continue
            var pos = word.indexOf(r.from)
            while (pos >= 0) {
                if ((!r.atStart || pos == 0) && (!r.atEnd || pos + r.from.length == word.length)) {
                    val cand = word.substring(0, pos) + r.to + word.substring(pos + r.from.length)
                    testSug(out, cand, compound)
                    var prev = 0
                    var sp = cand.indexOf(' ')
                    while (sp >= 0) {
                        if (goodSug(cand.substring(prev, sp), false)) {
                            val before = out.size
                            testSug(out, cand.substring(sp + 1), compound)
                            if (out.size > before) {
                                out[out.size - 1] = cand
                                record(cand)
                            }
                        }
                        prev = sp + 1
                        sp = cand.indexOf(' ', prev)
                    }
                }
                pos = word.indexOf(r.from, pos + 1)
            }
        }
    }

    private var mapBudget = 0

    /**
     * Le lettere della stessa famiglia `MAP`: "e" "é" "è"... Solo fra
     * lettere con la stessa maiuscola — il francese mette nelle famiglie
     * anche "b" e "B", e con tutte le combinazioni una parola di dieci
     * lettere ne darebbe milioni; le maiuscole sbagliate le provano già
     * gli altri passi.
     */
    private fun mapChars(out: MutableList<String>, word: String, compound: Boolean) {
        if (word.length < 2 || aff.families.isEmpty()) return
        mapBudget = MAP_BUDGET
        mapRelated(word, StringBuilder(), 0, out, compound)
    }

    private fun mapRelated(word: String, cand: StringBuilder, wn: Int, out: MutableList<String>, compound: Boolean) {
        if (mapBudget <= 0) return
        if (wn >= word.length) {
            mapBudget--
            val c = cand.toString()
            if (c != word && out.size < MAX_SUG) testSug(out, c, compound)
            return
        }
        var inMap = false
        val cn = cand.length
        for (group in aff.families) {
            for (member in group) {
                if (member.isEmpty() || !word.startsWith(member, wn)) continue
                inMap = true
                val upper = Character.isUpperCase(member[0])
                for (other in group) {
                    if (other.isEmpty() || Character.isUpperCase(other[0]) != upper) continue
                    cand.setLength(cn)
                    cand.append(other)
                    mapRelated(word, cand, wn + member.length, out, compound)
                    if (mapBudget <= 0) return
                }
            }
        }
        cand.setLength(cn)
        if (!inMap) {
            cand.append(word[wn])
            mapRelated(word, cand, wn + 1, out, compound)
            cand.setLength(cn)
        }
    }

    /** Due lettere vicine scambiate; per le parole di 4 e 5 lettere anche due coppie ("ahev" → "have"). */
    private fun swapChar(out: MutableList<String>, word: String, compound: Boolean) {
        val c = word.toCharArray()
        if (c.size < 2) return
        for (i in 0 until c.size - 1) {
            swap(c, i, i + 1)
            testSug(out, String(c), compound)
            swap(c, i, i + 1)
        }
        if (c.size == 4 || c.size == 5) {
            val n = c.size
            c[0] = word[1]; c[1] = word[0]; c[2] = word[2]
            c[n - 2] = word[n - 1]; c[n - 1] = word[n - 2]
            testSug(out, String(c), compound)
            if (n == 5) {
                c[0] = word[0]; c[1] = word[2]; c[2] = word[1]
                testSug(out, String(c), compound)
            }
        }
    }

    /** Due lettere scambiate a distanza (fino a quattro posti). */
    private fun longSwapChar(out: MutableList<String>, word: String, compound: Boolean) {
        val c = word.toCharArray()
        for (p in c.indices) for (q in c.indices) {
            val dist = abs(q - p)
            if (dist > 1 && dist <= MAX_CHAR_DISTANCE) {
                swap(c, p, q)
                testSug(out, String(c), compound)
                swap(c, p, q)
            }
        }
    }

    /** Una lettera maiuscola dove andava, o un tasto vicino sulla tastiera (`KEY`). */
    private fun badCharKey(out: MutableList<String>, word: String, compound: Boolean) {
        val c = word.toCharArray()
        for (i in c.indices) {
            val tmp = c[i]
            val up = tmp.uppercaseChar()
            if (up != tmp) {
                c[i] = up
                val keep = cat
                cat = 1
                testSug(out, String(c), compound)
                cat = keep
                c[i] = tmp
            }
            var loc = keyboard.indexOf(tmp)
            while (loc >= 0) {
                if (loc > 0 && keyboard[loc - 1] != '|') {
                    c[i] = keyboard[loc - 1]
                    testSug(out, String(c), compound)
                }
                if (loc + 1 < keyboard.length && keyboard[loc + 1] != '|') {
                    c[i] = keyboard[loc + 1]
                    testSug(out, String(c), compound)
                }
                loc = keyboard.indexOf(tmp, loc + 1)
            }
            c[i] = tmp
        }
    }

    /**
     * Una lettera di troppo. Se è una doppia ("untill") sale fra le due
     * lettere scambiate: sbagliare le doppie è l'errore più comune.
     */
    private fun extraChar(out: MutableList<String>, word: String, compound: Boolean) {
        if (word.length < 2) return
        for (i in word.indices.reversed()) {
            val doubled = (i > 0 && word[i - 1] == word[i]) || (i + 1 < word.length && word[i + 1] == word[i])
            withCategory(if (doubled) 2 else cat) { testSug(out, word.removeRange(i, i + 1), compound) }
        }
    }

    /** Una lettera dimenticata: ogni lettera di `TRY` in ogni posto. Una doppia dimenticata ("Zimer") sale come sopra. */
    private fun forgotChar(out: MutableList<String>, word: String, compound: Boolean) {
        for (t in aff.tryChars) {
            for (i in word.length downTo 0) {
                val doubled = (i > 0 && word[i - 1] == t) || (i < word.length && word[i] == t)
                withCategory(if (doubled) 2 else cat) { testSug(out, word.substring(0, i) + t + word.substring(i), compound) }
            }
            if (timeUp()) return
        }
    }

    private inline fun withCategory(c: Int, action: () -> Unit) {
        val keep = cat
        cat = c
        action()
        cat = keep
    }

    /** Una lettera finita qualche posto più in là (o più in qua). */
    private fun moveChar(out: MutableList<String>, word: String, compound: Boolean) {
        val n = word.length
        if (n < 2) return
        var c = word.toCharArray()
        for (p in 0 until n) {
            var q = p + 1
            while (q < n && q - p <= MAX_CHAR_DISTANCE) {
                swap(c, q, q - 1)
                if (q - p >= 2) testSug(out, String(c), compound)
                q++
            }
            c = word.toCharArray()
        }
        for (p in n - 1 downTo 1) {
            var q = p - 1
            while (q >= 0 && p - q <= MAX_CHAR_DISTANCE) {
                swap(c, q, q + 1)
                if (p - q >= 2) testSug(out, String(c), compound)
                q--
            }
            c = word.toCharArray()
        }
    }

    /** Una lettera sbagliata: ogni posto, con ogni lettera di `TRY`. */
    private fun badChar(out: MutableList<String>, word: String, compound: Boolean) {
        val c = word.toCharArray()
        for (t in aff.tryChars) {
            for (i in c.indices.reversed()) {
                val tmp = c[i]
                if (tmp == t) continue
                c[i] = t
                testSug(out, String(c), compound)
                c[i] = tmp
            }
            if (timeUp()) return
        }
    }

    /** Due lettere ripetute due volte: "vacacation" → "vacation". */
    private fun doubleTwoChars(out: MutableList<String>, word: String, compound: Boolean) {
        val wl = word.length
        if (wl < 5) return
        var state = 0
        for (i in 2 until wl) {
            if (word[i] == word[i - 2]) {
                state++
                if (state == 3 || (state == 2 && i >= 4)) {
                    testSug(out, word.substring(0, i - 1) + word.substring(i + 1), compound)
                    state = 0
                }
            } else state = 0
        }
    }

    /**
     * Lo spazio dimenticato: "dellacasa" → "della casa". Solo se i due
     * pezzi sono lunghi almeno quattro lettere, o il primo è una parolina
     * come "a", "in", "di" e l'altro ne ha almeno tre: altrimenti "colour"
     * diventerebbe "col our", "occured" "occur ed", "color" "col or" —
     * parole vere, ma a caso. E ne propone uno solo, il primo: "inthe" dà
     * "in the", non anche "int he".
     */
    private fun twoWords(out: MutableList<String>, word: String, compound: Boolean) {
        if (word.length < 3) return
        for (i in 1 until word.length) {
            if (Character.isLowSurrogate(word[i])) continue
            val p1 = word.substring(0, i)
            val p2 = word.substring(i)
            val plausible = (p1.length >= 4 && p2.length >= 4) ||
                (d.lower(p1) in shortWords && p2.length >= 3)
            if (plausible && goodSug(p1, compound) && goodSug(p2, compound)) {
                val pair = "$p1 $p2"
                if (pair !in out && out.size < MAX_SUG) {
                    out += pair
                    record(pair)
                }
                return
            }
        }
    }

    // ---- 2. Le parole che si somigliano -------------------------------

    /** `SuggestMgr::ngsuggest`. */
    private fun ngramSuggest(out: MutableList<String>, word: String, cap: Int) {
        val n = word.length
        val roots = arrayOfNulls<String>(MAX_ROOTS)
        val rootFlags = arrayOfNulls<String>(MAX_ROOTS)
        val scores = IntArray(MAX_ROOTS) { -100 * it }
        var lp = MAX_ROOTS - 1
        var checked = 0
        for ((root, entry) in d.words) {
            if (abs(n - root.length) > 4) continue
            if (cap == CAP_NONE && !german && root.isNotEmpty() && Character.isUpperCase(root[0])) continue
            if (++checked % 4096 == 0 && timeUp()) return
            val flags = d.anyHomonym(entry) { f ->
                !has(f, aff.forbidden) && !has(f, aff.noSuggest) && !has(f, aff.onlyInCompound)
            } ?: continue
            val sc = ngram(3, word, root, NGRAM_LONGER_WORSE or NGRAM_LOWERING) + leftCommon(word, root)
            if (sc > scores[lp]) {
                scores[lp] = sc
                roots[lp] = root
                rootFlags[lp] = flags
                var low = sc
                for (j in 0 until MAX_ROOTS) if (scores[j] < low) { lp = j; low = scores[j] }
            }
        }
        if (roots.all { it == null }) return

        // La soglia: quanto somiglia la parola stessa con una lettera su quattro cancellata.
        var thresh = 0
        for (sp in 1..3) {
            val mw = word.toCharArray()
            var k = sp
            while (k < n) { mw[k] = '*'; k += 4 }
            thresh += ngram(n, word, String(mw), NGRAM_ANY_MISMATCH or NGRAM_LOWERING)
        }
        thresh = thresh / 3 - 1

        val guess = arrayOfNulls<String>(MAX_GUESS)
        val gscore = IntArray(MAX_GUESS) { -100 * it }
        lp = MAX_GUESS - 1
        for (r in 0 until MAX_ROOTS) {
            val root = roots[r] ?: continue
            for (form in expand(root, rootFlags[r]!!, word)) {
                val sc = ngram(n, word, form, NGRAM_ANY_MISMATCH or NGRAM_LOWERING) + leftCommon(word, form)
                if (sc > thresh && sc > gscore[lp] && form !in guess) {
                    gscore[lp] = sc
                    guess[lp] = form
                    var low = sc
                    for (j in 0 until MAX_GUESS) if (gscore[j] < low) { lp = j; low = gscore[j] }
                }
            }
            if (timeUp()) break
        }
        sortByScore(guess, gscore)

        // Il punteggio finale: sottosequenza comune, inizio uguale, lettere al loro posto.
        for (i in 0 until MAX_GUESS) {
            val g = guess[i] ?: continue
            val gl = d.lower(g)
            val len = gl.length
            val lcs = lcsLength(word, gl)
            if (n == len && n == lcs) {
                gscore[i] += 2000
                break
            }
            var re = ngram(2, word, gl, NGRAM_ANY_MISMATCH or NGRAM_WEIGHTED)
            if (re != 0) re += ngram(2, gl, word, NGRAM_ANY_MISMATCH or NGRAM_WEIGHTED)
            val (common, swapped) = commonPositions(word, gl)
            gscore[i] = 2 * lcs - abs(n - len) +
                leftCommon(word, gl) +
                (if (common > 0) 1 else 0) +
                (if (swapped) 10 else 0) +
                ngram(4, word, gl, NGRAM_ANY_MISMATCH or NGRAM_LOWERING) +
                re +
                (if (re < n + len) -1000 else 0)
        }
        sortByScore(guess, gscore)

        val oldCount = out.size
        var same = false
        for (i in 0 until MAX_GUESS) {
            val g = guess[i] ?: continue
            if (out.size >= oldCount + MAX_NGRAM_SUGS || out.size >= MAX_SUG) break
            if (same && gscore[i] <= 1000) continue
            if (gscore[i] > 1000) same = true
            else if (gscore[i] < -100) {
                same = true
                if (out.size > oldCount) continue
            }
            if (out.any { g.contains(it) } || !goodSug(g, false)) continue
            out += g
        }
    }

    /** Le forme di una radice che possono somigliare a [bad]: solo le desinenze e i prefissi che [bad] ha davvero. */
    private fun expand(root: String, flags: String, bad: String): List<String> {
        val forms = ArrayList<String>()
        val cross = ArrayList<Boolean>()
        if (!has(flags, aff.needAffix) && !has(flags, aff.onlyInCompound)) {
            forms += root
            cross += false
        }
        for (f in flags) {
            for (se in aff.suffixesByFlag[f] ?: continue) {
                if ((se.append.isEmpty() || (bad.length > se.append.length && bad.endsWith(se.append))) && !restricted(se)) {
                    suffixAdd(se, root)?.let { forms += it; cross += se.cross }
                }
            }
        }
        val suffixed = forms.size
        for (j in 0 until suffixed) {
            if (!cross[j]) continue
            for (f in flags) {
                for (pe in aff.prefixesByFlag[f] ?: continue) {
                    if (pe.cross && (pe.append.isEmpty() || (bad.length > pe.append.length && bad.startsWith(pe.append)))) {
                        prefixAdd(pe, forms[j])?.let { forms += it; cross += false }
                    }
                }
            }
        }
        for (f in flags) {
            for (pe in aff.prefixesByFlag[f] ?: continue) {
                if ((pe.append.isEmpty() || (bad.length > pe.append.length && bad.startsWith(pe.append))) && !restricted(pe)) {
                    prefixAdd(pe, root)?.let { forms += it }
                }
            }
        }
        return if (forms.size > MAX_WORDS) forms.subList(0, MAX_WORDS) else forms
    }

    private fun restricted(a: Affix) =
        has(a.cont, aff.needAffix) || has(a.cont, aff.circumfix) || has(a.cont, aff.onlyInCompound)

    private fun suffixAdd(se: Affix, word: String): String? {
        val len = word.length
        if ((len > se.strip.length || (len == 0 && aff.fullStrip)) && len >= se.condition.size &&
            se.condition.matchesEnd(word) && word.endsWith(se.strip)
        ) return word.substring(0, len - se.strip.length) + se.append
        return null
    }

    private fun prefixAdd(pe: Affix, word: String): String? {
        val len = word.length
        if ((len > pe.strip.length || (len == 0 && aff.fullStrip)) && len >= pe.condition.size &&
            pe.condition.matchesStart(word) && word.startsWith(pe.strip)
        ) return pe.append + word.substring(pe.strip.length)
        return null
    }

    /** `SuggestMgr::ngram`: quanti pezzi di 1, 2... [n] lettere di [s1] ci sono anche in [s2]. */
    private fun ngram(n: Int, s1: String, s2in: String, opt: Int): Int {
        val l2 = s2in.length
        if (l2 == 0) return 0
        val l1 = s1.length
        val t = if (opt and NGRAM_LOWERING != 0 && s2in.any { Character.isUpperCase(it) }) d.lower(s2in) else s2in
        var nscore = 0
        for (j in 1..n) {
            var ns = 0
            for (i in 0..l1 - j) {
                if (containsRegion(t, s1, i, j)) ns++
                else if (opt and NGRAM_WEIGHTED != 0) {
                    ns--
                    if (i == 0 || i == l1 - j) ns--
                }
            }
            nscore += ns
            if (ns < 2 && opt and NGRAM_WEIGHTED == 0) break
        }
        var ns = 0
        if (opt and NGRAM_LONGER_WORSE != 0) ns = (l2 - l1) - 2
        if (opt and NGRAM_ANY_MISMATCH != 0) ns = abs(l2 - l1) - 2
        return nscore - (if (ns > 0) ns else 0)
    }

    private fun containsRegion(hay: String, s: String, from: Int, len: Int): Boolean {
        for (start in 0..hay.length - len) if (hay.regionMatches(start, s, from, len)) return true
        return false
    }

    /** Quante lettere uguali all'inizio (la prima anche se nel dizionario è maiuscola). */
    private fun leftCommon(s1: String, s2: String): Int {
        if (s1.isEmpty() || s2.isEmpty()) return 0
        if (s1[0] != s2[0] && s1[0] != s2[0].lowercaseChar()) return 0
        var i = 1
        while (i < s1.length && i < s2.length && s1[i] == s2[i]) i++
        return i
    }

    private fun lcsLength(a: String, b: String): Int {
        val prev = IntArray(b.length + 1)
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                cur[j] = if (a[i - 1] == b[j - 1]) prev[j - 1] + 1 else maxOf(prev[j], cur[j - 1])
            }
            System.arraycopy(cur, 0, prev, 0, cur.size)
        }
        return prev[b.length]
    }

    /** Le lettere allo stesso posto, e se le due parole differiscono solo per due lettere scambiate. */
    private fun commonPositions(s1: String, s2: String): Pair<Int, Boolean> {
        var num = 0
        var diff = 0
        val diffPos = IntArray(2)
        var i = 0
        while (i < s1.length && i < s2.length) {
            if (s1[i] == s2[i]) num++
            else {
                if (diff < 2) diffPos[diff] = i
                diff++
            }
            i++
        }
        val swapped = diff == 2 && s1.length == s2.length &&
            s1[diffPos[0]] == s2[diffPos[1]] && s1[diffPos[1]] == s2[diffPos[0]]
        return num to swapped
    }

    private fun sortByScore(words: Array<String?>, scores: IntArray) {
        val order = words.indices.sortedWith(compareByDescending<Int> { if (words[it] == null) Int.MIN_VALUE else scores[it] })
        val w = order.map { words[it] }
        val s = order.map { scores[it] }
        for (i in words.indices) {
            words[i] = w[i]
            scores[i] = s[i]
        }
    }

    private fun swap(c: CharArray, i: Int, j: Int) {
        val t = c[i]
        c[i] = c[j]
        c[j] = t
    }

    companion object {
        private const val TIME_LIMIT_NS = 1_200_000_000L
        private const val MAX_SUG = 15
        private const val MAX_COMPOUND_SUGS = 3
        private const val MAX_NGRAM_SUGS = 4
        private const val MAX_CHAR_DISTANCE = 4
        private const val MAX_ROOTS = 100
        private const val MAX_GUESS = 200
        private const val MAX_WORDS = 100
        private const val MAP_BUDGET = 3000

        private const val NGRAM_LONGER_WORSE = 1
        private const val NGRAM_ANY_MISMATCH = 2
        private const val NGRAM_LOWERING = 4
        private const val NGRAM_WEIGHTED = 8

        /**
         * Errori tipici che il dizionario italiano non elenca: la "q" di
         * "acqua" e di "scuola", e gli apostrofi sbagliati più comuni.
         */
        private val EXTRA_REP = mapOf(
            "it" to listOf(
                RepRule("q", "cq", false, false),
                RepRule("cq", "q", false, false),
                RepRule("qu", "cu", false, false),
                RepRule("cu", "qu", false, false),
                RepRule("pò", "po'", false, true),
                RepRule("cè", "c'è", true, true),
                RepRule("qual'", "qual ", true, false)
            )
        )

        /** Le paroline di una o due lettere che possono stare da sole in "due parole attaccate". */
        private val SHORT_WORDS = mapOf(
            "en" to setOf("a", "i", "an", "in", "on", "at", "of", "to", "is", "it", "be", "by", "as", "or", "if", "we", "he", "me", "my", "so", "no", "do", "go", "up", "us", "am"),
            "it" to setOf("a", "e", "è", "o", "i", "il", "la", "lo", "le", "di", "da", "in", "su", "un", "ci", "ne", "si", "mi", "ti", "vi", "se", "ma", "ho", "ha", "fa", "va", "tu", "io"),
            "de" to setOf("du", "er", "es", "im", "am", "um", "zu", "in", "an", "ob", "so", "wo", "da", "ja"),
            "fr" to setOf("a", "à", "y", "le", "la", "de", "du", "un", "en", "et", "il", "je", "tu", "on", "ne", "se", "ce", "ma", "ta", "sa", "au", "ou", "où", "ni", "si"),
            "es" to setOf("a", "e", "o", "u", "y", "el", "la", "lo", "de", "en", "un", "se", "me", "te", "le", "mi", "tu", "su", "ya", "no", "si", "al", "yo")
        )
    }
}
