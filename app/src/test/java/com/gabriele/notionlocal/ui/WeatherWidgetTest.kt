package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.MAX_WEATHER_CITIES
import com.gabriele.notionlocal.data.widgets.WeatherCity
import com.gabriele.notionlocal.data.widgets.WeatherWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.parseForecast
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
 * **Il widget del meteo** (17/10/2026): le città (al massimo dieci, la prima
 * è la principale) e la pagina di una città disegnata col meteo vero di
 * Bergamo, senza rete. L'immagine va in `app/build/widget-screenshots/`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeatherWidgetTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var widgetId: String

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSettings.init(context)
        WidgetStore.init(context)
        WidgetStore.add(WidgetKind.WEATHER)
        widgetId = WidgetStore.state.widgets.filterIsInstance<WeatherWidget>().last().id
    }

    @After
    fun tearDown() {
        WidgetStore.remove(widgetId)
    }

    private fun weather() = WidgetStore.state.widgets.filterIsInstance<WeatherWidget>().single { it.id == widgetId }

    private fun city(name: String) = WeatherCity(name = name, region = "Italia", latitude = 45.0, longitude = 9.0)

    @Test
    fun citiesAddMoveReplaceRemove() {
        listOf("Bergamo", "Milano", "Roma").forEach { WidgetStore.addWeatherCity(widgetId, city(it)) }
        assertEquals(listOf("Bergamo", "Milano", "Roma"), weather().cities.map { it.name })
        // Roma principale: in cima.
        val roma = weather().cities.last()
        WidgetStore.moveWeatherCity(widgetId, roma.id, -2)
        assertEquals(listOf("Roma", "Bergamo", "Milano"), weather().cities.map { it.name })
        // Milano diventa Napoli, nello stesso posto.
        val milano = weather().cities.last()
        WidgetStore.replaceWeatherCity(widgetId, milano.id, city("Napoli"))
        assertEquals(listOf("Roma", "Bergamo", "Napoli"), weather().cities.map { it.name })
        WidgetStore.removeWeatherCity(widgetId, roma.id)
        assertEquals(listOf("Bergamo", "Napoli"), weather().cities.map { it.name })
        // Non più di dieci.
        repeat(15) { WidgetStore.addWeatherCity(widgetId, city("Città $it")) }
        assertEquals(MAX_WEATHER_CITIES, weather().cities.size)
    }

    @Test
    fun aCityPageWithItsWeather() {
        WidgetStore.addWeatherCity(widgetId, WeatherCity(name = "Bergamo", region = "Lombardia, Italia", latitude = 45.696, longitude = 9.667))
        WidgetStore.addWeatherCity(widgetId, city("Milano"))
        val json = javaClass.classLoader!!.getResourceAsStream("weather/forecast_bergamo.json")!!.bufferedReader().readText()
        val bergamo = weather().cities.first()
        WidgetStore.setWeatherReport(widgetId, bergamo.id, parseForecast(json, System.currentTimeMillis())!!)
        // Solo il meteo: gli altri widget restano fuori dall'immagine.
        val others = WidgetStore.state.widgets.filter { it.id != widgetId }
        others.forEach { WidgetStore.remove(it.id) }

        compose.setContent {
            NotionLocalTheme {
                // Barra chiusa: niente rete nel test.
                Column(modifier = Modifier.width(300.dp).background(SidebarBackground)) { WidgetsSection(active = false) }
            }
        }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("Bergamo").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("20°").fetchSemanticsNodes().isNotEmpty())
        val root = compose.activity.window.decorView.rootView as android.view.ViewGroup
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        val dir = File("build/widget-screenshots").apply { mkdirs() }
        val crop = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, minOf(bitmap.height, 700))
        File(dir, "weather.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
