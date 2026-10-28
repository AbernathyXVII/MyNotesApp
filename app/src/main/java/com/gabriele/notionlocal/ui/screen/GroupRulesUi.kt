package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DateGroupBy
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.canGroup
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings

// ---------------------------------------------------------------------------
// **I raggruppamenti, quanti se ne vogliono** (dal 28/10/2026, chiesto
// dall'utente: "la stessa cosa [dei Sort] con il Group: appena ne creo uno,
// dentro Group non solo la lista delle regole del grouping, ma anche la
// possibilità di farne altre"). Dentro "Group" (della tabella) o "Group by"
// (della bacheca): in cima "Add a group", sotto le regole numerate
// nell'ordine in cui contano. Toccandone una, la sua proprietà, "Hide empty
// groups", "Date by" e "Remove group". "Sub groups" non c'è più: i gruppi
// dentro i gruppi sono le regole dopo la prima.
// ---------------------------------------------------------------------------

internal const val GROUP_RULE_ROW_TAG = "groupRuleRow"

/** Cosa si fa delle regole: tutte ricevono se sono quelle della bacheca. */
internal class GroupRuleActions(
    val add: (Boolean, String) -> Unit,
    /** Cambia la proprietà di una regola: la vecchia, la nuova. */
    val setColumn: (Boolean, String, String) -> Unit,
    val move: (Boolean, String, Int) -> Unit,
    val remove: (Boolean, String) -> Unit,
    val removeAll: (Boolean) -> Unit,
    val setHideEmpty: (Boolean, String, Boolean) -> Unit,
    val setDateBy: (Boolean, String, DateGroupBy) -> Unit
)

/** Il valore della voce "Group" nelle impostazioni: i nomi delle proprietà, nell'ordine ("Stato, Genere"). */
internal fun groupRulesSummary(rules: List<GroupRuleSpec>, columns: List<DatabaseColumnEntity>): String =
    rules.mapNotNull { rule -> columns.find { it.id == rule.column }?.name?.let(DbStrings::columnName) }
        .joinToString(", ")
        .ifEmpty { DbStrings.none }

/** Le proprietà con cui si può raggruppare (vedi `canGroup`). */
private fun groupable(columns: List<DatabaseColumnEntity>) = columns.filter { it.type.canGroup }

@Composable
private fun GroupNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

/**
 * **L'elenco dei raggruppamenti**: "Add a group" in alto (aggiungerne tanti
 * non vuol dire scendere in fondo), poi le regole numerate. Toccandone una
 * si apre la sua pagina; il menu ⋮ la sposta su o giù o la toglie.
 */
@Composable
internal fun GroupRulesPage(
    board: Boolean,
    rules: List<GroupRuleSpec>,
    columns: List<DatabaseColumnEntity>,
    enabled: Boolean,
    actions: GroupRuleActions?,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit
) {
    val canGroupBy = groupable(columns)
    val used = rules.mapTo(HashSet()) { it.column }
    GroupNote(if (board) DbStrings.boardGroupRulesNote else DbStrings.groupRulesNote)
    SheetGroup {
        SettingsRow(
            icon = Icons.Filled.Add,
            label = DbStrings.addGroup,
            value = "",
            onClick = onAdd,
            enabled = enabled && canGroupBy.any { it.id !in used }
        )
    }
    Spacer(modifier = Modifier.size(12.dp))
    when {
        canGroupBy.isEmpty() -> GroupNote(DbStrings.groupingNeedsProperty)
        rules.isEmpty() -> GroupNote(DbStrings.noGroups)
        else -> {
            SheetGroup {
                rules.forEachIndexed { index, rule ->
                    if (index > 0) HorizontalDivider()
                    key(rule.column) {
                        GroupRuleRow(board, index, rule, rules.size, columns, enabled, actions, onOpen)
                    }
                }
            }
            // Nella bacheca senza regole le colonne restano (la prima proprietà a
            // selezione singola): "Remove all groups" toglie solo le fasce e la scelta.
            Spacer(modifier = Modifier.size(16.dp))
            SheetGroup {
                SettingsRow(
                    icon = Icons.Filled.Delete,
                    label = DbStrings.removeAllGroups,
                    value = "",
                    onClick = { actions?.removeAll?.invoke(board) },
                    danger = true,
                    enabled = enabled
                )
            }
            if (canGroupBy.all { it.id in used }) GroupNote(DbStrings.groupsNeedMoreProperties)
        }
    }
}

