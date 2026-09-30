package com.gabriele.notionlocal.data.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.WidgetStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * **La sveglia è suonata** (vedi `NoteAlarms`): si mandano i promemoria
 * arrivati, si fanno avanzare timer e pomodori finiti (che mandano la loro
 * notifica), e si rimette la sveglia alla cosa dopo.
 *
 * Può succedere ad app chiusa: l'app parte solo per questo, senza
 * schermate. Impostazioni e widget si caricano qui, prima di tutto.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val result = goAsync()
        AppSettings.init(app)
        // Sul thread principale, come ogni modifica dei widget.
        WidgetStore.onAlarm(app)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NoteAlarms.deliverDue(app)
                NoteAlarms.rescheduleNow(app)
            } finally {
                result.finish()
            }
        }
    }
}

/**
 * **La sveglia va rimessa**: il telefono si è riacceso (le sveglie di
 * Android non sopravvivono a un riavvio), l'app è stata aggiornata, o
 * sono cambiati ora o fuso.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val result = goAsync()
        AppSettings.init(app)
        WidgetStore.init(app)
        // Riaccendendosi il telefono ha tolto le notifiche dei timer.
        WidgetStore.restoreTimerNotifications()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NoteAlarms.rescheduleNow(app)
            } finally {
                result.finish()
            }
        }
    }
}
