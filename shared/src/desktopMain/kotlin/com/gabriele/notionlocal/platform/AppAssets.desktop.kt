package com.gabriele.notionlocal.platform

actual fun readAppAsset(context: PlatformContext, path: String): String? = runCatching {
    PlatformContext::class.java.getResourceAsStream("/" + path)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
}.getOrNull()

actual fun canDrawEmoji(emoji: String): Boolean = true
