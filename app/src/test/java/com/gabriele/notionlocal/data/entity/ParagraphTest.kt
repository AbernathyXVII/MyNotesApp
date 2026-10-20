package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **"Paragraph"** (21/10/2026, chiesto dall'utente, come su Word): lo spazio
 * fra i blocchi e dopo i marcatori degli elenchi, in Settings, per pagina e
 * per gruppo di righe. Vince il più vicino al testo.
 */
class ParagraphTest {

    private fun line(
        text: String,
        type: BlockType = BlockType.PARAGRAPH,
        attrs: BlockAttrs = BlockAttrs()
    ) = BlockEntity(pageId = "p", type = type, textJson = """[{"text":"$text"}]""", attrsJson = BlockAttrs.encode(attrs))

    @Test
    fun theLineWinsOverThePageThatWinsOverSettings() {
        val plain = line("a", BlockType.BULLET_LIST_ITEM)
        val own = line("b", BlockType.BULLET_LIST_ITEM, BlockAttrs(spacing = 12, markerGap = 4))
        val settings = ParagraphLook(appSpacing = 2, appGaps = ListGaps(all = 10))
        assertEquals(2, settings.spacingOf(plain))
        assertEquals(10, settings.gapOf(plain, BlockType.BULLET_LIST_ITEM))
        val page = settings.copy(page = PageParagraph(blockSpacing = 6, gaps = ListGaps(all = 0)))
        assertEquals(6, page.spacingOf(plain))
        assertEquals(0, page.gapOf(plain, BlockType.BULLET_LIST_ITEM))
        assertEquals(12, page.spacingOf(own))
        assertEquals(4, page.gapOf(own, BlockType.BULLET_LIST_ITEM))
        // Niente scelto da nessuno: com'era.
        assertEquals(0, ParagraphLook().spacingOf(plain))
        assertNull(ParagraphLook().gapOf(plain, BlockType.CHECKBOX))
        assertTrue(ParagraphLook().isPlain)
    }

    @Test
    fun theListsCanHaveTheSameSpaceOrOneEach() {
        val same = ListGaps(all = 6)
        MARKER_LIST_TYPES.forEach { assertEquals(6, same.of(it)) }
        // Spento, ognuno parte da quello di tutti: niente salta.
        val each = same.withSame(false)
        MARKER_LIST_TYPES.forEach { assertEquals(6, each.of(it)) }
        val changed = each.with(BlockType.CHECKBOX, 16)
        assertEquals(6, changed.of(BlockType.NUMBERED_LIST_ITEM))
        assertEquals(16, changed.of(BlockType.CHECKBOX))
        // Riacceso, tutti prendono quello dei numerati.
        val again = changed.withSame(true)
        MARKER_LIST_TYPES.forEach { assertEquals(6, again.of(it)) }
        assertEquals(changed, ListGaps.parse(ListGaps.encode(changed)))
    }

    @Test
    fun aPageThatFollowsSettingsWritesNothing() {
        assertNull(PageParagraph.encode(PageParagraph()))
        val own = PageParagraph(blockSpacing = 8, gaps = ListGaps(same = false, numbered = 2, bulleted = null, todo = 12))
        assertEquals(own, PageParagraph.parse(PageParagraph.encode(own)))
        // Un JSON rovinato vale come "come Settings".
        assertEquals(PageParagraph(), PageParagraph.parse("{rotto"))
    }

    @Test
    fun aNewLineKeepsTheParagraphOfTheLineItComesFrom() {
        assertEquals("{}", line("a").attrsForNewLine())
        val own = line("a", BlockType.NUMBERED_LIST_ITEM, BlockAttrs(spacing = 8, markerGap = 12, linkTarget = true))
        // Lo spazio sì, il segnalibro no.
        assertEquals(BlockAttrs(spacing = 8, markerGap = 12), BlockAttrs.parse(own.attrsForNewLine()))
        val held = line("b", BlockType.PARAGRAPH, BlockAttrs(listIndent = "BULLET_LIST_ITEM", spacing = 4))
        assertEquals(BlockAttrs(listIndent = "BULLET_LIST_ITEM", spacing = 4), BlockAttrs.parse(held.attrsForNewLine()))
        // Senza "Paragraph" il JSON non si apre nemmeno.
        assertNull(line("c", attrs = BlockAttrs(name = "x")).paragraphAttrs)
    }

    @Test
    fun aGroupWithItsOwnSpacingStartsWhereTheSpacingChanges() {
        val a = line("a")
        val b = line("b")
        val c = line("c", attrs = BlockAttrs(spacing = 8))
        val d = line("d", attrs = BlockAttrs(spacing = 8))
        val image = line("", BlockType.IMAGE)
        val e = line("e", attrs = BlockAttrs(spacing = 8))
        val f = line("f")
        val look = ParagraphLook()
        val flowing = { type: BlockType -> type != BlockType.IMAGE }
        val starts = spacingStartsOf(listOf(a, b, c, d, image, e, f), look, flowing)
        // La chiave è la riga sopra il confine: scrivendo nel gruppo non cambia.
        assertEquals(mapOf(c.id to SPACING_KEY_PREFIX + b.id, f.id to SPACING_KEY_PREFIX + e.id), starts)
        // Dopo un'isola il gruppo comincia comunque: lì non serve un confine.
        assertTrue(e.id !in starts)
        // Con la pagina a 8 anche le righe senza misura sono a 8: nessun confine fra loro e c, d.
        val page8 = ParagraphLook(page = PageParagraph(blockSpacing = 8))
        assertEquals(emptyMap<String, String>(), spacingStartsOf(listOf(a, b, c, d), page8, flowing))
    }
}
