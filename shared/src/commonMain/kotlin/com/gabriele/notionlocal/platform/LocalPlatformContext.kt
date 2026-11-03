package com.gabriele.notionlocal.platform

import androidx.compose.runtime.ProvidableCompositionLocal

/**
 * Il [PlatformContext] dentro le schermate: sul telefono è proprio
 * `LocalContext` di Compose, sul PC lo mette la finestra (`desktop/Main.kt`).
 */
expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>
