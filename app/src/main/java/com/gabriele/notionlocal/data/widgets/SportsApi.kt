package com.gabriele.notionlocal.data.widgets

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * **Le domande a ESPN**, per il widget dello sport: le giornate della
 * stagione, le partite di una giornata e la classifica — per il tennis
 * quella ATP o WTA dei giocatori. Le domande che si possono fare insieme
 * partono insieme: sul telefono il DNS a volte impiega 16 secondi a trovare
 * ESPN, e una dopo l'altra facevano aspettare minuti.
 */
object SportsApi {

    private const val BASE = "https://site.api.espn.com/apis"
    private val DAY = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val MONTH = DateTimeFormatter.ofPattern("yyyyMM")

    /** Le giornate della stagione (vedi `buildRounds`); null senza rete. */
    suspend fun rounds(competition: SportCompetition): List<SportRound>? = withContext(Dispatchers.IO) {
        val sport = Sport.of(competition.sport)
        val board = WidgetHttp.get(scoreboardUrl(competition))?.let { parseScoreboard(it, sport) } ?: return@withContext null
        // Le coppe di calcio hanno solo le fasi: i giorni di partita si
        // cercano mese per mese, tutti insieme.
        val extraDays = if (sport == Sport.SOCCER && board.matchDays.isEmpty() && board.stages.isNotEmpty()) {
            monthsOf(board.stages).map { month ->
                async {
                    WidgetHttp.get(scoreboardUrl(competition) + "?dates=${MONTH.format(month)}&limit=1000")
                        ?.let { parseScoreboard(it, sport)?.matches }
                }
            }.awaitAll().filterNotNull().flatten().map { espnDay(it.start) }
        } else {
            emptyList()
        }
        buildRounds(board, sport, extraDays)
    }

    /** I mesi della stagione, dalla prima fase all'ultima (al massimo un anno e mezzo). */
    private fun monthsOf(stages: List<CalendarStage>): List<YearMonth> {
        val first = YearMonth.from(stages.minOf { it.from })
        val last = YearMonth.from(stages.maxOf { it.to })
        return generateSequence(first) { it.plusMonths(1) }.takeWhile { it <= last }.take(18).toList()
    }

    /** Le partite di una giornata, in ordine di tempo; null senza rete. */
    suspend fun roundMatches(competition: SportCompetition, round: SportRound): List<SportMatch>? = withContext(Dispatchers.IO) {
        val sport = Sport.of(competition.sport)
        val queries = if (round.week != null) {
            listOf("?seasontype=${round.seasonType ?: 2}&week=${round.week}")
        } else {
            round.days.map { "?dates=" + DAY.format(LocalDate.parse(it)) }
        }
        val answers = queries.map { query ->
            async { WidgetHttp.get(scoreboardUrl(competition) + query)?.let { parseScoreboard(it, sport)?.matches } }
        }.awaitAll()
        if (answers.isNotEmpty() && answers.all { it == null }) return@withContext null
        var matches = answers.filterNotNull().flatten().distinctBy { it.id }.sortedBy { it.start }
        if (sport == Sport.TENNIS) {
            // Per un giorno ESPN dà i tornei interi (più di cento incontri): solo quelli del giorno.
            matches = onDays(matches, round.days.map(LocalDate::parse).toSet())
        }
        matches
    }

    /** La classifica (nel tennis il ranking); null senza rete. */
    suspend fun standings(competition: SportCompetition): List<StandingGroup>? = withContext(Dispatchers.IO) {
        val sport = Sport.of(competition.sport)
        if (sport == Sport.TENNIS) {
            WidgetHttp.get("$BASE/site/v2/sports/tennis/${competition.league}/rankings")?.let { parseTennisRankings(it) }
        } else {
            WidgetHttp.get("$BASE/v2/sports/${sport.path}/${competition.league}/standings")?.let { parseStandings(it, sport) }
        }
    }

    private fun scoreboardUrl(competition: SportCompetition): String =
        "$BASE/site/v2/sports/${Sport.of(competition.sport).path}/${competition.league}/scoreboard"
}

/**
 * **Quando si riscarica**: solo con la barra aperta, e solo la competizione
 * che si vede. Le giornate due volte al giorno, la giornata di adesso ogni
 * cinque minuti (ogni minuto con una partita in corso), la classifica ogni
 * mezz'ora; o tutto subito con "Refresh". Le altre giornate, quando ci si
 * arriva scorrendo, restano in memoria finché l'app è aperta.
 */
object SportsUpdates {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val inFlight = mutableSetOf<String>()

    private val failedState = mutableStateOf<Set<String>>(emptySet())

    /** Le competizioni (per id) e le giornate (`roundKey`) la cui ultima domanda non ha avuto risposta. */
    val failed: Set<String> get() = failedState.value

    /** Una giornata scaricata scorrendo. */
    data class CachedRound(val fetchedAt: Long, val matches: List<SportMatch>)

