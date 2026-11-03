package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **Le righe in ordine di lettura** (20/10/2026): la prima riga vuota per il
 * Web Clipper, e i posti di "Move to line…".
 */
class PageLinesTest {

    private val page = "p"

    private fun line(id: String, order: Int, text: String = "", parent: String? = null, type: BlockType = BlockType.PARAGRAPH) =
        BlockEntity(id = id, pageId = page, parentBlockId = parent, type = type, textJson = text, orderIndex = order)

    // Il "testo" di prova è il textJson stesso.
    private val textOf: (BlockEntity) -> String = { it.textJson }

    private val blocks = listOf(
        line("title", 0, "Titolo"),
        line("toggle", 1, "Toggle", type = BlockType.TOGGLE),
        line("inside-1", 0, "Dentro", parent = "toggle"),
        line("inside-2", 1, "   ", parent = "toggle"),
        line("after", 2, "Dopo"),
        line("empty", 3, "")
    )

    @Test
    fun eachBlockIsFollowedByWhatItHasInside() {
        assertEquals(
            listOf("title" to 0, "toggle" to 0, "inside-1" to 1, "inside-2" to 1, "after" to 0, "empty" to 0),
            blocks.shuffled().inReadingOrder().map { it.block.id to it.depth }
        )
    }

    @Test
    fun theFirstEmptyLineFromTheTop() {
        // Una riga di soli spazi è vuota, anche dentro un toggle.
        assertEquals("inside-2", blocks.firstEmptyLine(textOf)?.id)
        // Un toggle vuoto con delle righe dentro non è una riga vuota; un'immagine nemmeno.
        val noEmpty = listOf(
            line("t", 0, "", type = BlockType.TOGGLE),
            line("t-child", 0, "x", parent = "t"),
            line("img", 1, "", type = BlockType.IMAGE),
            line("parent", 2, ""),
            line("child", 0, "y", parent = "parent")
        )
        assertNull(noEmpty.firstEmptyLine(textOf))
        assertNull(emptyList<BlockEntity>().firstEmptyLine(textOf))
    }

    @Test
    fun aBlockCannotMoveUnderItselfOrWhatItHasInside() {
        assertEquals(
            listOf("title", "after", "empty"),
            blocks.moveTargetsFor("toggle").map { it.block.id }
        )
        assertEquals(
            listOf("toggle", "inside-1", "inside-2", "after", "empty"),
            blocks.moveTargetsFor("title").map { it.block.id }
        )
    }
}
