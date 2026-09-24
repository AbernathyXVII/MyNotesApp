package com.gabriele.notionlocal.data.widgets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * I calcoli dei widget della barra laterale, senza Android: quanto è
 * passato dell'anno e delle date dell'utente, l'ordine dei fusi da ovest a
 * est, i turni del pomodoro, e il salvataggio che deve rileggersi uguale.
 */
class WidgetsTest {

    private val rome = ZoneId.of("Europe/Rome")

    /** Mercoledì 1 luglio 2026, mezzogiorno a Roma. */
    private val summerNoon = LocalDate.of(2026, 7, 1).atTime(12, 0).atZone(rome).toInstant()

    private fun bar(kind: ProgressKind) = ProgressBarSetting(kind = kind)

    @Test
    fun dayIsHalfDoneAtNoon() {
        assertEquals(0.5, bar(ProgressKind.DAY).fractionAt(summerNoon, rome)!!, 1e-9)
    }

    @Test
    fun weekStartsOnMonday() {
        // Mercoledì a mezzogiorno: passati lunedì, martedì e mezzo mercoledì.
        assertEquals(2.5 / 7, bar(ProgressKind.WEEK).fractionAt(summerNoon, rome)!!, 1e-9)
    }

    @Test
    fun monthAndYearCountFromTheirFirstDay() {
        assertEquals(0.5 / 31, bar(ProgressKind.MONTH).fractionAt(summerNoon, rome)!!, 1e-9)
        // 181 giorni e mezzo su 365 (il 2026 non è bisestile).
        assertEquals(181.5 / 365, bar(ProgressKind.YEAR).fractionAt(summerNoon, rome)!!, 1e-3)
    }

    @Test
    fun customDatesGoFromStartToEndAndStopAtTheEdges() {
        val custom = ProgressBarSetting(
            kind = ProgressKind.CUSTOM,
            name = "Esame",
            startEpochDay = LocalDate.of(2026, 6, 21).toEpochDay(),
            endEpochDay = LocalDate.of(2026, 7, 11).toEpochDay()
        )
        // Dal 21 giugno all'1 luglio a mezzogiorno: 10,5 giorni su 20.
        assertEquals(10.5 / 20, custom.fractionAt(summerNoon, rome)!!, 1e-9)
        val before = LocalDate.of(2026, 6, 1).atStartOfDay(rome).toInstant()
        val after = LocalDate.of(2026, 8, 1).atStartOfDay(rome).toInstant()
        assertEquals(0.0, custom.fractionAt(before, rome)!!, 0.0)
        assertEquals(1.0, custom.fractionAt(after, rome)!!, 0.0)
        assertNull(ProgressBarSetting(kind = ProgressKind.CUSTOM).fractionAt(summerNoon, rome))
    }

    @Test
    fun zonesAreSortedFromWestToEast() {
        val zones = listOf("Asia/Kolkata", "Europe/Rome", "Pacific/Honolulu", "America/New_York", "Asia/Tokyo", "America/Los_Angeles")
        assertEquals(
            listOf("Pacific/Honolulu", "America/Los_Angeles", "America/New_York", "Europe/Rome", "Asia/Kolkata", "Asia/Tokyo"),
            sortedWestToEast(zones, summerNoon)
        )
    }

    @Test
    fun theOrderFollowsDaylightSavingTime() {
        // Londra e Lisbona: in estate tutte e due a +1, a gennaio a 0, e
        // Reykjavík (sempre 0) passa da prima a pari con loro.
        val zones = listOf("Europe/London", "Atlantic/Reykjavik")
        assertEquals(listOf("Atlantic/Reykjavik", "Europe/London"), sortedWestToEast(zones, summerNoon))
        val winter = Instant.parse("2026-01-15T12:00:00Z")
        // A pari distanza da Greenwich vale il nome: "London" prima di "Reykjavik".
        assertEquals(listOf("Europe/London", "Atlantic/Reykjavik"), sortedWestToEast(zones, winter))
    }

    @Test
    fun namesAndOffsetsReadLikeTheWidget() {
        assertEquals("Los Angeles", cityName("America/Los_Angeles"))
        assertEquals("Buenos Aires", cityName("America/Argentina/Buenos_Aires"))
        assertEquals("America", regionName("America/Argentina/Buenos_Aires"))
        assertEquals("GMT+5:30", gmtLabel(ZoneId.of("Asia/Kolkata"), summerNoon))
        assertEquals("GMT-10", gmtLabel(ZoneId.of("Pacific/Honolulu"), summerNoon))
        assertEquals("GMT", gmtLabel(ZoneId.of("Europe/London"), Instant.parse("2026-01-15T12:00:00Z")))
        assertTrue("Europe/Rome" in selectableZoneIds())
        assertTrue(selectableZoneIds().none { it.startsWith("US/") || it.startsWith("Etc/") })
    }

    @Test
    fun aFinishedSessionTurnsIntoABreakWithoutStopping() {
        val start = 1_000_000L
        val pomodoro = PomodoroWidget(running = true, endsAt = start + 25 * MINUTE_MS, remainingMs = 25 * MINUTE_MS)
        val now = start + 25 * MINUTE_MS + 2_000
        val (next, ended) = pomodoro.advancedTo(now)
        assertEquals(PomodoroPhase.BREAK, next.phase)
        assertTrue(next.running)
        // La pausa conta dal momento in cui la sessione è finita, non da quando ce ne si accorge.
        assertEquals(start + 30 * MINUTE_MS, next.endsAt)
        assertEquals(start + 25 * MINUTE_MS, ended)
    }

    @Test
    fun phasesMissedWhileTheAppWasClosedAreCaughtUp() {
        val start = 0L
        val pomodoro = PomodoroWidget(running = true, endsAt = start + 25 * MINUTE_MS)
        // Un'ora e dieci dopo l'avvio: sessione (25), pausa (5), sessione (25), pausa (5) → di nuovo sessione, dal 60.
        val (next, ended) = pomodoro.advancedTo(start + 70 * MINUTE_MS)
        assertEquals(PomodoroPhase.SESSION, next.phase)
        assertEquals(start + 85 * MINUTE_MS, next.endsAt)
        assertEquals(start + 60 * MINUTE_MS, ended)
    }

    @Test
    fun aPausedOrUnfinishedPomodoroDoesNotMove() {
        val paused = PomodoroWidget(running = false, remainingMs = 10 * MINUTE_MS)
        assertEquals(paused to null, paused.advancedTo(Long.MAX_VALUE / 2))
        val running = PomodoroWidget(running = true, endsAt = 5_000)
        assertEquals(running to null, running.advancedTo(4_000))
        assertEquals(1_000, running.remainingAt(4_000))
    }

    @Test
    fun everyWidgetIsSavedAndReadBackTheSame() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val state = WidgetsState(
            hidden = true,
            widgets = listOf(
                TimeZonesWidget(clocks = listOf(ClockSetting(zoneId = "Asia/Tokyo", analog = false)), list = listOf("Europe/Rome")),
                LifeProgressWidget(),
                CounterWidget(value = -3, name = "Caffè"),
                PomodoroWidget(sessionName = "Studio", sessionMinutes = 50, running = true, endsAt = 123L)
            )
        )
        assertEquals(state, json.decodeFromString<WidgetsState>(json.encodeToString(state)))
    }
}
