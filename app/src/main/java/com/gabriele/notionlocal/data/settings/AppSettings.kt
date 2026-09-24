package com.gabriele.notionlocal.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.ZoneId
import java.util.Locale

/** Il tema: scuro, chiaro, o come il telefono. */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Quanto si muove l'app: tutto, poco (animazioni più brevi) o niente. */
enum class AnimationLevel { FULL, REDUCED, OFF }

/** Come si raggruppano le cifre: 1.000.000 (virgola per i decimali) o 1,000,000 (punto per i decimali). */
enum class NumberStyle { DOT_GROUPS, COMMA_GROUPS }

/** Come si scrive una data. L'ora, quando c'è, segue sempre in 24 ore. */
enum class DateStyle {
    FULL,
    SHORT,
    MONTH_DAY_YEAR_DOT,
    MONTH_DAY_YEAR_SLASH,
    DAY_MONTH_YEAR_DOT,
    DAY_MONTH_YEAR_SLASH,
    YEAR_MONTH_DAY_DOT,
    YEAR_MONTH_DAY_SLASH,
    CHINESE
}

/**
 * La lingua delle voci dell'app. Traduce solo quello che l'app scrive da
 * sé — menu, impostazioni, nomi dei tipi di proprietà — mai quello che
 * scrive l'utente.
 */
enum class AppLanguage(val locale: Locale) {
    ENGLISH(Locale.ENGLISH),
    ITALIAN(Locale.ITALIAN),
    GERMAN(Locale.GERMAN),
    FRENCH(Locale.FRENCH),
    SPANISH(Locale("es")),
    CHINESE(Locale.SIMPLIFIED_CHINESE),
    KOREAN(Locale.KOREAN),
    JAPANESE(Locale.JAPANESE)
}

/** Da dove parte l'app quando la si apre. */
enum class StartupMode { LAST_VISITED, HOME, SPECIFIC_PAGE }

/** Cosa succede alle pagine nel cestino. */
enum class TrashPolicy { KEEP_30_DAYS, EMPTY_AUTOMATICALLY }

/** Come si ordinano i preferiti. */
enum class FavoritesSort { DATE_ADDED, ALPHABETICAL }

/**
 * Le impostazioni dell'app, salvate sul telefono.
 *
 * **Un oggetto solo, e osservabile.** Ogni valore sta in uno stato di
 * Compose: la schermata che lo legge si ridisegna da sola quando cambia,
 * senza che nessuno debba passarlo giù di mano in mano. Serve soprattutto
 * a lingua, tema e formati, che li leggono decine di punti diversi — anche
 * fuori dalle schermate, come la funzione che scrive una data.
 *
 * Il salvataggio è `SharedPreferences` e non il database: sono scelte del
 * telefono, non contenuto. Nel database finirebbero dentro ogni backup e
 * ogni copia, e una pagina esportata si porterebbe dietro il fuso orario
 * di chi l'ha scritta.
 *
 * `init` va chiamato una volta all'avvio, prima di qualunque lettura.
 */
object AppSettings {

    private lateinit var prefs: SharedPreferences

    /** Berlino, che segue da sé l'ora legale: il fuso di partenza chiesto dall'utente. */
    const val DEFAULT_TIME_ZONE = "Europe/Berlin"

