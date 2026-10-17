package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.gabriele.notionlocal.data.widgets.SportsUpdates
import com.gabriele.notionlocal.data.widgets.SportsWidget
import com.gabriele.notionlocal.data.widgets.StandingGroup
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.matchesForWidget
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle

// ---------------------------------------------------------------------------
// **Il widget dello sport** (dal 17/10/2026, chiesto dall'utente): una pagina
// per competizione, che si sfogliano. Ogni pagina: i risultati appena finiti,
// le partite in corso e le prossime. Un tocco apre tutte le partite e la
// classifica; tenendo premuto si scelgono le competizioni. Vedi `SportsWidget`.
// ---------------------------------------------------------------------------

/** Il rosso delle partite in corso. */
private val LiveRed = Color(0xFFE5484D)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SportsView(
    widget: SportsWidget,
    active: Boolean,
    now: Long,
    managing: Boolean,
    onManagingChange: (Boolean) -> Unit
) {
    var detailFor by remember(widget.id) { mutableStateOf<String?>(null) }
    var adding by remember(widget.id) { mutableStateOf(false) }

    // Con la barra aperta si guarda ogni mezzo minuto se c'è da riscaricare:
    // ogni cinque minuti, ogni minuto con una partita in corso.
    val latest by rememberUpdatedState(widget)
    LaunchedEffect(active, widget.competitions.map { it.id }) {
        while (active) {
            SportsUpdates.refresh(latest, AppSettings.zoneId)
            delay(30_000)
        }
    }

    if (widget.competitions.isEmpty()) {
        AddRow(label = WidgetStrings.addCompetition, onClick = { adding = true })
    } else {
        val pagerState = rememberPagerState(pageCount = { widget.competitions.size })
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state = pagerState,
                key = { page -> widget.competitions.getOrNull(page)?.id ?: page },
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val competition = widget.competitions.getOrNull(page) ?: return@HorizontalPager
                CompetitionPage(
                    competition = competition,
                    report = widget.reports[competition.id],
                    now = now,
                    failed = competition.id in SportsUpdates.failed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { detailFor = competition.id },
                            onLongClick = { onManagingChange(true) }
                        )
                )
            }
            if (widget.competitions.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(widget.competitions.size) { i ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (i == pagerState.currentPage) NotionWhite else NotionGray400.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }

    detailFor?.let { id ->
        val competition = widget.competitions.find { it.id == id }
        if (competition == null) {
            detailFor = null
        } else {
            CompetitionSheet(competition, widget.reports[competition.id], now, onDismiss = { detailFor = null })
        }
    }
    if (managing) {
        CompetitionsSheet(widget = widget, startWithCatalog = false, onDismiss = { onManagingChange(false) })
    }
    if (adding) {
        CompetitionsSheet(widget = widget, startWithCatalog = true, onDismiss = { adding = false })
    }
}

