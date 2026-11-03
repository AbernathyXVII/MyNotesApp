package com.gabriele.notionlocal.data.automations

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.PropertyEdit
import com.gabriele.notionlocal.data.entity.AutomationConfig
import com.gabriele.notionlocal.data.entity.AutomationEntity
import com.gabriele.notionlocal.data.entity.AutomationTrigger
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.Repeat
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * **Le automazioni dei database** (03/11/2026, chiesto dall'utente: "ogni
 * lunedì crea la pagina della settimana", "quando spunto Fatto, metti la data
 * di oggi"): quando partono e cosa fanno.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AutomationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)
    private val zone = ZoneId.of("Europe/Rome")

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.pageDao().insert(PageEntity(id = "todo", title = "To do", isDatabase = true))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "fatto", pageId = "todo", name = "Fatto", type = ColumnType.CHECKBOX, orderIndex = 0))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "todo", name = "Quando", type = ColumnType.DATE, orderIndex = 1))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "stato", pageId = "todo", name = "Stato", type = ColumnType.SELECT, orderIndex = 2,
            optionsJson = """[{"label":"Da fare"},{"label":"In corso"},{"label":"Fatto"}]"""))
        db.databaseDao().insertRow(DatabaseRowEntity(id = "r1", pageId = "todo", title = "Spesa"))
        Automations.settleMs = 0
        Automations.init(context)
    }

    @After
    fun tearDown() {
        DatabaseChanges.listener = null
        Thread.sleep(300)
        AppDatabase.resetForTests()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < until) {
            if (condition()) return
            Thread.sleep(30)
        }
        throw AssertionError("Non è successo: $what")
    }

    private fun cell(rowId: String, columnId: String) = runBlocking { db.databaseDao().getCell(rowId, columnId)?.value.orEmpty() }

    private fun automation(name: String, trigger: AutomationTrigger, vararg actions: ButtonStep, createdAt: Long = System.currentTimeMillis()) = runBlocking {
        val entity = AutomationEntity(
            databaseId = "todo",
            name = name,
            configJson = AutomationConfig.encode(AutomationConfig(trigger, actions.toList())),
            createdAt = createdAt
        )
        db.automationDao().upsert(entity)
        entity
    }

    @Test
    fun theDaysComeBackAtTheirTime() {
        val weekly = AutomationTrigger.Schedule(Repeat.WEEKLY, days = listOf(1), minute = 9 * 60)
        // Lunedì 2 novembre 2026, alle 10: l'ultima volta è stamattina alle 9, la prossima lunedì dopo.
        val monday10 = ZonedDateTime.of(2026, 11, 2, 10, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 2, 9, 0, 0, 0, zone), weekly.lastOccurrence(monday10))
        assertEquals(ZonedDateTime.of(2026, 11, 9, 9, 0, 0, 0, zone), weekly.nextOccurrence(monday10))
        // Alle 8 di lunedì l'ultima era la settimana prima.
        assertEquals(ZonedDateTime.of(2026, 10, 26, 9, 0, 0, 0, zone), weekly.lastOccurrence(monday10.withHour(8)))
        // Il 31 di ogni mese, a febbraio è il 28.
        val monthly = AutomationTrigger.Schedule(Repeat.MONTHLY, dayOfMonth = 31, minute = 0)
        assertEquals(LocalDate.of(2027, 2, 28), monthly.nextOccurrence(ZonedDateTime.of(2027, 2, 1, 12, 0, 0, 0, zone))!!.toLocalDate())
        // Una settimana senza giorni non tocca mai.
        assertNull(AutomationTrigger.Schedule(Repeat.WEEKLY, days = emptyList()).nextOccurrence(monday10))
    }

    @Test
    fun aConditionFiresOnlyWhenTheValueArrives() {
        val done = AutomationTrigger.PropertyEdited("fatto", listOf("true"))
        assertTrue(done.fires("false", "true"))
        assertFalse(done.fires("true", "false"))
        assertFalse(done.fires("true", "true"))
        val tags = AutomationTrigger.PropertyEdited("tag", listOf("Urgente"))
        assertTrue(tags.fires("Casa", "Casa|Urgente"))
        assertFalse(tags.fires("Urgente", "Casa|Urgente"))
        // Senza valori, ogni cambiamento.
        assertTrue(AutomationTrigger.PropertyEdited("nota").fires("a", "ab"))
        // Il quando e le azioni si salvano e si rileggono.
        val config = AutomationConfig(done, listOf(ButtonStep.EditPages(edits = listOf(PropertyEdit(columnId = "quando", op = EditOp.TODAY)))))
        assertEquals(config, AutomationConfig.parse(AutomationConfig.encode(config)))
    }

    @Test
    fun checkingDoneWritesTodaysDate() = runBlocking<Unit> {
        automation(
            "Fatto oggi",
            AutomationTrigger.PropertyEdited("fatto", listOf("true")),
            ButtonStep.EditPages(databaseId = null, edits = listOf(PropertyEdit(columnId = "quando", op = EditOp.TODAY)))
        )
        DatabaseRepository(db).setCheckbox("r1", "fatto", "true")
        waitFor("la data di oggi") { cell("r1", "quando").isNotEmpty() }
        val start = parseDateRange(cell("r1", "quando"))!!.start
        assertEquals(LocalDate.now(AppSettings.zoneId), Instant.ofEpochMilli(start).atZone(AppSettings.zoneId).toLocalDate())

        // Togliendo la spunta non riparte: la data resta quella.
        db.databaseDao().getCell("r1", "quando")!!.let { db.databaseDao().updateCell(it.copy(value = "")) }
        DatabaseRepository(db).setCheckbox("r1", "fatto", "false")
        Thread.sleep(400)
        assertEquals("", cell("r1", "quando"))
    }

    @Test
    fun aNewPageGetsItsStatusAndTheAutomationDoesNotRestartItself() = runBlocking<Unit> {
        automation(
            "Nuova",
            AutomationTrigger.PageAdded(),
            ButtonStep.EditPages(databaseId = null, edits = listOf(PropertyEdit(columnId = "stato", op = EditOp.SET, value = "Da fare")))
        )
        // Cambiare lo Stato lo rimette "In corso": se le automazioni si facessero
        // partire fra loro, le due girerebbero per sempre.
        automation(
            "Stato cambiato",
            AutomationTrigger.PropertyEdited("stato"),
            ButtonStep.EditPages(databaseId = null, edits = listOf(PropertyEdit(columnId = "stato", op = EditOp.SET, value = "In corso")))
        )
        DatabaseRepository(db).addRow(DatabaseRowEntity(id = "r2", pageId = "todo", title = "Bollette"))
        waitFor("lo Stato della pagina nuova") { cell("r2", "stato").isNotEmpty() }
        Thread.sleep(400)
        assertEquals("Da fare", cell("r2", "stato"))
        // Cambiato a mano, invece, l'altra parte.
        DatabaseRepository(db).setCellValue("r2", "stato", "Fatto")
        waitFor("In corso") { cell("r2", "stato") == "In corso" }
    }

    @Test
    fun everyMondayTheWeekPageIsBornFromItsTemplateOnce() = runBlocking<Unit> {
        db.pageDao().insert(PageEntity(id = "tpl", title = "Settimana tipo", isTemplate = true))
        db.blockDao().insert(BlockEntity(id = "t1", pageId = "tpl", textJson = """[{"text":"Obiettivi"}]"""))
        // Nata la domenica prima: il lunedì alle 9 è da fare.
        val sunday = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, AppSettings.zoneId)
        automation(
            "Settimana",
            AutomationTrigger.Schedule(Repeat.WEEKLY, days = listOf(1), minute = 9 * 60),
            ButtonStep.AddPage(title = "Settimana {week}", templateId = "tpl"),
            createdAt = sunday.toInstant().toEpochMilli()
        )
        val monday = ZonedDateTime.of(2026, 11, 2, 9, 30, 0, 0, AppSettings.zoneId)
        assertEquals(ZonedDateTime.of(2026, 11, 9, 9, 0, 0, 0, AppSettings.zoneId).toInstant().toEpochMilli(), Automations.nextAt(context, monday))
        Automations.runDue(context, monday)
        val week = db.databaseDao().getRowsForPageOnce("todo").single { it.title.startsWith("Settimana") }
        assertEquals("Settimana 45", week.title)
        val blocks = db.blockDao().getBlocksForPageOnce(week.linkedPageId!!)
        assertTrue(blocks.any { it.textJson.contains("Obiettivi") })
        // Una volta sola: tornando a guardare, niente di nuovo fino a lunedì prossimo.
        Automations.runDue(context, monday.plusHours(3))
        assertEquals(1, db.databaseDao().getRowsForPageOnce("todo").count { it.title.startsWith("Settimana") })
        // Col database nel cestino si ferma: niente sveglia, niente pagine.
        db.pageDao().update(db.pageDao().getById("todo")!!.copy(trashedAt = 1))
        assertNull(Automations.nextAt(context, monday))
        Automations.runDue(context, monday.plusWeeks(1))
        assertEquals(1, db.databaseDao().getRowsForPageOnce("todo").count { it.title.startsWith("Settimana") })
    }
}
