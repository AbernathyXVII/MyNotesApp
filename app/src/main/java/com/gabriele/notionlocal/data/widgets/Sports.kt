package com.gabriele.notionlocal.data.widgets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * **Lo sport** (dal 17/10/2026, chiesto dall'utente): una o più competizioni
 * di sport diversi — calcio, tennis, basket, baseball, football americano —
 * una per pagina, che si sfogliano come le città del meteo. Ogni pagina
 * mostra le partite più vicine ad adesso (i risultati appena finiti, quelle
 * in corso, le prossime); un tocco apre tutte le partite e la classifica;
 * tenendo premuto si scelgono le competizioni.
 *
 * I dati vengono da **ESPN** (site.api.espn.com): gratuito e senza chiave,
 * ma non è un servizio ufficiale per le app, e potrebbe cambiare. L'ultimo
 * stato scaricato resta salvato (`reports`) e si mostra anche senza rete.
 */
@Serializable
@SerialName("sports")
data class SportsWidget(
    override val id: String = newWidgetId(),
    /** Le competizioni, la prima è quella che si vede aprendo la barra. */
    val competitions: List<SportCompetition> = emptyList(),
    /** L'ultimo stato scaricato di ogni competizione, per id. */
    val reports: Map<String, SportReport> = emptyMap()
) : Widget()

const val MAX_SPORT_COMPETITIONS = 10

/** Ogni quanto si riscaricano le partite con la barra aperta; con una partita in corso, ogni minuto. */
const val SPORTS_REFRESH_MS = 5 * MINUTE_MS
const val SPORTS_LIVE_REFRESH_MS = MINUTE_MS

/** La classifica cambia meno: si riscarica al massimo ogni mezz'ora. */
const val STANDINGS_REFRESH_MS = 30 * MINUTE_MS

enum class Sport(val path: String, val emoji: String) {
    SOCCER("soccer", "⚽"),
    TENNIS("tennis", "🎾"),
    BASKETBALL("basketball", "🏀"),
    BASEBALL("baseball", "⚾"),
    FOOTBALL("football", "🏈");

    companion object {
        fun of(path: String): Sport = entries.find { it.path == path } ?: SOCCER
    }
}

@Serializable
data class SportCompetition(
    val id: String = newWidgetId(),
    /** `Sport.path`: "soccer", "tennis"… */
    val sport: String,
    /** La competizione per ESPN: "ita.1", "uefa.champions", "nba"… */
    val league: String,
    val name: String
)

/** Una competizione che si può scegliere. */
data class CatalogEntry(val sport: Sport, val league: String, val name: String) {
    fun toCompetition(): SportCompetition = SportCompetition(sport = sport.path, league = league, name = name)
}

/** Le competizioni fra cui scegliere, per sport (tutte provate su ESPN il 17/10/2026). */
val SPORT_CATALOG: List<CatalogEntry> = listOf(
    CatalogEntry(Sport.SOCCER, "ita.1", "Serie A"),
    CatalogEntry(Sport.SOCCER, "ita.2", "Serie B"),
    CatalogEntry(Sport.SOCCER, "ita.coppa_italia", "Coppa Italia"),
    CatalogEntry(Sport.SOCCER, "uefa.champions", "Champions League"),
    CatalogEntry(Sport.SOCCER, "uefa.europa", "Europa League"),
    CatalogEntry(Sport.SOCCER, "uefa.europa.conf", "Conference League"),
    CatalogEntry(Sport.SOCCER, "eng.1", "Premier League"),
    CatalogEntry(Sport.SOCCER, "esp.1", "LaLiga"),
    CatalogEntry(Sport.SOCCER, "ger.1", "Bundesliga"),
    CatalogEntry(Sport.SOCCER, "fra.1", "Ligue 1"),
    CatalogEntry(Sport.SOCCER, "uefa.nations", "Nations League"),
    CatalogEntry(Sport.SOCCER, "fifa.world", "FIFA World Cup"),
    CatalogEntry(Sport.SOCCER, "fifa.worldq.uefa", "World Cup Qualifying (UEFA)"),
    CatalogEntry(Sport.TENNIS, "atp", "ATP"),
    CatalogEntry(Sport.TENNIS, "wta", "WTA"),
    CatalogEntry(Sport.BASKETBALL, "nba", "NBA"),
    CatalogEntry(Sport.BASKETBALL, "wnba", "WNBA"),
    CatalogEntry(Sport.BASKETBALL, "mens-college-basketball", "NCAA Basketball"),
    CatalogEntry(Sport.BASEBALL, "mlb", "MLB"),
    CatalogEntry(Sport.FOOTBALL, "nfl", "NFL"),
    CatalogEntry(Sport.FOOTBALL, "college-football", "NCAA Football")
)

@Serializable
data class SportReport(
    val fetchedAt: Long,
    val matches: List<SportMatch>,
    val standings: List<StandingGroup> = emptyList(),
    val standingsFetchedAt: Long? = null
)

enum class MatchState { PRE, IN, POST }

@Serializable
data class SportMatch(
    val id: String,
    /** Quando comincia, in millisecondi. */
    val start: Long,
    val state: MatchState,
    /** Quello che dice ESPN: "45'", "Q3 5:21", "Final", "Postponed"… */
    val status: String = "",
    val home: MatchSide,
    val away: MatchSide,
    /** Tennis: il torneo e il turno ("Shanghai Masters · Final"). */
    val note: String = ""
) {
    val postponed: Boolean get() = status.contains("postpon", ignoreCase = true) || status.contains("cancel", ignoreCase = true)
}

@Serializable
data class MatchSide(
    val name: String,
    /** Il punteggio; nel tennis i set, uno per numero. */
    val score: String? = null,
    val sets: List<Int> = emptyList(),
    val winner: Boolean = false
)

/** Un gruppo della classifica: il campionato intero, o una conferenza, un girone. */
@Serializable
data class StandingGroup(
    val name: String,
    /** Le intestazioni delle colonne ("GP", "W", "D"…). */
    val columns: List<String>,
    val rows: List<StandingRow>
)

@Serializable
data class StandingRow(val rank: Int, val team: String, val values: List<String>)

/** Va riscaricata: non c'è, o è vecchia (più in fretta se c'è una partita in corso). */
fun SportReport?.isStale(now: Long): Boolean {
    if (this == null) return true
    val every = if (matches.any { it.state == MatchState.IN }) SPORTS_LIVE_REFRESH_MS else SPORTS_REFRESH_MS
    return now - fetchedAt >= every
}

/**
 * **Le partite del widget**, al massimo [limit]: le ultime due finite, quelle
 * in corso e le prossime, in ordine di tempo. Le rinviate stanno con le
 * prossime solo se il loro giorno non è passato.
 */
fun matchesForWidget(matches: List<SportMatch>, now: Long, limit: Int = 5): List<SportMatch> {
    val live = matches.filter { it.state == MatchState.IN }.sortedBy { it.start }.take(limit)
    val room = limit - live.size
    val finished = matches.filter { it.state == MatchState.POST && !it.postponed }.sortedByDescending { it.start }
    val upcoming = matches.filter { it.state == MatchState.PRE && it.start >= now - 6 * 60 * MINUTE_MS }.sortedBy { it.start }
    // Senza partite in arrivo (stagione finita) tutto il posto va ai risultati.
    val recent = finished.take(if (upcoming.isEmpty()) room else minOf(2, room))
    val next = upcoming.take(room - recent.size)
    return (recent + live + next).sortedBy { it.start }
}

// --- Le risposte di ESPN, lette ---

private val lenientJson = Json { ignoreUnknownKeys = true }

/** Le partite di una risposta "scoreboard", e i giorni di partita della stagione (se li dà). */
data class Scoreboard(val matches: List<SportMatch>, val matchDays: List<LocalDate>)

fun parseScoreboard(json: String, sport: Sport): Scoreboard? = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    val events = root.array("events")
    val matches = if (sport == Sport.TENNIS) {
        events.flatMap { tennisMatches(it as? JsonObject ?: return@flatMap emptyList()) }
    } else {
        events.mapNotNull { teamMatch(it as? JsonObject ?: return@mapNotNull null) }
    }
    val league = root.array("leagues").firstOrNull() as? JsonObject
    // Il calendario "a giorni" è una lista di date; quello "a settimane"
    // (NFL, coppe) è fatto di oggetti, e lì basta la risposta di partenza.
    val days = league?.array("calendar").orEmpty().mapNotNull { day ->
        day.string()?.let { parseDate(it)?.atOffset(java.time.ZoneOffset.UTC)?.toLocalDate() }
    }
    Scoreboard(matches, days)
}.getOrNull()

