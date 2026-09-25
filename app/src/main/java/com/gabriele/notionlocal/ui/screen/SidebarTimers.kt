package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.widgets.CountdownTimer
import com.gabriele.notionlocal.data.widgets.StopwatchWidget
import com.gabriele.notionlocal.data.widgets.TimerWidget
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.countdownText
import com.gabriele.notionlocal.data.widgets.durationLabel
import com.gabriele.notionlocal.data.widgets.fastestAndSlowestLap
import com.gabriele.notionlocal.data.widgets.lapRows
import com.gabriele.notionlocal.data.widgets.stopwatchText
import com.gabriele.notionlocal.data.widgets.wheelDurationMs
import com.gabriele.notionlocal.data.widgets.wheelValues
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Cronometro e timer della barra laterale (dal 25/09/2026), chiesti
 * dall'utente "uguali a quelli di Samsung" con le foto dell'orologio del
 * S25 Ultra: stessi colori (le cifre color pesca, Start e Resume arancio,
 * Stop rosso scuro), stesso quadrante a tacche, stessa lista dei giri —
 * ma in piccolo, larghi quanto la barra.
 */

private val Peach = Color(0xFFF2C6A5)
private val SamsungOrange = Color(0xFFE39A62)
private val StopRed = Color(0xFFE5646E)
private val StopBackground = Color(0xFF3B1E22)
private val GrayButton = Color(0xFF3A3A3A)
private val DialFace = Color(0xFF232323)
private val OverallHand = Color(0xFF5B5BEA)
private val FastestLap = Color(0xFFE8A873)

private val DIAL_SIZE = 176.dp
private val ACTION_BUTTON_SIZE = 64.dp

// --- Cronometro ---

/**
 * Il cronometro: il quadrante col tempo e due lancette — la blu per i
 * secondi del tempo totale, la bianca per quelli del giro in corso (dopo il
 * primo Lap), come su Samsung — poi i due pulsanti e la lista dei giri.
 *
 * I pulsanti, a sinistra e a destra: **Lap / Start** all'inizio (Lap
 * spento), **Lap / Stop** mentre corre, **Reset / Resume** da fermo.
 */
@Composable
internal fun StopwatchView(widget: StopwatchWidget, active: Boolean) {
    val now by rememberFrameNow(widget.running && active)
    val elapsed = widget.elapsedAt(now)
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.size(DIAL_SIZE), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(DIAL_SIZE)) {
                drawDial(activeTicks = null)
                // La lancetta bianca del giro, sotto la blu del totale.
                widget.laps.lastOrNull()?.let { lastLap ->
                    drawHand(fraction = ((elapsed - lastLap) % 60_000) / 60_000f, color = NotionWhite.copy(alpha = 0.85f), width = 1.5.dp.toPx())
                }
                drawHand(fraction = (elapsed % 60_000) / 60_000f, color = OverallHand, width = 2.5.dp.toPx())
                drawCircle(OverallHand, radius = 4.dp.toPx())
            }
            Text(
                text = stopwatchText(elapsed),
                color = Peach,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.offset(y = (-20).dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (widget.running || widget.idle) {
                ActionCircle(WidgetStrings.lap, GrayButton, enabled = widget.running) { WidgetStore.stopwatchLap(widget.id) }
            } else {
                ActionCircle(WidgetStrings.reset, GrayButton) { WidgetStore.stopwatchReset(widget.id) }
            }
            when {
                widget.running -> ActionCircle(WidgetStrings.stop, StopBackground, textColor = StopRed) { WidgetStore.stopwatchStop(widget.id) }
                widget.idle -> ActionCircle(WidgetStrings.start, SamsungOrange) { WidgetStore.stopwatchStart(widget.id) }
                else -> ActionCircle(WidgetStrings.resume, SamsungOrange) { WidgetStore.stopwatchStart(widget.id) }
            }
        }
        if (widget.laps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LapTable(widget.laps)
        }
    }
}

/** L'ora rinfrescata a ogni fotogramma finché `active`: i centesimi del cronometro. */
@Composable
private fun rememberFrameNow(active: Boolean) = produceState(System.currentTimeMillis(), active) {
    value = System.currentTimeMillis()
    while (active) {
        withFrameMillis { }
        value = System.currentTimeMillis()
    }
}

