package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.DateRange
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.plainText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * **Le notifiche dell'app** (dal 25/09/2026): da dove vengono e quando
 * suonano. Vengono da due posti:
 *
 *  - le righe **"/notification"** (`BlockType.NOTIFICATION`): suonano a
 *    `notifyAt`, col testo della riga — o, se è vuota, col nome della pagina;
 *  - le date dei database con **"Include notification"** acceso
 *    (`DateRange.notify`): suonano all'inizio e alla fine (se c'è), all'ora
 *    della data, a ogni ripetizione se si ripete, col nome della pagina della
 *    riga. Dal 16/10/2026 la notifica ha sempre un'ora scelta; le 9:00 valgono
 *    solo per le date salvate prima con la notifica e senza ora.
 *
 * Qui dentro solo il calcolo, senza Android: chi le programma e chi le
 * manda è `NoteAlarms`, chi le mostra `Notifier`.
 */

/** Una notifica da mandare: quando, cosa dire, e cosa aprire toccandola. */
data class ReminderFire(
    /** Unica per notifica e per momento: due ripetizioni della stessa data sono due notifiche. */
    val key: String,
    val at: Long,
    val title: String,
    val body: String,
    /** La pagina da aprire: quella della riga "/notification". */
    val pageId: String? = null,
    /** La riga di database, per le date: si apre la sua pagina. */
    val rowId: String? = null,
    /** Il database della riga: si apre questo se la riga non ha una pagina (database semplice). */
    val databaseId: String? = null
)

/** Una riga "/notification" col titolo della sua pagina, come la legge `BlockDao.getNotificationRows`. */
data class NotificationBlockRow(
    val blockId: String,
    val pageId: String,
    val textJson: String,
    val notifyAt: Long,
    val pageTitle: String
)

/** Una cella data con "Include notification", come la legge `DatabaseDao.getNotifyingDateCells`. */
data class DateReminderRow(
    val rowId: String,
    val columnId: String,
    val value: String,
    val rowTitle: String,
    val databaseId: String,
    val databaseTitle: String
)

/** Le parole della notifica di una data che ha inizio e fine. */
data class ReminderWords(val untitled: String, val starts: String, val ends: String)

/**
 * Il momento esatto in cui suona una data: all'ora, se la data ce l'ha,
 * altrimenti alle 9:00 di quel giorno (nel fuso dell'app).
 */
fun notifyInstant(millis: Long, hasTime: Boolean, zone: ZoneId): Long {
    if (hasTime) return millis
    val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return LocalDateTime.of(day, DEFAULT_NOTIFY_TIME).atZone(zone).toInstant().toEpochMilli()
}

/**
 * I momenti in cui suona una data fra `from` (escluso) e `to` (compreso):
 * l'inizio e, se c'è, la fine (`true` = fine). Una data che si ripete suona
 * a ogni ripetizione, e la fine di ogni ripetizione cade tanti giorni dopo
 * quanti ne separano la fine dall'inizio della prima.
 */
fun dateReminderTimes(range: DateRange, zone: ZoneId, from: Long, to: Long): List<Pair<Long, Boolean>> {
    if (!range.notify || to <= from) return emptyList()
    val start = Instant.ofEpochMilli(range.start).atZone(zone)
    val end = range.end?.let { Instant.ofEpochMilli(it).atZone(zone) }
    val startTime = if (range.hasTime) start.toLocalTime() else DEFAULT_NOTIFY_TIME
    val endTime = if (range.hasTime) end?.toLocalTime() else DEFAULT_NOTIFY_TIME
    val spanDays = end?.let { ChronoUnit.DAYS.between(start.toLocalDate(), it.toLocalDate()) }

    val rule = range.recurrence
    val startDates: List<LocalDate> = if (rule == null) {
        listOf(start.toLocalDate())
    } else {
        // Anche le ripetizioni partite prima di `from` possono avere la
        // fine dopo: la finestra si allarga all'indietro di quanto dura.
        val fromDay = Instant.ofEpochMilli(from).atZone(zone).toLocalDate().minusDays((spanDays ?: 0) + 1)
        val toDay = Instant.ofEpochMilli(to).atZone(zone).toLocalDate()
        rule.occurrences(start.toLocalDate(), fromDay, toDay)
    }

    val result = mutableListOf<Pair<Long, Boolean>>()
    for (day in startDates) {
        result += LocalDateTime.of(day, startTime).atZone(zone).toInstant().toEpochMilli() to false
        if (spanDays != null && endTime != null) {
            result += LocalDateTime.of(day.plusDays(spanDays), endTime).atZone(zone).toInstant().toEpochMilli() to true
        }
    }
    // Inizio e fine nello stesso istante (un giorno solo, senza ora): una notifica sola.
    return result.filter { (at, _) -> at > from && at <= to }.distinctBy { it.first }
}

private val json = Json { ignoreUnknownKeys = true }

/** Le notifiche delle righe "/notification" fra `from` (escluso) e `to` (compreso). */
fun blockReminders(rows: List<NotificationBlockRow>, from: Long, to: Long, untitled: String): List<ReminderFire> =
    rows.filter { it.notifyAt in (from + 1)..to }.map { row ->
        val text = runCatching { json.decodeFromString<List<RichTextSpan>>(row.textJson).plainText() }
            .getOrDefault("").trim()
        val pageTitle = row.pageTitle.ifBlank { untitled }
        ReminderFire(
            key = "b:${row.blockId}:${row.notifyAt}",
            at = row.notifyAt,
            // Il testo scritto; se non c'è, il nome della pagina.
            title = text.ifEmpty { pageTitle },
            body = if (text.isEmpty()) "" else pageTitle,
            pageId = row.pageId
        )
    }

/** Le notifiche delle date dei database fra `from` (escluso) e `to` (compreso). */
fun dateReminders(rows: List<DateReminderRow>, zone: ZoneId, from: Long, to: Long, words: ReminderWords): List<ReminderFire> =
    rows.flatMap { row ->
        val range = parseDateRange(row.value) ?: return@flatMap emptyList()
        dateReminderTimes(range, zone, from, to).map { (at, isEnd) ->
            val database = row.databaseTitle.ifBlank { words.untitled }
            ReminderFire(
                key = "d:${row.rowId}:${row.columnId}:$at:${if (isEnd) "e" else "s"}",
                at = at,
                // Il nome della pagina della riga: "Revive +".
                title = row.rowTitle.ifBlank { words.untitled },
                body = when {
                    range.end == null -> database
                    isEnd -> "$database · ${words.ends}"
                    else -> "$database · ${words.starts}"
                },
                rowId = row.rowId,
                databaseId = row.databaseId
            )
        }
    }

/** Tutte le notifiche fra `from` (escluso) e `to` (compreso), dal database vero. */
suspend fun loadReminders(db: AppDatabase, zone: ZoneId, from: Long, to: Long, words: ReminderWords): List<ReminderFire> =
    blockReminders(db.blockDao().getNotificationRows(), from, to, words.untitled) +
        dateReminders(db.databaseDao().getNotifyingDateCells(), zone, from, to, words)
