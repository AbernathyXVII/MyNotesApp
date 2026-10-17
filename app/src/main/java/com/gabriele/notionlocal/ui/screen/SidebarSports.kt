package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CatalogEntry
import com.gabriele.notionlocal.data.widgets.MAX_SPORT_COMPETITIONS
import com.gabriele.notionlocal.data.widgets.MatchSide
import com.gabriele.notionlocal.data.widgets.MatchState
import com.gabriele.notionlocal.data.widgets.SPORT_CATALOG
import com.gabriele.notionlocal.data.widgets.Sport
import com.gabriele.notionlocal.data.widgets.SportCompetition
import com.gabriele.notionlocal.data.widgets.SportMatch
import com.gabriele.notionlocal.data.widgets.SportReport
import com.gabriele.notionlocal.data.widgets.SportRound
import com.gabriele.notionlocal.data.widgets.SportsUpdates
import com.gabriele.notionlocal.data.widgets.SportsWidget
import com.gabriele.notionlocal.data.widgets.StandingGroup
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.competitionFlag
import com.gabriele.notionlocal.data.widgets.hasMatchdayNumbers
import com.gabriele.notionlocal.data.widgets.teamDisplayName
import com.gabriele.notionlocal.data.widgets.todayIn
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

// ---------------------------------------------------------------------------
// **Il widget dello sport** (dal 17/10/2026, chiesto dall'utente). In alto la
// competizione, con la bandiera, e le frecce per passare alle altre (grigie
// alla prima e all'ultima). Sotto una giornata intera, con gli stemmi delle
// squadre; scorrendo, le giornate prima e dopo. Un tocco apre le partite
// della giornata e la classifica; tenendo premuto si scelgono le
// competizioni. Vedi `SportsWidget`.
// ---------------------------------------------------------------------------

/** Il rosso delle partite in corso. */
private val LiveRed = Color(0xFFE5484D)

@Composable
internal fun SportsView(
    widget: SportsWidget,
    active: Boolean,
    now: Long,
    managing: Boolean,
    onManagingChange: (Boolean) -> Unit
) {
    var adding by remember(widget.id) { mutableStateOf(false) }

    // Aprendo la barra si vede la prima competizione.
    LaunchedEffect(widget.id, active) {
        if (active) widget.competitions.firstOrNull()?.let { SportsUpdates.shown[widget.id] = it.id }
    }
    val index = widget.competitions.indexOfFirst { it.id == SportsUpdates.shown[widget.id] }.coerceAtLeast(0)
    val competition = widget.competitions.getOrNull(index)

    if (competition == null) {
        AddRow(label = WidgetStrings.addCompetition, onClick = { adding = true })
    } else {
        // Con la barra aperta, ogni mezzo minuto si guarda se c'è da
        // riscaricare la competizione che si vede (vedi `SportsUpdates`).
        val latest by rememberUpdatedState(widget)
        LaunchedEffect(active, competition.id) {
            while (active) {
                latest.competitions.find { it.id == competition.id }?.let { SportsUpdates.refresh(latest, it, AppSettings.zoneId) }
                delay(30_000)
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            CompetitionHeader(
                competition = competition,
                index = index,
                count = widget.competitions.size,
                onMove = { delta -> widget.competitions.getOrNull(index + delta)?.let { SportsUpdates.shown[widget.id] = it.id } }
            )
            CompetitionRounds(widget, competition, active, now, onManage = { onManagingChange(true) })
        }
    }

    if (managing) {
        CompetitionsSheet(widget = widget, startWithCatalog = false, onDismiss = { onManagingChange(false) })
    }
    if (adding) {
        CompetitionsSheet(widget = widget, startWithCatalog = true, onDismiss = { adding = false })
    }
}

/** La bandiera e il nome in mezzo, le frecce ai lati: grigie se non c'è una competizione da quella parte. */
@Composable
private fun CompetitionHeader(competition: SportCompetition, index: Int, count: Int, onMove: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArrowButton(Icons.Filled.KeyboardArrowLeft, WidgetStrings.previousCompetition, enabled = index > 0) { onMove(-1) }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompetitionFlag(competition.league, 18.sp)
            Text(
                competition.name,
                color = NotionWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        ArrowButton(Icons.Filled.KeyboardArrowRight, WidgetStrings.nextCompetition, enabled = index < count - 1) { onMove(1) }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = description, tint = if (enabled) NotionWhite else NotionGray400.copy(alpha = 0.35f))
    }
}

