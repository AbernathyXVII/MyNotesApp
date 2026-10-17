package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.MAX_SPORT_COMPETITIONS
import com.gabriele.notionlocal.data.widgets.SPORT_CATALOG
import com.gabriele.notionlocal.data.widgets.Sport
import com.gabriele.notionlocal.data.widgets.SportReport
import com.gabriele.notionlocal.data.widgets.SportsWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.buildRounds
import com.gabriele.notionlocal.data.widgets.currentRound
import com.gabriele.notionlocal.data.widgets.parseScoreboard
import com.gabriele.notionlocal.data.widgets.parseStandings
import com.gabriele.notionlocal.ui.screen.TeamLogos
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
import java.time.LocalDate

/**
 * **Il widget dello sport** (17/10/2026): le competizioni (niente doppioni,
 * al massimo dieci), la giornata della Serie A disegnata con le partite
 * vere, e le frecce per passare all'NBA — senza rete (niente stemmi).
 * L'immagine va in `app/build/widget-screenshots/`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SportsWidgetTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var widgetId: String

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSettings.init(context)
        WidgetStore.init(context)
        TeamLogos.enabled = false
        WidgetStore.add(WidgetKind.SPORTS)
        widgetId = WidgetStore.state.widgets.filterIsInstance<SportsWidget>().last().id
    }

    @After
    fun tearDown() {
        WidgetStore.remove(widgetId)
        TeamLogos.enabled = true
    }

    private fun sports() = WidgetStore.state.widgets.filterIsInstance<SportsWidget>().single { it.id == widgetId }

    private fun resource(name: String) = javaClass.classLoader!!.getResourceAsStream("sports/$name")!!.bufferedReader().readText()

    @Test
    fun competitionsAddMoveRemove() {
        val serieA = SPORT_CATALOG.first { it.league == "ita.1" }
        val nba = SPORT_CATALOG.first { it.league == "nba" }
        WidgetStore.addSportCompetition(widgetId, serieA.toCompetition())
        WidgetStore.addSportCompetition(widgetId, nba.toCompetition())
        // La stessa competizione due volte no.
        WidgetStore.addSportCompetition(widgetId, serieA.toCompetition())
        assertEquals(listOf("Serie A", "NBA"), sports().competitions.map { it.name })
        WidgetStore.moveSportCompetition(widgetId, sports().competitions.last().id, -1)
        assertEquals(listOf("NBA", "Serie A"), sports().competitions.map { it.name })
        WidgetStore.removeSportCompetition(widgetId, sports().competitions.first().id)
        assertEquals(listOf("Serie A"), sports().competitions.map { it.name })
        SPORT_CATALOG.forEach { WidgetStore.addSportCompetition(widgetId, it.toCompetition()) }
        assertEquals(MAX_SPORT_COMPETITIONS, sports().competitions.size)
    }

    @Test
    fun theRoundOfSerieAAndTheArrows() {
        WidgetStore.addSportCompetition(widgetId, SPORT_CATALOG.first { it.league == "ita.1" }.toCompetition())
        WidgetStore.addSportCompetition(widgetId, SPORT_CATALOG.first { it.league == "nba" }.toCompetition())
        val (serieA, nba) = sports().competitions
        val now = System.currentTimeMillis()
        val serieABoard = parseScoreboard(resource("seriea_scoreboard.json"), Sport.SOCCER)!!
        val serieARounds = buildRounds(serieABoard, Sport.SOCCER)
        WidgetStore.setSportReport(
            widgetId,
            serieA.id,
            SportReport(
                fetchedAt = now,
                matches = serieABoard.matches,
                standings = parseStandings(resource("soccer_ita_1_standings.json"), Sport.SOCCER),
                standingsFetchedAt = now,
                rounds = serieARounds,
                roundsFetchedAt = now,
                round = currentRound(serieARounds, LocalDate.of(2026, 10, 17))
            )
        )
        val nbaBoard = parseScoreboard(resource("basketball_nba_scoreboard.json"), Sport.BASKETBALL)!!
        val nbaRounds = buildRounds(nbaBoard, Sport.BASKETBALL)
        WidgetStore.setSportReport(
            widgetId,
            nba.id,
            SportReport(
                fetchedAt = now,
                matches = nbaBoard.matches,
                rounds = nbaRounds,
                roundsFetchedAt = now,
                round = currentRound(nbaRounds, LocalDate.of(2026, 10, 16))
            )
        )
        WidgetStore.state.widgets.filter { it.id != widgetId }.forEach { WidgetStore.remove(it.id) }

        compose.setContent {
            NotionLocalTheme {
                // Barra chiusa: niente rete nel test.
                Column(modifier = Modifier.width(300.dp).background(SidebarBackground)) { WidgetsSection(active = false) }
            }
        }
        compose.waitForIdle()
        fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        // La prima competizione, con la bandiera, e la sua giornata intera.
        assertTrue(shows("Serie A"))
        assertTrue(shows("🇮🇹"))
        assertTrue(shows("Matchday 7 · Fri 16 – Mon 19 Oct"))
        assertTrue(shows("Venezia"))
        assertTrue(shows("Genoa"))
        // La prima: la freccia a sinistra è grigia.
        compose.onNodeWithContentDescription("Previous competition").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next competition").assertIsEnabled()
        saveScreenshot("sports.png")

        // A destra c'è l'NBA, l'ultima: ora è grigia la freccia a destra.
        compose.onNodeWithContentDescription("Next competition").performClick()
        compose.waitForIdle()
        assertTrue(shows("NBA"))
        assertTrue(shows("Magic"))
        assertTrue(shows("Fri 16 Oct"))
        compose.onNodeWithContentDescription("Previous competition").assertIsEnabled()
        compose.onNodeWithContentDescription("Next competition").assertIsNotEnabled()
    }

    @Test
    fun logoAddresses() {
        val juventus = "https://a.espncdn.com/i/teamlogos/soccer/500/111.png"
        // Lo stemma per i fondi scuri, piccolo; le bandiere restano come sono.
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500-dark/111.png", TeamLogos.darkUrl(juventus))
        assertEquals("https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/111.png&w=64&h=64", TeamLogos.smallUrl(juventus))
        val italy = "https://a.espncdn.com/i/teamlogos/countries/500/ita.png"
        assertEquals(italy, TeamLogos.darkUrl(italy))
        assertEquals("https://example.com/a.png", TeamLogos.smallUrl("https://example.com/a.png"))
    }

    private fun saveScreenshot(name: String) {
        val root = compose.activity.window.decorView.rootView as android.view.ViewGroup
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        val dir = File("build/widget-screenshots").apply { mkdirs() }
        val crop = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, minOf(bitmap.height, 900))
        File(dir, name).outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
