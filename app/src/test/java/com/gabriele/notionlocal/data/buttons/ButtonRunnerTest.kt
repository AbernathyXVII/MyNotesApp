package com.gabriele.notionlocal.data.buttons

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
import java.time.ZoneId

/**
 * **Il motore dei bottoni** (27/10/2026): ogni azione sul database vero, in
 * memoria — i blocchi messi al posto giusto, la riga nuova, le righe
 * cambiate e solo quelle, la conferma che ferma, le pagine aperte alla fine,
 * e i problemi detti senza fermare il resto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ButtonRunnerTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var runner: ButtonRunner
    private val zone = ZoneId.of("Europe/Rome")
    private val now = 1_800_000_000_000L

    private val page = PageEntity(id = "page", title = "Diario")
    private val tasks = PageEntity(id = "tasks", title = "Tasks", isDatabase = true)
    private val done = DatabaseColumnEntity(id = "done", pageId = "tasks", name = "Done", type = ColumnType.CHECKBOX, orderIndex = 0)
    private val count = DatabaseColumnEntity(id = "count", pageId = "tasks", name = "Count", type = ColumnType.NUMBER, orderIndex = 1)
    private val status = DatabaseColumnEntity(
        id = "status", pageId = "tasks", name = "Status", type = ColumnType.SELECT, orderIndex = 2,
        optionsJson = """[{"label":"Da fare"},{"label":"Fatto"}]"""
    )
    private val whenDone = DatabaseColumnEntity(id = "when", pageId = "tasks", name = "When", type = ColumnType.DATE, orderIndex = 3)
    private val pasta = DatabaseRowEntity(id = "pasta", pageId = "tasks", title = "Pasta", orderIndex = 0)
    private val pane = DatabaseRowEntity(id = "pane", pageId = "tasks", title = "Pane", orderIndex = 1)
    private val latte = DatabaseRowEntity(id = "latte", pageId = "tasks", title = "Latte", orderIndex = 2)

    /** La schermata finta: ricorda cosa le si chiede e risponde alle conferme con [answers]. */
    private class FakeHost(val answers: MutableList<Boolean> = mutableListOf()) : ButtonHost {
        val asked = mutableListOf<String>()
        val opened = mutableListOf<String>()
        val notified = mutableListOf<Pair<String, String>>()
        var notifications = true
        override suspend fun confirm(message: String): Boolean {
            asked += message
            return answers.removeFirstOrNull() ?: true
        }
        override fun openPage(pageId: String, isDatabase: Boolean) {
            opened += "page:$pageId"
        }
        override fun openUrl(url: String) {
            opened += "url:$url"
        }
        override fun notify(title: String, text: String): Boolean {
            notified += title to text
            return notifications
        }
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(page)
        pages.createPage(tasks)
        listOf(done, count, status, whenDone).forEach { db.databaseDao().insertColumn(it) }
        listOf(pasta, pane, latte).forEach { db.databaseDao().insertRow(it) }
        cell(pasta, done, "true")
        cell(pasta, status, "Fatto")
        cell(pane, count, "2")
        cell(pane, status, "Da fare")
        runner = ButtonRunner(pages)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun cell(row: DatabaseRowEntity, column: DatabaseColumnEntity, value: String) =
        runBlocking { db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = column.id, value = value)) }

    private fun value(rowId: String, column: DatabaseColumnEntity): String =
        runBlocking { db.databaseDao().getCell(rowId, column.id)?.value.orEmpty() }

    private fun run(config: ButtonConfig, place: ButtonPlace, host: FakeHost = FakeHost()): ButtonOutcome =
        runBlocking { runner.run(config, place, host, now = now, zone = zone) }

    private fun button(vararg steps: ButtonStep) = ButtonConfig(name = "Bottone", steps = steps.toList())

    private fun text(block: BlockEntity): String =
        Json.decodeFromString<List<RichTextSpan>>(block.textJson).joinToString("") { it.text }

    /** La pagina coi suoi blocchi di primo livello, in ordine: "B" è il bottone. */
    private fun pageWith(vararg texts: String): List<BlockEntity> = runBlocking {
        texts.mapIndexed { i, t ->
            val block = if (t == "B") {
                BlockEntity(id = "button", pageId = page.id, type = BlockType.BUTTON, orderIndex = i, attrsJson = BlockAttrs.encode(BlockAttrs(button = ButtonConfig())))
            } else {
                BlockEntity(pageId = page.id, textJson = """[{"text":"$t"}]""", orderIndex = i)
            }
            db.blockDao().insert(block)
            block
        }
    }

    private fun shownPage(): List<String> = runBlocking {
        db.blockDao().getBlocksForPageOnce(page.id).filter { it.parentBlockId == null }.sortedBy { it.orderIndex }
            .map { if (it.type == BlockType.BUTTON) "B" else text(it) }
    }

    // --- Insert blocks ---

    @Test
    fun insertBlocksGoesBelowTheButtonAndTheNewestComesFirst() {
        pageWith("Prima", "B", "Dopo")
        val config = button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "Uno"), ButtonBlock(type = "CHECKBOX", text = "Due"))))
        val place = ButtonPlace(pageId = page.id, blockId = "button")
        val outcome = run(config, place)
        assertEquals(2, outcome.insertedBlocks)
        assertEquals(listOf("Prima", "B", "Uno", "Due", "Dopo"), shownPage())
        val todo = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.single { text(it) == "Due" }
        assertEquals(BlockType.CHECKBOX, todo.type)
        // Toccato di nuovo, i blocchi nuovi vanno subito sotto il bottone, prima di quelli di prima.
        run(button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "Tre")))), place)
        assertEquals(listOf("Prima", "B", "Tre", "Uno", "Due", "Dopo"), shownPage())
    }

    @Test
    fun insertBlocksAboveTheButtonAtTheTopAndAtTheBottom() {
        pageWith("Prima", "B", "Dopo")
        val place = ButtonPlace(pageId = page.id, blockId = "button")
        run(button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "Sopra")), place = InsertPlace.ABOVE_BUTTON)), place)
        assertEquals(listOf("Prima", "Sopra", "B", "Dopo"), shownPage())
        run(button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "Cima")), place = InsertPlace.TOP_OF_PAGE)), place)
        assertEquals(listOf("Cima", "Prima", "Sopra", "B", "Dopo"), shownPage())
        run(button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(type = "DIVIDER"), ButtonBlock(text = "Fondo")), place = InsertPlace.BOTTOM_OF_PAGE)), place)
        assertEquals(listOf("Cima", "Prima", "Sopra", "B", "Dopo", "", "Fondo"), shownPage())
    }

    @Test
    fun aButtonInsideAToggleInsertsInsideTheToggle() = runBlocking<Unit> {
        val toggle = BlockEntity(id = "toggle", pageId = page.id, type = BlockType.TOGGLE, textJson = """[{"text":"Cartella"}]""", orderIndex = 0)
        db.blockDao().insert(toggle)
        db.blockDao().insert(BlockEntity(id = "button", pageId = page.id, parentBlockId = "toggle", type = BlockType.BUTTON, orderIndex = 0))
        run(button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "Dentro")))), ButtonPlace(page.id, "button"))
        val inside = db.blockDao().getBlocksForPageOnce(page.id).filter { it.parentBlockId == "toggle" }.sortedBy { it.orderIndex }
        assertEquals(listOf(BlockType.BUTTON, BlockType.PARAGRAPH), inside.map { it.type })
    }

    @Test
    fun aRowButtonInsertsAtTheBottomOfTheRowPageMadeForIt() = runBlocking<Unit> {
        // "Sotto il bottone", ma il bottone di una riga non sta nella pagina: in fondo a lei.
        val outcome = run(
            button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "Nota")), place = InsertPlace.BELOW_BUTTON)),
            ButtonPlace(pageId = null, rowId = pane.id)
        )
        assertEquals(1, outcome.insertedBlocks)
        val rowPage = db.databaseDao().getRowById(pane.id)!!.linkedPageId!!
        assertEquals(listOf("Nota"), db.blockDao().getBlocksForPageOnce(rowPage).map { text(it) })
    }

    @Test
    fun aLockedPageGetsNoBlocks() = runBlocking<Unit> {
        pageWith("Prima", "B")
        db.pageDao().update(page.copy(isLocked = true))
        val outcome = run(button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "No")))), ButtonPlace(page.id, "button"))
        assertEquals(listOf(ButtonProblem.LOCKED_PAGE), outcome.problems)
        assertEquals(listOf("Prima", "B"), shownPage())
    }

    @Test
    fun theEditorCanPutTheBlocksItself() {
        pageWith("B")
        var asked: Triple<String, InsertPlace, List<NewBlock>>? = null
        val host = object : ButtonHost by FakeHost() {
            override suspend fun insertBlocks(pageId: String, anchorBlockId: String?, place: InsertPlace, blocks: List<NewBlock>): Boolean? {
                asked = Triple(anchorBlockId!!, place, blocks)
                return true
            }
        }
        runBlocking { runner.run(button(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "X")))), ButtonPlace(page.id, "button"), host) }
        assertEquals("button", asked!!.first)
        assertEquals(InsertPlace.BELOW_BUTTON, asked!!.second)
        // L'ha messo l'editor: il motore non l'ha scritto un'altra volta.
        assertEquals(listOf("B"), shownPage())
    }

    // --- Add page to ---

    @Test
    fun addPageMakesARowAtTheEndWithItsValues() = runBlocking<Unit> {
        val host = FakeHost()
        val outcome = run(
            button(
                ButtonStep.AddPage(
                    databaseId = tasks.id,
                    title = "Uova",
                    edits = listOf(
                        PropertyEdit(columnId = done.id, op = EditOp.CHECK),
                        PropertyEdit(columnId = count.id, op = EditOp.ADD, value = "3"),
                        PropertyEdit(columnId = status.id, op = EditOp.SET, value = "Da fare"),
                        PropertyEdit(columnId = whenDone.id, op = EditOp.NOW)
                    ),
                    openAfter = true
                )
            ),
            ButtonPlace(page.id),
            host
        )
        assertEquals(1, outcome.createdRows)
        val row = db.databaseDao().getRowsForPageOnce(tasks.id).single { it.title == "Uova" }
        assertEquals(3, row.orderIndex)
        assertEquals("true", value(row.id, done))
        assertEquals("3", value(row.id, count))
        assertEquals("Da fare", value(row.id, status))
        assertEquals(now, parseDateRange(value(row.id, whenDone))!!.start)
        // "Open it afterwards": la pagina della riga, nata adesso.
        val rowPage = db.databaseDao().getRowById(row.id)!!.linkedPageId!!
        assertEquals(listOf("page:$rowPage"), host.opened)
    }

    @Test
    fun aLinkedViewAddsToItsDatabase() = runBlocking<Unit> {
        val view = PageEntity(id = "view", title = "Vista", isDatabase = true, sourceDatabaseId = tasks.id)
        pages.createPage(view)
        run(button(ButtonStep.AddPage(databaseId = view.id, title = "Dalla vista")), ButtonPlace(page.id))
        assertTrue(db.databaseDao().getRowsForPageOnce(tasks.id).any { it.title == "Dalla vista" })
        assertTrue(db.databaseDao().getRowsForPageOnce(view.id).isEmpty())
    }

    @Test
    fun aMissingDatabaseIsSaidAndTheOtherActionsGoOn() = runBlocking<Unit> {
        val host = FakeHost()
        val outcome = run(
            button(
                ButtonStep.AddPage(databaseId = "cancellato", title = "X"),
                ButtonStep.Notify(text = "Dopo")
            ),
            ButtonPlace(page.id),
            host
        )
        assertEquals(listOf(ButtonProblem.NO_DATABASE), outcome.problems)
        // La notifica parte lo stesso, col nome del bottone come titolo.
        assertEquals(listOf("Bottone" to "Dopo"), host.notified)
        assertEquals(3, db.databaseDao().getRowsForPageOnce(tasks.id).size)
    }

    @Test
    fun aDatabaseInTheTrashGetsNothing() = runBlocking<Unit> {
        db.pageDao().update(tasks.copy(trashedAt = 1L))
        val outcome = run(button(ButtonStep.AddPage(databaseId = tasks.id, title = "X")), ButtonPlace(page.id))
        assertEquals(listOf(ButtonProblem.NO_DATABASE), outcome.problems)
    }

    @Test
    fun aDatabaseLockedWithThePasswordGetsNothing() = runBlocking<Unit> {
        db.pageDao().update(tasks.copy(isProtected = true))
        val outcome = run(button(ButtonStep.AddPage(databaseId = tasks.id, title = "X")), ButtonPlace(page.id))
        assertEquals(listOf(ButtonProblem.LOCKED_DATABASE), outcome.problems)
        assertEquals(3, db.databaseDao().getRowsForPageOnce(tasks.id).size)
    }

    // --- Edit pages in ---

    @Test
    fun thisPageIsTheRowOfTheButton() {
        val config = button(
            ButtonStep.EditPages(
                edits = listOf(
                    PropertyEdit(columnId = done.id, op = EditOp.TOGGLE),
                    PropertyEdit(columnId = count.id, op = EditOp.ADD, value = "1"),
                    PropertyEdit(columnId = NAME_PROPERTY_ID, op = EditOp.SET, value = "Pane integrale")
                )
            )
        )
        val outcome = run(config, ButtonPlace(pageId = null, rowId = pane.id))
        assertTrue(outcome.thisPageChanged)
        // "This page" si vede già: non si conta fra le righe cambiate altrove.
        assertEquals(0, outcome.changedRows)
        assertEquals("true", value(pane.id, done))
        assertEquals("3", value(pane.id, count))
        assertEquals("Pane integrale", runBlocking { db.databaseDao().getRowById(pane.id)!!.title })
        // Le altre righe non si toccano.
        assertEquals("", value(latte.id, done))
        assertEquals("true", value(pasta.id, done))
        // Toccato di nuovo, la casella torna come prima e il numero cresce ancora.
        run(config, ButtonPlace(pageId = null, rowId = pane.id))
        assertEquals("false", value(pane.id, done))
        assertEquals("4", value(pane.id, count))
    }

    @Test
    fun thisPageWithoutARowIsSaid() {
        val outcome = run(
            button(ButtonStep.EditPages(edits = listOf(PropertyEdit(columnId = done.id, op = EditOp.CHECK)))),
            ButtonPlace(page.id, "button")
        )
        assertEquals(listOf(ButtonProblem.NO_THIS_PAGE), outcome.problems)
    }

    @Test
    fun theButtonInTheRowPageWorksOnItsRow() = runBlocking<Unit> {
        val rowPage = pages.ensureRowPage(latte.id)!!
        val row = pages.rowOfPage(rowPage)
        assertEquals(latte.id, row?.id)
        run(button(ButtonStep.EditPages(edits = listOf(PropertyEdit(columnId = done.id, op = EditOp.CHECK)))), ButtonPlace(rowPage, rowId = row!!.id))
        assertEquals("true", value(latte.id, done))
    }

    @Test
    fun editPagesChangesOnlyTheRowsThatPassTheFilter() {
        val outcome = run(
            button(
                ButtonStep.EditPages(
                    databaseId = tasks.id,
                    filter = ButtonFilter(status.id, listOf("Da fare")),
                    edits = listOf(PropertyEdit(columnId = status.id, op = EditOp.SET, value = "Fatto"), PropertyEdit(columnId = done.id, op = EditOp.CHECK))
                )
            ),
            ButtonPlace(page.id)
        )
        assertEquals(1, outcome.changedRows)
        assertEquals("Fatto", value(pane.id, status))
        assertEquals("true", value(pane.id, done))
        // Latte non ha lo stato: non passa.
        assertEquals("", value(latte.id, status))
        assertEquals("", value(latte.id, done))
    }

    @Test
    fun withoutAFilterEveryRowChangesAndOnlyTheOnesThatNeedIt() {
        val outcome = run(
            button(ButtonStep.EditPages(databaseId = tasks.id, edits = listOf(PropertyEdit(columnId = done.id, op = EditOp.UNCHECK)))),
            ButtonPlace(page.id)
        )
        // Solo Pasta era spuntata: le altre erano già "non spuntate".
        assertEquals(1, outcome.changedRows)
        assertEquals("false", value(pasta.id, done))
    }

    @Test
    fun togglingRowsWithSubRowsGivesEachRowWhatItAsked() = runBlocking<Unit> {
        // Pane ha sotto Latte: invertendo tutte, ognuna si inverte da com'era.
        db.databaseDao().updateRow(latte.copy(parentRowId = pane.id))
        run(
            button(ButtonStep.EditPages(databaseId = tasks.id, edits = listOf(PropertyEdit(columnId = done.id, op = EditOp.TOGGLE)))),
            ButtonPlace(page.id)
        )
        assertEquals("false", value(pasta.id, done))
        assertEquals("true", value(pane.id, done))
        assertEquals("true", value(latte.id, done))
    }

    @Test
    fun aFilterOnADeletedPropertyChangesNothing() = runBlocking<Unit> {
        val outcome = run(
            button(
                ButtonStep.EditPages(
                    databaseId = tasks.id,
                    filter = ButtonFilter("cancellata", listOf("x")),
                    edits = listOf(PropertyEdit(columnId = done.id, op = EditOp.CHECK))
                )
            ),
            ButtonPlace(page.id)
        )
        assertEquals(listOf(ButtonProblem.NO_FILTER), outcome.problems)
        assertEquals("", value(pane.id, done))
        assertEquals("", value(latte.id, done))
    }

    @Test
    fun aDeletedPropertyAndABadValueAreSaidAndTheRestIsDone() {
        val outcome = run(
            button(
                ButtonStep.EditPages(
                    edits = listOf(
                        PropertyEdit(columnId = "cancellata", op = EditOp.SET, value = "x"),
                        PropertyEdit(columnId = status.id, op = EditOp.SET, value = "Inventato"),
                        PropertyEdit(columnId = done.id, op = EditOp.CHECK)
                    )
                )
            ),
            ButtonPlace(pageId = null, rowId = latte.id)
        )
        assertEquals(listOf(ButtonProblem.NO_PROPERTY, ButtonProblem.BAD_VALUE), outcome.problems)
        assertEquals("true", value(latte.id, done))
    }

    // --- Show confirmation, Open, Send notification ---

    @Test
    fun cancelInTheConfirmationStopsTheActionsAfterIt() {
        val host = FakeHost(answers = mutableListOf(false))
        val outcome = run(
            button(
                ButtonStep.EditPages(edits = listOf(PropertyEdit(columnId = count.id, op = EditOp.ADD, value = "1"))),
                ButtonStep.Confirm(message = "Anche il resto?"),
                ButtonStep.EditPages(edits = listOf(PropertyEdit(columnId = done.id, op = EditOp.CHECK))),
                ButtonStep.Open(url = "example.com")
            ),
            ButtonPlace(pageId = null, rowId = pane.id),
            host
        )
        assertTrue(outcome.cancelled)
        assertEquals(listOf("Anche il resto?"), host.asked)
        // Quello prima della domanda è fatto, quello dopo no, e non si apre niente.
        assertEquals("3", value(pane.id, count))
        assertEquals("", value(pane.id, done))
        assertTrue(host.opened.isEmpty())
    }

    @Test
    fun continueInTheConfirmationGoesOn() {
        val host = FakeHost(answers = mutableListOf(true))
        run(
            button(ButtonStep.Confirm(), ButtonStep.EditPages(edits = listOf(PropertyEdit(columnId = done.id, op = EditOp.CHECK)))),
            ButtonPlace(pageId = null, rowId = pane.id),
            host
        )
        assertEquals("true", value(pane.id, done))
    }

    @Test
    fun pagesAndLinksOpenAtTheEndTheLastPageWins() = runBlocking<Unit> {
        val other = PageEntity(id = "other", title = "Altra")
        pages.createPage(other)
        val host = FakeHost()
        run(
            button(
                ButtonStep.Open(pageId = page.id),
                ButtonStep.Open(url = "example.com/x"),
                ButtonStep.Open(pageId = other.id),
                ButtonStep.Open(url = "mailto:a@b.it")
            ),
            ButtonPlace(page.id),
            host
        )
        assertEquals(listOf("url:https://example.com/x", "url:mailto:a@b.it", "page:other"), host.opened)
    }

    @Test
    fun aPageInTheTrashDoesNotOpen() = runBlocking<Unit> {
        val other = PageEntity(id = "other", title = "Altra", trashedAt = 5L)
        pages.createPage(other)
        val host = FakeHost()
        val outcome = run(button(ButtonStep.Open(pageId = other.id)), ButtonPlace(page.id), host)
        assertEquals(listOf(ButtonProblem.NO_PAGE), outcome.problems)
        assertTrue(host.opened.isEmpty())
    }

    @Test
    fun notificationsOffAreSaid() {
        val host = FakeHost().apply { notifications = false }
        val outcome = run(button(ButtonStep.Notify(title = "Acqua", text = "Bevi")), ButtonPlace(page.id), host)
        assertEquals(listOf("Acqua" to "Bevi"), host.notified)
        assertEquals(listOf(ButtonProblem.NOTIFICATIONS_OFF), outcome.problems)
    }

    @Test
    fun theActionsRunInOrderAndSeeEachOther() = runBlocking<Unit> {
        // Prima nasce la riga, poi "Edit pages in" la trova fra le altre.
        val outcome = run(
            button(
                ButtonStep.AddPage(databaseId = tasks.id, title = "Nuova"),
                ButtonStep.EditPages(
                    databaseId = tasks.id,
                    filter = ButtonFilter(NAME_PROPERTY_ID, listOf("nuova")),
                    edits = listOf(PropertyEdit(columnId = count.id, op = EditOp.SET, value = "7"))
                )
            ),
            ButtonPlace(page.id)
        )
        assertEquals(2, outcome.done)
        val row = db.databaseDao().getRowsForPageOnce(tasks.id).single { it.title == "Nuova" }
        assertEquals("7", value(row.id, count))
        assertNull(outcome.problems.firstOrNull())
        assertFalse(outcome.cancelled)
    }
}
