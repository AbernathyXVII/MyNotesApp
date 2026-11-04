package com.gabriele.notionlocal.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build

actual fun copyRichText(context: PlatformContext, plain: String, html: String): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newHtmlText("PageFlow", plain, html))
    return true
}

actual val systemAnnouncesCopies: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
