package com.gabriele.notionlocal.data

import android.content.Context
import android.graphics.Paint
import com.gabriele.notionlocal.data.entity.BlockAttrs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.Normalizer

/**
 * Le categorie delle emoji, nell'ordine e coi nomi del selettore di Notion
 * (foto dell'utente del 07/10/2026). `key` è il nome nel file
 * `assets/emoji/emoji.txt`.
 */
enum class EmojiCategory(val key: String) {
    PEOPLE("people"),
    NATURE("nature"),
    FOOD("food"),
    ACTIVITY("activity"),
    TRAVEL("travel"),
    OBJECTS("objects"),
    SYMBOLS("symbols"),
    FLAGS("flags")
}

/** Un'emoji del selettore; `acceptsTone` se ha le versioni coi colori della pelle. */
data class EmojiEntry(val emoji: String, val acceptsTone: Boolean)

/**
 * **Tutte le emoji del selettore delle icone** (dal 07/10/2026, chiesto
 * dall'utente: "le stesse emoji che mi si aprono quando le vedo su Notion").
 *
 * Vengono da Unicode 16.0 (`emoji-test.txt`), divise nelle categorie di
 * Notion, senza le varianti dei colori della pelle — quelle le fa
 * `withSkinTone`. I nomi per cercarle sono quelli del CLDR, **in tutte e
 * otto le lingue dell'app insieme**: "gatto" trova il gatto anche con l'app
 * in inglese. Gli asset si rigenerano con
 * `tools/emoji/make_emoji_assets.py`, per esempio con un Unicode più nuovo.
 */
class EmojiCatalog(
    val sections: List<Pair<EmojiCategory, List<EmojiEntry>>>,
    /**
     * Per ogni emoji i suoi nomi e le parole chiave in una stringa sola,
     * `"| gatto che sorride | gatto | cat face |"`: minuscoli, senza accenti,
     * ogni parola preceduta da uno spazio. Una stringa per emoji, non una
     * lista di parole: con otto lingue sarebbero centinaia di migliaia di
     * oggetti.
     */
    private val searchText: Map<String, String>
) {
    val all: List<EmojiEntry> = sections.flatMap { it.second }

    /**
     * Le emoji che hanno nei nomi o nelle parole chiave **tutte** le parole
     * scritte, ognuna come inizio di una parola ("gat" trova "gatto", non
     * "delicato"; in cinese e giapponese, che non separano le parole, basta
     * che ci sia), senza badare a maiuscole e accenti. Prima quelle con un
     * nome o una parola chiave uguale a quello che si è scritto, poi quelle
     * con le parole intere, poi le altre, ognuna nell'ordine di Notion.
     */
    fun search(query: String): List<EmojiEntry> {
        val wanted = searchWords(query)
        val tokens = wanted.split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        return all.mapNotNull { entry ->
            val text = searchText[entry.emoji] ?: return@mapNotNull null
            val matches = tokens.all { token ->
                text.contains(" $token") || (token.any(::isCjk) && text.contains(token))
            }
            if (!matches) return@mapNotNull null
            val rank = when {
                text.contains("| $wanted |") -> 0
                tokens.all { text.contains(" $it ") } -> 1
                else -> 2
            }
            rank to entry
        }.sortedBy { it.first }.map { it.second }
    }

    companion object {
        /**
         * Il catalogo dai due file: `structure` (le categorie e le emoji) e le
         * righe dei nomi, di più lingue insieme. `supported` dice se il
         * telefono sa disegnare un'emoji: quelle che non sa (troppo nuove)
         * restano fuori, invece di comparire come quadratini.
         */
        fun parse(structure: String, names: List<String>, supported: (String) -> Boolean): EmojiCatalog {
            val sections = mutableListOf<Pair<EmojiCategory, MutableList<EmojiEntry>>>()
            for (line in structure.lineSequence()) {
                if (line.isBlank()) continue
                // Le categorie cominciano con "@": "#" no, c'è l'emoji "#️⃣".
                if (line.startsWith("@")) {
                    val category = EmojiCategory.entries.firstOrNull { it.key == line.substring(1).trim() } ?: continue
                    sections += category to mutableListOf()
                    continue
                }
                val parts = line.split('\t')
                val emoji = parts[0]
                if (sections.isEmpty() || !supported(emoji)) continue
                sections.last().second += EmojiEntry(emoji, acceptsTone = parts.getOrNull(1) == "1")
            }
            val texts = HashMap<String, StringBuilder>()
            for (line in names) {
                val tab = line.indexOf('\t')
                if (tab <= 0) continue
                val text = texts.getOrPut(line.substring(0, tab)) { StringBuilder("|") }
                line.substring(tab + 1).split('|').forEach { part ->
                    val words = searchWords(part)
                    if (words.isNotEmpty()) text.append(' ').append(words).append(" |")
                }
            }
            return EmojiCatalog(sections.filter { it.second.isNotEmpty() }, texts.mapValues { it.value.toString() })
        }
    }
}

