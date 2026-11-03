package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * **Il time-blocking come Google Calendar** (30/09/2026): dove va ogni
 * evento nella griglia del giorno, come si affiancano quelli che si
 * accavallano, e l'evento che si scrive nella finestra.
 */
class CalendarEventTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val day = LocalDate.of(2026, 10, 11)

    private fun millis(at: LocalDateTime) = at.atZone(zone).toInstant().toEpochMilli()
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = day) = date.atTime(hour, minute)

    private fun row(title: String) = DatabaseRowEntity(pageId = "db", title = title)

    private fun timed(title: String, start: LocalDateTime, end: LocalDateTime?) =
        row(title) to DateRange(start = millis(start), end = end?.let { millis(it) }, hasTime = true)

    private fun untimed(title: String, first: LocalDate, last: LocalDate? = null) =
        row(title) to DateRange(start = millis(first.atStartOfDay()), end = last?.let { millis(it.atStartOfDay()) })

    @Test
    fun aBlockGoesFromItsStartToItsEnd() {
        val blocks = dayBlocks(day, listOf(timed("Corsetta", at(9), at(10))), zone)
        assertEquals(1, blocks.size)
        assertEquals(9 * 60, blocks[0].startMinute)
        assertEquals(10 * 60, blocks[0].endMinute)
        assertEquals(0, blocks[0].column)
        assertEquals(1, blocks[0].columns)
    }

    @Test
    fun anEventAcrossMidnightIsCutOnEachDay() {
        // Dormire, dalle 23 di sabato alle 8 di domenica.
        val sleep = timed("Dormire", at(23, date = day.minusDays(1)), at(8))
        val sunday = dayBlocks(day, listOf(sleep), zone).single()
        assertEquals(0, sunday.startMinute)
        assertEquals(8 * 60, sunday.endMinute)
        val saturday = dayBlocks(day.minusDays(1), listOf(sleep), zone).single()
        assertEquals(23 * 60, saturday.startMinute)
        assertEquals(24 * 60, saturday.endMinute)
        // Il giorno dopo non c'è.
        assertTrue(dayBlocks(day.plusDays(1), listOf(sleep), zone).isEmpty())
    }

    @Test
    fun anEventEndingAtMidnightDoesNotSpillIntoTheNextDay() {
        val late = timed("Film", at(22), at(0, date = day.plusDays(1)))
        assertEquals(24 * 60, dayBlocks(day, listOf(late), zone).single().endMinute)
        assertTrue(dayBlocks(day.plusDays(1), listOf(late), zone).isEmpty())
    }

    @Test
    fun aTimeWithoutAnEndIsHalfAnHour() {
        val block = dayBlocks(day, listOf(timed("Chiamata", at(15), null)), zone).single()
        assertEquals(15 * 60, block.startMinute)
        assertEquals(15 * 60 + EVENT_SLOT_MINUTES, block.endMinute)
    }

    @Test
    fun overlappingEventsStandSideBySide() {
        val blocks = dayBlocks(
            day,
            listOf(
                timed("Coreano", at(10), at(13)),
                timed("Pranzo", at(12), at(13)),
                timed("Studio", at(12, 30), at(14)),
                // Dopo le 14 il gruppo è finito: torna largo tutto.
                timed("Palestra", at(14), at(15))
            ),
            zone
        ).associateBy { it.row.title }
        assertEquals(0, blocks.getValue("Coreano").column)
        assertEquals(1, blocks.getValue("Pranzo").column)
        assertEquals(2, blocks.getValue("Studio").column)
        listOf("Coreano", "Pranzo", "Studio").forEach { assertEquals(it, 3, blocks.getValue(it).columns) }
        assertEquals(0, blocks.getValue("Palestra").column)
        assertEquals(1, blocks.getValue("Palestra").columns)
    }

    @Test
    fun aFreedColumnIsUsedAgain() {
        val blocks = dayBlocks(
            day,
            listOf(
                timed("Lungo", at(9), at(12)),
                timed("Primo", at(9), at(10)),
                timed("Secondo", at(10), at(11))
            ),
            zone
        ).associateBy { it.row.title }
        // "Secondo" comincia quando "Primo" finisce: stessa colonna, due in tutto.
        assertEquals(0, blocks.getValue("Lungo").column)
        assertEquals(1, blocks.getValue("Primo").column)
        assertEquals(1, blocks.getValue("Secondo").column)
        assertEquals(2, blocks.getValue("Secondo").columns)
    }

    @Test
    fun veryShortEventsCountForHowTheyAreDrawn() {
        // Due da cinque minuti, uno dopo l'altro: disegnati alti venti minuti
        // si toccherebbero, quindi stanno affiancati.
        val blocks = dayBlocks(
            day,
            listOf(timed("A", at(9), at(9, 5)), timed("B", at(9, 5), at(9, 10))),
            zone
        )
        assertEquals(setOf(0, 1), blocks.map { it.column }.toSet())
    }

    @Test
    fun untimedAndLongEventsGoInTheStripOnTop() {
        val dated = listOf(
            untimed("Compleanno", day),
            untimed("Vacanza", day.minusDays(2), day.plusDays(1)),
            timed("Volo", at(8, date = day.minusDays(1)), at(20)),
            timed("Corsetta", at(9), at(10)),
            untimed("Ieri", day.minusDays(1))
        )
        assertEquals(listOf("Compleanno", "Vacanza", "Volo"), allDayOn(day, dated, zone).map { it.first.title })
        assertEquals(listOf("Corsetta"), dayBlocks(day, dated, zone).map { it.row.title })
    }

    @Test
    fun theTappedSlotIsItsHalfHour() {
        assertEquals(0, slotStartMinute(0f))
        assertEquals(0, slotStartMinute(29.9f))
        assertEquals(30, slotStartMinute(30f))
        assertEquals(9 * 60 + 30, slotStartMinute(9 * 60 + 47f))
        // Fuori dalla griglia si resta dentro.
        assertEquals(23 * 60 + 30, slotStartMinute(24 * 60 + 5f))
        assertEquals(0, slotStartMinute(-3f))
    }

    @Test
    fun aNewEventIsHalfAnHourOnTheSlot() {
        val draft = EventDraft.at(at(0))
        assertEquals(at(0), draft.start)
        assertEquals(at(0, 30), draft.end)
        assertFalse(draft.allDay)
        assertNull(draft.recurrence)
        assertNull(draft.color)
    }

    @Test
    fun movingTheStartKeepsTheLength() {
        val draft = EventDraft.at(at(9)).withEnd(at(10, 30)).withStart(at(14))
        assertEquals(at(14), draft.start)
        assertEquals(at(15, 30), draft.end)
    }

    @Test
    fun anEndBeforeTheStartIsNotAccepted() {
        val draft = EventDraft.at(at(9)).withEnd(at(8))
        assertEquals(at(9, 30), draft.end)
    }

    @Test
    fun allDayOnAndOffAgain() {
        val allDay = EventDraft.at(at(10)).withAllDay(true)
        assertTrue(allDay.allDay)
        // Le ore restano, per quando lo si spegne.
        assertEquals(at(10), allDay.withAllDay(false).start)
        assertEquals(at(10, 30), allDay.withAllDay(false).end)
        // Una data che non ha mai avuto ore riparte dalle 9.
        val fromDay = EventDraft(start = day.atStartOfDay(), end = day.atStartOfDay(), allDay = true).withAllDay(false)
        assertEquals(at(9), fromDay.start)
        assertEquals(at(9, 30), fromDay.end)
    }

    @Test
    fun theCellValueIsTheUsualDateFormat() {
        val weekly = Recurrence(RecurrenceFreq.WEEKLY, weekdays = setOf(DayOfWeek.SUNDAY))
        val draft = EventDraft.at(at(9)).withEnd(at(10)).copy(recurrence = weekly, notify = true)
        val range = parseDateRange(draft.cellValue(zone))!!
        assertEquals(millis(at(9)), range.start)
        assertEquals(millis(at(10)), range.end)
        assertTrue(range.hasTime)
        assertEquals(weekly, range.recurrence)
        assertTrue(range.notify)

        // Tutto il giorno: solo il giorno, e la fine solo se è un altro.
        val oneDay = parseDateRange(draft.withAllDay(true).copy(recurrence = null, notify = false).cellValue(zone))!!
        assertEquals(millis(day.atStartOfDay()), oneDay.start)
        assertNull(oneDay.end)
        assertFalse(oneDay.hasTime)
        val twoDays = EventDraft(start = at(9), end = at(9, date = day.plusDays(1)), allDay = true).cellValue(zone)
        assertEquals(millis(day.plusDays(1).atStartOfDay()), parseDateRange(twoDays)!!.end)
    }

    @Test
    fun anExistingPageBecomesTheEventToEdit() {
        val page = row("Coreano").copy(eventColor = "#039BE5")
        val range = DateRange(start = millis(at(10)), end = millis(at(13)), hasTime = true)
        val draft = EventDraft.of(page, range, zone, fallback = at(9))
        assertEquals(page.id, draft.rowId)
        assertEquals("Coreano", draft.title)
        assertEquals(at(10), draft.start)
        assertEquals(at(13), draft.end)
        assertEquals("#039BE5", draft.color)
        // Salvata senza toccare niente, la cella resta uguale.
        assertEquals(encodeDateRange(range.start, range.end, hasTime = true), draft.cellValue(zone))

        // Una pagina senza data parte dal giorno di riserva.
        val undated = EventDraft.of(row("Senza data"), null, zone, fallback = at(9))
        assertEquals(at(9), undated.start)
        assertEquals(at(9, 30), undated.end)
    }

    @Test
    fun theColorsAreGoogleCalendars() {
        assertEquals(11, EVENT_COLORS.size)
        assertEquals("Tomato", EVENT_COLORS.first().name)
        assertEquals("Graphite", EVENT_COLORS.last().name)
        assertEquals(EVENT_COLORS.size, EVENT_COLORS.map { it.hex }.toSet().size)
    }
}
