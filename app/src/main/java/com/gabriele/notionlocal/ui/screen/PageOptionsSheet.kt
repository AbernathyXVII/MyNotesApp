package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.data.TextStats
import com.gabriele.notionlocal.ui.format.Formats
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.TrashPolicy
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.FavoriteStar

/**
 * Il menu dei tre puntini, uguale per una pagina e per un database.
 *
 * Sta in un file suo perché lo aprono due schermate diverse —
 * `PageEditorScreen` e `DatabaseViewScreen` — e due copie si
 * sarebbero separate al primo cambiamento, che è esattamente quello
 * che si vuole evitare in un menu di comandi.
 *
 * Ogni voce che non ha senso per la pagina aperta **non si vede**,
 * invece di vedersi spenta: sulla pagina principale non si può
 * spostare né buttare niente, e "Lock view" riguarda solo
 * l'impaginazione di un database. Le uniche spente sono Import ed
 * Export, e lì è diverso: si vedono apposta, per dire che arriveranno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageOptionsSheet(
    page: PageEntity,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSearch: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveTo: () -> Unit,
    onMoveToTrash: () -> Unit,
    onToggleViewLock: () -> Unit,
    onToggleLock: () -> Unit,
    onUpdates: () -> Unit,
    /**
     * Solo per un database che da qualche parte è mostrato come pagina
     * (dopo "Turn into page"): lo rimette dentro la pagina che lo
     * richiama. Null altrove, e allora la voce non c'è.
     */
    onTurnIntoDatabase: (() -> Unit)? = null,
    /**
     * Quanto testo c'è nella pagina, per la voce "X words" in fondo.
     * Null per i database — il loro contenuto sono righe, e le righe non
     * si contano — e per il breve istante in cui il conteggio si sta
     * facendo: la voce compare quando c'è il numero.
     */
    textStats: TextStats? = null
) {
    val isRoot = page.id == PageEntity.ROOT_PAGE_ID

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        // Scorre: aperto il conteggio del testo, le voci sono più di
        // quante ne stiano su uno schermo basso.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = page.title.ifBlank { Strings.untitled },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OptionsGroup {
                // La pagina principale non si mette fra i preferiti: è
                // quella da cui si parte sempre.
                if (!isRoot) {
                    OptionRow(
                        icon = if (page.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        label = Strings.favorite,
                        iconTint = if (page.isFavorite) FavoriteStar else null,
                        trailing = { Switch(checked = page.isFavorite, onCheckedChange = { onToggleFavorite() }) },
                        onClick = onToggleFavorite
                    )
                    HorizontalDivider()
                }
                OptionRow(
                    icon = Icons.Filled.Search,
                    label = Strings.search,
                    onClick = onSearch
                )
                // Duplicare la pagina principale vorrebbe dire copiare
                // l'archivio intero, sottopagine comprese, dentro sé
                // stesso. Spostarla e buttarla, le voci qui sotto, non
                // si può comunque.
                if (!isRoot) {
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.ContentCopy,
                        label = Strings.duplicate,
                        onClick = onDuplicate
                    )
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.DriveFileMove,
                        label = Strings.moveTo,
                        onClick = onMoveTo
                    )
                    if (onTurnIntoDatabase != null) {
                        HorizontalDivider()
                        OptionRow(
                            icon = Icons.Filled.TableChart,
                            label = Strings.turnIntoDatabase,
                            onClick = onTurnIntoDatabase
                        )
                    }
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.Delete,
                        label = Strings.moveToTrash,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onMoveToTrash
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            OptionsGroup {
                // Solo sui database: bloccare l'impaginazione di una
                // pagina di testo non vorrebbe dire niente.
                if (page.isDatabase) {
                    OptionRow(
                        icon = Icons.Filled.ViewQuilt,
                        label = Strings.lockView,
                        trailing = { Switch(checked = page.isViewLocked, onCheckedChange = { onToggleViewLock() }) },
                        onClick = onToggleViewLock
                    )
                    HorizontalDivider()
                }
                OptionRow(
                    icon = if (page.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    label = Strings.lockPage,
                    trailing = { Switch(checked = page.isLocked, onCheckedChange = { onToggleLock() }) },
                    onClick = onToggleLock
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            OptionsGroup {
                OptionRow(
                    icon = Icons.Filled.SystemUpdateAlt,
                    label = Strings.import,
                    enabled = false,
                    onClick = {}
                )
                HorizontalDivider()
                OptionRow(
                    icon = Icons.Filled.Upload,
                    label = Strings.export,
                    enabled = false,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            OptionsGroup {
                OptionRow(
                    icon = Icons.Filled.History,
                    label = Strings.updates,
                    onClick = onUpdates
                )
            }

            // **Il conteggio del testo**, in fondo come l'ha chiesto
            // l'utente e come sta su Notion. Chiuso dice solo le parole,
            // che è quello che si cerca quasi sempre; toccandolo si apre
            // **lì sotto** il resto, come la finestra "Word Count" di
            // Word. Sotto e non in una finestra nuova: una finestra sopra
            // un'altra finestra, su un telefono, copre proprio il menu da
            // cui si arriva, e per richiuderla servirebbe un tocco in più.
            if (textStats != null) {
                var statsOpen by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.size(12.dp))
                OptionsGroup {
                    OptionRow(
                        icon = Icons.Filled.TextFields,
                        label = Strings.wordCount(textStats.words),
                        trailing = {
                            Icon(
                                imageVector = if (statsOpen) {
                                    Icons.Filled.KeyboardArrowUp
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { statsOpen = !statsOpen }
                    )
                    if (statsOpen) {
                        HorizontalDivider()
                        // Nell'ordine chiesto dall'utente: le parole per
                        // prime, anche se si leggono già sulla voce sopra.
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            StatRow(Strings.statWords, textStats.words)
                            StatRow(Strings.statLetters, textStats.letters)
                            StatRow(Strings.statNumbers, textStats.digits)
                            StatRow(Strings.statLettersAndNumbers, textStats.lettersAndDigits)
                            StatRow(Strings.statCharactersNoSpaces, textStats.charactersNoSpaces)
                            StatRow(Strings.statCharactersWithSpaces, textStats.charactersWithSpaces)
                            StatRow(Strings.statLines, textStats.lines)
                            StatRow(Strings.statJapanese, textStats.japanese)
                            StatRow(Strings.statChinese, textStats.chinese)
                            StatRow(Strings.statKorean, textStats.korean)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Una riga del conteggio del testo: il nome a sinistra, il numero a
 * destra, come nella finestra di Word. Rientrata fin sotto il testo della
 * voce "X words", così si legge come il suo contenuto e non come altre
 * voci del menu; il numero segue il formato scelto nelle impostazioni
 * (1.000 o 1,000).
 */
@Composable
private fun StatRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = Formats.number(value.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Dove mettere una pagina, scelto da un elenco.
 *
 * L'elenco lo prepara il ViewModel: sono le pagine di testo non nel
 * cestino, meno quella che si sta spostando. Un database non compare
 * perché il suo contenuto sono righe, non blocchi, e un collegamento
 * lì dentro non avrebbe dove stare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToSheet(
    destinations: List<PageEntity>,
    onDismiss: () -> Unit,
    onPick: (PageEntity) -> Unit
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
                text = Strings.moveTo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (destinations.isEmpty()) {
                Text(
                    text = Strings.noOtherPage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                // Una colonna normale dentro una colonna che scorre
                // già: una lista pigra qui dentro si troverebbe
                // un'altezza senza limite e non saprebbe misurarsi. Le
                // destinazioni sono poche per definizione — sono le
                // pagine di testo dell'archivio — quindi non serve.
                OptionsGroup {
                    destinations.forEachIndexed { index, destination ->
                        if (index > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(destination) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = destination.icon)
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = destination.title.ifBlank { Strings.untitled },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

/** La conferma prima di buttare una pagina: è l'unica voce che toglie qualcosa di visibile. */
@Composable
fun MoveToTrashDialog(
    pageTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Dice dove va la pagina e quando se ne andrà davvero, secondo la
    // regola scelta nel cestino: buttarla non deve sembrare più
    // definitivo — né meno — di quello che è.
    val fate = when (AppSettings.trashPolicy) {
        TrashPolicy.KEEP_30_DAYS -> Strings.trashFateKeep
        TrashPolicy.EMPTY_AUTOMATICALLY -> Strings.trashFateEmpty
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.moveToTrash) },
        text = { Text("${Strings.moveToTrashText(pageTitle.ifBlank { Strings.untitled })} $fate") },
        confirmButton = { TextButton(onClick = onConfirm) { Text(Strings.moveToTrash) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
private fun OptionsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
    iconTint: Color? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    // Spenta, la voce si vede ma non risponde: è come si dice "questa
    // arriverà" senza doverlo scrivere da nessuna parte.
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        tint != null -> tint
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) (iconTint ?: contentColor) else contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
        trailing?.invoke()
    }
}
