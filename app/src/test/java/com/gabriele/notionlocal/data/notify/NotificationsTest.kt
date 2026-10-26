package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.data.entity.DateRange
import com.gabriele.notionlocal.data.entity.Recurrence
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.parseDateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Le notifiche senza Android: il comando "/notification" come lo scrive
 * l'utente, il segno "Include notification" nelle date, e quando suona cosa.
 * Gli esempi sono quelli della richiesta del 25/09/2026.
 */
class NotificationsTest {

    private val rome = ZoneId.of("Europe/Rome")

    /** Venerdì 25 settembre 2026, le 18:00. */
    private val now = LocalDateTime.of(2026, 9, 25, 18, 0)

    private fun parse(line: String) = NotificationCommands.parse(line, now)

    private fun millis(at: LocalDateTime) = at.atZone(rome).toInstant().toEpochMilli()

    private fun dayMillis(day: LocalDate) = day.atStartOfDay(rome).toInstant().toEpochMilli()

    // --- Il comando ---

    @Test
    fun dateTimeAndText() {
        val command = parse("/notification 22.10.2026 19:41 Buttare l'immondizia")!!
        assertEquals(LocalDateTime.of(2026, 10, 22, 19, 41), command.at)
        assertEquals("Buttare l'immondizia", command.text)
    }

