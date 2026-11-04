package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.platform.PlatformContext

actual fun prepareCounterSounds(context: PlatformContext) = CounterSounds.prepare(context)

actual fun playCounterSound(sound: CounterSound, volume: Int) = CounterSounds.play(sound, volume)
