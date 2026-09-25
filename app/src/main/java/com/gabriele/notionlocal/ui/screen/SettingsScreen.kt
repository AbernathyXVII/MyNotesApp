package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.gabriele.notionlocal.MainActivity
import com.gabriele.notionlocal.data.AppCleanup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.settings.AnimationLevel
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.DateStyle
import com.gabriele.notionlocal.data.settings.NumberStyle
import com.gabriele.notionlocal.data.settings.ThemeMode
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

/** Quale finestra di scelta è aperta. */
private enum class SettingsDialog { THEME, SOUND, MUTE_FOR, ANIMATIONS, NUMBER, DATE, LANGUAGE, TIME_ZONE, CLEAR_CACHE, DELETE_ALL }

/**
 * Le impostazioni. Ogni scelta si salva appena la si fa e vale subito: il
 * tema cambia sotto le dita, la lingua ridisegna le voci, le date si
 * riscrivono nel formato nuovo ovunque compaiano.
 *
 * Carattere e dimensione del carattere non ci sono più (tolti il
 * 25/09/2026, su richiesta): si scelgono sul testo selezionato, nella
 * barra Aa della pagina.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Quanto occupa la cache, contato fuori dal disegno: sono file su disco.
    var cacheBytes by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { cacheBytes = withContext(Dispatchers.IO) { AppCleanup.cacheBytes(context) } }

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.settings, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SettingsItem(
                Strings.theme,
                icon = Icons.Filled.DarkMode,
                value = themeLabel(AppSettings.themeMode),
                onClick = { dialog = SettingsDialog.THEME }
            )

            SectionHeader(Strings.appSounds)
            SettingsItem(
                Strings.notificationSound,
                icon = Icons.Filled.MusicNote,
                value = AppSettings.notificationSoundName ?: Strings.defaultSound,
                onClick = { dialog = SettingsDialog.SOUND }
            )
            SwitchItem(
                Strings.turnOffNotifications,
                checked = AppSettings.notificationsMuted
            ) { AppSettings.notificationsMuted = it }
            val mutedUntil = AppSettings.mutedUntil
            SettingsItem(
                Strings.turnOffForAWhile,
                icon = Icons.Filled.Timer,
                value = mutedUntil?.let { Strings.offUntil(Formats.dateTime(it)) },
                enabled = !AppSettings.notificationsMuted,
                onClick = { dialog = SettingsDialog.MUTE_FOR },
                trailing = if (mutedUntil != null) {
                    { TextButton(onClick = { AppSettings.mutedUntil = null }) { Text(Strings.turnBackOn) } }
                } else {
                    null
                }
            )
            Text(
                Strings.notificationsAbout,
                color = NotionGray400,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 12.dp))

            SettingsItem(
                Strings.animations,
                icon = Icons.Filled.Animation,
                value = animationLabel(AppSettings.animations),
                onClick = { dialog = SettingsDialog.ANIMATIONS }
            )
            SettingsItem(
                Strings.numberFormat,
                icon = Icons.Filled.Numbers,
                value = Formats.numberSample(AppSettings.numberStyle),
                onClick = { dialog = SettingsDialog.NUMBER }
            )
            SettingsItem(
                Strings.dateFormat,
                icon = Icons.Filled.CalendarMonth,
                value = "${dateStyleLabel(AppSettings.dateStyle)} — ${Formats.sample(AppSettings.dateStyle, today())}",
                onClick = { dialog = SettingsDialog.DATE }
            )
            SettingsItem(
                Strings.language,
                icon = Icons.Filled.Language,
                value = languageName(AppSettings.language),
                onClick = { dialog = SettingsDialog.LANGUAGE }
            )
            SettingsItem(
                Strings.timeZone,
                icon = Icons.Filled.Public,
                value = zoneLabel(AppSettings.timeZoneId),
                onClick = { dialog = SettingsDialog.TIME_ZONE }
            )

            // **Sotto il fuso orario, come ha chiesto l'utente** (26/09/2026):
            // svuotare la cache e cancellare tutto il contenuto. Separate
            // dal resto da una riga: non sono scelte, sono azioni.
            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 12.dp))
            SettingsItem(
                Strings.clearCache,
                icon = Icons.Filled.CleaningServices,
                value = Formatter.formatShortFileSize(context, cacheBytes),
                onClick = { dialog = SettingsDialog.CLEAR_CACHE }
            )
            SettingsItem(
                Strings.deleteAllContent,
                icon = Icons.Filled.DeleteForever,
                value = Strings.deleteAllContentNote,
                onClick = { dialog = SettingsDialog.DELETE_ALL }
            )
        }
    }

    when (dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = Strings.theme,
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = AppSettings.themeMode,
            onSelect = { AppSettings.themeMode = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.SOUND -> SoundDialog(onDismiss = { dialog = null })
        SettingsDialog.MUTE_FOR -> MuteForDialog(onDismiss = { dialog = null })
        SettingsDialog.ANIMATIONS -> ChoiceDialog(
            title = Strings.animations,
            options = AnimationLevel.entries.map { it to animationLabel(it) },
            selected = AppSettings.animations,
            onSelect = { AppSettings.animations = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.NUMBER -> ChoiceDialog(
            title = Strings.numberFormat,
            options = NumberStyle.entries.map { it to Formats.numberSample(it) },
            selected = AppSettings.numberStyle,
            onSelect = { AppSettings.numberStyle = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.DATE -> ChoiceDialog(
            title = Strings.dateFormat,
            options = DateStyle.entries.map { it to dateStyleLabel(it) },
            details = DateStyle.entries.associateWith { Formats.sample(it, today()) },
            selected = AppSettings.dateStyle,
            onSelect = { AppSettings.dateStyle = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.LANGUAGE -> ChoiceDialog(
            title = Strings.language,
            subtitle = Strings.languageNote,
            options = AppLanguage.entries.map { it to languageName(it) },
            selected = AppSettings.language,
            onSelect = { AppSettings.language = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.TIME_ZONE -> TimeZoneDialog(onDismiss = { dialog = null })
        SettingsDialog.CLEAR_CACHE -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(Strings.clearCacheTitle) },
            text = { Text(Strings.clearCacheText) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    scope.launch {
                        AppCleanup.clearCache(context)
                        cacheBytes = AppCleanup.cacheBytes(context)
                        Toast.makeText(context, Strings.cacheCleared, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(Strings.clear) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(Strings.cancel) } }
        )
        SettingsDialog.DELETE_ALL -> DeleteAllDialog(
            onDismiss = { dialog = null },
            onConfirm = {
                scope.launch {
                    AppCleanup.deleteAllContent(context)
                    restartApp(context)
                }
            }
        )
        null -> Unit
    }
}

private fun today(): LocalDate = LocalDate.now(AppSettings.zoneId)

/**
 * **"Elimina tutto il contenuto"**: una volta fatto non si torna indietro,
 * quindi il pulsante rosso si accende solo dopo aver **scritto la parola**
 * ("ELIMINA" in italiano). Un tocco sbagliato non deve bastare.
 */
