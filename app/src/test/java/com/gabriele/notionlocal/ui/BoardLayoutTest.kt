package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.BoardLayout
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La bacheca coi sottogruppi** (14/10/2026, chiesto dall'utente): fasce
 * orizzontali, una per valore della seconda proprietà, ognuna con tutte le
 * colonne; e le colonne fatte anche da una casella, non solo da un tag.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1200dp-h2000dp-mdpi")
class BoardLayoutTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shop = DatabaseColumnEntity(
        id = "shop", pageId = "food", name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0,
        optionsJson = """[{"label":"IPERPOLI","color":"#E03E3E"},{"label":"Lidl","color":"#3A6590"}]"""
    )
    private val kind = DatabaseColumnEntity(
        id = "kind", pageId = "food", name = "Tipologia", type = ColumnType.SELECT, orderIndex = 1,
        optionsJson = """[{"label":"Cibo","color":"#3F6F55"},{"label":"Casa","color":"#9065B0"},{"label":"Altro","color":"#787774"}]"""
    )
    private val bought = DatabaseColumnEntity(id = "bought", pageId = "food", name = "Preso", type = ColumnType.CHECKBOX, orderIndex = 2)

    private val pasta = DatabaseRowEntity(id = "pasta", pageId = "food", title = "Pasta", orderIndex = 0)
    private val shampoo = DatabaseRowEntity(id = "shampoo", pageId = "food", title = "Shampoo", orderIndex = 1)
    private val salt = DatabaseRowEntity(id = "salt", pageId = "food", title = "Sale", orderIndex = 2)

    private val state = DatabaseTableState(
        columns = listOf(shop, kind, bought),
        rows = listOf(pasta, shampoo, salt),
        cellValues = mapOf(
            ("pasta" to "shop") to "IPERPOLI", ("pasta" to "kind") to "Cibo",
            ("shampoo" to "shop") to "Lidl", ("shampoo" to "kind") to "Casa",
            ("salt" to "shop") to "IPERPOLI", ("salt" to "kind") to "Cibo", ("salt" to "bought") to "true"
        )
    )

    private var addedInGroup: String? = null
    private var addedInSubGroup: Pair<String, String>? = null

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    private fun show(group: DatabaseColumnEntity, sub: DatabaseColumnEntity? = null, hideEmptySubGroups: Boolean = true, hideEmptyGroups: Boolean = false) {
        compose.setContent {
            NotionLocalTheme {
                BoardLayout(
                    state = state,
                    groupColumn = group,
                    subGroupColumn = sub,
                    hideEmptyGroups = hideEmptyGroups,
                    hideEmptySubGroups = hideEmptySubGroups,
                    newRowTick = 0,
                    onOpenRow = {},
                    onRowLongPress = {},
                    onAddRowInGroup = { addedInGroup = it },
                    onAddRowInSubGroup = { group, subValue -> addedInSubGroup = group to subValue },
                    onAddGroupProperty = {}
                )
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(text: String) = compose.onNodeWithText(text).getBoundsInRoot()

    /** La scheda `card` sta nella colonna che ha in cima `header` (le colonne sono larghe 260dp). */
    private fun assertInColumn(card: String, header: String) {
        val left = bounds(header).left
        val cardLeft = bounds(card).left
        assertTrue("$card non è sotto $header", cardLeft >= left && cardLeft < left + 240.dp)
    }

    @Test
    fun eachCardSitsInItsColumnInsideItsSubGroup() {
        show(shop, kind)
        // Le fasce coi loro nomi, in ordine; "Altro" è vuota e si nasconde.
        assertTrue(bounds("Cibo").top < bounds("Casa").top)
        assertEquals(0, compose.onAllNodesWithText("Altro").fetchSemanticsNodes().size)
        // Pasta sotto IPERPOLI nella fascia "Cibo", Shampoo sotto Lidl in "Casa".
        assertInColumn("Pasta", "IPERPOLI")
        assertInColumn("Shampoo", "Lidl")
        assertTrue(bounds("Cibo").top < bounds("Pasta").top && bounds("Pasta").top < bounds("Casa").top)
        assertTrue(bounds("Casa").top < bounds("Shampoo").top)
        // Il valore delle colonne e quello delle fasce non si ripetono sulle schede.
        assertEquals(1, compose.onAllNodesWithText("IPERPOLI").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("Cibo").fetchSemanticsNodes().size)
    }

    @Test
    fun emptySubGroupsCanBeShown() {
        show(shop, kind, hideEmptySubGroups = false)
        compose.onNodeWithText("Altro").assertIsDisplayed()
    }

    @Test
    fun tappingASubGroupClosesIt() {
        show(shop, kind)
        compose.onNodeWithText("Cibo").performClick()
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithText("Pasta").fetchSemanticsNodes().size)
        compose.onNodeWithText("Shampoo").assertIsDisplayed()
        compose.onNodeWithText("Cibo").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Pasta").assertIsDisplayed()
    }

    @Test
    fun newInsideASubGroupGetsBothValues() {
        show(shop, kind)
        // In ogni fascia un "New" per colonna: IPERPOLI, Lidl e quella senza
        // valore. Il quarto è il primo della fascia "Casa".
        compose.onAllNodesWithText("New")[3].performClick()
        compose.waitForIdle()
        assertEquals("IPERPOLI" to "Casa", addedInSubGroup)
    }

    @Test
    fun theBoardCanBeGroupedByACheckbox() {
        show(bought)
        assertInColumn("Sale", "Checked")
        assertInColumn("Pasta", "Unchecked")
        assertInColumn("Shampoo", "Unchecked")
        // Una casella non ha un "senza valore": le colonne sono due, e due i "New".
        assertEquals(2, compose.onAllNodesWithText("New").fetchSemanticsNodes().size)
        compose.onAllNodesWithText("New")[1].performClick()
        compose.waitForIdle()
        assertEquals("false", addedInGroup)
    }

    @Test
    fun withoutSubGroupsTheColumnWithoutAValueIsStillThere() {
        show(shop)
        compose.onNodeWithText("No Supermercato").assertIsDisplayed()
        assertEquals(3, compose.onAllNodesWithText("New").fetchSemanticsNodes().size)
        compose.onAllNodesWithText("New")[2].performClick()
        compose.waitForIdle()
        assertEquals("", addedInGroup)
    }

    @Test
    fun ofItsOwnTheBoardShowsTheEmptyColumns() {
        // "Altro" è vuota, e c'è la colonna senza valore.
        show(kind)
        assertEquals(1, compose.onAllNodesWithText("Altro").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("No Tipologia", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
    }

    @Test
    fun hideEmptyGroupsTakesAwayTheColumnsWithoutCards() {
        // Con "Hide empty groups" (21/10/2026) restano solo quelle con schede.
        show(kind, hideEmptyGroups = true)
        assertEquals(0, compose.onAllNodesWithText("Altro").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("No Tipologia", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        assertInColumn("Pasta", "Cibo")
        assertInColumn("Shampoo", "Casa")
    }

    @Test
    fun aBoardWithThousandsOfColumnsShowsTwelveAndLoadMore() {
        // Raggruppata per una data giorno per giorno (26/10/2026): l'app si
        // chiudeva per la memoria, con 4.000 colonne disegnate insieme.
        val day = DatabaseColumnEntity(id = "day", pageId = "food", name = "Giorno", type = ColumnType.DATE, orderIndex = 0)
        val rows = (0 until 30).map { DatabaseRowEntity(id = "r$it", pageId = "food", title = "Riga $it", orderIndex = it) } +
            DatabaseRowEntity(id = "senza", pageId = "food", title = "Senza data", orderIndex = 30)
        val cells = (0 until 30).associate { ("r$it" to "day") to java.time.LocalDate.of(2026, 1, 1).plusDays(it.toLong())
            .atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli().toString() }
        compose.setContent {
            NotionLocalTheme {
                androidx.compose.runtime.CompositionLocalProvider(com.gabriele.notionlocal.ui.screen.LocalRowLimits provides com.gabriele.notionlocal.ui.screen.RowLimits()) {
                    BoardLayout(
                        state = DatabaseTableState(columns = listOf(day), rows = rows, cellValues = cells),
                        groupColumn = day,
                        newRowTick = 0,
                        onOpenRow = {},
                        onRowLongPress = {},
                        onAddRowInGroup = {},
                        onAddGroupProperty = {}
                    )
                }
            }
        }
        compose.waitForIdle()
        assertEquals(12, (0 until 30).count { compose.onAllNodesWithText("Riga $it").fetchSemanticsNodes().isNotEmpty() })
        // La colonna senza data resta, per le schede nuove; le altre 18 dietro "Load more".
        assertEquals(1, compose.onAllNodesWithText("Senza data").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("18").fetchSemanticsNodes().size)
    }
}