package com.gabriele.notionlocal.platform

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun isKeyboardVisible(): Boolean = WindowInsets.isImeVisible

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun keyboardTargetInsets(): WindowInsets = WindowInsets.imeAnimationTarget