/** La bandiera della competizione (vedi `competitionFlag`), con lo spazio dopo; niente se non c'è. */
@Composable
private fun CompetitionFlag(league: String, size: TextUnit) {
    val flag = competitionFlag(league)
    if (flag.isNotEmpty()) {
        Text(flag, fontSize = size)
        Spacer(modifier = Modifier.width(8.dp))
    }
}

/** Le partite di una giornata: null finché non arrivano. */
private data class RoundContent(val matches: List<SportMatch>?, val fetchedAt: Long, val failed: Boolean)

/** La giornata di adesso sta nel widget; le altre nella memoria di `SportsUpdates`. */
private fun roundContent(report: SportReport, competitionId: String, page: Int, round: SportRound): RoundContent =
    if (page == report.round) {
        RoundContent(report.matches.takeIf { report.fetchedAt > 0 }, report.fetchedAt, competitionId in SportsUpdates.failed)
    } else {
        val cached = SportsUpdates.cachedRound(competitionId, round)
        RoundContent(cached?.matches, cached?.fetchedAt ?: 0L, SportsUpdates.roundKey(competitionId, round) in SportsUpdates.failed)
    }

/** Le giornate della competizione, una per pagina, a partire da quella di adesso. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompetitionRounds(widget: SportsWidget, competition: SportCompetition, active: Boolean, now: Long, onManage: () -> Unit) {
    val sport = Sport.of(competition.sport)
    val report = widget.reports[competition.id]
    if (report == null || report.rounds.isEmpty()) {
        val text = when {
            report != null -> WidgetStrings.noMatches
            competition.id in SportsUpdates.failed -> WidgetStrings.noConnection
            else -> WidgetStrings.loading
        }
        Text(
            text = text,
            color = NotionGray400,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onManage)
                .padding(vertical = 16.dp)
        )
        return
    }
    var detail by remember(competition.id) { mutableStateOf<Int?>(null) }
    val numbered = hasMatchdayNumbers(report.rounds, report.standings, sport)
    // Gli stemmi di tutte le squadre (la classifica) e della giornata, in
    // anticipo: scorrendo, o aprendo la classifica, ci sono già.
    val cacheDir = LocalContext.current.cacheDir
    LaunchedEffect(competition.id, report.standings, report.matches) {
        TeamLogos.prefetch(
            cacheDir,
            report.standings.flatMap { group -> group.rows.map { it.logo } } +
                report.matches.flatMap { listOf(it.home.logo, it.away.logo) }
        )
    }
    key(competition.id) {
        val pagerState = rememberPagerState(initialPage = report.round.coerceIn(0, report.rounds.lastIndex)) { report.rounds.size }
        // La giornata su cui ci si ferma scorrendo si scarica (quella di adesso si aggiorna da sé).
        val latest by rememberUpdatedState(report)
        LaunchedEffect(active, pagerState.settledPage) {
            while (active) {
                val page = pagerState.settledPage
                val shown = latest
                if (page != shown.round) {
                    shown.rounds.getOrNull(page)?.let { round ->
                        SportsUpdates.loadRound(competition, round, todayIn(AppSettings.zoneId, System.currentTimeMillis()))
                    }
                }
                delay(30_000)
            }
        }
        HorizontalPager(
            state = pagerState,
            key = { page -> report.rounds.getOrNull(page)?.key ?: page },
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val round = report.rounds.getOrNull(page) ?: return@HorizontalPager
            RoundPage(
                title = roundTitle(round, page, numbered),
                content = roundContent(report, competition.id, page, round),
                sport = sport,
                now = now,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = { detail = page }, onLongClick = onManage)
            )
        }
    }
    detail?.let { page ->
        val round = report.rounds.getOrNull(page)
        if (round == null) {
            detail = null
        } else {
            CompetitionSheet(
                competition = competition,
                title = roundTitle(round, page, numbered),
                content = roundContent(report, competition.id, page, round),
                standings = report.standings,
                now = now,
                onDismiss = { detail = null }
            )
        }
    }
}

@Composable
private fun RoundPage(title: String, content: RoundContent, sport: Sport, now: Long, modifier: Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(title, color = NotionGray400, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val matches = content.matches
        when {
            matches == null -> Text(
                text = if (content.failed) WidgetStrings.noConnection else WidgetStrings.loading,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            matches.isEmpty() -> Text(
                WidgetStrings.noMatches,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            else -> {
                Spacer(modifier = Modifier.height(4.dp))
                MatchList(matches, sport, now, compact = true)
            }
        }
        if (content.fetchedAt > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = WidgetStrings.updatedAt(clockAt(content.fetchedAt)) + if (content.failed) " · " + WidgetStrings.noConnection else "",
                color = NotionGray400,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Il nome della giornata e i suoi giorni: "Giornata 7 · Fri 16 – Mon 19
 * Oct" dove il numero torna (vedi `hasMatchdayNumbers`), "Week 7 · …" o
 * "League Phase · …" col nome di ESPN, altrimenti solo i giorni.
 */
