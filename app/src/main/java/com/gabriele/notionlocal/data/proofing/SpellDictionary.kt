package com.gabriele.notionlocal.data.proofing

import java.io.BufferedReader
import java.text.Normalizer
import java.util.Locale

/**
 * Un dizionario di **Hunspell** — il formato dei correttori di LibreOffice,
 * Firefox e Chrome — letto e usato da Kotlin, senza librerie native.
 *
 * Un dizionario di Hunspell sono due file. Il `.dic` elenca le **radici**,
 * ognuna con le sue bandierine: `parlare/ALKh`. Il `.aff` dice cosa vuol
 * dire ogni bandierina: quali **desinenze** e quali **prefissi** quella
 * radice accetta, e a che condizioni ("togli `are`, aggiungi `iamo`, se la
 * parola finisce in `are`"). Così novantamila righe bastano per milioni di
 * forme: "parliamo" non è scritta da nessuna parte, la si trova togliendo
 * `iamo` e rimettendo `are`.
 *
 * Il controllo segue passo passo quello di Hunspell (hunspell.cxx,
 * affixmgr.cxx), solo per le regole che usano i sei dizionari dell'app —
 * inglese americano e britannico, italiano, tedesco, francese, spagnolo:
 *  - prefissi e desinenze, insieme quando la regola lo permette, e **due
 *    desinenze una sull'altra** (la seconda la permette la prima: lo
 *    spagnolo "dámelo", il francese "l'homme", dove l'elisione la concede
 *    la desinenza vuota del plurale);
 *  - le bandierine speciali: `NEEDAFFIX` (radice che da sola non è una
 *    parola), `ONLYINCOMPOUND` (solo dentro una parola composta),
 *    `FORBIDDENWORD`, `KEEPCASE` (non cambia maiuscole), `CIRCUMFIX`
 *    (prefisso e desinenza solo insieme), `NOSUGGEST`;
 *  - le **parole composte del tedesco** ("Haustür" = "Haus" + "tür"), con
 *    `COMPOUNDBEGIN`, `COMPOUNDMIDDLE`, `COMPOUNDEND`, `COMPOUNDPERMITFLAG`
 *    e `COMPOUNDMIN`;
 *  - le maiuscole: "Casa" va bene se c'è "casa", "CASA" anche; "Roma"
 *    scritta "roma" no; il tedesco "STRASSE" si ritrova in "Straße";
 *  - `BREAK`: "ben-noto" si controlla a pezzi se intero non c'è;
 *  - `ICONV`: l'apostrofo curvo diventa dritto prima di cercare.
 *
 * Non c'è `COMPOUNDRULE`: nei dizionari inglesi serve solo agli ordinali
 * scritti in cifre ("21st"), e le parole con numeri non si controllano.
 *
 * Un dizionario letto non cambia più: lo si può usare da più thread.
 */
