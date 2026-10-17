package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.MatchDetail
import com.gabriele.notionlocal.data.widgets.MatchDetails
import com.gabriele.notionlocal.data.widgets.MatchEvent
import com.gabriele.notionlocal.data.widgets.MatchEventKind
import com.gabriele.notionlocal.data.widgets.MatchSide
import com.gabriele.notionlocal.data.widgets.MatchStat
import com.gabriele.notionlocal.data.widgets.MatchState
import com.gabriele.notionlocal.data.widgets.Side
import com.gabriele.notionlocal.data.widgets.Sport
import com.gabriele.notionlocal.data.widgets.SportCompetition
import com.gabriele.notionlocal.data.widgets.SportMatch
import com.gabriele.notionlocal.data.widgets.teamDisplayName
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import java.time.Instant

// ---------------------------------------------------------------------------
// **Una partita da vicino** (dal 17/10/2026, chiesto dall'utente, come la fa
// Sofascore): toccando una partita. In alto le due squadre e il risultato;
// nel calcio la cronaca — dall'ultimo evento al primo, la casa a sinistra e
// gli ospiti a destra — e le statistiche; negli altri sport i punti per
// periodo (e le azioni da punti nella NFL) e le statistiche; nel tennis i
// set. Vedi `MatchDetail`.
// ---------------------------------------------------------------------------