private fun roundTitle(round: SportRound, index: Int, numbered: Boolean): String {
    val name = when {
        numbered -> WidgetStrings.matchday(index + 1)
        round.label.isNotEmpty() -> round.label
        else -> ""
    }
    val dates = roundDates(round.firstDay, round.lastDay)
    return if (name.isEmpty()) dates else "$name · $dates"
}

/** "Sat 17 Oct", "Fri 16 – Mon 19 Oct", "Sat 31 Oct – Mon 2 Nov"; in Asia "10月16日 (金)". */
private fun roundDates(from: LocalDate, to: LocalDate): String {
    val language = AppSettings.language
    val locale = language.locale
    fun weekday(day: LocalDate) = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    if (language == AppLanguage.CHINESE || language == AppLanguage.JAPANESE || language == AppLanguage.KOREAN) {
        val pattern = DateTimeFormatter.ofPattern(if (language == AppLanguage.KOREAN) "M월 d일" else "M月d日")
        fun one(day: LocalDate) = pattern.format(day) + " (" + weekday(day) + ")"
        return if (from == to) one(from) else one(from) + " – " + one(to)
    }
    fun month(day: LocalDate) = day.month.getDisplayName(TextStyle.SHORT, locale)
    fun dayOf(day: LocalDate) = weekday(day) + " " + day.dayOfMonth
    return when {
        from == to -> "${dayOf(from)} ${month(from)}"
        from.year == to.year && from.month == to.month -> "${dayOf(from)} – ${dayOf(to)} ${month(to)}"
        else -> "${dayOf(from)} ${month(from)} – ${dayOf(to)} ${month(to)}"
    }
}

/**
 * **Le partite di una giornata**, in ordine di tempo, divise per giorno
 * (col nome del giorno solo se sono più d'uno); nel tennis per torneo.
 */
@Composable
private fun MatchList(matches: List<SportMatch>, sport: Sport, now: Long, compact: Boolean) {
    val zone = AppSettings.zoneId
    val today = todayIn(zone, now)
    val sorted = matches.sortedBy { it.start }
    val groups: List<Pair<String, List<SportMatch>>> = if (sport == Sport.TENNIS) {
        sorted.groupBy { it.tournament }.toList()
    } else {
        sorted.groupBy { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }.map { (day, list) -> dayTitle(day, today) to list }
    }
    groups.forEachIndexed { i, (title, list) ->
        if (title.isNotEmpty() && (groups.size > 1 || sport == Sport.TENNIS)) {
            Text(
                title,
                color = if (compact) NotionGray400 else NotionWhite,
                fontSize = if (compact) 11.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (i == 0) 2.dp else 10.dp, bottom = 2.dp)
            )
        }
        list.forEach { match -> MatchRow(match, sport, compact) }
    }
}

/**
 * Una partita su due righe, una per squadra (o giocatore), con lo stemma
 * (o la bandiera) a sinistra, il punteggio a destra e il vincitore in
 * grassetto. Prima: l'ora, o il minuto in rosso se è in corso, o "Finale".
 */