class SpellDictionary private constructor(
    internal val locale: Locale,
    internal val aff: AffixData,
    /** Le radici: le bandierine di ognuna, o un `Array<String>` se la stessa parola ha più righe. */
    internal val words: HashMap<String, Any>
) {

    /** Quante radici ha il dizionario. */
    val size: Int get() = words.size

    /**
     * Se [word] è scritta giusta. È una parola sola, senza spazi intorno;
     * apostrofi curvi e lettere accentate scomposte vanno bene.
     */
    fun check(word: String): Boolean {
        val w = normalize(word)
        if (w.isEmpty()) return true
        return spell(w, 0)
    }

    /** L'apostrofo come lo vuole il dizionario, e le lettere accentate in un segno solo. */
    internal fun normalize(word: String): String {
        var w = Normalizer.normalize(word.trim(), Normalizer.Form.NFC)
        if (aff.iconv.isNotEmpty()) w = convert(w, aff.iconv)
        // Tutti e sei i dizionari scrivono l'apostrofo dritto.
        return w.replace('’', '\'').replace('ʼ', '\'').replace('‘', '\'')
    }

    // ---- Controllo di una parola intera: maiuscole e BREAK -------------

    internal fun spell(word: String, depth: Int): Boolean {
        val info = CheckInfo()
        if (spellCases(word, info)) return true
        if (info.forbidden || depth > 3) return false
        return spellBreaks(word, depth)
    }

    /** `Hunspell::spell`: la parola com'è, e poi minuscola o con l'iniziale grande, secondo le maiuscole che ha. */
    private fun spellCases(word: String, info: CheckInfo): Boolean {
        when (capType(word)) {
            CAP_NONE, CAP_MIXED, CAP_MIXED_INIT -> return checkWord(word, info) != null
            CAP_ALL -> {
                if (checkWord(word, info) != null) return true
                // "DELL'ANNO" → "dell'Anno", "Dell'Anno".
                val apos = word.indexOf('\'')
                if (apos >= 0 && apos < word.length - 1) {
                    val low = lower(word)
                    val p1 = low.substring(0, apos + 1)
                    val p2 = initCap(low.substring(apos + 1))
                    if (checkWord(p1 + p2, info) != null) return true
                    if (checkWord(initCap(p1) + p2, info) != null) return true
                }
                if (aff.checkSharps && word.contains("SS")) {
                    val low = lower(word)
                    if (spellSharps(low, 0, 0, 0, info)) return true
                    if (spellSharps(initCap(low), 0, 0, 0, info)) return true
                }
                val init = checkWord(initCap(lower(word)), info)
                if (info.forbidden) return false
                if (init != null && !has(init, aff.keepCase)) return true
                val low = checkWord(lower(word), info)
                return low != null && !has(low, aff.keepCase)
            }
            else -> { // Iniziale maiuscola
                if (checkWord(word, info) != null) return true
                if (info.forbidden) return false
                val low = lower(word)
                val rv = checkWord(low, info) ?: return false
                return !has(rv, aff.keepCase) || (aff.checkSharps && low.contains('ß'))
            }
        }
    }

    /** "STRASSE": prova ogni "ss" anche come "ß", fino a cinque. */
    private fun spellSharps(base: String, from: Int, n: Int, replaced: Int, info: CheckInfo): Boolean {
        val pos = base.indexOf("ss", from)
        if (pos >= 0 && n < 5) {
            val sharp = base.substring(0, pos) + "ß" + base.substring(pos + 2)
            if (spellSharps(sharp, pos + 1, n + 1, replaced + 1, info)) return true
            return spellSharps(base, pos + 2, n + 1, replaced, info)
        }
        return replaced > 0 && checkWord(base, info) != null
    }

    /** I punti dove una parola si può spezzare (`BREAK`): "ben-noto" va bene se vanno bene "ben" e "noto". */
    private fun spellBreaks(word: String, depth: Int): Boolean {
        val breaks = aff.breaks
        if (breaks.isEmpty()) return false
        var count = 0
        for (b in breaks) {
            val plain = b.removePrefix("^").removeSuffix("$")
            if (plain.isEmpty()) continue
            var pos = word.indexOf(plain)
            while (pos >= 0) { count++; pos = word.indexOf(plain, pos + plain.length) }
        }
        if (count >= 10) return false
        val wl = word.length
        // Solo all'inizio o solo in fondo: "^'" e "'$" del francese.
        for (b in breaks) {
            val plen = b.length
            if (plen == 1 || plen > wl) continue
            if (b[0] == '^' && word.startsWith(b.substring(1)) && spell(word.substring(plen - 1), depth + 1)) return true
            if (b[plen - 1] == '$' && word.endsWith(b.substring(0, plen - 1)) && spell(word.substring(0, wl - plen + 1), depth + 1)) return true
        }
        // Al secondo punto, per le parole del dizionario che il segno ce l'hanno già ("E-Mail-Adresse").
        for (b in breaks) {
            val plen = b.length
            var found = word.indexOf(b)
            if (found > 0 && found < wl - plen) {
                val found2 = word.indexOf(b, found + 1)
                if (found2 > 0 && found2 < wl - plen) found = found2
                if (!spell(word.substring(found + plen), depth + 1)) continue
                if (spell(word.substring(0, found), depth + 1)) return true
            }
        }
        for (b in breaks) {
            val plen = b.length
            val found = word.indexOf(b)
            if (found > 0 && found < wl - plen) {
                if (!spell(word.substring(found + plen), depth + 1)) continue
                if (spell(word.substring(0, found), depth + 1)) return true
            }
        }
        return false
    }

    // ---- Una forma esatta: radice, prefissi e desinenze, composti -------

    internal class CheckInfo { var forbidden = false }

    /**
     * `Hunspell::checkword`: la forma così com'è, maiuscole comprese.
     * Restituisce le bandierine della radice trovata, o `null`.
     */
    internal fun checkWord(word: String, info: CheckInfo): String? {
        val direct = words[word]
        if (direct != null) {
            if (anyHomonym(direct) { has(it, aff.forbidden) } != null) {
                info.forbidden = true
                return null
            }
            val found = anyHomonym(direct) { !has(it, aff.needAffix) && !has(it, aff.onlyInCompound) }
            if (found != null) return found
        }
        var rv = affixCheck(word, NONE, CPD_NOT)
        if (rv != null && has(rv, aff.onlyInCompound)) rv = null
        if (rv != null) {
            if (has(rv, aff.forbidden)) {
                info.forbidden = true
                return null
            }
            return rv
        }
        if (aff.compounds && compoundCheck(word, 0, 0)) return ""
        return null
    }

    internal inline fun anyHomonym(entry: Any, test: (String) -> Boolean): String? {
        if (entry is String) return if (test(entry)) entry else null
        @Suppress("UNCHECKED_CAST")
        for (h in entry as Array<String>) if (test(h)) return h
        return null
    }

    internal fun homonym(word: String, test: (String) -> Boolean): String? {
        val entry = words[word] ?: return null
        return anyHomonym(entry, test)
    }

    /** `AffixMgr::affix_check`. */
    internal fun affixCheck(word: String, needFlag: Char, inCpd: Int): String? {
        prefixCheck(word, inCpd, needFlag)?.let { return it }
        suffixCheck(word, false, null, NONE, needFlag, inCpd)?.let { return it }
        if (aff.contFlags.isNotEmpty()) {
            suffixCheckTwoSfx(word, false, null, needFlag)?.let { return it }
            prefixCheckTwoSfx(word, CPD_NOT, needFlag)?.let { return it }
        }
        return null
    }

    /** I prefissi che la parola può avere: quelli che aggiungono il suo inizio, o niente. */
    internal inline fun forPrefixes(word: String, action: (Affix) -> String?): String? {
        val max = minOf(word.length, aff.maxPrefix)
        for (k in 0..max) {
            val list = aff.prefixes[if (k == 0) "" else word.substring(0, k)] ?: continue
            for (pe in list) action(pe)?.let { return it }
        }
        return null
    }

    internal inline fun forSuffixes(word: String, action: (Affix) -> String?): String? {
        val max = minOf(word.length, aff.maxSuffix)
        for (k in 0..max) {
            val list = aff.suffixes[if (k == 0) "" else word.substring(word.length - k)] ?: continue
            for (se in list) action(se)?.let { return it }
        }
        return null
    }

    /** `AffixMgr::prefix_check`. */
    internal fun prefixCheck(word: String, inCpd: Int, needFlag: Char): String? = forPrefixes(word) { pe ->
        if ((inCpd != CPD_NOT || !has(pe.cont, aff.onlyInCompound)) &&
            (inCpd != CPD_END || has(pe.cont, aff.compoundPermit))
        ) prefixCheckWord(pe, word, inCpd, needFlag) else null
    }

    /** `PfxEntry::checkword`: togli il prefisso, rimetti quello che aveva tolto, cerca la radice. */
    private fun prefixCheckWord(pe: Affix, word: String, inCpd: Int, needFlag: Char): String? {
        val tmpl = word.length - pe.append.length
        if (tmpl > 0 || (tmpl == 0 && aff.fullStrip)) {
            val root = pe.strip + word.substring(pe.append.length)
            if (pe.condition.matchesStart(root)) {
                homonym(root) { f ->
                    has(f, pe.flag) && !has(pe.cont, aff.needAffix) &&
                        (needFlag == NONE || has(f, needFlag) || has(pe.cont, needFlag))
                }?.let { return it }
                if (pe.cross) suffixCheck(root, true, pe, NONE, needFlag, inCpd)?.let { return it }
            }
        }
        return null
    }

    /** `AffixMgr::suffix_check`: [ppfx] è il prefisso già tolto, [cclass] la desinenza esterna già tolta. */
    internal fun suffixCheck(word: String, cross: Boolean, ppfx: Affix?, cclass: Char, needFlag: Char, inCpd: Int): String? =
        forSuffixes(word) { se ->
            if (suffixAllowed(se, ppfx, cclass, inCpd) &&
                (se.append.isEmpty() || inCpd != CPD_END || ppfx != null || !has(se.cont, aff.onlyInCompound))
            ) suffixCheckWord(se, word, cross, ppfx, cclass, needFlag, if (inCpd != CPD_NOT) NONE else aff.onlyInCompound)
            else null
        }

    private fun suffixAllowed(se: Affix, ppfx: Affix?, cclass: Char, inCpd: Int): Boolean {
        // Le desinenze non vanno all'inizio di un composto, se non lo permettono.
        if (inCpd == CPD_BEGIN && !has(se.cont, aff.compoundPermit)) return false
        if (aff.circumfix != NONE) {
            val p = ppfx != null && has(ppfx.cont, aff.circumfix)
            val s = has(se.cont, aff.circumfix)
            if (p != s) return false
        }
        if (inCpd == CPD_NOT && has(se.cont, aff.onlyInCompound)) return false
        if (cclass == NONE && has(se.cont, aff.needAffix) && (ppfx == null || has(ppfx.cont, aff.needAffix))) return false
        return true
    }

    /** `SfxEntry::checkword`. */
    private fun suffixCheckWord(
        se: Affix, word: String, cross: Boolean, ppfx: Affix?, cclass: Char, needFlag: Char, badFlag: Char
    ): String? {
        if (cross && !se.cross) return null
        val tmpl = word.length - se.append.length
        if ((tmpl > 0 || (tmpl == 0 && aff.fullStrip)) && tmpl + se.strip.length >= se.condition.size) {
            val root = word.substring(0, tmpl) + se.strip
            if (se.condition.matchesEnd(root)) {
                return homonym(root) { f ->
                    (has(f, se.flag) || (ppfx != null && has(ppfx.cont, se.flag))) &&
                        (!cross || (ppfx != null && (has(f, ppfx.flag) || has(se.cont, ppfx.flag)))) &&
                        (cclass == NONE || has(se.cont, cclass)) &&
                        (badFlag == NONE || !has(f, badFlag)) &&
                        (needFlag == NONE || has(f, needFlag) || has(se.cont, needFlag))
                }
            }
        }
        return null
    }

    /** `AffixMgr::suffix_check_twosfx`: una desinenza sopra un'altra. */
    internal fun suffixCheckTwoSfx(word: String, cross: Boolean, ppfx: Affix?, needFlag: Char): String? =
        forSuffixes(word) { se ->
            if (aff.contFlags.indexOf(se.flag) >= 0) suffixCheckTwoSfxWord(se, word, cross, ppfx, needFlag) else null
        }

    private fun suffixCheckTwoSfxWord(se: Affix, word: String, cross: Boolean, ppfx: Affix?, needFlag: Char): String? {
        if (cross && !se.cross) return null
        val tmpl = word.length - se.append.length
        if ((tmpl > 0 || (tmpl == 0 && aff.fullStrip)) && tmpl + se.strip.length >= se.condition.size) {
            val root = word.substring(0, tmpl) + se.strip
            if (se.condition.matchesEnd(root)) {
                return if (ppfx != null && !has(se.cont, ppfx.flag)) {
                    suffixCheck(root, cross, ppfx, se.flag, needFlag, CPD_NOT)
                } else {
                    suffixCheck(root, false, null, se.flag, needFlag, CPD_NOT)
                }
            }
        }
        return null
    }

    /** `AffixMgr::prefix_check_twosfx`: un prefisso e due desinenze. */
    internal fun prefixCheckTwoSfx(word: String, inCpd: Int, needFlag: Char): String? = forPrefixes(word) { pe ->
        val tmpl = word.length - pe.append.length
        if ((tmpl > 0 || (tmpl == 0 && aff.fullStrip)) && pe.cross && inCpd != CPD_BEGIN) {
            val root = pe.strip + word.substring(pe.append.length)
            if (pe.condition.matchesStart(root)) suffixCheckTwoSfx(root, true, pe, needFlag) else null
        } else null
    }

    /** Se [word] è una parola composta valida, come la vogliono i suggerimenti. */
    internal fun checkCompound(word: String): Boolean = aff.compounds && compoundCheck(word, 0, 0)

    /**
     * `AffixMgr::compound_check`, per i composti a bandierine del tedesco:
     * la prima parte con `COMPOUNDBEGIN`, quelle in mezzo con
     * `COMPOUNDMIDDLE`, l'ultima con `COMPOUNDEND` — o `COMPOUNDFLAG`
     * ovunque. Ogni parte è lunga almeno `COMPOUNDMIN`.
     */
    private fun compoundCheck(word: String, wordNum: Int, depth: Int): Boolean {
        val n = word.length
        val cmin = aff.compoundMin
        if (n < cmin * 2 || depth > 8) return false
        val partFlag = if (wordNum == 0) aff.compoundBegin else aff.compoundMiddle
        for (i in cmin..n - cmin) {
            val st = word.substring(0, i)
            var rv = homonym(st) { f ->
                !has(f, aff.needAffix) && (has(f, aff.compoundFlag) || has(f, partFlag))
            }
            if (rv == null && aff.compoundFlag != NONE) {
                rv = prefixCheck(st, CPD_BEGIN, aff.compoundFlag)
                    ?: suffixCheck(st, false, null, NONE, aff.compoundFlag, CPD_BEGIN)
            }
            if (rv == null && partFlag != NONE) {
                rv = suffixCheck(st, false, null, NONE, partFlag, CPD_BEGIN)
                    ?: prefixCheck(st, CPD_BEGIN, partFlag)
            }
            if (rv == null || has(rv, aff.forbidden)) continue
            val rest = word.substring(i)
            val end = homonym(rest) { f ->
                !has(f, aff.needAffix) && (has(f, aff.compoundFlag) || has(f, aff.compoundEnd))
            }
            if (end != null) {
                if (has(end, aff.forbidden)) continue
                return true
            }
            var affixed: String? = null
            if (aff.compoundFlag != NONE) affixed = affixCheck(rest, aff.compoundFlag, CPD_END)
            if (affixed == null && aff.compoundEnd != NONE) affixed = affixCheck(rest, aff.compoundEnd, CPD_END)
            if (affixed != null) {
                if (has(affixed, aff.forbidden)) continue
                return true
            }
            if (compoundCheck(rest, wordNum + 1, depth + 1)) return true
        }
        return false
    }

    // ---- Maiuscole -------------------------------------------------------

    internal fun lower(w: String) = w.lowercase(locale)

    internal fun initCap(w: String) = if (w.isEmpty()) w else w.substring(0, 1).uppercase(locale) + w.substring(1)

    internal fun capType(w: String): Int {
        var ncap = 0
        var neutral = 0
        var letters = 0
        for (c in w) {
            letters++
            if (Character.isUpperCase(c) || Character.isTitleCase(c)) ncap++
            else if (!Character.isLowerCase(c)) neutral++
        }
        val firstCap = w.isNotEmpty() && (Character.isUpperCase(w[0]) || Character.isTitleCase(w[0]))
        return when {
            ncap == 0 -> CAP_NONE
            ncap == 1 && firstCap -> CAP_INIT
            ncap == letters || ncap + neutral == letters -> CAP_ALL
            ncap > 1 && firstCap -> CAP_MIXED_INIT
            else -> CAP_MIXED
        }
    }

    companion object {
        internal const val CAP_NONE = 0
        internal const val CAP_INIT = 1
        internal const val CAP_ALL = 2
        internal const val CAP_MIXED = 3
        internal const val CAP_MIXED_INIT = 4

        internal const val CPD_NOT = 0
        internal const val CPD_BEGIN = 1
        internal const val CPD_END = 2

        /**
         * Legge un dizionario: il file delle regole [aff] e quello delle
         * radici [dic], tutti e due in UTF-8. [locale] decide minuscole e
         * maiuscole (la "ß" tedesca, per esempio).
         */
        fun read(aff: BufferedReader, dic: BufferedReader, locale: Locale): SpellDictionary {
            val data = AffixData.parse(aff)
            return SpellDictionary(locale, data, readWords(dic, data))
        }

        private fun readWords(dic: BufferedReader, aff: AffixData): HashMap<String, Any> {
            val first = dic.readLine()?.trim().orEmpty()
            val expected = first.toIntOrNull() ?: 0
            val words = HashMap<String, Any>(maxOf(16, expected * 4 / 3 + 1))
            val flagCache = HashMap<String, String>()
            while (true) {
                val raw = dic.readLine() ?: break
                var line = raw
                // I campi morfologici dopo una tabulazione, o dopo uno spazio come " po:nome".
                val tab = line.indexOf('\t')
                if (tab >= 0) line = line.substring(0, tab)
                line = cutMorphology(line).trimEnd()
                if (line.isEmpty()) continue
                var slash = -1
                var i = line.indexOf('/')
                while (i >= 0) {
                    if (i > 0 && line[i - 1] == '\\') { i = line.indexOf('/', i + 1); continue }
                    slash = i
                    break
                }
                val word = (if (slash >= 0) line.substring(0, slash) else line).replace("\\/", "/")
                if (word.isEmpty() || word.contains(' ')) continue
                val flags = if (slash >= 0) {
                    val text = line.substring(slash + 1)
                    flagCache.getOrPut(text) { aff.flags(text) }
                } else ""
                when (val old = words[word]) {
                    null -> words[word] = flags
                    is String -> words[word] = arrayOf(old, flags)
                    else -> {
                        @Suppress("UNCHECKED_CAST")
                        words[word] = (old as Array<String>) + flags
                    }
                }
            }
            return words
        }

        private fun cutMorphology(line: String): String {
            var p = line.indexOf(':')
            while (p >= 0) {
                if (p >= 3 && line[p - 3] == ' ') return line.substring(0, p - 3)
                p = line.indexOf(':', p + 1)
            }
            return line
        }

        /** `ICONV` e simili: a ogni posto, la regola più lunga che ci sta. */
        internal fun convert(word: String, table: List<Pair<String, String>>): String {
            if (table.none { word.contains(it.first) }) return word
            val sb = StringBuilder()
            var i = 0
            while (i < word.length) {
                var best: Pair<String, String>? = null
                for (rule in table) {
                    if (word.startsWith(rule.first, i) && (best == null || rule.first.length > best.first.length)) best = rule
                }
                if (best != null) {
                    sb.append(best.second)
                    i += best.first.length
                } else {
                    sb.append(word[i])
                    i++
                }
            }
            return sb.toString()
        }
    }
}

