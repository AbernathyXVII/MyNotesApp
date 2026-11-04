package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable

/** Sul PC: niente tastiera sullo schermo, niente da fare. */
@Composable
actual fun ClearFocusWhenKeyboardCloses(
    watch: KeyboardCloseWatch,
    anythingFocused: Boolean,
    focusInFlight: Boolean,
    settleDelayMs: Long
) = Unit
