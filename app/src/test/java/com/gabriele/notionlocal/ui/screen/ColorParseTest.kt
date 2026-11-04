package com.gabriele.notionlocal.ui.screen

import android.app.Application
import com.gabriele.notionlocal.data.entity.TAG_COLORS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I colori dei tag sono quelli di prima** (dal 04/11/2026): `parseColorString`,
 * nel codice comune, legge un colore come `android.graphics.Color.parseColor`.
 * Lo si confronta con quello di Android sui colori dei tag e su qualche altro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ColorParseTest {
    @Test
    fun theSameColorsAsAndroid() {
        val texts = TAG_COLORS.map { it.hex } +
            listOf("#FF0000", "#80FF0000", "#00000000", "#ffffff", "red", "Blue", "darkgray", "lightgrey", "teal", "navy")
        for (text in texts) {
            assertEquals(text, android.graphics.Color.parseColor(text), parseColorString(text))
        }
    }

    @Test
    fun whatIsNotAColorFailsLikeOnAndroid() {
        for (text in listOf("#12345", "#GGGGGG", "rosso", "#1234567")) {
            assertThrows(text, IllegalArgumentException::class.java) { android.graphics.Color.parseColor(text) }
            assertThrows(text, IllegalArgumentException::class.java) { parseColorString(text) }
        }
        // Il testo vuoto: Android dà un altro errore (`StringIndexOutOfBoundsException`),
        // che `tagColor` non prendeva; qui è un colore sbagliato come gli altri, e il tag resta grigio.
        assertThrows(IllegalArgumentException::class.java) { parseColorString("") }
    }
}