/** Nessuna bandierina: [has] con questa dà sempre falso. */
internal const val NONE = '\u0000'

internal fun has(flags: String, flag: Char) = flag != NONE && flags.indexOf(flag) >= 0

/**
 * Una regola di prefisso o di desinenza: togli [strip], aggiungi
 * [append], se la parola rispetta [condition]. [cont] sono le bandierine
 * che la forma ottenuta si porta dietro: un'altra desinenza permessa,
 * `ONLYINCOMPOUND`, la parte di composto che può fare.
 */
internal class Affix(
    val flag: Char,
    val cross: Boolean,
    val strip: String,
    val append: String,
    val cont: String,
    val condition: AffixCondition
)

/**
 * La condizione di una regola: una lettera per posto, come `[^aeiou]y` —
 * una lettera fra quelle, una non fra quelle, o `.` qualunque. Per una
 * desinenza si guarda la fine della radice, per un prefisso l'inizio.
 */
internal class AffixCondition private constructor(
    private val sets: Array<String?>,
    private val negated: BooleanArray
) {
    val size: Int get() = sets.size

    fun matchesEnd(word: String): Boolean {
        if (word.length < sets.size) return false
        val start = word.length - sets.size
        for (i in sets.indices) if (!matchAt(i, word[start + i])) return false
        return true
    }

    fun matchesStart(word: String): Boolean {
        if (word.length < sets.size) return false
        for (i in sets.indices) if (!matchAt(i, word[i])) return false
        return true
    }

    private fun matchAt(i: Int, c: Char): Boolean {
        val set = sets[i] ?: return true
        return (set.indexOf(c) >= 0) != negated[i]
    }

    companion object {
        private val ANY = AffixCondition(emptyArray(), BooleanArray(0))

        /** [cache]: le stesse condizioni tornano migliaia di volte in un dizionario. */
        fun parse(text: String, cache: HashMap<String, AffixCondition>): AffixCondition {
            if (text == ".") return ANY
            return cache.getOrPut(text) {
                val sets = ArrayList<String?>()
                val neg = ArrayList<Boolean>()
                var i = 0
                while (i < text.length) {
                    when (val c = text[i]) {
                        '[' -> {
                            val close = text.indexOf(']', i + 1).let { if (it < 0) text.length else it }
                            var body = text.substring(i + 1, close)
                            val n = body.startsWith("^")
                            if (n) body = body.substring(1)
                            sets += body
                            neg += n
                            i = close + 1
                        }
                        '.' -> { sets += null; neg += false; i++ }
                        else -> { sets += c.toString(); neg += false; i++ }
                    }
                }
                AffixCondition(sets.toTypedArray(), neg.toBooleanArray())
            }
        }
    }
}

