package com.gabriele.notionlocal.data.widgets

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * **Le domande a ESPN**, per il widget dello sport: le partite (e i giorni di
 * partita vicini a oggi, dove il calendario va a giorni) e la classifica — per
 * il tennis quella ATP o WTA dei giocatori.
 */
object SportsApi {

    private const val BASE = "https://site.api.espn.com/apis"
    private val DAY = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Lo stato di adesso di una competizione; null senza rete. La classifica
     * di prima resta se è recente.
     *
     * Le domande partono **tutte insieme** (dal 17/10/2026): sul telefono il
     * DNS a volte impiega 16 secondi a trovare ESPN, e sei domande una dopo
     * l'altra facevano aspettare due o tre minuti. La prima volta
     * [onMatches] riceve subito le partite della prima risposta, da mostrare
     * mentre arrivano gli altri giorni e la classifica.
     */
    suspend fun report(
        competition: SportCompetition,
        previous: SportReport?,
        zone: ZoneId,
        now: Long = System.currentTimeMillis(),
        onMatches: suspend (SportReport) -> Unit = {}
    ): SportReport? = withContext(Dispatchers.IO) {
        val sport = Sport.of(competition.sport)
        val today = todayIn(zone, now)
        val standingsFresh = previous != null && previous.standings.isNotEmpty() &&
            previous.standingsFetchedAt?.let { now - it < STANDINGS_REFRESH_MS } == true
        val newStandings = if (standingsFresh) null else async {
            if (sport == Sport.TENNIS) {
                WidgetHttp.get("$BASE/site/v2/sports/tennis/${competition.league}/rankings")?.let { parseTennisRankings(it) }
            } else {
                WidgetHttp.get("$BASE/v2/sports/${sport.path}/${competition.league}/standings")?.let { parseStandings(it, sport) }
            }
        }
        val board = WidgetHttp.get(scoreboardUrl(competition, null))?.let { parseScoreboard(it, sport) }
            ?: return@withContext null

        fun near(all: List<SportMatch>): List<SportMatch> {
            val matches = all.distinctBy { it.id }.sortedBy { it.start }
            if (sport != Sport.TENNIS) return matches
            // Un torneo intero sono più di cento incontri: bastano quelli vicini a oggi.
            val from = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = today.plusDays(3).atStartOfDay(zone).toInstant().toEpochMilli()
            return matches.filter { it.state == MatchState.IN || it.start in from until to }
        }

        if (previous == null) {
            onMatches(SportReport(fetchedAt = now, matches = near(board.matches).take(300), standings = emptyList(), standingsFetchedAt = null))
        }
        val extraDays = if (sport == Sport.TENNIS) emptyList() else matchDaysAround(board.matchDays, today)
        val more = extraDays.map { day ->
            async { WidgetHttp.get(scoreboardUrl(competition, day))?.let { parseScoreboard(it, sport)?.matches }.orEmpty() }
        }.awaitAll().flatten()
        val fetched = newStandings?.await()
        val (standings, standingsAt) = if (fetched != null) {
            fetched to now
        } else {
            previous?.standings.orEmpty() to previous?.standingsFetchedAt
        }
        SportReport(fetchedAt = now, matches = near(board.matches + more).take(300), standings = standings, standingsFetchedAt = standingsAt)
    }

    private fun scoreboardUrl(competition: SportCompetition, day: LocalDate?): String =
        "$BASE/site/v2/sports/${Sport.of(competition.sport).path}/${competition.league}/scoreboard" +
            (day?.let { "?dates=${DAY.format(it)}" } ?: "")
}

/**
 * **Quando si riscaricano le partite**: solo con la barra aperta, ogni cinque
 * minuti, ogni minuto se una partita è in corso; o subito con "Refresh". Come
 * `WeatherUpdates`.
 */
object SportsUpdates {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val inFlight = mutableSetOf<String>()

    private val failedState = mutableStateOf<Set<String>>(emptySet())
    val failed: Set<String> get() = failedState.value

    fun refresh(widget: SportsWidget, zone: ZoneId, force: Boolean = false, now: Long = System.currentTimeMillis()) {
        widget.competitions.forEach { competition ->
            if (competition.id in inFlight) return@forEach
            val previous = widget.reports[competition.id]
            if (!force && !previous.isStale(now)) return@forEach
            inFlight += competition.id
            scope.launch {
                val report = SportsApi.report(competition, previous, zone) { first ->
                    withContext(Dispatchers.Main) { WidgetStore.setSportReport(widget.id, competition.id, first) }
                }
                if (report != null) {
                    WidgetStore.setSportReport(widget.id, competition.id, report)
                    failedState.value = failedState.value - competition.id
                } else {
                    failedState.value = failedState.value + competition.id
                }
                inFlight -= competition.id
            }
        }
    }
}
