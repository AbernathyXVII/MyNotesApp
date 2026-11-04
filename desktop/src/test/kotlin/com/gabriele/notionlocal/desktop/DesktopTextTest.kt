package com.gabriele.notionlocal.desktop

import androidx.compose.ui.text.font.FontFamily
import com.gabriele.notionlocal.data.entity.PageFont
import com.gabriele.notionlocal.data.proofing.ProofingLanguage
import com.gabriele.notionlocal.data.proofing.SpellDictionaries
import com.gabriele.notionlocal.data.webclip.ClipPiece
import com.gabriele.notionlocal.data.webclip.parseClipContent
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.ui.theme.fontFamily
import com.gabriele.notionlocal.ui.theme.label
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Il testo sul PC** (dal 04/11/2026): il dizionario del correttore si legge
 * dalle risorse del programma (gli stessi file degli asset del telefono), il
 * contenuto di una pagina salvata dal Web Clipper si legge senza org.json, e
 * ogni font della pagina ha il suo nome e una famiglia di caratteri.
 */
class DesktopTextTest {
    private val dir: File = Files.createTempDirectory("pageflow-text").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun italianDictionaryFromTheProgramResources() {
        val dictionary = runBlocking { SpellDictionaries.get(context, ProofingLanguage.ITALIAN) }
        assertNotNull(dictionary)
        assertTrue("una parola giusta", dictionary!!.check("casa"))
        assertFalse("una parola sbagliata", dictionary.check("cqsa"))
    }

    @Test
    fun webClipContentWithoutOrgJson() {
        val json = """
            {"title":" Un articolo ","site":"esempio.it","blocks":[
              {"k":"h","l":3,"runs":[{"t":"Titolo"}]},
              {"k":"p","runs":[{"t":"Ciao ","b":true},{"t":"mondo","h":"https://esempio.it"}]},
              {"k":"ul","d":1,"runs":[{"t":"voce"}]},
              {"k":"table","rows":[["a","b"],["c","d"]]},
              {"k":"img","src":"https://esempio.it/x.png","alt":"x","w":0.5},
              {"k":"hr"},
              {"k":"sconosciuto"}
            ]}
        """.trimIndent()
        val content = parseClipContent(json)
        assertNotNull(content)
        assertEquals("Un articolo", content!!.title)
        assertEquals("esempio.it", content.site)
        assertEquals(6, content.blocks.size)
        assertEquals(3, (content.blocks[0] as ClipPiece.Heading).level)
        val paragraph = content.blocks[1] as ClipPiece.Paragraph
        assertTrue(paragraph.spans[0].bold)
        assertEquals("https://esempio.it", paragraph.spans[1].link)
        assertEquals(1, (content.blocks[2] as ClipPiece.ListItem).depth)
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), (content.blocks[3] as ClipPiece.Table).rows)
        assertEquals(0.5f, (content.blocks[4] as ClipPiece.Image).width)
        assertEquals(ClipPiece.Divider, content.blocks[5])
        assertNull(parseClipContent("non è JSON"))
    }

    @Test
    fun everyPageFontHasANameAndAFamily() {
        PageFont.entries.forEach { font ->
            assertTrue(font.label.isNotBlank())
            assertNotNull(font.fontFamily())
        }
        assertEquals(FontFamily.Monospace, PageFont.CONSOLAS.fontFamily())
        assertEquals(FontFamily.Serif, PageFont.GARAMOND.fontFamily())
    }
}
