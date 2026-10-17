package com.gabriele.notionlocal.data.widgets

import android.content.Context
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import com.gabriele.notionlocal.data.notify.NoteAlarms
import com.gabriele.notionlocal.data.notify.Notifier
import com.gabriele.notionlocal.data.notify.TimerNotifications
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneId

/**
 * **I widget della barra laterale**: cosa c'è, e cosa contiene.
 *
 * Come `AppSettings`: un oggetto solo, osservabile (la barra si ridisegna
 * da sola quando qualcosa cambia) e salvato nelle preferenze del telefono
 * a ogni modifica, in JSON. Il numero del contatore, i fusi, i colori delle
 * barre, e un pomodoro a metà restano anche chiudendo l'app.
 *
 * **Il pomodoro suona anche con la barra chiusa**: non è la barra a
 * contare, ma un'attesa di questo oggetto, che vive quanto l'app e dorme
 * fino alla fine della fase (`scheduleAlarm`). **E dal 25/09/2026 anche ad
 * app chiusa**: la fine di timer e pomodori mette anche la sveglia di
 * Android (`NoteAlarms`), che quando suona fa partire l'app e passa di qui
 * (`onAlarm`). Ogni fine manda una notifica (`Notifier`) e suona.
 *
 * **I timer partiti stanno anche nella tendina** (dal 30/09/2026), col
 * tempo che scorre: a ogni modifica `update` passa il prima e il dopo a
 * `TimerNotifications`, che mette, cambia e toglie le loro notifiche.
 *
 * `init` va chiamato all'avvio, dopo `AppSettings.init`.
 */
