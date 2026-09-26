package com.gabriele.notionlocal.ui.screen

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.widgets.ClockSetting
import com.gabriele.notionlocal.data.widgets.CounterWidget
import com.gabriele.notionlocal.data.widgets.DEFAULT_BAR_COLOR
import com.gabriele.notionlocal.data.widgets.LifeProgressWidget
import com.gabriele.notionlocal.data.widgets.PomodoroPhase
import com.gabriele.notionlocal.data.widgets.PomodoroWidget
import com.gabriele.notionlocal.data.widgets.ProgressBarSetting
import com.gabriele.notionlocal.data.widgets.ProgressKind
import com.gabriele.notionlocal.data.widgets.TimeZonesWidget
import com.gabriele.notionlocal.data.widgets.Widget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.cityName
import com.gabriele.notionlocal.data.widgets.fractionAt
import com.gabriele.notionlocal.data.widgets.gmtLabel
import com.gabriele.notionlocal.data.widgets.msUntilTimerTicks
import com.gabriele.notionlocal.data.widgets.regionName
import com.gabriele.notionlocal.data.widgets.selectableZoneIds
import com.gabriele.notionlocal.data.widgets.sortedWestToEast
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * **I widget in cima alla barra laterale** (chiesti il 24/09/2026): fusi
 * orari, avanzamento, contatore, pomodoro. Si aggiungono dal pulsante in
 * fondo, si spostano e si tolgono dai tre puntini di ciascuno, e la
 * freccetta accanto al titolo nasconde o riapre tutta la sezione — una
 * scelta che resta (vedi `WidgetStore`).
 *
 * `active`: la barra è aperta. Solo allora gli orologi e le barre si
 * aggiornano ogni secondo; da chiusa la barra resta disegnata dietro alla
 * pagina (è fatta così), e ridisegnarla ogni secondo per niente
 * consumerebbe batteria. Il pomodoro invece conta e suona anche da chiusa:
 * lo fa `WidgetStore`, non questa schermata.
 */
@Composable
internal fun WidgetsSection(active: Boolean) {
    val state = WidgetStore.state
    val now by rememberNow(active && !state.hidden)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Strings.widgets,
            color = NotionGray400,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        // L'interruttore della sezione: una freccetta, su per chiudere e
        // giù per riaprire, come un gruppo che si ripiega.
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { WidgetStore.setHidden(!state.hidden) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state.hidden) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = if (state.hidden) WidgetStrings.showWidgets else WidgetStrings.hideWidgets,
                tint = NotionGray400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    if (state.hidden) {
        Spacer(modifier = Modifier.height(6.dp))
        return
    }

    state.widgets.forEachIndexed { index, widget ->
        WidgetCard(widget = widget, index = index, count = state.widgets.size, now = now, active = active)
    }
    AddWidgetButton()
}

/** L'ora di adesso, rinfrescata allo scoccare di ogni secondo finché `active`. */
@Composable
private fun rememberNow(active: Boolean) = produceState(System.currentTimeMillis(), active) {
    value = System.currentTimeMillis()
    while (active) {
        delay(1_000 - System.currentTimeMillis() % 1_000 + 5)
        value = System.currentTimeMillis()
    }
}

/**
 * L'ora per un pomodoro che corre, rinfrescata **quando cambia la cifra
 * del timer** e non allo scoccare dei secondi dell'orologio.
 *
 * Il timer parte quando si preme ▶, a metà di un secondo qualunque: con
 * l'ora di `rememberNow` il primo "00:59" arrivava al primo scatto
 * dell'orologio dopo un secondo intero di timer, fino a quasi due secondi
 * dopo ▶, e sembrava partire in ritardo (visto sul telefono il
 * 25/09/2026). Qui si dorme fino all'istante esatto in cui mancano dei
 * secondi interi — quando `timerText`, che arrotonda per eccesso, cambia —
 * e si riparte da capo appena il timer cambia (▶, pausa, fase nuova).
 */
@Composable
private fun rememberTimerNow(endsAt: Long?, active: Boolean) =
    produceState(System.currentTimeMillis(), endsAt, active) {
        value = System.currentTimeMillis()
        while (active && endsAt != null) {
            val left = endsAt - System.currentTimeMillis()
            if (left <= 0) break
            delay(msUntilTimerTicks(left) + 5)
            value = System.currentTimeMillis()
        }
    }