private fun teamMatch(event: JsonObject): SportMatch? {
    val competition = event.array("competitions").firstOrNull() as? JsonObject ?: return null
    val sides = competition.array("competitors").mapNotNull { it as? JsonObject }
    val home = sides.find { it.string("homeAway") == "home" } ?: sides.getOrNull(0) ?: return null
    val away = sides.find { it.string("homeAway") == "away" } ?: sides.getOrNull(1) ?: return null
    val state = stateOf(event.obj("status") ?: competition.obj("status"))
    return SportMatch(
        id = event.string("id") ?: return null,
        start = parseDate(event.string("date") ?: competition.string("date") ?: return null)?.toEpochMilli() ?: return null,
        state = state,
        status = statusText(event.obj("status") ?: competition.obj("status")),
        home = teamSide(home, state),
        away = teamSide(away, state)
    )
}

private fun teamSide(side: JsonObject, state: MatchState): MatchSide {
    val team = side.obj("team") ?: side.obj("athlete")
    val name = team?.string("shortDisplayName") ?: team?.string("displayName") ?: team?.string("name") ?: "?"
    return MatchSide(
        name = name,
        // Prima della partita ESPN scrive "0": meglio niente.
        score = side.string("score")?.takeIf { state != MatchState.PRE },
        winner = side.boolean("winner")
    )
}

