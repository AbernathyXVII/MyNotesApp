package com.gabriele.notionlocal.ui

import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.ui.screen.RenderItem
import com.gabriele.notionlocal.ui.screen.groupIntoRenderItems
import com.gabriele.notionlocal.ui.screen.renderItemKey
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Il campo delle righe di testo resta lo stesso anche quando cambia la
 * sua prima riga: altrimenti viene rifatto e la tastiera si chiude.
 */
class RunKeyTest {

    private fun line(text: String, type: BlockType = BlockType.PARAGRAPH) =
        BlockEntity(pageId = "p", type = type, textJson = """[{"text":"$text"}]""")

    private fun keys(blocks: List<BlockEntity>) = groupIntoRenderItems(blocks).map { renderItemKey(it) }

    private fun runKeys(blocks: List<BlockEntity>) =
        groupIntoRenderItems(blocks).filterIsInstance<RenderItem.Run>().map { it.key }

    @Test
    fun aNewLineAboveTheFirstOneKeepsTheFieldAtTheTopOfThePage() {
        // L'Invio a inizio di "prova": la riga vuota nasce sopra.
        val prova = line("prova")
        val x = line("x")
        assertEquals(runKeys(listOf(prova, x)), runKeys(listOf(line(""), prova, x)))
    }

    @Test
    fun aNewLineAboveTheFirstOneKeepsTheFieldUnderAnIsland() {
        val link = line("", BlockType.PAGE_LINK)
        val prova = line("prova")
        assertEquals(
            keys(listOf(link, prova)),
            keys(listOf(link, line(""), prova))
        )
    }

    @Test
    fun everyItemHasItsOwnKey() {
        val blocks = listOf(
            line("a"), line("", BlockType.DIVIDER), line("b"), line("c"),
            line("", BlockType.PAGE_LINK), line("", BlockType.TOGGLE), line("d")
        )
        val all = keys(blocks)
        assertEquals(6, all.size)
        assertEquals(all.size, all.toSet().size)
    }
}
