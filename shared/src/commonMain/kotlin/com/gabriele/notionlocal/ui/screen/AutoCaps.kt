package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable

/**
 * **La maiuscola a inizio frase** della tastiera del telefono, nei campi di
 * testo che la chiedono (vedi `AutoCaps.android.kt`). Sul PC si scrive con
 * una tastiera vera, e il contenuto resta com'è.
 */
@Composable
expect fun AutoCaps(content: @Composable () -> Unit)
