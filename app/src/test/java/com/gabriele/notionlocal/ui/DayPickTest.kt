package com.gabriele.notionlocal.ui

import com.gabriele.notionlocal.ui.screen.pickCalendarDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * **Il tocco sui giorni del calendario della data** (30/09/2026):
 * ritoccando un giorno col pallino arancione, il pallino se ne va.
 */
class DayPickTest {

    private fun d(day: Int) = LocalDate.of(2026, 9, day)

    @Test
    fun aSingleDateComesAndGoes() {
        assertEquals(d(15) to null, pickCalendarDay(d(15), null, null, withEnd = false))
        // Ritoccato: via il pallino.
        assertEquals(null to null, pickCalendarDay(d(15), d(15), null, withEnd = false))
        // Un altro giorno: il pallino si sposta, come prima.
        assertEquals(d(20) to null, pickCalendarDay(d(20), d(15), null, withEnd = false))
    }

    @Test
    fun inAPeriodEachEndGoesOnItsOwn() {
        // Inizio, poi fine.
        assertEquals(d(15) to null, pickCalendarDay(d(15), null, null, withEnd = true))
        assertEquals(d(15) to d(20), pickCalendarDay(d(20), d(15), null, withEnd = true))
        // Ritoccata la fine: resta l'inizio.
        assertEquals(d(15) to null, pickCalendarDay(d(20), d(15), d(20), withEnd = true))
        // Ritoccato l'inizio: resta la fine, da sola.
        assertEquals(d(20) to null, pickCalendarDay(d(15), d(15), d(20), withEnd = true))
        // Ritoccato l'inizio senza fine: niente.
        assertEquals(null to null, pickCalendarDay(d(15), d(15), null, withEnd = true))
    }

    @Test
    fun theOtherTapsAsBefore() {
        // Dopo un periodo completo si ricomincia.
        assertEquals(d(25) to null, pickCalendarDay(d(25), d(15), d(20), withEnd = true))
        // Prima dell'inizio si ricomincia.
        assertEquals(d(10) to null, pickCalendarDay(d(10), d(15), null, withEnd = true))
    }
}
