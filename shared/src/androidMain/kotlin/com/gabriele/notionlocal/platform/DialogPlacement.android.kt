package com.gabriele.notionlocal.platform

import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

@Composable
actual fun PlaceDialogAtBottom() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setGravity(Gravity.BOTTOM) }
}
