package com.gabriele.notionlocal.data.entity

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Ogni quanto si ripete una cosa. Sono le stesse quattro di Google
 * Calendar: più in là di "ogni anno" non serve niente, e meno di "ogni
 * giorno" non esiste senza tirare dentro le ore.
 */
enum class RecurrenceFreq { DAILY, WEEKLY, MONTHLY, YEARLY }

/**
 * Come si ripete una cosa mese per mese: "il 21 di ogni mese" oppure
 * "il terzo lunedì di ogni mese".
 *
 * Sono due regole diverse e non intercambiabili — il 21 cade in un
 * giorno della settimana sempre diverso, il terzo lunedì cambia data
 * ogni mese — e sceglierne una al posto dell'utente sarebbe indovinare.
 */
enum class MonthlyMode { DAY_OF_MONTH, NTH_WEEKDAY }

/** Quando smette di ripetersi. */
sealed class RecurrenceEnd {
    object Never : RecurrenceEnd()

    /** Fino a questo giorno compreso. */
    data class OnDate(val epochDay: Long) : RecurrenceEnd()

    /** Dopo tante volte in tutto, contando anche la prima. */
    data class AfterCount(val count: Int) : RecurrenceEnd()
}

/**
 * La regola di ripetizione di una data.
 *
 * **Non genera righe nel database.** Una pagina che si ripete resta
 * una pagina sola con una regola attaccata: le occorrenze le calcola
 * il calendario quando disegna, e spariscono quando la regola cambia.
 * L'alternativa — creare davvero una pagina per ogni ripetizione —
 * riempirebbe il database di copie e renderebbe impossibile
 * correggere "tutte le volte" con una modifica sola.
 */
data class Recurrence(
    val freq: RecurrenceFreq,
    /** Ogni quante unità: 1 = ogni settimana, 3 = ogni tre settimane. */
    val interval: Int = 1,
    /** Solo per WEEKLY: in quali giorni. Vuoto = il giorno della data di partenza. */
    val weekdays: Set<DayOfWeek> = emptySet(),
    /** Solo per MONTHLY. */
    val monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
    val end: RecurrenceEnd = RecurrenceEnd.Never
) {
    /**
     * I giorni in cui la cosa capita dentro la finestra chiesta,
     * partendo da `start`.
     *
     * La finestra è obbligatoria di proposito: una regola senza fine
     * genererebbe occorrenze all'infinito, e chi disegna un mese ha
     * bisogno solo di quel mese.
     */
    fun occurrences(start: LocalDate, from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to.isBefore(start)) return emptyList()
        val step = interval.coerceAtLeast(1)
        val limitDate = (end as? RecurrenceEnd.OnDate)?.let { LocalDate.ofEpochDay(it.epochDay) }
        val maxCount = (end as? RecurrenceEnd.AfterCount)?.count?.coerceAtLeast(1)

        val result = mutableListOf<LocalDate>()
        var produced = 0
        // Tetto di sicurezza: una regola sbagliata non deve poter
        // bloccare il disegno del calendario in un ciclo infinito.
        var guard = 0

        fun offer(day: LocalDate): Boolean {
            if (limitDate != null && day.isAfter(limitDate)) return false
            if (maxCount != null && produced >= maxCount) return false
            produced++
            if (!day.isBefore(from) && !day.isAfter(to)) result.add(day)
            return true
        }

        when (freq) {
            RecurrenceFreq.DAILY -> {
                var day = start
                while (!day.isAfter(to) && guard++ < MAX_STEPS) {
                    if (!offer(day)) break
                    day = day.plusDays(step.toLong())
                }
            }

            RecurrenceFreq.WEEKLY -> {
                val days = weekdays.ifEmpty { setOf(start.dayOfWeek) }
                // Si procede settimana per settimana dal lunedì della
                // settimana di partenza, così "ogni 3 settimane" conta
                // le settimane e non i giorni.
                var weekStart = start.minusDays((start.dayOfWeek.value - 1).toLong())
                outer@ while (!weekStart.isAfter(to) && guard++ < MAX_STEPS) {
                    for (i in 0..6) {
                        val day = weekStart.plusDays(i.toLong())
                        if (day.isBefore(start)) continue
                        if (day.dayOfWeek !in days) continue
                        if (!offer(day)) break@outer
                    }
                    weekStart = weekStart.plusWeeks(step.toLong())
                }
            }

            RecurrenceFreq.MONTHLY -> {
                var month = start.withDayOfMonth(1)
                val nth = ((start.dayOfMonth - 1) / 7) + 1
                val isLast = start.plusWeeks(1).month != start.month
                while (!month.isAfter(to) && guard++ < MAX_STEPS) {
                    val day = when (monthlyMode) {
                        MonthlyMode.DAY_OF_MONTH ->
                            // I mesi corti non hanno il 31: lì la cosa
                            // capita l'ultimo giorno, che è quello che
                            // ci si aspetta da "ogni mese il 31".
                            month.withDayOfMonth(
                                start.dayOfMonth.coerceAtMost(month.lengthOfMonth())
                            )
                        MonthlyMode.NTH_WEEKDAY -> nthWeekdayOf(month, start.dayOfWeek, nth, isLast)
                    }
                    if (!day.isBefore(start)) {
                        if (!offer(day)) break
                    }
                    month = month.plusMonths(step.toLong())
                }
            }

            RecurrenceFreq.YEARLY -> {
                var day = start
                while (!day.isAfter(to) && guard++ < MAX_STEPS) {
                    if (!offer(day)) break
                    day = day.plusYears(step.toLong())
                }
            }
        }
        return result
    }

    private companion object {
        const val MAX_STEPS = 10_000
    }
}

