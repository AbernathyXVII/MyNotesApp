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
import com.gabriele.notionlocal.data.settings.NotificationPriority
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
 *
 * **Vibrazione, priorità e pallino sull'icona** (dal 30/09/2026, la pagina
 * Notifications) valgono per i promemoria. Android non lascia cambiare
 * queste cose a un canale che c'è già: per ogni combinazione c'è un canale
 * suo (`remindersChannel`), e scegliendone un'altra quello vecchio si
 * toglie. Con la priorità bassa l'app non suona. Timer e pomodoro restano
 * sul loro canale, alto e con la vibrazione: sono sveglie.
 */
object Notifier {

    /** L'inizio del nome dei canali dei promemoria; il resto dice le scelte (vedi `remindersChannel`). */
    private const val REMINDERS = "reminders"
    const val CHANNEL_TIMERS = "timers"

    /** Il canale dei promemoria per queste scelte: "reminders.high.vibrate.badge". */
    fun remindersChannel(priority: NotificationPriority, vibrate: Boolean, badge: Boolean): String =
        "$REMINDERS.${priority.name.lowercase()}.${if (vibrate) "vibrate" else "still"}.${if (badge) "badge" else "nobadge"}"

    /** Il canale dei promemoria con le scelte di adesso. */
    val currentRemindersChannel: String
        get() = remindersChannel(AppSettings.notificationPriority, AppSettings.notificationVibrate, AppSettings.notificationBadge)

    fun importanceOf(priority: NotificationPriority): Int = when (priority) {
        NotificationPriority.LOW -> NotificationManager.IMPORTANCE_LOW
        NotificationPriority.NORMAL -> NotificationManager.IMPORTANCE_DEFAULT
        NotificationPriority.HIGH -> NotificationManager.IMPORTANCE_HIGH
    }

    private fun compatPriorityOf(priority: NotificationPriority): Int = when (priority) {
        NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
        NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
    }

    /** Cosa aprire toccando la notifica: vedi `NotificationTarget`. */
    const val EXTRA_PAGE_ID = "notify.pageId"
    const val EXTRA_ROW_ID = "notify.rowId"
    const val EXTRA_DATABASE_ID = "notify.databaseId"

    /**
     * I canali come li vogliono le scelte di adesso: quello dei promemoria
     * (vedi `remindersChannel`) e quello dei timer. I canali dei promemoria
     * di scelte vecchie — e quello di prima delle scelte, "reminders" — si
     * tolgono, così nelle impostazioni del telefono ce n'è uno solo.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val priority = AppSettings.notificationPriority
        val reminders = currentRemindersChannel
        manager.createNotificationChannel(
            NotificationChannel(reminders, NotifyStrings.channelReminders, importanceOf(priority)).apply {
                setSound(null, null)
                enableVibration(AppSettings.notificationVibrate)
                setShowBadge(AppSettings.notificationBadge)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMERS, NotifyStrings.channelTimers, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(true)
            }
        )
        manager.notificationChannels
            .filter { it.id.startsWith(REMINDERS) && it.id != reminders }
            .forEach { manager.deleteNotificationChannel(it.id) }
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
            posted = post(context, currentRemindersChannel, fire.key.hashCode(), fire.title, fire.body, intent) || posted
        }
        if (posted && AppSettings.notificationPriority != NotificationPriority.LOW) ring(context, null)
    }

    /**
     * **"Send a test notification"** (la pagina Notifications): una notifica
     * subito, con il suono, la vibrazione, la priorità e il pallino scelti.
     * Arriva anche con le notifiche messe a tacere dall'app, perché è chiesta
     * apposta; false se Android non la lascia mostrare.
     */
    fun postTest(context: Context): Boolean {
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val posted = post(context, currentRemindersChannel, TEST_ID, NotifyStrings.testTitle, NotifyStrings.testText, intent)
        if (posted && AppSettings.notificationPriority != NotificationPriority.LOW) ring(context, null)
        return posted
    }

    private const val TEST_ID = 0x7E57

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
            .setPriority(if (channel == CHANNEL_TIMERS) NotificationCompat.PRIORITY_HIGH else compatPriorityOf(AppSettings.notificationPriority))
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
