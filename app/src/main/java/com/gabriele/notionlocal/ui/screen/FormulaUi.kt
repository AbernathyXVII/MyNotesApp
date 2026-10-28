package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.RollupFormatting
import com.gabriele.notionlocal.data.formula.FormulaConfig
import com.gabriele.notionlocal.data.formula.FormulaEngine
import com.gabriele.notionlocal.data.formula.FormulaValue
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.FormulaStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.viewmodel.DatabaseViewModel
import com.gabriele.notionlocal.viewmodel.FormulaPreview

// ---------------------------------------------------------------------------
// **La proprietà Formula** (dal 28/10/2026, chiesta dall'utente: "la proprietà
// Formula di Notion: giorni che mancano, prezzo × quantità, se è spuntato
// allora…"): la cella col risultato e la finestra in cui si scrive la
// formula, con l'anteprima su una riga vera. Il calcolo sta in
// `data/formula/`.
// ---------------------------------------------------------------------------

internal const val FORMULA_FIELD_TAG = "formulaField"
internal const val FORMULA_PREVIEW_TAG = "formulaPreview"
internal const val FORMULA_CELL_TAG = "formulaCell"
internal const val FORMULA_CHIP_TAG = "formulaChip"

/** I risultati delle formule della tabella, (riga, colonna) → valore: li mettono la tabella e la pagina di una riga. */
internal val LocalFormulaValues = compositionLocalOf<Map<Pair<String, String>, FormulaValue>> { emptyMap() }

/** Apre la finestra della formula di una colonna, con l'anteprima su una riga; null col database bloccato. */
internal val LocalFormulaEditor = compositionLocalOf<((DatabaseColumnEntity, String?) -> Unit)?> { null }

/**
 * La finestra della formula della colonna [column], con l'anteprima sulla
 * riga [rowId] (o sulla prima): la aprono una cella, il menu
 * dell'intestazione e le proprietà in cima alla pagina di una riga.
 */
@Composable
internal fun FormulaEditorFor(
    viewModel: DatabaseViewModel,
    column: DatabaseColumnEntity,
    rowId: String?,
    onDismiss: () -> Unit
) {
    val state by viewModel.tableState.collectAsStateWithLifecycle()
    val dataPage by viewModel.dataPage.collectAsStateWithLifecycle()
    FormulaEditorSheet(
        columnName = DbStrings.columnName(column.name),
        initial = remember(column.id) {
            FormulaConfig.parse(state.columns.find { it.id == column.id }?.optionsJson ?: column.optionsJson).expression
        },
        columns = state.columns.filter { it.id != column.id },
        nameTitle = dataPage?.nameColumnTitle,
        preview = { viewModel.previewFormula(column.id, it, rowId) },
        onDismiss = onDismiss,
        onSave = { viewModel.setFormula(column.id, it) }
    )
}

