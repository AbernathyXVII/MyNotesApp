package com.gabriele.notionlocal.data.proofing

import android.content.Context
import com.gabriele.notionlocal.data.settings.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Le lingue del correttore: quelle dell'app (Settings → Language), con
 * l'inglese in due, americano e britannico. Il nome di ognuna è scritto
 * nella sua lingua, come nella scelta della lingua dell'app.
 *
 * Cinese, coreano e giapponese ci sono ma spenti ([available] falso):
 * il cinese e il giapponese non separano le parole con gli spazi, e un
 * correttore per loro è un'altra cosa; il dizionario coreano di Hunspell
 * pesa da solo più di tutti gli altri insieme.
 *
 * I dizionari stanno negli asset, in `proofing/<cartella>/`, con la loro
 * licenza: vedi `tools/proofing/get_dictionaries.py`.
 */
enum class ProofingLanguage(
    /** Come si salva: nelle impostazioni e nella pagina. */
    val code: String,
    val label: String,
    internal val assetDir: String?,
    internal val locale: Locale
) {
    ENGLISH_US("en-US", "English (US)", "en_US", Locale.US),
    ENGLISH_UK("en-GB", "English (UK)", "en_GB", Locale.UK),
    ITALIAN("it", "Italiano", "it", Locale.ITALIAN),
    GERMAN("de", "Deutsch", "de", Locale.GERMAN),
    FRENCH("fr", "Français", "fr", Locale.FRENCH),
    SPANISH("es", "Español", "es", Locale("es")),
    CHINESE("zh", "中文", null, Locale.SIMPLIFIED_CHINESE),
    KOREAN("ko", "한국어", null, Locale.KOREAN),
    JAPANESE("ja", "日本語", null, Locale.JAPANESE);

    val available: Boolean get() = assetDir != null

    companion object {
        fun fromCode(code: String?): ProofingLanguage? = entries.firstOrNull { it.code == code }

        /** La lingua di partenza: quella dell'app, se ha un dizionario; se no l'inglese americano. */
        fun forApp(language: AppLanguage): ProofingLanguage = when (language) {
            AppLanguage.ITALIAN -> ITALIAN
            AppLanguage.GERMAN -> GERMAN
            AppLanguage.FRENCH -> FRENCH
            AppLanguage.SPANISH -> SPANISH
            else -> ENGLISH_US
        }
    }
}

/**
 * I dizionari letti. Leggerne uno prende qualche decimo di secondo e una
 * decina di MB: lo si fa la prima volta che serve, fuori dal filo
 * principale, e poi lo si tiene. Al più due lingue alla volta — quella di
 * sempre e quella di una pagina scritta in un'altra lingua.
 */
object SpellDictionaries {

    private const val KEEP = 2

    private val mutex = Mutex()
    private val loaded = LinkedHashMap<ProofingLanguage, SpellDictionary>()

    suspend fun get(context: Context, language: ProofingLanguage): SpellDictionary? {
        val dir = language.assetDir ?: return null
        return mutex.withLock {
            loaded[language]?.let { return@withLock it }
            val assets = context.applicationContext.assets
            val dictionary = withContext(Dispatchers.Default) {
                assets.open("proofing/$dir/index.aff").bufferedReader(Charsets.UTF_8).use { aff ->
                    assets.open("proofing/$dir/index.dic").bufferedReader(Charsets.UTF_8).use { dic ->
                        SpellDictionary.read(aff, dic, language.locale)
                    }
                }
            }
            loaded[language] = dictionary
            while (loaded.size > KEEP) loaded.remove(loaded.keys.first())
            dictionary
        }
    }
}