private val YellowCard = Color(0xFFF5C518)
private val RedCard = Color(0xFFE5484D)
private val SubIn = Color(0xFF3FB950)
private val HomeBar = Color(0xFF4C8DFF)
private val AwayBar = Color(0xFFE5A04C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MatchSheet(competition: SportCompetition, match: SportMatch, now: Long, onDismiss: () -> Unit) {
    val sport = Sport.of(competition.sport)
    // Si scarica appena si apre; finché resta aperta si guarda ogni mezzo
    // minuto se c'è da riscaricare (in corso, ogni minuto).
    LaunchedEffect(match.id, match.state) {
        if (sport == Sport.TENNIS) return@LaunchedEffect
        while (true) {
            MatchDetails.load(competition, match)
            delay(30_000)
        }
    }
    var tab by remember(match.id) { mutableIntStateOf(0) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompetitionFlag(competition.league, 14.sp)
                Text(
                    listOf(competition.name, match.note).filter { it.isNotBlank() }.joinToString(" · "),
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MatchHeader(match, sport)
            Spacer(modifier = Modifier.height(12.dp))
            if (sport == Sport.TENNIS) {
                TennisSets(match)
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }
            TabRow(selectedTabIndex = tab, containerColor = DarkSheet) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(if (sport == Sport.SOCCER) WidgetStrings.events else WidgetStrings.scoreTab) },
                    unselectedContentColor = NotionGray400
                )
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(WidgetStrings.statistics) }, unselectedContentColor = NotionGray400)
            }
            val detail = MatchDetails.of(match.id)?.detail
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
                when {
                    detail == null -> Text(
                        if (MatchDetails.failed(match.id)) WidgetStrings.noConnection else WidgetStrings.loading,
                        color = NotionGray400,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                    tab == 1 -> StatsTable(detail.stats)
                    sport == Sport.SOCCER -> EventsTimeline(detail.events)
                    else -> ScoreByPeriod(detail, match)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/** Le due squadre con lo stemma, e in mezzo il risultato (o l'ora d'inizio) e lo stato. */
@Composable
private fun MatchHeader(match: SportMatch, sport: Sport) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TeamColumn(match.home, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
            val started = match.state != MatchState.PRE && !match.postponed
            val score = if (started) {
                "${match.home.score ?: 0} - ${match.away.score ?: 0}"
            } else {
                Formats.timeFormatter().format(Instant.ofEpochMilli(match.start).atZone(AppSettings.zoneId))
            }
            // Nel tennis il punteggio sono i set, sotto.
            val live = match.state == MatchState.IN
            if (sport != Sport.TENNIS || !started) {
                // In rosso mentre si gioca, come nel widget.
                Text(score, color = if (live) RedCard else NotionWhite, fontSize = if (started) 30.sp else 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                when {
                    match.postponed -> WidgetStrings.postponed
                    live -> match.status.ifEmpty { "Live" }
                    match.state == MatchState.POST -> WidgetStrings.finalScore
                    else -> Formats.date(match.start)
                },
                color = if (live) RedCard else NotionGray400,
                fontSize = 12.sp,
                fontWeight = if (live) FontWeight.SemiBold else null
            )
        }
        TeamColumn(match.away, Modifier.weight(1f))
    }
}

@Composable
private fun TeamColumn(side: MatchSide, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TeamLogo(side.logo, 40.dp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            teamDisplayName(side.name),
            color = NotionWhite,
            fontSize = 14.sp,
            fontWeight = if (side.winner) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- La cronaca (calcio) ---

/** Dall'ultimo evento al primo, come Sofascore: le fini dei tempi col punteggio, il recupero, poi gli eventi. */
@Composable
private fun EventsTimeline(events: List<MatchEvent>) {
    if (events.isEmpty()) {
        Text(WidgetStrings.noEvents, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    Spacer(modifier = Modifier.height(8.dp))
    events.asReversed().forEach { event ->
        when (event.kind) {
            MatchEventKind.HALF_TIME -> PeriodLine("${WidgetStrings.halfTimeShort} ${event.homeScore} - ${event.awayScore}")
            MatchEventKind.FULL_TIME -> PeriodLine("${WidgetStrings.fullTimeShort} ${event.homeScore} - ${event.awayScore}")
            MatchEventKind.ADDED_TIME -> if (event.added > 0) AddedTime(event.added)
            else -> EventRow(event)
        }
    }
}

@Composable
private fun PeriodLine(label: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(label, color = NotionWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp))
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AddedTime(minutes: Int) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            WidgetStrings.additionalTime(minutes),
            color = NotionWhite,
            fontSize = 11.sp,
            modifier = Modifier
                .background(NotionGray400.copy(alpha = 0.18f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/** Un evento: della casa a sinistra (minuto, segno, nomi), degli ospiti a destra, a specchio. */
@Composable
private fun EventRow(event: MatchEvent) {
    val home = event.side != Side.AWAY
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = if (home) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val minute: @Composable () -> Unit = {
            Text(event.minute, color = NotionGray400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = if (home) TextAlign.Start else TextAlign.End, modifier = Modifier.width(44.dp))
        }
        if (home) {
            minute()
            EventMark(event)
            Spacer(modifier = Modifier.width(10.dp))
            EventBody(event, home = true)
        } else {
            EventBody(event, home = false)
            Spacer(modifier = Modifier.width(10.dp))
            EventMark(event)
            minute()
        }
    }
}

/** Il segno dell'evento: il pallone, le frecce del cambio, il cartellino, il VAR. */
@Composable
private fun EventMark(event: MatchEvent) {
    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        when (event.kind) {
            MatchEventKind.GOAL, MatchEventKind.PENALTY_GOAL ->
                Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(18.dp))
            MatchEventKind.OWN_GOAL ->
                Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = RedCard, modifier = Modifier.size(18.dp))
            MatchEventKind.PENALTY_MISSED ->
                Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
            MatchEventKind.SUBSTITUTION -> Row {
                Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = SubIn, modifier = Modifier.size(12.dp))
                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = RedCard, modifier = Modifier.size(12.dp))
            }
            MatchEventKind.YELLOW_CARD -> Card(YellowCard)
            MatchEventKind.RED_CARD -> Card(RedCard)
            MatchEventKind.SECOND_YELLOW -> Box {
                Card(YellowCard, Modifier.offset(x = (-3).dp, y = (-2).dp))
                Card(RedCard, Modifier.offset(x = 3.dp, y = 2.dp))
            }
            MatchEventKind.VAR_NO_GOAL -> Text(
                "VAR",
                color = NotionWhite,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.border(1.dp, NotionGray400, RoundedCornerShape(3.dp)).padding(horizontal = 2.dp)
            )
            else -> {}
        }
    }
}

@Composable
private fun Card(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 10.dp, height = 14.dp).background(color, RoundedCornerShape(2.dp)))
}

@Composable
private fun EventBody(event: MatchEvent, home: Boolean) {
    val align = if (home) Alignment.Start else Alignment.End
    val textAlign = if (home) TextAlign.Start else TextAlign.End
    Column(horizontalAlignment = align) {
        when (event.kind) {
            MatchEventKind.GOAL, MatchEventKind.PENALTY_GOAL, MatchEventKind.OWN_GOAL -> {
                val mark = when (event.kind) {
                    MatchEventKind.OWN_GOAL -> " (${WidgetStrings.ownGoalMark})"
                    MatchEventKind.PENALTY_GOAL -> " (${WidgetStrings.penaltyMark})"
                    else -> ""
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val chip: @Composable () -> Unit = { ScoreChip("${event.homeScore} - ${event.awayScore}") }
                    if (home) chip()
                    Spacer(modifier = Modifier.width(if (home) 8.dp else 0.dp))
                    Text(event.player + mark, color = NotionWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(if (home) 0.dp else 8.dp))
                    if (!home) chip()
                }
                if (event.other.isNotEmpty()) SecondLine(WidgetStrings.assistBy(event.other), textAlign)
            }
            MatchEventKind.PENALTY_MISSED -> {
                MainLine(event.player, textAlign)
                SecondLine(WidgetStrings.missedPenalty, textAlign)
            }
            MatchEventKind.SUBSTITUTION -> {
                MainLine(event.player, textAlign)
                SecondLine(event.other, textAlign)
            }
            MatchEventKind.YELLOW_CARD, MatchEventKind.SECOND_YELLOW, MatchEventKind.RED_CARD -> {
                MainLine(event.player, textAlign)
                val reason = event.reason?.let { WidgetStrings.cardReason(it) } ?: event.reasonText
                if (reason.isNotEmpty()) SecondLine(reason, textAlign)
            }
            MatchEventKind.VAR_NO_GOAL -> {
                MainLine(WidgetStrings.goalCancelled, textAlign)
                if (event.player.isNotEmpty()) SecondLine(event.player, textAlign)
            }
            else -> {}
        }
    }
}

