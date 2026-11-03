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
    val bars: List<ProgressBarSetting> = defaultProgressBars(),
    /**
     * **Il decimale** sulle percentuali (dal 29/09/2026, "Show decimals" nel
     * menu del widget): "Anno: 74,6%" invece di "Anno: 74%".
     */
    val decimals: Boolean = false
) : Widget()

/**
 * La percentuale di una barra come si scrive accanto: per difetto, così il
 * 100% arriva solo alla fine; col decimale, uno solo, col separatore delle
 * impostazioni (74,6 o 74.6).
 */
fun progressPercentText(fraction: Double, decimals: Boolean, formatNumber: (String) -> String): String {
    if (!decimals) return "${kotlin.math.floor(fraction * 100).toInt()}%"
    val tenths = kotlin.math.floor(fraction * 1000).toLong()
    return formatNumber("${tenths / 10}.${tenths % 10}") + "%"
}

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
    val name: String = "",
    /**
     * **I suoni** (dal 18/10/2026, chiesti dall'utente): un click su +, uno
     * più lieve su −, da spegnere o cambiare (vedi `CounterSound`). I
     * contatori di prima non hanno queste chiavi: suoni accesi, quelli di
     * partenza.
     */
    val soundsOn: Boolean = true,
    val plusSound: String = CounterSound.CLICK.name,
    val minusSound: String = CounterSound.SOFT_CLICK.name,
    /**
     * **Il volume dei click**, da 0 a 100 (dal 18/10/2026, chiesto
     * dall'utente): 100 è il volume "Sistema" del telefono. I contatori di
     * prima suonano a 100, come prima.
     */
    val volume: Int = 100
) : Widget()

enum class PomodoroPhase { SESSION, BREAK }

/**
 * **Qualcosa è finito** in un widget, e va detto con una notifica (dal
 * 25/09/2026): un timer, o una fase del pomodoro. Vedi `Notifier`.
 */
sealed class WidgetEvent {
    data class TimerDone(val timerId: String, val durationMs: Long) : WidgetEvent()

    /** `nowIn`: la fase appena cominciata; `nextMinutes`: quanto dura. */
    data class PomodoroPhaseEnded(
        val widgetId: String,
        val sessionName: String,
        val nowIn: PomodoroPhase,
        val nextMinutes: Int
    ) : WidgetEvent()
}

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
    /**
     * **Quanto dura la sessione di studio, scritto nel riquadro grande**
     * (dal 28/09/2026): la sua voce non è più nell'ingranaggio, e il tempo
     * scritto toccando il timer durante la sessione vale anche per le
     * sessioni dopo, secondi compresi. Null = `sessionMinutes`, per i
     * pomodori di prima.
     */
    val sessionMs: Long? = null,
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
     * **"Break sound"**: il suono di quando comincia la pausa, scelto fra le
     * suonerie del telefono dall'ingranaggio. Fino al 28/09/2026 era il
     * suono unico di fine fase, e si salva ancora col suo nome di allora.
     * Null = il suono delle notifiche dell'app. Il nome è quello da far
     * leggere, perché dall'indirizzo non si capisce.
     */
    @SerialName("soundUri") val breakSoundUri: String? = null,
    @SerialName("soundName") val breakSoundName: String? = null,
    /** **"Study sound"** (dal 28/09/2026): il suono di quando ricomincia la sessione di studio. */
    val studySoundUri: String? = null,
    val studySoundName: String? = null
) : Widget() {
    fun lengthOf(phase: PomodoroPhase): Long = when (phase) {
        PomodoroPhase.SESSION -> sessionMs ?: (sessionMinutes * MINUTE_MS)
        PomodoroPhase.BREAK -> breakMinutes * MINUTE_MS
    }

    /** Il suono di quando comincia `phase`: la pausa col suo, lo studio col suo. */
    fun soundFor(phase: PomodoroPhase): String? = when (phase) {
        PomodoroPhase.SESSION -> studySoundUri
        PomodoroPhase.BREAK -> breakSoundUri
    }

    /**
     * Il tempo scritto toccando il timer: vale per la fase in corso, che corra
     * o no. Durante la sessione di studio è anche **la durata delle sessioni
     * dopo** (vedi `sessionMs`).
     */
    fun withTimeWritten(ms: Long, now: Long): PomodoroWidget {
        val left = ms.coerceAtLeast(0)
        val lengths = if (phase == PomodoroPhase.SESSION && left > 0) copy(sessionMs = left) else this
        return if (running) lengths.copy(endsAt = now + left, remainingMs = left) else lengths.copy(remainingMs = left)
    }

    /**
     * Il tempo che manca adesso. Mentre corre, **mai più di quello con cui
     * è partito** (`remainingMs`): nell'istante in cui si preme ▶ l'ora in
     * mano alla schermata è ancora quella di prima, indietro fino a quasi
     * un secondo — un minuto appena avviato si leggeva "01:01" per un
     * attimo (visto sul telefono il 24/09/2026).
     */
    fun remainingAt(now: Long): Long =
        if (running && endsAt != null) (endsAt - now).coerceAtMost(remainingMs).coerceAtLeast(0) else remainingMs
}

