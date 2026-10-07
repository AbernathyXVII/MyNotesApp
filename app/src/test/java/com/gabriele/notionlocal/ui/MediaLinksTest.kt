package com.gabriele.notionlocal.ui

import android.app.Application
import com.gabriele.notionlocal.ui.screen.formatPlayTime
import com.gabriele.notionlocal.ui.screen.linkHost
import com.gabriele.notionlocal.ui.screen.normalizedLink
import com.gabriele.notionlocal.ui.screen.vimeoId
import com.gabriele.notionlocal.ui.screen.youTubeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I link dei video e degli audio** (06/10/2026): YouTube e Vimeo si
 * riconoscono in tutti i modi in cui si scrivono; un indirizzo senza
 * "https://" lo prende; quello che non è un link no.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MediaLinksTest {

    @Test
    fun youTubeIsRecognizedInEveryForm() {
        listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/watch?v=dQw4w9WgXcQ&t=42s",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ?si=abc",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ"
        ).forEach { assertEquals(it, "dQw4w9WgXcQ", youTubeId(it)) }
        assertNull(youTubeId("https://www.youtube.com/"))
        assertNull(youTubeId("https://example.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun vimeoIsRecognized() {
        assertEquals("76979871", vimeoId("https://vimeo.com/76979871"))
        assertEquals("76979871", vimeoId("https://player.vimeo.com/video/76979871"))
        assertNull(vimeoId("https://vimeo.com/channels"))
        assertNull(vimeoId("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun aLinkWithoutHttpsGetsIt_andWordsAreNotLinks() {
        assertEquals("https://example.com/video.mp4", normalizedLink("  example.com/video.mp4 "))
        assertEquals("http://example.com/a.mp3", normalizedLink("http://example.com/a.mp3"))
        assertNull(normalizedLink("ciao"))
        assertNull(normalizedLink("due parole.com"))
        assertNull(normalizedLink("ftp://example.com/file"))
        assertNull(normalizedLink(""))
        assertEquals("youtube.com", linkHost("https://www.youtube.com/watch?v=x"))
    }

    @Test
    fun thePlayTimeReadsLikeAClock() {
        assertEquals("0:00", formatPlayTime(0))
        assertEquals("1:05", formatPlayTime(65_400))
        assertEquals("1:02:07", formatPlayTime(3_727_000))
    }
}
