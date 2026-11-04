package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.platform.PlatformContext

actual fun postButtonNotification(context: PlatformContext, title: String, text: String): Boolean =
    Notifier.postButton(context, title, text)
