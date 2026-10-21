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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * **Lo sport** (dal 17/10/2026, chiesto dall'utente): una o più competizioni
 * di sport diversi — calcio, tennis, basket, baseball, football americano.
 *
 * Dalla v0.107.1 il widget mostra **una competizione alla volta**, che si
 * cambia con le frecce, e **una giornata intera per pagina**: scorrendo si
 * vedono le giornate prima e dopo, tutta la stagione. Un tocco apre le
 * partite della giornata e la classifica; tenendo premuto si scelgono le
 * competizioni.
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

/** Le giornate della stagione cambiano di rado (un rinvio, una data fissata): due volte al giorno. */
const val ROUNDS_REFRESH_MS = 12 * 60 * MINUTE_MS

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

/** Il globo che mostra l'Europa e l'Africa (non c'è un'emoji della sola Europa). */
const val GLOBE_EUROPE_AFRICA = "🌍"
const val GLOBE_AMERICAS = "🌎"
const val GLOBE_ASIA = "🌏"
const val GLOBE_WORLD = "🌐"

/**
 * **La bandiera di una competizione** (dal 17/10/2026, chiesto dall'utente):
 * quella del paese del campionato; per le competizioni continentali il
 * globo col continente (le coppe UEFA, le qualificazioni europee); per
 * quelle mondiali (i Mondiali, ATP e WTA) il globo intero. Vuota se non si
 * sa.
 */
fun competitionFlag(league: String): String {
    val country = league.substringBefore('.')
    val continent = league.substringAfterLast('.')
    return when {
        country == "ita" -> "🇮🇹"
        // L'Inghilterra (non il Regno Unito): la croce di San Giorgio.
        country == "eng" -> "🏴󠁧󠁢󠁥󠁮󠁧󠁿"
        country == "esp" -> "🇪🇸"
        country == "ger" -> "🇩🇪"
        country == "fra" -> "🇫🇷"
        country == "uefa" || continent == "uefa" || country == "caf" || continent == "caf" -> GLOBE_EUROPE_AFRICA
        country == "conmebol" || continent == "conmebol" || country == "concacaf" || continent == "concacaf" -> GLOBE_AMERICAS
        country == "afc" || continent == "afc" || country == "ofc" || continent == "ofc" -> GLOBE_ASIA
        country == "fifa" || league == "atp" || league == "wta" -> GLOBE_WORLD
        league in setOf("nba", "wnba", "mens-college-basketball", "mlb", "nfl", "college-football") ->
            "🇺🇸"
        else -> ""
    }
}

@Serializable
data class SportReport(
    /** Quando sono state scaricate [matches]. */
    val fetchedAt: Long,
    /** Le partite della giornata di adesso, [round]. */
    val matches: List<SportMatch>,
    val standings: List<StandingGroup> = emptyList(),
    val standingsFetchedAt: Long? = null,
    /** Le giornate della stagione, in ordine. */
    val rounds: List<SportRound> = emptyList(),
    val roundsFetchedAt: Long? = null,
    /** La giornata di [matches], in [rounds]; -1 senza giornate. */
    val round: Int = -1,
    /** La stagione in corso, come la dà ESPN. */
    val season: SeasonInfo? = null,
    /** Com'era fatto quando è stato scaricato: se è più vecchio di `SPORT_REPORT_VERSION` si riscarica tutto. */
    val version: Int = 1
)

/**
 * La versione di `SportReport` (2 dalla v0.107.1: giornate, stemmi; 3 dalla
 * v0.110: la stagione). Quelli salvati prima si riscaricano subito,
 * altrimenti la classifica restava senza stemmi fino alla mezz'ora dopo (e
 * la stagione senza anno fino al giorno dopo).
 */
const val SPORT_REPORT_VERSION = 3

/**
 * **Una stagione** (dal 17/10/2026): l'anno con cui la chiede ESPN (per la
 * Serie A 2026-27 è 2026, per la NBA 2026-27 è 2027), il nome che le dà
 * ("2026-27 Italian Serie A", "2026-27", "2026") e il primo e l'ultimo giorno.
 */
@Serializable
data class SeasonInfo(val year: Int, val name: String = "", val start: String = "", val end: String = "")

/** Il primo anno scritto nel nome ("2026-27 Italian Serie A" → 2026), se c'è. */
private fun SeasonInfo.firstYearInName(): Int? = Regex("^(\\d{4})").find(name.trim())?.groupValues?.get(1)?.toIntOrNull()

/** Se la stagione va a cavallo di due anni ("2026-27"). */
private fun SeasonInfo.spansTwoYears(): Boolean = Regex("^\\d{4}-\\d{2,4}").containsMatchIn(name.trim())

/**
 * **Il nome di una stagione per l'utente** (dal 17/10/2026, chiesto
 * dall'utente): "2026-2027" per i campionati a cavallo di due anni, "2026"
 * per gli altri. [year] è l'anno di ESPN di un'altra stagione della stessa
 * competizione.
 */
fun seasonLabel(info: SeasonInfo, year: Int = info.year): String {
    if (!info.spansTwoYears()) return year.toString()
    val first = year + ((info.firstYearInName() ?: info.year) - info.year)
    return "$first-${first + 1}"
}

/** Le stagioni fra cui scegliere: quella in corso e le nove prima. */
fun seasonYears(info: SeasonInfo, count: Int = 10): List<Int> = (info.year downTo info.year - count + 1).toList()

/**
 * **Un giorno dentro un'altra stagione**: la metà di quella in corso, tanti
 * anni prima. Chiedendo a ESPN le partite di quel giorno arriva il calendario
 * di quella stagione.
 */
fun seasonProbeDay(info: SeasonInfo, year: Int): LocalDate? {
    val start = runCatching { LocalDate.parse(info.start.take(10)) }.getOrNull() ?: return null
    val end = runCatching { LocalDate.parse(info.end.take(10)) }.getOrNull() ?: return null
    val middle = start.plusDays(ChronoUnit.DAYS.between(start, end) / 2)
    return middle.minusYears((info.year - year).toLong())
}

/**
 * **Una giornata**: i giorni (quelli di ESPN, cioè americani) in cui si gioca.
 * Nei campionati di calcio i giorni di partita vicini (venerdì-lunedì, o il
 * turno infrasettimanale); nella NFL la settimana; nel basket, nel baseball e
 * nel tennis il giorno.
 */
@Serializable
data class SportRound(
    /** Il primo e l'ultimo giorno, "2026-10-16". */
    val from: String,
    val to: String,
    /** I giorni da chiedere a ESPN uno per uno; vuoto se si chiede la settimana ([week]). */
    val days: List<String> = emptyList(),
    /** Il nome che le dà ESPN: "Week 7", "League Phase"; vuoto nei campionati. */
    val label: String = "",
    /** NFL e college: la parte della stagione (1 precampionato, 2 regolare, 3 playoff) e la settimana. */
    val seasonType: Int? = null,
    val week: Int? = null,
    /** NFL e college: l'anno della stagione, perché la settimana si chiede per stagione. */
    val season: Int? = null
) {
    val key: String get() = "$from/$to"
    val firstDay: LocalDate get() = LocalDate.parse(from)
    val lastDay: LocalDate get() = LocalDate.parse(to)
}

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
    val note: String = "",
    /** Tennis: il torneo, per dividere le partite di una giornata. */
    val tournament: String = "",
    /** Lo stadio (dal 18/10/2026), per la finestra della partita. */
    val venue: String = ""
) {
    val postponed: Boolean get() = status.contains("postpon", ignoreCase = true) || status.contains("cancel", ignoreCase = true)
}

