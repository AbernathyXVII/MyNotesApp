package com.gabriele.notionlocal.data.entity

/**
 * **Le notifiche in anticipo di una data, come su Google Calendar** (dal
 * 26/10/2026, chiesto dall'utente: "la notifica dieci ore prima, tre giorni
 * prima, due settimane prima"). Ognuna è un numero di minuti prima
 * dell'inizio (`DateRange.reminders`); qui come si dice e quali si
 * propongono.
 */

/** L'unità di una notifica in anticipo, quelle di Google Calendar: quanti minuti vale. */
enum class ReminderUnit(val minutes: Int) { MINUTES(1), HOURS(60), DAYS(24 * 60), WEEKS(7 * 24 * 60) }

/**
 * [minutes] prima, nell'unità più grande che li divide senza resto: 600 →
 * 10 ore, 4320 → 3 giorni, 20160 → 2 settimane, 90 → 90 minuti.
 */
fun reminderParts(minutes: Int): Pair<Int, ReminderUnit> {
    if (minutes <= 0) return 0 to ReminderUnit.MINUTES
    val unit = ReminderUnit.entries.last { minutes % it.minutes == 0 }
    return minutes / unit.minutes to unit
}

/**
 * Quelle che si scelgono con un tocco, come su Google Calendar: all'inizio,
 * 10 e 30 minuti prima, un'ora, un giorno, una settimana. Le altre da
 * "Custom…".
 */
val REMINDER_PRESETS = listOf(0, 10, 30, 60, 24 * 60, 7 * 24 * 60)

/**
 * Al massimo un anno prima: le ripetizioni si contano fino a dove possono
 * avere una notifica, e senza un limite sarebbero troppe.
 */
const val REMINDER_MAX_MINUTES = 52 * 7 * 24 * 60

/** Se [amount] volte [unit] si può scegliere in "Custom…": almeno uno, al massimo un anno. */
fun isValidReminder(amount: Int, unit: ReminderUnit): Boolean =
    amount >= 1 && amount.toLong() * unit.minutes <= REMINDER_MAX_MINUTES
