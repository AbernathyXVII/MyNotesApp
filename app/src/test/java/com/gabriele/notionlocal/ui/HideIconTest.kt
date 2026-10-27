package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Hide icon"** (27/10/2026, chiesto dall'utente): tenendo premuta
 * l'intestazione di una proprietà, la sua icona si nasconde, e torna con
 * "Show icon". Di suo si vede. Vale anche per il "Aa" della proprietà
 * principale. Qui in un database dentro una pagina, come sul telefono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class HideIconTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
            db.pageDao().insert(PageEntity(id = "agenda", parentId = "pagina", title = "Agenda", isDatabase = true))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "agenda", name = "Date", type = ColumnType.DATE, orderIndex = 0))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r1", pageId = "agenda", title = "Riunione", orderIndex = 0))
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "agenda", orderIndex = 0))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun stored() = runBlocking {
        val db = AppDatabase.getInstance(context)
        db.databaseDao().getColumnsForPageOnce("agenda").single().hideIcon to db.pageDao().getById("agenda")!!.hideNameIcon
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            Thread.sleep(20)
            condition()
        }
        compose.waitForIdle()
    }

    private fun menuOf(header: String) {
        compose.onNodeWithText(header).performTouchInput { longClick() }
        compose.waitForIdle()
    }

    @Test
    fun hideAndShowTheIconOfAProperty() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Date").fetchSemanticsNodes().isNotEmpty() }
        // Di suo si vede: nessuna icona nascosta.
        assertEquals(false to false, stored())

        menuOf("Date")
        compose.onNodeWithText("Hide icon").performClick()
        waitFor { stored().first }
        // Tenendo premuta di nuovo, la voce è diventata "Show icon".
        menuOf("Date")
        compose.onNodeWithText("Show icon").performClick()
        waitFor { !stored().first }

        // Il "Aa" della proprietà principale.
        assertEquals(1, compose.onAllNodesWithText("Aa").fetchSemanticsNodes().size)
        menuOf("Name")
        compose.onNodeWithText("Hide icon").performClick()
        waitFor { stored().second }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Aa").fetchSemanticsNodes().isEmpty() }
        assertEquals(false to true, stored())
    }
}