@Serializable
data class MatchSide(
    val name: String,
    /** Il punteggio; nel tennis i set, uno per numero. */
    val score: String? = null,
    val sets: List<Int> = emptyList(),
    val winner: Boolean = false,
    /** Lo stemma della squadra (nel tennis la bandiera del giocatore), un indirizzo di ESPN. */
    val logo: String = ""
)

/**
 * **Il nome di una squadra come lo vuole l'utente** (dal 17/10/2026): "Inter"
 * e "Roma", non "Inter Milan" e "AS Roma" come li scrive ESPN. Solo nel
 * disegno: i dati restano quelli di ESPN.
 */
fun teamDisplayName(name: String): String = SHORT_TEAM_NAMES[name] ?: name

private val SHORT_TEAM_NAMES = mapOf(
    "Inter Milan" to "Inter",
    "AS Roma" to "Roma"
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
data class StandingRow(val rank: Int, val team: String, val values: List<String>, val logo: String = "")

/** Va riscaricata: non c'è, o è vecchia (più in fretta se c'è una partita in corso). */
fun SportReport?.isStale(now: Long): Boolean = this == null || matchesStale(matches, fetchedAt, now)

fun matchesStale(matches: List<SportMatch>, fetchedAt: Long, now: Long): Boolean {
    val every = if (matches.any { it.state == MatchState.IN }) SPORTS_LIVE_REFRESH_MS else SPORTS_REFRESH_MS
    return now - fetchedAt >= every
}

/** Il giorno di oggi nel fuso dell'app, per scegliere la giornata. */
fun todayIn(zone: ZoneId, now: Long): LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

/**
 * Il giorno di ESPN di una partita: quello americano (le partite NBA delle
 * 1:30 italiane sono della sera prima), come nelle sue domande per giorno.
 */
fun espnDay(start: Long): LocalDate = Instant.ofEpochMilli(start).atOffset(ZoneOffset.ofHours(-5)).toLocalDate()

/** Le partite di quei giorni di ESPN: per un giorno, ESPN dà i tornei di tennis interi. */
fun onDays(matches: List<SportMatch>, days: Collection<LocalDate>): List<SportMatch> = matches.filter { espnDay(it.start) in days }

// --- Le giornate ---

/**
 * **Le giornate della stagione** dal calendario di ESPN:
 * - NFL e college a settimane ("Week 7"), come le dà ESPN;
 * - basket, baseball e tennis: un giorno per pagina;
 * - calcio: i giorni di partita vicini (vedi `soccerRounds`), col nome della
 *   fase se ESPN lo dà ("League Phase"). Per le coppe ESPN dà solo le fasi:
 *   i giorni di partita ([extraDays]) si cercano a parte, mese per mese.
 *
 * Senza calendario (succede fuori stagione) restano i giorni delle partite
 * della risposta.
 */
fun buildRounds(board: Scoreboard, sport: Sport, extraDays: List<LocalDate> = emptyList()): List<SportRound> {
    if (sport == Sport.FOOTBALL && board.stages.any { it.week != null }) {
        return board.stages.filter { it.week != null }.map { stage ->
            SportRound(
                from = stage.from.toString(),
                to = stage.to.toString(),
                label = stage.label,
                seasonType = stage.seasonType,
                week = stage.week,
                season = board.season?.year
            )
        }
    }
    val days = (board.matchDays + extraDays).ifEmpty { board.matches.map { espnDay(it.start) } }.distinct().sorted()
    val groups = if (sport == Sport.SOCCER) soccerRounds(days) else days.map { listOf(it) }
    return groups.map { group ->
        val stage = board.stages.find { group.first() in it.from..it.to }
        SportRound(
            from = group.first().toString(),
            to = group.last().toString(),
            days = group.map { it.toString() },
            label = stage?.label.orEmpty()
        )
    }
}

/**
 * **Le giornate del calcio**: ESPN non dice il numero della giornata, solo i
 * giorni in cui si gioca. Giorni attaccati sono una giornata (Serie A,
 * Premier, Bundesliga, Ligue 1: tornano 38 e 34). Se il gruppo dura più di
 * quattro giorni (LaLiga con i recuperi a metà settimana, un torneo che
 * gioca ogni giorno) si divide in turno infrasettimanale (martedì-giovedì) e
 * turno del fine settimana (venerdì-lunedì).
 */
fun soccerRounds(days: List<LocalDate>): List<List<LocalDate>> {
    val groups = mutableListOf<MutableList<LocalDate>>()
    days.distinct().sorted().forEach { day ->
        val last = groups.lastOrNull()
        if (last != null && ChronoUnit.DAYS.between(last.last(), day) <= 1) last += day else groups += mutableListOf(day)
    }
    return groups.flatMap { group ->
        if (ChronoUnit.DAYS.between(group.first(), group.last()) <= 3) {
            listOf(group)
        } else {
            val split = mutableListOf<MutableList<LocalDate>>()
            group.forEach { day ->
                val last = split.lastOrNull()
                if (last != null && slotOf(last.last()) == slotOf(day)) last += day else split += mutableListOf(day)
            }
            split
        }
    }
}

/** Il turno di un giorno: il martedì del turno infrasettimanale o il venerdì del fine settimana. */
private fun slotOf(day: LocalDate): LocalDate = when (day.dayOfWeek) {
    DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY))
    DayOfWeek.MONDAY -> day.minusDays(3)
    else -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
}

