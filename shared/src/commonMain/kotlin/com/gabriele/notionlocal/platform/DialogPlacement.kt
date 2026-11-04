package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable

/**
 * **La finestra di dialogo sta in fondo allo schermo** (dentro il contenuto di
 * un `Dialog`): sul telefono è Android a metterla lì, così sa dov'è la barra
 * di navigazione (vedi `SamsungPickers`); sul PC resta dov'è.
 */
@Composable
expect fun PlaceDialogAtBottom()