const val MINUTE_MS = 60_000L

/**
 * **Cronometro** (dal 25/09/2026, chiesto dall'utente "uguale a quello di
 * Samsung"): Start, e il tempo corre; Stop lo ferma, Resume lo riprende da
 * dov'era, Reset lo azzera; Lap segna un giro, e sotto c'è la lista dei
 * giri col tempo del giro e quello totale.
 *
 * Come il pomodoro **si conta sull'orologio**: si salva quando è partita
 * la corsa in corso e quanto era già accumulato prima, e il tempo si ricava
 * da lì — resta giusto con la barra chiusa e ad app chiusa.
 */
@Serializable
@SerialName("stopwatch")
data class StopwatchWidget(
    override val id: String = newWidgetId(),
    /** Il tempo delle corse già finite, cioè fino all'ultimo Stop. */
    val accumulatedMs: Long = 0,
    /** Quando è partita la corsa in corso; null a cronometro fermo. */
    val startedAt: Long? = null,
    /** Il tempo totale segnato a ogni Lap, dal primo all'ultimo. */
    val laps: List<Long> = emptyList()
) : Widget() {
    val running: Boolean get() = startedAt != null

    /** Mai partito, o azzerato: i pulsanti sono Lap (spento) e Start. */
    val idle: Boolean get() = !running && accumulatedMs == 0L && laps.isEmpty()

    fun elapsedAt(now: Long): Long = accumulatedMs + (startedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)
}

/**
 * **Timer** (dal 25/09/2026, come quello di Samsung): uno o più conti alla
 * rovescia, uno per pagina, che si sfogliano in orizzontale. Ognuno si
 * imposta con tre rotelle — ore da 0 a 99, minuti e secondi da 0 a 59 — e
 * parte con Start.
 */
@Serializable
@SerialName("timer")
data class TimerWidget(
    override val id: String = newWidgetId(),
    val timers: List<CountdownTimer> = listOf(CountdownTimer())
) : Widget()

/**
 * Un conto alla rovescia. Quattro stati: **da impostare** (le rotelle,
 * `started` falso), **in corsa**, **in pausa**, **finito** (0:00 in rosso
 * finché non lo si riporta alle rotelle).
 */
@Serializable
data class CountdownTimer(
    val id: String = newWidgetId(),
    /** Il tempo scelto con le rotelle: quello con cui parte e a cui torna. */
    val durationMs: Long = 10 * MINUTE_MS,
    val started: Boolean = false,
    val running: Boolean = false,
    val finished: Boolean = false,
    /** A timer fermo, quanto manca; mentre corre, quanto mancava quando è ripartito. */
    val remainingMs: Long = durationMs,
    /** Mentre corre: quando finisce. */
    val endsAt: Long? = null,
    /**
     * La sua notifica nella tendina (vedi `TimerNotifications`) tolta con
     * uno scorrimento: non torna finché non lo si fa ripartire, o non lo si
     * mette in pausa o lo si riprende. Dal 30/09/2026.
     */
    val notificationDismissed: Boolean = false
) {
    /** Quanto manca adesso: mai più di quanto mancava alla partenza, come il pomodoro. */
    fun remainingAt(now: Long): Long = when {
        finished -> 0
        running && endsAt != null -> (endsAt - now).coerceAtMost(remainingMs).coerceAtLeast(0)
        else -> remainingMs
    }
}

/** Il tempo delle tre rotelle, in millisecondi. */
fun wheelDurationMs(hours: Int, minutes: Int, seconds: Int): Long =
    ((hours * 60L + minutes) * 60L + seconds) * 1_000L

/** Le tre rotelle di un tempo: ore (fino a 99), minuti, secondi. */
fun wheelValues(durationMs: Long): Triple<Int, Int, Int> {
    val totalSeconds = durationMs / 1_000
    return Triple((totalSeconds / 3_600).toInt().coerceAtMost(99), ((totalSeconds / 60) % 60).toInt(), (totalSeconds % 60).toInt())
}

// --- Calcoli, senza Android: si provano coi test ---

// Cronometro

fun StopwatchWidget.started(now: Long): StopwatchWidget = if (running) this else copy(startedAt = now)

fun StopwatchWidget.stopped(now: Long): StopwatchWidget =
    if (!running) this else copy(accumulatedMs = elapsedAt(now), startedAt = null)

fun StopwatchWidget.lapped(now: Long): StopwatchWidget = if (!running) this else copy(laps = laps + elapsedAt(now))