/**
 * **La giornata di adesso**: quella di oggi, o la prossima. Una giornata di
 * più giorni resta anche il giorno dopo l'ultima partita (il martedì si
 * vedono i risultati della giornata finita il lunedì). A stagione finita,
 * l'ultima.
 */
fun currentRound(rounds: List<SportRound>, today: LocalDate): Int {
    if (rounds.isEmpty()) return -1
    val index = rounds.indexOfFirst { round ->
        round.lastDay >= today || (round.firstDay != round.lastDay && round.lastDay == today.minusDays(1))
    }
    return if (index == -1) rounds.lastIndex else index
}

/**
 * **La giornata di "Today"** (dal 20/10/2026, chiesto dall'utente): quella di
 * oggi, o la prossima. A differenza di `currentRound` non c'è il giorno dopo:
 * fino alle 23:59 del giorno dell'ultima partita resta quella giornata, da
 * mezzanotte si va alla successiva. A stagione finita, l'ultima.
 */
fun todayRound(rounds: List<SportRound>, today: LocalDate): Int {
    if (rounds.isEmpty()) return -1
    val index = rounds.indexOfFirst { it.lastDay >= today }
    return if (index == -1) rounds.lastIndex else index
}

/**
 * **"Giornata 7"** solo dove torna: un campionato di calcio con andata e
 * ritorno, dove le giornate trovate sono proprio due per ogni avversaria
 * (38 con venti squadre). Altrimenti (LaLiga con i recuperi, le coppe) le
 * pagine hanno solo le date.
 */
