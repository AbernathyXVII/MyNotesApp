package com.gabriele.notionlocal.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.VersionReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId

/**
 * **"Page history"** (02/11/2026, chiesto dall'utente: "Updates ti mostra
 * cosa è cambiato, ma non riporta una pagina a com'era ieri"): le versioni
 * di una pagina su un database vero. Una nasce solo se la pagina è
 * cambiata; ripristinandone una torna tutto (titolo, toggle col suo
 * contenuto, tabelle, disegni), com'era prima finisce nella cronologia, e
 * le sottopagine nate dopo non si perdono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageVersionsTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository
    private val zone = ZoneId.of("Europe/Rome")

    private val page = PageEntity(id = "p", title = "Diario")

    private fun text(id: String, words: String, order: Int, parent: String? = null, type: BlockType = BlockType.PARAGRAPH) =
        BlockEntity(id = id, pageId = "p", parentBlockId = parent, type = type, textJson = """[{"text":"$words"}]""", orderIndex = order)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PageRepository(db)
        repo.createPage(page)
        db.blockDao().insertAll(
            listOf(
                text("uno", "Primo paragrafo", 0),
                text("lista", "Spesa", 1, type = BlockType.TOGGLE),
                text("latte", "Latte", 0, parent = "lista", type = BlockType.BULLET_LIST_ITEM),
                text("pane", "Pane", 1, parent = "lista", type = BlockType.BULLET_LIST_ITEM),
                BlockEntity(id = "tab", pageId = "p", type = BlockType.TABLE, orderIndex = 2)
            )
        )
        db.tableCellDao().insertCell(TableCellEntity(id = "c1", blockId = "tab", rowIndex = 0, colIndex = 0, text = "Lunedì"))
        db.inkDao().insertAll(listOf(InkStrokeEntity(id = "s1", pageId = "p", anchor = "uno", tool = InkTool.PEN, color = 1L, width = 2f, points = "1,1;2,2")))
    }

    @After
    fun tearDown() = db.close()

    private fun versions() = runBlocking { repo.observeVersions("p").first() }

    private fun blocks() = runBlocking { db.blockDao().getBlocksForPageOnce("p") }

    private fun save(reason: VersionReason = VersionReason.WHILE_OPEN, now: Long = System.currentTimeMillis()) =
        runBlocking { repo.saveVersion("p", reason, now, zone) }

    @Test
    fun aVersionOnlyWhenThePageChanged() = runBlocking<Unit> {
        assertTrue(save(VersionReason.OPENED))
        assertFalse("uguale all'ultima: niente", save())
        // Un toggle chiuso non è una modifica.
        db.blockDao().insertAll(listOf(blocks().single { it.id == "lista" }.copy(isExpanded = false)))
        assertFalse(save())
        db.blockDao().insertAll(listOf(text("due", "Secondo paragrafo", 3)))
        assertTrue(save())
        val list = versions()
        assertEquals(listOf(VersionReason.WHILE_OPEN, VersionReason.OPENED), list.map { it.reason })
        assertEquals(listOf(6, 5), list.map { it.lines })
        assertEquals("Diario", list.first().title)
    }

    @Test
    fun noVersionsForDatabasesNorForEmptyPages() = runBlocking<Unit> {
        repo.createPage(PageEntity(id = "db", title = "Giochi", isDatabase = true))
        repo.createPage(PageEntity(id = "vuota", title = "Vuota"))
        assertFalse(repo.saveVersion("db", VersionReason.OPENED))
        assertFalse(repo.saveVersion("vuota", VersionReason.OPENED))
        assertFalse(repo.saveVersion("non c'è", VersionReason.OPENED))
    }

    @Test
    fun oldVersionsGoAway() = runBlocking<Unit> {
        val day = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        assertTrue(save(now = now - 40 * day))
        db.blockDao().insertAll(listOf(text("due", "Secondo", 3)))
        assertTrue(save(now = now - 3 * day))
        db.blockDao().insertAll(listOf(text("tre", "Terzo", 4)))
        assertTrue(save(now = now))
        // Quella di quaranta giorni fa se ne va salvando l'ultima.
        assertEquals(listOf(now, now - 3 * day), versions().map { it.at })
    }

    @Test
    fun restoringBringsEverythingBackAndKeepsWhatItWas() = runBlocking<Unit> {
        save(VersionReason.OPENED)
        val yesterday = versions().single()
        // Poi la pagina si rovina: il titolo cambia, il toggle perde un figlio,
        // la tabella si svuota, il disegno se ne va, arriva una riga nuova.
        db.pageDao().setTitle("p", "Diario rovinato", 1L)
        db.blockDao().delete(blocks().single { it.id == "pane" })
        db.tableCellDao().insertCell(TableCellEntity(id = "c1", blockId = "tab", rowIndex = 0, colIndex = 0, text = ""))
        db.inkDao().deleteForPage("p")
        db.blockDao().insertAll(listOf(text("nuova", "Riga nuova", 5)))

        assertTrue(repo.restoreVersion("p", yesterday.id))

        assertEquals("Diario", db.pageDao().getById("p")!!.title)
        val back = blocks()
        assertEquals(setOf("uno", "lista", "latte", "pane", "tab"), back.map { it.id }.toSet())
        assertEquals("lista", back.single { it.id == "pane" }.parentBlockId)
        assertEquals("Lunedì", db.tableCellDao().getCellsForPageOnce("p").single().text)
        assertEquals(listOf("s1"), db.inkDao().getForPageOnce("p").map { it.id })

        // Com'era prima del ripristino è nella cronologia, e si può rimettere.
        val before = versions().first { it.reason == VersionReason.BEFORE_RESTORE }
        assertEquals("Diario rovinato", before.title)
        assertTrue(repo.restoreVersion("p", before.id))
        assertEquals("Diario rovinato", db.pageDao().getById("p")!!.title)
        assertEquals(setOf("uno", "lista", "latte", "tab", "nuova"), blocks().map { it.id }.toSet())
    }

    @Test
    fun subpagesBornLaterKeepTheirLinkAndTrashedOnesDoNotComeBack() = runBlocking<Unit> {
        repo.createPage(PageEntity(id = "vecchia", parentId = "p", title = "Vecchia"))
        db.blockDao().insertAll(listOf(BlockEntity(id = "link-vecchia", pageId = "p", type = BlockType.PAGE_LINK, linkedPageId = "vecchia", orderIndex = 3)))
        save(VersionReason.OPENED)
        val version = versions().single()
        // Dopo: una sottopagina nuova dentro il toggle, e quella vecchia nel cestino.
        repo.createPage(PageEntity(id = "nuova", parentId = "p", title = "Nuova"))
        db.blockDao().insertAll(listOf(BlockEntity(id = "link-nuova", pageId = "p", parentBlockId = "lista", type = BlockType.PAGE_LINK, linkedPageId = "nuova", orderIndex = 2)))
        repo.moveToTrash("vecchia")

        assertTrue(repo.restoreVersion("p", version.id))

        val back = blocks()
        assertTrue("la pagina nel cestino non torna", back.none { it.linkedPageId == "vecchia" })
        val link = back.single { it.linkedPageId == "nuova" }
        // In fondo alla pagina, fuori dal toggle.
        assertEquals(null, link.parentBlockId)
        assertEquals(back.filter { it.parentBlockId == null }.maxOf { it.orderIndex }, link.orderIndex)
    }

    @Test
    fun aVersionThatIsNotThereRestoresNothing() = runBlocking<Unit> {
        assertFalse(repo.restoreVersion("p", "non c'è"))
        assertEquals(5, blocks().size)
    }
}
