package com.gabriele.notionlocal.data.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * **Lo sport** (17/10/2026): risposte vere di ESPN (scaricate quel giorno,
 * alleggerite dei campi che l'app non legge, in `resources/sports`) lette
 * come si deve, e la scelta delle partite da mostrare nel widget.
 */
class SportsTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("sports/$name")!!.bufferedReader().readText()

    @Test
    fun serieAToday() {
        val board = parseScoreboard(resource("seriea_scoreboard.json"), Sport.SOCCER)!!
        assertEquals(3, board.matches.size)
        val first = board.matches.first()
        assertEquals("Venezia", first.home.name)
        assertEquals("Napoli", first.away.name)
        assertEquals(MatchState.PRE, first.state)
        // Prima della partita niente "0 - 0".
        assertNull(first.home.score)
        assertEquals(Instant.parse("2026-10-17T13:00:00Z").toEpochMilli(), first.start)
        // Il calendario della stagione, a giorni.
        assertEquals(82, board.matchDays.size)
    }

    @Test
    fun nbaResultsAndNflWeek() {
        val nba = parseScoreboard(resource("basketball_nba_scoreboard.json"), Sport.BASKETBALL)!!
        assertEquals(12, nba.matches.size)
        val game = nba.matches.first()
        assertEquals("Magic", game.home.name)
        assertEquals("136", game.home.score)
        assertTrue(game.home.winner)
        assertEquals("115", game.away.score)
        assertFalse(game.away.winner)
        assertEquals(MatchState.POST, game.state)
        // La NFL va a settimane: il calendario non è fatto di giorni, basta la risposta di partenza.
        val nfl = parseScoreboard(resource("football_nfl_scoreboard.json"), Sport.FOOTBALL)!!
        assertEquals(14, nfl.matches.size)
        assertEquals(13, nfl.matches.count { it.state == MatchState.PRE })
        assertTrue(nfl.matchDays.isEmpty())
    }

    @Test
    fun tennisSinglesWithSets() {
        val board = parseScoreboard(resource("tennis_atp_scoreboard.json"), Sport.TENNIS)!!
        assertEquals(209, board.matches.size)
        val match = board.matches.single { it.id == "186308" }
        assertEquals("A. Ilagan", match.home.name)
        assertEquals(listOf(3, 7, 5), match.home.sets)
        assertEquals("N. Basilashvili", match.away.name)
        assertEquals(listOf(6, 6, 7), match.away.sets)
        assertTrue(match.away.winner)
        assertEquals("Rolex Shanghai Masters · Qualifying 1st Round", match.note)
    }

    @Test
    fun standingsInOrder() {
        val serieA = parseStandings(resource("soccer_ita_1_standings.json"), Sport.SOCCER)
        assertEquals(1, serieA.size)
        assertEquals(listOf("GP", "W", "D", "L", "GD", "P"), serieA.single().columns)
        assertEquals(20, serieA.single().rows.size)
        assertEquals(listOf("Inter Milan", "Lazio", "AS Roma"), serieA.single().rows.take(3).map { it.team })
        assertEquals(listOf("6", "5", "1", "0", "+10", "16"), serieA.single().rows.first().values)
        // NBA: due conferenze, ognuna dalla prima testa di serie (ESPN le dà in un altro ordine).
        val nba = parseStandings(resource("basketball_nba_standings.json"), Sport.BASKETBALL)
        assertEquals(listOf("Eastern Conference", "Western Conference"), nba.map { it.name })
        assertEquals("Celtics", nba[0].rows.first().team)
        assertEquals(listOf("4", "0", "1.000", "-"), nba[0].rows.first().values)
        assertEquals("Grizzlies", nba[1].rows.first().team)
        // Tennis: la classifica dei giocatori, coi punti.
        val atp = parseTennisRankings(resource("tennis_atp_rankings.json"))
        assertEquals("Jannik Sinner", atp.single().rows.first().team)
        assertEquals(listOf("11000"), atp.single().rows.first().values)
        assertEquals(emptyList<StandingGroup>(), parseStandings("non è json", Sport.SOCCER))
    }

    @Test
    fun theMatchDaysToAskFor() {
        val days = listOf("2026-10-10", "2026-10-11", "2026-10-17", "2026-10-18", "2026-10-24", "2026-10-25").map(LocalDate::parse)
        // Oggi è un giorno di partita: l'ultimo prima, oggi, i due dopo.
        assertEquals(
            listOf("2026-10-11", "2026-10-17", "2026-10-18", "2026-10-24").map(LocalDate::parse),
            matchDaysAround(days, LocalDate.parse("2026-10-17"))
        )
        // Oggi no: l'ultimo prima e i due dopo.
        assertEquals(
            listOf("2026-10-11", "2026-10-17", "2026-10-18").map(LocalDate::parse),
            matchDaysAround(days, LocalDate.parse("2026-10-12"))
        )
    }

    private fun match(id: String, startHours: Long, state: MatchState) = SportMatch(
        id = id,
        start = startHours * 60 * MINUTE_MS,
        state = state,
        home = MatchSide("A"),
        away = MatchSide("B")
    )

    @Test
    fun whatTheWidgetShows() {
        val now = 100 * 60 * MINUTE_MS
        val matches = listOf(
            match("vecchia", 70, MatchState.POST),
            match("ieri", 80, MatchState.POST),
            match("stamattina", 95, MatchState.POST),
            match("adesso", 99, MatchState.IN),
            match("stasera", 104, MatchState.PRE),
            match("domani", 124, MatchState.PRE),
            match("dopodomani", 148, MatchState.PRE)
        )
        // Le ultime due finite, quella in corso, le prossime: cinque, in ordine di tempo.
        assertEquals(listOf("ieri", "stamattina", "adesso", "stasera", "domani"), matchesForWidget(matches, now).map { it.id })
        // Stagione finita: solo risultati, i più recenti.
        val over = matches.filter { it.state == MatchState.POST }
        assertEquals(listOf("vecchia", "ieri", "stamattina"), matchesForWidget(over, now).map { it.id })
        // Con una partita in corso si riscarica ogni minuto, altrimenti ogni cinque.
        val live = SportReport(fetchedAt = now, matches = matches)
        assertTrue(live.isStale(now + MINUTE_MS))
        val quiet = SportReport(fetchedAt = now, matches = matches.filter { it.state != MatchState.IN })
        assertFalse(quiet.isStale(now + MINUTE_MS))
        assertTrue(quiet.isStale(now + 5 * MINUTE_MS))
    }
}
