package com.gabriele.notionlocal.data.widgets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * I widget della barra laterale, come si salvano.
 *
 * Stanno nelle preferenze del telefono (vedi `WidgetStore`) e non nel
 * database delle note, per la stessa ragione delle impostazioni: sono
 * attrezzi di chi usa il telefono, non contenuto. Nel database finirebbero
 * dentro ogni backup e ogni copia di pagina.
 *
 * Chiesti dall'utente il 24/09/2026: fusi orari (orologi ed elenco),
 * barre dell'avanzamento, contatore, pomodoro — e un interruttore per
 * nasconderli tutti.
 */
@Serializable
data class WidgetsState(
    /** La sezione dei widget chiusa col suo interruttore: resta chiusa anche riaprendo l'app. */
    val hidden: Boolean = false,
    val widgets: List<Widget> = emptyList()
)

internal fun newWidgetId(): String = UUID.randomUUID().toString()

@Serializable
sealed class Widget {
    abstract val id: String
}

/**
 * **Fusi orari**: gli orologi (analogici o digitali, uno per fuso) e sotto
 * l'elenco dei fusi con la loro ora, sempre ordinato da ovest a est.
 */
@Serializable
@SerialName("time_zones")
data class TimeZonesWidget(
    override val id: String = newWidgetId(),
    val clocks: List<ClockSetting> = emptyList(),
    /** Gli id dei fusi dell'elenco ("Europe/Rome"), nell'ordine in cui sono stati aggiunti: si ordinano quando si mostrano. */
    val list: List<String> = emptyList()
) : Widget()

@Serializable
data class ClockSetting(
    val id: String = newWidgetId(),
    val zoneId: String,
    val analog: Boolean = true
)

/** **Avanzamento**: quanto è passato dell'anno, del mese, della settimana, del giorno, e delle date dell'utente. */
@Serializable
@SerialName("life_progress")
data class LifeProgressWidget(
    override val id: String = newWidgetId(),
    val bars: List<ProgressBarSetting> = defaultProgressBars()
) : Widget()

enum class ProgressKind { YEAR, MONTH, WEEK, DAY, CUSTOM }

@Serializable
data class ProgressBarSetting(
    val id: String = newWidgetId(),
    val kind: ProgressKind,
    /** Solo per le date dell'utente: il nome scritto accanto alla barra. */
    val name: String = "",
    /** Solo per le date dell'utente: da quando si conta, e il giorno a cui si arriva (`LocalDate.toEpochDay`). */
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
    val colorHex: String = DEFAULT_BAR_COLOR
)

/** Il verde della foto dell'utente. */
const val DEFAULT_BAR_COLOR = "#2E9E3E"

fun defaultProgressBars(): List<ProgressBarSetting> = listOf(
    ProgressBarSetting(kind = ProgressKind.YEAR),
    ProgressBarSetting(kind = ProgressKind.MONTH),
    ProgressBarSetting(kind = ProgressKind.WEEK),
    ProgressBarSetting(kind = ProgressKind.DAY)
)

/** **Contatore**: un numero, più uno, meno uno, e si azzera. */
@Serializable
@SerialName("counter")
data class CounterWidget(
    override val id: String = newWidgetId(),
    val value: Int = 0,
    /** Facoltativo: cosa si sta contando, scritto sopra al numero. */
    val name: String = ""
) : Widget()

enum class PomodoroPhase { SESSION, BREAK }

/**
 * **Pomodoro**: una sessione di studio e una pausa, a turno.
 *
 * **Si conta sull'orologio, non a colpi di secondo**: quando corre si
 * salva *quando* finirà la fase (`endsAt`), e il tempo che manca si
 * ricava da lì ogni volta. Così resta giusto con la barra laterale
 * chiusa, con l'app in secondo piano o chiusa e riaperta: nessun
 * conteggio può restare indietro, perché non c'è un conteggio.
 */
@Serializable
@SerialName("pomodoro")
data class PomodoroWidget(
    override val id: String = newWidgetId(),
    /** Il nome della sessione di studio; vuoto = "Sessione". */
    val sessionName: String = "",
    val sessionMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val phase: PomodoroPhase = PomodoroPhase.SESSION,
    val running: Boolean = false,
    /**
     * Il tempo che manca, a timer fermo; a timer che corre, quello che
     * mancava quando è partito (vedi `remainingAt`).
     */
    val remainingMs: Long = 25 * MINUTE_MS,
    /** Quando finirà la fase, a timer che corre. */
    val endsAt: Long? = null,
    /**
     * Il suono di fine fase **di questo pomodoro**, scelto fra le suonerie
     * del telefono dall'ingranaggio (dal 25/09/2026, chiesto dall'utente).
     * Null = il suono delle notifiche dell'app, com'era prima. Il nome è
     * quello da far leggere, perché dall'indirizzo non si capisce.
     */
    val soundUri: String? = null,
    val soundName: String? = null
) : Widget() {
    fun lengthOf(phase: PomodoroPhase): Long = when (phase) {
        PomodoroPhase.SESSION -> sessionMinutes * MINUTE_MS
        PomodoroPhase.BREAK -> breakMinutes * MINUTE_MS
    }

    /**
     * Il tempo che manca adesso. Mentre corre, **mai più di quello con cui
     * è partito** (`remainingMs`): l'ora che la barra laterale passa qui si
     * rinfresca allo scoccare di ogni secondo, e subito dopo ▶ può essere
     * indietro di quasi un secondo — un minuto appena avviato si leggeva
     * "01:01" per un attimo (visto sul telefono il 24/09/2026).
     */
    fun remainingAt(now: Long): Long =
        if (running && endsAt != null) (endsAt - now).coerceAtMost(remainingMs).coerceAtLeast(0) else remainingMs
}