@Composable
private fun CompetitionPage(competition: SportCompetition, report: SportReport?, now: Long, failed: Boolean, modifier: Modifier) {
    val sport = Sport.of(competition.sport)
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sport.emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                competition.name,
                color = NotionWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (report == null) {
            Text(
                text = if (failed) WidgetStrings.noConnection else WidgetStrings.loading,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            return@Column
        }
        val shown = matchesForWidget(report.matches, now)
        if (shown.isEmpty()) {
            Text(WidgetStrings.noMatches, color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            shown.forEach { match -> MatchRow(match, sport, now, compact = true) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = WidgetStrings.updatedAt(clockAt(report.fetchedAt)) + if (failed) " · " + WidgetStrings.noConnection else "",
            color = NotionGray400,
            fontSize = 10.sp
        )
    }
}

/**
 * Una partita su due righe, una per squadra (o giocatore), col punteggio a
 * destra e il vincitore in grassetto. A sinistra: l'ora, o il minuto in rosso
 * se è in corso, o "Finale".
 */
@Composable
private fun MatchRow(match: SportMatch, sport: Sport, now: Long, compact: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 3.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.width(if (compact) 52.dp else 64.dp)) {
            // Nella finestra il giorno è già nel titolo sopra le partite.
            val (top, bottom, live) = statusLines(match, now, withDay = compact)
            if (top.isNotEmpty()) Text(top, color = NotionGray400, fontSize = 10.sp, maxLines = 1)
            Text(bottom, color = if (live) LiveRed else NotionGray400, fontSize = 11.sp, maxLines = 2, fontWeight = if (live) FontWeight.SemiBold else null)
        }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            side.name,
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

/** Le due righe a sinistra di una partita: (sopra, sotto, in corso). */
private fun statusLines(match: SportMatch, now: Long, withDay: Boolean): Triple<String, String, Boolean> {
    val zone = AppSettings.zoneId
    val start = Instant.ofEpochMilli(match.start).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val day = if (!withDay) "" else when (start.toLocalDate()) {
        today -> ""
        today.plusDays(1) -> WidgetStrings.tomorrow
        else -> start.dayOfWeek.getDisplayName(TextStyle.SHORT, AppSettings.language.locale) + " ${start.dayOfMonth}"
    }
    return when {
        match.postponed -> Triple(day, WidgetStrings.postponed, false)
        match.state == MatchState.IN -> Triple("", match.status.ifEmpty { "Live" }, true)
        match.state == MatchState.POST -> Triple(day, WidgetStrings.finalScore, false)
        else -> Triple(day, Formats.timeFormatter().format(start), false)
    }
}

private fun clockAt(millis: Long): String = Formats.timeFormatter().format(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId))

// --- Partite e classifica ---

/** Tutte le partite scaricate, divise per giorno, e la classifica (o il ranking, nel tennis). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetitionSheet(competition: SportCompetition, report: SportReport?, now: Long, onDismiss: () -> Unit) {
    val sport = Sport.of(competition.sport)
    var tab by remember { mutableIntStateOf(0) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sport.emoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(competition.name, color = NotionWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
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
            if (report == null) {
                Text(WidgetStrings.noConnection, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
                return@Column
            }
            if (tab == 0) MatchesList(report, sport, now) else StandingsTable(report.standings, sport)
        }
    }
}

@Composable
private fun MatchesList(report: SportReport, sport: Sport, now: Long) {
    val zone = AppSettings.zoneId
    val byDay = report.matches.sortedBy { it.start }.groupBy { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }
    if (byDay.isEmpty()) {
        Text(WidgetStrings.noMatches, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        byDay.forEach { (day, matches) ->
            item(key = "day-$day") {
                Text(
                    dayTitle(day, today),
                    color = NotionWhite,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                )
            }
            items(matches, key = { it.id }) { match -> MatchRow(match, sport, now, compact = false) }
        }
        item {
            Text(
                WidgetStrings.updatedAt(clockAt(report.fetchedAt)) + " · " + WidgetStrings.sportsSource,
                color = NotionGray400,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
            )
        }
    }
}

private fun dayTitle(day: LocalDate, today: LocalDate): String = when (day) {
    today -> WidgetStrings.today
    today.plusDays(1) -> WidgetStrings.tomorrow
    else -> day.dayOfWeek.getDisplayName(TextStyle.FULL, AppSettings.language.locale)
        .replaceFirstChar { it.titlecase(AppSettings.language.locale) } + " ${day.dayOfMonth}"
}

/** La classifica: una tabella per gruppo, col nome del gruppo se ce n'è più d'uno. */
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
                    header = true
                )
                HorizontalDivider()
            }
            items(group.rows, key = { "g$g-${it.rank}-${it.team}" }) { row ->
                StandingLine(rank = row.rank.toString(), team = row.team, values = row.values, header = false)
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun StandingLine(rank: String, team: String, values: List<String>, header: Boolean) {
    val color = if (header) NotionGray400 else NotionWhite
    val size = if (header) 11.sp else 13.sp
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rank, color = NotionGray400, fontSize = size, modifier = Modifier.width(28.dp))
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
        Text(sport.emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(competition.name, color = NotionWhite, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(WidgetStrings.sportName(sport), color = NotionGray400, style = MaterialTheme.typography.bodySmall)
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

/** Le competizioni fra cui scegliere, divise per sport; quelle già aggiunte hanno la spunta. */
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
