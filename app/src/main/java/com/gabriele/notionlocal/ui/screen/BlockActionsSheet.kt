package com.gabriele.notionlocal.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.FavoriteStar
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * **Il menu di un blocco**, come quello che su Notion si apre dai sei
 * puntini accanto a ogni riga. Ci si arriva in tre modi:
 *
 * - dall'icona coi sei puntini nella barra sopra la tastiera, per la riga
 *   in cui sta il cursore;
 * - tenendo premuto il collegamento a una pagina (o a un database) nel
 *   testo, dove il cursore non può entrare;
 * - dai sei puntini nella riga degli strumenti di un database dentro la
 *   pagina, per lo stesso motivo.
 *
 * Le voci dipendono da cosa è il blocco, come chiesto dall'utente il
 * 24/09/2026:
 *
 * - **una riga di testo** (paragrafo, elenchi, casella, toggle): Turn
 *   into, Color, Duplicate. Icona, preferiti, spostare e cestino
 *   riguardano le pagine — "per i semplici campi di testo non mi
 *   servono" — e qui non ci sono; nemmeno Rename, perché una riga di
 *   testo non ha un nome da cambiare: il suo nome è il testo stesso;
 * - **una pagina**: Turn into, Color, Edit icon, Add to favorites,
 *   Rename, Duplicate, Move to, Move to trash;
 * - **un database**: in alto Turn into page, Turn into simple database,
 *   Turn into complex database, Lock database, Open as page; sotto le
 *   voci della pagina.
 *
 * Una voce che per quel blocco non è ancora costruita si vede **grigia**
 * (Turn into per pagine e database, Color per i database), come nei menu
 * "/" e "+": dice che esiste e che arriverà.
 *
 * Il menu è uno solo e le finestre che apre (colori, icona, nome, dove
 * duplicare, dove spostare, conferme) prendono il suo posto: stanno tutte
 * qui, e chi lo apre deve solo dire su quale blocco.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockActionsHost(
    blockId: String,
    blocks: List<BlockEntity>,
    viewModel: PageEditorViewModel,
    factory: ViewModelFactory,
    imageStore: PageImageStore,
    /** Cosa fa una voce di "Turn into": decide la schermata, come per i menu "/" e "+". */
    onTurnInto: (BlockEntity, SlashEntry) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val block = blocks.find { it.id == blockId }
    if (block == null) {
        // Il blocco non c'è più (cancellato mentre il menu si apriva, o da
        // un Annulla): niente da mostrare.
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val context = LocalContext.current

    val linkedId = block.linkedPageId?.takeIf {
        block.type == BlockType.PAGE_LINK || block.type == BlockType.DATABASE_LINK
    }
    // La pagina collegata **seguita nel tempo**: preferiti, lucchetto e
    // nome cambiano sotto gli occhi appena li si tocca. `Loaded` distingue
    // "non ancora letta" (null) da "non esiste più" (pagina null).
    val loaded by remember(linkedId) {
        if (linkedId == null) {
            flowOf(Loaded(null))
        } else {
            viewModel.observePageInfo(linkedId).map { Loaded(it) }
        }
    }.collectAsStateWithLifecycle(initialValue = null)
    val state = loaded ?: return
    val page = state.page
    if (linkedId != null && page == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    // **Vista collegata**: le voci che riguardano il database — semplice o
    // complesso, blocco, nome, icona — valgono per il database di origine,
    // che è quello che la vista mostra; duplicare, spostare, buttare e i
    // preferiti riguardano la vista, cioè questo blocco.
    val sourceId = page?.sourceDatabaseId
    val sourcePage by remember(sourceId) {
        if (sourceId == null) flowOf(null) else viewModel.observePageInfo(sourceId)
    }.collectAsStateWithLifecycle(initialValue = null)
    val dataPage = if (sourceId != null) sourcePage else page

    var step by remember { mutableStateOf(BlockMenuStep.MENU) }
    var moveExclusions by remember { mutableStateOf<PageRepository.MoveExclusions?>(null) }
    var rowPagesToLose by remember { mutableIntStateOf(0) }

    when (step) {
        BlockMenuStep.MENU, BlockMenuStep.TURN_INTO -> ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSheet
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                if (step == BlockMenuStep.TURN_INTO) {
                    TurnIntoList(
                        block = block,
                        onBack = { step = BlockMenuStep.MENU },
                        onPick = { entry ->
                            onDismiss()
                            // Il tipo che la riga ha già: si chiude e basta,
                            // senza una modifica vuota nella cronologia.
                            if ((entry.action as? SlashAction.Type)?.type != block.type) {
                                onTurnInto(block, entry)
                            }
                        }
                    )
                } else if (page == null) {
                    TextBlockMenu(
                        title = viewModel.plainTextOf(block).lineSequence().firstOrNull().orEmpty()
                            .ifBlank { EditorStrings.blockType(blockTypeLabel(block.type)) },
                        onTurnInto = { step = BlockMenuStep.TURN_INTO },
                        onColor = { step = BlockMenuStep.COLOR },
                        onDuplicate = {
                            onDismiss()
                            viewModel.duplicateBlock(block.id, imageStore)
                        }
                    )
                } else {
                    PageBlockMenu(
                        block = block,
                        page = page,
                        data = dataPage ?: page,
                        onTurnIntoPage = {
                            onDismiss()
                            // Come dalle impostazioni del database: il
                            // blocco diventa un collegamento e il database
                            // si apre a schermo intero.
                            viewModel.turnDatabaseIntoPage(block) { onNavigateToDatabase(page.id) }
                        },
                        onTurnIntoDatabase = {
                            onDismiss()
                            viewModel.turnLinkIntoDatabase(block)
                        },
                        onTurnIntoSimple = {
                            // Prima si guarda se c'è qualcosa da perdere: se
                            // nessuna pagina di riga ha contenuto non c'è
                            // niente da chiedere.
                            val data = dataPage ?: page
                            viewModel.countRowPagesWithContent(data.id) { count ->
                                if (count == 0) {
                                    onDismiss()
                                    viewModel.turnIntoSimpleDatabase(data.id, imageStore)
                                } else {
                                    rowPagesToLose = count
                                    step = BlockMenuStep.CONFIRM_SIMPLE
                                }
                            }
                        },
                        onTurnIntoComplex = {
                            onDismiss()
                            viewModel.turnIntoComplexDatabase((dataPage ?: page).id)
                        },
                        onToggleLock = {
                            val data = dataPage ?: page
                            viewModel.setLockedFor(data.id, !data.isLocked)
                        },
                        onOpenAsPage = {
                            onDismiss()
                            onNavigateToDatabase(page.id)
                        },
                        onColor = { step = BlockMenuStep.COLOR },
                        onEditIcon = { step = BlockMenuStep.EDIT_ICON },
                        onToggleFavorite = {
                            onDismiss()
                            viewModel.setFavoriteFor(page.id, !page.isFavorite)
                        },
                        onRename = { step = BlockMenuStep.RENAME },
                        onDuplicate = { step = BlockMenuStep.DUPLICATE },
                        onMoveTo = {
                            viewModel.loadMoveExclusionsFor(page.id) {
                                moveExclusions = it
                                step = BlockMenuStep.MOVE_TO
                            }
                        },
                        onMoveToTrash = { step = BlockMenuStep.TRASH }
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
            }
        }

        BlockMenuStep.COLOR -> {
            // Si parte dal colore che la riga ha già, se ne ha uno: è da lì
            // che di solito si vuole correggere.
            val firstSpan = viewModel.spansOf(block).firstOrNull()
            var onBackground by remember { mutableStateOf(false) }
            ColorPickerSheet(
                initialHex = if (onBackground) firstSpan?.background else firstSpan?.color,
                background = onBackground,
                onDismiss = onDismiss,
                onSwitchTarget = { onBackground = it },
                onPick = { hex ->
                    onDismiss()
                    viewModel.colorWholeBlock(block.id, onBackground, hex)
                }
            )
        }

        BlockMenuStep.EDIT_ICON -> if (dataPage != null) {
            // L'icona che la vista mostra è quella del database di origine.
            PageImageSheet(
                target = PageImageTarget.ICON,
                hasImage = dataPage.iconImage != null,
                store = imageStore,
                onPicked = { fileName ->
                    val previous = dataPage.iconImage
                    viewModel.setIconImageFor(dataPage.id, fileName)
                    previous?.takeIf { it != fileName }
                },
                onReposition = null,
                onDismiss = onDismiss
            )
        }

        BlockMenuStep.RENAME -> if (dataPage != null) {
            // Anche il nome: una vista collegata mostra quello del database.
            RenamePageDialog(
                current = dataPage.title,
                onDismiss = onDismiss,
                onConfirm = { title ->
                    onDismiss()
                    viewModel.renamePage(dataPage.id, title)
                }
            )
        }

        BlockMenuStep.DUPLICATE -> if (page != null) {
            DuplicateFlow(
                page = page,
                factory = factory,
                onDismiss = onDismiss,
                onChosen = { target, placeName ->
                    onDismiss()
                    // Si resta qui: accanto all'originale la copia si vede
                    // già, sotto il collegamento toccato; messa altrove, lo
                    // dice l'avviso.
                    viewModel.duplicateLinkedPage(
                        pageId = page.id,
                        besideBlockId = block.id,
                        target = target,
                        titleSuffix = Strings.copySuffix,
                        imageStore = imageStore
                    ) {
                        placeName?.let {
                            Toast.makeText(context, Strings.duplicatedInto(it), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        BlockMenuStep.MOVE_TO -> {
            val exclusions = moveExclusions
            if (page != null && exclusions != null) {
                // Lo stesso albero con la ricerca dei tre puntini: vedi
                // `MovePagePicker`. Si resta qui; l'avviso dice dove è
                // finita la pagina, che da qui sparisce.
                MovePagePicker(
                    page = page,
                    exclusions = exclusions,
                    factory = factory,
                    onDismiss = onDismiss,
                    onPick = { destination ->
                        onDismiss()
                        viewModel.moveLinkedPageTo(page.id, destination)
                        Toast.makeText(context, Strings.movedInto(destination.placeName()), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        BlockMenuStep.TRASH -> if (page != null) {
            MoveToTrashDialog(
                pageTitle = page.title,
                onConfirm = {
                    onDismiss()
                    viewModel.moveLinkedPageToTrash(page.id)
                },
                onDismiss = onDismiss
            )
        }

        BlockMenuStep.CONFIRM_SIMPLE -> if (page != null) {
            // Chiesto una volta sola e solo quando c'è davvero qualcosa da
            // perdere: l'utente ha detto che ci sta attento lui, ma un
            // numero davanti agli occhi costa un tocco e salva da uno
            // sbaglio che non si può annullare.
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(EditorStrings.simpleDatabaseLossTitle) },
                text = { Text(EditorStrings.simpleDatabaseLossText(rowPagesToLose)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDismiss()
                        viewModel.turnIntoSimpleDatabase((dataPage ?: page).id, imageStore)
                    }) {
                        Text(EditorStrings.turnIntoSimpleDatabase, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
            )
        }
    }
}

/** La pagina collegata, una volta letta: null dentro vuol dire che non esiste più. */
private class Loaded(val page: PageEntity?)

/** Le schermate del menu del blocco: il menu, la lista di "Turn into", e le finestre che si aprono dalle voci. */
private enum class BlockMenuStep { MENU, TURN_INTO, COLOR, EDIT_ICON, RENAME, DUPLICATE, MOVE_TO, TRASH, CONFIRM_SIMPLE }

/** Il nome inglese del tipo di un blocco, com'è scritto nel catalogo dei menu (`SLASH_ENTRIES`). */
private fun blockTypeLabel(type: BlockType): String =
    SLASH_ENTRIES.firstOrNull { (it.action as? SlashAction.Type)?.type == type }?.label ?: "Text"

@Composable
private fun MenuTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

/** La freccia che dice "questa voce apre un'altra schermata", come "Turn into ›" su Notion. */
@Composable
private fun Chevron() {
    Icon(
        Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

/** Il segno di spunta della scelta attuale: il tipo della riga, o il genere del database. */
@Composable
private fun CurrentMark() {
    Icon(
        Icons.Filled.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

/** Il menu per una riga di testo: tre voci, come ha chiesto l'utente. */
@Composable
private fun TextBlockMenu(
    title: String,
    onTurnInto: () -> Unit,
    onColor: () -> Unit,
    onDuplicate: () -> Unit
) {
    MenuTitle(title)
    OptionsGroup {
        OptionRow(icon = Icons.Filled.Autorenew, label = EditorStrings.turnInto, trailing = { Chevron() }, onClick = onTurnInto)
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.Brush, label = EditorStrings.color, trailing = { Chevron() }, onClick = onColor)
    }
    Spacer(modifier = Modifier.size(12.dp))
    OptionsGroup {
        OptionRow(icon = Icons.Filled.ContentCopy, label = Strings.duplicate, onClick = onDuplicate)
    }
}

/**
 * Il menu per una pagina o un database. I gruppi sono quelli di Notion:
 * trasformare e colorare, poi l'icona e i preferiti, poi nome, copia e
 * posto. Per un database, sopra a tutto, le voci del database.
 */
@Composable
private fun PageBlockMenu(
    block: BlockEntity,
    page: PageEntity,
    /** Chi ha i dati: il database di origine per una vista collegata, altrimenti `page`. */
    data: PageEntity,
    onTurnIntoPage: () -> Unit,
    onTurnIntoDatabase: () -> Unit,
    onTurnIntoSimple: () -> Unit,
    onTurnIntoComplex: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenAsPage: () -> Unit,
    onColor: () -> Unit,
    onEditIcon: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveTo: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    val linked = page.sourceDatabaseId != null
    // Una vista collegata si presenta come "↗ Nome del database", come nel
    // suo titolo dentro la pagina.
    MenuTitle((if (linked) "↗ " else "") + data.title.ifBlank { Strings.untitled })

    if (page.isDatabase) {
        OptionsGroup {
            // Dentro la pagina il database si può mandare a schermo
            // intero; già collegato come pagina, la voce al suo posto è il
            // contrario, rimetterlo dentro. Mostrare "Turn into page" su
            // qualcosa che è già una pagina sarebbe una voce che non fa
            // niente. Per una vista collegata dentro la pagina nessuna delle
            // due: una vista è fatta per stare lì, e come pagina a sé c'è
            // già "Open as page".
            if (block.type == BlockType.DATABASE_LINK) {
                if (!linked) {
                    OptionRow(icon = Icons.Filled.Description, label = Strings.turnIntoPage, onClick = onTurnIntoPage)
                    HorizontalDivider()
                }
            } else {
                OptionRow(icon = Icons.Filled.TableChart, label = Strings.turnIntoDatabase, onClick = onTurnIntoDatabase)
                HorizontalDivider()
            }
            // Le due voci ci sono sempre, e quella che il database è già si
            // vede spenta con la spunta: dice com'è adesso senza bisogno di
            // un'altra riga.
            OptionRow(
                icon = Icons.Filled.GridOn,
                label = EditorStrings.turnIntoSimpleDatabase,
                enabled = !data.isSimpleDatabase,
                trailing = if (data.isSimpleDatabase) {
                    { CurrentMark() }
                } else {
                    null
                },
                onClick = onTurnIntoSimple
            )
            HorizontalDivider()
            OptionRow(
                icon = Icons.Filled.TableChart,
                label = EditorStrings.turnIntoComplexDatabase,
                enabled = data.isSimpleDatabase,
                trailing = if (!data.isSimpleDatabase) {
                    { CurrentMark() }
                } else {
                    null
                },
                onClick = onTurnIntoComplex
            )
            HorizontalDivider()
            OptionRow(
                icon = if (data.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                label = EditorStrings.lockDatabase,
                trailing = { Switch(checked = data.isLocked, onCheckedChange = { onToggleLock() }) },
                onClick = onToggleLock
            )
            HorizontalDivider()
            OptionRow(icon = Icons.Filled.OpenInFull, label = EditorStrings.openAsPage, onClick = onOpenAsPage)
        }
        Spacer(modifier = Modifier.size(12.dp))
    }

    OptionsGroup {
        // Trasformare una pagina o un database in un'altra cosa non è
        // ancora costruito: la voce c'è, grigia, come nei menu "/" e "+".
        OptionRow(icon = Icons.Filled.Autorenew, label = EditorStrings.turnInto, enabled = false, onClick = {})
        HorizontalDivider()
        // Il colore di una pagina è quello del suo nome nel collegamento.
        // Un database dentro la pagina non ha un nome da colorare in quel
        // senso: grigio anche lui.
        OptionRow(
            icon = Icons.Filled.Brush,
            label = EditorStrings.color,
            enabled = block.type == BlockType.PAGE_LINK,
            trailing = if (block.type == BlockType.PAGE_LINK) {
                { Chevron() }
            } else {
                null
            },
            onClick = onColor
        )
    }
    Spacer(modifier = Modifier.size(12.dp))
    OptionsGroup {
        OptionRow(icon = Icons.Filled.EmojiEmotions, label = EditorStrings.editIcon, onClick = onEditIcon)
        HorizontalDivider()
        OptionRow(
            icon = if (page.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            label = if (page.isFavorite) EditorStrings.removeFromFavorites else EditorStrings.addToFavorites,
            iconTint = if (page.isFavorite) FavoriteStar else null,
            onClick = onToggleFavorite
        )
    }
    Spacer(modifier = Modifier.size(12.dp))
    OptionsGroup {
        OptionRow(icon = Icons.Filled.Edit, label = EditorStrings.rename, onClick = onRename)
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.ContentCopy, label = Strings.duplicate, onClick = onDuplicate)
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.DriveFileMove, label = Strings.moveTo, onClick = onMoveTo)
        HorizontalDivider()
        OptionRow(
            icon = Icons.Filled.Delete,
            label = Strings.moveToTrash,
            tint = MaterialTheme.colorScheme.error,
            onClick = onMoveToTrash
        )
    }
}

/**
 * "Turn into": **le stesse voci dei menu "/" e "+"**, nello stesso ordine
 * (`SLASH_ENTRIES`), con quelle non ancora costruite grigie. Il tipo che
 * la riga ha già porta la spunta.
 */
@Composable
private fun TurnIntoList(
    block: BlockEntity,
    onBack: () -> Unit,
    onPick: (SlashEntry) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
        }
        Text(
            text = EditorStrings.turnInto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    OptionsGroup {
        SLASH_ENTRIES.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider()
            val current = (entry.action as? SlashAction.Type)?.type == block.type
            OptionRow(
                icon = entry.icon,
                label = EditorStrings.blockType(entry.label),
                enabled = entry.enabled,
                trailing = if (current) {
                    { CurrentMark() }
                } else {
                    null
                },
                onClick = { onPick(entry) }
            )
        }
    }
}

/** Il nuovo nome di una pagina o di un database, da "Rename". */
@Composable
private fun RenamePageDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Tutto il nome selezionato: di solito lo si riscrive da capo, e per
    // correggerlo basta un tocco dentro.
    var value by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val confirm = { onConfirm(value.text.replace("\n", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(EditorStrings.rename) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.copy(text = it.text.replace("\n", "")) },
                singleLine = true,
                placeholder = { Text(Strings.untitled) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = { TextButton(onClick = confirm) { Text(Strings.done) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
