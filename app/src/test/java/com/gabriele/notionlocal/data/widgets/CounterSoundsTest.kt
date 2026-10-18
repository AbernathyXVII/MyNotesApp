package com.gabriele.notionlocal.data.widgets

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I suoni del contatore** (18/10/2026, chiesti dall'utente): un click su
 * +, uno più lieve su −, da spegnere o cambiare. I suoni li disegna l'app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CounterSoundsTest {

    @Test
    fun everySoundIsShortAudibleAndItsOwn() {
        val waves = CounterSound.entries.associateWith { CounterSounds.samples(it) }
        waves.forEach { (sound, samples) ->
            // Corto: un tocco non deve aspettare il suono di prima.
            assertTrue(sound.name, samples.size in 100..(COUNTER_SAMPLE_RATE / 4))
            assertTrue(sound.name, CounterSounds.peak(sound) > 0.1)
        }
        // Tutti diversi.
        assertEquals(CounterSound.entries.size, waves.values.map { it.toList() }.toSet().size)
        // Sempre uguali (il fruscio ha il seme fisso).
        assertTrue(CounterSounds.samples(CounterSound.CLICK).contentEquals(CounterSounds.samples(CounterSound.CLICK)))
        // Il meno, di partenza, è più lieve del più.
        assertTrue(CounterSounds.peak(CounterSound.SOFT_CLICK) < CounterSounds.peak(CounterSound.CLICK) / 2)
        // Suonarli senza audio vero non rompe niente, nemmeno a volume zero.
        CounterSounds.play(CounterSound.POP)
        CounterSounds.play(CounterSound.POP, volume = 0)
    }

    @Test
    fun theVolumeBarFollowsTheEar() {
        assertEquals(0f, CounterSounds.gain(0), 0f)
        assertEquals(1f, CounterSounds.gain(100), 0f)
        // A metà barra un quarto del guadagno: all'orecchio, circa metà.
        assertEquals(0.25f, CounterSounds.gain(50), 0.001f)
        // Fuori dalla barra si ferma ai capi; e sale sempre.
        assertEquals(1f, CounterSounds.gain(150), 0f)
        assertEquals(0f, CounterSounds.gain(-10), 0f)
        assertTrue((0..100).zipWithNext().all { (a, b) -> CounterSounds.gain(a) < CounterSounds.gain(b) })
    }

    @Test
    fun oldCountersHaveSoundsOnWithTheDefaults() {
        val json = Json { ignoreUnknownKeys = true }
        val old = json.decodeFromString<Widget>("""{"type":"counter","id":"c1","value":7,"name":"Caffè"}""") as CounterWidget
        assertEquals(7, old.value)
        assertTrue(old.soundsOn)
        assertEquals(CounterSound.CLICK, CounterSound.of(old.plusSound))
        assertEquals(CounterSound.SOFT_CLICK, CounterSound.of(old.minusSound))
        // Il volume, che i contatori di prima non hanno: tutto, come prima.
        assertEquals(100, old.volume)
        // Un nome che non c'è più torna al click.
        assertEquals(CounterSound.CLICK, CounterSound.of("NESSUNO"))
    }

    @Test
    fun soundsAreSavedWithTheCounter() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WidgetStore.init(context)
        WidgetStore.add(WidgetKind.COUNTER)
        val id = WidgetStore.state.widgets.filterIsInstance<CounterWidget>().last().id
        try {
            WidgetStore.setCounterSounds(id, on = false, plus = CounterSound.WOOD, minus = CounterSound.TICK)
            val counter = WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }
            assertFalse(counter.soundsOn)
            assertEquals("WOOD", counter.plusSound)
            assertEquals("TICK", counter.minusSound)
            // Il volume (18/10/2026): da 0 a 100, oltre non va.
            WidgetStore.setCounterVolume(id, 40)
            assertEquals(40, WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }.volume)
            WidgetStore.setCounterVolume(id, 250)
            assertEquals(100, WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }.volume)
            WidgetStore.setCounterVolume(id, -5)
            assertEquals(0, WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }.volume)
        } finally {
            WidgetStore.remove(id)
        }
    }
}
