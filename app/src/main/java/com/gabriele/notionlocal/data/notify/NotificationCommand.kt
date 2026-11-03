package com.gabriele.notionlocal.data.notify

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Quello che si è scritto dopo "/notification": quando, e cosa dire.
 * `text` vuoto = la notifica mostra il nome della pagina.
 */
data class NotificationCommand(val at: LocalDateTime, val text: String)

/** L'ora di una notifica a cui non si è data un'ora: le 9 di mattina (chiesto dall'utente). */
val DEFAULT_NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)

/**
 * **La riga "/notification"** (dal 25/09/2026), come l'ha descritta
 * l'utente: "/notification 22.10.2026 19:41 Buttare l'immondizia".
 *
 *  - la data: `22.10.2026`, `22/10/2026`, `22-10-26`, `2026-10-22`, o
 *    senza l'anno (`22.10`: quest'anno, o il prossimo se è già passata);
 *  - l'ora, dopo la data: `19:41` o `19.41`. **Senza, alle 9:00.** Un'ora
 *    senza data vuol dire oggi, o domani se oggi è già passata;
 *  - senza niente, le prossime 9:00;
 *  - il resto è il testo della notifica; fra parentesi, le parentesi si
 *    tolgono ("(Buttare l'immondizia)" → "Buttare l'immondizia").
 *
 * Vale anche "/notifica", all'italiana. Null se la riga non è un comando.
 */
object NotificationCommands {

    private val COMMAND = Regex("""^\s*/(notification|notifica)(?=\s|$)(.*)$""", RegexOption.IGNORE_CASE)
    private val DAY_MONTH = Regex("""^(\d{1,2})[./-](\d{1,2})(?:[./-](\d{2}|\d{4}))?$""")
    private val ISO = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""")
    private val TIME = Regex("""^(\d{1,2})[:.](\d{2})$""")

    fun parse(line: String, now: LocalDateTime): NotificationCommand? {
        val match = COMMAND.find(line) ?: return null
        var rest = match.groupValues[2].trim()

        var date: LocalDate? = null
        var dateHadYear = true
        var time: LocalTime? = null

        firstToken(rest)?.let { token ->
            val parsed = parseDate(token, now.toLocalDate())
            if (parsed != null) {
                date = parsed.first
                dateHadYear = parsed.second
                rest = rest.removePrefix(token).trimStart()
            }
        }
        firstToken(rest)?.let { token ->
            val parsed = parseTime(token)
            if (parsed != null) {
                time = parsed
                rest = rest.removePrefix(token).trimStart()
            }
        }

        val at = when {
            date != null -> {
                var at = LocalDateTime.of(date, time ?: DEFAULT_NOTIFY_TIME)
                // "22.10" senza l'anno: se quest'anno è già passato, il prossimo.
                if (!dateHadYear && at.isBefore(now)) at = at.plusYears(1)
                at
            }
            time != null -> {
                val today = LocalDateTime.of(now.toLocalDate(), time)
                if (today.isAfter(now)) today else today.plusDays(1)
            }
            else -> nextDefault(now)
        }
        return NotificationCommand(at, stripParentheses(rest.trim()))
    }

    /** Le prossime 9:00: oggi, se non sono ancora passate, se no domani. */
    fun nextDefault(now: LocalDateTime): LocalDateTime {
        val today = LocalDateTime.of(now.toLocalDate(), DEFAULT_NOTIFY_TIME)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun firstToken(text: String): String? =
        text.takeIf { it.isNotEmpty() }?.split(Regex("\\s+"), limit = 2)?.firstOrNull()?.takeIf { it.isNotEmpty() }

    /** La data e se aveva l'anno, o null se non è una data (vera: niente 31 febbraio). */
    private fun parseDate(token: String, today: LocalDate): Pair<LocalDate, Boolean>? {
        ISO.matchEntire(token)?.let { m ->
            val (y, mo, d) = m.destructured
            return runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()?.let { it to true }
        }
        val m = DAY_MONTH.matchEntire(token) ?: return null
        val day = m.groupValues[1].toInt()
        val month = m.groupValues[2].toInt()
        val yearText = m.groupValues[3]
        val year = when {
            yearText.isEmpty() -> today.year
            yearText.length == 2 -> 2000 + yearText.toInt()
            else -> yearText.toInt()
        }
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let { it to yearText.isNotEmpty() }
    }

    private fun parseTime(token: String): LocalTime? {
        val m = TIME.matchEntire(token) ?: return null
        val hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toInt()
        if (hour > 23 || minute > 59) return null
        return LocalTime.of(hour, minute)
    }

    private fun stripParentheses(text: String): String =
        if (text.length >= 2 && text.startsWith("(") && text.endsWith(")")) text.substring(1, text.length - 1).trim() else text
}
