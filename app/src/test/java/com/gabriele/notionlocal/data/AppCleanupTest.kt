package com.gabriele.notionlocal.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.entity.PageEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** "Elimina tutto il contenuto" e "Svuota la cache", sul database e sui file veri. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppCleanupTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        AppDatabase.resetForTests()
    }

    @Test
    fun deletingEverythingLeavesOnlyAnEmptyMySpace() = runBlocking<Unit> {
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.pageDao().insert(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "Il mio spazio", icon = "🌳"))
        db.pageDao().insert(PageEntity(id = "p1", title = "Pagina"))
        db.blockDao().insert(BlockEntity(id = "b1", pageId = "p1", orderIndex = 0, textJson = """[{"text":"Ciao"}]"""))
        db.inkDao().insertAll(
            listOf(InkStrokeEntity(pageId = "p1", anchor = "b1", tool = InkTool.PEN, color = 0, width = 2f, points = "1,2,50"))
        )
        val image = File(context.filesDir, "page-images").apply { mkdirs() }.resolve("x.img").apply { writeText("x") }

        AppCleanup.deleteAllContent(context)

        val root = db.pageDao().getById(PageEntity.ROOT_PAGE_ID)!!
        assertEquals("My Space", root.title)
        assertNull(db.pageDao().getById("p1"))
        assertNull(db.blockDao().getById("b1"))
        assertTrue(db.inkDao().getForPageOnce("p1").isEmpty())
        assertFalse(image.exists())
    }

    @Test
    fun clearingTheCacheEmptiesItAndNothingElse() = runBlocking<Unit> {
        File(context.cacheDir, "temp").apply { mkdirs() }.resolve("a.bin").writeBytes(ByteArray(1000))
        val kept = File(context.filesDir, "keep.txt").apply { writeText("resta") }
        assertTrue(AppCleanup.cacheBytes(context) >= 1000)

        AppCleanup.clearCache(context)

        assertEquals(0L, AppCleanup.cacheBytes(context))
        assertTrue(kept.exists())
    }
}
