package com.gabriele.notionlocal.data.entity

import com.gabriele.notionlocal.viewmodel.searchRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le celle "Color cell": come si salvano, come si rileggono, e che si cerca il testo e non il colore. */
class ColorCellValueTest {

    private fun roundTrip(color: String?, text: String) =
        parseColorCell(encodeColorCell(ColorCellValue(color, text)))

    @Test
    fun colorAndTextSurviveSaving() {
        assertEquals("#E03E3E|Urgente", encodeColorCell(ColorCellValue("#e03e3e", "Urgente")))
        assertEquals(ColorCellValue("#E03E3E", "Urgente"), roundTrip("#E03E3E", "Urgente"))
        // Il testo può contenere anche la barra: il colore sono i primi sette caratteri.
        assertEquals(ColorCellValue("#2383E2", "a|b"), roundTrip("#2383E2", "a|b"))
    }

    @Test
    fun onlyTheColorOrOnlyTheText() {
        assertEquals("#2E9E44", encodeColorCell(ColorCellValue("#2E9E44", "")))
        assertEquals(ColorCellValue("#2E9E44", ""), parseColorCell("#2E9E44"))
        assertEquals(ColorCellValue(null, "Solo testo"), parseColorCell("Solo testo"))
        assertTrue(parseColorCell("").isEmpty)
    }

    @Test
    fun aTextThatLooksLikeAColorStaysText() {
        assertEquals(ColorCellValue(null, "#FF0000 rosso"), roundTrip(null, "#FF0000 rosso"))
        assertEquals(ColorCellValue(null, "|barra"), roundTrip(null, "|barra"))
        // Un testo scritto prima in una colonna di testo, che somiglia a un colore.
        assertEquals(ColorCellValue(null, "#123456789"), parseColorCell("#123456789"))
    }

    @Test
    fun theTextIsWhiteOnDarkColorsAndBlackOnLightOnes() {
        assertTrue(darkTextOn("#FFE600"))
        assertTrue(darkTextOn("#FFFFFF"))
        assertTrue(darkTextOn("#12F20D"))
        assertFalse(darkTextOn("#E03E3E"))
        assertFalse(darkTextOn("#2383E2"))
        assertFalse(darkTextOn("#000000"))
    }

    @Test
    fun theSearchLooksAtTheTextNotAtTheColor() {
        val color = DatabaseColumnEntity(id = "c", pageId = "db", name = "Stato", type = ColumnType.COLOR)
        val row = DatabaseRowEntity(id = "r1", pageId = "db", title = "Riga")
        val cells = mapOf(("r1" to "c") to "#E03E3E|Urgente")
        assertEquals(listOf(row), searchRows(listOf(row), listOf(color), cells, "urgente", emptyMap()).first)
        assertTrue(searchRows(listOf(row), listOf(color), cells, "E03E", emptyMap()).first.isEmpty())
    }
}
