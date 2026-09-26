package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CounterWidget
import com.gabriele.notionlocal.data.widgets.LifeProgressWidget
import com.gabriele.notionlocal.data.widgets.ProgressBarSetting
import com.gabriele.notionlocal.data.widgets.ProgressKind
import com.gabriele.notionlocal.data.widgets.TimeZonesWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.ui.screen.WidgetsSection
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * **I widget disegnati davvero**, su un'immagine: in cloud il telefono non
 * c'è, e questo è il modo di guardarli. Il test passa se si disegnano senza
 * errori; l'immagine finisce in `app/build/widget-screenshots/` per
 * guardarla a occhio (non è un confronto automatico).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetsRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * La finestra disegnata su un'immagine, a mano: `captureToImage` con
     * Robolectric aspetta un fotogramma che non arriva mai.
     */
    private fun snapshot(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val content = (root as android.view.ViewGroup)
        val bitmap = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)
        content.draw(android.graphics.Canvas(bitmap))
        // Solo la parte disegnata: la finestra è alta, i widget no. Si
        // taglia dove finisce la colonna larga 300 (a sinistra del bordo
        // destro della finestra, che è solo sfondo).
        val probeX = (bitmap.width * 0.3).toInt()
        val empty = bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)
        var bottom = bitmap.height - 1
        while (bottom > 0 && bitmap.getPixel(probeX, bottom) == empty) bottom--
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, (bottom + 1).coerceAtLeast(1))
        val dir = File("build/widget-screenshots").apply { mkdirs() }
        File(dir, name).outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun allWidgetsRender() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSettings.init(context)
        WidgetStore.init(context)
        WidgetKind.entries.forEach { WidgetStore.add(it) }
        val state = WidgetStore.state
        val zones = state.widgets.filterIsInstance<TimeZonesWidget>().single()
        WidgetStore.addClock(zones.id, "America/Los_Angeles")
        WidgetStore.addClock(zones.id, "Asia/Tokyo")
        val tokyo = WidgetStore.state.widgets.filterIsInstance<TimeZonesWidget>().single().clocks.last()
        WidgetStore.updateClock(zones.id, tokyo.id) { it.copy(analog = false) }
        listOf("Asia/Kolkata", "Pacific/Honolulu", "Europe/Rome", "America/New_York").forEach {
            WidgetStore.addToZoneList(zones.id, it)
        }
        val life = state.widgets.filterIsInstance<LifeProgressWidget>().single()
        WidgetStore.addBar(
            life.id,
            ProgressBarSetting(
                kind = ProgressKind.CUSTOM,
                name = "Esame",
                startEpochDay = LocalDate.now().minusDays(20).toEpochDay(),
                endEpochDay = LocalDate.now().plusDays(10).toEpochDay(),
                colorHex = "#D9534F"
            )
        )
        val counter = state.widgets.filterIsInstance<CounterWidget>().single()
        repeat(3) { WidgetStore.changeCounter(counter.id, +1) }
        WidgetStore.renameCounter(counter.id, "Caffè")

        compose.setContent {
            NotionLocalTheme {
                Column(modifier = Modifier.width(300.dp).background(SidebarBackground)) {
                    WidgetsSection(active = false)
                }
            }
        }
        snapshot("widgets.png")

        // E chiusa: resta solo il titolo con la freccetta.
        WidgetStore.setHidden(true)
        snapshot("widgets-hidden.png")
        WidgetStore.setHidden(false)
    }
}