@Composable
private fun DeleteAllDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val word = Strings.deleteAllWord
    val armed = typed.trim().equals(word, ignoreCase = true) && !working
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(Strings.deleteAllTitle) },
        text = {
            Column {
                Text(Strings.deleteAllText)
                Spacer(modifier = Modifier.height(16.dp))
                Text(Strings.typeToConfirm(word), color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    enabled = !working,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    working = true
                    onConfirm()
                },
                enabled = armed
            ) {
                Text(
                    Strings.deleteEverything,
                    color = if (armed) MaterialTheme.colorScheme.error else NotionGray400
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text(Strings.cancel) } }
    )
}

/**
 * Fa ripartire l'app da capo, su una My Space vuota. Le schermate aperte
 * hanno ancora in mano il contenuto di prima — un Annulla rimetterebbe i
 * blocchi cancellati — e con l'attività chiusa se ne vanno anche loro.
 */
private fun restartApp(context: Context) {
    val activity = context.findActivity() ?: return
    val intent = Intent(activity, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(intent)
    activity.finish()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.DARK -> Strings.themeDark
    ThemeMode.LIGHT -> Strings.themeLight
    ThemeMode.SYSTEM -> Strings.themeSystem
}

private fun animationLabel(level: AnimationLevel) = when (level) {
    AnimationLevel.FULL -> Strings.animationsOn
    AnimationLevel.REDUCED -> Strings.animationsReduced
    AnimationLevel.OFF -> Strings.animationsOff
}

private fun dateStyleLabel(style: DateStyle) = when (style) {
    DateStyle.FULL -> Strings.dateFull
    DateStyle.SHORT -> Strings.dateShort
    DateStyle.MONTH_DAY_YEAR_DOT -> Strings.dateMonthDayYearDot
    DateStyle.MONTH_DAY_YEAR_SLASH -> Strings.dateMonthDayYearSlash
    DateStyle.DAY_MONTH_YEAR_DOT -> Strings.dateDayMonthYearDot
    DateStyle.DAY_MONTH_YEAR_SLASH -> Strings.dateDayMonthYearSlash
    DateStyle.YEAR_MONTH_DAY_DOT -> Strings.dateYearMonthDayDot
    DateStyle.YEAR_MONTH_DAY_SLASH -> Strings.dateYearMonthDaySlash
    DateStyle.CHINESE -> Strings.dateChinese
}

/**
 * Il nome di ogni lingua **nella sua lingua**: chi ha messo per sbaglio il
 * coreano deve poter ritrovare "Italiano" senza saper leggere il coreano.
 */
private fun languageName(language: AppLanguage) = when (language) {
    AppLanguage.ENGLISH -> "English"
    AppLanguage.ITALIAN -> "Italiano"
    AppLanguage.GERMAN -> "Deutsch"
    AppLanguage.FRENCH -> "Français"
    AppLanguage.SPANISH -> "Español"
    AppLanguage.CHINESE -> "中文"
    AppLanguage.KOREAN -> "한국어"
    AppLanguage.JAPANESE -> "日本語"
}

/** "Berlin (UTC+02:00)": la città, e di quanto è avanti adesso — ora legale compresa. */
private fun zoneLabel(id: String): String {
    val zone = runCatching { ZoneId.of(id) }.getOrNull() ?: return id
    val offset = zone.rules.getOffset(Instant.now())
    val city = id.substringAfterLast('/').replace('_', ' ')
    val region = id.substringBefore('/', missingDelimiterValue = "")
    val name = if (region.isNotEmpty()) "$city · $region" else zone.getDisplayName(TextStyle.FULL, AppSettings.language.locale)
    return "$name (UTC${if (offset.totalSeconds == 0) "" else offset.id})"
}

/** Una scelta fra poche voci: tocchi, si salva, si chiude. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    details: Map<T, String> = emptyMap()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(title, color = NotionWhite) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                options.forEach { (value, label) ->
                    RadioItem(
                        title = label,
                        detail = details[value],
                        selected = value == selected,
                        onClick = {
                            onSelect(value)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * Il suono delle notifiche: un file scelto dal telefono, o quello
 * predefinito. Il file si sceglie con l'esplora file del telefono, e il
 * permesso di leggerlo si tiene anche dopo il riavvio — senza, al primo
 * riavvio del telefono l'app non potrebbe più aprirlo.
 */
@Composable
private fun SoundDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        AppSettings.setNotificationSound(uri.toString(), name ?: uri.lastPathSegment)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(Strings.notificationSound, color = NotionWhite) },
        text = {
            Column {
                Text(
                    AppSettings.notificationSoundName ?: Strings.defaultSound,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DialogAction(Strings.chooseFromFiles) { picker.launch(arrayOf("audio/*")) }
                DialogAction(Strings.useDefaultSound) {
                    AppSettings.setNotificationSound(null, null)
                    onDismiss()
                }
                DialogAction(Strings.playSound) {
                    val uri = AppSettings.notificationSoundUri?.let(Uri::parse)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    // Una volta sola e per pochi secondi: un file scelto qui
                    // può essere una suoneria che si ripete all'infinito.
                    WidgetStore.playOnce(context, uri)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.done) } }
    )
}

