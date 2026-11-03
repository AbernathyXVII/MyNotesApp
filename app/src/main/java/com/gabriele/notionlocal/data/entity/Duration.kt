package com.gabriele.notionlocal.data.entity

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * **"Duration"** (dal 29/09/2026): quanto passa fra un inizio e una fine
 * scelti a mano, in anni, mesi, giorni, ore e minuti. Il valore della cella
 * è quello di una data con la fine e l'ora (`encodeDateRange`,
 * "inizio|fine|t"): una colonna Date con un periodo, cambiata in Duration,
 * mostra subito quanto dura, e al contrario torna il periodo.
 */

/** Le unità di una durata, dalla più grande alla più piccola. */
enum class DurationUnit { YEARS, MONTHS, DAYS, HOURS, MINUTES }

/**
 * Fino a quale unità si scrive una durata, dalle impostazioni della
 * proprietà (`DatabaseColumnEntity.durationFormat`): YEARS = solo gli anni,
 * MINUTES = tutto. Il resto si lascia fuori, senza decimali. Null o un nome
 * sconosciuto: tutto, fino ai minuti.
 */
fun durationFormatOf(name: String?): DurationUnit =
    DurationUnit.entries.find { it.name == name } ?: DurationUnit.MINUTES

data class DurationParts(
    val years: Long,
    val months: Long,
    val days: Long,
    val hours: Long,
    val minutes: Long
) {
    fun of(unit: DurationUnit): Long = when (unit) {
        DurationUnit.YEARS -> years
        DurationUnit.MONTHS -> months
        DurationUnit.DAYS -> days
        DurationUnit.HOURS -> hours
        DurationUnit.MINUTES -> minutes
    }
}

/**
 * Anni, mesi, giorni, ore e minuti interi fra `start` ed `end`, **come si
 * contano sul calendario**: dal 12/02/1996 00:00 al 29/09/2026 21:22 sono 30
 * anni (al 12/02/2026), 7 mesi (al 12/09/2026), 17 giorni, 21 ore e 22
 * minuti. Sull'ora dell'orologio, non sui millisecondi: il giorno del cambio
 * dell'ora legale dura un giorno anche se ha 23 o 25 ore.
 *
 * I mesi si contano tutti insieme dall'inizio (e poi si dividono in anni):
 * contando prima gli anni e poi i mesi, un inizio al 29 febbraio o al 31 si
 * accorcerebbe due volte.
 *
 * Null se la fine viene prima dell'inizio.
 */
fun durationBetween(start: LocalDateTime, end: LocalDateTime): DurationParts? {
    if (end.isBefore(start)) return null
    val totalMonths = ChronoUnit.MONTHS.between(start, end)
    var at = start.plusMonths(totalMonths)
    val days = ChronoUnit.DAYS.between(at, end)
    at = at.plusDays(days)
    val hours = ChronoUnit.HOURS.between(at, end)
    at = at.plusHours(hours)
    val minutes = ChronoUnit.MINUTES.between(at, end)
    return DurationParts(totalMonths / 12, totalMonths % 12, days, hours, minutes)
}

/**
 * Le unità da scrivere, dalla più grande fino a `format`. Quelle a zero si
 * saltano ("30 anni e 17 giorni", non "30 anni, 0 mesi e 17 giorni"); se
 * sono tutte a zero resta lo zero dell'ultima ("0 anni").
 */
fun durationUnits(parts: DurationParts, format: DurationUnit): List<Pair<DurationUnit, Long>> =
    DurationUnit.entries
        .filter { it.ordinal <= format.ordinal }
        .map { it to parts.of(it) }
        .filter { it.second > 0 }
        .ifEmpty { listOf(format to 0L) }

/** La durata di una cella "Duration", o null se manca l'inizio o la fine. */
fun durationOfCell(value: String, zone: ZoneId): DurationParts? {
    val range = parseDateRange(value) ?: return null
    val end = range.end ?: return null
    fun local(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
    return durationBetween(local(range.start), local(end))
}

/** Quanto dura una cella, in millisecondi: per ordinare. Null se manca l'inizio o la fine. */
fun durationMillisOfCell(value: String): Long? {
    val range = parseDateRange(value) ?: return null
    return range.end?.let { it - range.start }
}
