package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.blockLink
import com.gabriele.notionlocal.ui.screen.blockLinkArrowSpan
import com.gabriele.notionlocal.ui.screen.markBlockLinkEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **La freccia dei collegamenti dentro la pagina** (26/10/2026, l'utente su
 * "Fast notes": "Emergenc y", con la freccia sopra la y): l'ultima lettera
 * resta attaccata alle altre, e la freccia va nello spazio dopo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlockLinkArrowTest {

    private val density = Density(3f)
    private val measurer = TextMeasurer(
        createFontFamilyResolver(ApplicationProvider.getApplicationContext()),
        density,
        LayoutDirection.Ltr
    )
    private val style = TextStyle(fontSize = 16.sp)
    private val fontPx = with(density) { 16.sp.toPx() }
    private val link = blockLink("emergency")

    /** Il testo della riga come lo costruisce l'editor: i pezzi uno dopo l'altro, con lo spazio per la freccia. */
    private fun measure(spans: List<RichTextSpan>): TextLayoutResult {
        val builder = AnnotatedString.Builder()
        var pos = 0
        spans.forEachIndexed { i, span ->
            val start = pos
            builder.append(span.text)
            pos += span.text.length
            builder.markBlockLinkEnd(span, spans.getOrNull(i + 1), start, pos)
        }
        return measurer.measure(builder.toAnnotatedString(), style)
    }

    private val y = "Emergency".lastIndex

    @Test
    fun theLastLetterStaysWithTheOthersAndTheArrowComesAfter() {
        val plain = measurer.measure(AnnotatedString("Emergency room"), style)
        val linked = measure(listOf(RichTextSpan("Emergency", link = link), RichTextSpan(" room")))
        // La y non si stacca dalle altre lettere: è dov'è senza collegamento.
        assertEquals(plain.getBoundingBox(y).left, linked.getBoundingBox(y).left, 0.5f)
        assertEquals(plain.getBoundingBox(y).right, linked.getBoundingBox(y).right, 0.5f)
        // La freccia comincia dopo la y e finisce prima della parola dopo,
        // che si è spostata per farle posto.
        val (from, to) = blockLinkArrowSpan(linked.getBoundingBox(y).right, fontPx)
        assertTrue(from > linked.getBoundingBox(y).right)
        val room = linked.getBoundingBox(y + 2).left
        assertTrue(room > plain.getBoundingBox(y + 2).left + (to - from))
        assertTrue(to < room)
    }

    @Test
    fun atTheEndOfTheLineThereIsAlreadyRoom() {
        // Come le righe di "Fast notes": il collegamento chiude la riga.
        val plain = measurer.measure(AnnotatedString("Emergency"), style)
        val linked = measure(listOf(RichTextSpan("Emergency", link = link)))
        assertEquals(plain.getBoundingBox(y).left, linked.getBoundingBox(y).left, 0.5f)
        assertEquals(plain.size.width, linked.size.width)
    }

    @Test
    fun aLinkInTwoPiecesHasOneArrowAtTheEnd() {
        // "Emer" in grassetto e "gency": lo spazio va dopo la fine, non in mezzo.
        val plain = measurer.measure(AnnotatedString("Emergency room"), style)
        val linked = measure(
            listOf(RichTextSpan("Emer", link = link), RichTextSpan("gency", link = link), RichTextSpan(" room"))
        )
        assertEquals(plain.getBoundingBox(y).right, linked.getBoundingBox(y).right, 0.5f)
        assertTrue(linked.getBoundingBox(y + 2).left > plain.getBoundingBox(y + 2).left)
    }
}
