package com.gabriele.notionlocal.data.widgets

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
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
 * fino alla fine della fase (`scheduleAlarm`). Ad app chiusa non suona —
 * servirebbe un allarme di sistema — ma riaprendola il timer è al punto
 * giusto, perché si conta sull'orologio (vedi `PomodoroWidget`).
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

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("widgets", Context.MODE_PRIVATE)
        stateHolder.value = prefs.getString(KEY_STATE, null)
            ?.let { runCatching { json.decodeFromString<WidgetsState>(it) }.getOrNull() }
            ?: WidgetsState()
        // Le fasi finite mentre l'app era chiusa: si recuperano in
        // silenzio, senza suonare per qualcosa di già passato.
        advancePomodoros(ringIfRecent = false)
    }

    private fun update(transform: (WidgetsState) -> WidgetsState) {
        val next = transform(stateHolder.value)
        if (next == stateHolder.value) return
        stateHolder.value = next
        if (::prefs.isInitialized) prefs.edit().putString(KEY_STATE, json.encodeToString(next)).apply()
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

    // --- Avanzamento ---

    fun updateBar(widgetId: String, barId: String, transform: (ProgressBarSetting) -> ProgressBarSetting) =
        updateWidget<LifeProgressWidget>(widgetId) { w ->
            w.copy(bars = w.bars.map { if (it.id == barId) transform(it) else it })
        }

    fun addBar(widgetId: String, bar: ProgressBarSetting) = updateWidget<LifeProgressWidget>(widgetId) {
        it.copy(bars = it.bars + bar)
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

    /** Il tempo scritto a mano toccando il timer: vale per la fase in corso, che corra o no. */
    fun pomodoroSetRemaining(widgetId: String, ms: Long) = updateWidget<PomodoroWidget>(widgetId) { p ->
        val left = ms.coerceAtLeast(0)
        if (p.running) p.copy(endsAt = System.currentTimeMillis() + left, remainingMs = left) else p.copy(remainingMs = left)
    }

    /**
     * Le durate dall'ingranaggio. A timer fermo e intatto (all'inizio della
     * sua fase) il tempo mostrato si adegua; a metà, o mentre corre, resta
     * quello che c'è — le durate nuove valgono dal turno dopo.
     */
    fun pomodoroSetLengths(widgetId: String, sessionMinutes: Int, breakMinutes: Int) = updateWidget<PomodoroWidget>(widgetId) { p ->
        val updated = p.copy(
            sessionMinutes = sessionMinutes.coerceIn(1, MAX_POMODORO_MINUTES),
            breakMinutes = breakMinutes.coerceIn(1, MAX_POMODORO_MINUTES)
        )
        if (!p.running && p.remainingMs == p.lengthOf(p.phase)) updated.copy(remainingMs = updated.lengthOf(p.phase)) else updated
    }

    fun pomodoroRename(widgetId: String, name: String) = updateWidget<PomodoroWidget>(widgetId) { it.copy(sessionName = name) }

    /**
     * Fa avanzare i pomodori la cui fase è finita. Suona se una è finita
     * **da poco** (`RING_WINDOW_MS`): chi recupera fasi finite ad app
     * chiusa non deve suonare per il passato.
     */
    private fun advancePomodoros(ringIfRecent: Boolean) {
        val now = System.currentTimeMillis()
        var ring = false
        update { s ->
            s.copy(widgets = s.widgets.map { w ->
                if (w !is PomodoroWidget) return@map w
                val (next, ended) = w.advancedTo(now)
                if (ended != null && now - ended < RING_WINDOW_MS) ring = true
                next
            })
        }
        if (ring && ringIfRecent) ring()
        scheduleAlarm()
    }

    /** Dorme fino alla prima fase che finisce, fra tutti i pomodori che corrono. */
    private fun scheduleAlarm() {
        alarm?.cancel()
        val next = stateHolder.value.widgets
            .filterIsInstance<PomodoroWidget>()
            .filter { it.running }
            .mapNotNull { it.endsAt }
            .minOrNull() ?: return
        alarm = scope.launch {
            delay((next - System.currentTimeMillis()).coerceAtLeast(0))
            advancePomodoros(ringIfRecent = true)
        }
    }

    /**
     * Il suono di fine fase: quello scelto nelle impostazioni per le
     * notifiche, o quello del telefono. Tace se le notifiche sono messe a
     * tacere nelle impostazioni dell'app.
     */
    private fun ring() {
        if (!::appContext.isInitialized) return
        if (AppSettings.notificationsMuted || AppSettings.mutedUntil != null) return
        runCatching {
            val uri = AppSettings.notificationSoundUri?.let(Uri::parse)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(appContext, uri)?.play()
        }
    }

    /** Il fuso dell'app, per chi disegna le barre dell'avanzamento. */
    val zone: ZoneId get() = AppSettings.zoneId

    private const val KEY_STATE = "state"
    private const val RING_WINDOW_MS = 5_000L
    const val MAX_POMODORO_MINUTES = 180
}

/** I widget che si possono aggiungere, nell'ordine del menu. */
enum class WidgetKind { TIME_ZONES, LIFE_PROGRESS, COUNTER, POMODORO }
