package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Il database sul PC** (dal 04/11/2026): lo stesso di Room del telefono,
 * aperto con l'SQLite dentro l'app, in una cartella temporanea (quella vera di
 * PageFlow sul PC non si tocca). Si scrive una pagina con una riga, la si
 * rilegge, e la si ritrova anche chiudendo e riaprendo il file.
 */
class DesktopDatabaseTest {
    private val dir: File = Files.createTempDirectory("pageflow-db").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun aPageWrittenOnThePcIsReadBackAfterReopening() = runBlocking {
        val db = AppDatabase.getInstance(context)
        db.pageDao().insert(PageEntity(id = "p", title = "Scritta sul PC"))
        db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"Ciao da Windows"}]""", orderIndex = 0))
        assertEquals("Scritta sul PC", db.pageDao().getById("p")?.title)

        AppDatabase.closeInstance()
        assertTrue("il file del database non c'è", File(dir, "notion_local.db").isFile)
        val reopened = AppDatabase.getInstance(context)
        assertEquals("Scritta sul PC", reopened.pageDao().getById("p")?.title)
        val blocks = reopened.blockDao().getBlocksForPageOnce("p")
        assertEquals(listOf("""[{"text":"Ciao da Windows"}]"""), blocks.map { it.textJson })
    }
}
