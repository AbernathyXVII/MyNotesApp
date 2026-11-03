package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

/** I conti della vista Chart: dalle righe ai punti del grafico. */
class ChartTest {

    private val labels = ChartLabels(untitled = "Untitled", empty = "Empty", checked = "Checked", unchecked = "Unchecked", day = { it.toString() })
    private val price = DatabaseColumnEntity(id = "price", pageId = "db", name = "Price", type = ColumnType.NUMBER)
    private val platform = DatabaseColumnEntity(id = "platform", pageId = "db", name = "Platform", type = ColumnType.SELECT)
    private val columns = listOf(price, platform)

    // Le quattro pagine delle foto dell'utente: 1, 2, 3, 4 con 100, 200, 400, 300 dollari.
    private val rows = (1..4).map { DatabaseRowEntity(id = "r$it", pageId = "db", title = "$it") }
    private val cells = mapOf(
        ("r1" to "price") to "100", ("r2" to "price") to "200", ("r3" to "price") to "400", ("r4" to "price") to "300",
        ("r1" to "platform") to "PS5", ("r2" to "platform") to "PC", ("r3" to "platform") to "PS5", ("r4" to "platform") to ""
    )

    private fun points(settings: ChartSettings) =
        chartPoints(rows, columns, cells, settings, labels, ZoneId.of("Europe/Rome")).map { it.label to it.value }

    @Test
    fun byNameEachPageIsABarWithItsPrice() {
        assertEquals(
            listOf("1" to 100.0, "2" to 200.0, "3" to 400.0, "4" to 300.0),
            points(ChartSettings(valueColumnId = "price"))
        )
    }

    @Test
    fun sortedByPriceHighToLowAsInThePhoto() {
        assertEquals(
            listOf("3", "4", "2", "1"),
            points(ChartSettings(valueColumnId = "price", sort = ChartSort.VALUE_DESC)).map { it.first }
        )
    }

    @Test
    fun eachPageKeepsItsColorWhateverTheOrder() {
        // Come nelle foto: la "3" resta verde (terzo colore) anche prima di tutte.
        val sorted = chartPoints(rows, columns, cells, ChartSettings(valueColumnId = "price", sort = ChartSort.VALUE_DESC), labels)
        assertEquals(listOf("3" to 2, "4" to 3, "2" to 1, "1" to 0), sorted.map { it.label to it.colorIndex })
        val withoutZero = chartPoints(rows, columns, cells + (("r1" to "price") to "0"), ChartSettings(valueColumnId = "price", omitZero = true), labels)
        assertEquals(listOf(1, 2, 3), withoutZero.map { it.colorIndex })
    }

    @Test
    fun countByASelectPropertyWithTheEmptyOnesTogether() {
        assertEquals(
            listOf("PC" to 1.0, "PS5" to 2.0, "Empty" to 1.0),
            points(ChartSettings(groupColumnId = "platform"))
        )
    }

    @Test
    fun theSevenWaysToMakeANumberOfAGroup() {
        fun of(aggregation: ChartAggregation) =
            points(ChartSettings(groupColumnId = "platform", valueColumnId = "price", aggregation = aggregation)).toMap()["PS5"]
        assertEquals(500.0, of(ChartAggregation.VALUES))
        assertEquals(500.0, of(ChartAggregation.SUM))
        assertEquals(250.0, of(ChartAggregation.AVERAGE))
        assertEquals(100.0, of(ChartAggregation.MIN))
        assertEquals(400.0, of(ChartAggregation.MAX))
        assertEquals(250.0, of(ChartAggregation.MEDIAN))
        assertEquals(2.0, of(ChartAggregation.DISTINCT))
    }

    @Test
    fun omitZeroAndCumulative() {
        val withZero = cells + (("r2" to "price") to "0")
        val omitted = chartPoints(rows, columns, withZero, ChartSettings(valueColumnId = "price", omitZero = true), labels)
        assertEquals(listOf("1", "3", "4"), omitted.map { it.label })
        assertEquals(
            listOf(100.0, 300.0, 700.0, 1000.0),
            points(ChartSettings(valueColumnId = "price", cumulative = true)).map { it.second }
        )
    }

    @Test
    fun namesWithNumbersAreInNaturalOrder() {
        val many = listOf("Pagina 10", "Pagina 2", "Pagina 1").mapIndexed { i, t -> DatabaseRowEntity(id = "x$i", pageId = "db", title = t) }
        val sorted = chartPoints(many, columns, emptyMap(), ChartSettings(), labels).map { it.label }
        assertEquals(listOf("Pagina 1", "Pagina 2", "Pagina 10"), sorted)
    }

    @Test
    fun ticksAndTotalAsInNotion() {
        // Un massimo di 400: 0, 120, 240, 360, 480, come nelle foto.
        assertEquals(listOf(0.0, 120.0, 240.0, 360.0, 480.0), chartTicks(0.0, 400.0))
        // Contando le pagine le tacche sono numeri interi.
        assertEquals(listOf(0.0, 1.0, 2.0), chartTicks(0.0, 1.0, integers = true))
        // 186 non arriva a 300: si va di 60 in 60.
        assertEquals(listOf(0.0, 60.0, 120.0, 180.0, 240.0), chartTicks(0.0, 186.0))
        assertEquals("1K", compactNumber(1000.0))
        assertEquals("1.5M", compactNumber(1_500_000.0))
        assertEquals("250", compactNumber(250.0))
    }

    @Test
    fun aChosenRangeKeepsEveryTickInside() {
        val ps = chartPoints(rows, columns, cells, ChartSettings(valueColumnId = "price"), labels)
        assertEquals(ChartAxis(0.0, 480.0, listOf(0.0, 120.0, 240.0, 360.0, 480.0)), chartAxis(ps, ChartSettings()))
        val chosen = chartAxis(ps, ChartSettings(rangeMin = 100.0, rangeMax = 500.0))
        assertEquals(listOf(100.0, 200.0, 300.0, 400.0, 500.0), chosen.ticks)
        // Un minimo sopra il massimo non si può disegnare: si torna all'automatico.
        assertEquals(480.0, chartAxis(ps, ChartSettings(rangeMin = 900.0, rangeMax = 100.0)).high, 0.0)
    }

    @Test
    fun settingsAreSavedAndReadBack() {
        val settings = ChartSettings(
            type = ChartType.DONUT,
            valueColumnId = "price",
            sort = ChartSort.VALUE_DESC,
            referenceLines = listOf(ChartReferenceLine(250.0, "Media"))
        )
        assertEquals(settings, parseChartSettings(encodeChartSettings(settings)))
        assertEquals(ChartSettings(), parseChartSettings(null))
        assertEquals(null, encodeChartSettings(ChartSettings()))
        assertEquals(ChartSettings(), parseChartSettings("{non è json"))
    }
}
