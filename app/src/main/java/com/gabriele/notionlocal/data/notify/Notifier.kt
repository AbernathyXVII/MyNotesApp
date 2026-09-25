package com.gabriele.notionlocal.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gabriele.notionlocal.MainActivity
import com.gabriele.notionlocal.R
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.PomodoroPhase
import com.gabriele.notionlocal.data.widgets.WidgetEvent
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.durationLabel
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings

/**
 * **Chi mostra le notifiche** (dal 25/09/2026): i promemoria (righe
 * "/notification" e date dei database) e la fine dei timer e delle fasi
 * del pomodoro.
 *
 * I canali **non hanno suono**: il suono lo fa l'app, una volta sola, con
 * `WidgetStore.playOnce` — quello scelto nelle impostazioni ("Notification
 * sound"), o quello del pomodoro che è finito. Così cambiare il suono
 * nelle impostazioni vale subito, e le suonerie Samsung che si ripetono
 * all'infinito (vedi `playOnce`) suonano una volta. Vibrano, quello sì.
 *
 * Le impostazioni **"Turn off notifications"** e **"Turn off for a while"**
 * valgono qui: a notifiche spente non si mostra niente e non suona niente.
 */
object Notifier {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_TIMERS = "timers"

    /** Cosa aprire toccando la notifica: vedi `NotificationTarget`. */
    const val EXTRA_PAGE_ID = "notify.pageId"
    const val EXTRA_ROW_ID = "notify.rowId"
    const val EXTRA_DATABASE_ID = "notify.databaseId"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        fun channel(id: String, name: String) = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel(CHANNEL_REMINDERS, NotifyStrings.channelReminders))
        manager.createNotificationChannel(channel(CHANNEL_TIMERS, NotifyStrings.channelTimers))
    }

    /** Le notifiche messe a tacere dalle impostazioni dell'app. */
    val muted: Boolean get() = AppSettings.notificationsMuted || AppSettings.mutedUntil != null

    /** I promemoria arrivati: una notifica ciascuno, e il suono una volta sola. */
    fun postReminders(context: Context, fires: List<ReminderFire>) {
        if (fires.isEmpty() || muted) return
        ensureChannels(context)
        var posted = false
        fires.forEach { fire ->
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                fire.pageId?.let { putExtra(EXTRA_PAGE_ID, it) }
                fire.rowId?.let { putExtra(EXTRA_ROW_ID, it) }
                fire.databaseId?.let { putExtra(EXTRA_DATABASE_ID, it) }
            }
            posted = post(context, CHANNEL_REMINDERS, fire.key.hashCode(), fire.title, fire.body, intent) || posted
        }
        if (posted) ring(context, null)
    }

    /**
     * La fine di timer e fasi del pomodoro. Suona **anche senza il permesso
     * delle notifiche**, com'era prima che le notifiche ci fossero: chi ha
     * fatto partire un timer vuole sentirlo finire.
     */
    fun postWidgetEvents(context: Context, events: List<WidgetEvent>, pomodoroSound: String?) {
        if (events.isEmpty() || muted) return
        ensureChannels(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        events.forEach { event ->
            when (event) {
                is WidgetEvent.TimerDone -> post(
                    context, CHANNEL_TIMERS, "timer:${event.timerId}".hashCode(),
                    NotifyStrings.timeIsUp, NotifyStrings.timerOf(durationLabel(event.durationMs)), open
                )
                is WidgetEvent.PomodoroPhaseEnded -> {
                    val session = event.sessionName.ifBlank { WidgetStrings.session }
                    val (title, body) = when (event.nowIn) {
                        PomodoroPhase.BREAK -> session to NotifyStrings.breakTime(event.nextMinutes)
                        PomodoroPhase.SESSION -> NotifyStrings.breakOver to NotifyStrings.backTo(session, event.nextMinutes)
                    }
                    post(context, CHANNEL_TIMERS, "pomodoro:${event.widgetId}".hashCode(), title, body, open)
                }
            }
        }
        ring(context, pomodoroSound)
    }

    /** Mostra una notifica; false se non si può (permesso negato o notifiche dell'app spente nel telefono). */
    private fun post(context: Context, channel: String, id: Int, title: String, body: String, intent: Intent): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val tap = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .apply { if (body.isNotBlank()) setContentText(body) }
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (channel == CHANNEL_TIMERS) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        return runCatching { manager.notify(id, notification) }.isSuccess
    }

    /** Il suono: quello del pomodoro, se c'è, se no quello delle impostazioni, o quello del telefono. */
    private fun ring(context: Context, pomodoroSound: String?) {
        val uri = (pomodoroSound ?: AppSettings.notificationSoundUri)?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        WidgetStore.playOnce(context, uri)
    }
}
