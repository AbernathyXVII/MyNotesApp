package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **La notifica di un bottone** ("Send notification"): vero se è partita. Sul
 * telefono è `Notifier.postButton`, come prima; sul PC le notifiche non ci sono
 * ancora, e il bottone lo dice come quando sul telefono mancano i permessi.
 */
expect fun postButtonNotification(context: PlatformContext, title: String, text: String): Boolean