fun hasMatchdayNumbers(rounds: List<SportRound>, standings: List<StandingGroup>, sport: Sport): Boolean {
    if (sport != Sport.SOCCER || rounds.any { it.label.isNotEmpty() }) return false
    val teams = standings.singleOrNull()?.rows?.size ?: return false
    return teams > 2 && rounds.size == 2 * (teams - 1)
}

// --- Le risposte di ESPN, lette ---

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Una risposta "scoreboard": le partite, e il calendario della stagione — i
 * giorni di partita ([matchDays]) se ESPN lo dà a giorni, le settimane o le
 * fasi ([stages]) se lo dà a liste (NFL, coppe).
 */
data class Scoreboard(
    val matches: List<SportMatch>,
    val matchDays: List<LocalDate> = emptyList(),
    val stages: List<CalendarStage> = emptyList(),
    val season: SeasonInfo? = null
)

/** Una settimana (NFL: "Week 7") o una fase ("League Phase"), dal primo all'ultimo giorno. */
data class CalendarStage(val label: String, val from: LocalDate, val to: LocalDate, val seasonType: Int? = null, val week: Int? = null)

fun parseScoreboard(json: String, sport: Sport): Scoreboard? = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    val events = root.array("events")
    val matches = if (sport == Sport.TENNIS) {
        events.flatMap { tennisMatches(it as? JsonObject ?: return@flatMap emptyList()) }
    } else {
        events.mapNotNull { teamMatch(it as? JsonObject ?: return@mapNotNull null) }
    }
    val league = root.array("leagues").firstOrNull() as? JsonObject
    val calendar = league?.array("calendar").orEmpty()
    // Il calendario "a giorni" è una lista di date: quelle con partite, o
    // (baseball) quelle senza, fra l'inizio e la fine della stagione.
    val listed = calendar.mapNotNull { day -> day.string()?.let { parseDate(it)?.atOffset(ZoneOffset.UTC)?.toLocalDate() } }
    val whitelist = league?.string("calendarIsWhitelist") != "false"
    val days = if (whitelist || listed.isEmpty()) {
        listed
    } else {
        val start = league?.string("calendarStartDate")?.let { parseDate(it)?.atOffset(ZoneOffset.UTC)?.toLocalDate() }
        val end = league?.string("calendarEndDate")?.let { parseDate(it)?.atOffset(ZoneOffset.UTC)?.toLocalDate() }
        if (start == null || end == null || end <= start) emptyList() else {
            val off = listed.toSet()
            generateSequence(start) { it.plusDays(1) }.takeWhile { it < end }.filter { it !in off }.toList()
        }
    }
    // Quello "a liste": parti della stagione con dentro le settimane (NFL) o le fasi (coppe).
    val stages = calendar.mapNotNull { it as? JsonObject }.flatMap { section ->
        val seasonType = section.int("value")
        section.array("entries").mapNotNull { entry ->
            val item = entry as? JsonObject ?: return@mapNotNull null
            val from = item.string("startDate")?.let { parseDate(it) } ?: return@mapNotNull null
            val to = item.string("endDate")?.let { parseDate(it) } ?: return@mapNotNull null
            CalendarStage(
                label = item.string("label").orEmpty(),
                from = from.atOffset(ZoneOffset.UTC).toLocalDate(),
                // La fine è la mezzanotte americana del giorno dopo (07:59Z).
                to = to.atOffset(ZoneOffset.UTC).minusHours(8).toLocalDate(),
                seasonType = seasonType,
                week = if (sport == Sport.FOOTBALL) item.int("value") else null
            )
        }
    }
    val season = league?.obj("season")?.let { info ->
        info.int("year")?.let { year ->
            SeasonInfo(
                year = year,
                name = info.string("displayName").orEmpty(),
                start = info.string("startDate").orEmpty(),
                end = info.string("endDate").orEmpty()
            )
        }
    }
    Scoreboard(matches, days, stages, season)
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
        away = teamSide(away, state),
        venue = competition.obj("venue")?.string("fullName") ?: event.obj("venue")?.string("displayName").orEmpty()
    )
}

