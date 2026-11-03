package com.gabriele.notionlocal.data.proofing

import com.gabriele.notionlocal.data.settings.AppLanguage
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
    val assetDir: String?,
    val locale: Locale
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
