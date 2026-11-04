package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable

/**
 * **Il gesto "indietro"** (dal 04/11/2026, per le schermate nel codice comune):
 * sul telefono è il `BackHandler` di Android, lo stesso di prima; sul PC, per
 * ora, non fa niente (l'"indietro" del PC arriverà con le sue scorciatoie).
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