    /** Quanti giorni resta una pagina nel cestino, con `TrashPolicy.KEEP_30_DAYS`. */
    const val TRASH_RETENTION_DAYS = 30

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        themeModeState.value = enumOr(KEY_THEME, ThemeMode.DARK)
        animationsState.value = enumOr(KEY_ANIMATIONS, AnimationLevel.FULL)
        numberStyleState.value = enumOr(KEY_NUMBER_STYLE, NumberStyle.DOT_GROUPS)
        dateStyleState.value = enumOr(KEY_DATE_STYLE, DateStyle.DAY_MONTH_YEAR_DOT)
        languageState.value = enumOr(KEY_LANGUAGE, AppLanguage.ENGLISH)
        timeZoneIdState.value = prefs.getString(KEY_TIME_ZONE, null)
            ?.takeIf { runCatching { ZoneId.of(it) }.isSuccess }
            ?: DEFAULT_TIME_ZONE
        startupModeState.value = enumOr(KEY_STARTUP_MODE, StartupMode.HOME)
        startupPageIdState.value = prefs.getString(KEY_STARTUP_PAGE, null)
        trashPolicyState.value = enumOr(KEY_TRASH_POLICY, TrashPolicy.KEEP_30_DAYS)
        favoritesSortState.value = enumOr(KEY_FAVORITES_SORT, FavoritesSort.DATE_ADDED)
        favoritesDescendingState.value = prefs.getBoolean(KEY_FAVORITES_DESC, false)
        searchContentState.value = prefs.getBoolean(KEY_SEARCH_CONTENT, true)
        searchTrashState.value = prefs.getBoolean(KEY_SEARCH_TRASH, false)
        notificationSoundUriState.value = prefs.getString(KEY_SOUND_URI, null)
        notificationSoundNameState.value = prefs.getString(KEY_SOUND_NAME, null)
        notificationsMutedState.value = prefs.getBoolean(KEY_MUTED, false)
        mutedUntilState.value = prefs.getLong(KEY_MUTED_UNTIL, 0L).takeIf { it > 0L }
    }

    // --- Aspetto ---

    private val themeModeState = mutableStateOf(ThemeMode.DARK)
    var themeMode: ThemeMode
        get() = themeModeState.value
        set(value) { themeModeState.value = value; putEnum(KEY_THEME, value) }

    /**
     * Se il telefono è in modalità scura. Lo aggiorna `MainActivity`, che
     * è l'unica a saperlo; serve al tema "System".
     */
    var systemInDarkMode by mutableStateOf(true)

    /** Se in questo momento l'app è scura, tenendo conto di "System". */
    val isDark: Boolean
        get() = when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemInDarkMode
        }

    private val animationsState = mutableStateOf(AnimationLevel.FULL)
    var animations: AnimationLevel
        get() = animationsState.value
        set(value) { animationsState.value = value; putEnum(KEY_ANIMATIONS, value) }

    // --- Formati ---

    private val numberStyleState = mutableStateOf(NumberStyle.DOT_GROUPS)
    var numberStyle: NumberStyle
        get() = numberStyleState.value
        set(value) { numberStyleState.value = value; putEnum(KEY_NUMBER_STYLE, value) }

    private val dateStyleState = mutableStateOf(DateStyle.DAY_MONTH_YEAR_DOT)
    var dateStyle: DateStyle
        get() = dateStyleState.value
        set(value) { dateStyleState.value = value; putEnum(KEY_DATE_STYLE, value) }

    private val languageState = mutableStateOf(AppLanguage.ENGLISH)
    var language: AppLanguage
        get() = languageState.value
        set(value) { languageState.value = value; putEnum(KEY_LANGUAGE, value) }

    private val timeZoneIdState = mutableStateOf(DEFAULT_TIME_ZONE)
    var timeZoneId: String
        get() = timeZoneIdState.value
        set(value) {
            if (runCatching { ZoneId.of(value) }.isFailure) return
            timeZoneIdState.value = value
            prefs.edit().putString(KEY_TIME_ZONE, value).apply()
        }

    /** Il fuso in cui vive l'app: tutte le date si leggono e si scrivono qui. */
    val zoneId: ZoneId
        get() = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.of(DEFAULT_TIME_ZONE))

    // --- Avvio ---

    private val startupModeState = mutableStateOf(StartupMode.HOME)
    var startupMode: StartupMode
        get() = startupModeState.value
        set(value) { startupModeState.value = value; putEnum(KEY_STARTUP_MODE, value) }

    private val startupPageIdState = mutableStateOf<String?>(null)
    var startupPageId: String?
        get() = startupPageIdState.value
        set(value) {
            startupPageIdState.value = value
            prefs.edit().putString(KEY_STARTUP_PAGE, value).apply()
        }

    /**
     * L'ultima pagina aperta, come rotta di navigazione. Non è uno stato
     * osservato: cambia a ogni pagina aperta e nessuna schermata lo
     * mostra, serve solo al prossimo avvio.
     */
    var lastVisitedRoute: String?
        get() = prefs.getString(KEY_LAST_ROUTE, null)
        set(value) { prefs.edit().putString(KEY_LAST_ROUTE, value).apply() }

    // --- Cestino e preferiti ---

    private val trashPolicyState = mutableStateOf(TrashPolicy.KEEP_30_DAYS)
    var trashPolicy: TrashPolicy
        get() = trashPolicyState.value
        set(value) { trashPolicyState.value = value; putEnum(KEY_TRASH_POLICY, value) }

    private val favoritesSortState = mutableStateOf(FavoritesSort.DATE_ADDED)
    var favoritesSort: FavoritesSort
        get() = favoritesSortState.value
        set(value) { favoritesSortState.value = value; putEnum(KEY_FAVORITES_SORT, value) }

    private val favoritesDescendingState = mutableStateOf(false)
    var favoritesDescending: Boolean
        get() = favoritesDescendingState.value
        set(value) {
            favoritesDescendingState.value = value
            prefs.edit().putBoolean(KEY_FAVORITES_DESC, value).apply()
        }

    // --- Ricerca ---

    private val searchContentState = mutableStateOf(true)
    var searchContent: Boolean
        get() = searchContentState.value
        set(value) {
            searchContentState.value = value
            prefs.edit().putBoolean(KEY_SEARCH_CONTENT, value).apply()
        }

    private val searchTrashState = mutableStateOf(false)
    var searchTrash: Boolean
        get() = searchTrashState.value
        set(value) {
            searchTrashState.value = value
            prefs.edit().putBoolean(KEY_SEARCH_TRASH, value).apply()
        }

    // --- Suoni e notifiche ---
    //
    // Le notifiche vere non esistono ancora: queste scelte si salvano già,
    // così quando arriveranno le troveranno fatte.

    private val notificationSoundUriState = mutableStateOf<String?>(null)
    val notificationSoundUri: String? get() = notificationSoundUriState.value

    private val notificationSoundNameState = mutableStateOf<String?>(null)
    val notificationSoundName: String? get() = notificationSoundNameState.value

    fun setNotificationSound(uri: String?, name: String?) {
        notificationSoundUriState.value = uri
        notificationSoundNameState.value = name
        prefs.edit().putString(KEY_SOUND_URI, uri).putString(KEY_SOUND_NAME, name).apply()
    }

    private val notificationsMutedState = mutableStateOf(false)
    var notificationsMuted: Boolean
        get() = notificationsMutedState.value
        set(value) {
            notificationsMutedState.value = value
            prefs.edit().putBoolean(KEY_MUTED, value).apply()
        }

    private val mutedUntilState = mutableStateOf<Long?>(null)

    /** Fino a quando le notifiche tacciono, o null se non c'è una pausa in corso. */
    var mutedUntil: Long?
        get() = mutedUntilState.value?.takeIf { it > System.currentTimeMillis() }
        set(value) {
            mutedUntilState.value = value
            prefs.edit().putLong(KEY_MUTED_UNTIL, value ?: 0L).apply()
        }

    // --- Utilità ---

    private inline fun <reified T : Enum<T>> enumOr(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
            ?: default

    private fun putEnum(key: String, value: Enum<*>) {
        prefs.edit().putString(key, value.name).apply()
    }

    private const val KEY_THEME = "theme"
    private const val KEY_ANIMATIONS = "animations"
    private const val KEY_NUMBER_STYLE = "number_style"
    private const val KEY_DATE_STYLE = "date_style"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_TIME_ZONE = "time_zone"
    private const val KEY_STARTUP_MODE = "startup_mode"
    private const val KEY_STARTUP_PAGE = "startup_page"
    private const val KEY_LAST_ROUTE = "last_route"
    private const val KEY_TRASH_POLICY = "trash_policy"
    private const val KEY_FAVORITES_SORT = "favorites_sort"
    private const val KEY_FAVORITES_DESC = "favorites_desc"
    private const val KEY_SEARCH_CONTENT = "search_content"
    private const val KEY_SEARCH_TRASH = "search_trash"
    private const val KEY_SOUND_URI = "notification_sound_uri"
    private const val KEY_SOUND_NAME = "notification_sound_name"
    private const val KEY_MUTED = "notifications_muted"
    private const val KEY_MUTED_UNTIL = "notifications_muted_until"
}
