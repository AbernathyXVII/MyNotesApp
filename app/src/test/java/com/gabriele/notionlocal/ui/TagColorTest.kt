package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.TAG_COLORS
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
 * **Il colore dei tag dallo spettro** (05/10/2026): nella modifica di un
 * tag, "Color" apre lo stesso selettore degli altri colori dell'app, e
 * il colore scelto — anche fuori dai dieci di Notion — arriva al tag.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class TagColorTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val column = DatabaseColumnEntity(
        id = "col",
        pageId = "db",
        name = "Type",
        type = ColumnType.MULTI_SELECT,
        optionsJson = """[{"label":"Grass","color":"#3F6F55"}]"""
    )

    /** (colonna, etichetta, colore) dell'ultimo colore dato. */
    private var picked: Triple<String, String, String>? = null

    @Before
    fun setUp() {
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
                    onSetOptionColor = { id, label, hex -> picked = Triple(id, label, hex) },
                    onDeleteOption = { _, _ -> },
                    onMoveOptions = { _, _ -> }
                )
            }
        }
        // La cella, poi la modifica del tag, poi la riga del colore.
        compose.onNodeWithText("Grass").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Edit tag").performClick()
        compose.waitForIdle()
    }

    private fun openSpectrum() {
        compose.onNodeWithText("Color").performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun theColorRowShowsTheTagColor() {
        compose.onNodeWithText("Color").performScrollTo().assertExists()
        assertEquals(1, compose.onAllNodesWithText("#3F6F55").fetchSemanticsNodes().size)
    }

    @Test
    fun anyColorFromTheSpectrumGoesToTheTag() {
        openSpectrum()
        // Lo spettro: Rosso, Verde, Blu e Hex, come per il testo e le celle.
        compose.onNodeWithText("Red").assertExists()
        compose.onNode(hasSetTextAction() and hasText("#3F6F55")).performTextReplacement("#FFEB3B")
        compose.onNodeWithText("Apply").performClick()
        compose.waitForIdle()
        assertEquals(Triple("col", "Grass", "#FFEB3B"), picked)
    }

    @Test
    fun noColorGoesBackToNotionDefault() {
        openSpectrum()
        compose.onNodeWithText("Default").performClick()
        compose.waitForIdle()
        assertEquals(Triple("col", "Grass", TAG_COLORS.first().hex), picked)
    }
}