/** La lista dei giri come su Samsung: Lap, Lap times, Overall time, dal più recente; il più veloce arancio, il più lento rosso. */
@Composable
private fun LapTable(laps: List<Long>) {
    val rows = lapRows(laps)
    val highlight = fastestAndSlowestLap(rows)
    Column(modifier = Modifier.fillMaxWidth()) {
        LapLine(WidgetStrings.lap, WidgetStrings.lapTimes, WidgetStrings.overallTime, NotionGray400, 11.sp)
        HorizontalDivider(color = NotionGray400.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
        rows.forEach { row ->
            val color = when (row.number) {
                highlight?.first -> FastestLap
                highlight?.second -> StopRed
                else -> NotionWhite.copy(alpha = 0.85f)
            }
            LapLine("%02d".format(row.number), stopwatchText(row.lapMs), stopwatchText(row.overallMs), color, 13.sp)
        }
    }
}

@Composable
private fun LapLine(a: String, b: String, c: String, color: Color, size: TextUnit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        listOf(a, b, c).forEach { text ->
            Text(text, color = color, fontSize = size, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
        }
    }
}

// --- Timer ---

/**
 * Il timer: una pagina per ogni conto alla rovescia, sfogliate in
 * orizzontale coi pallini sotto. In alto a destra, quando il timer della
 * pagina è partito, **↻** lo riporta alle rotelle e **+** ne aggiunge un
 * altro, che parte insieme a quelli che corrono già: la pagina nuova si
 * apre da sola, con Cancel e Start come l'"Add timer" di Samsung.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimerView(widget: TimerWidget, active: Boolean) {
    val pagerState = rememberPagerState(pageCount = { widget.timers.size })
    var knownCount by remember(widget.id) { mutableStateOf(widget.timers.size) }
    LaunchedEffect(widget.timers.size) {
        if (widget.timers.size > knownCount) pagerState.animateScrollToPage(widget.timers.size - 1)
        knownCount = widget.timers.size
    }
    val current = widget.timers.getOrNull(pagerState.currentPage)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Sempre alta uguale, anche senza icone: il quadrante non salta
        // su e giù passando da una pagina all'altra.
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (current?.started == true) {
                TopIcon(Icons.Filled.Refresh, WidgetStrings.resetTimer) { WidgetStore.timerReset(widget.id, current.id) }
                TopIcon(Icons.Filled.Add, WidgetStrings.addTimer) { WidgetStore.timerAdd(widget.id) }
            }
        }
        HorizontalPager(
            state = pagerState,
            key = { page -> widget.timers.getOrNull(page)?.id ?: page },
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val timer = widget.timers.getOrNull(page) ?: return@HorizontalPager
            if (timer.started) {
                RunningTimerPage(widget.id, timer, active)
            } else {
                TimerSetupPage(widget.id, timer, canCancel = widget.timers.size > 1)
            }
        }
        if (widget.timers.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(widget.timers.size) { i ->
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

/** Le tre rotelle e Start. Con più timer, anche Cancel: toglie questo. */
@Composable
private fun TimerSetupPage(widgetId: String, timer: CountdownTimer, canCancel: Boolean) {
    val (h, m, s) = wheelValues(timer.durationMs)
    fun set(hours: Int, minutes: Int, seconds: Int) =
        WidgetStore.timerSetDuration(widgetId, timer.id, wheelDurationMs(hours, minutes, seconds))

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(WidgetStrings.hours, WidgetStrings.minutes, WidgetStrings.seconds).forEach { label ->
                Text(label, color = NotionGray400, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NumberWheel(range = 100, value = h, onValueChange = { set(it, m, s) }, modifier = Modifier.weight(1f))
            WheelColon()
            NumberWheel(range = 60, value = m, onValueChange = { set(h, it, s) }, modifier = Modifier.weight(1f))
            WheelColon()
            NumberWheel(range = 60, value = s, onValueChange = { set(h, m, it) }, modifier = Modifier.weight(1f))
        }
        // Niente tempi pronti (i 10, 15 e 30 minuti di Samsung): tolti su
        // richiesta dell'utente il 25/09/2026, il tempo si sceglie solo
        // con le rotelle.
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (canCancel) ActionCircle(Strings.cancel, GrayButton) { WidgetStore.timerRemove(widgetId, timer.id) }
            ActionCircle(WidgetStrings.start, SamsungOrange, enabled = timer.durationMs > 0) {
                WidgetStore.timerStart(widgetId, timer.id)
            }
        }
    }
}

@Composable
private fun WheelColon() {
    Text(":", color = Peach, fontSize = 24.sp, fontWeight = FontWeight.Bold)
}

private val WHEEL_ITEM_HEIGHT = 38.dp

/** Quante volte si ripete la rotella per sembrare senza fine, come su Samsung (dopo 99 c'è 00). */
private const val WHEEL_REPEATS = 1_000