    private val roundCache = mutableStateMapOf<String, CachedRound>()

    /** La competizione che si vede in ogni widget, per id del widget. */
    val shown = mutableStateMapOf<String, String>()

    fun roundKey(competitionId: String, round: SportRound): String = "$competitionId/${round.key}"

    fun cachedRound(competitionId: String, round: SportRound): CachedRound? = roundCache[roundKey(competitionId, round)]

    /** La competizione che si vede: le giornate, la giornata di adesso e la classifica, se sono vecchie. */
    fun refresh(widget: SportsWidget, competition: SportCompetition, zone: ZoneId, force: Boolean = false, now: Long = System.currentTimeMillis()) {
        if (competition.id in inFlight) return
        val previous = widget.reports[competition.id]
        @Suppress("NAME_SHADOWING")
        val force = force || (previous != null && previous.version < SPORT_REPORT_VERSION)
        val roundsStale = previous == null || previous.rounds.isEmpty() ||
            previous.roundsFetchedAt?.let { now - it >= ROUNDS_REFRESH_MS } != false
        val standingsStale = previous?.standingsFetchedAt?.let { now - it >= STANDINGS_REFRESH_MS } != false
        val roundMoved = previous != null && previous.round != currentRound(previous.rounds, todayIn(zone, now))
        if (!force && !roundsStale && !standingsStale && !roundMoved && !previous.isStale(now)) return
        inFlight += competition.id
        scope.launch {
            val (report, answered) = update(competition, previous, zone, now, force || roundsStale, force || standingsStale)
            if (report != null) {
                WidgetStore.setSportReport(widget.id, competition.id, report)
            }
            failedState.value = if (answered) failedState.value - competition.id else failedState.value + competition.id
            inFlight -= competition.id
        }
    }

    /** Tutto subito, per "Refresh": la competizione che si vede e le giornate guardate scorrendo. */
    fun refreshShown(widget: SportsWidget, zone: ZoneId) {
        val competition = widget.competitions.find { it.id == shown[widget.id] } ?: widget.competitions.firstOrNull() ?: return
        roundCache.keys.filter { it.startsWith(competition.id + "/") }.forEach { roundCache.remove(it) }
        refresh(widget, competition, zone, force = true)
    }

    /** Il nuovo stato della competizione (null se non è arrivato niente), e se ESPN ha risposto. */
    private suspend fun update(
        competition: SportCompetition,
        previous: SportReport?,
        zone: ZoneId,
        now: Long,
        fetchRounds: Boolean,
        fetchStandings: Boolean
    ): Pair<SportReport?, Boolean> = coroutineScope {
        val newStandings = if (fetchStandings) async { SportsApi.standings(competition) } else null
        val newRounds = if (fetchRounds) SportsApi.rounds(competition) else null
        val rounds = newRounds ?: previous?.rounds.orEmpty()
        val current = currentRound(rounds, todayIn(zone, now))
        val fresh = rounds.getOrNull(current)?.let { SportsApi.roundMatches(competition, it) }
        val standings = newStandings?.await()
        // Ha risposto: le partite della giornata, o (fuori stagione) un calendario vuoto.
        val answered = fresh != null || (newRounds != null && rounds.isEmpty())
        if (!answered && newRounds == null && previous == null) return@coroutineScope null to false
        val report = SportReport(
            // Senza le partite nuove resta la giornata di prima, con le sue.
            fetchedAt = if (fresh != null) now else previous?.fetchedAt ?: 0L,
            matches = fresh ?: previous?.matches.orEmpty(),
            standings = standings ?: previous?.standings.orEmpty(),
            standingsFetchedAt = if (standings != null) now else previous?.standingsFetchedAt,
            rounds = rounds,
            roundsFetchedAt = if (newRounds != null) now else previous?.roundsFetchedAt,
            round = if (fresh != null || previous == null) current else previous.round,
            version = SPORT_REPORT_VERSION
        )
        report to answered
    }

    /**
     * **Una giornata guardata scorrendo** (non quella di adesso, che sta nel
     * widget): si scarica se non c'è o è vecchia. Una giornata passata con
     * tutte le partite finite non cambia più.
     */
    fun loadRound(competition: SportCompetition, round: SportRound, today: LocalDate, now: Long = System.currentTimeMillis()) {
        val key = roundKey(competition.id, round)
        if (key in inFlight) return
        val cached = roundCache[key]
        if (cached != null) {
            val settled = round.lastDay < today && cached.matches.all { it.state == MatchState.POST || it.postponed }
            if (settled || !matchesStale(cached.matches, cached.fetchedAt, now)) return
        }
        inFlight += key
        scope.launch {
            val matches = SportsApi.roundMatches(competition, round)
            if (matches != null) {
                roundCache[key] = CachedRound(now, matches)
                failedState.value = failedState.value - key
            } else {
                failedState.value = failedState.value + key
            }
            inFlight -= key
        }
    }
}