/**
 * L'ennesimo `weekday` del mese, o l'ultimo se `last` è vero.
 *
 * "L'ultimo" non è "il quinto": certi mesi hanno quattro lunedì e
 * certi cinque, e chi dice "l'ultima domenica del mese" intende
 * sempre l'ultima, non la quinta quando c'è.
 */
private fun nthWeekdayOf(
    monthStart: LocalDate,
    weekday: DayOfWeek,
    nth: Int,
    last: Boolean
): LocalDate {
    if (last) {
        var day = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        while (day.dayOfWeek != weekday) day = day.minusDays(1)
        return day
    }
    var day = monthStart
    while (day.dayOfWeek != weekday) day = day.plusDays(1)
    val candidate = day.plusWeeks((nth - 1).toLong())
    // Se quel mese non arriva all'ennesimo (il quinto martedì non
    // esiste sempre), si prende l'ultimo che c'è.
    return if (candidate.month == monthStart.month) {
        candidate
    } else {
        candidate.minusWeeks(1)
    }
}

/**
 * La regola scritta come testo da salvare accanto alla data, e
 * riletta.
 *
 * Formato piatto a coppie `chiave=valore` separate da `;`: si legge a
 * occhio aprendo il database, e aggiungere un campo domani non rompe
 * quello che c'è già — le chiavi sconosciute vengono ignorate e quelle
 * mancanti hanno un valore di riserva.
 */
object RecurrenceCodec {

    fun encode(rule: Recurrence): String {
        val parts = mutableListOf(
            "FREQ=${rule.freq.name}",
            "INT=${rule.interval}"
        )
        if (rule.weekdays.isNotEmpty()) {
            parts += "DAYS=" + rule.weekdays.sortedBy { it.value }.joinToString(",") { it.name }
        }
        if (rule.freq == RecurrenceFreq.MONTHLY) {
            parts += "MODE=${rule.monthlyMode.name}"
        }
        when (val end = rule.end) {
            is RecurrenceEnd.OnDate -> parts += "UNTIL=${end.epochDay}"
            is RecurrenceEnd.AfterCount -> parts += "COUNT=${end.count}"
            RecurrenceEnd.Never -> Unit
        }
        return parts.joinToString(";")
    }

    fun decode(text: String): Recurrence? {
        if (text.isBlank()) return null
        val fields = text.split(";")
            .mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
            }
            .toMap()
        val freq = fields["FREQ"]?.let { name ->
            RecurrenceFreq.entries.find { it.name == name }
        } ?: return null
        return Recurrence(
            freq = freq,
            interval = fields["INT"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            weekdays = fields["DAYS"].orEmpty()
                .split(",")
                .mapNotNull { name -> DayOfWeek.entries.find { it.name == name } }
                .toSet(),
            monthlyMode = fields["MODE"]?.let { name ->
                MonthlyMode.entries.find { it.name == name }
            } ?: MonthlyMode.DAY_OF_MONTH,
            end = when {
                fields["UNTIL"] != null ->
                    fields["UNTIL"]?.toLongOrNull()
                        ?.let { RecurrenceEnd.OnDate(it) }
                        ?: RecurrenceEnd.Never
                fields["COUNT"] != null ->
                    fields["COUNT"]?.toIntOrNull()
                        ?.let { RecurrenceEnd.AfterCount(it) }
                        ?: RecurrenceEnd.Never
                else -> RecurrenceEnd.Never
            }
        )
    }
}

/**
 * Quanti giorni dura una ripetizione, per riportarsi dietro la durata
 * dell'originale: un impegno di tre giorni che si ripete ogni mese
 * dura tre giorni tutte le volte.
 */
fun daysBetweenInclusive(start: LocalDate, end: LocalDate): Long =
    ChronoUnit.DAYS.between(start, end)
