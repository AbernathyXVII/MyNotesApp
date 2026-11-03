package com.gabriele.notionlocal.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.proofing.SpellChecker
import com.gabriele.notionlocal.data.proofing.SpellDictionaryTest
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.LocalProofing
import com.gabriele.notionlocal.ui.screen.MisspelledWord
import com.gabriele.notionlocal.ui.screen.PageProofing
import com.gabriele.notionlocal.ui.screen.ProofingMenu
import com.gabriele.notionlocal.ui.screen.ProofingTransformation
import com.gabriele.notionlocal.ui.screen.drawMisspellings
import com.gabriele.notionlocal.ui.screen.proofingTaps
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.NotionWhite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Il correttore disegnato: un campo con due parole sbagliate e la riga
 * ondulata sotto (l'immagine va in `app/build/widget-screenshots/proofing.png`,
 * da guardare a occhio), e un tocco su una di loro che apre il suo menu.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofingRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun underlinesAndMenu() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        val checker = SpellChecker(SpellDictionaryTest.it)
        val text = "Domani vado a squola perche c'è il compito di Mario, che è lungo e noioso."
        val ranges = checker.misspelled(text)
        assertEquals(2, ranges.size)
        var opened: MisspelledWord? = null
        var lastLayout: TextLayoutResult? = null
        compose.setContent {
            NotionLocalTheme {
                CompositionLocalProvider(LocalProofing provides PageProofing(checker, {}, {})) {
                    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                    var menu by remember { mutableStateOf<Pair<MisspelledWord, IntRect>?>(null) }
                    Box(modifier = Modifier.width(400.dp).background(DarkBackground).padding(16.dp)) {
                        BasicTextField(
                            value = text,
                            onValueChange = {},
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = NotionWhite,
                                lineHeight = 24.sp,
                                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
                            ),
                            visualTransformation = ProofingTransformation(ranges, null),
                            onTextLayout = {
                                layout = it
                                lastLayout = it
                            },
                            modifier = Modifier
                                .testTag("field")
                                .drawBehind { drawMisspellings(layout) }
                                .proofingTaps(enabled = true, layout = { layout }) { word, at ->
                                    opened = word
                                    menu = word to at
                                }
                        )
                        menu?.let { (word, at) ->
                            ProofingMenu(text.substring(word.start, word.end), at, onReplace = {}, onDismiss = { menu = null })
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView as android.view.ViewGroup
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        val dir = File("build/widget-screenshots").apply { mkdirs() }
        val crop = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, minOf(bitmap.height, 260))
        File(dir, "proofing.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // Un tocco a metà di "squola": si apre il suo menu.
        val squola = ranges.first()
        val target = lastLayout!!.getBoundingBox(squola.first + 3).center
        compose.onNodeWithTag("field").performTouchInput { click(target) }
        compose.waitForIdle()
        assertEquals(MisspelledWord(0, squola.first, squola.last + 1), opened)
        // Il menu è aperto. I suggerimenti arrivano dopo, da un altro filo: Robolectric non
        // ridisegna la finestra del menu quando arrivano, quindi qui si guarda solo che ci sia.
        assertTrue(compose.onAllNodesWithText("Add to dictionary").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Ignore").fetchSemanticsNodes().isNotEmpty())
    }
}
