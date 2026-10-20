package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.darkTextOn
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.TagCell
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Il secondo colore dei tag** (20/10/2026): nella modifica di un tag, "Add a
 * second color" apre lo spettro; poi la linea in mezzo si accende e si spegne,
 * e il secondo colore si toglie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class TagSecondColorTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var column by mutableStateOf(
        DatabaseColumnEntity(
            id = "col",
            pageId = "db",
            name = "Type",
            type = ColumnType.MULTI_SELECT,
            optionsJson = """[{"label":"Grass","color":"#3F6F55"}]"""
        )
    )

    /** (etichetta, secondo colore, linea) di ogni modifica; e la si salva, come l'app. */
    private val calls = mutableListOf<Triple<String, String?, Boolean>>()

    private fun openEditor() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        compose.setContent {
            NotionLocalTheme {
                TagCell(
                    column = column,
                    value = "Grass",
                    allowMultiple = true,
                    onValueChange = {},
                    onAddOption = {},
                    onRenameOption = { _, _, _ -> },
                    onSetOptionColor = { _, _, _ -> },
                    onDeleteOption = { _, _ -> },
                    onMoveOptions = { _, _ -> },
                    onSetOptionSecondColor = { id, label, hex, line ->
                        assertEquals("col", id)
                        calls += Triple(label, hex, line)
                        val saved = Json.decodeFromString<List<SelectOption>>(column.optionsJson)
                        val updated = saved.map { if (it.label == label) it.copy(secondColor = hex, colorLine = hex != null && line) else it }
                        column = column.copy(optionsJson = Json.encodeToString(updated))
                    }
                )
            }
        }
        compose.onAllNodesWithText("Grass").onFirst().performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Edit tag").performClick()
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun addASecondColorFromTheSpectrum() {
        openEditor()
        tap("Add a second color")
        // Lo spettro parte dal primo colore.
        compose.onNode(hasSetTextAction() and hasText("#3F6F55")).performTextReplacement("#984F49")
        compose.onNodeWithText("Apply").performClick()
        compose.waitForIdle()
        assertEquals(listOf(Triple("Grass", "#984F49", false)), calls)
        compose.onNodeWithText("Second color").performScrollTo().assertExists()
        compose.onNodeWithText("#984F49").assertExists()
        compose.onNodeWithText("Line between colors").performScrollTo().assertExists()
    }

    @Test
    fun theLineGoesOnAndTheSecondColorGoesAway() {
        column = column.copy(optionsJson = """[{"label":"Grass","color":"#3F6F55","secondColor":"#984F49"}]""")
        openEditor()
        tap("Line between colors")
        tap("Remove second color")
        assertEquals(listOf(Triple("Grass", "#984F49", true), Triple("Grass", null, false)), calls)
        compose.onNodeWithText("Add a second color").performScrollTo().assertExists()
    }

    @Test
    fun theTextOnTwoColorsFollowsTheirAverage() {
        // Due colori scuri dei tag di Notion: scritta bianca.
        assertFalse(darkTextOn("#3F6F55", "#984F49"))
        assertTrue(darkTextOn("#FFEB3B", "#FFFFFF"))
        // Bianco e nero: la media è appena sotto la metà.
        assertFalse(darkTextOn("#FFFFFF", "#000000"))
        assertTrue(darkTextOn("#FFFFFF", "#404040"))
    }

    @Test
    fun tagsSavedBeforeHaveOneColor() {
        val old = Json.decodeFromString<List<SelectOption>>("""[{"label":"PS5","color":"#3A6590"}]""")
        assertEquals(listOf(SelectOption("PS5", "#3A6590")), old)
        assertNull(old.single().secondColor)
        assertFalse(old.single().colorLine)
    }
}
