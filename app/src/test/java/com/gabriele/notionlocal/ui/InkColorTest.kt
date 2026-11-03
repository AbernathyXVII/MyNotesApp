package com.gabriele.notionlocal.ui

import androidx.compose.ui.graphics.Color
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.ui.screen.hexToInkColor
import com.gabriele.notionlocal.ui.screen.inkHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Il colore delle penne passa dal selettore del pennello, che parla in "#RRGGBB". */
class InkColorTest {

    @Test
    fun aPenColorGoesToThePickerAndBack() {
        assertEquals("#2383E2", inkHex(0xFF2383E2, Color.White))
        assertEquals(0xFF2383E2, hexToInkColor("#2383E2"))
        assertEquals(0xFF000000, hexToInkColor("000000"))
    }

    @Test
    fun theTextColorOpensThePickerOnTheThemeColor() {
        assertEquals("#FFFFFF", inkHex(InkStrokeEntity.AUTO_COLOR, Color.White))
        assertEquals("#37352F", inkHex(InkStrokeEntity.AUTO_COLOR, Color(0xFF37352F)))
    }

    @Test
    fun halfWrittenCodesAreNotColors() {
        assertNull(hexToInkColor("#FF00"))
        assertNull(hexToInkColor("#GG0000"))
    }
}
