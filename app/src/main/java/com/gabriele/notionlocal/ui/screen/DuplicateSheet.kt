package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository.DestinationHit
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import com.gabriele.notionlocal.data.repository.PageRepository.MoveExclusions
import com.gabriele.notionlocal.data.repository.PageRepository.PageTreeNode
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.PagePickerViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * "Duplicate" dal menu dei tre puntini: prima dove mettere la copia, poi,
 * se la si vuole dentro un'altra pagina, quale.
 *
 * Il giro intero sta qui, uguale per una pagina e per un database a
 * schermo intero: chi lo apre riceve solo la destinazione scelta, e il
 * nome del posto per dire all'utente dov'è finita la copia (null quando
 * sta accanto all'originale, dove la si vede già).
 */
@Composable
internal fun DuplicateFlow(
    page: PageEntity,
    factory: ViewModelFactory,
    onDismiss: () -> Unit,
    onChosen: (target: DuplicateTarget, placeName: String?) -> Unit
) {
    var picking by remember { mutableStateOf(false) }
    if (!picking) {
        DuplicateSheet(
            isRowPage = page.isRowPage,
            onDismiss = onDismiss,
            onNextToOriginal = { onChosen(DuplicateTarget.NextToOriginal, null) },
            onMainMenu = { onChosen(DuplicateTarget.IntoPage(PageEntity.ROOT_PAGE_ID), Strings.mainMenu) },
            onChoosePage = { picking = true }
        )
    } else {
        DestinationPickerSheet(
            factory = factory,
            title = Strings.duplicateInto,
            hint = Strings.duplicateIntoHint,
            viewModelKey = "duplicate-destination",
            onDismiss = onDismiss,
            onPick = { node ->
                val name = node.title.ifBlank { Strings.untitled }
                when {
                    node.pageId != null -> onChosen(DuplicateTarget.IntoPage(node.pageId), name)
                    node.rowId != null -> onChosen(DuplicateTarget.IntoRow(node.rowId), name)
                }
            }
        )
    }
}

/** Le tre destinazioni, ognuna con una riga che dice cosa vuol dire. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicateSheet(
    isRowPage: Boolean,
    onDismiss: () -> Unit,
    onNextToOriginal: () -> Unit,
    onMainMenu: () -> Unit,
    onChoosePage: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = Strings.duplicate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Strings.duplicateWhere,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
            ) {
                // La pagina di una riga, accanto a sé, diventa una riga
                // nuova dello stesso database: la spiegazione lo dice,
                // perché altrimenti ci si aspetterebbe un collegamento.
                ChoiceRow(
                    icon = Icons.Filled.ContentCopy,
                    label = Strings.nextToOriginal,
                    hint = if (isRowPage) Strings.nextToOriginalRowHint else Strings.nextToOriginalPageHint,
                    onClick = onNextToOriginal
                )
                HorizontalDivider()
                ChoiceRow(
                    icon = Icons.Filled.Home,
                    label = Strings.endOfMainMenu,
                    hint = Strings.endOfMainMenuHint,
                    onClick = onMainMenu
                )
                HorizontalDivider()
                ChoiceRow(
                    icon = Icons.Filled.Description,
                    label = Strings.insidePage,
                    hint = Strings.insidePageHint,
                    onClick = onChoosePage
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ChoiceRow(
    icon: ImageVector,
    label: String,
    hint: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * La pagina in cui mettere qualcosa — la copia di "Duplicate", o la
 * pagina spostata da "Move to": l'albero della barra laterale, o,
 * scrivendo, le pagine col titolo che corrisponde.
 *
 * Si sceglie qualsiasi pagina, comprese quelle delle righe di tutti i
 * database: i database si aprono come cartelle per mostrarle, ma non si
 * scelgono, perché dentro di loro un collegamento non avrebbe dove stare.
 * Una riga mai aperta si sceglie lo stesso: la sua pagina nasce adesso.
 *
 * `excluded`, per "Move to": la pagina da spostare e quello che ha
 * dentro. Nell'albero si vedono grigie e non si aprono; cercando non
 * compaiono.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DestinationPickerSheet(
    factory: ViewModelFactory,
    title: String,
    hint: String,
    /** Distingue il ViewModel di "Duplicate" da quello di "Move to": ognuno ricorda i suoi rami aperti. */
    viewModelKey: String,
    onDismiss: () -> Unit,
    onPick: (PageTreeNode) -> Unit,
    excluded: MoveExclusions? = null
) {
    val picker: PagePickerViewModel = viewModel(factory = factory, key = viewModelKey)
    LaunchedEffect(Unit) { picker.reset() }
    val root by picker.root.collectAsStateWithLifecycle()
    val children by picker.children.collectAsStateWithLifecycle()
    val expanded by picker.expanded.collectAsStateWithLifecycle()
    val query by picker.query.collectAsStateWithLifecycle()
    val results by picker.results.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(PICKER_HEIGHT)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            )
            SearchField(
                value = query,
                onValueChange = picker::setQuery,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (query.isBlank()) {
                    // Niente chiavi: la stessa pagina può stare in due
                    // rami, e due righe con la stessa chiave fanno
                    // chiudere l'app.
                    val rows = root?.let { flattenTree(it, children, expanded) }.orEmpty()
                    items(rows) { row ->
                        val node = row.node
                        TreeRow(
                            row = row,
                            isCurrent = false,
                            isExpanded = node.pageId in expanded,
                            onOpen = { if (node.isDatabase) picker.toggle(node) else onPick(node) },
                            onToggle = { picker.toggle(node) },
                            enabled = excluded?.contains(node) != true
                        )
                        val id = node.pageId
                        if (id != null && id in expanded && node.hasChildren && children[id]?.isEmpty() == true) {
                            Text(
                                text = Strings.noPagesInside,
                                color = NotionGray400,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(
                                    start = TREE_START + TREE_INDENT * (row.depth + 1) + TREE_ARROW + 4.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                )
                            )
                        }
                    }
                } else if (results.none { excluded?.contains(it.node) != true }) {
                    item {
                        Text(
                            text = Strings.noResults,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    items(results.filter { excluded?.contains(it.node) != true }) { hit ->
                        HitRow(hit = hit, onClick = { onPick(hit.node) })
                    }
                }
            }
        }
    }
}

/** Una pagina trovata cercando: icona, titolo, e sotto dove sta. */
@Composable
private fun HitRow(hit: DestinationHit, onClick: () -> Unit) {
    val node = hit.node
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIconView(emoji = node.emoji, iconImage = node.iconImage, isDatabase = false, size = 18.dp)
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = node.title.ifBlank { Strings.untitled },
                color = NotionWhite,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            hit.place?.let { place ->
                Text(
                    text = Strings.inPlace(place.ifBlank { Strings.untitled }),
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(Strings.searchPages, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                cursorBrush = SolidColor(NotionWhite),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Se una pagina dell'albero, o una riga mai aperta, è fra quelle in cui non si può spostare. */
private fun MoveExclusions.contains(node: PageTreeNode): Boolean =
    node.pageId?.let { it in pageIds } ?: node.rowId?.let { it in rowIds } ?: false

/** Quanto sale la scelta della pagina: quasi tutto lo schermo, perché l'albero può essere lungo. */
private const val PICKER_HEIGHT = 0.9f
