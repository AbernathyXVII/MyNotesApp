package com.gabriele.notionlocal.data.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **La borsa** (17/10/2026): risposte vere di Yahoo Finance (in
 * `resources/stocks`) lette come si deve, e le sigle come si leggono.
 */
class StocksTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("stocks/$name")!!.bufferedReader().readText()

    @Test
    fun pricesOfSeveralStocksTogether() {
        val quotes = parseSpark(resource("spark.json"))
        assertEquals(setOf("LDO.MI", "ENEL.MI", "FTSEMIB.MI"), quotes.keys)
        val leonardo = quotes.getValue("LDO.MI")
        assertEquals(50.42, leonardo.price, 0.001)
        assertEquals(4.725, leonardo.changePercent, 0.001)
        assertEquals("EUR", leonardo.currency)
        assertEquals(2, leonardo.decimals)
        assertEquals(48.145, leonardo.previousClose!!, 0.001)
        assertEquals(49857.18, quotes.getValue("FTSEMIB.MI").price, 0.001)
        assertEquals(emptyMap<String, StockQuote>(), parseSpark("non è json"))
    }

    @Test
    fun searchingAStock() {
        val found = parseStockSearch(resource("search_enel.json"))
        assertEquals("ENEL.MI", found.first().symbol)
        assertEquals("Enel SpA", found.first().name)
        assertEquals("Milano", found.first().exchange)
        assertTrue(found.all { it.symbol.isNotBlank() })
    }

    @Test
    fun symbolsAsTheyRead() {
        assertEquals("LDO", displaySymbol("LDO.MI"))
        assertEquals("FTSEMIB", displaySymbol("FTSEMIB.MI"))
        assertEquals("EURUSD", displaySymbol("EURUSD=X"))
        assertEquals("GDAXI", displaySymbol("^GDAXI"))
        assertEquals("AAPL", displaySymbol("AAPL"))
    }

    @Test
    fun whenToRefresh() {
        val widget = StocksWidget(items = listOf(StockItem("LDO.MI", "Leonardo")), fetchedAt = 1_000_000)
        assertFalse(widget.isStale(1_000_000 + 4 * MINUTE_MS))
        assertTrue(widget.isStale(1_000_000 + 5 * MINUTE_MS))
        assertTrue(widget.copy(fetchedAt = null).isStale(0))
    }
}
