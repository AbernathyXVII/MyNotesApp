package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.TagCell
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Rinominare un tag** (05/10/2026): la tastiera si chiudeva alla prima
 * lettera, perché ogni lettera rinominava il tag e la modifica, non
 * trovandolo più col nome di prima, tornava all'elenco. Ora si scrive
 * quanto si vuole, e il tag si rinomina una volta sola, uscendo.
 *
 * Qui le opzioni della colonna non cambiano mai, come in quell'attimo in
 * cui il database non ha ancora salvato il nome nuovo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class TagRenameTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val column = DatabaseColumnEntity(
        id = "col",
        pageId = "db",
        name = "Platform",
        type = ColumnType.SELECT,
        optionsJson = """[{"label":"PS5","color":"#3A6590"}]"""
    )

    /** (colonna, nome vecchio, nome nuovo) di ogni rinomina. */
    private val renames = mutableListOf<Triple<String, String, String>>()
    private var shown by mutableStateOf(true)

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        compose.setContent {
            NotionLocalTheme {
                if (shown) {
                    TagCell(
                        column = column,
                        value = "PS5",
                        allowMultiple = false,
                        onValueChange = {},
                        onAddOption = {},
                        onRenameOption = { id, old, new -> renames += Triple(id, old, new) },
                        onSetOptionColor = { _, _, _ -> },
                        onDeleteOption = { _, _ -> }
                    )
                }
            }
        }
        compose.onNodeWithText("PS5").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Edit tag").performClick()
        compose.waitForIdle()
    }

    private fun nameField() = compose.onNode(hasSetTextAction() and hasText("PS5", substring = true))

    /** Scrive `added` in fondo al nome, una lettera alla volta, come sulla tastiera. */
    private fun type(added: String) {
        added.indices.forEach { i ->
            nameField().performTextReplacement("PS5" + added.substring(0, i + 1))
            compose.waitForIdle()
            // La modifica è ancora lì, col campo del nome.
            compose.onNodeWithText("Delete").assertExists()
        }
    }

    @Test
    fun typingSeveralLettersKeepsTheEditorOpen() {
        type(" Pro")
        nameField().assert(hasText("PS5 Pro"))
        // Mentre si scrive il tag non si rinomina.
        assertEquals(emptyList<Triple<String, String, String>>(), renames)
    }

    @Test
    fun doneRenamesOnce() {
        type(" Pro")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals(listOf(Triple("col", "PS5", "PS5 Pro")), renames)
    }

    @Test
    fun closingTheWindowKeepsTheNewName() {
        type(" Slim")
        // Come chiudere la finestra: la modifica se ne va.
        shown = false
        compose.waitForIdle()
        assertEquals(listOf(Triple("col", "PS5", "PS5 Slim")), renames)
    }
}
