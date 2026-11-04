package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.PlatformContext

actual object Biometrics {
    actual fun available(context: PlatformContext): Boolean = false

    actual fun deviceCredentialAvailable(context: PlatformContext): Boolean = false

    actual fun authenticate(
        context: PlatformContext,
        title: String,
        deviceCredential: Boolean,
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    ) = Unit
}
