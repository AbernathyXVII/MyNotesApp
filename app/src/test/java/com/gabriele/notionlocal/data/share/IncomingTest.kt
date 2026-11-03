package com.gabriele.notionlocal.data.share

import android.app.Application
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Condividi con notE"** (02/11/2026, chiesto dall'utente: "da Chrome,
 * dalla Galleria o da WhatsApp non puoi mandare niente a notE"): come si
 * legge quello che arriva, e la nota al volo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class IncomingTest {

    @Test
    fun aLinkFromChromeHasItsPageTitle() {
        val shared = splitSharedText("https://www.example.com/articolo", "Un articolo")
        assertEquals("Un articolo", shared.title)
        assertEquals("https://www.example.com/articolo", shared.link)
        assertEquals(emptyList<String>(), shared.lines)
        // Senza oggetto, il titolo è il sito.
        assertEquals("example.com", splitSharedText("https://www.example.com/articolo", null).title)
        // Chrome a volte manda il titolo anche come prima riga: non due volte.
        assertEquals(emptyList<String>(), splitSharedText("Un articolo\nhttps://example.com", "Un articolo").lines)
    }

    @Test
    fun aMessageFromWhatsAppKeepsItsLines() {
        val shared = splitSharedText("Comprare il latte\n\nE il pane\n\n\n\nPoi chiamare Marco  \n\n", null)
        assertEquals("Comprare il latte", shared.title)
        assertNull(shared.link)
        // Sotto il titolo il resto, mai due righe vuote di fila.
        assertEquals(listOf("E il pane", "", "Poi chiamare Marco"), shared.lines)
        // In fondo a una pagina che c'è già non c'è un titolo: tutte le righe.
        assertEquals(listOf("Comprare il latte", "", "E il pane", "", "Poi chiamare Marco"), shared.allLines)
    }

    @Test
    fun aTextEndingWithALinkKeepsBoth() {
        val shared = splitSharedText("Guarda questo video https://youtu.be/abc", null)
        assertEquals("https://youtu.be/abc", shared.link)
        assertEquals("Guarda questo video", shared.title)
        assertEquals(emptyList<String>(), shared.lines)
        assertEquals(listOf("Guarda questo video"), shared.allLines)
    }

    @Test
    fun aLongFirstLineIsCutInTheTitleButStaysInTheText() {
        val long = "a".repeat(120)
        val shared = splitSharedText(long, null)
        assertEquals(80, shared.title.length)
        assertEquals(listOf(long), shared.lines)
    }

    @Test
    fun theIntentIsRead() {
        val text = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://example.com").putExtra(Intent.EXTRA_SUBJECT, "  Esempio ")
        val read = Incoming.from(text)!!
        assertEquals("Esempio", read.subject)
        assertEquals("https://example.com", read.text)
        assertTrue(read.files.isEmpty())

        val photo = Uri.parse("content://media/external/images/1")
        val image = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, photo)
        assertEquals(listOf(photo), Incoming.from(image)!!.files)

        val more = listOf(photo, Uri.parse("content://media/external/images/2"))
        val many = Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(more))
        assertEquals(more, Incoming.from(many)!!.files)

        assertTrue(Incoming.from(Intent(Intent.ACTION_SEND).setType("text/plain"))!!.isEmpty)
        assertNull(Incoming.from(Intent(Intent.ACTION_VIEW)))
    }

    @Test
    fun aQuickNoteIsLeftOnlyAfterItWasOpen() {
        val intent = Intent(QuickNotes.ACTION)
        assertTrue(QuickNotes.takeFrom(intent))
        // Letta una volta: ruotando lo schermo non nasce un'altra nota.
        assertFalse(QuickNotes.takeFrom(intent))
        assertFalse(QuickNotes.takeFrom(Intent(Intent.ACTION_MAIN)))

        QuickNotes.created("nota", "riga")
        // Non ancora aperta: non è "lasciata".
        assertEquals(emptyList<String>(), QuickNotes.leftBehind(emptySet()))
        assertEquals(emptyList<String>(), QuickNotes.leftBehind(setOf("home", "nota")))
        // Dentro una sottopagina resta sotto.
        assertEquals(emptyList<String>(), QuickNotes.leftBehind(setOf("home", "nota", "sotto")))
        assertEquals(listOf("nota"), QuickNotes.leftBehind(setOf("home")))
        assertEquals(emptyList<String>(), QuickNotes.leftBehind(setOf("home")))
        // Il cursore, una volta sola e solo quando la riga c'è.
        QuickNotes.created("altra", "r1")
        assertNull(QuickNotes.takeFocus("altra") { false })
        assertEquals("r1", QuickNotes.takeFocus("altra") { true })
        assertNull(QuickNotes.takeFocus("altra") { true })
        QuickNotes.leftBehind(setOf("altra"))
        QuickNotes.leftBehind(emptySet())
    }
}
