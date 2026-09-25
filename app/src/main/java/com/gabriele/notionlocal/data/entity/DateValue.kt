package com.gabriele.notionlocal.data.entity

/**
 * Il valore di una cella data: un inizio, una fine facoltativa, se
 * l'ora conta e la regola di ripetizione.
 *
 * `hasTime` sta scritto nel valore e non si indovina dai millisecondi.
 * Indovinarlo vorrebbe dire dire "se non cade a mezzanotte allora c'è
 * un'ora", e una pagina messa davvero a mezzanotte perderebbe la sua
 * ora — o peggio, si vedrebbe comparire "00:00" dal nulla su tutte le
 * date senza ora.
 *
 * Sta qui e non nella schermata perché il formato non serve più solo a
 * disegnare: anche i filtri devono leggere una data per sapere se una
 * riga passa, e due copie della stessa lettura si sarebbero separate
 * al primo campo aggiunto.
 */
data class DateRange(
    val start: Long,
    val end: Long?,
    val hasTime: Boolean = false,
    /** La regola di ripetizione, se questa data si ripete. */
    val recurrence: Recurrence? = null,
    /**
     * **"Include notification"** (dal 25/09/2026): all'inizio (e alla
     * fine, se c'è) arriva una notifica col nome della pagina. Senza ora,
     * alle 9:00. Vedi `data/notify/Reminders.kt`.
     */
    val notify: Boolean = false
)

/** Il terzo campo del valore, quando l'ora conta. */
const val DATE_TIME_FLAG = "t"

/** Il quinto campo del valore, quando la data manda una notifica. */
const val DATE_NOTIFY_FLAG = "n"

/**
 * Legge il valore di una cella data.
 *
 * I campi sono nati uno alla volta e mancano nei valori scritti prima:
 * ognuno ha quindi un significato di riserva che coincide con com'era
 * il mondo quando quel campo non esisteva.
 */
fun parseDateRange(value: String): DateRange? {
    if (value.isBlank()) return null
    val parts = value.split(MULTI_VALUE_SEPARATOR)
    val start = parts.getOrNull(0)?.toLongOrNull() ?: return null
    val end = parts.getOrNull(1)?.toLongOrNull()
    return DateRange(
        start = start,
        // Una fine che precede l'inizio non vuol dire niente: la si
        // ignora invece di disegnare un intervallo al contrario.
        end = end?.takeIf { it >= start },
        hasTime = parts.getOrNull(2) == DATE_TIME_FLAG,
        recurrence = parts.getOrNull(3)?.let { RecurrenceCodec.decode(it) },
        notify = parts.getOrNull(4) == DATE_NOTIFY_FLAG
    )
}

fun encodeDateRange(
    start: Long,
    end: Long? = null,
    hasTime: Boolean = false,
    recurrence: Recurrence? = null,
    notify: Boolean = false
): String {
    if (!hasTime && end == null && recurrence == null && !notify) return start.toString()
    val endPart = end?.toString().orEmpty()
    val timePart = if (hasTime) DATE_TIME_FLAG else ""
    val base = "$start$MULTI_VALUE_SEPARATOR$endPart"
    val rule = recurrence?.let { RecurrenceCodec.encode(it) }.orEmpty()
    return when {
        // Il segno della notifica sta in fondo, dopo la ripetizione (anche
        // vuota): i valori scritti prima, che non l'hanno, restano validi.
        notify -> "$base$MULTI_VALUE_SEPARATOR$timePart$MULTI_VALUE_SEPARATOR$rule$MULTI_VALUE_SEPARATOR$DATE_NOTIFY_FLAG"
        recurrence != null -> "$base$MULTI_VALUE_SEPARATOR$timePart$MULTI_VALUE_SEPARATOR$rule"
        hasTime -> "$base$MULTI_VALUE_SEPARATOR$timePart"
        else -> base
    }
}
