package com.gabriele.notionlocal.platform

import java.io.InputStream

actual fun readAppAsset(context: PlatformContext, path: String): String? = runCatching {
    PlatformContext::class.java.getResourceAsStream("/" + path)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
}.getOrNull()

actual fun canDrawEmoji(emoji: String): Boolean = true

actual fun openAppAsset(context: PlatformContext, path: String): InputStream? =
    PlatformContext::class.java.getResourceAsStream("/" + path)