/** Una riga `REP`: [from] si sostituisce con [to]; `^` e `$` la legano all'inizio o alla fine della parola. */
internal class RepRule(val from: String, val to: String, val atStart: Boolean, val atEnd: Boolean)

internal enum class FlagMode { CHAR, LONG, NUM, UTF8 }

/** Tutto quello che dice il file `.aff`. */
internal class AffixData {
    var flagMode = FlagMode.CHAR
    private val flagIds = HashMap<String, Char>()

    val prefixes = HashMap<String, MutableList<Affix>>()
    val suffixes = HashMap<String, MutableList<Affix>>()
    val prefixesByFlag = HashMap<Char, MutableList<Affix>>()
    val suffixesByFlag = HashMap<Char, MutableList<Affix>>()
    var maxPrefix = 0
    var maxSuffix = 0
    /** Le bandierine di desinenza che compaiono dopo la "/" di un'altra regola: quelle che possono stare sopra un'altra desinenza. */
    var contFlags = ""

    var needAffix = NONE
    var onlyInCompound = NONE
    var forbidden = NONE
    var keepCase = NONE
    var circumfix = NONE
    var noSuggest = NONE
    var compoundFlag = NONE
    var compoundBegin = NONE
    var compoundMiddle = NONE
    var compoundEnd = NONE
    var compoundPermit = NONE
    var compoundMin = 3
    var fullStrip = false
    var checkSharps = false