/** Le parole di un testo per la ricerca: senza accenti né maiuscole, separate da uno spazio solo. */
private fun searchWords(text: String): String =
    normalizeForSearch(text).split(WORD_BREAK).filter { it.isNotEmpty() }.joinToString(" ")

/** Quello che separa le parole di un nome: tutto ciò che non è una lettera o un numero. */
private val WORD_BREAK = Regex("[^\\p{L}\\p{N}]+")

/** Le scritture che non separano le parole con gli spazi. */
private fun isCjk(c: Char): Boolean = when (Character.UnicodeScript.of(c.code)) {
    Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> true
    else -> false
}

/** Minuscole e senza accenti: "Caffè" e "caffe" si trovano a vicenda. */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .trim()

/**
 * I colori della pelle, dal più chiaro al più scuro (i modificatori di
 * Fitzpatrick di Unicode). 0 è il giallo di base.
 */
private val SKIN_TONES = intArrayOf(0x1F3FB, 0x1F3FC, 0x1F3FD, 0x1F3FE, 0x1F3FF)
const val SKIN_TONE_COUNT = 5

/**
 * **L'emoji col colore della pelle** `tone` (1..5; 0 la lascia gialla): il
 * modificatore va subito dopo il primo carattere, al posto dell'eventuale
 * selettore di presentazione (U+FE0F) — "🕵️" diventa "🕵🏽", "🧑‍💻" "🧑🏽‍💻".
 * Il generatore degli asset segna come `acceptsTone` solo le emoji per cui
 * questa regola dà emoji vere.
 */
fun withSkinTone(emoji: String, tone: Int): String {
    if (tone !in 1..SKIN_TONE_COUNT || emoji.isEmpty()) return emoji
    val first = emoji.codePointAt(0)
    var rest = emoji.substring(Character.charCount(first))
    if (rest.firstOrNull() == '\uFE0F') rest = rest.substring(1)
    return String(Character.toChars(first)) + String(Character.toChars(SKIN_TONES[tone - 1])) + rest
}

/**
 * Gli asset delle emoji letti una volta sola: sono quasi duemila emoji coi
 * nomi in otto lingue, e il selettore si apre spesso.
 */
object EmojiCatalogs {
    private val mutex = Mutex()
    private var cached: EmojiCatalog? = null

    /** Le lingue dei nomi: quelle dell'app. */
    private val LANGUAGES = listOf("en", "it", "de", "fr", "es", "zh", "ko", "ja")

    /** Se il telefono sa disegnare un'emoji in un carattere solo. Si cambia nei test. */
    @Volatile
    var glyphCheck: (String) -> Boolean = { emoji -> paint.hasGlyph(emoji) }

    private val paint by lazy { Paint() }

    suspend fun load(context: Context): EmojiCatalog = mutex.withLock {
        cached?.let { return it }
        val catalog = withContext(Dispatchers.IO) {
            val assets = context.applicationContext.assets
            fun read(name: String): String? = runCatching {
                assets.open("emoji/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrNull()
            val structure = read("emoji.txt").orEmpty()
            val names = LANGUAGES.flatMap { language -> read("names_$language.txt")?.lines().orEmpty() }
            EmojiCatalog.parse(structure, names, glyphCheck)
        }
        cached = catalog
        catalog
    }
}

/**
 * **Le icone caricate finora**, le "Custom" del selettore: le immagini delle
 * icone di pagine e riquadri. Una stessa immagine copiata con una pagina
 * duplicata è un file a sé, ma qui si vede una volta sola: i doppioni si
 * riconoscono dal contenuto.
 */
object IconLibrary {
    /**
     * Le immagini delle icone di pagine (fuori dal cestino) e riquadri, le
     * più recenti prima, senza doppioni. Se non si riesce a leggerle la
     * sezione resta vuota: il resto del selettore funziona lo stesso.
     */
    suspend fun customIcons(context: Context, store: PageImageStore): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDatabase.getInstance(context)
            val callouts = db.blockDao().calloutAttrs().mapNotNull { BlockAttrs.parse(it).iconImage }
            distinctByContent((db.pageDao().liveIconImages() + callouts).distinct(), store)
        }.getOrDefault(emptyList())
    }

    /** I file che esistono, uno per contenuto: il primo di ogni gruppo di copie uguali. */
    suspend fun distinctByContent(fileNames: List<String>, store: PageImageStore): List<String> = withContext(Dispatchers.IO) {
        val seen = HashSet<String>()
        fileNames.filter { name ->
            val file = store.fileFor(name)
            if (!file.isFile) return@filter false
            val digest = runCatching {
                val md = MessageDigest.getInstance("MD5")
                file.inputStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        md.update(buffer, 0, read)
                    }
                }
                md.digest().joinToString("") { "%02x".format(it) }
            }.getOrNull() ?: return@filter false
            seen.add(digest)
        }
    }
}
