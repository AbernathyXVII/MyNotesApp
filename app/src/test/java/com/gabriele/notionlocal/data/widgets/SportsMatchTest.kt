package com.gabriele.notionlocal.data.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Una partita da vicino** (17/10/2026): il "summary" vero di ESPN di
 * Frosinone-Sassuolo 4-2 (quello della foto dell'utente da Sofascore), di
 * una partita NBA e di una NFL, alleggeriti, in `resources/sports`.
 */
class SportsMatchTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("sports/$name")!!.bufferedReader().readText()

    private val frosinone by lazy { parseMatchSummary(resource("summary_frosinone_sassuolo.json"), Sport.SOCCER)!! }

    @Test
    fun theGoalsWithAssistsAndTheScore() {
        val goals = frosinone.events.filter { it.kind == MatchEventKind.GOAL }
        assertEquals(listOf("9'", "15'", "26'", "30'", "37'", "57'"), goals.map { it.minute })
        assertEquals(listOf("G. Calo", "D. Berardi", "A. Raimondo", "A. Raimondo", "K. Thorstvedt", "A. Raimondo"), goals.map { it.player })
        assertEquals(listOf(Side.HOME, Side.AWAY, Side.HOME, Side.HOME, Side.AWAY, Side.HOME), goals.map { it.side })
        // L'assist di Bakola sul gol di Berardi.
        assertEquals("D. Bakola", goals[1].other)
        assertEquals("", goals[0].other)
        // Il punteggio dopo ogni gol.
        assertEquals(listOf("1-0", "1-1", "2-1", "3-1", "3-2", "4-2"), goals.map { "${it.homeScore}-${it.awayScore}" })
    }

    @Test
    fun halvesAddedTimeAndVar() {
        val timeline = frosinone.events.filter { it.kind != MatchEventKind.SUBSTITUTION }
            .map { "${it.kind} ${it.minute}" }
        assertEquals(
            listOf(
                "GOAL 9'", "GOAL 15'", "VAR_NO_GOAL 17'", "GOAL 26'", "GOAL 30'", "GOAL 37'",
                "ADDED_TIME 45'", "HALF_TIME 45'+1'",
                "GOAL 57'", "YELLOW_CARD 82'",
                "ADDED_TIME 90'+3'", "FULL_TIME 90'+5'"
            ),
            timeline
        )
        val events = frosinone.events
        // Il recupero annunciato: 1 minuto nel primo tempo, 4 nel secondo.
        assertEquals(listOf(1, 4), events.filter { it.kind == MatchEventKind.ADDED_TIME }.map { it.added })
        // Il punteggio a fine tempo.
        assertEquals("3-2", events.single { it.kind == MatchEventKind.HALF_TIME }.let { "${it.homeScore}-${it.awayScore}" })
        assertEquals("4-2", events.single { it.kind == MatchEventKind.FULL_TIME }.let { "${it.homeScore}-${it.awayScore}" })
        // Il gol annullato dal VAR, del Frosinone.
        val var_ = events.single { it.kind == MatchEventKind.VAR_NO_GOAL }
        assertEquals("G. Kvernadze", var_.player)
        assertEquals(Side.HOME, var_.side)
    }

    @Test
    fun substitutionsAndCards() {
        val subs = frosinone.events.filter { it.kind == MatchEventKind.SUBSTITUTION }
        assertEquals(10, subs.size)
        // Chi entra e chi esce.
        val first = subs.first()
        assertEquals("58'", first.minute)
        assertEquals("K. Bowie", first.player)
        assertEquals("K. Thorstvedt", first.other)
        assertEquals(Side.AWAY, first.side)
        val last = subs.last()
        assertEquals("90'", last.minute)
        assertEquals("D. Birligea", last.player)
        assertEquals("A. Raimondo", last.other)
        // L'ammonizione col motivo.
        val card = frosinone.events.single { it.kind == MatchEventKind.YELLOW_CARD }
        assertEquals("K. Bowie", card.player)
        assertEquals(CardReason.FOUL, card.reason)
        assertEquals(Side.AWAY, card.side)
    }

    @Test
    fun theScorersUnderTheScoreLikeSofascore() {
        // Frosinone-Sassuolo 4-2: coi nomi interi e i minuti, uno per giocatore.
        val (home, away) = scorersOf(frosinone.events)
        assertEquals(
            listOf("Giacomo Calo" to listOf("9'"), "Antonio Raimondo" to listOf("26'", "30'", "57'")),
            home.map { it.name to it.minutes }
        )
        assertEquals(
            listOf("Domenico Berardi" to listOf("15'"), "Kristian Thorstvedt" to listOf("37'")),
            away.map { it.name to it.minutes }
        )
        // Il gol annullato dal VAR non c'è.
        assertTrue(home.none { it.name.contains("Kvernadze") })
        // L'autogol va alla squadra che ne approfitta, segnato.
        val ownGoal = MatchEvent(MatchEventKind.OWN_GOAL, "25'", Side.AWAY, "M. Dituro", playerFull = "Matías Dituro", homeScore = 0, awayScore = 1)
        val (_, withOwnGoal) = scorersOf(listOf(ownGoal))
        assertEquals(listOf(MatchEventKind.OWN_GOAL), withOwnGoal.single().marks)
    }

    @Test
    fun theStadiumComesWithTheMatch() {
        val json = """{"events":[{"id":"1","date":"2026-10-17T16:00Z","competitions":[{
            "venue":{"fullName":"Stadio Renato Dall'Ara","address":{"city":"Bologna"}},
            "status":{"type":{"state":"post","shortDetail":"FT"}},
            "competitors":[{"homeAway":"home","score":"1","team":{"shortDisplayName":"Bologna"}},
                           {"homeAway":"away","score":"3","team":{"shortDisplayName":"Inter Milan"}}]}]}]}"""
        val match = parseScoreboard(json, Sport.SOCCER)!!.matches.single()
        assertEquals("Stadio Renato Dall'Ara", match.venue)
        // Senza stadio: vuoto, e la riga non lo mostra.
        assertEquals("", parseScoreboard(json.replace(""""venue":{"fullName":"Stadio Renato Dall'Ara","address":{"city":"Bologna"}},""", ""), Sport.SOCCER)!!.matches.single().venue)
    }

    @Test
    fun cardReasonsAsEspnWritesThem() {
        assertEquals(CardReason.FOUL to "Bad foul", cardReasonOf("Andrea Carboni (Monza) is shown the yellow card for a bad foul."))
        assertEquals(CardReason.HANDBALL, cardReasonOf("X (Y) is shown the yellow card for handball.").first)
        assertEquals(CardReason.VIOLENT_CONDUCT, cardReasonOf("X (Y) is shown the red card for violent conduct.").first)
        assertEquals(CardReason.FOUL, cardReasonOf("Second yellow card to Mario Martín (Getafe) for a bad foul.").first)
        // Senza motivo.
        assertNull(cardReasonOf("Lorenzo Lucchesi (Monza) is shown the red card.").first)
        // Uno che non si traduce resta com'è.
        assertEquals(null to "Leaving the field", cardReasonOf("X (Y) is shown the yellow card for leaving the field."))
        assertEquals("D. Berardi", shortPlayerName("Domenico Berardi"))
        assertEquals("Pedrinho", shortPlayerName("Pedrinho"))
        assertEquals("K. De Bruyne", shortPlayerName("Kevin De Bruyne"))
    }

    @Test
    fun soccerStatisticsLikeSofascore() {
        val stats = frosinone.stats.associateBy { it.key }
        assertEquals("42%" to "58%", stats.getValue("possessionPct").let { it.home to it.away })
        // I passaggi riusciti con la percentuale.
        assertEquals("246 (74%)" to "377 (81%)", stats.getValue("accuratePasses").let { it.home to it.away })
        assertEquals(MATCH_STAT_KEYS.getValue(Sport.SOCCER), frosinone.stats.map { it.key })
        assertEquals(0.419f, stats.getValue("possessionPct").homeShare!!, 0.01f)
    }

    @Test
    fun nbaAndNflByPeriod() {
        val nba = parseMatchSummary(resource("summary_nba.json"), Sport.BASKETBALL)!!
        assertEquals(listOf("1", "2", "3", "4"), nba.periods)
        assertEquals(listOf("34", "36", "38", "28"), nba.homeLines)
        assertEquals(listOf("36", "29", "30", "20"), nba.awayLines)
        assertEquals(MATCH_STAT_KEYS.getValue(Sport.BASKETBALL), nba.stats.map { it.key })
        // Fuori casa i Miami Heat: i loro tiri a destra.
        assertEquals("44-103", nba.stats.single { it.key == "fieldGoalsMade-fieldGoalsAttempted" }.away)

        val nfl = parseMatchSummary(resource("summary_nfl.json"), Sport.FOOTBALL)!!
        // Quattro quarti e il supplementare.
        assertEquals(listOf("1", "2", "3", "4", "OT"), nfl.periods)
        assertEquals(6, nfl.scoringPlays.size)
        val first = nfl.scoringPlays.first()
        assertEquals("Field Goal Good", first.type)
        assertEquals(Side.AWAY, first.side)
        assertEquals(0 to 3, first.homeScore to first.awayScore)
        assertEquals("353", nfl.stats.single { it.key == "totalYards" }.away)
        assertNull(parseMatchSummary("non è json", Sport.SOCCER))
    }
}