/** Gli incontri di singolare di un torneo di tennis, col nome del torneo e il turno. */
private fun tennisMatches(event: JsonObject): List<SportMatch> {
    val tournament = event.string("shortName") ?: event.string("name") ?: ""
    return event.array("groupings").mapNotNull { it as? JsonObject }
        .filter { grouping -> grouping.obj("grouping")?.string("displayName")?.contains("Singles") == true }
        .flatMap { grouping ->
            grouping.array("competitions").mapNotNull { element ->
                val match = element as? JsonObject ?: return@mapNotNull null
                val players = match.array("competitors").mapNotNull { it as? JsonObject }.sortedBy { it.int("order") ?: 0 }
                if (players.size < 2) return@mapNotNull null
                val state = stateOf(match.obj("status"))
                val round = match.obj("round")?.string("displayName").orEmpty()
                SportMatch(
                    id = match.string("id") ?: return@mapNotNull null,
                    start = parseDate(match.string("date") ?: return@mapNotNull null)?.toEpochMilli() ?: return@mapNotNull null,
                    state = state,
                    status = statusText(match.obj("status")),
                    home = playerSide(players[0]),
                    away = playerSide(players[1]),
                    note = listOf(tournament, round).filter { it.isNotBlank() }.joinToString(" · ")
                )
            }
        }
}

private fun playerSide(side: JsonObject): MatchSide {
    val athlete = side.obj("athlete")
    val sets = side.array("linescores").mapNotNull { (it as? JsonObject)?.double("value")?.let { v -> Math.round(v).toInt() } }
    return MatchSide(
        name = athlete?.string("shortName") ?: athlete?.string("displayName") ?: "TBD",
        sets = sets,
        winner = side.boolean("winner")
    )
}

private fun stateOf(status: JsonObject?): MatchState = when (status?.obj("type")?.string("state")) {
    "in" -> MatchState.IN
    "post" -> MatchState.POST
    else -> MatchState.PRE
}

private fun statusText(status: JsonObject?): String = status?.obj("type")?.string("shortDetail").orEmpty()

/**
 * **La classifica**: un gruppo per conferenza o girone, con le colonne
 * giuste per lo sport, in ordine di posizione (ESPN la dà in un ordine suo
 * per NBA e NFL).
 */
