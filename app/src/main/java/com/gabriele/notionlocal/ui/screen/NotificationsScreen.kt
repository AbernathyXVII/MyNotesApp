package com.gabriele.notionlocal.ui.screen

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gabriele.notionlocal.data.notify.NotificationPermission
import com.gabriele.notionlocal.data.notify.Notifier
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.NotificationPriority
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite

/** Quale finestra è aperta sopra la pagina delle notifiche. */
private enum class NotificationsDialog { SOUND, PRIORITY, MUTE_FOR }

/**
 * **La pagina "Notifications"** della barra laterale (dal 30/09/2026,
 * chiesto dall'utente): le scelte che hanno le notifiche di ogni app, su
 * Android come su iPhone.
 *
 *  - **il suono**: uno dei suoni del telefono — quelli di serie e quelli
 *    scaricati, anche da altre app (Zedge…) — o un file audio qualsiasi;
 *  - **la vibrazione**, accesa o spenta;
 *  - **la priorità**: bassa (in silenzio, solo nella tendina), normale
 *    (suono e vibrazione) o alta (anche il riquadro in cima allo schermo);
 *  - **il pallino sull'icona dell'app** mentre c'è una notifica da leggere;
 *  - le notifiche spente, per sempre o per un po' (le stesse di Settings).
 *
 * Valgono per i promemoria delle pagine e dei database ("/notification" e
 * "Include notification"). Timer e pomodoro sono sveglie: suonano e
 * compaiono sempre. "Send a test notification" ne manda una subito, per
 * sentire e vedere com'è.
 */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<NotificationsDialog?>(null) }

    // Se Android le ha spente per tutta l'app, le scelte qui non servono a
    // niente: lo si dice in cima, con la strada per riaccenderle. Si
    // riguarda tornando dalle impostazioni del telefono.
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Ogni scelta cambia il canale delle notifiche (vedi `Notifier.ensureChannels`).
    fun changed() = Notifier.ensureChannels(context)

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.notifications, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            if (!allowed) {
                SettingsItem(
                    NotifyStrings.blockedByPhone,
                    icon = Icons.Filled.NotificationsOff,
                    value = NotifyStrings.openPhoneSettings,
                    onClick = {
                        NotificationPermission.request()
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                )
                HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 12.dp))
            }

            SettingsItem(
                Strings.notificationSound,
                icon = Icons.Filled.MusicNote,
                value = AppSettings.notificationSoundName ?: Strings.defaultSound,
                onClick = { dialog = NotificationsDialog.SOUND }
            )
            SwitchItem(
                NotifyStrings.vibration,
                checked = AppSettings.notificationVibrate
            ) {
                AppSettings.notificationVibrate = it
                changed()
            }
            SettingsItem(
                NotifyStrings.priority,
                icon = Icons.Filled.PriorityHigh,
                value = "${priorityLabel(AppSettings.notificationPriority)} · ${priorityDetail(AppSettings.notificationPriority)}",
                onClick = { dialog = NotificationsDialog.PRIORITY }
            )
            SwitchItem(
                NotifyStrings.appIconBadge,
                checked = AppSettings.notificationBadge,
                detail = NotifyStrings.appIconBadgeDetail
            ) {
                AppSettings.notificationBadge = it
                changed()
            }
            SettingsItem(
                NotifyStrings.sendTest,
                icon = Icons.Filled.NotificationsActive,
                onClick = {
                    NotificationPermission.request()
                    if (!Notifier.postTest(context)) {
                        Toast.makeText(context, NotifyStrings.blockedByPhone, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 12.dp))
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
                onClick = { dialog = NotificationsDialog.MUTE_FOR },
                trailing = if (mutedUntil != null) {
                    { TextButton(onClick = { AppSettings.mutedUntil = null }) { Text(Strings.turnBackOn) } }
                } else {
                    null
                }
            )
            Text(
                "${Strings.notificationsAbout} ${NotifyStrings.timersAlwaysRing}",
                color = NotionGray400,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }

    when (dialog) {
        NotificationsDialog.SOUND -> NotificationSoundDialog(onDismiss = { dialog = null })
        NotificationsDialog.PRIORITY -> ChoiceDialog(
            title = NotifyStrings.priority,
            options = NotificationPriority.entries.map { it to priorityLabel(it) },
            selected = AppSettings.notificationPriority,
            details = NotificationPriority.entries.associateWith { priorityDetail(it) },
            onSelect = {
                AppSettings.notificationPriority = it
                changed()
            },
            onDismiss = { dialog = null }
        )
        NotificationsDialog.MUTE_FOR -> MuteForDialog(onDismiss = { dialog = null })
        null -> Unit
    }
}

private fun priorityLabel(priority: NotificationPriority) = when (priority) {
    NotificationPriority.LOW -> NotifyStrings.priorityLow
    NotificationPriority.NORMAL -> NotifyStrings.priorityNormal
    NotificationPriority.HIGH -> NotifyStrings.priorityHigh
}

private fun priorityDetail(priority: NotificationPriority) = when (priority) {
    NotificationPriority.LOW -> NotifyStrings.priorityLowDetail
    NotificationPriority.NORMAL -> NotifyStrings.priorityNormalDetail
    NotificationPriority.HIGH -> NotifyStrings.priorityHighDetail
}

/**
 * **Il suono delle notifiche**: uno dei **suoni del telefono** — l'elenco
 * di Android, con quelli di serie e quelli scaricati (le suonerie aggiunte
 * da altre app ci finiscono dentro) —, un **file** qualsiasi preso
 * dall'esplora file, o quello predefinito. Di un file si tiene il permesso
 * di leggerlo anche dopo un riavvio: senza, al primo riavvio del telefono
 * l'app non potrebbe più aprirlo.
 *
 * È la stessa finestra in Settings e nella pagina Notifications.
 */
@Composable
internal fun NotificationSoundDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val phoneSounds = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val picked = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } ?: return@rememberLauncherForActivityResult
        val name = runCatching { RingtoneManager.getRingtone(context, picked)?.getTitle(context) }.getOrNull()
        AppSettings.setNotificationSound(picked.toString(), name ?: picked.lastPathSegment)
        onDismiss()
    }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
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
                DialogAction(NotifyStrings.phoneSounds) {
                    phoneSounds.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, Strings.notificationSound)
                            // Il predefinito dell'app è la voce qui sotto; il
                            // silenzio si fa con la priorità bassa.
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                AppSettings.notificationSoundUri?.let(Uri::parse)
                            )
                    )
                }
                DialogAction(Strings.chooseFromFiles) { file.launch(arrayOf("audio/*")) }
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
