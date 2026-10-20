package com.gabriele.notionlocal.ui

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.TableBlockContent
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.fail
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **La barra sotto una tabella** (21/10/2026): "+ Row", "+ Column" e il
 * cestino solo mentre si scrive in una casella, non in mezzo al testo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class TableBarTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Articolo")
    private val table = BlockEntity(pageId = page.id, type = BlockType.TABLE, tableRows = 2, tableCols = 2, orderIndex = 0)

    @Before
    fun setUp() = runBlocking<Unit> {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insert(table)
        listOf("Previous", "Next", "-", "Yoyogi Park").forEachIndexed { i, text ->
            db.tableCellDao().insertCell(TableCellEntity(blockId = table.id, rowIndex = i / 2, colIndex = i % 2, text = text))
        }
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("la tabella") { viewModel.blocks.value.size == 1 }
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    @Test
    fun theBarShowsOnlyWhileWritingInTheTable() {
        lateinit var focus: FocusManager
        compose.setContent {
            focus = LocalFocusManager.current
            NotionLocalTheme {
                TableBlockContent(block = viewModel.blocks.value.single(), depth = 0, viewModel = viewModel)
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Previous")).fetchSemanticsNodes().isNotEmpty() }
        // Da leggere: niente barra.
        compose.onNodeWithText("+ Row").assertDoesNotExist()
        compose.onNodeWithText("+ Column").assertDoesNotExist()

        // Il dito in una casella: la barra c'è, e funziona.
        compose.onNodeWithText("Next").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("+ Column").assertExists()
        compose.onNodeWithText("+ Row").assertExists().performClick()
        waitFor("la riga in più") { runBlocking { db.blockDao().getById(table.id)!!.tableRows } == 3 }
        compose.onNodeWithText("+ Row").assertExists()

        // Chiusa la tastiera, il cursore si spegne e la barra va via.
        compose.runOnIdle { focus.clearFocus() }
        compose.waitForIdle()
        compose.onNodeWithText("+ Row").assertDoesNotExist()
    }
}