fun parseStandings(json: String, sport: Sport): List<StandingGroup> = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    val columns = when (sport) {
        Sport.SOCCER -> listOf("gamesPlayed", "wins", "ties", "losses", "pointDifferential", "points")
        Sport.BASKETBALL, Sport.BASEBALL -> listOf("wins", "losses", "winPercent", "gamesBehind")
        Sport.FOOTBALL -> listOf("wins", "losses", "ties", "winPercent")
        Sport.TENNIS -> emptyList()
    }
    val groups = mutableListOf<StandingGroup>()
    fun walk(node: JsonObject) {
        node.array("children").mapNotNull { it as? JsonObject }.forEach { child ->
            val entries = child.obj("standings")?.array("entries").orEmpty().mapNotNull { it as? JsonObject }
            if (entries.isNotEmpty()) {
                val stats = entries.map { entry ->
                    entry.array("stats").mapNotNull { it as? JsonObject }.associateBy { it.string("name").orEmpty() }
                }
                val header = columns.map { name ->
                    stats.firstNotNullOfOrNull { it[name]?.string("abbreviation") } ?: name.take(3).uppercase()
                }
                val ranked = entries.indices.map { i ->
                    val s = stats[i]
                    val order = s["rank"]?.double("value") ?: s["playoffSeed"]?.double("value")
                    Triple(i, order, s)
                }.sortedWith(compareBy<Triple<Int, Double?, Map<String, JsonObject>>> { it.second ?: Double.MAX_VALUE }.thenBy { it.first })
                val rows = ranked.mapIndexed { position, (i, _, s) ->
                    val team = entries[i].obj("team")
                    StandingRow(
                        rank = position + 1,
                        team = team?.string("shortDisplayName") ?: team?.string("displayName") ?: "?",
                        values = columns.map { s[it]?.string("displayValue").orEmpty() }
                    )
                }
                groups += StandingGroup(name = child.string("name").orEmpty(), columns = header, rows = rows)
            }
            walk(child)
        }
    }
    walk(root)
    groups
}.getOrDefault(emptyList())

/** **La classifica del tennis**: i primi [limit] giocatori ATP o WTA, coi punti. */
fun parseTennisRankings(json: String, limit: Int = 100): List<StandingGroup> = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    root.array("rankings").mapNotNull { it as? JsonObject }.take(1).map { ranking ->
        StandingGroup(
            name = ranking.string("name").orEmpty(),
            columns = listOf("PTS"),
            rows = ranking.array("ranks").mapNotNull { it as? JsonObject }.take(limit).mapNotNull { entry ->
                val athlete = entry.obj("athlete") ?: return@mapNotNull null
                StandingRow(
                    rank = entry.int("current") ?: return@mapNotNull null,
                    team = athlete.string("displayName") ?: return@mapNotNull null,
                    values = listOf(entry.double("points")?.let { Math.round(it).toString() }.orEmpty())
                )
            }
        )
    }
}.getOrDefault(emptyList())

/**
 * I giorni di partita da chiedere oltre a quello che ESPN dà da sé: l'ultimo
 * giorno di partita prima di oggi (i risultati), oggi, e i prossimi due.
 * Solo per i calendari "a giorni".
 */
fun matchDaysAround(days: List<LocalDate>, today: LocalDate): List<LocalDate> {
    val sorted = days.distinct().sorted()
    val before = sorted.lastOrNull { it < today }
    val after = sorted.filter { it > today }.take(2)
    return listOfNotNull(before, today.takeIf { it in sorted }) + after
}

/** Il giorno di oggi nel fuso dell'app, per scegliere i giorni di partita. */
fun todayIn(zone: ZoneId, now: Long): LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

private fun parseDate(text: String): Instant? =
    runCatching { Instant.parse(text) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(text.replace("Z", ":00Z")).toInstant() }.getOrNull()

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()
private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.let { d -> Math.round(d).toInt() } }
private fun JsonObject.boolean(key: String): Boolean = (this[key] as? JsonPrimitive)?.contentOrNull == "true"
private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.contentOrNull
