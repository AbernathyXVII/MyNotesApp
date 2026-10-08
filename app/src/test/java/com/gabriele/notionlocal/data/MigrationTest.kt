package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.data.entity.DateSettings
import com.gabriele.notionlocal.data.entity.TimeFormatKind
import com.gabriele.notionlocal.data.entity.dateSettings
import com.gabriele.notionlocal.data.entity.CellAlignment
import com.gabriele.notionlocal.data.entity.cellAlignment
import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.attrs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Le migrazioni su un database vero**, aperto dall'app com'è.
 *
 * Si costruisce un file a una versione vecchia con lo schema che Room
 * generava allora (`schema/vNN.sql`, preso dal codice generato a quel
 * commit), con dentro qualche pagina; poi lo si apre con
 * `AppDatabase.getInstance`, che è quello che fa il telefono
 * all'aggiornamento. Room esegue le migrazioni e **controlla lo schema
 * che ne esce contro quello che si aspetta**: se una colonna mancasse o
 * fosse di un tipo diverso, l'apertura fallirebbe qui invece che sul
 * telefono dell'utente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    @After
    fun tearDown() {
        AppDatabase.resetForTests()
    }

    /** Un file alla versione `version`, fatto con le istruzioni di `schema/v<version>.sql`. */
    private fun databaseAtVersion(version: Int): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("notion_local.db")
        file.parentFile?.mkdirs()
        file.delete()

        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        javaClass.classLoader!!.getResourceAsStream("schema/v$version.sql")!!.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("--") }
            .forEach { old.execSQL(it) }
        old.version = version
        old.close()
        return context
    }

    @Test
    fun from28TheAppOpensAndThePagesAreStillThere() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(28))
        val page = db.pageDao().getById("p1")!!
        assertEquals("Vecchia pagina", page.title)
        assertTrue(page.isDatabase)
        // Le colonne nuove ci sono, vuote: nessuna pagina di prima era una
        // vista, e nessuna scheda aveva un'inquadratura sua.
        assertNull(page.sourceDatabaseId)
        assertNull(page.viewHiddenColumnIds)
        assertNull(page.cardCoverScale)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from29MsYaHeiGoesBackToTheSystemFontAndTheOtherFontsStay() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(29))
        // Letta senza chiudere l'app: "MS_YAHEI" non esiste più nell'enum.
        val yahei = db.pageDao().getById("p1")!!
        assertEquals("Pagina in MS YaHei", yahei.title)
        assertNull(yahei.pageFont)
        // Il Songti resta fino alla 30; dalla 31 il font non è più della
        // pagina ma del testo, e questa pagina di testo non ne ha.
        assertNull(db.pageDao().getById("p2")!!.pageFont)
        assertNull(yahei.cardCoverScale)
        assertNull(yahei.cardCoverOffsetX)
        assertNull(yahei.cardCoverOffsetY)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from30ThePageFontMovesIntoItsText() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(30))
        val json = Json { ignoreUnknownKeys = true }
        fun spans(blockId: String) = runBlocking {
            json.decodeFromString<List<RichTextSpan>>(db.blockDao().getById(blockId)!!.textJson)
        }

        // La pagina non ha più font né corpo suoi...
        val page = db.pageDao().getById("p1")!!
        assertNull(page.pageFont)
        assertNull(page.pageFontSize)
        // ...li ha il suo testo, e si vede come prima. Il pezzo che aveva
        // già un font suo lo tiene, e prende solo il corpo.
        assertEquals(
            listOf(
                RichTextSpan(text = "Ciao ", font = "GARAMOND", size = 20),
                RichTextSpan(text = "mondo", bold = true, font = "CONSOLAS", size = 20)
            ),
            spans("b1")
        )
        // Lo span vuoto del collegamento tiene solo il colore.
        assertEquals(listOf(RichTextSpan(text = "", color = "#E03E3E")), spans("b2"))
        // Una pagina senza font resta com'era.
        assertEquals(listOf(RichTextSpan(text = "Normale")), spans("b3"))
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from31TheBlocksGetAnEmptyNotificationTime() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(31))
        assertEquals("Pagina alla 31", db.pageDao().getById("p1")!!.title)
        // La colonna nuova c'è, vuota: nessuna riga di prima era una notifica.
        val block = db.blockDao().getById("b1")!!
        assertNull(block.notifyAt)
        assertTrue(db.blockDao().getNotificationRows().isEmpty())
        assertTrue(db.databaseDao().getNotifyingDateCells().isEmpty())
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from32ThePagesGetATableForTheirDrawings() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(32))
        assertEquals("Pagina alla 32", db.pageDao().getById("p1")!!.title)
        // La riga-notifica della 32 resta com'era.
        assertEquals(1790000000000L, db.blockDao().getById("b1")!!.notifyAt)
        // La tabella nuova c'è, vuota, e ci si scrive.
        assertTrue(db.inkDao().getForPageOnce("p1").isEmpty())
        db.inkDao().insertAll(
            listOf(
                InkStrokeEntity(
                    pageId = "p1", anchor = "b1", tool = InkTool.PEN,
                    color = InkStrokeEntity.AUTO_COLOR, width = 2f, points = "1,2,50;3,4,50"
                )
            )
        )
        assertEquals("b1", db.inkDao().getForPageOnce("p1").single().anchor)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from33NoPageIsLockedWithAPassword() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(33))
        val page = db.pageDao().getById("p1")!!
        assertEquals("Pagina alla 33", page.title)
        assertFalse(page.isProtected)
        assertEquals(0, db.pageDao().countProtected())
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from34ThePagesStayInTheMainMenuAndTheNotebooksAreEmpty() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(34))
        val page = db.pageDao().getById("p1")!!
        assertEquals("Pagina alla 34", page.title)
        assertNull(page.notebookSectionId)
        assertTrue(db.notebookDao().getNotebooksOnce().isEmpty())
        assertEquals(0, db.notebookDao().countProtectedSections())
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from35TheNumbersLookAsBeforeAndNoViewHasCalculations() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(35))
        val column = db.databaseDao().getColumnsForPageOnce("db1").single()
        assertEquals("Number", column.name)
        assertNull(column.numberFormat)
        assertNull(column.decimalPlaces)
        assertNull(column.numberDisplay)
        assertTrue(column.showNumber)
        assertFalse(column.ringCentered)
        assertNull(db.pageDao().getById("db1")!!.viewCalculations)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from36TheCheckboxCenteredBeforeStaysCentered() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(36))
        val columns = db.databaseDao().getColumnsForPageOnce("db1").associateBy { it.id }
        assertNull(columns.getValue("c1").contentAlignment)
        assertEquals(CellAlignment.LEFT, columns.getValue("c1").cellAlignment)
        assertEquals(CellAlignment.CENTER, columns.getValue("c2").cellAlignment)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from37TheDatesKeepTheOldFormatAndTheTimeAt24Hours() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(37))
        val date = db.databaseDao().getColumnsForPageOnce("db1").single { it.id == "c3" }
        assertNull(date.dateFormat)
        assertNull(date.timeFormat)
        assertEquals(DateSettings(), date.dateSettings())
        assertEquals(TimeFormatKind.H24, date.dateSettings().time)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from38NoPageIsATemplateAndTheRowPageStillHasItsRow() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(38))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id -> assertFalse(pages.getById(id)!!.isTemplate) }
        assertEquals("r1", db.databaseDao().getRowsLinkingTo("rp1").single().id)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from39NoViewIsAChartYetAndItsSettingsAreEmpty() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(39))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id -> assertNull(pages.getById(id)!!.chartSettings) }
        assertEquals("Database alla 39", pages.getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from40NoTableHasSubGroupsYet() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(40))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id -> assertNull(pages.getById(id)!!.tableSubGroupColumnId) }
        assertEquals("Database alla 40", pages.getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from41TheSubGroupsSwitchStartsLikeTheGroupsOne() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(41))
        val pages = db.pageDao()
        // Acceso sulla pagina che nascondeva i gruppi vuoti, spento sulle altre.
        assertEquals(true, pages.getById("p1")!!.hideEmptySubGroups)
        assertEquals(false, pages.getById("db1")!!.hideEmptySubGroups)
        assertEquals(false, pages.getById("db1")!!.hideEmptyGroups)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from42EveryDatabaseHasOnlyItsOwnView() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(42))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id ->
            val page = pages.getById(id)!!
            assertNull(page.viewHostId)
            assertNull(page.viewName)
            assertNull(page.activeViewId)
        }
        assertEquals(emptyList<Any>(), pages.getViewsOnce("db1"))
        assertEquals("Database alla 42", pages.getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from43EveryColumnKeepsItsWidth() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(43))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id ->
            assertNull(pages.getById(id)!!.columnWidths)
            assertNull(pages.getById(id)!!.fitColumns)
        }
        assertEquals("Database alla 43", pages.getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from44EveryRowIsAMainRow() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(44))
        val row = db.databaseDao().getRowsForPageOnce("db1").single()
        assertEquals("Riga alla 44", row.title)
        assertNull(row.parentRowId)
        assertEquals(false, row.subRowsCollapsed)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from45NameStaysFirst() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(45))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id -> assertEquals(-1, pages.getById(id)!!.nameColumnOrder) }
        assertEquals("Database alla 45", pages.getById("db1")!!.title)
        assertEquals("Date", db.databaseDao().getColumnsForPageOnce("db1").single().name)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from46EveryColumnKeepsItsType() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(46))
        val column = db.databaseDao().getColumnsForPageOnce("db1").single()
        assertEquals(com.gabriele.notionlocal.data.entity.ColumnType.DATE, column.type)
        assertNull(column.durationFormat)
        assertEquals(-1, db.pageDao().getById("db1")!!.nameColumnOrder)
        assertEquals("Database alla 46", db.pageDao().getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from47EveryTableKeepsItsCellsOnOneLine() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(47))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id -> assertEquals(0, pages.getById(id)!!.cellOverflow) }
        assertEquals("Database alla 47", pages.getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from48EveryBoardKeepsItsCardsPlain() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(48))
        val pages = db.pageDao()
        listOf("p1", "db1", "rp1").forEach { id -> assertEquals(false, pages.getById(id)!!.colorBoardCards) }
        assertEquals("Database alla 48", pages.getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from49NoRowHasAnEventColor() = runBlocking<Unit> {
        // Il colore degli eventi del calendario (30/09/2026): nessuna riga di
        // prima ne ha uno, restano del colore di sempre.
        val db = AppDatabase.getInstance(databaseAtVersion(49))
        val row = db.databaseDao().getRowById("r1")!!
        assertEquals("Riga alla 49", row.title)
        assertNull(row.eventColor)
        assertEquals("Database alla 49", db.pageDao().getById("db1")!!.title)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from50TheSortStaysAndThereIsNoSecondRule() = runBlocking<Unit> {
        // La seconda regola di ordinamento (05/10/2026): il database
        // ordinato per la sua colonna, all'indietro, resta così, e nessuno
        // ha una seconda regola.
        val db = AppDatabase.getInstance(databaseAtVersion(50))
        val database = db.pageDao().getById("db1")!!
        assertEquals("Database alla 50", database.title)
        assertEquals("c1", database.sortColumnId)
        assertTrue(database.sortDescending)
        listOf("p1", "db1", "rp1").forEach { id ->
            val page = db.pageDao().getById(id)!!
            assertNull(page.thenSortColumnId)
            assertEquals(false, page.thenSortDescending)
        }
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from51TheBlocksHaveNoExtrasAndKeepTheirText() = runBlocking<Unit> {
        // I blocchi nuovi del 06/10/2026: la riga che c'era resta com'era,
        // con le voci in più vuote.
        val db = AppDatabase.getInstance(databaseAtVersion(51))
        assertEquals("Pagina alla 51", db.pageDao().getById("p1")!!.title)
        val block = db.blockDao().getById("b1")!!
        assertEquals(BlockType.PARAGRAPH, block.type)
        assertEquals("[{\"text\":\"Riga alla 51\"}]", block.textJson)
        assertEquals("{}", block.attrsJson)
        assertEquals(BlockAttrs(), block.attrs)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from52NoPageHasAMaterialIconAndTheEmojisStay() = runBlocking<Unit> {
        // Le icone di Material (08/10/2026): nessuna pagina ne ha una, e
        // l'emoji scelta e l'icona del riquadro restano com'erano.
        val db = AppDatabase.getInstance(databaseAtVersion(52))
        val page = db.pageDao().getById("p1")!!
        assertEquals("Pagina alla 52", page.title)
        assertNull(page.iconSymbol)
        val cat = db.pageDao().getById("p2")!!
        assertEquals("🐈", cat.icon)
        assertNull(cat.iconSymbol)
        val callout = db.blockDao().getById("b1")!!
        assertEquals(BlockType.CALLOUT, callout.type)
        assertEquals(BlockAttrs(icon = "🔥"), callout.attrs)
        assertEquals(53, db.openHelper.readableDatabase.version)
    }
}
