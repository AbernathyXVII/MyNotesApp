package com.gabriele.notionlocal.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { error("La finestra non ha messo il PlatformContext (vedi desktop/Main.kt)") }
