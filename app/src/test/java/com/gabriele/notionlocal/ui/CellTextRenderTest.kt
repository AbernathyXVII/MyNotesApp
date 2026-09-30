package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.screen.LocalWrapCells
import com.gabriele.notionlocal.ui.screen.TextLikeCell
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **La cella di testo** (30/09/2026): quello che non ci sta finisce coi tre
 * puntini, e toccandola compare il riquadro grigio col testo intero, dove si
 * scrive; con "Wrap cells" va a capo e basta.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
// Il testo misurato davvero: senza, Robolectric non va mai a capo né taglia niente.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CellTextRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val long = "Un testo lunghissimo che in una colonna stretta non ci sta per niente"

    private fun cell(wrap: Boolean, onChange: (String) -> Unit = {}) {
        compose.setContent {
            NotionLocalTheme {
                var value by remember { mutableStateOf(long) }
                CompositionLocalProvider(LocalWrapCells provides wrap) {
                    Box(modifier = Modifier.width(96.dp)) {
                        TextLikeCell(
                            value = value,
                            onValueChange = {
                                value = it
                                onChange(it)
                            },
                            keyboardOptions = KeyboardOptions.Default,
                            cellKey = "r:c"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun aCutTextOpensTheWholeTextOnTap() {
        var last = ""
        cell(wrap = false) { last = it }
        // Una sola cella col testo, coi puntini.
        compose.onNodeWithText(long).assertIsDisplayed().performClick()
        compose.waitForIdle()
        // Ora il testo c'è due volte: nella cella e nel riquadro grigio sopra.
        assertEquals(2, compose.onAllNodesWithText(long, substring = true).fetchSemanticsNodes().size)
        // E nel riquadro ci si scrive.
        compose.onAllNodesWithText(long)[1].performTextInput("!")
        compose.waitForIdle()
        assertTrue(last, last.endsWith("!"))
    }

    @Test
    fun aWrappedTextIsTallInsteadOfCut() {
        cell(wrap = true)
        val node = compose.onNodeWithText(long).fetchSemanticsNode()
        // Più righe: ben più alta di una riga di testo (20 punti).
        assertTrue(node.size.height > 60 * 2)
    }
}
