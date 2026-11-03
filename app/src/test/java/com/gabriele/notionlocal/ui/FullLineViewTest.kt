package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.LocalSideItemKey
import com.gabriele.notionlocal.ui.screen.LocalSideScroll
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.ui.screen.SideScroll
import com.gabriele.notionlocal.ui.screen.SideScrollEffects
import com.gabriele.notionlocal.ui.screen.rememberSideLane
import com.gabriele.notionlocal.ui.screen.sideLane
import com.gabriele.notionlocal.ui.screen.sideScrollGestures
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **"Full line view"** (19/10/2026, chiesto dall'utente: le righe lunghe non
 * vanno a capo e si scorrono di lato, "falla bene"): una riga lunga in cima,
 * un riquadro fermo sotto (come un database) e, volendo, tante righe corte.
 * Schermo di 400dp a 1px per dp.
 */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class FullLineViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var list: LazyListState
    private lateinit var side: SideScroll
    private val long = "Una riga molto lunga che non sta nello schermo ".repeat(12).trim()
    private var value by mutableStateOf(TextFieldValue(long))
    private var layout: TextLayoutResult? = null

    /** "Full line view" acceso o spento, mentre la pagina è aperta. */
    private var fullLines by mutableStateOf(true)

    @After
    fun tearDown() {
        if (::side.isInitialized) side.offset = 0f
        AppSettings.init(ApplicationProvider.getApplicationContext())
        AppSettings.fullLineView = true
    }

    private fun show(shortLines: Int = 0) {
        compose.setContent {
            list = rememberLazyListState()
            val saved = remember { mutableFloatStateOf(0f) }
            side = remember { SideScroll(list, saved) }
            val scroll = if (fullLines) side else null
            CompositionLocalProvider(LocalSideScroll provides scroll) {
                Box {
                    LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxWidth().height(800.dp).sideScrollGestures(scroll, enabled = true).testTag("page")
                    ) {
                        item(key = "long") {
                            CompositionLocalProvider(LocalSideItemKey provides "long") { LongLine() }
                        }
                        // Fermo: non è una corsia, come un database o un'immagine.
                        item(key = "fixed") { Box(modifier = Modifier.fillMaxWidth().height(100.dp).testTag("fixed")) }
                        items((1..shortLines).toList(), key = { "short$it" }) { i ->
                            CompositionLocalProvider(LocalSideItemKey provides "short$i") {
                                val lane = rememberSideLane()
                                BasicText("Corta $i", modifier = Modifier.fillMaxWidth().height(100.dp).sideLane(lane))
                            }
                        }
                    }
                    if (scroll != null) SideScrollEffects(scroll, drawing = false)
                }
            }
        }
        compose.waitForIdle()
    }

    /** Come il campo di un'isola: la sua corsia, e il cursore portato in vista mentre si scrive. */
    @Composable
    private fun LongLine() {
        val lane = rememberSideLane()
        var shown by remember { mutableStateOf<TextLayoutResult?>(null) }
        var focused by remember { mutableStateOf(false) }
        val caret = remember { BringIntoViewRequester() }
        LaunchedEffect(value.selection, shown, focused) {
            val now = shown
            if (lane != null && focused && now != null) caret.bringIntoView(now.getCursorRect(value.selection.end))
        }
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            onTextLayout = {
                shown = it
                layout = it
            },
            textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            modifier = Modifier
                .fillMaxWidth()
                .sideLane(lane)
                .bringIntoViewRequester(caret)
                .onFocusChanged { focused = it.isFocused }
                .testTag("long")
        )
    }

    private fun swipeSideways(dx: Float, dy: Float = 0f) {
        compose.onNodeWithTag("page").performTouchInput {
            swipe(start = Offset(300f, 400f), end = Offset(300f + dx, 400f + dy), durationMillis = 300)
        }
        compose.waitForIdle()
    }

    @Test
    fun longLinesDoNotWrap() {
        fullLines = false
        show()
        assertTrue("con le righe che vanno a capo: ${layout?.lineCount} righe", (layout?.lineCount ?: 0) > 1)
        fullLines = true
        compose.waitForIdle()
        assertEquals(1, layout?.lineCount)
        assertTrue((layout?.size?.width ?: 0) > 2000)
    }

    @Test
    fun swipingSidewaysStopsAtTheEndOfTheLine() {
        show()
        repeat(12) { swipeSideways(-350f) }
        val textWidth = layout!!.size.width.toFloat()
        // Fino alla fine della riga, con 32dp d'aria, e non oltre.
        assertEquals(textWidth + 32f - 400f, side.offset, 1f)
        // Il riquadro fermo non si è mosso.
        assertEquals(0f, compose.onNodeWithTag("fixed").getUnclippedBoundsInRoot().left.value, 0.5f)
        // E indietro fino all'inizio, non prima.
        repeat(12) { swipeSideways(350f) }
        assertEquals(0f, side.offset, 0.5f)
    }

    @Test
    fun theFirstDirectionDecidesTheWholeGesture() {
        show(shortLines = 20)
        // Quasi in verticale: la pagina scende, le righe restano dove sono.
        swipeSideways(dx = -40f, dy = -300f)
        assertEquals(0f, side.offset, 0.5f)
        assertTrue(list.firstVisibleItemScrollOffset > 0 || list.firstVisibleItemIndex > 0)
        compose.runOnUiThread { runBlocking { list.scrollToItem(0) } }
        compose.waitForIdle()
        // Quasi di lato: le righe scorrono, la pagina resta ferma.
        swipeSideways(dx = -300f, dy = -40f)
        assertTrue("di lato: ${side.offset}", side.offset > 100f)
        assertEquals(0, list.firstVisibleItemIndex)
        assertEquals(0, list.firstVisibleItemScrollOffset)
    }

    @Test
    fun shorterLinesOnScreenBringTheRowsBack() {
        show(shortLines = 20)
        repeat(12) { swipeSideways(-350f) }
        assertTrue(side.offset > 1000f)
        // Scesi dove le righe sono tutte corte: non resta uno schermo vuoto, si torna all'inizio.
        compose.runOnUiThread { runBlocking { list.scrollToItem(5) } }
        compose.waitForIdle()
        assertEquals(0f, side.reach, 0.5f)
        assertEquals(0f, side.offset, 0.5f)
    }

    @Test
    fun theCaretStaysInViewWhileTyping() {
        show()
        compose.onNodeWithTag("long").performTouchInput { click(Offset(10f, 10f)) }
        compose.waitForIdle()
        // In fondo alla riga: le righe scorrono quanto basta per vederlo.
        compose.runOnUiThread { value = value.copy(selection = TextRange(long.length)) }
        compose.waitForIdle()
        val caretX = layout!!.getCursorRect(long.length).left - side.offset
        assertTrue("il cursore è a $caretX", caretX in 0f..400f)
        assertTrue(side.offset > 1000f)
        // A capo, all'inizio della riga: si torna all'inizio.
        compose.runOnUiThread { value = value.copy(selection = TextRange(0)) }
        compose.waitForIdle()
        assertEquals(0f, side.offset, 0.5f)
    }

    @Test
    fun theSettingIsOnByDefaultAndSaved() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSettings.init(context)
        assertTrue(AppSettings.fullLineView)
        compose.setContent { NotionLocalTheme { SettingsScreen(onBack = {}) } }
        compose.onNodeWithText("Full line view").performScrollTo().performClick()
        compose.waitForIdle()
        assertFalse(AppSettings.fullLineView)
    }
}
