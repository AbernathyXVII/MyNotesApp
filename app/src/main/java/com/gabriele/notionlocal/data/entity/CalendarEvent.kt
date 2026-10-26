package com.gabriele.notionlocal.data.entity

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * **Il time-blocking come Google Calendar** (dal 30/09/2026, chiesto
 * dall'utente): nella modalità Day del calendario ogni pagina con un'ora
 * è un blocco alto quanto dura, e toccando un quadratino vuoto della
 * griglia se ne crea una lì, con la finestra rapida di Google Calendar e,
 * da "More options", quella completa con le ripetizioni.
 *
 * Qui c'è la parte che non disegna niente: i colori, l'evento che si sta
 * scrivendo, e dove va ogni blocco nella giornata.
 */

/** Un colore di un evento: i nomi e i colori sono quelli di Google Calendar. */
data class EventColor(val name: String, val hex: String)

/**
 * **Il colore si sceglie evento per evento**, come su Google Calendar (è
 * la scelta dell'utente): si salva sulla riga (`DatabaseRowEntity.eventColor`).
 * Nessun colore = quello di sempre delle barre del calendario.
 */
val EVENT_COLORS = listOf(
    EventColor("Tomato", "#D50000"),
    EventColor("Flamingo", "#E67C73"),
    EventColor("Tangerine", "#F4511E"),
    EventColor("Banana", "#F6BF26"),
    EventColor("Sage", "#33B679"),
    EventColor("Basil", "#0B8043"),
    EventColor("Peacock", "#039BE5"),
    EventColor("Blueberry", "#3F51B5"),
    EventColor("Lavender", "#7986CB"),
    EventColor("Grape", "#8E24AA"),
    EventColor("Graphite", "#616161")
)

/** Un quadratino della griglia: mezz'ora, come su Google Calendar. È anche quanto dura un evento appena creato. */
const val EVENT_SLOT_MINUTES = 30

/** Quanto è corto al minimo un blocco disegnato: più basso non si leggerebbe né si toccherebbe. */
const val EVENT_MIN_VISIBLE_MINUTES = 20

private const val MINUTES_PER_DAY = 24 * 60

/**
 * L'evento che si sta scrivendo nella finestra: uno nuovo (`rowId` null)
 * o una pagina che c'è già ("Edit event").
 *
 * Inizio e fine hanno sempre data e ora; con "All day" le ore non contano
 * e si salvano i giorni soltanto.
 */
data class EventDraft(
    val rowId: String? = null,
    val title: String = "",
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean = false,
    val recurrence: Recurrence? = null,
    val notify: Boolean = false,
    /** "#RRGGBB" di `EVENT_COLORS`, o null per il colore di sempre. */
    val color: String? = null,
    /** Le notifiche, in minuti prima dell'inizio (vedi `DateRange.reminders`). */
    val reminders: List<Int> = listOf(0)
) {
    /** Spostando l'inizio, la fine lo segue e l'evento dura quanto prima, come su Google Calendar. */
    fun withStart(newStart: LocalDateTime): EventDraft =
        copy(start = newStart, end = newStart.plus(Duration.between(start, end).coerceAtLeast(Duration.ZERO)))

    /** Una fine prima dell'inizio non vuol dire niente: si torna a mezz'ora dopo l'inizio. */
    fun withEnd(newEnd: LocalDateTime): EventDraft =
        copy(end = if (allDay) maxOf(newEnd, start) else if (newEnd.isAfter(start)) newEnd else start.plusMinutes(EVENT_SLOT_MINUTES.toLong()))

    /**
     * "All day" acceso o spento. Spegnendolo su un evento che non ha mai
     * avuto ore (tutte e due a mezzanotte), si parte dalle 9:00 per
     * mezz'ora: mezzanotte non la sceglie nessuno.
     */
    fun withAllDay(on: Boolean): EventDraft {
        if (on) return copy(allDay = true, end = maxOf(end, start))
        val atMidnight = start.toLocalTime() == LocalTime.MIDNIGHT && end.toLocalTime() == LocalTime.MIDNIGHT
        if (!atMidnight && end.isAfter(start)) return copy(allDay = false)
        val newStart = start.toLocalDate().atTime(DEFAULT_START)
        val newEnd = end.toLocalDate().atTime(DEFAULT_START).plusMinutes(EVENT_SLOT_MINUTES.toLong())
        return copy(allDay = false, start = newStart, end = maxOf(newEnd, newStart.plusMinutes(EVENT_SLOT_MINUTES.toLong())))
    }

    /** Il valore da scrivere nella cella della data, nello stesso formato di sempre (`encodeDateRange`). */
    fun cellValue(zone: ZoneId): String {
        fun millis(at: LocalDateTime) = at.atZone(zone).toInstant().toEpochMilli()
        return if (allDay) {
            val first = start.toLocalDate()
            val last = end.toLocalDate()
            encodeDateRange(
                start = millis(first.atStartOfDay()),
                end = last.takeIf { it.isAfter(first) }?.let { millis(it.atStartOfDay()) },
                hasTime = false,
                recurrence = recurrence,
                notify = notify,
                reminders = reminders
            )
        } else {
            encodeDateRange(
                start = millis(start),
                end = millis(end),
                hasTime = true,
                recurrence = recurrence,
                notify = notify,
                reminders = reminders
            )
        }
    }

    companion object {
        private val DEFAULT_START: LocalTime = LocalTime.of(9, 0)

        /** Un evento nuovo sul quadratino toccato: mezz'ora, come su Google Calendar. */
        fun at(slot: LocalDateTime): EventDraft =
            EventDraft(start = slot, end = slot.plusMinutes(EVENT_SLOT_MINUTES.toLong()))

        /**
         * Una pagina che c'è già, per "Edit event". Una data con l'ora ma
         * senza fine diventa di mezz'ora; senza data, l'evento parte da
         * `fallback`.
         */
        fun of(row: DatabaseRowEntity, range: DateRange?, zone: ZoneId, fallback: LocalDateTime): EventDraft {
            if (range == null) return at(fallback).copy(rowId = row.id, title = row.title, color = row.eventColor)
            val start = Instant.ofEpochMilli(range.start).atZone(zone).toLocalDateTime()
            val end = range.end?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
            return EventDraft(
                rowId = row.id,
                title = row.title,
                start = start,
                end = end ?: if (range.hasTime) start.plusMinutes(EVENT_SLOT_MINUTES.toLong()) else start,
                allDay = !range.hasTime,
                recurrence = range.recurrence,
                notify = range.notify,
                color = row.eventColor,
                reminders = range.reminders
            )
        }
    }
}

/**
 * Un evento nella griglia di un giorno: da che minuto a che minuto, **di
 * quel giorno** (un evento dalle 23:00 all'1:00 è 23:00–24:00 in uno e
 * 0:00–1:00 nell'altro), e in quale colonna sta quando si accavalla con
 * altri.
 */
data class DayBlock(
    val row: DatabaseRowEntity,
    val range: DateRange,
    val startMinute: Int,
    val endMinute: Int,
    val column: Int = 0,
    val columns: Int = 1
)

/** Inizio e fine di una data con l'ora; senza fine, mezz'ora. */
private fun timedSpan(range: DateRange, zone: ZoneId): Pair<LocalDateTime, LocalDateTime> {
    val start = Instant.ofEpochMilli(range.start).atZone(zone).toLocalDateTime()
    val end = range.end?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
        ?: start.plusMinutes(EVENT_SLOT_MINUTES.toLong())
    return start to maxOf(end, start)
}

/**
 * Se una data sta nella striscia in cima al giorno e non nella griglia:
 * quelle senza ora, e quelle con l'ora lunghe un giorno o più, come fa
 * Google Calendar (un volo di tre giorni non è un blocco di tre giorni).
 */
private fun isAllDay(range: DateRange, zone: ZoneId): Boolean {
    if (!range.hasTime) return true
    val (start, end) = timedSpan(range, zone)
    return Duration.between(start, end).toMinutes() >= MINUTES_PER_DAY
}

/** Le pagine della striscia in cima a `day`: senza ora, o lunghe un giorno o più. */
fun allDayOn(
    day: LocalDate,
    dated: List<Pair<DatabaseRowEntity, DateRange>>,
    zone: ZoneId
): List<Pair<DatabaseRowEntity, DateRange>> = dated.filter { (_, range) ->
    if (!isAllDay(range, zone)) return@filter false
    val first = Instant.ofEpochMilli(range.start).atZone(zone).toLocalDate()
    val lastMoment = range.end?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
    // Una fine a mezzanotte in punto con l'ora non tocca quel giorno.
    val last = when {
        lastMoment == null -> first
        range.hasTime && lastMoment.toLocalTime() == LocalTime.MIDNIGHT -> lastMoment.toLocalDate().minusDays(1)
        else -> lastMoment.toLocalDate()
    }
    !day.isBefore(first) && !day.isAfter(maxOf(first, last))
}

/**
 * I blocchi della griglia di `day`, già messi uno accanto all'altro dove
 * si accavallano (vedi `sideBySide`).
 */
fun dayBlocks(
    day: LocalDate,
    dated: List<Pair<DatabaseRowEntity, DateRange>>,
    zone: ZoneId
): List<DayBlock> {
    val dayStart = day.atStartOfDay()
    val dayEnd = day.plusDays(1).atStartOfDay()
    val blocks = dated.mapNotNull { (row, range) ->
        if (isAllDay(range, zone)) return@mapNotNull null
        val (start, end) = timedSpan(range, zone)
        // Un evento di durata zero (inizio e fine uguali) sta comunque nel suo giorno.
        if (!start.isBefore(dayEnd) || (end.isAfter(start) && !end.isAfter(dayStart))) return@mapNotNull null
        if (end == start && start.isBefore(dayStart)) return@mapNotNull null
        val from = maxOf(start, dayStart)
        val to = minOf(end, dayEnd)
        DayBlock(
            row = row,
            range = range,
            startMinute = Duration.between(dayStart, from).toMinutes().toInt(),
            endMinute = Duration.between(dayStart, to).toMinutes().toInt()
        )
    }
    return sideBySide(blocks)
}

/**
 * **Gli eventi che si accavallano stanno affiancati**, come su Google
 * Calendar: ogni gruppo di eventi che si toccano si divide la larghezza in
 * colonne, e ognuno va nella prima colonna libera. Gli eventi più corti di
 * `EVENT_MIN_VISIBLE_MINUTES` contano per quanto sono disegnati, sennò due
 * da cinque minuti uno dopo l'altro finirebbero uno sopra l'altro.
 *
 * L'ordine è fisso — inizio, poi il più lungo, poi il nome — così due
 * eventi non si scambiano di posto ad ogni ridisegno.
 */
fun sideBySide(blocks: List<DayBlock>): List<DayBlock> {
    fun shownEnd(block: DayBlock) = maxOf(block.endMinute, block.startMinute + EVENT_MIN_VISIBLE_MINUTES)
    val sorted = blocks.sortedWith(
        compareBy<DayBlock> { it.startMinute }
            .thenByDescending { it.endMinute }
            .thenBy { it.row.title }
            .thenBy { it.row.id }
    )
    val result = mutableListOf<DayBlock>()
    var group = mutableListOf<DayBlock>()
    var columnEnds = mutableListOf<Int>()
    var groupEnd = -1

    fun closeGroup() {
        result += group.map { it.copy(columns = columnEnds.size) }
        group = mutableListOf()
        columnEnds = mutableListOf()
    }

    sorted.forEach { block ->
        if (group.isNotEmpty() && block.startMinute >= groupEnd) closeGroup()
        val free = columnEnds.indexOfFirst { it <= block.startMinute }
        val column = if (free >= 0) free else columnEnds.size.also { columnEnds += 0 }
        columnEnds[column] = shownEnd(block)
        group += block.copy(column = column)
        groupEnd = maxOf(if (group.size == 1) 0 else groupEnd, shownEnd(block))
    }
    if (group.isNotEmpty()) closeGroup()
    return result
}

/** Il quadratino di mezz'ora sotto il dito: `minute` è il punto toccato, dall'inizio del giorno. */
fun slotStartMinute(minute: Float): Int {
    val slot = (minute / EVENT_SLOT_MINUTES).toInt().coerceIn(0, MINUTES_PER_DAY / EVENT_SLOT_MINUTES - 1)
    return slot * EVENT_SLOT_MINUTES
}