    @Test
    fun withoutTimeItRingsAtNine() {
        val command = parse("/notification 22.10.2026")!!
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 0), command.at)
        assertEquals("", command.text)
    }

    @Test
    fun parenthesesAroundTheTextGoAway() {
        assertEquals("Buttare l'immondizia", parse("/notification 22.10.2026 17:41 (Buttare l'immondizia)")!!.text)
    }

    @Test
    fun otherWaysToWriteTheDate() {
        val expected = LocalDateTime.of(2026, 10, 22, 17, 41)
        assertEquals(expected, parse("/notification 22/10/2026 17:41")!!.at)
        assertEquals(expected, parse("/notification 22-10-26 17.41")!!.at)
        assertEquals(expected, parse("/notification 2026-10-22 17:41")!!.at)
        // Senza l'anno: quest'anno, o il prossimo se è già passata.
        assertEquals(expected, parse("/notification 22.10 17:41")!!.at)
        assertEquals(LocalDateTime.of(2027, 9, 10, 9, 0), parse("/notification 10.09")!!.at)
    }

    @Test
    fun onlyTheTimeMeansTodayOrTomorrow() {
        assertEquals(LocalDateTime.of(2026, 9, 25, 19, 30), parse("/notification 19:30 Cena")!!.at)
        // Le 17:41 di oggi sono passate: domani.
        assertEquals(LocalDateTime.of(2026, 9, 26, 17, 41), parse("/notification 17:41")!!.at)
    }

    @Test
    fun nothingMeansTheNextNineOClock() {
        val command = parse("/notification")!!
        assertEquals(LocalDateTime.of(2026, 9, 26, 9, 0), command.at)
        assertEquals("", command.text)
        assertEquals(
            LocalDateTime.of(2026, 9, 25, 9, 0),
            NotificationCommands.nextDefault(LocalDateTime.of(2026, 9, 25, 8, 59))
        )
    }

    @Test
    fun alsoInItalianAndInCapitals() {
        assertEquals(LocalDateTime.of(2026, 10, 22, 17, 41), parse("/Notifica 22.10.2026 17:41 x")!!.at)
    }

    @Test
    fun otherLinesAreNotCommands() {
        assertNull(parse("/notificationx 22.10.2026"))
        assertNull(parse("scrivo /notification 22.10.2026"))
        assertNull(parse("notification 22.10.2026"))
        assertNull(parse("/toggle"))
    }

    // --- "Include notification" nelle date ---

    @Test
    fun theNotifyMarkSurvivesSaving() {
        val start = dayMillis(LocalDate.of(2026, 10, 23))
        val end = dayMillis(LocalDate.of(2026, 12, 17))
        val value = encodeDateRange(start, end, notify = true)
        assertTrue(value.endsWith("|n"))
        val range = parseDateRange(value)!!
        assertTrue(range.notify)
        assertEquals(start, range.start)
        assertEquals(end, range.end)
        assertFalse(range.hasTime)
        // Senza il segno, come tutte le date scritte prima.
        assertFalse(parseDateRange(encodeDateRange(start))!!.notify)
        assertFalse(parseDateRange("$start|$end|t")!!.notify)
    }

    @Test
    fun aDayWithoutTimeRingsAtNine() {
        val range = DateRange(start = dayMillis(LocalDate.of(2026, 2, 23)), end = null, notify = true)
        val times = dateReminderTimes(range, rome, millis(now.minusYears(1)), millis(now.plusYears(1)))
        assertEquals(listOf(DateReminderTime(millis(LocalDateTime.of(2026, 2, 23, 9, 0)))), times)
    }

    @Test
    fun aPeriodRingsOnTheFirstAndTheLastDay() {
        val range = DateRange(
            start = dayMillis(LocalDate.of(2026, 10, 23)),
            end = dayMillis(LocalDate.of(2026, 12, 17)),
            notify = true
        )
        val times = dateReminderTimes(range, rome, millis(now), millis(now.plusYears(1)))
        assertEquals(
            listOf(
                DateReminderTime(millis(LocalDateTime.of(2026, 10, 23, 9, 0))),
                DateReminderTime(millis(LocalDateTime.of(2026, 12, 17, 9, 0)), isEnd = true)
            ),
            times
        )
    }

    @Test
    fun withTheTimeItRingsAtThatTime() {
        val at = LocalDateTime.of(2026, 10, 22, 17, 41)
        val range = DateRange(start = millis(at), end = null, hasTime = true, notify = true)
        assertEquals(listOf(DateReminderTime(millis(at))), dateReminderTimes(range, rome, millis(now), millis(now.plusYears(1))))
    }

    @Test
    fun withoutTheSwitchItNeverRings() {
        val range = DateRange(start = dayMillis(LocalDate.of(2026, 10, 23)), end = null)
        assertTrue(dateReminderTimes(range, rome, millis(now), millis(now.plusYears(1))).isEmpty())
    }

    @Test
    fun aRepeatingDateRingsEachTime() {
        // Ogni settimana dal venerdì 2 ottobre: nelle prime tre settimane, tre volte.
        val range = DateRange(
            start = dayMillis(LocalDate.of(2026, 10, 2)),
            end = null,
            recurrence = Recurrence(freq = RecurrenceFreq.WEEKLY),
            notify = true
        )
        val times = dateReminderTimes(range, rome, millis(now), millis(LocalDateTime.of(2026, 10, 20, 0, 0)))
        assertEquals(
            listOf(2, 9, 16).map { DateReminderTime(millis(LocalDateTime.of(2026, 10, it, 9, 0))) },
            times
        )
    }

    @Test
    fun onlyWhatFallsInTheWindow() {
        val range = DateRange(start = dayMillis(LocalDate.of(2026, 10, 23)), end = null, notify = true)
        val nine = millis(LocalDateTime.of(2026, 10, 23, 9, 0))
        // Dopo `from`, fino a `to` compreso: così non arriva mai due volte.
        assertEquals(1, dateReminderTimes(range, rome, nine - 1, nine).size)
        assertTrue(dateReminderTimes(range, rome, nine, nine + 60_000).isEmpty())
    }

    // --- Cosa dice la notifica ---

    private val words = ReminderWords(untitled = "Untitled", starts = "Inizio", ends = "Fine")

    @Test
    fun theLineSaysItsTextOrElseThePageTitle() {
        val at = millis(LocalDateTime.of(2026, 10, 22, 19, 41))
        val rows = listOf(
            NotificationBlockRow("b1", "p1", """[{"text":"Buttare l'immondizia"}]""", at, "Casa"),
            NotificationBlockRow("b2", "p1", """[{"text":""}]""", at, "Casa")
        )
        val fires = blockReminders(rows, at - 1, at, "Untitled")
        assertEquals("Buttare l'immondizia", fires[0].title)
        assertEquals("Casa", fires[0].body)
        assertEquals("Casa", fires[1].title)
        assertEquals("p1", fires[1].pageId)
    }

    @Test
    fun theDateSaysThePageOfItsRow() {
        // L'esempio dell'utente: gli album, "Revive +" delle IVE il 23 febbraio.
        val value = encodeDateRange(dayMillis(LocalDate.of(2026, 2, 23)), notify = true)
        val rows = listOf(DateReminderRow("r1", "c1", value, "Revive +", "db1", "Album"))
        val nine = millis(LocalDateTime.of(2026, 2, 23, 9, 0))
        val fire = dateReminders(rows, rome, nine - 1, nine, words).single()
        assertEquals("Revive +", fire.title)
        assertEquals("Album", fire.body)
        assertEquals(nine, fire.at)
        assertEquals("r1", fire.rowId)
        assertEquals("db1", fire.databaseId)
    }

    @Test
    fun aPeriodSaysWhichEndItIs() {
        val value = encodeDateRange(
            dayMillis(LocalDate.of(2026, 10, 23)),
            dayMillis(LocalDate.of(2026, 12, 17)),
            notify = true
        )
        val rows = listOf(DateReminderRow("r1", "c1", value, "Tour", "db1", "Concerti"))
        val fires = dateReminders(rows, rome, millis(now), millis(now.plusYears(1)), words)
        assertEquals(listOf("Concerti · Inizio", "Concerti · Fine"), fires.map { it.body })
        assertEquals(2, fires.map { it.key }.distinct().size)
    }
}
