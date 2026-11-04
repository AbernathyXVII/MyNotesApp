package com.gabriele.notionlocal.data.widgets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * **Countdown** (dal 27/10/2026, chiesto dall'utente: "come esiste già in
 * Notion tramite Indify"): quanto manca a una data, in mesi, settimane,
 * giorni, ore, minuti e secondi, ognuno su un foglietto che si sfoglia quando
 * cambia. Una data già passata conta quanto tempo è passato. In un widget ce
 * ne possono essere tanti, uno sotto l'altro ("vorrei che i Countdown si
 * vedessero tutti"): si aggiungono dai tre puntini, e si cambiano tenendoli
 * premuti.
 */
@Serializable
@SerialName("countdown")
data class CountdownWidget(
    override val id: String = newWidgetId(),
    val countdowns: List<CountdownEntry> = emptyList()
) : Widget()

@Serializable
data class CountdownEntry(
    val id: String = newWidgetId(),
    /** Il titolo sopra i foglietti ("Christmas"); vuoto, niente titolo. */
    val name: String = "",
    /** Il giorno e l'ora d'arrivo nel fuso dell'app, al secondo: "2026-12-25T00:00:00". */
    val target: String
) {
    val targetTime: LocalDateTime? get() = runCatching { LocalDateTime.parse(target) }.getOrNull()
}

/**
 * Quanto manca a una data, o quanto è passato ([past]): i mesi interi del
 * calendario, poi quello che resta in settimane, giorni, ore, minuti e
 * secondi. I mesi possono essere più di dodici, come su Indify.
 */
data class CountdownParts(
    val past: Boolean,
    val months: Long,
    val weeks: Long,
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long
)

fun countdownParts(now: LocalDateTime, target: LocalDateTime): CountdownParts {
    val past = target.isBefore(now)
    val from = (if (past) target else now).truncatedTo(ChronoUnit.SECONDS)
    val to = (if (past) now else target).truncatedTo(ChronoUnit.SECONDS)
    // I mesi del calendario: dal 31 gennaio "un mese" arriva al 28 febbraio.
    val months = ChronoUnit.MONTHS.between(from, to)
    var rest = ChronoUnit.SECONDS.between(from.plusMonths(months), to)
    val days = rest / 86_400
    rest %= 86_400
    return CountdownParts(
        past = past,
        months = months,
        weeks = days / 7,
        days = days % 7,
        hours = rest / 3_600,
        minutes = rest % 3_600 / 60,
        seconds = rest % 60
    )
}

/** Il primo countdown di un widget nuovo: il prossimo Natale, a mezzanotte, come nella foto dell'utente. */
fun christmasCountdown(name: String, today: LocalDate): CountdownEntry {
    var christmas = LocalDate.of(today.year, 12, 25)
    if (!christmas.isAfter(today)) christmas = christmas.plusYears(1)
    return CountdownEntry(name = name, target = christmas.atStartOfDay().toString())
}
