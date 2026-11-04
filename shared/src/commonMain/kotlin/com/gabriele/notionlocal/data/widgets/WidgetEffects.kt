package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.platform.PlatformContext

/*
 * **Quello che i widget fanno fuori dall'app** (dal 04/11/2026): `WidgetStore`
 * è nel codice comune, queste quattro cose le fa ogni sistema. Sul telefono
 * sono quelle di sempre; sul PC, per ora, niente: né tendina né sveglie.
 */

/** Le notifiche dei timer partiti, dal prima al dopo di una modifica (sul telefono `TimerNotifications.sync`). */
internal expect fun syncTimerNotifications(context: PlatformContext, before: WidgetsState, after: WidgetsState)

/** Le notifiche dei timer partiti, rimesse se non ci sono più (sul telefono `TimerNotifications.restore`). */
internal expect fun restoreTimerNotifications(context: PlatformContext, state: WidgetsState)

/** La notifica e il suono di pomodori e timer finiti (sul telefono `Notifier.postWidgetEvents`). */
internal expect fun postWidgetEvents(context: PlatformContext, events: List<WidgetEvent>, pomodoroSound: String?)

/** La sveglia per la prossima fine, ad app chiusa (sul telefono `NoteAlarms.reschedule`). */
internal expect fun rescheduleWidgetAlarm()
