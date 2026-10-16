package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **Il formato della data è uno solo, in Settings** (16/10/2026): "L'opzione
 * deve essere globale, non scelta singolarmente". Una colonna che se n'era
 * scelto uno suo (fino a quel giorno si poteva) non lo usa più, e salvandola
 * lo perde; l'ora a 12/24 ore o nascosta resta sua.
 */
class GlobalDateFormatTest {

    private val column = DatabaseColumnEntity(
        pageId = "videogames", name = "Release date", type = ColumnType.DATE,
        orderIndex = 0, dateFormat = "FULL", timeFormat = "H12"
    )

    @Test
    fun theColumnKeepsOnlyItsTimeFormat() {
        assertEquals(DateSettings(time = TimeFormatKind.H12), column.dateSettings())
    }

    @Test
    fun savingForgetsTheOldDateFormat() {
        val saved = column.withDateSettings(DateSettings(time = TimeFormatKind.HIDDEN))
        assertNull(saved.dateFormat)
        assertEquals("HIDDEN", saved.timeFormat)
    }
}
