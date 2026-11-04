package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.platform.PlatformContext

internal actual fun syncTimerNotifications(context: PlatformContext, before: WidgetsState, after: WidgetsState) = Unit

internal actual fun restoreTimerNotifications(context: PlatformContext, state: WidgetsState) = Unit

internal actual fun postWidgetEvents(context: PlatformContext, events: List<WidgetEvent>, pomodoroSound: String?) = Unit

internal actual fun rescheduleWidgetAlarm() = Unit
