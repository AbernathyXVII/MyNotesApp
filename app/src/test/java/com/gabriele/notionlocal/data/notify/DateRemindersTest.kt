package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.data.entity.DateRange
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.EventDraft
import com.gabriele.notionlocal.data.entity.Recurrence
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.entity.ReminderUnit
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.isValidReminder
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.reminderParts
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.ui.i18n.ReminderTexts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * **Più notifiche per una data, come su Google Calendar** (26/10/2026,
 * chiesto dall'utente: "la notifica dieci ore prima, tre giorni prima, due
 * settimane prima"): come si salvano, quando suonano e cosa dicono.
 */
class DateRemindersTest {

    private val rome = ZoneId.of("Europe/Rome")

    private fun millis(at: LocalDateTime) = at.atZone(rome).toInstant().toEpochMilli()

    private val tenHours = 10 * 60
    private val threeDays = 3 * 24 * 60
    private val twoWeeks = 2 * 7 * 24 * 60

    /** Il concerto: giovedì 12 novembre 2026 alle 21:00, fino alle 23:30. */
    private val concert = LocalDateTime.of(2026, 11, 12, 21, 0)
    private val concertEnd = LocalDateTime.of(2026, 11, 12, 23, 30)

    @Test
    fun theRemindersAreSavedBeforeTheMark() {
        val value = encodeDateRange(millis(concert), millis(concertEnd), hasTime = true, notify = true, reminders = listOf(twoWeeks, 0, tenHours, threeDays))
        // La "n" resta l'ultima: la sveglia (`getNotifyingDateCells`) la trova come prima.
        assertTrue(value.endsWith("|0,600,4320,20160|n"))
        val range = parseDateRange(value)!!
        assertTrue(range.notify)
        assertTrue(range.hasTime)
        assertEquals(millis(concertEnd), range.end)
        assertEquals(listOf(0, tenHours, threeDays, twoWeeks), range.reminders)
    }

    @Test
    fun onlyAtTheStartIsTheValueOfAlways() {
        val start = millis(concert)
        // Come le date salvate prima: niente campo in più.
        assertEquals("$start||t||n", encodeDateRange(start, hasTime = true, notify = true))
        assertEquals("$start||t||n", encodeDateRange(start, hasTime = true, notify = true, reminders = listOf(0)))
        assertEquals(listOf(0), parseDateRange("$start||t||n")!!.reminders)
        // Senza notifiche nell'elenco la notifica è spenta.
        assertFalse(parseDateRange(encodeDateRange(start, hasTime = true, notify = true, reminders = emptyList()))!!.notify)
        // E senza la "n" le notifiche non contano.
        assertFalse(parseDateRange("$start||t||600")!!.notify)
    }

    @Test
    fun eachReminderRingsBeforeTheStart() {
        val range = DateRange(millis(concert), millis(concertEnd), hasTime = true, notify = true, reminders = listOf(tenHours, threeDays, twoWeeks))
        val times = dateReminderTimes(range, rome, millis(LocalDateTime.of(2026, 10, 26, 18, 0)), millis(concertEnd.plusDays(1)))
        assertEquals(
            listOf(
                DateReminderTime(millis(LocalDateTime.of(2026, 11, 12, 11, 0)), before = tenHours),
                DateReminderTime(millis(LocalDateTime.of(2026, 11, 9, 21, 0)), before = threeDays),
                DateReminderTime(millis(LocalDateTime.of(2026, 10, 29, 21, 0)), before = twoWeeks)
            ),
            times
        )
        // Senza quella all'inizio, nemmeno quella alla fine.
        assertTrue(times.none { it.isEnd })
    }

    @Test
    fun theStartAndTheEndStayWithTheOthers() {
        val range = DateRange(millis(concert), millis(concertEnd), hasTime = true, notify = true, reminders = listOf(0, threeDays))
        val times = dateReminderTimes(range, rome, millis(LocalDateTime.of(2026, 10, 26, 18, 0)), millis(concertEnd.plusDays(1)))
        assertEquals(
            listOf(millis(concert), millis(LocalDateTime.of(2026, 11, 9, 21, 0)), millis(concertEnd)),
            times.map { it.at }
        )
        assertEquals(listOf(false, false, true), times.map { it.isEnd })
    }

    @Test
    fun daysBeforeAreAtTheSameTimeAcrossTheClockChange() {
        // Lunedì 26 ottobre 2026 alle 10:00; nella notte fra il 24 e il 25 l'ora legale finisce.
        val monday = LocalDateTime.of(2026, 10, 26, 10, 0)
        val range = DateRange(millis(monday), null, hasTime = true, notify = true, reminders = listOf(2 * 24 * 60))
        val times = dateReminderTimes(range, rome, millis(LocalDateTime.of(2026, 10, 1, 0, 0)), millis(monday))
        assertEquals(listOf(millis(LocalDateTime.of(2026, 10, 24, 10, 0))), times.map { it.at })
    }

    @Test
    fun aRepeatingDateRemindsAlsoBeforeTheNextTime() {
        // Ogni venerdì alle 9:00 dal 2 ottobre, due giorni prima: il mercoledì.
        val range = DateRange(
            start = millis(LocalDateTime.of(2026, 10, 2, 9, 0)),
            end = null,
            hasTime = true,
            recurrence = Recurrence(freq = RecurrenceFreq.WEEKLY),
            notify = true,
            reminders = listOf(2 * 24 * 60)
        )
        // Fino a giovedì 22: c'è anche il mercoledì 21, che è per il venerdì 23, fuori dalla finestra.
        val times = dateReminderTimes(range, rome, millis(LocalDateTime.of(2026, 9, 25, 18, 0)), millis(LocalDateTime.of(2026, 10, 22, 0, 0)))
        assertEquals(
            listOf(30 to 9, 7 to 10, 14 to 10, 21 to 10).map { (day, month) -> millis(LocalDateTime.of(2026, month, day, 9, 0)) },
            times.map { it.at }.sorted()
        )
    }

    @Test
    fun theNotificationSaysHowLongIsLeft() {
        val value = encodeDateRange(millis(concert), millis(concertEnd), hasTime = true, notify = true, reminders = listOf(0, threeDays))
        val rows = listOf(DateReminderRow("r1", "c1", value, "IVE", "db1", "Concerti"))
        val words = ReminderWords(untitled = "Untitled", starts = "Inizio", ends = "Fine", startsIn = { ReminderTexts.startsIn(it, AppLanguage.ITALIAN) })
        val fires = dateReminders(rows, rome, millis(LocalDateTime.of(2026, 10, 26, 18, 0)), millis(concertEnd.plusDays(1)), words)
            .sortedBy { it.at }
        assertEquals(listOf("Concerti · Inizia tra 3 giorni", "Concerti · Inizio", "Concerti · Fine"), fires.map { it.body })
        assertTrue(fires.all { it.title == "IVE" })
        // Una chiave per ognuna; quelle all'inizio e alla fine come sempre.
        assertEquals(3, fires.map { it.key }.distinct().size)
        assertEquals("d:r1:c1:${millis(concert)}:s", fires[1].key)
        assertEquals("d:r1:c1:${millis(LocalDateTime.of(2026, 11, 9, 21, 0))}:b4320", fires[0].key)
    }

    @Test
    fun anEventKeepsItsReminders() {
        val row = DatabaseRowEntity(id = "r1", pageId = "db", title = "Concerto")
        val draft = EventDraft(rowId = "r1", title = "Concerto", start = concert, end = concertEnd, notify = true, reminders = listOf(0, tenHours, twoWeeks))
        val range = parseDateRange(draft.cellValue(rome))!!
        assertEquals(listOf(0, tenHours, twoWeeks), range.reminders)
        assertEquals(listOf(0, tenHours, twoWeeks), EventDraft.of(row, range, rome, concert).reminders)
    }

    @Test
    fun theWordsInEveryLanguage() {
        assertEquals(10 to ReminderUnit.HOURS, reminderParts(tenHours))
        assertEquals(3 to ReminderUnit.DAYS, reminderParts(threeDays))
        assertEquals(2 to ReminderUnit.WEEKS, reminderParts(twoWeeks))
        assertEquals(90 to ReminderUnit.MINUTES, reminderParts(90))
        assertEquals("10 ore prima", ReminderTexts.before(tenHours, AppLanguage.ITALIAN))
        assertEquals("3 giorni prima", ReminderTexts.before(threeDays, AppLanguage.ITALIAN))
        assertEquals("2 settimane prima", ReminderTexts.before(twoWeeks, AppLanguage.ITALIAN))
        assertEquals("1 ora prima", ReminderTexts.before(60, AppLanguage.ITALIAN))
        assertEquals("10 hours before", ReminderTexts.before(tenHours, AppLanguage.ENGLISH))
        assertEquals("1 week before", ReminderTexts.before(7 * 24 * 60, AppLanguage.ENGLISH))
        assertEquals("Beginnt in 3 Tagen", ReminderTexts.startsIn(threeDays, AppLanguage.GERMAN))
        assertEquals("3 Tage vorher", ReminderTexts.before(threeDays, AppLanguage.GERMAN))
        assertEquals("提前10小时", ReminderTexts.before(tenHours, AppLanguage.CHINESE))
        assertEquals("10시간 전", ReminderTexts.before(tenHours, AppLanguage.KOREAN))
        assertEquals("2週間前", ReminderTexts.before(twoWeeks, AppLanguage.JAPANESE))
        assertEquals("Inizia tra 10 ore", ReminderTexts.startsIn(tenHours, AppLanguage.ITALIAN))
    }

    @Test
    fun atMostAYearBefore() {
        assertTrue(isValidReminder(52, ReminderUnit.WEEKS))
        assertFalse(isValidReminder(53, ReminderUnit.WEEKS))
        assertFalse(isValidReminder(0, ReminderUnit.HOURS))
        assertTrue(isValidReminder(999, ReminderUnit.MINUTES))
        // Un valore scritto a mano oltre l'anno non suona.
        val start = LocalDate.of(2026, 11, 12).atStartOfDay(rome).toInstant().toEpochMilli()
        assertEquals(listOf(600), parseDateRange("$start||t||600,99999999|n")!!.reminders)
    }
}
