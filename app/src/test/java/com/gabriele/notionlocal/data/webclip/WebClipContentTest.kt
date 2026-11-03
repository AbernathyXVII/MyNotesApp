package com.gabriele.notionlocal.data.webclip

import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.RichTextSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Text and Images"** (20/10/2026): l'articolo letto dalla pagina (il JSON
 * di `noteReader.serialize()`) diventa righe di notE nello stesso ordine.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebClipContentTest {

    // Come lo dà lo script sulla guida di rpgsite (accorciato).
    private val json = """
        {"title":"Persona 5 Royal Stats guide","site":"RPG Site","blocks":[
          {"k":"p","runs":[{"t":"Persona 5 Royal","b":true},{"t":" features the return of "},{"t":"Social Stats","b":true}]},
          {"k":"img","src":"https://images.rpgsite.net/a.webp","alt":"Stats","w":0.95},
          {"k":"cap","runs":[{"t":"The five stats","i":true}]},
          {"k":"h","l":2,"runs":[{"t":"How to increase Knowledge"}]},
          {"k":"h","l":3,"runs":[{"t":"Best ways"}]},
          {"k":"ul","d":0,"runs":[{"t":"Studying.","b":true},{"t":" Use our "},{"t":"exam answers","h":"https://www.rpgsite.net/x"}]},
          {"k":"ul","d":1,"runs":[{"t":"Nested"}]},
          {"k":"ol","d":0,"runs":[{"t":"First"}]},
          {"k":"img","src":"https://images.rpgsite.net/b.jpg","alt":"","w":0.4},
          {"k":"img","src":"https://images.rpgsite.net/missing.jpg","w":1},
          {"k":"quote","text":"One\nTwo"},
          {"k":"code","text":"val x = 1\n"},
          {"k":"table","rows":[["Rank","EXP"],["1","34"]]},
          {"k":"hr"},
          {"k":"unknown"},
          {"k":"p","runs":[{"t":"javascript link","h":"javascript:alert(1)"}]}
        ]}
    """.trimIndent()

    @Test
    fun theArticleBecomesNoteLinesInTheSameOrder() {
        val content = parseClipContent(json)!!
        assertEquals("Persona 5 Royal Stats guide", content.title)
        assertEquals(15, content.blocks.size)
        assertEquals(
            listOf("https://images.rpgsite.net/a.webp", "https://images.rpgsite.net/b.jpg", "https://images.rpgsite.net/missing.jpg"),
            content.imageSources
        )

        // Due immagini scaricate, la terza no: resta fuori.
        val blocks = content.toNewBlocks(
            mapOf("https://images.rpgsite.net/a.webp" to "a.img", "https://images.rpgsite.net/b.jpg" to "b.img")
        )
        assertEquals(
            listOf(
                BlockType.PARAGRAPH, BlockType.IMAGE, BlockType.PARAGRAPH, BlockType.PARAGRAPH, BlockType.PARAGRAPH,
                BlockType.BULLET_LIST_ITEM, BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.IMAGE,
                BlockType.QUOTE, BlockType.CODE, BlockType.TABLE, BlockType.DIVIDER, BlockType.PARAGRAPH
            ),
            blocks.map { it.type }
        )
        // Grassetti e link restano.
        assertEquals(
            listOf(RichTextSpan("Persona 5 Royal", bold = true), RichTextSpan(" features the return of "), RichTextSpan("Social Stats", bold = true)),
            blocks[0].spans
        )
        assertEquals("https://www.rpgsite.net/x", blocks[5].spans.last().link)
        // L'immagine larga quasi tutta la colonna va a tutta larghezza; quella stretta resta stretta.
        assertEquals("a.img", blocks[1].attrs.file)
        assertEquals("Stats", blocks[1].attrs.name)
        assertNull(blocks[1].attrs.width)
        assertEquals(0.4f, blocks[8].attrs.width)
        // La didascalia in corsivo, i titoletti grandi e in grassetto.
        assertEquals(listOf(RichTextSpan("The five stats", italic = true)), blocks[2].spans)
        assertEquals(listOf(RichTextSpan("How to increase Knowledge", bold = true, size = 24)), blocks[3].spans)
        assertEquals(listOf(RichTextSpan("Best ways", bold = true, size = 20)), blocks[4].spans)
        // Gli elenchi col loro rientro.
        assertEquals(listOf(0, 1, 0), blocks.subList(5, 8).map { it.indentLevel })
        // Citazione su due righe, codice, tabella.
        assertEquals("One\nTwo", blocks[9].spans.single().text)
        assertEquals("val x = 1", blocks[10].spans.single().text)
        assertEquals(listOf(listOf("Rank", "EXP"), listOf("1", "34")), blocks[11].cells)
        // Un link che esegue codice non resta un link.
        assertNull(blocks[13].spans.single().link)
    }

    @Test
    fun aBrokenAnswerIsNoArticle() {
        assertNull(parseClipContent("not json"))
        assertEquals(0, parseClipContent("""{"title":"x"}""")!!.blocks.size)
    }

    @Test
    fun theTitleOfTextAndImagesIsLarge() {
        val lines = webClipCaptions("Guide", null, null, "Clipped from", titleSize = headingSize(1))
        assertEquals(listOf(listOf(RichTextSpan("Guide", bold = true, size = 24))), lines)
    }
}
