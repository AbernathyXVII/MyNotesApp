package com.gabriele.notionlocal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **L'ordine naturale dei testi** (20/10/2026, Habit Tracker): i giorni del
 * mese in fila come numeri, non lettera per lettera.
 */
class NaturalOrderTest {

    @Test
    fun theDaysOfTheMonthInOrder() {
        val days = (1..31).map { it.toString() }
        assertEquals(days, days.shuffled(java.util.Random(7)).sortedWith(NaturalOrder))
        // Com'era: lettera per lettera.
        assertEquals(listOf("1", "10", "11"), listOf("11", "1", "10").sortedWith(String.CASE_INSENSITIVE_ORDER))
    }

    @Test
    fun numbersInsideTheText() {
        assertEquals(
            listOf("Giorno 2", "Giorno 9", "Giorno 10", "Giorno 21", "giorno 22"),
            listOf("Giorno 21", "giorno 22", "Giorno 10", "Giorno 2", "Giorno 9").sortedWith(NaturalOrder)
        )
        assertEquals(listOf("v0.9", "v0.10", "v1.2"), listOf("v1.2", "v0.10", "v0.9").sortedWith(NaturalOrder))
    }

    @Test
    fun lettersStillIgnoreTheirCaseAndNumbersComeFirst() {
        assertEquals(listOf("2", "10", "alfa", "Beta", "gamma"), listOf("gamma", "Beta", "10", "alfa", "2").sortedWith(NaturalOrder))
        // Il più corto prima: "Riga" prima di "Riga 1".
        assertTrue(NaturalOrder.compare("Riga", "Riga 1") < 0)
    }

    @Test
    fun equalNumbersAreStillInAFixedOrder() {
        assertTrue(NaturalOrder.compare("1", "01") != 0)
        assertEquals(NaturalOrder.compare("01", "1"), -NaturalOrder.compare("1", "01"))
        assertTrue(NaturalOrder.compare("a", "A") != 0)
        assertEquals(0, NaturalOrder.compare("12", "12"))
    }
}
