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
    fun aJustStartedPomodoroNeverShowsMoreThanItStartedWith() {
        // ▶ premuto a metà secondo, con l'ora della barra ancora ferma allo scatto prima.
        val started = 10_600L
        val pomodoro = PomodoroWidget(running = true, remainingMs = MINUTE_MS, endsAt = started + MINUTE_MS)
        assertEquals(MINUTE_MS, pomodoro.remainingAt(10_005))
        assertEquals(MINUTE_MS - 405, pomodoro.remainingAt(11_005))
    }

    @Test
    fun theTimerRedrawsExactlyWhenItsDigitChanges() {
        // Appena avviato: "01:00" per un secondo pieno, poi "00:59".
        assertEquals(1_000, msUntilTimerTicks(60_000))
        // Avviato a metà di un secondo dell'orologio: conta il timer, non l'orologio.
        assertEquals(600, msUntilTimerTicks(59_600))
        assertEquals(1, msUntilTimerTicks(59_001))
        assertEquals(1_000, msUntilTimerTicks(59_000))
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
                PomodoroWidget(sessionName = "Studio", sessionMinutes = 50, running = true, endsAt = 123L),
                StopwatchWidget(accumulatedMs = 4_000, startedAt = 99L, laps = listOf(800L, 4_420L)),
                TimerWidget(timers = listOf(CountdownTimer(durationMs = 90_000).startedAt(1_000), CountdownTimer()))
            )
        )
        assertEquals(state, json.decodeFromString<WidgetsState>(json.encodeToString(state)))
    }

    // --- Cronometro ---

    @Test
    fun theStopwatchCountsWhileRunningAndKeepsItsTimeWhenStopped() {
        val idle = StopwatchWidget()
        assertTrue(idle.idle)
        val running = idle.started(1_000)
        assertEquals(2_500, running.elapsedAt(3_500))
        val stopped = running.stopped(3_500)
        assertEquals(2_500, stopped.elapsedAt(99_999))
        assertTrue(!stopped.running && !stopped.idle)
        // Resume riparte da dov'era.
        assertEquals(3_500, stopped.started(10_000).elapsedAt(11_000))
        assertTrue(stopped.cleared().idle)
    }

    @Test
    fun lapsListTheNewestFirstWithLapAndOverallTimes() {
        // I tempi delle foto dell'utente: 0,80 — 4,42 — 7,76.
        val laps = StopwatchWidget().started(0).lapped(800).lapped(4_420).lapped(7_760).laps
        val rows = lapRows(laps)
        assertEquals(listOf(3, 2, 1), rows.map { it.number })
        assertEquals(LapRow(3, 3_340, 7_760), rows[0])
        assertEquals(LapRow(2, 3_620, 4_420), rows[1])
        assertEquals(LapRow(1, 800, 800), rows[2])
        // Come nella foto: il primo (0,80) il più veloce, il secondo (3,62) il più lento.
        assertEquals(1 to 2, fastestAndSlowestLap(rows))
        assertNull(fastestAndSlowestLap(rows.take(2)))
    }

    @Test
    fun stopwatchTimesReadLikeSamsung() {
        assertEquals("00:32.52", stopwatchText(32_520))
        assertEquals("00:00.80", stopwatchText(800))
        assertEquals("1:02:03.45", stopwatchText(3_723_450))
    }

    // --- Timer ---

    @Test
    fun aTimerRunsPausesResumesAndFinishes() {
        val set = CountdownTimer(durationMs = 10 * MINUTE_MS)
        val running = set.startedAt(0)
        assertEquals(5 * MINUTE_MS, running.remainingAt(5 * MINUTE_MS))
        val paused = running.pausedAt(5 * MINUTE_MS)
        assertEquals(5 * MINUTE_MS, paused.remainingAt(99 * MINUTE_MS))
        val resumed = paused.resumedAt(20 * MINUTE_MS)
        assertEquals(25 * MINUTE_MS, resumed.endsAt)
        assertEquals(resumed to null, resumed.finishedBy(24 * MINUTE_MS))
        val (finished, endedAt) = resumed.finishedBy(26 * MINUTE_MS)
        assertTrue(finished.finished && !finished.running)
        assertEquals(25 * MINUTE_MS, endedAt)
        assertEquals(0, finished.remainingAt(30 * MINUTE_MS))
        // ↻: di nuovo alle rotelle, col tempo di prima.
        val back = finished.backToSetup()
        assertTrue(!back.started && back.durationMs == 10 * MINUTE_MS && back.remainingMs == 10 * MINUTE_MS)
    }

    @Test
    fun theWheelsGoFromZeroTo99HoursAnd59MinutesAndSeconds() {
        assertEquals(Triple(0, 10, 0), wheelValues(10 * MINUTE_MS))
        assertEquals(Triple(99, 59, 59), wheelValues(wheelDurationMs(99, 59, 59)))
        assertEquals(wheelDurationMs(1, 2, 3), 3_723_000L)
    }

    @Test
    fun timerTextsReadLikeSamsung() {
        assertEquals("9:55", countdownText(595_000))
        assertEquals("10:00", countdownText(599_001))
        assertEquals("1:09:55", countdownText(4_195_000))
        assertEquals("10 m", durationLabel(10 * MINUTE_MS))
        assertEquals("1 h 30 m", durationLabel(90 * MINUTE_MS))
        assertEquals("45 s", durationLabel(45_000))
    }

    @Test
    fun onlyTheTimersAddedAfterTheFirstCanBeDeleted() {
        val first = CountdownTimer()
        val second = CountdownTimer().startedAt(0)
        val third = CountdownTimer()
        val widget = TimerWidget(timers = listOf(first, second, third))
        // Il primo resta sempre.
        assertEquals(widget, widget.withoutTimer(first.id))
        // Dal secondo in poi si toglie, in qualunque stato (qui uno che corre).
        assertEquals(listOf(first, third), widget.withoutTimer(second.id).timers)
        assertEquals(listOf(first, second), widget.withoutTimer(third.id).timers)
        // Un id che non c'è non cambia niente.
        assertEquals(widget, widget.withoutTimer("nessuno"))
    }
}