/** Il risultato della formula di una riga: un testo, un numero, una data, o la casella di un sì/no. */
@Composable
internal fun FormulaCell(
    column: DatabaseColumnEntity,
    row: DatabaseRowEntity,
    text: String,
    contentPadding: Dp,
    alignment: Alignment = Alignment.CenterStart
) {
    val value = LocalFormulaValues.current[row.id to column.id]
    val edit = LocalFormulaEditor.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .testTag(FORMULA_CELL_TAG)
            .then(if (edit != null) Modifier.clickable { edit(column, row.id) } else Modifier)
            .padding(horizontal = contentPadding)
            .cellWrapPadding(),
        contentAlignment = alignment
    ) {
        when (value) {
            is FormulaValue.Bool -> Icon(
                if (value.value) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = if (value.value) FormulaStrings.checked else FormulaStrings.unchecked,
                modifier = Modifier.size(20.dp),
                tint = if (value.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Un errore si vede, piccolo: toccando la cella la finestra dice quale.
            is FormulaValue.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(FormulaStrings.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            else -> Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = cellMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- La finestra della formula ---

/** Una funzione dell'elenco: come si scrive e che cosa si inserisce toccandola (il cursore fra le parentesi). */
internal data class FormulaFunction(val name: String, val signature: String)

internal val FORMULA_FUNCTIONS: List<Pair<() -> String, List<FormulaFunction>>> = listOf(
    { FormulaStrings.logic } to listOf(
        FormulaFunction("if", "if(condition, then, else)"),
        FormulaFunction("ifs", "ifs(condition1, value1, …, else)"),
        FormulaFunction("and", "and(a, b)"),
        FormulaFunction("or", "or(a, b)"),
        FormulaFunction("not", "not(a)"),
        FormulaFunction("empty", "empty(value)")
    ),
    { FormulaStrings.numbers } to listOf(
        FormulaFunction("round", "round(number, decimals)"),
        FormulaFunction("floor", "floor(number)"),
        FormulaFunction("ceil", "ceil(number)"),
        FormulaFunction("abs", "abs(number)"),
        FormulaFunction("min", "min(a, b, …)"),
        FormulaFunction("max", "max(a, b, …)"),
        FormulaFunction("sum", "sum(a, b, …)"),
        FormulaFunction("mean", "mean(a, b, …)"),
        FormulaFunction("sqrt", "sqrt(number)"),
        FormulaFunction("pow", "pow(number, exponent)"),
        FormulaFunction("mod", "mod(a, b)"),
        FormulaFunction("toNumber", "toNumber(value)")
    ),
    { FormulaStrings.text } to listOf(
        FormulaFunction("concat", "concat(a, b, …)"),
        FormulaFunction("join", "join(\", \", a, b, …)"),
        FormulaFunction("length", "length(text)"),
        FormulaFunction("lower", "lower(text)"),
        FormulaFunction("upper", "upper(text)"),
        FormulaFunction("contains", "contains(text, piece)"),
        FormulaFunction("replace", "replace(text, find, with)"),
        FormulaFunction("replaceAll", "replaceAll(text, find, with)"),
        FormulaFunction("slice", "slice(text, start, end)"),
        FormulaFunction("format", "format(value)")
    ),
    { FormulaStrings.dates } to listOf(
        FormulaFunction("now", "now()"),
        FormulaFunction("today", "today()"),
        FormulaFunction("dateBetween", "dateBetween(date1, date2, \"days\")"),
        FormulaFunction("dateAdd", "dateAdd(date, 1, \"days\")"),
        FormulaFunction("dateSubtract", "dateSubtract(date, 1, \"days\")"),
        FormulaFunction("formatDate", "formatDate(date, \"DD/MM/YYYY\")"),
        FormulaFunction("year", "year(date)"),
        FormulaFunction("month", "month(date)"),
        FormulaFunction("date", "date(date)"),
        FormulaFunction("day", "day(date)"),
        FormulaFunction("week", "week(date)"),
        FormulaFunction("hour", "hour(date)"),
        FormulaFunction("minute", "minute(date)"),
        FormulaFunction("dateStart", "dateStart(date)"),
        FormulaFunction("dateEnd", "dateEnd(date)")
    )
)

/** Gli operatori da toccare: quello che si inserisce. Sulla tastiera del telefono alcuni sono scomodi. */
private val FORMULA_OPERATORS = listOf(
    "+", "-", "*", "/", "%", "^", "(", ")", ",", "\"\"", "==", "!=", ">", "<", ">=", "<=", "and", "or", "not", "?", ":", "true", "false"
)

/** Che cosa scrive un operatore toccato, e di quanto torna indietro il cursore: le virgolette lo tengono dentro. */
internal fun operatorSnippet(op: String): Pair<String, Int> = when (op) {
    "(", ")", "true", "false" -> op to 0
    "," -> ", " to 0
    "\"\"" -> op to 1
    else -> "$op " to 0
}

/** Mette [snippet] al posto della parte scelta; il cursore va [caretBack] caratteri prima della sua fine. */
internal fun TextFieldValue.inserting(snippet: String, caretBack: Int = 0): TextFieldValue {
    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)
    val before = text.substring(0, start)
    // Uno spazio davanti a una parola o a un operatore, perché "prop(\"A\")and" non si legge.
    val spaced = snippet.first().isLetter() || (snippet.length > 1 && snippet.endsWith(" ") && !snippet.startsWith(","))
    val glue = if (spaced && before.isNotEmpty() && !before.last().isWhitespace() && before.last() != '(') " " else ""
    val newText = before + glue + snippet + text.substring(end)
    val caret = before.length + glue.length + snippet.length - caretBack
    return TextFieldValue(newText, TextRange(caret))
}

/** Come si scrive il nome di una proprietà in una formula: prop("Nome"), con le virgolette protette. */
internal fun propReference(name: String): String = "prop(\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\")"

/**
 * **La finestra della formula**: in cima la formula, sotto il risultato su
 * una riga vera (o che cosa non va), poi da toccare le proprietà, gli
 * operatori, le funzioni e gli esempi. Chiudendola, la formula si salva.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormulaEditorSheet(
    columnName: String,
    initial: String,
    columns: List<DatabaseColumnEntity>,
    nameTitle: String?,
    preview: (String) -> FormulaPreview?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    NoteBottomSheet(
        onDismissRequest = {
            if (value.text != initial) onSave(value.text)
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        FormulaEditorContent(
            columnName = columnName,
            value = value,
            onValueChange = { value = it },
            columns = columns,
            nameTitle = nameTitle,
            preview = preview,
            onCancel = onDismiss,
            onDone = {
                if (value.text != initial) onSave(value.text)
                onDismiss()
            },
            modifier = Modifier.fillMaxHeight(0.92f).navigationBarsPadding().imePadding()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FormulaEditorContent(
    columnName: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    columns: List<DatabaseColumnEntity>,
    nameTitle: String?,
    preview: (String) -> FormulaPreview?,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val result = remember(value.text) { preview(value.text) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Functions, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                columnName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = onCancel) { Text(Strings.cancel) }
            TextButton(onClick = onDone) { Text(Strings.done) }
        }
        HorizontalDivider()
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (value.text.isEmpty()) {
                    Text(
                        FormulaStrings.placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AutoCaps {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    // Una formula è codice: niente correttore né maiuscole ("prop" non diventa
                    // "Prop", né "dateAdd" "date Add"). Con Ascii anche `AutoCaps` la lascia stare.
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .testTag(FORMULA_FIELD_TAG)
                )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            FormulaPreviewLine(result)
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FormulaSection(FormulaStrings.properties)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val name = nameTitle?.takeIf { it.isNotBlank() } ?: FormulaEngine.NAME
                FormulaChip(DbStrings.columnName(name), Icons.Filled.Notes) { onValueChange(value.inserting(propReference(name))) }
                columns.filter { it.type != ColumnType.BUTTON }.forEach { column ->
                    FormulaChip(DbStrings.columnName(column.name), column.type.icon()) {
                        onValueChange(value.inserting(propReference(column.name)))
                    }
                }
            }
            FormulaSection(FormulaStrings.operators)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FORMULA_OPERATORS.forEach { op ->
                    FormulaChip(op, null, monospace = true) {
                        val (snippet, back) = operatorSnippet(op)
                        onValueChange(value.inserting(snippet, back))
                    }
                }
            }
            FormulaSection(FormulaStrings.examples)
            FormulaExamples(columns, nameTitle) { onValueChange(TextFieldValue(it, TextRange(it.length))) }
            FORMULA_FUNCTIONS.forEach { (group, functions) ->
                FormulaSection(group())
                functions.forEach { f ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(value.inserting(f.name + "()", 1)) }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(f.signature, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                        Text(
                            FormulaStrings.describe(f.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (group() == FormulaStrings.dates) {
                    Text(
                        FormulaStrings.units,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/** Sotto la formula: il risultato sulla riga, o che cosa non va. */
@Composable
private fun FormulaPreviewLine(result: FormulaPreview?) {
    Column(modifier = Modifier.fillMaxWidth().testTag(FORMULA_PREVIEW_TAG)) {
        if (result == null) {
            Text(FormulaStrings.noRows, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Text(
            FormulaStrings.resultFor(result.row.title.ifBlank { Strings.untitled }),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.size(2.dp))
        when (val v = result.value) {
            is FormulaValue.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.size(6.dp))
                Text(FormulaStrings.errorText(v.error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            is FormulaValue.Bool -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (v.value) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (v.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (v.value) FormulaStrings.checked else FormulaStrings.unchecked, style = MaterialTheme.typography.bodyMedium)
            }
            is FormulaValue.Empty -> Text(FormulaStrings.empty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Text(FormulaEngine.display(v, RollupFormatting.formatter), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Gli esempi chiesti dall'utente, con le proprietà di questo database quando
 * ci sono (la prima data, i primi due numeri, la prima casella); toccandone
 * uno prende il posto della formula.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormulaExamples(columns: List<DatabaseColumnEntity>, nameTitle: String?, onPick: (String) -> Unit) {
    val date = columns.firstOrNull { it.type == ColumnType.DATE }?.name ?: "Date"
    val numbers = columns.filter { it.type == ColumnType.NUMBER }.map { it.name }
    val checkbox = columns.firstOrNull { it.type == ColumnType.CHECKBOX }?.name ?: "Done"
    val examples = listOf(
        FormulaStrings.exampleDaysLeft to "dateBetween(${propReference(date)}, today(), \"days\")",
        FormulaStrings.examplePriceTimesQuantity to "${propReference(numbers.getOrElse(0) { "Price" })} * ${propReference(numbers.getOrElse(1) { "Quantity" })}",
        FormulaStrings.exampleIfChecked to "if(${propReference(checkbox)}, \"✓ ${FormulaStrings.exampleDone}\", \"${FormulaStrings.exampleToDo}\")"
    )
    Column {
        examples.forEach { (label, formula) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(formula) }
                    .padding(vertical = 6.dp)
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formula,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FormulaSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun FormulaChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    monospace: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag(FORMULA_CHIP_TAG)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.let { if (monospace) it.copy(fontFamily = FontFamily.Monospace) else it },
            maxLines = 1
        )
    }
}
