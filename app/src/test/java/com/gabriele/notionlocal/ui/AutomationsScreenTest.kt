package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.PropertyEdit
import com.gabriele.notionlocal.data.entity.AutomationConfig
import com.gabriele.notionlocal.data.entity.AutomationEntity
import com.gabriele.notionlocal.data.entity.AutomationTrigger
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.Repeat
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.AUTOMATIONS_TAG
import com.gabriele.notionlocal.ui.screen.AutomationsSheet
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * **"Automations"** di un database (03/11/2026): l'elenco col quando a
 * parole, l'interruttore, e l'automazione che si toglie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class AutomationsScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.pageDao().insert(PageEntity(id = "todo", title = "To do", isDatabase = true))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "fatto", pageId = "todo", name = "Fatto", type = ColumnType.CHECKBOX, orderIndex = 0))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "todo", name = "Quando", type = ColumnType.DATE, orderIndex = 1))
        db.automationDao().upsert(
            AutomationEntity(
                id = "a1", databaseId = "todo", name = "Data di oggi", orderIndex = 0,
                configJson = AutomationConfig.encode(
                    AutomationConfig(
                        AutomationTrigger.PropertyEdited("fatto", listOf("true")),
                        listOf(ButtonStep.EditPages(edits = listOf(PropertyEdit(columnId = "quando", op = EditOp.TODAY))))
                    )
                )
            )
        )
        db.automationDao().upsert(
            AutomationEntity(
                id = "a2", databaseId = "todo", orderIndex = 1,
                configJson = AutomationConfig.encode(AutomationConfig(AutomationTrigger.Schedule(Repeat.WEEKLY, days = listOf(1), minute = 9 * 60)))
            )
        )
    }

    @After
    fun tearDown() {
        compose.waitForIdle()
        Thread.sleep(300)
        AppDatabase.resetForTests()
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
            condition()
        }
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theListSaysWhenAndTheSwitchAndDeleteWork() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                AutomationsSheet(databaseId = "todo", databaseTitle = "To do", factory = factory, onDismiss = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(AUTOMATIONS_TAG).fetchSemanticsNodes().isNotEmpty() }
        waitFor { shown("Data di oggi") }
        // Il quando a parole; quella senza nome si chiama col suo quando.
        waitFor { shown("Fatto becomes Checked · 1 action") }
        assertEquals(true, shown("Every week (Monday) at 09:00"))

        // Spenta dall'interruttore: nel database resta, spenta.
        val switches = compose.onAllNodes(isToggleable() and hasAnyAncestor(hasTestTag(AUTOMATIONS_TAG)))
        switches[0].assertIsOn()
        switches[0].performClick()
        waitFor { runBlocking { db.automationDao().getById("a1")?.enabled == false } }

        // Aperta e tolta.
        compose.onNodeWithText("Data di oggi").performClick()
        waitFor { shown("Edit automation") }
        assertEquals(true, shown("When"))
        compose.onNodeWithText("Delete automation").performClick()
        waitFor { runBlocking { db.automationDao().getById("a1") == null } }
        assertFalse(runBlocking { db.automationDao().getById("a2") } == null)
    }
}