/** Una regola: il numero, l'icona e il nome della proprietà, cosa fa nella bacheca, e il menu ⋮. */
@Composable
private fun GroupRuleRow(
    board: Boolean,
    index: Int,
    rule: GroupRuleSpec,
    count: Int,
    columns: List<DatabaseColumnEntity>,
    enabled: Boolean,
    actions: GroupRuleActions?,
    onOpen: (String) -> Unit
) {
    val column = columns.find { it.id == rule.column }
    var menu by remember(rule.column) { mutableStateOf(false) }
    val faded = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(GROUP_RULE_ROW_TAG)
            .clickable(enabled = enabled) { onOpen(rule.column) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp)
        )
        Icon(
            column?.type?.icon() ?: Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            text = column?.name?.let(DbStrings::columnName).orEmpty(),
            modifier = Modifier.weight(1f),
            color = if (enabled) Color.Unspecified else faded
        )
        if (board) {
            Text(
                text = if (index == 0) DbStrings.boardColumns else DbStrings.boardBands,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menu = true }, enabled = enabled) {
                Icon(Icons.Filled.MoreVert, contentDescription = DbStrings.moreOptions)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(EditorStrings.moveUp) },
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
                    enabled = index > 0,
                    onClick = {
                        menu = false
                        actions?.move?.invoke(board, rule.column, -1)
                    }
                )
                DropdownMenuItem(
                    text = { Text(EditorStrings.moveDown) },
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                    enabled = index < count - 1,
                    onClick = {
                        menu = false
                        actions?.move?.invoke(board, rule.column, 1)
                    }
                )
                DropdownMenuItem(
                    text = { Text(DbStrings.removeGroup, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menu = false
                        actions?.remove?.invoke(board, rule.column)
                    }
                )
            }
        }
    }
}

/** "Add a group": le proprietà che non sono già in una regola. Toccandone una, un livello in più. */
@Composable
internal fun GroupAddPage(
    rules: List<GroupRuleSpec>,
    columns: List<DatabaseColumnEntity>,
    onChoose: (String) -> Unit
) {
    val used = rules.mapTo(HashSet()) { it.column }
    val free = groupable(columns).filter { it.id !in used }
    if (free.isEmpty()) {
        GroupNote(if (columns.none { it.type.canGroup }) DbStrings.groupingNeedsProperty else DbStrings.groupsNeedMoreProperties)
        return
    }
    SheetGroup {
        free.forEachIndexed { index, column ->
            if (index > 0) HorizontalDivider()
            SettingsRow(
                icon = column.type.icon(),
                label = DbStrings.columnName(column.name),
                value = "",
                onClick = { onChoose(column.id) }
            )
        }
    }
}

/**
 * **Una regola**: la sua proprietà (scegliendone una che fa già un altro
 * livello, i due si scambiano), "Hide empty groups", per una data "Date by",
 * e "Remove group".
 */
@Composable
internal fun GroupRulePage(
    board: Boolean,
    rule: GroupRuleSpec,
    rules: List<GroupRuleSpec>,
    columns: List<DatabaseColumnEntity>,
    enabled: Boolean,
    actions: GroupRuleActions?,
    onColumnChanged: (String) -> Unit,
    onRemoved: () -> Unit
) {
    val column = columns.find { it.id == rule.column }
    SheetGroup {
        groupable(columns).forEachIndexed { i, candidate ->
            if (i > 0) HorizontalDivider()
            // Accanto a quelle che fanno già un livello, il suo numero.
            val place = rules.indexOfFirst { it.column == candidate.id }
            SettingsRow(
                icon = candidate.type.icon(),
                label = DbStrings.columnName(candidate.name),
                value = when {
                    candidate.id == rule.column -> "✓"
                    place >= 0 -> "${place + 1}"
                    else -> ""
                },
                onClick = {
                    if (candidate.id != rule.column) {
                        actions?.setColumn?.invoke(board, rule.column, candidate.id)
                        onColumnChanged(candidate.id)
                    }
                },
                enabled = enabled
            )
        }
    }
    Spacer(modifier = Modifier.size(12.dp))
    SheetGroup {
        SettingsToggleRow(
            icon = Icons.Filled.VisibilityOff,
            label = DbStrings.hideEmptyGroups,
            checked = rule.hideEmpty,
            enabled = enabled,
            onCheckedChange = { actions?.setHideEmpty?.invoke(board, rule.column, it) }
        )
    }
    if (column?.type == ColumnType.DATE) {
        Spacer(modifier = Modifier.size(12.dp))
        DateGroupBySection(rule.dateBy, enabled = enabled) { actions?.setDateBy?.invoke(board, rule.column, it) }
    }
    Spacer(modifier = Modifier.size(16.dp))
    SheetGroup {
        SettingsRow(
            icon = Icons.Filled.Delete,
            label = DbStrings.removeGroup,
            value = "",
            onClick = {
                actions?.remove?.invoke(board, rule.column)
                onRemoved()
            },
            danger = true,
            enabled = enabled
        )
    }
}
