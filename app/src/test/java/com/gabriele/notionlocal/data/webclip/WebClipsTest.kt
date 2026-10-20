package com.gabriele.notionlocal.data.webclip

import com.gabriele.notionlocal.data.entity.RichTextSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** **Il Web Clipper** (20/10/2026): il link scritto nel pannello e le righe sotto la foto. */
class WebClipsTest {

    @Test
    fun theLinkIsCompletedOrRefused() {
        val game8 = "https://game8.co/games/Shin-Megami-Tensei-III-Nocturne/archives/331985"
        assertEquals(game8, webClipUrl("  $game8 "))
        // Senza "https://" va bene lo stesso, come nella barra del browser.
        assertEquals("https://game8.co/games", webClipUrl("game8.co/games"))
        assertEquals("http://example.com", webClipUrl("http://example.com"))
        // Non sono siti.
        assertNull(webClipUrl(""))
        assertNull(webClipUrl("ciao"))
        assertNull(webClipUrl("due parole.com"))
        assertNull(webClipUrl("javascript:alert(1)"))
        assertNull(webClipUrl("ftp://example.com/file"))
        assertNull(webClipUrl("https://localhost"))
    }

    @Test
    fun theHostNamesTheClipWithoutATitle() {
        assertEquals("game8.co", webClipHost("https://game8.co/games/x"))
        assertEquals("example.com", webClipHost("https://www.example.com"))
    }

    @Test
    fun underThePictureTitleNoteAndSource() {
        val lines = webClipCaptions(
            title = "Prologue",
            note = "Prima riga\n\n  seconda riga ",
            url = "https://game8.co/x",
            clippedFrom = "Clipped from"
        )
        assertEquals(
            listOf(
                listOf(RichTextSpan(text = "Prologue", bold = true)),
                listOf(RichTextSpan(text = "Prima riga")),
                listOf(RichTextSpan(text = "seconda riga")),
                listOf(
                    RichTextSpan(text = "Clipped from ", color = CLIP_SOURCE_COLOR, size = CLIP_SOURCE_SIZE),
                    RichTextSpan(text = "https://game8.co/x", link = "https://game8.co/x", size = CLIP_SOURCE_SIZE)
                )
            ),
            lines
        )
        // Senza titolo, nota e link non c'è niente.
        assertEquals(emptyList<List<RichTextSpan>>(), webClipCaptions(" ", "", null, "Clipped from"))
    }
}
