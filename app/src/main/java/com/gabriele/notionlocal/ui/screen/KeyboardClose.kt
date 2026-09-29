package com.gabriele.notionlocal.ui.screen

import android.os.SystemClock
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay

/**
 * Da quanto il cursore deve stare fermo nello stesso campo perché una
 * tastiera che scende voglia dire "l'ha chiusa l'utente". La tastiera
 * Samsung sparisce e riappare **durante i cambi di campo**: più corti di
 * così (l'attesa più lunga dell'editor è di 450 ms).
 */
private const val FOCUS_SETTLED_MS = 600L

/**
 * **Quando il cursore ha cambiato campo l'ultima volta.** Ogni campo di
 * scrittura lo segna prendendo il fuoco (`markFocusChanges`), e l'editor
 * anche quando il cursore passa da una riga all'altra.
 */
internal class KeyboardCloseWatch {
    private var lastFocusChangeAt = 0L

    fun markFocusChange() {
        lastFocusChangeAt = SystemClock.uptimeMillis()
    }

    val focusSettled: Boolean
        get() = SystemClock.uptimeMillis() - lastFocusChangeAt >= FOCUS_SETTLED_MS
}

/** Quello della schermata: la pagina, o il database a tutta pagina. */
internal val LocalKeyboardCloseWatch = staticCompositionLocalOf<KeyboardCloseWatch?> { null }

/** Un campo di scrittura dice quando prende il cursore: vedi `KeyboardCloseWatch`. */
internal fun Modifier.markFocusChanges(): Modifier = composed {
    val watch = LocalKeyboardCloseWatch.current
    onFocusChanged { if (it.isFocused) watch?.markFocusChange() }
}

/**
 * **Chiusa la tastiera, il cursore si spegne.**
 *
 * Il gesto indietro con la tastiera aperta se lo prende la tastiera, e
 * all'app non arriva: il segnale è la tastiera che si chiude. Prima si
 * aspettava che fosse **scesa del tutto** e poi ancora 450 ms, per la
 * tastiera Samsung che sparisce e riappare nei cambi di campo: misurato
 * sul telefono il 29/09/2026, il cursore si spegneva 750 ms dopo che la
 * tastiera aveva cominciato a scendere ("ci mette almeno un secondo per
 * sparire", l'utente).
 *
 * Adesso, **se il cursore è fermo nello stesso campo da un po'**, la
 * tastiera che comincia a scendere (`imeAnimationTarget` a zero) vuol
 * dire che l'ha chiusa l'utente: il cursore si spegne subito, insieme a
 * lei. Subito dopo un cambio di campo, o con un cambio chiesto dall'app in
 * viaggio (`focusInFlight`), resta l'attesa di prima.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClearFocusWhenKeyboardCloses(
    watch: KeyboardCloseWatch,
    anythingFocused: Boolean,
    focusInFlight: Boolean,
    settleDelayMs: Long
) {
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val imeComing = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current) > 0
    LaunchedEffect(anythingFocused) { if (anythingFocused) watch.markFocusChange() }
    LaunchedEffect(imeVisible, imeComing, anythingFocused, focusInFlight) {
        if (focusInFlight || !anythingFocused) return@LaunchedEffect
        if (imeVisible && !imeComing && watch.focusSettled) {
            focusManager.clearFocus()
        } else if (!imeVisible) {
            delay(settleDelayMs)
            focusManager.clearFocus()
        }
    }
}
