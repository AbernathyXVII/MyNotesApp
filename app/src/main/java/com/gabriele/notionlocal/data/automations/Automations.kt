package com.gabriele.notionlocal.data.automations

import android.content.Context
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonHost
import com.gabriele.notionlocal.data.buttons.ButtonOutcome
import com.gabriele.notionlocal.data.buttons.ButtonPlace
import com.gabriele.notionlocal.data.buttons.ButtonRunner
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.entity.AutomationEntity
import com.gabriele.notionlocal.data.entity.AutomationTrigger
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.data.notify.NoteAlarms
import com.gabriele.notionlocal.data.notify.Notifier
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * **Il motore delle automazioni** (dal 03/11/2026, vedi `AutomationEntity`).
 *
 * - **Page added** e **Property edited**: `DatabaseRepository` dice cosa è
 *   cambiato ([DatabaseChanges]); qui si cercano le automazioni del database
 *   che quella modifica fa partire. Si aspetta un attimo prima di farle: una
 *   cella di testo si salva a ogni lettera, e "a ogni cambiamento" vuol dire
 *   quando si è finito di scrivere; per una condizione ("quando diventa
 *   Fatto") si guarda che valga ancora.
 * - **A orario**: la sveglia delle notifiche (`NoteAlarms`) suona anche per
 *   la prossima automazione, e allora — o aprendo l'app, se il telefono era
 *   spento — partono quelle arrivate. Ognuna una volta sola, anche se nel
 *   frattempo ne sono passate più d'una.
 *
 * Le azioni sono quelle dei bottoni, fatte da `ButtonRunner` senza finestre:
 * niente conferme né pagine da aprire, le notifiche sì. "This page" è la
 * riga che ha fatto partire l'automazione; "Add page to" senza un database
 * scelto aggiunge al database dell'automazione.
 */
object Automations {

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = HashMap<String, Job>()

    /** Quanto aspettare dopo una modifica prima di far partire l'automazione. */
    internal var settleMs = 1_000L

    /** All'avvio dell'app: in ascolto delle modifiche, e quelle a orario arrivate mentre era chiusa. */
    fun init(context: Context) {
        appContext = context.applicationContext
        DatabaseChanges.listener = object : DatabaseChangeListener {
            override fun rowAdded(databaseId: String, rowId: String) {
                scope.launch { onRowAdded(databaseId, rowId) }
            }

            override fun cellChanged(rowId: String, columnId: String, before: String, after: String) {
                scope.launch { onCellChanged(rowId, columnId, before, after) }
            }
        }
        scope.launch {
            runCatching { runDue(appContext) }
            NoteAlarms.reschedule()
        }
    }

    private fun database(): AppDatabase = AppDatabase.getInstance(appContext)

    private suspend fun onRowAdded(databaseId: String, rowId: String) {
        val automations = runCatching { database().automationDao().enabledFor(databaseId) }.getOrDefault(emptyList())
        for (automation in automations) {
            if (automation.config.trigger !is AutomationTrigger.PageAdded) continue
            later("${automation.id}:$rowId") { runFor(automation.id, rowId) }
        }
    }

    private suspend fun onCellChanged(rowId: String, columnId: String, before: String, after: String) {
        val db = database()
        val row = db.databaseDao().getRowById(rowId) ?: return
        val automations = db.automationDao().enabledFor(row.pageId)
        if (automations.isEmpty()) return
        val column = db.databaseDao().getColumnById(columnId)
        // Una casella mai toccata non ha la cella: "" e "false" sono la stessa cosa.
        fun normal(value: String) = if (column?.type == ColumnType.CHECKBOX && value != "true") "false" else value
        for (automation in automations) {
            val trigger = automation.config.trigger as? AutomationTrigger.PropertyEdited ?: continue
            if (trigger.columnId != columnId || !trigger.fires(normal(before), normal(after))) continue
            later("${automation.id}:$rowId") {
                // Con una condizione, deve valere ancora: spuntata e subito tolta, niente.
                val current = normal(db.databaseDao().getCell(rowId, columnId)?.value.orEmpty())
                if (trigger.matches(current)) runFor(automation.id, rowId)
            }
        }
    }

    /** Fa [action] fra [settleMs]; un'altra richiesta con la stessa [key] prende il suo posto. */
    private fun later(key: String, action: suspend () -> Unit) {
        synchronized(pending) {
            pending.remove(key)?.cancel()
            pending[key] = scope.launch {
                delay(settleMs)
                synchronized(pending) { pending.remove(key) }
                runCatching { action() }
            }
        }
    }

    private suspend fun runFor(automationId: String, rowId: String?) {
        val db = database()
        val automation = db.automationDao().getById(automationId)?.takeIf { it.enabled } ?: return
        val row = rowId?.let { db.databaseDao().getRowById(it) }
        if (rowId != null && row == null) return
        run(appContext, db, automation, ButtonPlace(pageId = row?.linkedPageId, rowId = row?.id))
    }

    /**
     * **Fa le azioni** di [automation] per [place] (la riga, o niente per
     * quelle a orario), segnate come fatte da un'automazione.
     */
    suspend fun run(context: Context, db: AppDatabase, automation: AutomationEntity, place: ButtonPlace, now: Long = System.currentTimeMillis()): ButtonOutcome {
        // Un database nel cestino è fermo: le sue automazioni non fanno niente.
        if (!alive(db, automation)) return ButtonOutcome(0, false, emptyList(), 0, 0, 0)
        val app = context.applicationContext
        val store = PageImageStore(app)
        val steps = automation.config.actions.map { step ->
            // "Add page to" senza database: quello dell'automazione.
            if (step is ButtonStep.AddPage && step.databaseId == null) step.copy(databaseId = automation.databaseId, openAfter = false) else step
        }
        val outcome = withContext(AutomationRun()) {
            ButtonRunner(PageRepository(db)) { store.copy(it) }
                .run(ButtonConfig(name = automation.name, steps = steps), place, SilentHost(app), now = now, zone = AppSettings.zoneId)
        }
        db.automationDao().setLastRun(automation.id, now)
        return outcome
    }

    /**
     * **Le automazioni a orario arrivate**: ognuna la cui ultima volta è dopo
     * quella in cui è partita (o in cui è nata) parte adesso, una volta.
     */
    suspend fun runDue(context: Context, now: ZonedDateTime = ZonedDateTime.now(AppSettings.zoneId)) {
        val db = AppDatabase.getInstance(context.applicationContext)
        for (automation in db.automationDao().allEnabled()) {
            val schedule = automation.config.trigger as? AutomationTrigger.Schedule ?: continue
            val last = schedule.lastOccurrence(now)?.toInstant()?.toEpochMilli() ?: continue
            if (last > (automation.lastRunAt ?: automation.createdAt)) {
                runCatching { run(context, db, automation, ButtonPlace(pageId = null), now.toInstant().toEpochMilli()) }
            }
        }
    }

    /** Quando deve partire la prossima automazione a orario, per la sveglia; null se nessuna. */
    suspend fun nextAt(context: Context, now: ZonedDateTime = ZonedDateTime.now(AppSettings.zoneId)): Long? =
        runCatching {
            val db = AppDatabase.getInstance(context.applicationContext)
            db.automationDao().allEnabled()
                .filter { alive(db, it) }
                .mapNotNull { (it.config.trigger as? AutomationTrigger.Schedule)?.nextOccurrence(now)?.toInstant()?.toEpochMilli() }
                .minOrNull()
        }.getOrNull()

    /** Se il database dell'automazione c'è ancora, fuori dal cestino. */
    private suspend fun alive(db: AppDatabase, automation: AutomationEntity): Boolean =
        db.pageDao().getById(automation.databaseId)?.trashedAt == null

    /** Un'automazione non ha finestre: niente conferme né pagine da aprire; le notifiche sì. */
    private class SilentHost(private val context: Context) : ButtonHost {
        override suspend fun confirm(message: String): Boolean = true
        override suspend fun insertBlocks(pageId: String, anchorBlockId: String?, place: InsertPlace, blocks: List<NewBlock>): Boolean? = null
        override fun openPage(pageId: String, isDatabase: Boolean) {}
        override fun openUrl(url: String) {}
        override fun notify(title: String, text: String): Boolean = Notifier.postButton(context, title, text)
    }
}