// --- La cornice di ogni widget ---

@Composable
private fun WidgetCard(widget: Widget, index: Int, count: Int, now: Long, active: Boolean) {
    var menuOpen by remember(widget.id) { mutableStateOf(false) }
    var confirmRemove by remember(widget.id) { mutableStateOf(false) }
    var renaming by remember(widget.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(start = 12.dp, end = 4.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = WidgetStrings.kindName(widget.kind()),
                color = NotionGray400,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MoreHoriz,
                        contentDescription = WidgetStrings.widgetOptions,
                        tint = NotionGray400,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(WidgetStrings.moveUp) },
                        enabled = index > 0,
                        onClick = { menuOpen = false; WidgetStore.move(widget.id, -1) }
                    )
                    DropdownMenuItem(
                        text = { Text(WidgetStrings.moveDown) },
                        enabled = index < count - 1,
                        onClick = { menuOpen = false; WidgetStore.move(widget.id, +1) }
                    )
                    if (widget is CounterWidget) {
                        DropdownMenuItem(
                            text = { Text(WidgetStrings.rename) },
                            onClick = { menuOpen = false; renaming = true }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(WidgetStrings.remove, color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; confirmRemove = true }
                    )
                }
            }
        }
        // Una colonna e non un Box: ogni widget mette in fila più righe
        // (orologi, elenco, barre), e un Box le disegnerebbe una sopra
        // l'altra — visto nel primo disegno di prova.
        Column(modifier = Modifier.padding(end = 8.dp)) {
            when (widget) {
                is TimeZonesWidget -> TimeZonesView(widget, now)
                is LifeProgressWidget -> LifeProgressView(widget, now)
                is CounterWidget -> CounterView(widget)
                is PomodoroWidget -> PomodoroView(widget, active)
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(WidgetStrings.removeWidgetTitle) },
            text = { Text(WidgetStrings.removeWidgetText) },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; WidgetStore.remove(widget.id) }) {
                    Text(WidgetStrings.remove, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(Strings.cancel) } }
        )
    }
    if (renaming && widget is CounterWidget) {
        TextInputDialog(
            title = WidgetStrings.rename,
            initial = widget.name,
            onDismiss = { renaming = false },
            onConfirm = { renaming = false; WidgetStore.renameCounter(widget.id, it) }
        )
    }
}

private fun Widget.kind(): WidgetKind = when (this) {
    is TimeZonesWidget -> WidgetKind.TIME_ZONES
    is LifeProgressWidget -> WidgetKind.LIFE_PROGRESS
    is CounterWidget -> WidgetKind.COUNTER
    is PomodoroWidget -> WidgetKind.POMODORO
}

@Composable
private fun AddWidgetButton() {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        AddRow(label = WidgetStrings.addWidget, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            WidgetKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(WidgetStrings.kindName(kind)) },
                    onClick = { open = false; WidgetStore.add(kind) }
                )
            }
        }
    }
}

/** Una riga "+ qualcosa": aggiungere un widget, un orologio, un fuso, una data. */
@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Una voce di menu con la spunta quando è la scelta attuale. */
@Composable
private fun CheckedMenuItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = if (checked) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        onClick = onClick
    )
}

// --- Fusi orari ---

/** Per quale scopo si sta scegliendo un fuso. */
private sealed interface ZonePick {
    data object NewClock : ZonePick
    data class Clock(val clockId: String) : ZonePick
    data object List : ZonePick
}

