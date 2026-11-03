package com.gabriele.notionlocal.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.platform.LocalContext

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = LocalContext