fun StopwatchWidget.cleared(): StopwatchWidget = copy(accumulatedMs = 0, startedAt = null, laps = emptyList())

/** Una riga della lista dei giri: il numero, il tempo del giro, il tempo totale. */
data class LapRow(val number: Int, val lapMs: Long, val overallMs: Long)

/** I giri come li mostra la lista: **dal più recente**, come su Samsung. */
fun lapRows(laps: List<Long>): List<LapRow> =
    laps.mapIndexed { i, overall -> LapRow(i + 1, overall - (laps.getOrNull(i - 1) ?: 0L), overall) }.reversed()

/**
 * Il giro più veloce e quello più lento, per colorarli come su Samsung
 * (arancio il più veloce, rosso il più lento). Solo da tre giri in su: con
 * due, sarebbero colorati tutti e due e non direbbe niente.
 */
fun fastestAndSlowestLap(rows: List<LapRow>): Pair<Int, Int>? {
    if (rows.size < 3) return null
    return rows.minBy { it.lapMs }.number to rows.maxBy { it.lapMs }.number
}

/** "00:32.52" (minuti, secondi, centesimi); oltre l'ora "1:02:03.45". */
fun stopwatchText(ms: Long): String {
    val centis = (ms / 10) % 100
    val totalSeconds = ms / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    val tail = "%02d:%02d.%02d".format(minutes, seconds, centis)
    return if (hours > 0) "$hours:$tail" else tail
}

// Timer

fun CountdownTimer.startedAt(now: Long): CountdownTimer =
    copy(started = true, running = true, finished = false, remainingMs = durationMs, endsAt = now + durationMs, notificationDismissed = false)

fun CountdownTimer.pausedAt(now: Long): CountdownTimer =
    if (!running) this else copy(running = false, remainingMs = remainingAt(now), endsAt = null, notificationDismissed = false)

fun CountdownTimer.resumedAt(now: Long): CountdownTimer =
    if (running || finished || !started) this else copy(running = true, endsAt = now + remainingMs, notificationDismissed = false)

/** Di nuovo alle rotelle, col tempo che c'era. */
/**
 * Il widget senza un timer. **Il primo non si toglie mai**, e con lui il
 * widget resta sempre con almeno un timer: si eliminano solo quelli
 * aggiunti col "+", dal secondo in poi (chiesto dall'utente il 25/09/2026).
 */
fun TimerWidget.withoutTimer(timerId: String): TimerWidget =
    if (timers.firstOrNull()?.id == timerId) this else copy(timers = timers.filterNot { it.id == timerId })

fun CountdownTimer.backToSetup(): CountdownTimer =
    copy(started = false, running = false, finished = false, remainingMs = durationMs, endsAt = null, notificationDismissed = false)

/** Se è arrivato a zero, finito; con quando è finito, per decidere se suonare. */
fun CountdownTimer.finishedBy(now: Long): Pair<CountdownTimer, Long?> {
    val end = endsAt
    if (!running || end == null || end > now) return this to null
    return copy(running = false, finished = true, remainingMs = 0, endsAt = null, notificationDismissed = false) to end
}

/**
 * **Ha la sua notifica nella tendina** (dal 30/09/2026): partito e non
 * finito, che corra o sia in pausa, e non tolto a mano.
 */
val CountdownTimer.inNotification: Boolean get() = started && !finished && !notificationDismissed

/** I timer che hanno la loro notifica, per id: con il widget che li contiene. */
fun WidgetsState.notifiedTimers(): Map<String, Pair<String, CountdownTimer>> =
    widgets.filterIsInstance<TimerWidget>()
        .flatMap { w -> w.timers.filter { it.inNotification }.map { it.id to (w.id to it) } }
        .toMap()

/** "9:55", "0:05", "1:09:55": arrotondato per eccesso come il pomodoro. */
fun countdownText(ms: Long): String {
    val totalSeconds = (ms + 999) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** Il tempo impostato, in piccolo sopra il timer come su Samsung: "10 m", "1 h 30 m", "45 s". */
fun durationLabel(ms: Long): String {
    val (h, m, s) = wheelValues(ms)
    return listOfNotNull(h.takeIf { it > 0 }?.let { "$it h" }, m.takeIf { it > 0 }?.let { "$it m" }, s.takeIf { it > 0 }?.let { "$it s" })
        .joinToString(" ").ifEmpty { "0 s" }
}

/**
 * Fra quanti millisecondi cambia la cifra di un timer a cui mancano
 * `leftMs`: il timer si legge arrotondato per eccesso ("01:00" finché
 * manca più di 59 secondi), quindi cambia quando mancano esattamente dei
 * secondi interi. Da 60 000 è un secondo pieno, da 59 600 sono 600.
 */
fun msUntilTimerTicks(leftMs: Long): Long = (leftMs - 1) % 1_000 + 1

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