@Composable
private fun TimeZonesView(widget: TimeZonesWidget, now: Long) {
    var picking by remember(widget.id) { mutableStateOf<ZonePick?>(null) }
    val instant = Instant.ofEpochMilli(now)

    // Gli orologi, due per riga: la barra è larga 300 punti, e due
    // quadranti affiancati si leggono ancora bene.
    widget.clocks.chunked(2).forEach { pair ->
        Row(modifier = Modifier.fillMaxWidth()) {
            pair.forEach { clock ->
                ClockCell(
                    clock = clock,
                    instant = instant,
                    onSetAnalog = { analog -> WidgetStore.updateClock(widget.id, clock.id) { it.copy(analog = analog) } },
                    onChangeZone = { picking = ZonePick.Clock(clock.id) },
                    onRemove = { WidgetStore.removeClock(widget.id, clock.id) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
    }
    AddRow(label = WidgetStrings.addClock, onClick = { picking = ZonePick.NewClock })

    // L'elenco, sotto gli orologi: sempre da ovest a est, ricalcolato a
    // ogni secondo, perché l'ora legale sposta i fusi.
    if (widget.list.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
    sortedWestToEast(widget.list, instant).forEach { zoneId ->
        ZoneListRow(zoneId = zoneId, instant = instant, onRemove = { WidgetStore.removeFromZoneList(widget.id, zoneId) })
    }
    AddRow(label = WidgetStrings.addToList, onClick = { picking = ZonePick.List })

    picking?.let { target ->
        ZonePickerSheet(
            onDismiss = { picking = null },
            onPick = { zoneId ->
                picking = null
                when (target) {
                    ZonePick.NewClock -> WidgetStore.addClock(widget.id, zoneId)
                    is ZonePick.Clock -> WidgetStore.updateClock(widget.id, target.clockId) { it.copy(zoneId = zoneId) }
                    ZonePick.List -> WidgetStore.addToZoneList(widget.id, zoneId)
                }
            }
        )
    }
}

/** Il fuso di un orologio salvato; se l'id non esiste più (aggiornamenti del sistema), quello dell'app. */
private fun zoneOf(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(AppSettings.zoneId)

private val CLOCK_SIZE = 108.dp

/**
 * Un orologio: il nome della città sopra, e l'orologio — analogico, con
 * l'ora scritta dentro come nella foto dell'utente, o digitale. **Tenendolo
 * premuto** si sceglie fra i due, si cambia fuso o lo si toglie.
 */
@Composable
private fun ClockCell(
    clock: ClockSetting,
    instant: Instant,
    onSetAnalog: (Boolean) -> Unit,
    onChangeZone: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember(clock.id) { mutableStateOf(false) }
    val time = instant.atZone(zoneOf(clock.zoneId))
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(clock.id) { detectTapGestures(onLongPress = { menuOpen = true }) }
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = cityName(clock.zoneId),
                color = NotionWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (clock.analog) AnalogClock(time, CLOCK_SIZE) else DigitalClock(time, CLOCK_SIZE)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            CheckedMenuItem(WidgetStrings.analog, checked = clock.analog) { menuOpen = false; onSetAnalog(true) }
            CheckedMenuItem(WidgetStrings.digital, checked = !clock.analog) { menuOpen = false; onSetAnalog(false) }
            DropdownMenuItem(text = { Text(WidgetStrings.changeTimeZone) }, onClick = { menuOpen = false; onChangeZone() })
            DropdownMenuItem(
                text = { Text(WidgetStrings.remove, color = MaterialTheme.colorScheme.error) },
                onClick = { menuOpen = false; onRemove() }
            )
        }
    }
}

/** "4:58:04": l'ora in 24 ore, come tutte le ore dell'app. */
private val CLOCK_TIME = DateTimeFormatter.ofPattern("H:mm:ss")

/** "dom 20 set • PDT": il giorno nella lingua dell'app, e la sigla del fuso (quella inglese, l'unica che ce l'ha per tutti). */
private fun clockDateLine(time: ZonedDateTime): String {
    val day = DateTimeFormatter.ofPattern("EEE d MMM", AppSettings.language.locale).format(time)
    val abbreviation = DateTimeFormatter.ofPattern("zzz", Locale.US).format(time)
    return "$day • $abbreviation"
}

@Composable
private fun AnalogClock(time: ZonedDateTime, size: Dp) {
    val hands = NotionWhite
    val ticks = NotionGray400
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        // L'ora scritta sta sotto le lancette, nella metà bassa, come
        // nella foto: il quadrante resta leggibile e l'ora esatta c'è.
        Column(
            modifier = Modifier.offset(y = size * 0.2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(CLOCK_TIME.format(time), color = NotionWhite, fontSize = 12.sp)
            Text(clockDateLine(time), color = NotionGray400, fontSize = 8.sp, maxLines = 1)
        }
        Canvas(modifier = Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            fun at(angleDegrees: Double, length: Float): Offset {
                val radians = Math.toRadians(angleDegrees - 90)
                return center + Offset((cos(radians) * length).toFloat(), (sin(radians) * length).toFloat())
            }
            for (i in 0 until 12) {
                val main = i % 3 == 0
                drawLine(
                    color = ticks,
                    start = at(i * 30.0, radius * if (main) 0.8f else 0.87f),
                    end = at(i * 30.0, radius * 0.97f),
                    strokeWidth = (if (main) 2.dp else 1.5.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }
            val seconds = time.second.toDouble()
            val minutes = time.minute + seconds / 60
            val hours = time.hour % 12 + minutes / 60
            drawLine(hands, center, at(hours * 30, radius * 0.5f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(hands, center, at(minutes * 6, radius * 0.74f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ticks, center, at(seconds * 6, radius * 0.82f), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(hands, radius = 3.dp.toPx(), center = center)
        }
    }
}

/** L'orologio digitale: grande l'ora, sotto il giorno e il fuso. Alto quanto quello analogico, così le righe restano pari. */
@Composable
private fun DigitalClock(time: ZonedDateTime, height: Dp) {
    Column(
        modifier = Modifier.height(height),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(CLOCK_TIME.format(time), color = NotionWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Text(clockDateLine(time), color = NotionGray400, fontSize = 10.sp, maxLines = 1)
    }
}

/** Una riga dell'elenco dei fusi: la città, l'ora, il sole o la luna. Tenendola premuta si toglie. */
@Composable
private fun ZoneListRow(zoneId: String, instant: Instant, onRemove: () -> Unit) {
    var menuOpen by remember(zoneId) { mutableStateOf(false) }
    val time = instant.atZone(zoneOf(zoneId))
    val daytime = time.hour in 6..17
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(zoneId) { detectTapGestures(onLongPress = { menuOpen = true }) }
                .padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cityName(zoneId),
                color = NotionWhite,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(DateTimeFormatter.ofPattern("H:mm").format(time), color = NotionWhite, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (daytime) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                contentDescription = if (daytime) WidgetStrings.day else WidgetStrings.night,
                tint = if (daytime) Color(0xFFFFC53D) else Color(0xFFB8C4E6),
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(WidgetStrings.remove, color = MaterialTheme.colorScheme.error) },
                onClick = { menuOpen = false; onRemove() }
            )
        }
    }
}

/**
 * La scelta del fuso: tutti quelli con un nome di città, **da ovest a est**
 * come l'elenco, con la distanza da Greenwich di adesso; si cerca per
 * città, regione o "GMT+2".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZonePickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val all = remember {
        val now = Instant.now()
        sortedWestToEast(selectableZoneIds(), now).map { id -> id to gmtLabel(ZoneId.of(id), now) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                WidgetStrings.chooseTimeZone,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(WidgetStrings.searchCity, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it.replace("\n", "") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                        cursorBrush = SolidColor(NotionWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val wanted = query.trim()
            val shown = if (wanted.isEmpty()) all else all.filter { (id, gmt) ->
                id.replace('_', ' ').contains(wanted, ignoreCase = true) || gmt.contains(wanted, ignoreCase = true)
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(shown, key = { it.first }) { (id, gmt) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(cityName(id), color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
                        Text("${regionName(id)} · $gmt", color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// --- Avanzamento ---

@Composable
private fun LifeProgressView(widget: LifeProgressWidget, now: Long) {
    var editing by remember(widget.id) { mutableStateOf<ProgressBarSetting?>(null) }
    var adding by remember(widget.id) { mutableStateOf(false) }
    var coloring by remember(widget.id) { mutableStateOf<ProgressBarSetting?>(null) }
    val instant = Instant.ofEpochMilli(now)

    widget.bars.forEach { bar ->
        ProgressRow(
            bar = bar,
            fraction = bar.fractionAt(instant, WidgetStore.zone),
            onColor = { coloring = bar },
            onEdit = if (bar.kind == ProgressKind.CUSTOM) { { editing = bar } } else null,
            onDelete = if (bar.kind == ProgressKind.CUSTOM) { { WidgetStore.removeBar(widget.id, bar.id) } } else null
        )
    }
    AddRow(label = WidgetStrings.addDate, onClick = { adding = true })

    if (adding || editing != null) {
        val initial = editing
        DateBarDialog(
            initial = initial,
            onDismiss = { adding = false; editing = null },
            onConfirm = { name, start, end ->
                if (initial == null) {
                    WidgetStore.addBar(
                        widget.id,
                        ProgressBarSetting(kind = ProgressKind.CUSTOM, name = name, startEpochDay = start.toEpochDay(), endEpochDay = end.toEpochDay())
                    )
                } else {
                    WidgetStore.updateBar(widget.id, initial.id) {
                        it.copy(name = name, startEpochDay = start.toEpochDay(), endEpochDay = end.toEpochDay())
                    }
                }
                adding = false
                editing = null
            }
        )
    }
    coloring?.let { bar ->
        ColorPickerSheet(
            initialHex = bar.colorHex,
            background = false,
            onDismiss = { coloring = null },
            onSwitchTarget = {},
            onPick = { hex ->
                coloring = null
                WidgetStore.updateBar(widget.id, bar.id) { it.copy(colorHex = hex ?: DEFAULT_BAR_COLOR) }
            },
            showTargetSwitch = false
        )
    }
}

private fun ProgressBarSetting.label(): String = when (kind) {
    ProgressKind.YEAR -> WidgetStrings.year
    ProgressKind.MONTH -> WidgetStrings.month
    ProgressKind.WEEK -> WidgetStrings.week
    ProgressKind.DAY -> WidgetStrings.dayOfToday
    ProgressKind.CUSTOM -> name.ifBlank { Strings.untitled }
}

/**
 * Una barra: la cornice arrotondata, dentro il pieno quanto è passato, e
 * accanto "Anno: 73%" nello stesso colore, come nella foto dell'utente.
 * Tenendola premuta si cambia il colore (e, per le date dell'utente, le si
 * modifica o le si toglie).
 */
@Composable
private fun ProgressRow(
    bar: ProgressBarSetting,
    fraction: Double?,
    onColor: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    var menuOpen by remember(bar.id) { mutableStateOf(false) }
    val color = hexToColor(bar.colorHex) ?: hexToColor(DEFAULT_BAR_COLOR) ?: Color.Green
    val percent = fraction?.let { floor(it * 100).toInt() }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(bar.id) { detectTapGestures(onLongPress = { menuOpen = true }) }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .border(1.5.dp, color, RoundedCornerShape(7.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((fraction ?: 0.0).toFloat())
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${bar.label()}: ${percent?.let { "$it%" } ?: "—"}",
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Larghezza fissa: con l'etichetta che si allarga e si
                // stringe ("Anno" e "Settimana") le barre venivano lunghe
                // ognuna a modo suo, mentre nella foto sono tutte uguali.
                modifier = Modifier.width(PROGRESS_LABEL_WIDTH)
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text(WidgetStrings.barColor) }, onClick = { menuOpen = false; onColor() })
            if (onEdit != null) {
                DropdownMenuItem(text = { Text(WidgetStrings.editDate) }, onClick = { menuOpen = false; onEdit() })
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text(Strings.delete, color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

private val PROGRESS_LABEL_WIDTH = 112.dp

/** Una data dell'utente: nome, da quando e fino a quando. Si parte da oggi e da fra un mese. */
@Composable
private fun DateBarDialog(
    initial: ProgressBarSetting?,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, LocalDate) -> Unit
) {
    val today = LocalDate.now(WidgetStore.zone)
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var start by remember { mutableStateOf(initial?.startEpochDay?.let(LocalDate::ofEpochDay) ?: today) }
    var end by remember { mutableStateOf(initial?.endEpochDay?.let(LocalDate::ofEpochDay) ?: today.plusMonths(1)) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && end.isAfter(start)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) WidgetStrings.addDate else WidgetStrings.editDate) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace("\n", "") },
                    singleLine = true,
                    label = { Text(WidgetStrings.name) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                DateField(WidgetStrings.from, start) { pickingStart = true }
                DateField(WidgetStrings.to, end) { pickingEnd = true }
                if (!end.isAfter(start)) {
                    Text(WidgetStrings.endBeforeStart, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(name.trim(), start, end) }) { Text(Strings.done) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
    if (pickingStart) DayPickerDialog(initial = start, onPick = { start = it }, onDismiss = { pickingStart = false })
    if (pickingEnd) DayPickerDialog(initial = end, onPick = { end = it }, onDismiss = { pickingEnd = false })
}

@Composable
private fun DateField(label: String, day: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NotionGray400, modifier = Modifier.width(56.dp))
        Text(Formats.date(day), color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- Contatore ---

@Composable
private fun CounterView(widget: CounterWidget) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (widget.name.isNotBlank()) {
            Text(widget.name, color = NotionWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SquareButton(Icons.Filled.Remove, WidgetStrings.decrease) { WidgetStore.changeCounter(widget.id, -1) }
            Text(
                text = Formats.number(widget.value.toString()),
                color = NotionWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 72.dp).padding(horizontal = 8.dp)
            )
            SquareButton(Icons.Filled.Add, WidgetStrings.increase) { WidgetStore.changeCounter(widget.id, +1) }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // La freccetta che gira: rimette a zero.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable { WidgetStore.resetCounter(widget.id) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = WidgetStrings.reset, tint = NotionGray400, modifier = Modifier.size(20.dp))
        }
    }
}

/** Il pulsante quadrato arrotondato del contatore, come nella foto. */
@Composable
private fun SquareButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = NotionWhite, modifier = Modifier.size(22.dp))
    }
}

// --- Pomodoro ---

private val SessionAccent = Color(0xFFE8826E)
private val BreakAccent = Color(0xFF7FC49A)

/**
 * Il pomodoro, nel riquadro arrotondato della foto: il tempo grande (lo si
 * tocca per scriverlo a mano), il nome della fase (lo si tocca per
 * rinominare la sessione), e i tre pulsanti — avvia/pausa, da capo,
 * ingranaggio per le durate di sessione e pausa.
 */
@Composable
private fun PomodoroView(widget: PomodoroWidget, active: Boolean) {
    var settingTime by remember(widget.id) { mutableStateOf(false) }
    var renaming by remember(widget.id) { mutableStateOf(false) }
    var settings by remember(widget.id) { mutableStateOf(false) }
    val accent = if (widget.phase == PomodoroPhase.SESSION) SessionAccent else BreakAccent
    val now by rememberTimerNow(widget.endsAt.takeIf { widget.running }, active)
    val remaining = widget.remainingAt(now)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .clickable { settingTime = true }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timerText(remaining),
                color = NotionWhite,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.22f))
                    .clickable(enabled = widget.phase == PomodoroPhase.SESSION) { renaming = true }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (widget.phase == PomodoroPhase.SESSION) widget.sessionName.ifBlank { WidgetStrings.session } else WidgetStrings.breakTime,
                    color = NotionWhite,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            RoundButton(
                icon = if (widget.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = if (widget.running) WidgetStrings.pause else WidgetStrings.start,
                background = accent
            ) {
                if (widget.running) WidgetStore.pomodoroPause(widget.id) else WidgetStore.pomodoroStart(widget.id)
            }
            Spacer(modifier = Modifier.width(6.dp))
            RoundButton(Icons.Filled.Refresh, WidgetStrings.reset, accent) { WidgetStore.pomodoroReset(widget.id) }
            Spacer(modifier = Modifier.width(6.dp))
            RoundButton(Icons.Filled.Settings, WidgetStrings.timerSettings, accent) { settings = true }
        }
    }

    if (settingTime) {
        TimeInputDialog(
            initialMs = remaining,
            onDismiss = { settingTime = false },
            onConfirm = { ms -> settingTime = false; WidgetStore.pomodoroSetRemaining(widget.id, ms) }
        )
    }
    if (renaming) {
        TextInputDialog(
            title = WidgetStrings.sessionName,
            initial = widget.sessionName,
            onDismiss = { renaming = false },
            onConfirm = { renaming = false; WidgetStore.pomodoroRename(widget.id, it) }
        )
    }
    if (settings) {
        PomodoroSettingsDialog(
            sessionMinutes = widget.sessionMinutes,
            breakMinutes = widget.breakMinutes,
            soundUri = widget.soundUri,
            soundName = widget.soundName,
            onDismiss = { settings = false },
            onConfirm = { s, b, uri, name ->
                settings = false
                WidgetStore.pomodoroSetLengths(widget.id, s, b)
                WidgetStore.pomodoroSetSound(widget.id, uri, name)
            }
        )
    }
}

/** "25:00", e "1:30:00" oltre l'ora. */
private fun timerText(ms: Long): String {
    val totalSeconds = (ms + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, background: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Color(0xFF2B1B17), modifier = Modifier.size(22.dp))
    }
}

/** Il tempo scritto a mano: minuti e secondi, con la tastiera dei numeri. */
@Composable
private fun TimeInputDialog(initialMs: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val totalSeconds = (initialMs + 999) / 1000
    var minutes by remember { mutableStateOf((totalSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((totalSeconds % 60).toString()) }
    val m = minutes.toIntOrNull()
    val s = seconds.toIntOrNull()
    val valid = m != null && s != null && m in 0..WidgetStore.MAX_POMODORO_MINUTES && s in 0..59 && (m > 0 || s > 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WidgetStrings.setTime) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberField(minutes, WidgetStrings.minutes, Modifier.weight(1f)) { minutes = it }
                Text(" : ", fontSize = 24.sp)
                NumberField(seconds, WidgetStrings.seconds, Modifier.weight(1f)) { seconds = it }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm((m!! * 60L + s!!) * 1000L) }) { Text(Strings.done) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
private fun NumberField(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

/**
 * L'ingranaggio: quanto dura la sessione di studio e quanto la pausa, coi
 * pulsanti − e +, e **il suono di fine fase** (dal 25/09/2026), scelto
 * nell'elenco delle suonerie del telefono — la schermata di sistema, con
 * l'anteprima di ogni suono. Come le durate, vale solo con "Fatto".
 */
@Composable
private fun PomodoroSettingsDialog(
    sessionMinutes: Int,
    breakMinutes: Int,
    soundUri: String?,
    soundName: String?,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var session by remember { mutableStateOf(sessionMinutes) }
    var pause by remember { mutableStateOf(breakMinutes) }
    var sound by remember { mutableStateOf(soundUri) }
    var soundLabel by remember { mutableStateOf(soundName) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val picked = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } ?: return@rememberLauncherForActivityResult
        sound = picked.toString()
        soundLabel = runCatching { RingtoneManager.getRingtone(context, picked)?.getTitle(context) }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WidgetStrings.timerSettings) },
        text = {
            Column {
                LengthStepper(WidgetStrings.sessionLength, session) { session = it }
                Spacer(modifier = Modifier.height(12.dp))
                LengthStepper(WidgetStrings.breakLength, pause) { pause = it }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            picker.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, WidgetStrings.sound)
                                    // "Predefinito" e "Nessuno" dell'elenco di
                                    // sistema non servono: il predefinito
                                    // dell'app è la voce qui sotto, e un
                                    // pomodoro muto non l'ha chiesto nessuno.
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sound?.let(Uri::parse))
                            )
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(WidgetStrings.sound, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        text = if (sound == null) Strings.defaultSound else soundLabel ?: WidgetStrings.sound,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
                if (sound != null) {
                    TextButton(onClick = { sound = null; soundLabel = null }) { Text(Strings.useDefaultSound) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(session, pause, sound, soundLabel) }) { Text(Strings.done) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
private fun LengthStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SquareButton(Icons.Filled.Remove, WidgetStrings.decrease) { onChange((value - 1).coerceAtLeast(1)) }
            Text(
                text = "$value ${WidgetStrings.minutesShort}",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 88.dp)
            )
            SquareButton(Icons.Filled.Add, WidgetStrings.increase) {
                onChange((value + 1).coerceAtMost(WidgetStore.MAX_POMODORO_MINUTES))
            }
        }
    }
}

/** Una riga di testo da scrivere: il nome del contatore, della sessione. */
@Composable
private fun TextInputDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.replace("\n", "") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.trim()) }) { Text(Strings.done) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
