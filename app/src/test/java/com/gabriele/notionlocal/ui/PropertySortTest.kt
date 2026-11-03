package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.SortKind
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.encodeSortRules
import com.gabriele.notionlocal.data.entity.sortKindOfTexts
import com.gabriele.notionlocal.data.entity.sortRules
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PROPERTY_MENU_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Sort" nel menu di una proprietà** (02/11/2026, chiesto dall'utente:
 * "toccando su Sort mi ordini solo la proprietà che ho selezionato poco
 * prima"): non apre più la finestra di tutte le regole, ma i due versi di
 * quella proprietà, con le parole dei suoi valori — A → Z per le parole, dal
 * più grande al più piccolo per i numeri. Scelto, la proprietà diventa la
 * prima regola; le altre restano sotto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1000dp-h800dp-mdpi")
class PropertySortTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setUp(rules: List<SortRuleSpec> = emptyList()) = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
            db.pageDao().insert(
                PageEntity(id = "giochi", parentId = "pagina", title = "Giochi", isDatabase = true, sortRulesJson = encodeSortRules(rules))
            )
            db.databaseDao().insertColumn(
                DatabaseColumnEntity(
                    id = "console", pageId = "giochi", name = "Console", type = ColumnType.SELECT, orderIndex = 0,
                    optionsJson = """[{"label":"Xbox"},{"label":"Wii"}]"""
                )
            )
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "voto", pageId = "giochi", name = "Voto", type = ColumnType.NUMBER, orderIndex = 1))
            // Un testo che ha solo numeri: si ordina come i numeri.
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "anno", pageId = "giochi", name = "Anno", type = ColumnType.TEXT, orderIndex = 2))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "finito", pageId = "giochi", name = "Finito", type = ColumnType.CHECKBOX, orderIndex = 3))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "uscita", pageId = "giochi", name = "Uscita", type = ColumnType.DATE, orderIndex = 4))
            listOf(
                Triple("zelda", "Zelda", listOf("console" to "Wii", "voto" to "9", "anno" to "2017")),
                Triple("halo", "Halo", listOf("console" to "Xbox", "voto" to "7", "anno" to "2001")),
                Triple("mario", "Mario", listOf("console" to "Wii", "voto" to "10", "anno" to "1985"))
            ).forEachIndexed { i, (id, title, cells) ->
                db.databaseDao().insertRow(DatabaseRowEntity(id = id, pageId = "giochi", title = title, orderIndex = i))
                cells.forEach { (column, value) -> db.databaseDao().insertCell(DatabaseCellEntity(rowId = id, columnId = column, value = value)) }
            }
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "giochi", orderIndex = 0))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun open(rules: List<SortRuleSpec> = emptyList()) {
        setUp(rules)
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Zelda").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun menuShown() = compose.onAllNodesWithTag(PROPERTY_MENU_TAG).fetchSemanticsNodes().isNotEmpty()

    private fun shown(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** La voce [text] ha la spunta. */
    private fun checked(text: String) =
        compose.onAllNodes(hasText(text) and hasContentDescription("Selected")).fetchSemanticsNodes().isNotEmpty()

    private fun rules() = runBlocking { AppDatabase.getInstance(context).pageDao().getById("giochi")!!.sortRules() }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            Thread.sleep(20)
            condition()
        }
        compose.waitForIdle()
    }

    private fun topOf(text: String): Float = compose.onAllNodesWithText(text).fetchSemanticsNodes().first().positionInRoot.y

    /** Le righe dall'alto in basso. */
    private fun rowOrder(): List<String> = listOf("Zelda", "Halo", "Mario").sortedBy(::topOf)

    /** Il menu della proprietà [header], con "Sort" aperto. */
    private fun openSort(header: String) {
        compose.onNodeWithText(header).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Sort").performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun aWordStartsWithALetterANumberWithADigit() {
        assertEquals(SortKind.NUMBER, sortKindOfTexts(listOf("12", "3 km", "", "-4", "0,5")))
        assertEquals(SortKind.TEXT, sortKindOfTexts(listOf("12", "Zelda")))
        assertEquals(SortKind.TEXT, sortKindOfTexts(listOf("", " ")))
        assertEquals(SortKind.TEXT, sortKindOfTexts(emptyList()))
    }

    @Test
    fun sortOpensTheTwoChoicesOfThisPropertyInTheMenu() {
        open()
        openSort("Console")
        // Niente finestra di tutte le regole: il menu resta, coi due versi sotto "Sort".
        assertTrue(menuShown())
        assertTrue(shown("A → Z") && shown("Z → A"))
        assertTrue(topOf("Sort") < topOf("A → Z") && topOf("A → Z") < topOf("Z → A"))
        assertTrue("nessuna regola, niente da togliere", !shown("Remove sort"))
        assertTrue(!shown("Add a sort rule"))
        // Toccando "Sort" di nuovo si richiude.
        compose.onNodeWithText("Sort").performClick()
        compose.waitForIdle()
        assertTrue(!shown("A → Z"))
    }

    @Test
    fun wordsFromZToA() {
        open()
        openSort("Console")
        compose.onNodeWithText("Z → A").performClick()
        waitFor { rules() == listOf(SortRuleSpec("console", descending = true)) }
        assertTrue(!menuShown())
        // Xbox prima di Wii; fra le due Wii resta l'ordine di prima.
        waitFor { rowOrder() == listOf("Halo", "Zelda", "Mario") }
    }

    @Test
    fun numbersFromTheLargestFirst() {
        open()
        openSort("Voto")
        assertTrue(!shown("A → Z"))
        // Prima "dal più grande al più piccolo", come chiesto.
        assertTrue(topOf("Largest → smallest") < topOf("Smallest → largest"))
        compose.onNodeWithText("Largest → smallest").performClick()
        waitFor { rules() == listOf(SortRuleSpec("voto", descending = true)) }
        waitFor { rowOrder() == listOf("Mario", "Zelda", "Halo") }

        openSort("Voto")
        compose.onNodeWithText("Smallest → largest").performClick()
        waitFor { rules() == listOf(SortRuleSpec("voto", descending = false)) }
        waitFor { rowOrder() == listOf("Halo", "Zelda", "Mario") }
    }

    @Test
    fun aTextWithOnlyNumbersIsSortedAsNumbers() {
        open()
        openSort("Anno")
        assertTrue(shown("Largest → smallest") && !shown("A → Z"))
        compose.onNodeWithText("Largest → smallest").performClick()
        waitFor { rowOrder() == listOf("Zelda", "Halo", "Mario") }
    }

    @Test
    fun checkboxesAndDatesHaveTheirWords() {
        open()
        openSort("Finito")
        assertTrue(shown("Checked first") && shown("Unchecked first"))
        compose.onNodeWithText("Checked first").performClick()
        waitFor { rules() == listOf(SortRuleSpec("finito", descending = true)) }
        openSort("Uscita")
        assertTrue(shown("Newest first") && shown("Oldest first"))
    }

    @Test
    fun theNameToo() {
        open()
        openSort("Name")
        compose.onNodeWithText("Z → A").performClick()
        waitFor { rules() == listOf(SortRuleSpec(SORT_BY_NAME, descending = true)) }
        waitFor { rowOrder() == listOf("Zelda", "Mario", "Halo") }
    }

    @Test
    fun theOtherRulesStayBelowAndRemoveSortTakesOnlyThisOne() {
        open(listOf(SortRuleSpec("console"), SortRuleSpec("voto")))
        openSort("Voto")
        // È nelle regole ma non la prima: niente spunta, "Remove sort" sì.
        assertTrue(!checked("Smallest → largest") && !checked("Largest → smallest"))
        assertTrue(shown("Remove sort"))
        compose.onNodeWithText("Largest → smallest").performClick()
        waitFor { rules() == listOf(SortRuleSpec("voto", descending = true), SortRuleSpec("console")) }

        // Ora è la prima: la spunta sul verso scelto.
        openSort("Voto")
        assertTrue(checked("Largest → smallest") && !checked("Smallest → largest"))
        compose.onNodeWithText("Remove sort").performClick()
        waitFor { rules() == listOf(SortRuleSpec("console")) }
        assertTrue(!menuShown())
    }
}
