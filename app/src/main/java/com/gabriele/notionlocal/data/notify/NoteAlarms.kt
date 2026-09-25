package com.gabriele.notionlocal.data.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * **La sveglia delle notifiche** (dal 25/09/2026).
 *
 * Una sveglia sola di Android (`AlarmManager`), messa alla prima cosa che
 * deve suonare: un promemoria (vedi `Reminders.kt`) o la fine di un timer o
 * di una fase del pomodoro. Suona anche ad app chiusa e col telefono in
 * standby; quando suona, `AlarmReceiver` manda tutto quello che è arrivato
 * e rimette la sveglia alla cosa dopo. Dopo un riavvio la rimette
 * `RescheduleReceiver`.
 *
 * **Si ricorda fin dove ha già mandato** (`deliveredUpTo`): consegnare è
 * "tutto quello che cade fra l'ultima volta e adesso". Così una notifica
 * non arriva due volte, e una persa a telefono spento arriva appena si
 * riaccende — se non è passata da più di `MISSED_WINDOW_MS`: a un giorno
 * di distanza non servirebbe più a niente.
 *
 * Una sveglia messa per una cosa poi cancellata non fa danni: quando
 * suona, si rilegge il database, e quello che non c'è più non arriva.
 * Per questo si rimette la sveglia solo quando le notifiche **nascono o
 * cambiano ora** (e all'avvio, e uscendo dall'app), non a ogni modifica.
 */
object NoteAlarms {

    private const val PREFS = "notifications"
    private const val KEY_DELIVERED_UP_TO = "deliveredUpTo"
    private const val MISSED_WINDOW_MS = 12 * 60 * 60 * 1000L
    /** Fin dove guardare avanti: le ripetizioni vanno contate, e senza un limite sarebbero infinite. */
    private const val HORIZON_MS = 3L * 366 * 24 * 60 * 60 * 1000L
    private const val DEBOUNCE_MS = 400L

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pending: Job? = null

    /** All'avvio dell'app: i canali, e la sveglia rimessa. */
    fun init(context: Context) {
        appContext = context.applicationContext
        Notifier.ensureChannels(appContext)
        reschedule()
    }

    /**
     * Rimette la sveglia, **fra un attimo**: chi chiama ha appena scritto
     * nel database, e la scrittura deve fare in tempo ad arrivare. Più
     * richieste ravvicinate diventano una.
     */
    fun reschedule() {
        if (!::appContext.isInitialized) return
        pending?.cancel()
        pending = scope.launch {
            delay(DEBOUNCE_MS)
            rescheduleNow(appContext)
        }
    }

    /** Rimette la sveglia adesso: alla prima cosa che deve ancora suonare. */
    suspend fun rescheduleNow(context: Context) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val deliveredUpTo = deliveredUpTo(app, now)
        val nextReminder = runCatching {
            loadReminders(AppDatabase.getInstance(app), AppSettings.zoneId, deliveredUpTo, now + HORIZON_MS, words())
                .minOfOrNull { it.at }
        }.getOrNull()
        val nextWidget = WidgetStore.nextEnd()
        val next = listOfNotNull(nextReminder, nextWidget).minOrNull()

        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            app, 0, Intent(app, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (next == null) {
            alarms.cancel(pendingIntent)
            return
        }
        // Una cosa già passata (arrivata a telefono spento) suona subito.
        val at = next.coerceAtLeast(now + 500)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        }
    }

    /** Manda i promemoria arrivati dall'ultima volta a adesso (vedi il commento in cima). */
    suspend fun deliverDue(context: Context) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val from = deliveredUpTo(app, now)
        val due = runCatching {
            loadReminders(AppDatabase.getInstance(app), AppSettings.zoneId, from, now, words())
        }.getOrDefault(emptyList())
            .filter { now - it.at <= MISSED_WINDOW_MS }
            .sortedBy { it.at }
        prefs(app).edit().putLong(KEY_DELIVERED_UP_TO, now).apply()
        Notifier.postReminders(app, due)
    }

    private fun words() = ReminderWords(untitled = Strings.untitled, starts = NotifyStrings.starts, ends = NotifyStrings.ends)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Fin dove si è già mandato. La prima volta è **adesso**: le date già
     * passate prima che le notifiche esistessero non devono arrivare tutte
     * insieme.
     */
    private fun deliveredUpTo(context: Context, now: Long): Long {
        val prefs = prefs(context)
        val saved = prefs.getLong(KEY_DELIVERED_UP_TO, 0L)
        if (saved > 0L) return saved
        prefs.edit().putLong(KEY_DELIVERED_UP_TO, now).apply()
        return now
    }
}
