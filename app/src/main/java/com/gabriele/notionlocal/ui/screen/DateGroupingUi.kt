package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.DateGroupBy
import com.gabriele.notionlocal.data.entity.DateUnit
import com.gabriele.notionlocal.ui.i18n.DateGroupStrings

/**
 * **"Date by"** nelle impostazioni di un raggruppamento per data (dal
 * 21/10/2026): gruppi per giorno, settimana, mese o anno, e "Newest first"
 * per cominciare dai più recenti. Compare sotto l'elenco delle proprietà
 * solo quando quella scelta è una data.
 */
@Composable
internal fun DateGroupBySection(by: DateGroupBy, enabled: Boolean = true, onChange: (DateGroupBy) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            DateGroupStrings.dateBy,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        SheetGroup {
            DateUnit.entries.forEachIndexed { index, unit ->
                if (index > 0) HorizontalDivider()
                SettingsRow(
                    icon = when (unit) {
                        DateUnit.DAY -> Icons.Filled.CalendarToday
                        DateUnit.WEEK -> Icons.Filled.CalendarViewWeek
                        DateUnit.MONTH -> Icons.Filled.CalendarMonth
                        DateUnit.YEAR -> Icons.Filled.Event
                    },
                    label = DateGroupStrings.unitName(unit),
                    enabled = enabled,
                    value = if (unit == by.unit) "✓" else null,
                    onClick = { onChange(by.copy(unit = unit)) }
                )
            }
            HorizontalDivider()
            SettingsToggleRow(
                icon = Icons.Filled.SwapVert,
                label = DateGroupStrings.newestFirst,
                checked = by.newestFirst,
                enabled = enabled,
                onCheckedChange = { onChange(by.copy(newestFirst = it)) }
            )
        }
    }
}