@Composable
private fun MatchRow(match: SportMatch, sport: Sport, compact: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 3.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val (status, live) = statusOf(match)
        Text(
            status,
            color = if (live) LiveRed else NotionGray400,
            fontSize = 11.sp,
            maxLines = 2,
            fontWeight = if (live) FontWeight.SemiBold else null,
            modifier = Modifier.width(if (compact) 48.dp else 60.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            SideLine(match.home, match.state, sport)
            SideLine(match.away, match.state, sport)
            if (!compact && match.note.isNotEmpty()) {
                Text(match.note, color = NotionGray400, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SideLine(side: MatchSide, state: MatchState, sport: Sport) {
    val weight = if (state == MatchState.POST && side.winner) FontWeight.Bold else null
    Row(modifier = Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(side.logo, 16.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            teamDisplayName(side.name),
            color = NotionWhite,
            fontSize = 13.sp,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val score = if (sport == Sport.TENNIS) side.sets.joinToString(" ") else side.score.orEmpty()
        if (score.isNotEmpty()) {
            Text(score, color = NotionWhite, fontSize = 13.sp, fontWeight = weight, textAlign = TextAlign.End)
        }
    }
}

/** A sinistra di una partita: (cosa scrivere, se è in corso). */
private fun statusOf(match: SportMatch): Pair<String, Boolean> = when {
    match.postponed -> WidgetStrings.postponed to false
    match.state == MatchState.IN -> match.status.ifEmpty { "Live" } to true
    match.state == MatchState.POST -> WidgetStrings.finalScore to false
    else -> Formats.timeFormatter().format(Instant.ofEpochMilli(match.start).atZone(AppSettings.zoneId)) to false
}

private fun clockAt(millis: Long): String = Formats.timeFormatter().format(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId))

private fun dayTitle(day: LocalDate, today: LocalDate): String = when (day) {
    today -> WidgetStrings.today
    today.plusDays(1) -> WidgetStrings.tomorrow
    else -> day.dayOfWeek.getDisplayName(TextStyle.FULL, AppSettings.language.locale)
        .replaceFirstChar { it.titlecase(AppSettings.language.locale) } + " ${day.dayOfMonth}"
}

// --- Partite e classifica ---

/** Le partite della giornata toccata, divise per giorno, e la classifica (o il ranking, nel tennis). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetitionSheet(
    competition: SportCompetition,
    title: String,
    content: RoundContent,
    standings: List<StandingGroup>,
    now: Long,
    onDismiss: () -> Unit
) {
    val sport = Sport.of(competition.sport)
    var tab by remember { mutableIntStateOf(0) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompetitionFlag(competition.league, 22.sp)
                Text(competition.name, color = NotionWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(title, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            TabRow(selectedTabIndex = tab, containerColor = DarkSheet) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(WidgetStrings.matches) }, unselectedContentColor = NotionGray400)
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    unselectedContentColor = NotionGray400,
                    text = { Text(if (sport == Sport.TENNIS) WidgetStrings.ranking else WidgetStrings.standings) }
                )
            }
            if (tab == 1) {
                StandingsTable(standings, sport)
                return@Column
            }
            val matches = content.matches
            when {
                matches == null -> Text(
                    if (content.failed) WidgetStrings.noConnection else WidgetStrings.loading,
                    color = NotionGray400,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                matches.isEmpty() -> Text(WidgetStrings.noMatches, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
                else -> Column(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MatchList(matches, sport, now, compact = false)
                    Text(
                        WidgetStrings.updatedAt(clockAt(content.fetchedAt)) + " · " + WidgetStrings.sportsSource,
                        color = NotionGray400,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                    )
                }
            }
        }
    }
}

/** La classifica: una tabella per gruppo, col nome del gruppo se ce n'è più d'uno, e gli stemmi. */
@Composable
private fun StandingsTable(groups: List<StandingGroup>, sport: Sport) {
    if (groups.isEmpty()) {
        Text(WidgetStrings.noStandings, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        groups.forEachIndexed { g, group ->
            item(key = "group-$g") {
                if (groups.size > 1) {
                    Text(
                        group.name,
                        color = NotionWhite,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                StandingLine(
                    rank = "#",
                    team = if (sport == Sport.TENNIS) WidgetStrings.player else WidgetStrings.team,
                    values = group.columns,
                    logo = null
                )
                HorizontalDivider()
            }
            items(group.rows, key = { "g$g-${it.rank}-${it.team}" }) { row ->
                StandingLine(rank = row.rank.toString(), team = teamDisplayName(row.team), values = row.values, logo = row.logo)
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/** Una riga della classifica; [logo] null è l'intestazione. */
@Composable
private fun StandingLine(rank: String, team: String, values: List<String>, logo: String?) {
    val header = logo == null
    val color = if (header) NotionGray400 else NotionWhite
    val size = if (header) 11.sp else 13.sp
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rank, color = NotionGray400, fontSize = size, modifier = Modifier.width(28.dp))
        if (logo == null) Spacer(modifier = Modifier.width(22.dp)) else {
            TeamLogo(logo, 16.dp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(team, color = color, fontSize = size, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        values.forEachIndexed { i, value ->
            Text(
                value,
                color = color,
                fontSize = size,
                // L'ultima colonna (i punti, nel calcio) in grassetto.
                fontWeight = if (!header && i == values.lastIndex) FontWeight.SemiBold else null,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(if (value.length > 4 || values.size <= 2) 52.dp else 36.dp)
            )
        }
    }
}

// --- Le competizioni ---

/**
 * **Le competizioni del widget**: "Add a competition" in cima, e per ognuna,
 * dal menu ⋮, Show first, Move up, Move down, Remove competition.
 * [startWithCatalog]: si apre già sull'elenco da cui scegliere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetitionsSheet(widget: SportsWidget, startWithCatalog: Boolean, onDismiss: () -> Unit) {
    var picking by remember { mutableStateOf(startWithCatalog) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            if (picking) {
                Catalog(
                    widget = widget,
                    onBack = if (startWithCatalog && widget.competitions.isEmpty()) null else ({ picking = false }),
                    onPick = { entry ->
                        WidgetStore.addSportCompetition(widget.id, entry.toCompetition())
                        if (startWithCatalog) onDismiss() else picking = false
                    }
                )
            } else {
                Text(WidgetStrings.competitions, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(WidgetStrings.competitionsNote, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (widget.competitions.size < MAX_SPORT_COMPETITIONS) {
                    AddRow(label = WidgetStrings.addCompetition, onClick = { picking = true })
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                    items(widget.competitions, key = { it.id }) { competition ->
                        val index = widget.competitions.indexOf(competition)
                        CompetitionRow(
                            competition = competition,
                            index = index,
                            count = widget.competitions.size,
                            onMove = { delta -> WidgetStore.moveSportCompetition(widget.id, competition.id, delta) },
                            onRemove = { WidgetStore.removeSportCompetition(widget.id, competition.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CompetitionRow(competition: SportCompetition, index: Int, count: Int, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var menu by remember(competition.id) { mutableStateOf(false) }
    val sport = Sport.of(competition.sport)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
        CompetitionFlag(competition.league, 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(competition.name, color = NotionWhite, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${sport.emoji} ${WidgetStrings.sportName(sport)}", color = NotionGray400, style = MaterialTheme.typography.bodySmall)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = WidgetStrings.widgetOptions, tint = NotionGray400)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (index > 0) {
                    DropdownMenuItem(text = { Text(WidgetStrings.makeFirst) }, onClick = { menu = false; onMove(-index) })
                }
                DropdownMenuItem(text = { Text(WidgetStrings.moveUp) }, enabled = index > 0, onClick = { menu = false; onMove(-1) })
                DropdownMenuItem(text = { Text(WidgetStrings.moveDown) }, enabled = index < count - 1, onClick = { menu = false; onMove(1) })
                DropdownMenuItem(
                    text = { Text(WidgetStrings.removeCompetition, color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onRemove() }
                )
            }
        }
    }
}

/** Le competizioni fra cui scegliere, divise per sport, con la bandiera; quelle già aggiunte hanno la spunta. */
@Composable
private fun Catalog(widget: SportsWidget, onBack: (() -> Unit)?, onPick: (CatalogEntry) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite) }
        }
        Text(WidgetStrings.addCompetition, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    val added = widget.competitions.map { it.sport to it.league }.toSet()
    val full = widget.competitions.size >= MAX_SPORT_COMPETITIONS
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
        SPORT_CATALOG.groupBy { it.sport }.forEach { (sport, entries) ->
            item(key = "sport-${sport.path}") {
                Text(
                    "${sport.emoji}  ${WidgetStrings.sportName(sport)}",
                    color = NotionGray400,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                )
            }
            items(entries, key = { "${it.sport.path}/${it.league}" }) { entry ->
                val already = (entry.sport.path to entry.league) in added
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !already && !full) { onPick(entry) }
                        .padding(horizontal = 8.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompetitionFlag(entry.league, 16.sp)
                    Text(
                        entry.name,
                        color = if (already || full) NotionGray400 else NotionWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (already) Icon(Icons.Filled.Check, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
