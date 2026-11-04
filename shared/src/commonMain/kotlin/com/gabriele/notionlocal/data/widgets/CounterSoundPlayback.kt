package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.platform.PlatformContext

/*
 * **I suoni del contatore** (dal 04/11/2026, per la barra laterale nel codice
 * comune): sul telefono `CounterSounds`, come prima; sul PC per ora muti.
 */

/** I suoni pronti prima del primo tocco: vedi `CounterSounds.prepare`. */
expect fun prepareCounterSounds(context: PlatformContext)

/** Un suono del contatore, al volume [volume] (0–100). */
expect fun playCounterSound(sound: CounterSound, volume: Int)
