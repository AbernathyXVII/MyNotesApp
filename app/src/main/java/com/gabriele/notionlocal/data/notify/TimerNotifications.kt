package com.gabriele.notionlocal.data.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gabriele.notionlocal.MainActivity
import com.gabriele.notionlocal.R
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CountdownTimer
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.WidgetsState
import com.gabriele.notionlocal.data.widgets.countdownText
import com.gabriele.notionlocal.data.widgets.durationLabel
import com.gabriele.notionlocal.data.widgets.notifiedTimers
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * **I timer che corrono, nella tendina** (dal 30/09/2026, chiesto
 * dall'utente): come il timer di Samsung, ogni timer partito ha una sua
 * notifica fissa col tempo che scorre, finché non finisce, non lo si
 * annulla o non la si toglie con uno scorrimento.
 *
 * - **In corsa**: il conto alla rovescia lo fa Android (il cronometro
 *   delle notifiche, all'indietro fino alla fine), quindi scorre anche ad
 *   app chiusa, senza che l'app debba aggiornarla ogni secondo. Sotto,
 *   l'ora in cui finisce; i pulsanti "Pause" e "Cancel".
 * - **In pausa**: il tempo che manca, fermo; "Resume" e "Cancel".
 * - **Finito**: se ne va, e arriva quella di "Time's up" (`Notifier`).
 *
 * Su Android 16 chiede di essere una **"live update"**: il telefono la
 * può mostrare anche come pastiglia col tempo nella barra di stato e
 * sulla schermata di blocco. Per questo niente disegni fatti a mano: solo
 * le parti standard di una notifica, le uniche che Android accetta lì.
 *
 * Toccandola si apre l'app con la barra laterale aperta, dove c'è il
 * widget del timer. Toglierla con uno scorrimento non ferma il timer: se
 * ne ricorda `CountdownTimer.notificationDismissed`, e torna solo quando
 * il timer cambia (pausa, ripresa, di nuovo "Start").
 *
 * Le mette e le toglie `WidgetStore`, a ogni modifica (`sync`); i pulsanti
 * arrivano a `TimerActionReceiver`.
 */
object TimerNotifications {

    /** Il canale, silenzioso: la notifica sta lì, non suona e non vibra. */
    const val CHANNEL = "timers.running"

    const val ACTION_PAUSE = "com.gabriele.notionlocal.timer.PAUSE"
    const val ACTION_RESUME = "com.gabriele.notionlocal.timer.RESUME"
    const val ACTION_CANCEL = "com.gabriele.notionlocal.timer.CANCEL"
    const val ACTION_DISMISSED = "com.gabriele.notionlocal.timer.DISMISSED"
    const val EXTRA_WIDGET_ID = "timer.widgetId"
    const val EXTRA_TIMER_ID = "timer.timerId"

    /**
     * La richiesta di Android 16 per le "live update"
     * (`Notification.EXTRA_REQUEST_PROMOTED_ONGOING`): scritta a mano,
     * perché l'app si compila con le librerie di Android 15.
     */
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    private val END_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun idOf(timerId: String): Int = "timerRunning:$timerId".hashCode()

    /**
     * Da `before` ad `after`: toglie le notifiche dei timer che non ne
     * hanno più una (finiti, annullati, eliminati, tolti a mano) e rimette
     * quelle dei timer cambiati. Quelle dei timer uguali restano come sono.
     */
    fun sync(context: Context, before: WidgetsState, after: WidgetsState) {
        val old = before.notifiedTimers()
        val new = after.notifiedTimers()
        (old.keys - new.keys).forEach { cancel(context, it) }
        new.forEach { (timerId, entry) -> if (old[timerId] != entry) post(context, entry.first, entry.second) }
    }

    /**
     * All'avvio: rimette quelle che mancano. Il telefono le toglie quando
     * si riaccende o quando l'app si aggiorna, ma il timer intanto corre.
     */
    fun restore(context: Context, state: WidgetsState) {
        val shown = runCatching {
            context.getSystemService(NotificationManager::class.java)?.activeNotifications?.map { it.id }?.toSet()
        }.getOrNull().orEmpty()
        state.notifiedTimers().forEach { (timerId, entry) ->
            if (idOf(timerId) !in shown) post(context, entry.first, entry.second)
        }
    }

    private fun cancel(context: Context, timerId: String) {
        runCatching { NotificationManagerCompat.from(context).cancel(idOf(timerId)) }
    }

    /** La notifica di un timer: in corsa col cronometro, in pausa col tempo fermo. */
    private fun post(context: Context, widgetId: String, timer: CountdownTimer) {
        val manager = NotificationManagerCompat.from(context)
        if (Notifier.muted || !manager.areNotificationsEnabled()) return
        Notifier.ensureChannels(context)
        val id = idOf(timer.id)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Notifier.EXTRA_OPEN_WIDGETS, true)
        }
        val tap = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val endsAt = timer.endsAt
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(NotifyStrings.timerOf(durationLabel(timer.durationMs)))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tap)
            .setDeleteIntent(broadcast(context, ACTION_DISMISSED, widgetId, timer.id))
            .addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })
        if (timer.running && endsAt != null) {
            builder
                .setWhen(endsAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setContentText("${WidgetStrings.timerEndsAt} ${END_TIME.format(Instant.ofEpochMilli(endsAt).atZone(AppSettings.zoneId))}")
                .addAction(0, WidgetStrings.pause, broadcast(context, ACTION_PAUSE, widgetId, timer.id))
        } else {
            builder
                .setShowWhen(false)
                .setContentText(NotifyStrings.pausedAt(countdownText(timer.remainingMs)))
                .addAction(0, WidgetStrings.resume, broadcast(context, ACTION_RESUME, widgetId, timer.id))
        }
        builder.addAction(0, Strings.cancel, broadcast(context, ACTION_CANCEL, widgetId, timer.id))
        runCatching { manager.notify(id, builder.build()) }
    }

    /**
     * Un pulsante della notifica, o lo scorrimento via: il timer cambia
     * come dal widget, e `WidgetStore` aggiorna la notifica. False se
     * l'azione non è una delle nostre.
     */
    fun handle(action: String?, widgetId: String, timerId: String): Boolean {
        when (action) {
            ACTION_PAUSE -> WidgetStore.timerPause(widgetId, timerId)
            ACTION_RESUME -> WidgetStore.timerResume(widgetId, timerId)
            ACTION_CANCEL -> WidgetStore.timerReset(widgetId, timerId)
            ACTION_DISMISSED -> WidgetStore.timerNotificationDismissed(widgetId, timerId)
            else -> return false
        }
        return true
    }

    private fun broadcast(context: Context, action: String, widgetId: String, timerId: String): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getBroadcast(
            context, "$action:$timerId".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * **I pulsanti della notifica di un timer** ("Pause", "Resume", "Cancel")
 * e il suo scorrimento via. Può arrivare ad app chiusa: impostazioni e
 * widget si caricano qui. Il timer cambia (`TimerNotifications.handle`),
 * poi la sveglia di Android si rimette alla fine nuova, perché ad app
 * chiusa non c'è nessun altro a farlo.
 */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val widgetId = intent.getStringExtra(TimerNotifications.EXTRA_WIDGET_ID) ?: return
        val timerId = intent.getStringExtra(TimerNotifications.EXTRA_TIMER_ID) ?: return
        AppSettings.init(app)
        WidgetStore.init(app)
        if (!TimerNotifications.handle(intent.action, widgetId, timerId)) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NoteAlarms.rescheduleNow(app)
            } finally {
                result.finish()
            }
        }
    }
}
