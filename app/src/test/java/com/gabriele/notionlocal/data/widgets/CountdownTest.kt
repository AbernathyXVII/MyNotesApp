package com.gabriele.notionlocal.data.widgets

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * **Il widget Countdown** (27/10/2026, chiesto dall'utente, come quello di
 * Indify su Notion): quanto manca, in mesi del calendario e poi settimane,
 * giorni, ore, minuti e secondi; una data passata conta il tempo trascorso.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CountdownTest {

    private fun at(text: String) = LocalDateTime.parse(text)

    @Test
    fun theTimeLeftInCalendarMonthsThenTheRest() {
        // Dalle 18 del 27 ottobre a Natale: un mese (al 27 novembre), poi 27
        // giorni e 6 ore, cioè 3 settimane, 6 giorni e 6 ore.
        val parts = countdownParts(at("2026-10-27T18:00:00"), at("2026-12-25T00:00:00"))
        assertEquals(CountdownParts(past = false, months = 1, weeks = 3, days = 6, hours = 6, minutes = 0, seconds = 0), parts)
        // Un secondo dopo, i secondi scendono e tutto il resto con loro.
        assertEquals(
            CountdownParts(past = false, months = 1, weeks = 3, days = 6, hours = 5, minutes = 59, seconds = 59),
            countdownParts(at("2026-10-27T18:00:01"), at("2026-12-25T00:00:00"))
        )
    }

    @Test
    fun aMonthFromTheEndOfJanuaryEndsInFebruary() {
        // Dal 31 gennaio "un mese" arriva al 28 febbraio: al primo marzo manca un giorno in più.
        val parts = countdownParts(at("2026-01-31T00:00:00"), at("2026-03-01T00:00:00"))
        assertEquals(1L, parts.months)
        assertEquals(1L, parts.days)
        assertEquals(0L, parts.weeks)
    }

    @Test
    fun monthsGoOnPastTwelve() {
        val parts = countdownParts(at("2026-10-27T00:00:00"), at("2028-01-27T00:00:00"))
        assertEquals(15L, parts.months)
    }

    @Test
    fun aDateInThePastCountsTheTimeSince() {
        val parts = countdownParts(at("2026-10-27T18:00:00"), at("2026-10-25T17:59:30"))
        assertTrue(parts.past)
        assertEquals(CountdownParts(past = true, months = 0, weeks = 0, days = 2, hours = 0, minutes = 0, seconds = 30), parts)
    }

    @Test
    fun theFirstCountdownIsTheNextChristmas() {
        assertEquals(at("2026-12-25T00:00:00"), christmasCountdown("Christmas", LocalDate.of(2026, 10, 27)).targetTime)
        // Il giorno di Natale, quello dell'anno dopo.
        assertEquals(at("2027-12-25T00:00:00"), christmasCountdown("Christmas", LocalDate.of(2026, 12, 25)).targetTime)
    }

    @Test
    fun countdownsAreSavedOneAfterTheOther() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WidgetStore.init(context)
        WidgetStore.add(WidgetKind.COUNTDOWN, christmasCountdown("Christmas", LocalDate.of(2026, 10, 27)))
        val widget = WidgetStore.state.widgets.filterIsInstance<CountdownWidget>().last()
        try {
            assertEquals(listOf("Christmas"), widget.countdowns.map { it.name })
            val exam = CountdownEntry(name = "Esame", target = "2027-02-10T09:30:00")
            WidgetStore.addCountdown(widget.id, exam)
            val now = WidgetStore.state.widgets.filterIsInstance<CountdownWidget>().single { it.id == widget.id }
            assertEquals(listOf("Christmas", "Esame"), now.countdowns.map { it.name })
            WidgetStore.updateCountdown(widget.id, exam.copy(target = "2025-01-01T00:00:00"))
            assertEquals(
                at("2025-01-01T00:00:00"),
                WidgetStore.state.widgets.filterIsInstance<CountdownWidget>().single { it.id == widget.id }.countdowns.last().targetTime
            )
            WidgetStore.removeCountdown(widget.id, exam.id)
            assertEquals(1, WidgetStore.state.widgets.filterIsInstance<CountdownWidget>().single { it.id == widget.id }.countdowns.size)

            // Come si salva: un widget "countdown" coi suoi countdown dentro.
            val json = Json { ignoreUnknownKeys = true }
            val saved = json.encodeToString<Widget>(widget)
            assertTrue(saved, "\"countdown\"" in saved)
            assertEquals(widget, json.decodeFromString<Widget>(saved))
            assertFalse(json.decodeFromString<Widget>(saved).let { it as CountdownWidget }.countdowns.isEmpty())
        } finally {
            WidgetStore.remove(widget.id)
        }
    }
}
