package com.gabriele.notionlocal.data.notify

import android.content.Intent
import androidx.compose.runtime.mutableStateOf

/**
 * **Il permesso di mostrare notifiche** (Android 13 e dopo lo chiede
 * all'utente). Lo chiede `MainActivity`, che è l'unica a poterlo fare; qui
 * c'è solo il filo per chiederglielo da dove serve: creando una notifica
 * con "/notification", accendendo "Include notification" in una data,
 * facendo partire un timer o un pomodoro. Chiesto quando serve, e non
 * all'avvio: così si capisce a cosa serve.
 */
object NotificationPermission {
    /** Lo imposta `MainActivity` finché è viva. */
    var requester: (() -> Unit)? = null

    fun request() {
        requester?.invoke()
    }
}

/** Cosa aprire dopo aver toccato una notifica. */
data class OpenRequest(val pageId: String?, val rowId: String?, val databaseId: String?)

/**
 * **La pagina da aprire toccando una notifica.** `MainActivity` la legge
 * dall'intent (all'avvio o ad app già aperta), la navigazione
 * (`AppNavHost`) la apre e la consuma.
 */
object NotificationTarget {
    val pending = mutableStateOf<OpenRequest?>(null)

    fun takeFrom(intent: Intent?) {
        intent ?: return
        val pageId = intent.getStringExtra(Notifier.EXTRA_PAGE_ID)
        val rowId = intent.getStringExtra(Notifier.EXTRA_ROW_ID)
        val databaseId = intent.getStringExtra(Notifier.EXTRA_DATABASE_ID)
        if (pageId == null && rowId == null && databaseId == null) return
        pending.value = OpenRequest(pageId, rowId, databaseId)
        // Letta una volta: ruotando lo schermo non si riapre.
        intent.removeExtra(Notifier.EXTRA_PAGE_ID)
        intent.removeExtra(Notifier.EXTRA_ROW_ID)
        intent.removeExtra(Notifier.EXTRA_DATABASE_ID)
    }
}