private fun teamSide(side: JsonObject, state: MatchState): MatchSide {
    val team = side.obj("team") ?: side.obj("athlete")
    val name = team?.string("shortDisplayName") ?: team?.string("displayName") ?: team?.string("name") ?: "?"
    return MatchSide(
        name = name,
        // Prima della partita ESPN scrive "0": meglio niente.
        score = side.string("score")?.takeIf { state != MatchState.PRE },
        winner = side.boolean("winner"),
        logo = team?.let { logoOf(it) }.orEmpty()
    )
}

/** Lo stemma: "logo", o il primo di "logos"; per i giocatori la bandiera ("flag", un oggetto o un indirizzo). */
private fun logoOf(team: JsonObject): String? =
    team.string("logo")
        ?: (team.array("logos").firstOrNull() as? JsonObject)?.string("href")
        ?: team.obj("flag")?.string("href")
        ?: team.string("flag")

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
                    note = listOf(tournament, round).filter { it.isNotBlank() }.joinToString(" · "),
                    tournament = tournament
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
        winner = side.boolean("winner"),
        logo = athlete?.let { logoOf(it) }.orEmpty()
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
                        values = columns.map { s[it]?.string("displayValue").orEmpty() },
                        logo = team?.let { logoOf(it) }.orEmpty()
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

/** **La classifica del tennis**: i primi [limit] giocatori ATP o WTA, coi punti e la bandiera. */
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
                    values = listOf(entry.double("points")?.let { Math.round(it).toString() }.orEmpty()),
                    logo = logoOf(athlete).orEmpty()
                )
            }
        )
    }
}.getOrDefault(emptyList())

private fun parseDate(text: String): Instant? =
    runCatching { Instant.parse(text) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(text.replace("Z", ":00Z")).toInstant() }.getOrNull()

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()
private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.let { d -> Math.round(d).toInt() } ?: it.contentOrNull?.toIntOrNull() }
private fun JsonObject.boolean(key: String): Boolean = (this[key] as? JsonPrimitive)?.contentOrNull == "true"
private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.contentOrNull
