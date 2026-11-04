package com.gabriele.notionlocal.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

private val NoKeyboard = WindowInsets(0, 0, 0, 0)

@Composable
actual fun isKeyboardVisible(): Boolean = false

@Composable
actual fun keyboardTargetInsets(): WindowInsets = NoKeyboard
