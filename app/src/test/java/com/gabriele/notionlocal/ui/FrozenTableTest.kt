package com.gabriele.notionlocal.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.screen.FrozenColumn
import com.gabriele.notionlocal.ui.screen.LocalStickyViewport
import com.gabriele.notionlocal.ui.screen.PinnedHeaderSection
import com.gabriele.notionlocal.ui.screen.StickyViewport
import com.gabriele.notionlocal.ui.screen.frozenFirstCell
import com.gabriele.notionlocal.ui.screen.stickyViewport
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Prima colonna e prima riga ferme** (08/10/2026): scorrendo a destra la
 * prima cella resta al bordo sinistro, scorrendo in giù la riga dei titoli
 * resta in cima — sotto il margine chiesto, e mai oltre la sua tabella.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class FrozenTableTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var vertical: ScrollState
    private lateinit var horizontal: ScrollState

    /** Sopra la tabella 100dp di altro, poi l'intestazione (40dp) e 20 righe da 40dp; dopo, 600dp di altro. */
    private fun show(pin: Boolean, inset: Int = 0) {
        compose.setContent {
            NotionLocalTheme {
                vertical = rememberScrollState()
                horizontal = rememberScrollState()
                val viewport = remember { StickyViewport.forScroll(vertical, inset.dp) }
                CompositionLocalProvider(LocalStickyViewport provides viewport) {
                    Column(modifier = Modifier.size(300.dp, 400.dp).verticalScroll(vertical).stickyViewport(viewport)) {
                        Spacer(modifier = Modifier.height(100.dp))
                        Column(modifier = Modifier.horizontalScroll(horizontal)) {
                            PinnedHeaderSection(pin = pin, header = {
                                Box(modifier = Modifier.testTag("header").size(600.dp, 40.dp))
                            }) {
                                repeat(20) { index ->
                                    Row {
                                        Box(
                                            modifier = Modifier
                                                .frozenFirstCell(FrozenColumn(horizontal))
                                                .testTag("first$index")
                                                .size(100.dp, 40.dp)
                                        )
                                        Box(modifier = Modifier.width(500.dp).height(40.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(600.dp))
                    }
                }
            }
        }
    }

    @Test
    fun theHeaderStaysOnTopWhileItsTableScrollsBy_thenLeavesWithIt() {
        show(pin = true)
        compose.onNodeWithTag("header").assertTopPositionInRootIsEqualTo(100.dp)
        runBlocking { vertical.scrollTo(300) }
        compose.waitForIdle()
        compose.onNodeWithTag("header").assertTopPositionInRootIsEqualTo(0.dp)
        // Oltre la fine della tabella (100 + 40 + 800 = 940) se ne va con lei:
        // il suo fondo coincide con quello dell'ultima riga.
        runBlocking { vertical.scrollTo(1000) }
        compose.waitForIdle()
        compose.onNodeWithTag("header").assertTopPositionInRootIsEqualTo((900 - 1000).dp)
    }

    @Test
    fun theHeaderStopsUnderTheInset() {
        show(pin = true, inset = 56)
        runBlocking { vertical.scrollTo(300) }
        compose.waitForIdle()
        compose.onNodeWithTag("header").assertTopPositionInRootIsEqualTo(56.dp)
    }

    @Test
    fun turnedOffTheHeaderScrollsAway() {
        show(pin = false)
        runBlocking { vertical.scrollTo(300) }
        compose.waitForIdle()
        compose.onNodeWithTag("header").assertTopPositionInRootIsEqualTo((-200).dp)
    }

    @Test
    fun theFirstCellStaysOnTheLeftWhileTheTableScrollsRight() {
        show(pin = false)
        compose.onNodeWithTag("first0").assertLeftPositionInRootIsEqualTo(0.dp)
        runBlocking { horizontal.scrollTo(250) }
        compose.waitForIdle()
        compose.onNodeWithTag("first0").assertLeftPositionInRootIsEqualTo(0.dp)
        compose.onNodeWithTag("first3").assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun inAScrollingPageTheHeaderStaysUnderTheFloatingButtons() {
        // Come nella pagina: una LazyColumn, la tabella in una delle sue righe.
        lateinit var list: LazyListState
        compose.setContent {
            NotionLocalTheme {
                list = rememberLazyListState()
                val viewport = remember { StickyViewport.forList(list, 56.dp) }
                CompositionLocalProvider(LocalStickyViewport provides viewport) {
                    LazyColumn(state = list, modifier = Modifier.size(300.dp, 400.dp).stickyViewport(viewport)) {
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                        item {
                            Column {
                                PinnedHeaderSection(pin = true, header = {
                                    Box(modifier = Modifier.testTag("header").size(300.dp, 40.dp))
                                }) {
                                    repeat(20) { Box(modifier = Modifier.size(300.dp, 40.dp)) }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(600.dp)) }
                    }
                }
            }
        }
        runBlocking { list.scrollToItem(1, 300) }
        compose.waitForIdle()
        compose.onNodeWithTag("header").assertTopPositionInRootIsEqualTo(56.dp)
        runBlocking { list.scrollBy(100f) }
        compose.waitForIdle()
        compose.onNodeWithTag("header").assertTopPositionInRootIsEqualTo(56.dp)
    }
}
