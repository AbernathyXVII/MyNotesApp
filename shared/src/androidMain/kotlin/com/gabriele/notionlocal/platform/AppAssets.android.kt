package com.gabriele.notionlocal.platform

import android.graphics.Paint

actual fun readAppAsset(context: PlatformContext, path: String): String? = runCatching {
    context.applicationContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}.getOrNull()

private val glyphPaint by lazy { Paint() }

actual fun canDrawEmoji(emoji: String): Boolean = glyphPaint.hasGlyph(emoji)
