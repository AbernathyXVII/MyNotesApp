package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Le ripetizioni: "ogni sabato", "l'ultima domenica del mese" senza dover
 * partire da un'ultima domenica, e fino al 2100.
 */
class RecurrenceTest {

    // Sabato 26 settembre 2026: il giorno in cui l'utente l'ha chiesto.
    private val today = LocalDate.of(2026, 9, 26)

    @Test
    fun everySaturdayFromToday() {
        val rule = Recurrence(RecurrenceFreq.WEEKLY, weekdays = setOf(DayOfWeek.SATURDAY))
        val october = rule.occurrences(today, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertEquals(listOf(3, 10, 17, 24, 31), october.map { it.dayOfMonth })
    }

    @Test
    fun theLastSundayOfTheMonthWithoutStartingOnOne() {
        val rule = Recurrence(
            RecurrenceFreq.MONTHLY,
            monthlyMode = MonthlyMode.NTH_WEEKDAY,
            nthWeek = Recurrence.LAST_WEEK,
            nthWeekday = DayOfWeek.SUNDAY
        )
        val days = rule.occurrences(today, today, LocalDate.of(2026, 12, 31))
        // 27 settembre, 25 ottobre, 29 novembre (il mese ha cinque domeniche), 27 dicembre.
        assertEquals(
            listOf(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 29), LocalDate.of(2026, 12, 27)),
            days
        )
    }

    @Test
    fun aChosenDayOfTheMonthFallsOnTheLastDayOfShortMonths() {
        val rule = Recurrence(RecurrenceFreq.MONTHLY, monthDay = 31)
        val days = rule.occurrences(today, today, LocalDate.of(2027, 2, 28))
        assertEquals(LocalDate.of(2026, 9, 30), days.first())
        assertEquals(LocalDate.of(2027, 2, 28), days.last())
    }

    @Test
    fun untilTheYear2100AndNotAfter() {
        val rule = Recurrence(RecurrenceFreq.DAILY, end = RecurrenceEnd.OnDate(Recurrence.END_2100.toEpochDay()))
        val lastDays = rule.occurrences(today, LocalDate.of(2100, 12, 30), LocalDate.of(2101, 1, 2))
        assertEquals(listOf(LocalDate.of(2100, 12, 30), LocalDate.of(2100, 12, 31)), lastDays)
        // Ogni giorno per settantaquattro anni: si salta dritti alla finestra.
        assertEquals(1, rule.occurrences(today, LocalDate.of(2099, 6, 1), LocalDate.of(2099, 6, 1)).size)
    }

    @Test
    fun theChosenFieldsAreSavedAndOldRulesReadTheSame() {
        val rule = Recurrence(
            RecurrenceFreq.MONTHLY,
            monthlyMode = MonthlyMode.NTH_WEEKDAY,
            nthWeek = Recurrence.LAST_WEEK,
            nthWeekday = DayOfWeek.SUNDAY,
            end = RecurrenceEnd.OnDate(Recurrence.END_2100.toEpochDay())
        )
        assertEquals(rule, RecurrenceCodec.decode(RecurrenceCodec.encode(rule)))
        val old = RecurrenceCodec.decode("FREQ=MONTHLY;INT=1;MODE=NTH_WEEKDAY")!!
        assertNull(old.nthWeek)
        assertNull(old.nthWeekday)
        assertTrue(old.occurrences(today, today, today.plusMonths(1)).isNotEmpty())
    }
}
