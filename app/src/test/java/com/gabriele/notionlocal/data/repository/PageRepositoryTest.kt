package com.gabriele.notionlocal.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import com.gabriele.notionlocal.data.repository.PageRepository.PageTreeNode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Le operazioni che toccano i dati, provate su un database vero (Room in
 * memoria, con le chiavi esterne accese come nell'app) invece che sul
 * telefono. Scritti il 24/09/2026, in una sessione cloud dove il telefono
 * non c'era: coprono spostare, duplicare e buttare pagine con il loro
 * Annulla e Ripristina, e le correzioni trovate in quella sessione.
 *
 * Ogni test costruisce la sua piccola pagina: il menu principale, una
 * pagina "Parent" con due righe di testo e in mezzo il collegamento a
 * "Child", e una pagina "Destination".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository

    private val root = PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "Root")
    private val parent = PageEntity(title = "Parent")
    private val child = PageEntity(title = "Child")
    private val destination = PageEntity(title = "Destination")

    private lateinit var childLink: BlockEntity

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PageRepository(db)
        listOf(root, parent, child, destination).forEach { repo.createPage(it) }
        link(root.id, parent.id, 0)
        link(root.id, destination.id, 1)
        paragraph(parent.id, 0, "before")
        childLink = link(parent.id, child.id, 1)
        paragraph(parent.id, 2, "after")
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- Costruzione ---

    private suspend fun link(inPage: String, to: String, order: Int, type: BlockType = BlockType.PAGE_LINK): BlockEntity {
        val block = BlockEntity(pageId = inPage, type = type, linkedPageId = to, orderIndex = order)
        repo.saveBlock(block)
        return block
    }

    private suspend fun paragraph(inPage: String, order: Int, text: String, parentBlockId: String? = null): BlockEntity {
        val block = BlockEntity(
            pageId = inPage,
            parentBlockId = parentBlockId,
            textJson = """[{"text":"$text"}]""",
            orderIndex = order
        )
        repo.saveBlock(block)
        return block
    }

    private fun node(pageId: String) = PageTreeNode(pageId, null, "", "", null, false, false)

    private suspend fun blocksOf(pageId: String) =
        db.blockDao().getBlocksForPageOnce(pageId).sortedBy { it.orderIndex }

    private suspend fun linksTo(pageId: String) = db.blockDao().getBlocksLinkingTo(pageId)

    /** Un database con una colonna e una riga la cui pagina è `rowPage`, con un valore nella cella. */
    private suspend fun databaseWithRow(rowPage: PageEntity): Triple<PageEntity, DatabaseRowEntity, DatabaseCellEntity> {
        val database = PageEntity(title = "Database", isDatabase = true)
        repo.createPage(database)
        link(parent.id, database.id, 3, BlockType.DATABASE_LINK)
        val column = DatabaseColumnEntity(pageId = database.id, name = "Status")
        db.databaseDao().insertColumn(column)
        repo.createPage(rowPage)
        val row = DatabaseRowEntity(pageId = database.id, title = rowPage.title, linkedPageId = rowPage.id)
        db.databaseDao().insertRow(row)
        val cell = DatabaseCellEntity(rowId = row.id, columnId = column.id, value = "Done")
        db.databaseDao().insertCell(cell)
        return Triple(database, row, cell)
    }

    // --- Move to ---

    @Test
    fun moveGoesToTheEndOfTheDestination_andUndoPutsItBackInItsPlace() = runBlocking<Unit> {
        paragraph(destination.id, 0, "already here")
        val moved = repo.movePageTo(child.id, node(destination.id))
        assertNotNull(moved)
        assertTrue(linksTo(child.id).none { it.pageId == parent.id })
        val last = blocksOf(destination.id).last()
        assertEquals(child.id, last.linkedPageId)
        assertEquals(BlockType.PAGE_LINK, last.type)

        val undone = repo.undoPageChange(moved!!)
        assertNotNull(undone)
        assertTrue(linksTo(child.id).none { it.pageId == destination.id })
        // Stesso blocco, stesso posto: fra "before" e "after".
        val texts = blocksOf(parent.id).map { it.linkedPageId ?: it.textJson }
        assertEquals(3, texts.size)
        assertEquals(child.id, texts[1])
        assertEquals(childLink.id, blocksOf(parent.id)[1].id)

        val redone = repo.redoPageChange(undone!!)
        assertNotNull(redone)
        assertEquals(listOf(destination.id), linksTo(child.id).map { it.pageId })
    }

    @Test
    fun movingTheSamePageToWhereItIsPutsItAtTheEnd() = runBlocking<Unit> {
        assertNotNull(repo.movePageTo(child.id, node(parent.id)))
        assertEquals(child.id, blocksOf(parent.id).last().linkedPageId)
        assertEquals(1, linksTo(child.id).size)
    }

    @Test
    fun aDatabaseArrivesAsAPageLink_andUndoShowsItOpenAgain() = runBlocking<Unit> {
        val database = PageEntity(title = "Tasks", isDatabase = true)
        repo.createPage(database)
        link(parent.id, database.id, 3, BlockType.DATABASE_LINK)

        val moved = repo.movePageTo(database.id, node(destination.id))!!
        assertEquals(BlockType.PAGE_LINK, linksTo(database.id).single().type)

        repo.undoPageChange(moved)
        val back = linksTo(database.id).single()
        assertEquals(parent.id, back.pageId)
        assertEquals(BlockType.DATABASE_LINK, back.type)
    }

    @Test
    fun aPageCannotGoInsideItsOwnSubpage() = runBlocking<Unit> {
        val grandChild = PageEntity(title = "Grandchild")
        repo.createPage(grandChild)
        link(child.id, grandChild.id, 0)

        val exclusions = repo.moveExclusions(child.id)
        assertTrue(child.id in exclusions.pageIds)
        assertTrue(grandChild.id in exclusions.pageIds)
        assertFalse(parent.id in exclusions.pageIds)

        assertNull(repo.movePageTo(child.id, node(grandChild.id)))
        assertNull(repo.movePageTo(child.id, node(child.id)))
        // Niente è cambiato.
        assertEquals(listOf(parent.id), linksTo(child.id).map { it.pageId })
    }

    @Test
    fun aRowPageLeavesItsDatabase_andUndoBringsBackTheRowWithItsValues() = runBlocking<Unit> {
        val rowPage = PageEntity(title = "Row page", isRowPage = true)
        val (database, row, cell) = databaseWithRow(rowPage)

        val moved = repo.movePageTo(rowPage.id, node(destination.id))!!
        assertNull(db.databaseDao().getRowById(row.id))
        assertFalse(db.pageDao().getById(rowPage.id)!!.isRowPage)
        assertEquals(listOf(destination.id), linksTo(rowPage.id).map { it.pageId })

        repo.undoPageChange(moved)
        assertEquals(rowPage.id, db.databaseDao().getRowById(row.id)!!.linkedPageId)
        assertEquals("Done", db.databaseDao().getCellsForRowOnce(row.id).single().value)
        assertTrue(db.pageDao().getById(rowPage.id)!!.isRowPage)
        assertTrue(linksTo(rowPage.id).isEmpty())
        assertEquals(database.id, db.databaseDao().getRowById(row.id)!!.pageId)
        assertEquals(cell.id, db.databaseDao().getCellsForRowOnce(row.id).single().id)
    }

    @Test
    fun aPageCanGoIntoARowThatWasNeverOpened() = runBlocking<Unit> {
        val database = PageEntity(title = "Database", isDatabase = true)
        repo.createPage(database)
        val row = DatabaseRowEntity(pageId = database.id, title = "Never opened")
        db.databaseDao().insertRow(row)

        val moved = repo.movePageTo(child.id, PageTreeNode(null, row.id, "", "", null, false, false))
        assertNotNull(moved)
        val rowPageId = db.databaseDao().getRowById(row.id)!!.linkedPageId
        assertNotNull(rowPageId)
        assertEquals(listOf(rowPageId), linksTo(child.id).map { it.pageId })
    }

    // --- Duplicate ---

    @Test
    fun duplicateNextToALink_undoSendsTheCopyToTheTrash_redoBringsItBack() = runBlocking<Unit> {
        val copyId = repo.duplicatePage(
            child.id,
            DuplicateTarget.NextToOriginal,
            "copy",
            besideLinkBlockId = childLink.id
        ) { it }!!
        // Subito sotto il collegamento toccato.
        assertEquals(copyId, blocksOf(parent.id)[2].linkedPageId)

        val change = repo.describeCopy(copyId)
        val undone = repo.undoPageChange(change)!!
        assertNotNull(db.pageDao().getById(copyId)!!.trashedAt)
        assertTrue(linksTo(copyId).isEmpty())

        repo.redoPageChange(undone)
        assertNull(db.pageDao().getById(copyId)!!.trashedAt)
        assertEquals(copyId, blocksOf(parent.id)[2].linkedPageId)
        assertEquals(child.id, blocksOf(parent.id)[1].linkedPageId)
    }

    @Test
    fun duplicateOfARowPage_undoRemovesTheNewRow_redoPutsItBack() = runBlocking<Unit> {
        val rowPage = PageEntity(title = "Row page", isRowPage = true)
        val (database, _, _) = databaseWithRow(rowPage)
        val copyId = repo.duplicatePage(rowPage.id, DuplicateTarget.NextToOriginal, "copy") { it }!!
        assertEquals(2, db.databaseDao().getRowsForPageOnce(database.id).size)

        val undone = repo.undoPageChange(repo.describeCopy(copyId))!!
        assertEquals(1, db.databaseDao().getRowsForPageOnce(database.id).size)
        assertFalse(db.pageDao().getById(copyId)!!.isRowPage)

        repo.redoPageChange(undone)
        val rows = db.databaseDao().getRowsForPageOnce(database.id)
        assertEquals(2, rows.size)
        val copyRow = rows.single { it.linkedPageId == copyId }
        assertEquals("Done", db.databaseDao().getCellsForRowOnce(copyRow.id).single().value)
        assertTrue(db.pageDao().getById(copyId)!!.isRowPage)
    }

    // --- Move to trash ---

    @Test
    fun trashUndoTakesItOutOfTheTrashAndBackInItsPlace() = runBlocking<Unit> {
        val trashed = repo.moveToTrash(child.id)!!
        assertNotNull(db.pageDao().getById(child.id)!!.trashedAt)
        assertTrue(linksTo(child.id).isEmpty())

        val undone = repo.undoPageChange(trashed)!!
        assertNull(db.pageDao().getById(child.id)!!.trashedAt)
        assertEquals(childLink.id, blocksOf(parent.id)[1].id)

        repo.redoPageChange(undone)
        assertNotNull(db.pageDao().getById(child.id)!!.trashedAt)
    }

    @Test
    fun undoOfAPageDeletedForeverMeanwhileDoesNothing() = runBlocking<Unit> {
        val trashed = repo.moveToTrash(child.id)!!
        repo.deletePermanently(child.id)
        assertNull(repo.undoPageChange(trashed))
    }

    // --- Annulla con le foto dei blocchi ---

    @Test
    fun undoSnapshotKeepsTheTextOfTheTables() = runBlocking<Unit> {
        val table = BlockEntity(pageId = parent.id, type = BlockType.TABLE, orderIndex = 3)
        repo.saveBlock(table)
        repo.setTableCellText(table.id, 0, 0, "kept")
        val snapshot = blocksOf(parent.id)

        repo.replaceAllBlocks(parent.id, snapshot)

        val cells: List<TableCellEntity> = db.tableCellDao().getCellsForBlockOnce(table.id)
        assertEquals("kept", cells.single().text)
    }

    @Test
    fun undoSnapshotDoesNotBringBackALinkToADeletedDatabase() = runBlocking<Unit> {
        val database = PageEntity(title = "Deleted", isDatabase = true)
        repo.createPage(database)
        link(parent.id, database.id, 3, BlockType.DATABASE_LINK)
        val snapshot = blocksOf(parent.id)
        // Come `deleteDatabaseBlock`: via la pagina del database e il blocco.
        repo.deletePage(db.pageDao().getById(database.id)!!)
        blocksOf(parent.id).filter { it.type == BlockType.DATABASE_LINK }.forEach { repo.deleteBlock(it) }

        // Prima questa riga chiudeva l'app: chiave esterna verso una pagina che non c'è.
        repo.replaceAllBlocks(parent.id, snapshot)

        assertEquals(3, blocksOf(parent.id).size)
        assertTrue(blocksOf(parent.id).none { it.type == BlockType.DATABASE_LINK })
    }

    @Test
    fun undoSnapshotPutsBackAToggleWhoseChildComesFirstInOrderIndex() = runBlocking<Unit> {
        val toggle = BlockEntity(pageId = parent.id, type = BlockType.TOGGLE, orderIndex = 3)
        repo.saveBlock(toggle)
        val inside = paragraph(parent.id, 0, "inside", parentBlockId = toggle.id)
        // Come la foto di Annulla: ordinata solo per `orderIndex`, quindi il
        // figlio (0) prima del suo toggle (3).
        val snapshot = blocksOf(parent.id)
        assertTrue(snapshot.indexOfFirst { it.id == inside.id } < snapshot.indexOfFirst { it.id == toggle.id })

        // Prima questa riga chiudeva l'app: chiave esterna verso il genitore.
        repo.replaceAllBlocks(parent.id, snapshot)

        assertEquals(snapshot.map { it.id }.toSet(), blocksOf(parent.id).map { it.id }.toSet())
        assertEquals(toggle.id, db.blockDao().getById(inside.id)?.parentBlockId)
    }

    @Test
    fun undoSnapshotDoesNotBringBackALinkToAPageMovedElsewhere() = runBlocking<Unit> {
        val snapshot = blocksOf(parent.id)
        repo.movePageTo(child.id, node(destination.id))

        repo.replaceAllBlocks(parent.id, snapshot)

        assertEquals(listOf(destination.id), linksTo(child.id).map { it.pageId })
    }

    // --- Menu del blocco ---

    @Test
    fun duplicateBlockCopiesAToggleWithItsChildrenAndSubpages() = runBlocking<Unit> {
        val toggle = BlockEntity(pageId = parent.id, type = BlockType.TOGGLE, orderIndex = 3)
        repo.saveBlock(toggle)
        paragraph(parent.id, 0, "inside", parentBlockId = toggle.id)
        val sub = PageEntity(title = "Sub")
        repo.createPage(sub)
        repo.saveBlock(BlockEntity(pageId = parent.id, parentBlockId = toggle.id, type = BlockType.PAGE_LINK, linkedPageId = sub.id, orderIndex = 1))

        val copyId = repo.duplicateBlock(toggle.id) { it }!!

        val copyChildren = db.blockDao().getChildren(copyId)
        assertEquals(2, copyChildren.size)
        val copiedSub = copyChildren.single { it.type == BlockType.PAGE_LINK }.linkedPageId
        assertNotNull(copiedSub)
        assertTrue("la sottopagina della copia è un'altra pagina", copiedSub != sub.id)
        // La copia sta subito sotto l'originale.
        val order = blocksOf(parent.id).filter { it.parentBlockId == null }.map { it.id }
        assertEquals(order.indexOf(toggle.id) + 1, order.indexOf(copyId))
    }

    @Test
    fun unnestChildrenBringsTheToggleContentOutBelowIt() = runBlocking<Unit> {
        val toggle = BlockEntity(pageId = parent.id, type = BlockType.TOGGLE, orderIndex = 3)
        repo.saveBlock(toggle)
        val a = paragraph(parent.id, 0, "a", parentBlockId = toggle.id)
        val b = paragraph(parent.id, 1, "b", parentBlockId = toggle.id)

        repo.unnestAndConvert(toggle.id, BlockType.PARAGRAPH)

        val top = blocksOf(parent.id).filter { it.parentBlockId == null }.map { it.id }
        assertEquals(listOf(a.id, b.id), top.subList(top.indexOf(toggle.id) + 1, top.indexOf(toggle.id) + 3))
    }

    // --- Ricerca dentro un database ---

    @Test
    fun searchIndexHasTheTextInsideEachRowPage() = runBlocking<Unit> {
        val rowPage = PageEntity(title = "Row page", isRowPage = true)
        val (database, row, _) = databaseWithRow(rowPage)
        repo.saveBlock(
            BlockEntity(
                pageId = rowPage.id,
                textJson = """[{"text":"Farina e "},{"text":"zucchero","bold":true},{"text":" segreto","spoiler":true}]""",
                orderIndex = 0
            )
        )
        val table = BlockEntity(pageId = rowPage.id, type = BlockType.TABLE, orderIndex = 1)
        repo.saveBlock(table)
        repo.setTableCellText(table.id, 1, 1, "Budget 2026")
        val sub = PageEntity(title = "Ricette della nonna")
        repo.createPage(sub)
        link(rowPage.id, sub.id, 2)
        // Una riga mai aperta: nessuna pagina, nessun testo.
        db.databaseDao().insertRow(DatabaseRowEntity(pageId = database.id, title = "Never opened"))

        val index = DatabaseRepository(db).searchIndex(database.id)

        assertEquals(setOf(row.id), index.keys)
        val text = index.getValue(row.id)
        assertTrue(text.contains("Farina e zucchero segreto"))
        assertTrue(text.contains("Budget 2026"))
        assertTrue(text.contains("Ricette della nonna"))
        // I nomi dei campi del JSON non sono testo dell'utente.
        assertFalse(text.contains("bold"))
    }

    // --- Viste collegate ---

    @Test
    fun aLinkedViewStartsIdenticalToItsDatabaseButOwnsNoData() = runBlocking<Unit> {
        val rowPage = PageEntity(title = "Zelda", isRowPage = true)
        val (database, _, _) = databaseWithRow(rowPage)
        val status = db.databaseDao().getColumnsForPageOnce(database.id).single()
        db.databaseDao().insertColumn(DatabaseColumnEntity(pageId = database.id, name = "Secret", hidden = true))
        val secret = db.databaseDao().getColumnsForPageOnce(database.id).single { it.name == "Secret" }
        db.pageDao().update(
            db.pageDao().getById(database.id)!!.copy(
                tableGroupColumnId = status.id,
                filterColumnId = status.id,
                filterValue = "Done",
                sortDescending = true
            )
        )

        val viewId = repo.createLinkedView(database.id)!!
        val view = db.pageDao().getById(viewId)!!

        assertEquals(database.id, view.sourceDatabaseId)
        assertTrue(view.isDatabase)
        assertEquals(status.id, view.tableGroupColumnId)
        assertEquals(status.id, view.filterColumnId)
        assertEquals("Done", view.filterValue)
        assertTrue(view.sortDescending)
        assertEquals(secret.id, view.viewHiddenColumnIds)
        assertEquals("Database", view.title)
        // Nessun dato suo: righe e colonne restano quelle dell'origine.
        assertTrue(db.databaseDao().getRowsForPageOnce(viewId).isEmpty())
        assertTrue(db.databaseDao().getColumnsForPageOnce(viewId).isEmpty())

        // Una vista di una vista mostra il database vero.
        val viewOfView = repo.createLinkedView(viewId)!!
        assertEquals(database.id, db.pageDao().getById(viewOfView)!!.sourceDatabaseId)
    }

    @Test
    fun linkedViewsStayOutOfTheTreeTheSearchAndTheSourcePicker() = runBlocking<Unit> {
        val database = PageEntity(title = "Giochi", isDatabase = true)
        repo.createPage(database)
        link(parent.id, database.id, 3, BlockType.DATABASE_LINK)
        val viewId = repo.createLinkedView(database.id)!!
        link(destination.id, viewId, 0, BlockType.DATABASE_LINK)

        assertTrue(repo.treeChildren(destination.id).isEmpty())
        // Il triangolino nella barra laterale: una pagina con solo una vista non ne ha.
        assertEquals(0, db.blockDao().countLinksInPage(destination.id))
        assertEquals(listOf(database.id), repo.treeChildren(parent.id).mapNotNull { it.pageId }.filter { it == database.id || it == viewId })
        assertEquals(listOf(database.id), repo.search("Giochi", inContent = false, inTrash = false).mapNotNull { it.pageId })
        assertEquals(listOf(database.id), repo.linkableDatabases().map { it.page.id })
        assertEquals("Parent", repo.linkableDatabases().single().place)
    }

    @Test
    fun removingALinkedViewLeavesTheDatabaseAndItsRows() = runBlocking<Unit> {
        val rowPage = PageEntity(title = "Zelda", isRowPage = true)
        val (database, row, _) = databaseWithRow(rowPage)
        val viewId = repo.createLinkedView(database.id)!!
        // Come `deleteDatabaseBlock` su una vista collegata.
        repo.deletePage(db.pageDao().getById(viewId)!!)

        assertNull(db.pageDao().getById(viewId))
        assertEquals(row.id, db.databaseDao().getRowsForPageOnce(database.id).single().id)
        assertEquals(rowPage.id, db.databaseDao().getRowById(row.id)!!.linkedPageId)
    }

    @Test
    fun viewHiddenColumnsAreStoredOnTheViewOnly() = runBlocking<Unit> {
        val (database, _, _) = databaseWithRow(PageEntity(title = "Zelda", isRowPage = true))
        val column = db.databaseDao().getColumnsForPageOnce(database.id).single()
        val viewId = repo.createLinkedView(database.id)!!

        repo.setViewHiddenColumns(viewId, setOf(column.id))

        assertEquals(column.id, db.pageDao().getById(viewId)!!.viewHiddenColumnIds)
        assertFalse(db.databaseDao().getColumnsForPageOnce(database.id).single().hidden)
        repo.setViewHiddenColumns(viewId, emptySet())
        assertNull(db.pageDao().getById(viewId)!!.viewHiddenColumnIds)
    }

    @Test
    fun turnIntoSimpleDatabaseDeletesTheRowPagesButKeepsTheRows() = runBlocking<Unit> {
        val rowPage = PageEntity(title = "Row page", isRowPage = true)
        val (database, row, _) = databaseWithRow(rowPage)
        paragraph(rowPage.id, 0, "some content")
        assertEquals(1, repo.countRowPagesWithContent(database.id))

        repo.turnIntoSimpleDatabase(database.id)

        assertNull(db.pageDao().getById(rowPage.id))
        val kept = db.databaseDao().getRowById(row.id)!!
        assertNull(kept.linkedPageId)
        assertEquals("Done", db.databaseDao().getCellsForRowOnce(row.id).single().value)
        assertTrue(db.pageDao().getById(database.id)!!.isSimpleDatabase)
    }
}
