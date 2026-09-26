package com.gabriele.notionlocal.data.security

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Le pagine bloccate con la password: la password salvata, **chi blocca
 * chi** (sottopagine, righe, viste collegate), la ricerca che non mostra il
 * contenuto chiuso, e il database dentro una pagina che diventa un
 * collegamento.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageLockTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        context.getDatabasePath("notion_local.db").delete()
        db = AppDatabase.getInstance(context)
        repository = PageRepository(db)
        AppLock.keepOnlyUnlocked(null)
        val pages = db.pageDao()
        val blocks = db.blockDao()
        pages.insert(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
        // My Space → Diario (bloccato) → Sottopagina; Diario contiene anche il database Spese.
        pages.insert(PageEntity(id = "diary", title = "Diario"))
        pages.insert(PageEntity(id = "sub", title = "Sottopagina"))
        pages.insert(PageEntity(id = "db", title = "Spese", isDatabase = true))
        pages.insert(PageEntity(id = "row", title = "Cena", isRowPage = true))
        pages.insert(PageEntity(id = "view", title = "Spese", isDatabase = true, sourceDatabaseId = "db"))
        pages.insert(PageEntity(id = "free", title = "Pagina libera"))
        blocks.insert(BlockEntity(id = "l1", pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "diary", orderIndex = 0))
        blocks.insert(BlockEntity(id = "l2", pageId = "diary", type = BlockType.PAGE_LINK, linkedPageId = "sub", orderIndex = 0))
        blocks.insert(BlockEntity(id = "l3", pageId = "diary", type = BlockType.DATABASE_LINK, linkedPageId = "db", orderIndex = 1))
        blocks.insert(BlockEntity(id = "t1", pageId = "diary", orderIndex = 2, textJson = """[{"text":"Il mio segreto"}]"""))
        blocks.insert(BlockEntity(id = "l4", pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.DATABASE_LINK, linkedPageId = "view", orderIndex = 1))
        blocks.insert(BlockEntity(id = "l5", pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "free", orderIndex = 2))
        db.databaseDao().insertRow(DatabaseRowEntity(id = "r1", pageId = "db", title = "Cena", linkedPageId = "row"))
    }

    @After
    fun tearDown() {
        AppLock.keepOnlyUnlocked(null)
        AppDatabase.resetForTests()
    }

    @Test
    fun thePasswordIsCheckedButNeverStored() {
        val stored = PasswordHash.create("gatto")
        assertTrue(PasswordHash.verify("gatto", stored))
        assertFalse(PasswordHash.verify("Gatto", stored))
        assertFalse(stored.contains("gatto"))
        // Un sale nuovo ogni volta: la stessa password non dà la stessa stringa.
        assertNotEquals(stored, PasswordHash.create("gatto"))
        assertFalse(PasswordHash.verify("gatto", "rovinata"))
    }

    @Test
    fun aLockedPageClosesEverythingInside() = runBlocking<Unit> {
        repository.setProtected("diary", true)
        assertEquals("diary", repository.lockingPageOf("diary"))
        assertEquals("diary", repository.lockingPageOf("sub"))
        assertEquals("diary", repository.lockingPageOf("db"))
        // La pagina di una riga del database che sta nella pagina bloccata.
        assertEquals("diary", repository.lockingPageOf("row"))
        assertNull(repository.lockingPageOf("free"))
        assertNull(repository.lockingPageOf(PageEntity.ROOT_PAGE_ID))
    }

    @Test
    fun aLockedDatabaseClosesItsRowsAndItsLinkedViewsAndBecomesALink() = runBlocking<Unit> {
        repository.setProtected("db", true)
        assertEquals("db", repository.lockingPageOf("row"))
        assertEquals("db", repository.lockingPageOf("view"))
        // Dentro la pagina era un database: ora è un collegamento, come con "Turn into page".
        assertEquals(BlockType.PAGE_LINK, db.blockDao().getById("l3")!!.type)
    }

    @Test
    fun theSearchDoesNotShowWhatIsLocked() = runBlocking<Unit> {
        assertTrue(repository.search("segreto", inContent = true, inTrash = false).any { it.pageId == "diary" })
        repository.setProtected("diary", true)
        assertTrue(repository.search("segreto", inContent = true, inTrash = false).isEmpty())
        // Il titolo di una pagina bloccata si trova (si vede anche nella barra), senza il testo.
        val byTitle = repository.search("Diario", inContent = true, inTrash = false).single()
        assertNull(byTitle.snippet)
        // Quello che sta dentro sparisce, come dalla barra.
        assertTrue(repository.search("Sottopagina", inContent = true, inTrash = false).isEmpty())
        // Aperta, si trova tutto.
        AppLock.unlock("diary")
        assertTrue(repository.search("segreto", inContent = true, inTrash = false).any { it.pageId == "diary" })
    }

    @Test
    fun theSidebarDoesNotShowWhatIsInsideALockedPage() = runBlocking<Unit> {
        repository.setProtected("diary", true)
        assertTrue(repository.treeChildren("diary").isEmpty())
        AppLock.unlock("diary")
        assertEquals(2, repository.treeChildren("diary").size)
    }

    @Test
    fun leavingForAPageThatIsNotInsideClosesItAgain() {
        AppLock.unlock("diary")
        AppLock.keepOnlyUnlocked("diary")
        assertTrue(AppLock.isUnlocked("diary"))
        AppLock.keepOnlyUnlocked(null)
        assertFalse(AppLock.isUnlocked("diary"))
    }
}