@Composable
private fun MainLine(text: String, align: TextAlign) {
    Text(text, color = NotionWhite, fontSize = 13.sp, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun SecondLine(text: String, align: TextAlign) {
    Text(text, color = NotionGray400, fontSize = 11.sp, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ScoreChip(score: String) {
    Text(
        score,
        color = NotionWhite,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.dp, HomeBar, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

// --- Le statistiche ---

/** Una riga per statistica: i due valori ai lati, il nome in mezzo, sotto la barra delle due parti. */
@Composable
private fun StatsTable(stats: List<MatchStat>) {
    val shown = stats.mapNotNull { stat -> WidgetStrings.statLabel(stat.key)?.let { it to stat } }
    if (shown.isEmpty()) {
        Text(WidgetStrings.noStats, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    Spacer(modifier = Modifier.height(8.dp))
    shown.forEach { (label, stat) ->
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val homeAhead = (stat.homeShare ?: 0.5f) > 0.5f
                val awayAhead = (stat.homeShare ?: 0.5f) < 0.5f
                Text(stat.home, color = NotionWhite, fontSize = 13.sp, fontWeight = if (homeAhead) FontWeight.Bold else null, modifier = Modifier.weight(1f))
                Text(label, color = NotionGray400, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text(stat.away, color = NotionWhite, fontSize = 13.sp, fontWeight = if (awayAhead) FontWeight.Bold else null, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            stat.homeShare?.let { share ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                    Box(modifier = Modifier.weight(share.coerceAtLeast(0.001f)).fillMaxHeight().background(HomeBar, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.weight((1f - share).coerceAtLeast(0.001f)).fillMaxHeight().background(AwayBar, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

// --- Gli altri sport ---

/** I punti per periodo (quarti, inning) e, nella NFL, le azioni che hanno fatto punti. */
@Composable
private fun ScoreByPeriod(detail: MatchDetail, match: SportMatch) {
    if (detail.periods.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        val columns = detail.periods + WidgetStrings.totalShort
        PeriodRow(label = { Spacer(modifier = Modifier.width(22.dp)) }, values = columns, header = true)
        HorizontalDivider()
        PeriodRow(label = { TeamLogo(match.home.logo, 18.dp) }, values = detail.homeLines + (match.home.score ?: ""), header = false)
        PeriodRow(label = { TeamLogo(match.away.logo, 18.dp) }, values = detail.awayLines + (match.away.score ?: ""), header = false)
    }
    if (detail.scoringPlays.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        detail.scoringPlays.asReversed().forEach { play ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(if (play.side == Side.AWAY) match.away.logo else match.home.logo, 20.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(play.type, color = NotionWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Q${play.period} ${play.clock} · ${play.text}", color = NotionGray400, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("${play.homeScore} - ${play.awayScore}", color = NotionWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (detail.periods.isEmpty() && detail.scoringPlays.isEmpty()) {
        Text(WidgetStrings.noEvents, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
    }
}

@Composable
private fun PeriodRow(label: @Composable () -> Unit, values: List<String>, header: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(30.dp)) { label() }
        values.forEachIndexed { i, value ->
            Text(
                value,
                color = if (header) NotionGray400 else NotionWhite,
                fontSize = if (header) 11.sp else 13.sp,
                // L'ultima colonna, il totale, in grassetto.
                fontWeight = if (!header && i == values.lastIndex) FontWeight.Bold else null,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Nel tennis: i due giocatori con la bandiera e i loro set. */
@Composable
private fun TennisSets(match: SportMatch) {
    listOf(match.home, match.away).forEach { side ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(side.logo, 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                side.name,
                color = NotionWhite,
                fontSize = 14.sp,
                fontWeight = if (side.winner) FontWeight.Bold else null,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            side.sets.forEach { games ->
                Text(games.toString(), color = NotionWhite, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
            }
        }
    }
}
