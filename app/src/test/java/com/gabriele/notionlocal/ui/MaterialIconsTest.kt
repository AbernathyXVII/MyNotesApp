package com.gabriele.notionlocal.ui

import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.IconSymbol
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.symbolField
import com.gabriele.notionlocal.ui.screen.MATERIAL_ICON_NAMES
import com.gabriele.notionlocal.ui.screen.materialIconByName
import com.gabriele.notionlocal.ui.screen.materialIconWords
import com.gabriele.notionlocal.ui.screen.searchMaterialIcons
import com.gabriele.notionlocal.ui.screen.withIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Le icone di Material della scheda "Icons"** (08/10/2026): ogni nome
 * dell'elenco generato dà davvero la sua icona, si cercano per le parole del
 * nome, e il colore viaggia con l'icona.
 */
class MaterialIconsTest {

    @Test
    fun everyNameGivesItsIcon_withTheNumberedOnesLast() {
        assertTrue(MATERIAL_ICON_NAMES.size > 2000)
        val missing = MATERIAL_ICON_NAMES.filter { materialIconByName(it) == null }
        assertEquals(emptyList<String>(), missing)
        assertEquals("Filled.Home", materialIconByName("Home")!!.name)
        // Le spostate fra quelle che si specchiano vengono da lì.
        assertEquals("AutoMirrored.Filled.ArrowBack", materialIconByName("ArrowBack")!!.name)
        assertNull(materialIconByName("NonEsiste"))
        assertEquals("Abc", MATERIAL_ICON_NAMES.first())
        assertTrue(MATERIAL_ICON_NAMES.last().trimStart('_').first().isDigit())
    }

    @Test
    fun theNameIsSplitIntoWords_andTheSearchUsesTheirBeginnings() {
        assertEquals(listOf("access", "alarm"), materialIconWords("AccessAlarm"))
        assertEquals(listOf("3d", "rotation"), materialIconWords("_3dRotation"))
        assertEquals(listOf("filter", "1"), materialIconWords("Filter1"))
        assertTrue("Home" in searchMaterialIcons("home"))
        assertTrue("AccessAlarm" in searchMaterialIcons("alar acc"))
        assertTrue(searchMaterialIcons("zzzqqq").isEmpty())
        assertEquals(MATERIAL_ICON_NAMES, searchMaterialIcons("  "))
    }

    @Test
    fun theColorTravelsWithTheIcon() {
        assertEquals("Home", IconSymbol("Home").encoded)
        assertEquals("Home|#E03E3E", IconSymbol("Home", "#E03E3E").encoded)
        assertEquals(IconSymbol("Home", "#E03E3E"), IconSymbol.parse("Home|#E03E3E"))
        assertEquals(IconSymbol("Home"), IconSymbol.parse("Home"))
        assertNull(IconSymbol.parse(""))
        assertEquals("Star|#0B6E99", IconChoice.Symbol(IconSymbol("Star", "#0B6E99")).symbolField)
        // Nel riquadro l'icona di Material prende il posto dell'immagine; un'emoji la toglie.
        val withSymbol = BlockAttrs(icon = "💡", iconImage = "a.png").withIcon(IconChoice.Symbol(IconSymbol("Star")))
        assertEquals(BlockAttrs(icon = "💡", symbol = "Star"), withSymbol)
        assertEquals(BlockAttrs(icon = "🔥"), withSymbol.withIcon(IconChoice.Emoji("🔥")))
        assertEquals(BlockAttrs(icon = ""), withSymbol.withIcon(IconChoice.None))
    }
}
