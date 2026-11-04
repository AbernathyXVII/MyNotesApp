package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged

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
class KeyboardCloseWatch {
    private var lastFocusChangeAt = 0L

    fun markFocusChange() {
        lastFocusChangeAt = (System.nanoTime() / 1_000_000)
    }

    val focusSettled: Boolean
        get() = (System.nanoTime() / 1_000_000) - lastFocusChangeAt >= FOCUS_SETTLED_MS
}

/** Quello della schermata: la pagina, o il database a tutta pagina. */
val LocalKeyboardCloseWatch = staticCompositionLocalOf<KeyboardCloseWatch?> { null }

/** Un campo di scrittura dice quando prende il cursore: vedi `KeyboardCloseWatch`. */
fun Modifier.markFocusChanges(): Modifier = composed {
    val watch = LocalKeyboardCloseWatch.current
    onFocusChanged { if (it.isFocused) watch?.markFocusChange() }
}

/**
 * **Chiusa la tastiera, il cursore si spegne** (vedi `KeyboardClose.android.kt`).
 * Sul PC la tastiera sullo schermo non c'è, e il cursore resta finché non si
 * tocca altrove.
 */
@Composable
expect fun ClearFocusWhenKeyboardCloses(
    watch: KeyboardCloseWatch,
    anythingFocused: Boolean,
    focusInFlight: Boolean,
    settleDelayMs: Long
)
