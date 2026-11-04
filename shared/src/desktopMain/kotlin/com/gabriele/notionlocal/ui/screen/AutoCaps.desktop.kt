package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable

/** Sul PC: niente da fare, la tastiera è una tastiera vera. */
@Composable
actual fun AutoCaps(content: @Composable () -> Unit) = content()
