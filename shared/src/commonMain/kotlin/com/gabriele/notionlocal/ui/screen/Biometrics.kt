package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **L'impronta** (e il blocco schermo del telefono) per le pagine bloccate
 * con la password: sul telefono la finestra di sistema di Android, da Android
 * 11; sul PC, per ora, non c'è, e si usa solo la password. Nel codice comune
 * dal 04/11/2026 (prima stava in `LockUi`).
 */
expect object Biometrics {
    /** Se si può chiedere l'impronta. */
    fun available(context: PlatformContext): Boolean

    /** Il blocco schermo del telefono (PIN, sequenza, password): serve a rifare una password dimenticata. */
    fun deviceCredentialAvailable(context: PlatformContext): Boolean

    /**
     * Chiede l'impronta (o, con [deviceCredential], il blocco schermo). Una
     * sola delle due risposte arriva: [onSuccess], o [onFallback] se si è
     * toccato "Usa la password", si è annullato o non è andata.
     */
    fun authenticate(
        context: PlatformContext,
        title: String,
        deviceCredential: Boolean,
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    )
}
