package com.gabriele.notionlocal.desktop

import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.SearchViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **I ViewModel sul PC** (dal 04/11/2026): gli stessi del telefono, sul
 * database del PC in una cartella temporanea. Sul PC il "filo principale" dei
 * ViewModel è quello di Swing, la finestra: qui si aspetta che abbiano finito.
 */
class DesktopViewModelTest {
    private val dir: File = Files.createTempDirectory("pageflow-vm").toFile()
    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository

    @Before
    fun setUp() = runBlocking {
        val context = DesktopContext(dir)
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        repo = PageRepository(db)
        repo.createPage(PageEntity(id = "p", title = "Lista della spesa"))
        listOf("Latte", "Pane", "Mele").forEachIndexed { i, text ->
            repo.saveBlock(BlockEntity(pageId = "p", textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end) {
            if (condition()) return
            Thread.sleep(20)
        }
        fail("Non è successo: $what")
    }

    @Test
    fun theEditorOpensAPageOnThePc() {
        val viewModel = PageEditorViewModel(repo)
        viewModel.load("p")
        waitFor("le righe della pagina") { viewModel.blocks.value.size == 3 }
        assertEquals(listOf("""[{"text":"Latte"}]""", """[{"text":"Pane"}]""", """[{"text":"Mele"}]"""), viewModel.blocks.value.map { it.textJson })
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun theSearchFindsThePageOnThePc() {
        val viewModel = SearchViewModel(repo)
        viewModel.onQueryChange("Pane")
        waitFor("il risultato") { viewModel.results.value.any { it.pageId == "p" } }
        viewModel.viewModelScope.cancel()
    }
}
