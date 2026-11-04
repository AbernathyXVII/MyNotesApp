package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.data.notify.NoteAlarms
import com.gabriele.notionlocal.data.notify.Notifier
import com.gabriele.notionlocal.data.notify.TimerNotifications
import com.gabriele.notionlocal.platform.PlatformContext

internal actual fun syncTimerNotifications(context: PlatformContext, before: WidgetsState, after: WidgetsState) =
    TimerNotifications.sync(context, before, after)

internal actual fun restoreTimerNotifications(context: PlatformContext, state: WidgetsState) =
    TimerNotifications.restore(context, state)

internal actual fun postWidgetEvents(context: PlatformContext, events: List<WidgetEvent>, pomodoroSound: String?) =
    Notifier.postWidgetEvents(context, events, pomodoroSound)

internal actual fun rescheduleWidgetAlarm() = NoteAlarms.reschedule()
