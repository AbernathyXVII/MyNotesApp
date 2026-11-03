package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonFilter
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.NAME_PROPERTY_ID
import com.gabriele.notionlocal.data.buttons.PropertyEdit
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.screen.blockTypeName

// ---------------------------------------------------------------------------
// **I bottoni nell'export** (dal 03/11/2026, chiesto dall'utente: "nel PDF (o
// nel file di esportazione) vengano visualizzate le istruzioni in merito del
// bottone (tipo se faccio cosa X, allora accade cosa Y)"): il nome del
// bottone e, una per riga, le azioni che fa toccandolo, con le stesse parole
// della finestra in cui si scelgono.
// ---------------------------------------------------------------------------

/** I nomi che servono per scrivere le azioni: i titoli di database e pagine, e le proprietà dei database. */
class ButtonNames(
    val titles: Map<String, String> = emptyMap(),
    val columns: Map<String, List<DatabaseColumnEntity>> = emptyMap()
)

/** Le pagine e i database che le azioni di [config] nominano, da leggere prima di scriverle. */
fun buttonReferences(config: ButtonConfig): Pair<Set<String>, Set<String>> {
    val pages = HashSet<String>()
    val databases = HashSet<String>()
    for (step in config.steps) when (step) {
        is ButtonStep.AddPage -> step.databaseId?.let { databases += it; pages += it }
        is ButtonStep.EditPages -> step.databaseId?.let { databases += it; pages += it }
        is ButtonStep.Open -> step.pageId?.let { pages += it }
        else -> {}
    }
    return pages to databases
}

/** **Il bottone a parole**: "Quando lo tocchi", poi le azioni numerate. */
fun exportButton(config: ButtonConfig, names: ButtonNames, untitled: String): ExportButton {
    val name = config.name.ifBlank { EditorStrings.blockType("Button") }
    val steps = config.steps.mapIndexed { index, step -> "${index + 1}. " + stepText(step, names, untitled) }
    return ExportButton(listOfNotNull(config.icon, name).joinToString(" "), steps)
}

private fun quoted(text: String) = "«${text.trim()}»"

private fun stepText(step: ButtonStep, names: ButtonNames, untitled: String): String {
    fun database(id: String?) = id?.let { names.titles[it]?.ifBlank { untitled } } ?: ButtonStrings.thisPage
    return when (step) {
        is ButtonStep.Confirm -> "${ButtonStrings.showConfirmation}: " + quoted(step.message.ifBlank { ButtonStrings.areYouSure })
        is ButtonStep.InsertBlocks -> "${ButtonStrings.insertBlocks} (${ButtonStrings.placeName(step.place).lowercase()}): " +
            step.blocks.joinToString(", ") { block ->
                blockTypeName(block.type) + if (block.type == "DIVIDER" || block.text.isBlank()) "" else " " + quoted(block.text)
            }
        is ButtonStep.AddPage -> buildString {
            append("${ButtonStrings.addPageTo} ${quoted(database(step.databaseId))}")
            if (step.title.isNotBlank()) append(": ").append(quoted(step.title))
            val columns = names.columns[step.databaseId].orEmpty()
            if (step.edits.isNotEmpty()) append(" — ").append(step.edits.joinToString("; ") { editText(it, columns) })
            if (step.openAfter) append(" — ").append(ButtonStrings.openAfterwards.lowercase())
        }
        is ButtonStep.EditPages -> buildString {
            val columns = names.columns[step.databaseId].orEmpty()
            append("${ButtonStrings.editPagesIn} ${quoted(database(step.databaseId))}")
            append(" — ").append(filterText(step.filter, columns))
            if (step.edits.isNotEmpty()) append(": ").append(step.edits.joinToString("; ") { editText(it, columns) })
        }
        is ButtonStep.Open -> "${ButtonStrings.openPageOrUrl}: " +
            (step.pageId?.let { quoted(names.titles[it]?.ifBlank { untitled } ?: untitled) } ?: step.url)
        is ButtonStep.Notify -> "${ButtonStrings.sendNotification}: " + quoted(step.title) +
            if (step.text.isBlank()) "" else " — ${step.text.trim()}"
    }
}

private fun columnName(columnId: String, columns: List<DatabaseColumnEntity>): String =
    if (columnId == NAME_PROPERTY_ID) ButtonStrings.name else columns.find { it.id == columnId }?.name ?: "?"

private fun valuesText(value: String): String =
    value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.joinToString(", ")

private fun editText(edit: PropertyEdit, columns: List<DatabaseColumnEntity>): String {
    val column = columns.find { it.id == edit.columnId }
    val op = if (column?.type == ColumnType.NUMBER) ButtonStrings.numberOpName(edit.op) else ButtonStrings.opName(edit.op)
    val withValue = edit.op == EditOp.SET || edit.op == EditOp.ADD || edit.op == EditOp.REMOVE
    return "${columnName(edit.columnId, columns)}: $op" + if (withValue && edit.value.isNotBlank()) " " + quoted(valuesText(edit.value)) else ""
}

private fun filterText(filter: ButtonFilter?, columns: List<DatabaseColumnEntity>): String {
    if (filter == null) return ButtonStrings.allPages.lowercase()
    val column = columns.find { it.id == filter.columnId }
    val values = filter.values.joinToString(", ") { value ->
        when {
            column?.type == ColumnType.CHECKBOX && value == "true" -> ButtonStrings.checked
            column?.type == ColumnType.CHECKBOX -> ButtonStrings.unchecked
            else -> valuesText(value)
        }
    }
    return "${ButtonStrings.onlyPagesWhere.lowercase()} ${columnName(filter.columnId, columns)} ${ButtonStrings.isWord} ${quoted(values)}"
}