const val MINUTE_MS = 60_000L

// --- Calcoli, senza Android: si provano coi test ---

/**
 * Il turno successivo di un pomodoro che corre, se la fase è finita.
 *
 * Finita la sessione comincia la pausa, e finita la pausa di nuovo la
 * sessione, **senza fermarsi**, come i timer "25 + 5". Se ne sono passate
 * più d'una — l'app era chiusa — si recupera fino a quella in corso
 * adesso, contando dal momento esatto in cui ognuna è finita e non da
 * quando ce ne si accorge: altrimenti il ritardo si accumulerebbe.
 *
 * Restituisce il widget aggiornato e **quando è finita l'ultima fase**
 * (null se non ne è finita nessuna): chi chiama suona solo se è appena
 * successo.
 */
fun PomodoroWidget.advancedTo(now: Long): Pair<PomodoroWidget, Long?> {
    if (!running || endsAt == null || endsAt > now) return this to null
    var phase = phase
    var ends: Long = endsAt
    var lastEnded: Long = endsAt
    while (ends <= now) {
        lastEnded = ends
        phase = if (phase == PomodoroPhase.SESSION) PomodoroPhase.BREAK else PomodoroPhase.SESSION
        // Una durata a zero farebbe girare il ciclo all'infinito.
        ends += lengthOf(phase).coerceAtLeast(MINUTE_MS)
    }
    return copy(phase = phase, endsAt = ends, remainingMs = ends - now) to lastEnded
}

/**
 * Quanto è passato, fra 0 e 1, di una barra dell'avanzamento, in un fuso.
 *
 * La settimana comincia **il lunedì**, come in Italia e nello standard ISO.
 * Una data dell'utente si conta dal suo inizio (a mezzanotte) al suo
 * giorno di arrivo (a mezzanotte, cioè quando quel giorno comincia): a
 * quel punto la barra è piena. Null se una data dell'utente non ha i suoi
 * due giorni.
 */
fun ProgressBarSetting.fractionAt(now: Instant, zone: ZoneId): Double? {
    val here = now.atZone(zone)
    val today = here.toLocalDate()
    val (from, to) = when (kind) {
        ProgressKind.YEAR -> today.withDayOfYear(1).let { it to it.plusYears(1) }
        ProgressKind.MONTH -> today.withDayOfMonth(1).let { it to it.plusMonths(1) }
        ProgressKind.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusWeeks(1) }
        ProgressKind.DAY -> today to today.plusDays(1)
        ProgressKind.CUSTOM -> {
            val start = startEpochDay ?: return null
            val end = endEpochDay ?: return null
            LocalDate.ofEpochDay(start) to LocalDate.ofEpochDay(end)
        }
    }
    return fractionBetween(from.atStartOfDay(zone), to.atStartOfDay(zone), here)
}

private fun fractionBetween(from: ZonedDateTime, to: ZonedDateTime, now: ZonedDateTime): Double {
    val total = to.toInstant().toEpochMilli() - from.toInstant().toEpochMilli()
    if (total <= 0) return if (now.isBefore(to)) 0.0 else 1.0
    val done = now.toInstant().toEpochMilli() - from.toInstant().toEpochMilli()
    return (done.toDouble() / total).coerceIn(0.0, 1.0)
}

/**
 * I fusi dell'elenco **da ovest a est**: dal più indietro rispetto a
 * Greenwich al più avanti, come l'ha chiesto l'utente ("SEMPRE ordinati").
 * Si ordina sull'ora **di adesso**, perché l'ora legale sposta i fusi:
 * New York e Londra non hanno sempre la stessa distanza. A parità di ora,
 * in ordine di nome.
 */
fun sortedWestToEast(zoneIds: List<String>, now: Instant): List<String> =
    zoneIds
        .mapNotNull { id -> runCatching { ZoneId.of(id) }.getOrNull()?.let { id to it } }
        .sortedWith(
            compareBy<Pair<String, ZoneId>> { (_, zone) -> zone.rules.getOffset(now).totalSeconds }
                .thenBy { (id, _) -> cityName(id) }
        )
        .map { it.first }

/** Il nome di città di un fuso: "America/Los_Angeles" → "Los Angeles". */
fun cityName(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')

/** La parte del mondo di un fuso: "America/Los_Angeles" → "America". */
fun regionName(zoneId: String): String = zoneId.substringBefore('/').replace('_', ' ')

/** "GMT+2", "GMT-3:30", "GMT": la distanza da Greenwich adesso. */
fun gmtLabel(zone: ZoneId, now: Instant): String {
    val seconds = zone.rules.getOffset(now).totalSeconds
    if (seconds == 0) return "GMT"
    val sign = if (seconds < 0) "-" else "+"
    val abs = kotlin.math.abs(seconds)
    val hours = abs / 3600
    val minutes = (abs % 3600) / 60
    return if (minutes == 0) "GMT$sign$hours" else "GMT$sign$hours:${minutes.toString().padStart(2, '0')}"
}

/**
 * I fusi fra cui scegliere: quelli con un nome di città, delle regioni del
 * mondo. Fuori gli alias vecchi ("US/Pacific", "Etc/GMT+5", "SystemV/…"),
 * che sarebbero doppioni di un fuso che c'è già col nome di una città.
 */
fun selectableZoneIds(): List<String> {
    val regions = setOf("Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")
    return ZoneId.getAvailableZoneIds().filter { it.substringBefore('/') in regions && it.contains('/') }
}
