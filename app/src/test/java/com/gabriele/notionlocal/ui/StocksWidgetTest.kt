package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.NumberStyle
import com.gabriele.notionlocal.data.widgets.MAX_STOCKS
import com.gabriele.notionlocal.data.widgets.StockItem
import com.gabriele.notionlocal.data.widgets.StocksWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.parseSpark
import com.gabriele.notionlocal.ui.screen.WidgetsSection
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * **Il widget della borsa** (17/10/2026): i titoli (niente doppioni, al
 * massimo trenta, nell'ordine scelto) e la lista disegnata coi prezzi veri,
 * come nella foto dell'utente. L'immagine va in `app/build/widget-screenshots/`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StocksWidgetTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var widgetId: String
    private lateinit var numberStyle: NumberStyle

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSettings.init(context)
        WidgetStore.init(context)
        numberStyle = AppSettings.numberStyle
        WidgetStore.add(WidgetKind.STOCKS)
        widgetId = WidgetStore.state.widgets.filterIsInstance<StocksWidget>().last().id
    }

    @After
    fun tearDown() {
        WidgetStore.remove(widgetId)
        Snapshot.withMutableSnapshot { AppSettings.numberStyle = numberStyle }
        Snapshot.sendApplyNotifications()
    }

    private fun stocks() = WidgetStore.state.widgets.filterIsInstance<StocksWidget>().single { it.id == widgetId }

    @Test
    fun stocksAddMoveRemove() {
        WidgetStore.addStock(widgetId, StockItem("LDO.MI", "Leonardo"))
        WidgetStore.addStock(widgetId, StockItem("ENEL.MI", "Enel"))
        WidgetStore.addStock(widgetId, StockItem("LDO.MI", "Leonardo"))
        assertEquals(listOf("LDO.MI", "ENEL.MI"), stocks().items.map { it.symbol })
        WidgetStore.moveStock(widgetId, "ENEL.MI", -1)
        assertEquals(listOf("ENEL.MI", "LDO.MI"), stocks().items.map { it.symbol })
        WidgetStore.removeStock(widgetId, "ENEL.MI")
        assertEquals(listOf("LDO.MI"), stocks().items.map { it.symbol })
        repeat(40) { WidgetStore.addStock(widgetId, StockItem("T$it", "Titolo $it")) }
        assertEquals(MAX_STOCKS, stocks().items.size)
    }

    @Test
    fun theListLikeThePhoto() {
        Snapshot.withMutableSnapshot { AppSettings.numberStyle = NumberStyle.DOT_GROUPS }
        Snapshot.sendApplyNotifications()
        listOf("FTSEMIB.MI" to "FTSE MIB", "LDO.MI" to "Leonardo S.p.a.", "ENEL.MI" to "Enel SpA").forEach { (symbol, name) ->
            WidgetStore.addStock(widgetId, StockItem(symbol, name))
        }
        val json = javaClass.classLoader!!.getResourceAsStream("stocks/spark.json")!!.bufferedReader().readText()
        WidgetStore.setStockQuotes(widgetId, parseSpark(json), System.currentTimeMillis())
        WidgetStore.state.widgets.filter { it.id != widgetId }.forEach { WidgetStore.remove(it.id) }

        compose.setContent {
            NotionLocalTheme {
                Column(modifier = Modifier.width(300.dp).background(SidebarBackground)) { WidgetsSection(active = false) }
            }
        }
        compose.waitForIdle()
        // Come nella foto: sigla, variazione col segno, prezzo coi separatori di Settings.
        assertTrue(compose.onAllNodesWithText("FTSEMIB").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("+0,93%").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("49.857,18").fetchSemanticsNodes().isNotEmpty())
        // 4,725% arrotondato come si scrive, non come lo conserva il computer (4,7249…).
        assertTrue(compose.onAllNodesWithText("+4,73%").fetchSemanticsNodes().isNotEmpty())
        val root = compose.activity.window.decorView.rootView as android.view.ViewGroup
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        val dir = File("build/widget-screenshots").apply { mkdirs() }
        val crop = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, minOf(bitmap.height, 600))
        File(dir, "stocks.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