object WidgetStore {

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateHolder = mutableStateOf(WidgetsState())
    val state: WidgetsState get() = stateHolder.value

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var alarm: Job? = null
    private var lastSystemAlarmEnd: Long? = null

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        load(context)
        // Le fasi finite mentre l'app era chiusa: si recuperano in
        // silenzio, senza suonare per qualcosa di già passato.
        advancePomodoros(ringIfRecent = false)
    }

    /**
     * Le notifiche dei timer partiti, se non ci sono più: il telefono le
     * toglie riaccendendosi o aggiornando l'app, ma i timer intanto
     * corrono. All'avvio dell'app e dopo un riavvio (non a ogni `init`: un
     * pulsante della notifica che fa partire l'app non deve rimettere
     * quella appena tolta).
     */
    fun restoreTimerNotifications() {
        if (::appContext.isInitialized) TimerNotifications.restore(appContext, stateHolder.value)
    }

    private fun load(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("widgets", Context.MODE_PRIVATE)
        stateHolder.value = prefs.getString(KEY_STATE, null)
            ?.let { runCatching { json.decodeFromString<WidgetsState>(it) }.getOrNull() }
            ?: WidgetsState()
    }

    /**
     * **La sveglia di Android è suonata** (vedi `AlarmReceiver`): si fa
     * avanzare quello che è finito, con la sua notifica. Se l'app era
     * chiusa lo stato si carica ora, **senza** il recupero silenzioso di
     * `init`: è proprio la fine appena arrivata che deve suonare. La
     * finestra è più larga (`ALARM_RING_WINDOW_MS`): la sveglia può
     * arrivare con qualche secondo di ritardo, e l'app deve anche partire.
     * Se l'app era aperta e l'attesa interna è arrivata prima, qui non c'è
     * più niente da fare: la stessa fine non suona due volte.
     */
    fun onAlarm(context: Context) {
        if (!::prefs.isInitialized) load(context)
        advancePomodoros(ringIfRecent = true, window = ALARM_RING_WINDOW_MS)
    }

    /** La prima fine fra pomodori e timer che corrono, per la sveglia di Android. */
    fun nextEnd(): Long? {
        val widgets = stateHolder.value.widgets
        val pomodoroEnds = widgets.filterIsInstance<PomodoroWidget>().filter { it.running }.mapNotNull { it.endsAt }
        val timerEnds = widgets.filterIsInstance<TimerWidget>().flatMap { it.timers }.filter { it.running }.mapNotNull { it.endsAt }
        return (pomodoroEnds + timerEnds).minOrNull()
    }

    private fun update(transform: (WidgetsState) -> WidgetsState) {
        val previous = stateHolder.value
        val next = transform(previous)
        if (next == previous) return
        stateHolder.value = next
        if (::prefs.isInitialized) prefs.edit().putString(KEY_STATE, json.encodeToString(next)).apply()
        if (::appContext.isInitialized) TimerNotifications.sync(appContext, previous, next)
        scheduleAlarm()
    }

    private inline fun <reified T : Widget> updateWidget(id: String, crossinline transform: (T) -> T) = update { s ->
        s.copy(widgets = s.widgets.map { w -> if (w.id == id && w is T) transform(w) else w })
    }

    // --- La sezione ---

    fun setHidden(hidden: Boolean) = update { it.copy(hidden = hidden) }

    /** Un widget nuovo, in fondo. Gli orologi partono col fuso dell'app, l'avanzamento con le sue quattro barre. */
    fun add(kind: WidgetKind) = update { s ->
        val widget: Widget = when (kind) {
            WidgetKind.TIME_ZONES -> TimeZonesWidget(clocks = listOf(ClockSetting(zoneId = AppSettings.timeZoneId)))
            WidgetKind.LIFE_PROGRESS -> LifeProgressWidget()
            WidgetKind.COUNTER -> CounterWidget()
            WidgetKind.POMODORO -> PomodoroWidget()
            WidgetKind.STOPWATCH -> StopwatchWidget()
            WidgetKind.TIMER -> TimerWidget()
            WidgetKind.WEATHER -> WeatherWidget()
        }
        s.copy(widgets = s.widgets + widget)
    }

    fun remove(id: String) = update { s -> s.copy(widgets = s.widgets.filterNot { it.id == id }) }

    /** Su (-1) o giù (+1) di un posto. */
    fun move(id: String, delta: Int) = update { s ->
        val list = s.widgets.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@update s
        list.add(to, list.removeAt(from))
        s.copy(widgets = list)
    }

    // --- Fusi orari ---

    fun addClock(widgetId: String, zoneId: String) = updateWidget<TimeZonesWidget>(widgetId) {
        it.copy(clocks = it.clocks + ClockSetting(zoneId = zoneId))
    }

    fun updateClock(widgetId: String, clockId: String, transform: (ClockSetting) -> ClockSetting) =
        updateWidget<TimeZonesWidget>(widgetId) { w ->
            w.copy(clocks = w.clocks.map { if (it.id == clockId) transform(it) else it })
        }

    fun removeClock(widgetId: String, clockId: String) = updateWidget<TimeZonesWidget>(widgetId) { w ->
        w.copy(clocks = w.clocks.filterNot { it.id == clockId })
    }

    fun addToZoneList(widgetId: String, zoneId: String) = updateWidget<TimeZonesWidget>(widgetId) { w ->
        if (zoneId in w.list) w else w.copy(list = w.list + zoneId)
    }

    fun removeFromZoneList(widgetId: String, zoneId: String) = updateWidget<TimeZonesWidget>(widgetId) { w ->
        w.copy(list = w.list - zoneId)
    }

    // --- Meteo (dal 17/10/2026) ---

    /** Una città in fondo; la prima aggiunta è la principale. Non più di `MAX_WEATHER_CITIES`. */
    fun addWeatherCity(widgetId: String, city: WeatherCity) = updateWidget<WeatherWidget>(widgetId) { w ->
        if (w.cities.size >= MAX_WEATHER_CITIES) w else w.copy(cities = w.cities + city)
    }

    /** Un'altra città al posto di questa, nello stesso posto; il meteo di quella di prima se ne va. */
    fun replaceWeatherCity(widgetId: String, cityId: String, city: WeatherCity) = updateWidget<WeatherWidget>(widgetId) { w ->
        w.copy(cities = w.cities.map { if (it.id == cityId) city else it }, reports = w.reports - cityId)
    }

    fun removeWeatherCity(widgetId: String, cityId: String) = updateWidget<WeatherWidget>(widgetId) { w ->
        w.copy(cities = w.cities.filterNot { it.id == cityId }, reports = w.reports - cityId)
    }

    /** Su (-1) o giù (+1) di un posto; in cima diventa la principale. */
    fun moveWeatherCity(widgetId: String, cityId: String, delta: Int) = updateWidget<WeatherWidget>(widgetId) { w ->
        val list = w.cities.toMutableList()
        val from = list.indexOfFirst { it.id == cityId }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@updateWidget w
        list.add(to, list.removeAt(from))
        w.copy(cities = list)
    }

    /** Il meteo appena scaricato di una città (se c'è ancora). */
    fun setWeatherReport(widgetId: String, cityId: String, report: WeatherReport) = updateWidget<WeatherWidget>(widgetId) { w ->
        if (w.cities.none { it.id == cityId }) w else w.copy(reports = w.reports + (cityId to report))
    }

    // --- Avanzamento ---

    fun updateBar(widgetId: String, barId: String, transform: (ProgressBarSetting) -> ProgressBarSetting) =
        updateWidget<LifeProgressWidget>(widgetId) { w ->
            w.copy(bars = w.bars.map { if (it.id == barId) transform(it) else it })
        }

    fun addBar(widgetId: String, bar: ProgressBarSetting) = updateWidget<LifeProgressWidget>(widgetId) {
        it.copy(bars = it.bars + bar)
    }

    /** "Show decimals": il decimale sulle percentuali del widget. */
    fun setProgressDecimals(widgetId: String, on: Boolean) = updateWidget<LifeProgressWidget>(widgetId) {
        it.copy(decimals = on)
    }

    fun removeBar(widgetId: String, barId: String) = updateWidget<LifeProgressWidget>(widgetId) { w ->
        w.copy(bars = w.bars.filterNot { it.id == barId && it.kind == ProgressKind.CUSTOM })
    }

    // --- Contatore ---

    fun changeCounter(widgetId: String, delta: Int) = updateWidget<CounterWidget>(widgetId) { it.copy(value = it.value + delta) }
    fun resetCounter(widgetId: String) = updateWidget<CounterWidget>(widgetId) { it.copy(value = 0) }
    fun renameCounter(widgetId: String, name: String) = updateWidget<CounterWidget>(widgetId) { it.copy(name = name) }

    // --- Pomodoro ---

    fun pomodoroStart(widgetId: String) = updateWidget<PomodoroWidget>(widgetId) { p ->
        if (p.running) p else {
            // Un timer arrivato a zero riparte dalla fase intera.
            val left = p.remainingMs.takeIf { it > 0 } ?: p.lengthOf(p.phase)
            p.copy(running = true, endsAt = System.currentTimeMillis() + left, remainingMs = left)
        }
    }

    fun pomodoroPause(widgetId: String) = updateWidget<PomodoroWidget>(widgetId) { p ->
        if (!p.running) p else p.copy(running = false, remainingMs = p.remainingAt(System.currentTimeMillis()), endsAt = null)
    }

    /** Da capo: ferma, sulla sessione di studio, alla sua durata piena. */
    fun pomodoroReset(widgetId: String) = updateWidget<PomodoroWidget>(widgetId) { p ->
        p.copy(running = false, endsAt = null, phase = PomodoroPhase.SESSION, remainingMs = p.lengthOf(PomodoroPhase.SESSION))
    }

    /** Il tempo scritto a mano toccando il timer: vedi `PomodoroWidget.withTimeWritten`. */
    fun pomodoroSetRemaining(widgetId: String, ms: Long) = updateWidget<PomodoroWidget>(widgetId) { p ->
        p.withTimeWritten(ms, System.currentTimeMillis())
    }

    /**
     * La durata della pausa dall'ingranaggio (quella della sessione di studio
     * si scrive nel riquadro grande, dal 28/09/2026). In pausa, a timer fermo
     * e intatto, il tempo mostrato si adegua; a metà, o mentre corre, resta
     * quello che c'è — la durata nuova vale dalla pausa dopo.
     */
    fun pomodoroSetBreakLength(widgetId: String, breakMinutes: Int) = updateWidget<PomodoroWidget>(widgetId) { p ->
        val updated = p.copy(breakMinutes = breakMinutes.coerceIn(1, MAX_POMODORO_MINUTES))
        if (!p.running && p.remainingMs == p.lengthOf(p.phase)) updated.copy(remainingMs = updated.lengthOf(p.phase)) else updated
    }

    fun pomodoroRename(widgetId: String, name: String) = updateWidget<PomodoroWidget>(widgetId) { it.copy(sessionName = name) }

    /** "Study sound" e "Break sound" di un pomodoro; null = il suono delle notifiche dell'app. */
    fun pomodoroSetSounds(
        widgetId: String,
        studyUri: String?,
        studyName: String?,
        breakUri: String?,
        breakName: String?
    ) = updateWidget<PomodoroWidget>(widgetId) {
        it.copy(
            studySoundUri = studyUri,
            studySoundName = studyName?.takeIf { studyUri != null },
            breakSoundUri = breakUri,
            breakSoundName = breakName?.takeIf { breakUri != null }
        )
    }

    // --- Cronometro ---

    fun stopwatchStart(widgetId: String) = updateWidget<StopwatchWidget>(widgetId) { it.started(System.currentTimeMillis()) }
    fun stopwatchStop(widgetId: String) = updateWidget<StopwatchWidget>(widgetId) { it.stopped(System.currentTimeMillis()) }
    fun stopwatchLap(widgetId: String) = updateWidget<StopwatchWidget>(widgetId) { it.lapped(System.currentTimeMillis()) }
    fun stopwatchReset(widgetId: String) = updateWidget<StopwatchWidget>(widgetId) { it.cleared() }

    // --- Timer ---

    private fun updateTimer(widgetId: String, timerId: String, transform: (CountdownTimer) -> CountdownTimer) =
        updateWidget<TimerWidget>(widgetId) { w -> w.copy(timers = w.timers.map { if (it.id == timerId) transform(it) else it }) }

    /** Il tempo scelto con le rotelle, salvato subito: resta anche chiudendo la barra. */
    fun timerSetDuration(widgetId: String, timerId: String, durationMs: Long) = updateTimer(widgetId, timerId) {
        if (it.started) it else it.copy(durationMs = durationMs, remainingMs = durationMs)
    }

    fun timerStart(widgetId: String, timerId: String) = updateTimer(widgetId, timerId) {
        if (it.durationMs <= 0) it else it.startedAt(System.currentTimeMillis())
    }

    fun timerPause(widgetId: String, timerId: String) = updateTimer(widgetId, timerId) { it.pausedAt(System.currentTimeMillis()) }
    fun timerResume(widgetId: String, timerId: String) = updateTimer(widgetId, timerId) { it.resumedAt(System.currentTimeMillis()) }
    fun timerReset(widgetId: String, timerId: String) = updateTimer(widgetId, timerId) { it.backToSetup() }

    /** La sua notifica tolta dalla tendina: non la si rimette finché il timer non cambia (vedi `notificationDismissed`). */
    fun timerNotificationDismissed(widgetId: String, timerId: String) = updateTimer(widgetId, timerId) {
        if (it.inNotification) it.copy(notificationDismissed = true) else it
    }

    /** Un timer in più, da impostare, in fondo: il "+" in alto a destra. */
    fun timerAdd(widgetId: String) = updateWidget<TimerWidget>(widgetId) { it.copy(timers = it.timers + CountdownTimer()) }

    /**
     * Toglie un timer aggiunto col "+" (il cestino in alto a destra, dal
     * secondo timer in poi); il primo resta sempre. Se stava correndo, la
     * sua sveglia se ne va con lui: `update` le ricalcola.
     */
    fun timerRemove(widgetId: String, timerId: String) = updateWidget<TimerWidget>(widgetId) { it.withoutTimer(timerId) }

    /**
     * Fa avanzare pomodori e timer arrivati alla fine. Suona se qualcosa è
     * finito **da poco** (`RING_WINDOW_MS`): chi recupera ad app chiusa
     * non deve suonare per il passato. (Fino al 25/09/2026 c'erano solo i
     * pomodori.)
     */
    private fun advancePomodoros(ringIfRecent: Boolean, window: Long = RING_WINDOW_MS) {
        val now = System.currentTimeMillis()
        val events = mutableListOf<WidgetEvent>()
        // Il suono del primo pomodoro appena finito: due che finiscono
        // insieme suonano una volta sola, come prima. I timer suonano col
        // suono delle notifiche dell'app.
        var sound: String? = null
        update { s ->
            s.copy(widgets = s.widgets.map { w ->
                when (w) {
                    is PomodoroWidget -> {
                        val (next, ended) = w.advancedTo(now)
                        if (ended != null && now - ended < window) {
                            // Il suono della fase che comincia: "Break sound" per
                            // la pausa, "Study sound" per lo studio.
                            if (events.none { it is WidgetEvent.PomodoroPhaseEnded }) sound = w.soundFor(next.phase)
                            events += WidgetEvent.PomodoroPhaseEnded(
                                widgetId = w.id,
                                sessionName = w.sessionName,
                                nowIn = next.phase,
                                // Per eccesso: una sessione di 1:30 si annuncia di 2 minuti, non di 1.
                                nextMinutes = ((next.lengthOf(next.phase) + MINUTE_MS - 1) / MINUTE_MS).toInt()
                            )
                        }
                        next
                    }
                    is TimerWidget -> w.copy(timers = w.timers.map { t ->
                        val (next, ended) = t.finishedBy(now)
                        if (ended != null && now - ended < window) events += WidgetEvent.TimerDone(t.id, t.durationMs)
                        next
                    })
                    else -> w
                }
            })
        }
        if (events.isNotEmpty() && ringIfRecent && ::appContext.isInitialized) {
            Notifier.postWidgetEvents(appContext, events, sound)
        }
        scheduleAlarm()
    }

    /** Dorme fino alla prima fine, fra tutti i pomodori e i timer che corrono. */
    private fun scheduleAlarm() {
        alarm?.cancel()
        val next = nextEnd()
        // La sveglia di Android, per quando l'app sarà chiusa: rimessa solo
        // se la prima fine è cambiata, non a ogni tocco del contatore.
        if (next != lastSystemAlarmEnd) {
            lastSystemAlarmEnd = next
            NoteAlarms.reschedule()
        }
        if (next == null) return
        alarm = scope.launch {
            delay((next - System.currentTimeMillis()).coerceAtLeast(0))
            advancePomodoros(ringIfRecent = true)
        }
    }

    /** Il suono che sta suonando adesso, per fermarlo: vedi `playOnce`. */
    private var playing: Ringtone? = null
    private var stopPlaying: Job? = null

    /**
     * Suona un suono **una volta sola, e al massimo per `MAX_SOUND_MS`**.
     *
     * Le suonerie del telefono (quelle "Galaxy" di Samsung, per esempio)
     * sono fatte per ripetersi finché non si risponde: il file stesso lo
     * chiede (l'etichetta `ANDROID_LOOP`), e suonate come un avviso non
     * finivano più — trovato sul telefono il 25/09/2026, scelta "Asteroid"
     * per il pomodoro. Si chiede di non ripetere (da Android 9), e in ogni
     * caso lo si ferma dopo qualche secondo: vale anche per un brano lungo
     * scelto come suono delle notifiche. Un suono nuovo ferma il vecchio.
     */
    fun playOnce(context: Context, uri: Uri) {
        stopPlaying?.cancel()
        playing?.stop()
        val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone.isLooping = false
        runCatching { ringtone.play() }
        playing = ringtone
        stopPlaying = scope.launch {
            delay(MAX_SOUND_MS)
            ringtone.stop()
            if (playing === ringtone) playing = null
        }
    }

    /** Il fuso dell'app, per chi disegna le barre dell'avanzamento. */
    val zone: ZoneId get() = AppSettings.zoneId

    private const val KEY_STATE = "state"
    private const val RING_WINDOW_MS = 5_000L
    private const val ALARM_RING_WINDOW_MS = 60_000L
    private const val MAX_SOUND_MS = 8_000L
    const val MAX_POMODORO_MINUTES = 180
}

/** I widget che si possono aggiungere, nell'ordine del menu. */
enum class WidgetKind { TIME_ZONES, LIFE_PROGRESS, COUNTER, POMODORO, STOPWATCH, TIMER, WEATHER }