    var tryChars = ""
    var keyboard = "qwertyuiop|asdfghjkl|zxcvbnm"
    val iconv = ArrayList<Pair<String, String>>()
    var breaks: List<String> = listOf("-", "^-", "-$")
    val rep = ArrayList<RepRule>()
    val map = ArrayList<List<String>>()

    /**
     * Le famiglie di lettere per i suggerimenti: quelle di `MAP`, più le
     * lettere accentate di `TRY` messe con la loro lettera semplice — lo
     * spagnolo non ha "n" e "ñ" in `MAP`, il tedesco nessuna `MAP`, ma
     * "ninos" deve dare "niños" e "schon" anche "schön". Col tedesco
     * (`CHECKSHARPS`) anche "s" e "ß": "Strase" → "Straße".
     */
    val families: List<List<String>> by lazy {
        val groups = map.map { it.toMutableList() }.toMutableList()
        if (checkSharps) groups += mutableListOf("s", "ß")
        val letters = (tryChars + map.flatten().joinToString("")).toSet()
        for (c in letters) {
            val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)[0]
            if (base == c || !Character.isLetter(base)) continue
            val group = groups.firstOrNull { base.toString() in it }
                ?: mutableListOf(base.toString()).also { groups += it }
            if (c.toString() !in group) group += c.toString()
        }
        groups
    }

    /** Se ci sono parole composte a bandierine (il tedesco). */
    val compounds: Boolean get() = compoundFlag != NONE || compoundBegin != NONE

    fun flag(token: String): Char = flagIds.getOrPut(token) { (flagIds.size + 1).toChar() }

    /** Le bandierine scritte in [text], nel formato del dizionario, come un carattere ciascuna. */
    fun flags(text: String): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder()
        when (flagMode) {
            FlagMode.CHAR -> for (c in text) sb.append(flag(c.toString()))
            FlagMode.LONG -> {
                var i = 0
                while (i < text.length) {
                    sb.append(flag(text.substring(i, minOf(i + 2, text.length))))
                    i += 2
                }
            }
            FlagMode.NUM -> for (t in text.split(',')) if (t.isNotBlank()) sb.append(flag(t.trim()))
            FlagMode.UTF8 -> {
                var i = 0
                while (i < text.length) {
                    val cp = text.codePointAt(i)
                    val n = Character.charCount(cp)
                    sb.append(flag(text.substring(i, i + n)))
                    i += n
                }
            }
        }
        return sb.toString()
    }

    private fun single(text: String?): Char = if (text.isNullOrEmpty()) NONE else flags(text).firstOrNull() ?: NONE

    companion object {
        private val SPACES = Regex("\\s+")

        fun parse(reader: BufferedReader): AffixData {
            val lines = reader.readLines()
            val data = AffixData()
            // Il formato delle bandierine serve prima di leggere qualunque regola.
            lines.firstOrNull { it.startsWith("FLAG ") }?.let {
                data.flagMode = when (it.split(SPACES).getOrNull(1)) {
                    "long" -> FlagMode.LONG
                    "num" -> FlagMode.NUM
                    "UTF-8" -> FlagMode.UTF8
                    else -> FlagMode.CHAR
                }
            }
            val pending = HashMap<String, Pair<Boolean, Int>>()
            var breaks: MutableList<String>? = null
            val contFlags = StringBuilder()
            val conditions = HashMap<String, AffixCondition>()
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val p = line.split(SPACES)
                when (p[0]) {
                    "TRY" -> data.tryChars = p.getOrElse(1) { "" }
                    "KEY" -> data.keyboard = p.getOrElse(1) { data.keyboard }
                    "NEEDAFFIX", "PSEUDOROOT" -> data.needAffix = data.single(p.getOrNull(1))
                    "ONLYINCOMPOUND" -> data.onlyInCompound = data.single(p.getOrNull(1))
                    "FORBIDDENWORD" -> data.forbidden = data.single(p.getOrNull(1))
                    "KEEPCASE" -> data.keepCase = data.single(p.getOrNull(1))
                    "CIRCUMFIX" -> data.circumfix = data.single(p.getOrNull(1))
                    "NOSUGGEST" -> data.noSuggest = data.single(p.getOrNull(1))
                    "COMPOUNDFLAG" -> data.compoundFlag = data.single(p.getOrNull(1))
                    "COMPOUNDBEGIN" -> data.compoundBegin = data.single(p.getOrNull(1))
                    "COMPOUNDMIDDLE" -> data.compoundMiddle = data.single(p.getOrNull(1))
                    "COMPOUNDEND", "COMPOUNDLAST" -> data.compoundEnd = data.single(p.getOrNull(1))
                    "COMPOUNDPERMITFLAG" -> data.compoundPermit = data.single(p.getOrNull(1))
                    "COMPOUNDMIN" -> data.compoundMin = (p.getOrNull(1)?.toIntOrNull() ?: 3).coerceAtLeast(1)
                    "FULLSTRIP" -> data.fullStrip = true
                    "CHECKSHARPS" -> data.checkSharps = true
                    "ICONV" -> if (p.size >= 3) data.iconv += p[1] to p[2]
                    "BREAK" -> {
                        if (breaks == null) breaks = ArrayList()
                        if (p.size >= 2 && !(p.size == 2 && p[1].toIntOrNull() != null)) breaks!!.add(p[1])
                    }
                    "REP" -> if (p.size >= 3) {
                        var from = p[1]
                        val atStart = from.startsWith("^")
                        if (atStart) from = from.substring(1)
                        val atEnd = from.endsWith("$")
                        if (atEnd) from = from.dropLast(1)
                        data.rep += RepRule(from.replace('_', ' '), p[2].replace('_', ' '), atStart, atEnd)
                    }
                    "MAP" -> if (p.size >= 2 && !(p.size == 2 && p[1].toIntOrNull() != null)) data.map += mapGroup(p[1])
                    "PFX", "SFX" -> {
                        if (p.size < 4) continue
                        val isPrefix = p[0] == "PFX"
                        val key = p[0] + " " + p[1]
                        val state = pending[key]
                        if (state == null || state.second <= 0) {
                            pending[key] = (p[2] == "Y") to (p[3].toIntOrNull() ?: 0)
                            continue
                        }
                        pending[key] = state.first to state.second - 1
                        val flag = data.flag(p[1])
                        val strip = p[2].let { if (it == "0") "" else it }
                        val appendField = p[3]
                        val slash = appendField.indexOf('/')
                        val append = (if (slash >= 0) appendField.substring(0, slash) else appendField).let { if (it == "0") "" else it }
                        val cont = if (slash >= 0) data.flags(appendField.substring(slash + 1)) else ""
                        for (c in cont) if (contFlags.indexOf(c) < 0) contFlags.append(c)
                        val affix = Affix(flag, state.first, strip, append, cont, AffixCondition.parse(p.getOrElse(4) { "." }, conditions))
                        if (isPrefix) {
                            data.prefixes.getOrPut(append) { ArrayList() } += affix
                            data.prefixesByFlag.getOrPut(flag) { ArrayList() } += affix
                            data.maxPrefix = maxOf(data.maxPrefix, append.length)
                        } else {
                            data.suffixes.getOrPut(append) { ArrayList() } += affix
                            data.suffixesByFlag.getOrPut(flag) { ArrayList() } += affix
                            data.maxSuffix = maxOf(data.maxSuffix, append.length)
                        }
                    }
                }
            }
            breaks?.let { data.breaks = it }
            data.contFlags = contFlags.toString()
            return data
        }

        /** Una riga `MAP`: lettere singole, o gruppi fra parentesi come "(ss)". */
        private fun mapGroup(text: String): List<String> {
            val out = ArrayList<String>()
            var i = 0
            while (i < text.length) {
                if (text[i] == '(') {
                    val close = text.indexOf(')', i + 1).let { if (it < 0) text.length else it }
                    out += text.substring(i + 1, close)
                    i = close + 1
                } else {
                    val n = Character.charCount(text.codePointAt(i))
                    out += text.substring(i, i + n)
                    i += n
                }
            }
            return out
        }
    }
}
