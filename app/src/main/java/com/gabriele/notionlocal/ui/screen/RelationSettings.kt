package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.RollupCalc
import com.gabriele.notionlocal.data.entity.RollupConfig
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.rollupCalcsFor
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.RelationStrings
import com.gabriele.notionlocal.ui.i18n.Strings

/**
 * **Le impostazioni di "Relation" e "Rollup"** nella finestra della proprietà
 * (dal 21/10/2026): a quale database si collega una Relation, e se vale nei
 * due sensi; quale Relation legge un Rollup, quale proprietà e che calcolo.
 */

/** Il nome di un database, o "Untitled". */
private fun databaseTitle(title: String?): String = title?.ifBlank { null } ?: Strings.untitled

/**
 * "Related to": la scelta del database. Per una Relation che c'è già il
 * database non si cambia più ([fixed]): le righe collegate sono di quello.
 */
@Composable
internal fun RelationSettingsSection(
    databases: List<PageRepository.LinkableDatabase>?,
    database: String?,
    databaseName: String?,
    fixed: Boolean,
    onDatabase: (String) -> Unit,
    twoWay: Boolean,
    onTwoWay: (Boolean) -> Unit,
    twinName: String,
    onTwinName: (String) -> Unit
) {
    // "Show on …" subito sotto il database scelto (dal 21/10/2026): in fondo
    // all'elenco di tutti i database finiva fuori dallo schermo.
    @Composable
    fun twoWayControls() {
        Column(modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)) {
            SettingsToggleRow(
                icon = Icons.Filled.SyncAlt,
                label = RelationStrings.showOn(databaseTitle(databaseName)),
                checked = twoWay,
                onCheckedChange = onTwoWay
            )
            Text(
                RelationStrings.showOnHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 34.dp, bottom = 6.dp)
            )
            if (twoWay) {
                SectionLabel(RelationStrings.twinName)
                SheetTextField(value = twinName, onValueChange = onTwinName, placeholder = RelationStrings.twinName, leading = Icons.Filled.Notes)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(RelationStrings.relatedTo)
        if (fixed) {
            DatabaseChoiceRow(title = databaseTitle(databaseName), place = null, selected = true, onClick = null)
            twoWayControls()
        } else {
            Text(
                RelationStrings.chooseDatabaseHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            databases?.forEach { item ->
                DatabaseChoiceRow(
                    title = databaseTitle(item.page.title),
                    place = item.place,
                    selected = item.page.id == database,
                    onClick = { onDatabase(item.page.id) }
                )
                if (item.page.id == database) twoWayControls()
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun DatabaseChoiceRow(title: String, place: String?, selected: Boolean, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.TableChart,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!place.isNullOrBlank()) {
                Text(
                    place,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = DbStrings.selected, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Un Rollup: quale Relation di questo database ([relations]), quale
 * proprietà delle righe collegate ("Name" o una colonna di là) e quale
 * calcolo, fra quelli che hanno senso per quella proprietà.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RollupSettingsSection(
    relations: List<DatabaseColumnEntity>,
    related: Map<String, DatabaseSnapshot>,
    config: RollupConfig?,
    onChange: (RollupConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (relations.isEmpty() || config == null) {
            Text(
                RelationStrings.needsRelation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            return@Column
        }
        SectionLabel(RelationStrings.relation)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            relations.forEach { relation ->
                FilterChip(
                    selected = relation.id == config.relation,
                    onClick = {
                        // Un'altra Relation, un altro database: si riparte dal nome.
                        if (relation.id != config.relation) onChange(RollupConfig(relation.id))
                    },
                    label = { Text(DbStrings.columnName(relation.name)) }
                )
            }
        }
        val target = relations.find { it.id == config.relation }?.relationConfig()?.let { related[it.database] }
        // Si legge ogni proprietà di là, tranne gli altri Rollup: non hanno niente di salvato.
        val properties = target?.columns.orEmpty().filter { it.type != ColumnType.ROLLUP }
        SectionLabel(RelationStrings.property)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = config.property == SORT_BY_NAME,
                onClick = { onChange(config.copy(property = SORT_BY_NAME, calc = keepCalc(config, null))) },
                label = { Text(RelationStrings.name) }
            )
            properties.forEach { property ->
                FilterChip(
                    selected = property.id == config.property,
                    onClick = { onChange(config.copy(property = property.id, calc = keepCalc(config, property.type))) },
                    label = { Text(DbStrings.columnName(property.name)) },
                    // Grigia come le icone dei tipi altrove (il chip la farebbe blu), chiara se scelta.
                    leadingIcon = {
                        Icon(
                            property.type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (property.id == config.property) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
        val propertyType = properties.find { it.id == config.property }?.type
        SectionLabel(RelationStrings.calculate)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rollupCalcsFor(propertyType).forEach { calc ->
                FilterChip(
                    selected = calc == config.calc,
                    onClick = { onChange(config.copy(calc = calc)) },
                    label = { Text(RelationStrings.calcName(calc)) }
                )
            }
        }
    }
}

/** Il calcolo resta se vale anche per la proprietà nuova, altrimenti si torna a "Show original". */
private fun keepCalc(config: RollupConfig, type: ColumnType?) =
    config.calc.takeIf { it in rollupCalcsFor(type) } ?: RollupCalc.SHOW_ORIGINAL
