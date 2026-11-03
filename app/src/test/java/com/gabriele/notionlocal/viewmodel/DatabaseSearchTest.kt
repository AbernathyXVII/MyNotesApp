package com.gabriele.notionlocal.viewmodel

import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Quali righe tiene la ricerca dentro un database (la lente accanto a
 * Sort), e quali vanno illuminate per intero perché la parola non è nel
 * nome ma dentro. Vedi `searchRows`.
 */
class DatabaseSearchTest {

    private val text = DatabaseColumnEntity(id = "text", pageId = "db", name = "Notes", type = ColumnType.TEXT)
    private val tags = DatabaseColumnEntity(id = "tags", pageId = "db", name = "Tags", type = ColumnType.MULTI_SELECT)
    private val done = DatabaseColumnEntity(id = "done", pageId = "db", name = "Done", type = ColumnType.CHECKBOX)
    private val date = DatabaseColumnEntity(id = "date", pageId = "db", name = "When", type = ColumnType.DATE)
    private val columns = listOf(text, tags, done, date)

    private val shopping = DatabaseRowEntity(id = "r1", pageId = "db", title = "Lista della Spesa")
    private val trip = DatabaseRowEntity(id = "r2", pageId = "db", title = "Viaggio")
    private val recipe = DatabaseRowEntity(id = "r3", pageId = "db", title = "Ricetta")
    private val empty = DatabaseRowEntity(id = "r4", pageId = "db", title = "")
    private val rows = listOf(shopping, trip, recipe, empty)

    private val cells = mapOf(
        ("r2" to "text") to "Prenotare il treno",
        ("r2" to "tags") to "estate|mare",
        ("r3" to "done") to "true",
        ("r3" to "date") to "2026-09-24"
    )
    private val index = mapOf("r3" to "Farina, uova e ZUCCHERO\nForno a 180")

    private fun search(query: String) = searchRows(rows, columns, cells, query, index)

    @Test
    fun blankQueryKeepsEverythingAndSearchesNothing() {
        val (kept, search) = search("   ")
        assertEquals(rows, kept)
        assertNull(search)
    }

    @Test
    fun titleMatchIgnoresCaseAndIsNotLitAsAWholeRow() {
        val (kept, search) = search("spesa")
        assertEquals(listOf(shopping), kept)
        assertEquals("spesa", search!!.query)
        assertTrue(search.foundInside.isEmpty())
    }

    @Test
    fun propertyMatchKeepsTheRowAndLightsItUp() {
        val (kept, search) = search("TRENO")
        assertEquals(listOf(trip), kept)
        assertEquals(setOf("r2"), search!!.foundInside)
    }

    @Test
    fun multiSelectValuesAreSearchedOneByOne() {
        assertEquals(listOf(trip), search("mare").first)
        // Il separatore salvato non deve incollare due valori in una parola.
        assertTrue(search("estate|mare").first.isEmpty())
    }

    @Test
    fun pageContentMatchLightsUpTheRow() {
        val (kept, search) = search("zucchero")
        assertEquals(listOf(recipe), kept)
        assertEquals(setOf("r3"), search!!.foundInside)
    }

    @Test
    fun checkboxesAndDatesAreNotSearched() {
        assertTrue(search("true").first.isEmpty())
        assertTrue(search("2026").first.isEmpty())
    }

    @Test
    fun theQueryIsTrimmed() {
        assertEquals(listOf(trip), search("  viaggio ").first)
    }

    @Test
    fun aRowCreatedWhileSearchingStaysVisible() {
        val fresh = DatabaseRowEntity(id = "new", pageId = "db", title = "")
        val (kept, _) = searchRows(rows + fresh, columns, cells, "spesa", index, setOf("new"))
        assertEquals(listOf(shopping, fresh), kept)
    }

    @Test
    fun accentedLettersIgnoreCaseToo() {
        val row = DatabaseRowEntity(id = "r5", pageId = "db", title = "Perché è così")
        val (kept, _) = searchRows(listOf(row), columns, emptyMap(), "PERCHÉ È", emptyMap())
        assertEquals(listOf(row), kept)
    }
}
