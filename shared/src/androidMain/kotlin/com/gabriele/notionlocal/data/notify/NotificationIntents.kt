package com.gabriele.notionlocal.data.notify

import android.content.Intent

/**
 * **La pagina da aprire, dall'`Intent` della notifica toccata** (`MainActivity`).
 * Era dentro `NotificationTarget` fino al 04/11/2026: quello è passato al
 * codice comune, e l'`Intent` è di Android.
 */
fun NotificationTarget.takeFrom(intent: Intent?) {
    intent ?: return
    if (intent.getBooleanExtra(Notifier.EXTRA_OPEN_WIDGETS, false)) {
        openWidgets.value = true
        intent.removeExtra(Notifier.EXTRA_OPEN_WIDGETS)
    }
    if (intent.getBooleanExtra(Notifier.EXTRA_OPEN_BACKUP, false)) {
        openBackup.value = true
        intent.removeExtra(Notifier.EXTRA_OPEN_BACKUP)
    }
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
