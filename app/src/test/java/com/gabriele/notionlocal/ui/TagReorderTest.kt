package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.reorderedAs
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.TagCell
import com.gabriele.notionlocal.ui.screen.moveWhileDragging
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Riordinare i tag** (20/10/2026): "Reorder" in alto a destra nella finestra
 * dei tag, poi si trascinano le maniglie a tre lineette.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class TagReorderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun options(vararg labels: String) = labels.map { SelectOption(it, "#3A6590") }

    @Test
    fun reorderedAsFollowsTheNewOrder() {
        val tags = options("Fire", "Water", "Grass")
        assertEquals(listOf("Grass", "Fire", "Water"), tags.reorderedAs(listOf("Grass", "Fire", "Water")).map { it.label })
    }

    @Test
    fun aTagMissingFromTheOrderGoesLastAndAGoneOneDoesNotComeBack() {
        val tags = options("Fire", "Water", "Grass")
        // "Grass" creato mentre si trascinava, "Ice" eliminato nel frattempo.
        assertEquals(
            listOf("Water", "Fire", "Grass"),
            tags.reorderedAs(listOf("Water", "Ice", "Fire")).map { it.label }
        )
    }

    @Test
    fun theDraggedTagPassesANeighbourOnlyBeyondItsHalf() {
        val order = listOf("A", "B", "C", "D")
        val slot = { _: String -> 100f }
        assertEquals(order to 40f, moveWhileDragging(order, "A", 40f, slot))
        assertEquals(listOf("B", "A", "C", "D") to -40f, moveWhileDragging(order, "A", 60f, slot))
        assertEquals(listOf("B", "C", "A", "D") to 10f, moveWhileDragging(order, "A", 210f, slot))
        assertEquals(listOf("A", "D", "B", "C") to -20f, moveWhileDragging(order, "D", -220f, slot))
        // Oltre l'ultimo non si va: resta l'ultimo, spostato.
        assertEquals(listOf("B", "C", "D", "A") to 500f, moveWhileDragging(order, "A", 800f, slot))
    }

    private var column by mutableStateOf(
        DatabaseColumnEntity(
            id = "col",
            pageId = "db",
            name = "Type",
            type = ColumnType.MULTI_SELECT,
            optionsJson = Json.encodeToString(options("Fire", "Water", "Grass"))
        )
    )

    /** Ogni ordine salvato; e il database lo salva davvero, come l'app. */
    private val moves = mutableListOf<List<String>>()

    private fun openReorder() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        compose.setContent {
            NotionLocalTheme {
                TagCell(
                    column = column,
                    value = "Fire",
                    allowMultiple = true,
                    onValueChange = {},
                    onAddOption = {},
                    onRenameOption = { _, _, _ -> },
                    onSetOptionColor = { _, _, _ -> },
                    onDeleteOption = { _, _ -> },
                    onMoveOptions = { id, labels ->
                        assertEquals("col", id)
                        moves += labels
                        val saved = Json.decodeFromString<List<SelectOption>>(column.optionsJson)
                        column = column.copy(optionsJson = Json.encodeToString(saved.reorderedAs(labels)))
                    }
                )
            }
        }
        compose.onAllNodesWithText("Fire").onFirst().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Reorder").performClick()
        compose.waitForIdle()
    }

    private fun handles() = compose.onAllNodesWithContentDescription("Drag to reorder")

    /** Quanto dista una maniglia dalla successiva: un tag col suo filo. */
    private fun slot(): Float {
        val first = handles()[0].fetchSemanticsNode().boundsInRoot.top
        val second = handles()[1].fetchSemanticsNode().boundsInRoot.top
        return second - first
    }

    /** Trascina la maniglia `index` di `rows` tag, a piccoli passi come un dito. */
    private fun drag(index: Int, rows: Float) {
        val distance = slot() * rows
        handles()[index].performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, distance / 10)) }
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun draggingTheFirstTagPastTheSecondSavesTheNewOrder() {
        openReorder()
        assertEquals(3, handles().fetchSemanticsNodes().size)
        drag(0, 1.2f)
        assertEquals(listOf(listOf("Water", "Fire", "Grass")), moves)
    }

    @Test
    fun aShortDragSavesNothing() {
        openReorder()
        drag(1, 0.3f)
        assertEquals(emptyList<List<String>>(), moves)
    }

    @Test
    fun aSecondDragStartsFromTheSavedOrder() {
        openReorder()
        drag(2, -2.2f)
        drag(2, -1.2f)
        assertEquals(
            listOf(listOf("Grass", "Fire", "Water"), listOf("Grass", "Water", "Fire")),
            moves
        )
    }

    @Test
    fun doneGoesBackToTheTagList() {
        openReorder()
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Create a tag").assertExists()
        assertEquals(0, handles().fetchSemanticsNodes().size)
    }
}
