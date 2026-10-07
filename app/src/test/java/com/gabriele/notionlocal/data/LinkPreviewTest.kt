package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.ui.screen.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/**
 * **L'anteprima dei segnalibri e il peso dei file** (06/10/2026): dall'HTML
 * di una pagina si prendono titolo, descrizione e immagine, coi tag "og:"
 * prima di tutto e il `<title>` se mancano.
 */
class LinkPreviewTest {

    @Test
    fun theOpenGraphTagsComeFirst_inAnyAttributeOrder() {
        val html = """
            <html><head>
            <title>Titolo della scheda</title>
            <meta content="Il titolo vero" property="og:title">
            <meta property='og:description' content='Una descrizione &amp; altro &#8211; ok'>
            <meta name="description" content="Quella di riserva">
            <meta property="og:image" content="/img/anteprima.png">
            </head><body></body></html>
        """.trimIndent()
        val preview = LinkPreviews.parse(html, "https://example.com/articoli/uno")
        assertEquals("Il titolo vero", preview.title)
        assertEquals("Una descrizione & altro – ok", preview.description)
        assertEquals("https://example.com/img/anteprima.png", preview.imageUrl)
    }

    @Test
    fun withoutOpenGraphTheTitleTagAndTheDescriptionAreUsed() {
        val html = "<html><head><title>\n  Solo   il titolo </title><meta name=\"description\" content=\"Breve\"></head></html>"
        val preview = LinkPreviews.parse(html, "https://example.com/")
        assertEquals("Solo il titolo", preview.title)
        assertEquals("Breve", preview.description)
        assertNull(preview.imageUrl)
    }

    @Test
    fun aPageWithNothingGivesNothing() {
        val preview = LinkPreviews.parse("<html><body>niente</body></html>", "https://example.com/")
        assertEquals(LinkPreview(null, null, null), preview)
    }

    @Test
    fun theSizeOfAFileReadsLikeOnTheFilesApp() {
        assertEquals("820 B", formatFileSize(820, Locale.ITALY))
        assertEquals("12 KB", formatFileSize(12 * 1024, Locale.ITALY))
        assertEquals("1,4 MB", formatFileSize((1.4 * 1024 * 1024).toLong(), Locale.ITALY))
        assertEquals("1.4 MB", formatFileSize((1.4 * 1024 * 1024).toLong(), Locale.US))
    }
}