/**
 * Una rotella da far scorrere in verticale: tre numeri visibili, quello in
 * mezzo è quello scelto (grande, color pesca), e ci si ferma sempre su un
 * numero intero. Gira senza fine: sopra 00 c'è l'ultimo numero.
 *
 * Il valore si comunica **quando la rotella si ferma**, non a ogni numero
 * che passa; e se il valore cambia da fuori (un timer riportato alle
 * rotelle, un'altra pagina) la rotella ci va da sola.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(range: Int, value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val total = range * WHEEL_REPEATS
    val middle = (WHEEL_REPEATS / 2) * range
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = middle + value - 1)
    val itemPx = with(LocalDensity.current) { WHEEL_ITEM_HEIGHT.toPx() }
    // L'elemento al centro dei tre visibili.
    val centerIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + 1 + if (listState.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val picked = centerIndex % range
            if (picked != value) onValueChange(picked)
        }
    }
    LaunchedEffect(value) {
        if (!listState.isScrollInProgress && centerIndex % range != value) {
            val base = centerIndex - centerIndex % range
            listState.scrollToItem(base + value - 1)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WHEEL_ITEM_HEIGHT * 3)
    ) {
        items(total) { index ->
            val selected = index == centerIndex
            Box(modifier = Modifier.height(WHEEL_ITEM_HEIGHT), contentAlignment = Alignment.Center) {
                Text(
                    text = "%02d".format(index % range),
                    color = if (selected) Peach else NotionGray400.copy(alpha = 0.35f),
                    fontSize = if (selected) 28.sp else 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Un timer partito: il quadrante con le tacche arancio per il tempo che
 * resta, dentro il tempo impostato ("10 m"), quello che manca ("9:55") e
 * la campanella con l'ora in cui finirà. Sotto, Pausa o Riprendi; finito,
 * 0:00 in rosso e Reset per tornare alle rotelle.
 */
@Composable
private fun RunningTimerPage(widgetId: String, timer: CountdownTimer, active: Boolean) {
    val now by rememberTimerNow(timer.endsAt.takeIf { timer.running }, active)
    val remaining = timer.remainingAt(now)
    val fraction = if (timer.durationMs > 0) remaining.toFloat() / timer.durationMs else 0f

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(DIAL_SIZE), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(DIAL_SIZE)) { drawDial(activeTicks = fraction) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(durationLabel(timer.durationMs), color = Peach, fontSize = 12.sp)
                Text(
                    text = countdownText(remaining),
                    color = if (timer.finished) StopRed else Peach,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                val end = timer.endsAt
                if (timer.running && end != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = WidgetStrings.timerEndsAt,
                            tint = NotionWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            END_TIME.format(Instant.ofEpochMilli(end).atZone(WidgetStore.zone)),
                            color = NotionWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        when {
            timer.finished -> ActionCircle(WidgetStrings.reset, GrayButton) { WidgetStore.timerReset(widgetId, timer.id) }
            timer.running -> ActionCircle(WidgetStrings.pause, GrayButton) { WidgetStore.timerPause(widgetId, timer.id) }
            else -> ActionCircle(WidgetStrings.resume, SamsungOrange) { WidgetStore.timerResume(widgetId, timer.id) }
        }
    }
}

private val END_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// --- Pezzi comuni ---

/** Un pulsante tondo con la scritta dentro, come Start, Stop e Lap di Samsung. */
@Composable
private fun ActionCircle(
    label: String,
    background: Color,
    textColor: Color = NotionWhite,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(ACTION_BUTTON_SIZE)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun TopIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Peach, modifier = Modifier.size(22.dp))
    }
}

/**
 * Il quadrante: sessanta tacche in cerchio attorno a un disco scuro. Per il
 * timer `activeTicks` è la parte di tempo che resta: quelle tacche sono
 * arancio, le altre — il tempo già passato, da mezzogiorno in senso orario
 * — spente. Per il cronometro null: tacche grigie, più chiare ogni cinque.
 */
private fun DrawScope.drawDial(activeTicks: Float?) {
    val radius = size.minDimension / 2
    val tickOuter = radius - 1.dp.toPx()
    val tickInner = radius - 8.dp.toPx()
    val gone = activeTicks?.let { ((1f - it) * 60).toInt() } ?: 0
    for (i in 0 until 60) {
        val angle = (i / 60f) * 2 * PI - PI / 2
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        val color = when {
            activeTicks != null -> if (i >= gone) SamsungOrange else NotionGray400.copy(alpha = 0.25f)
            i % 5 == 0 -> NotionWhite.copy(alpha = 0.8f)
            else -> NotionGray400.copy(alpha = 0.5f)
        }
        drawLine(
            color = color,
            start = center + direction * tickInner,
            end = center + direction * tickOuter,
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    val face = radius - 14.dp.toPx()
    drawCircle(DialFace, radius = face)
    drawCircle(NotionGray400.copy(alpha = 0.25f), radius = face, style = Stroke(width = 1.dp.toPx()))
}

/** Una lancetta dal centro fino al bordo del disco, girata di `fraction` di giro da mezzogiorno. */
private fun DrawScope.drawHand(fraction: Float, color: Color, width: Float) {
    val angle = fraction * 2 * PI - PI / 2
    val length = size.minDimension / 2 - 10.dp.toPx()
    drawLine(
        color = color,
        start = center,
        end = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * length,
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}