@Composable
private fun DialogAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = NotionWhite,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

/** Notifiche spente per un po': un'ora, otto ore, un giorno, una settimana. */
@Composable
private fun MuteForDialog(onDismiss: () -> Unit) {
    val hour = 60L * 60 * 1000
    val options = listOf(
        Strings.forOneHour to hour,
        Strings.forEightHours to 8 * hour,
        Strings.forOneDay to 24 * hour,
        Strings.forOneWeek to 7 * 24 * hour
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(Strings.turnOffForAWhile, color = NotionWhite) },
        text = {
            Column {
                options.forEach { (label, duration) ->
                    DialogAction(label) {
                        AppSettings.mutedUntil = System.currentTimeMillis() + duration
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * Il fuso orario, cercandolo per città o regione. Si elencano solo i fusi
 * "Regione/Città" (Europe/Berlin), non le sigle come CET: le sigle non
 * seguono l'ora legale allo stesso modo, e la città è quello che uno cerca.
 */
@Composable
private fun TimeZoneDialog(onDismiss: () -> Unit) {
    val all = remember {
        ZoneId.getAvailableZoneIds()
            .filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .sorted()
    }
    var filter by remember { mutableStateOf("") }
    val shown = remember(filter) {
        val needle = filter.trim().replace(' ', '_')
        if (needle.isEmpty()) all else all.filter { it.contains(needle, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(Strings.timeZone, color = NotionWhite) },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    if (filter.isEmpty()) {
                        Text(Strings.searchTimeZones, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
                    }
                    BasicTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                        cursorBrush = SolidColor(NotionWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider(color = NotionGray900)
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it }) { id ->
                        RadioItem(
                            title = zoneLabel(id),
                            selected = id == AppSettings.timeZoneId,
                            onClick = {
                                AppSettings.timeZoneId = id
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
