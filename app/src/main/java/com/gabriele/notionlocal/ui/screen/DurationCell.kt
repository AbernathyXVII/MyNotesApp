package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.entity.durationBetween
import com.gabriele.notionlocal.data.entity.durationOfCell
import com.gabriele.notionlocal.data.entity.durationUnits
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Quanto dura una cella "Duration", a parole e fino all'unità scelta nelle
 * impostazioni della proprietà ("30 years, 7 months and 17 days"). Null se
 * manca l'inizio o la fine.
 */
internal fun durationLabel(value: String, format: DurationUnit): String? =
    durationOfCell(value, AppSettings.zoneId)?.let { DbStrings.durationText(durationUnits(it, format)) }

/**
 * **La cella "Duration"** (dal 29/09/2026): quanto passa fra un inizio e una
 * fine scelti a mano. Toccandola si scelgono data e ora dell'uno e
 * dell'altra (`DurationEditorSheet`).
 */
@Composable
internal fun DurationCell(
    value: String,
    onValueChange: (String) -> Unit,
    /** Il nome della proprietà: il titolo della finestra. */
    title: String,
    format: DurationUnit,
    contentPadding: Dp = 0.dp
) {
    val locked = LocalDatabaseLocked.current
    var editing by remember { mutableStateOf(false) }
    // Tutta la cella si tocca, come quella della data: il tocco sul trattino
    // di una cella vuota non deve finire sulla pagina sotto.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(enabled = !locked) { editing = true }
            .padding(horizontal = contentPadding),
        contentAlignment = LocalCellAlignment.current.box
    ) {
        Text(
            text = durationLabel(value, format) ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (editing) {
        DurationEditorSheet(
            title = title,
            value = value,
            format = format,
            onConfirm = {
                onValueChange(it)
                editing = false
            },
            onDismiss = { editing = false }
        )
    }
}

/** Quale data o quale ora si sta scegliendo nella finestra della durata. */
private enum class DurationPick { START_DATE, START_TIME, END_DATE, END_TIME }

/**
 * La finestra della durata: in cima quanto dura, sotto la data e l'ora
 * dell'inizio e della fine, ognuna col suo selettore (il calendario, con le
 * rotelle dell'anno toccando il mese, e l'orologio). L'ora parte da 00:00.
 * "Done" salva solo con tutte e due le date, e la fine dopo l'inizio;
 * "Clear" svuota la cella.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationEditorSheet(
    title: String,
    value: String,
    format: DurationUnit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val range = remember(value) { parseDateRange(value) }
    fun local(millis: Long?) = millis?.let { Instant.ofEpochMilli(it).atZone(AppSettings.zoneId).toLocalDateTime() }
    var startDate by remember { mutableStateOf(local(range?.start)?.toLocalDate()) }
    var startTime by remember { mutableStateOf(local(range?.start)?.toLocalTime() ?: LocalTime.MIDNIGHT) }
    var endDate by remember { mutableStateOf(local(range?.end)?.toLocalDate()) }
    var endTime by remember { mutableStateOf(local(range?.end)?.toLocalTime() ?: LocalTime.MIDNIGHT) }
    var picking by remember { mutableStateOf<DurationPick?>(null) }

    val start = startDate?.let { LocalDateTime.of(it, startTime) }
    val end = endDate?.let { LocalDateTime.of(it, endTime) }
    val parts = if (start != null && end != null) durationBetween(start, end) else null
    val canSave = parts != null || (startDate == null && endDate == null)

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            TextButton(
                enabled = canSave,
                onClick = {
                    if (start == null || end == null) {
                        onConfirm("")
                    } else {
                        onConfirm(
                            encodeDateRange(
                                start = start.atZone(AppSettings.zoneId).toInstant().toEpochMilli(),
                                end = end.atZone(AppSettings.zoneId).toInstant().toEpochMilli(),
                                hasTime = true
                            )
                        )
                    }
                }
            ) { Text(Strings.done) }
        }
        HorizontalDivider()
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            // Quanto dura, come lo scriverà la cella; o cosa manca.
            Text(
                text = when {
                    parts != null -> DbStrings.durationText(durationUnits(parts, format))
                    start != null && end != null -> DbStrings.durationEndBeforeStart
                    else -> DbStrings.durationChooseBoth
                },
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    parts != null -> MaterialTheme.colorScheme.onBackground
                    start != null && end != null -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
            )
            HorizontalDivider()
            DurationPickRow(DbStrings.startDate, startDate?.format(DURATION_DATE_FORMAT)) { picking = DurationPick.START_DATE }
            DurationPickRow(DbStrings.startTime, startTime.format(DURATION_TIME_FORMAT)) { picking = DurationPick.START_TIME }
            HorizontalDivider()
            DurationPickRow(DbStrings.endDate, endDate?.format(DURATION_DATE_FORMAT)) { picking = DurationPick.END_DATE }
            DurationPickRow(DbStrings.endTime, endTime.format(DURATION_TIME_FORMAT)) { picking = DurationPick.END_TIME }
            HorizontalDivider()
            Text(
                text = DbStrings.clear,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConfirm("") }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    when (picking) {
        DurationPick.START_DATE, DurationPick.END_DATE -> {
            val forStart = picking == DurationPick.START_DATE
            SamsungDateDialog(
                // Una data non ancora scelta parte da oggi: il più delle volte
                // una delle due è oggi, e l'anno si cambia con le rotelle.
                initial = (if (forStart) startDate else endDate) ?: LocalDate.now(AppSettings.zoneId),
                onPick = { if (forStart) startDate = it else endDate = it },
                onDismiss = { picking = null }
            )
        }
        DurationPick.START_TIME, DurationPick.END_TIME -> {
            val forStart = picking == DurationPick.START_TIME
            SamsungTimeDialog(
                initial = if (forStart) startTime else endTime,
                onPick = { if (forStart) startTime = it else endTime = it },
                onDismiss = { picking = null }
            )
        }
        null -> Unit
    }
}

/** "Start date   12 Feb 1996": una riga della finestra, col valore in azzurro (o il trattino). */
@Composable
private fun DurationPickRow(label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
    }
}

/** Le date nello stile scelto nelle impostazioni dell'app. */
private val DURATION_DATE_FORMAT: DateTimeFormatter get() = Formats.dateFormatter()
private val DURATION_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
