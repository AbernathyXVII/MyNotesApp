package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.ui.screen.CaretAnchor
import com.gabriele.notionlocal.ui.screen.CaretInView
import com.gabriele.notionlocal.ui.screen.followCaretOnly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La pagina segue il cursore, non il pezzo di testo** (19/10/2026, chiesto
 * dall'utente): un pezzo di 60 righe che comincia a metà schermo. Toccato in
 * mezzo, Compose portava in vista tutto il campo (l'inizio in cima: la pagina
 * saltava su); accorciato lo schermo dalla tastiera, lo stesso. Ora la pagina
 * si muove solo se il cursore finirebbe coperto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class CaretInViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var list: LazyListState
    private val text = (1..60).joinToString("\n") { "Riga $it" }
    private var value by mutableStateOf(TextFieldValue(text))

    /** Quanto è lungo lo schermo: la tastiera lo accorcia. */
    private var viewport by mutableStateOf(800.dp)

    /** Sopra 300dp di altro, poi il campo: 60 righe da 20dp. */
    private fun show(fixed: Boolean) {
        compose.setContent {
            list = rememberLazyListState()
            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            val caret = remember { CaretInView() }
            caret.caretRect = { layout?.getCursorRect(value.selection.end)?.let { Rect(it.left, it.top, it.right + 1f, it.bottom) } }
            Column {
                LazyColumn(state = list, modifier = Modifier.fillMaxWidth().height(viewport)) {
                    item { Spacer(modifier = Modifier.height(300.dp)) }
                    item {
                        Box(modifier = if (fixed) Modifier.followCaretOnly(caret) else Modifier) {
                            if (fixed) CaretAnchor(caret)
                            BasicTextField(
                                value = value,
                                onValueChange = { value = it },
                                onTextLayout = { layout = it },
                                textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                                modifier = Modifier.fillMaxWidth().testTag("field")
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(300.dp)) }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Di quanto è scesa la pagina. */
    private fun scrolled(): Dp = with(compose.density) {
        if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toDp() else 10_000.dp
    }

    /** Un tocco sul campo, `y` dp sotto il suo inizio: il campo prende il fuoco. */
    private fun tapField(y: Dp) {
        compose.onNodeWithTag("field").performTouchInput {
            click(Offset(50f, with(compose.density) { y.toPx() }))
        }
        compose.waitForIdle()
    }

    /** Il cursore all'inizio della riga `line`, (line - 1) × 20dp sotto l'inizio del campo. */
    private fun caretAtLine(line: Int) {
        compose.runOnUiThread { value = value.copy(selection = TextRange(text.indexOf("Riga $line\n"))) }
        compose.waitForIdle()
    }

    @Test
    fun withoutThePageJumpedToTheStartOfThePiece() {
        // Com'era: toccato a 200dp sotto l'inizio (500dp dal bordo, ben in vista).
        show(fixed = false)
        tapField(200.dp)
        assertTrue("scesa di ${scrolled()}", scrolled() > 250.dp)
    }

    @Test
    fun aTapInViewMovesNothing() {
        show(fixed = true)
        tapField(200.dp)
        assertEquals(0.dp, scrolled())
    }

    @Test
    fun theKeyboardMovesThePageOnlyIfItCoversTheCursor() {
        show(fixed = true)
        tapField(40.dp)
        // Il cursore sulla riga 3, a 340dp dal bordo.
        caretAtLine(3)
        assertEquals(0.dp, scrolled())
        // La tastiera accorcia lo schermo a 500dp: il cursore resta in vista, niente si muove.
        viewport = 500.dp
        compose.waitForIdle()
        assertEquals(0.dp, scrolled())
        // Si chiude, il cursore va sulla riga 21 (a 700dp), e la tastiera si riapre: lo coprirebbe.
        viewport = 800.dp
        compose.waitForIdle()
        caretAtLine(21)
        assertEquals(0.dp, scrolled())
        viewport = 500.dp
        compose.waitForIdle()
        // La pagina sale di quanto basta per il cursore (720 - 500), non fino all'inizio del pezzo (300).
        val moved = scrolled()
        assertTrue("salita di $moved", moved in 200.dp..260.dp)
    }
}
