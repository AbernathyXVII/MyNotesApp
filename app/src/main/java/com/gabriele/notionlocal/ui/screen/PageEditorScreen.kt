package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.text.style.TextOverflow
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.TextStats
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontFamily
import com.gabriele.notionlocal.data.entity.PageFont
import com.gabriele.notionlocal.ui.theme.PageFontGroup
import com.gabriele.notionlocal.ui.theme.label
import com.gabriele.notionlocal.ui.theme.lookalike
import com.gabriele.notionlocal.ui.theme.group
import com.gabriele.notionlocal.ui.theme.fontFamily
import com.gabriele.notionlocal.ui.theme.LocalPageTypography
import com.gabriele.notionlocal.ui.theme.PageTypography
import com.gabriele.notionlocal.ui.theme.DEFAULT_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.MIN_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.MAX_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.applyTextEdit
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.data.entity.setColorInRange
import com.gabriele.notionlocal.data.entity.setFormatInRange
import com.gabriele.notionlocal.data.entity.toggleFormatInRange
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.FavoriteStar
import com.gabriele.notionlocal.ui.theme.FloatingButtonBackground
import com.gabriele.notionlocal.ui.i18n.Strings
import androidx.compose.foundation.shape.CircleShape
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.theme.SpoilerCover
import com.gabriele.notionlocal.ui.theme.SpoilerRevealed
import com.gabriele.notionlocal.viewmodel.FormatType
import com.gabriele.notionlocal.viewmodel.BackspaceOutcome
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.SplitOutcome
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * I tipi "di testo scorrevole" vengono uniti, quando consecutivi, in un
 * unico campo di testo condiviso (vedi MergedTextRunField) — è quello
 * che rende selezione e backspace naturali tra un blocco e l'altro,
 * esattamente come in un editor di testo normale, invece che isolati
 * blocco per blocco. Tutti gli altri tipi restano "isole" separate,
 * renderizzate come prima tramite BlockRow.
 */
private val FLOWING_TYPES = setOf(
    BlockType.PARAGRAPH,
    BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM,
    // **Anche le caselle da spuntare.** Finché erano isole, ognuna era
    // un campo di testo a sé: andare a capo spostava il fuoco da un
    // campo all'altro, e ogni cambio di specie ne distruggeva uno
    // mentre aveva il cursore — da lì la tastiera che spariva, la
    // maiuscola che faceva uno scatto, le lettere nella riga sbagliata
    // e l'app che si chiudeva. Dentro il campo condiviso andare a capo
    // è solo scrivere un a-capo, come fra due paragrafi.
    BlockType.CHECKBOX
)

/**
 * Lo spazio che il quadratino occupa a inizio riga, scritto come testo
 * e reso invisibile: il quadratino vero viene disegnato sopra.
 *
 * È un carattere di riempimento e non il disegno della casella perché
 * così la larghezza non dipende da quali glifi ha il carattere
 * tipografico, e soprattutto non cambia quando la casella viene
 * spuntata — altrimenti il testo si sposterebbe a ogni tocco.
 */
private const val CHECKBOX_MARKER = "    "

/**
 * Quanto respirano le righe di un gruppo che contiene caselle da
 * spuntare.
 *
 * Il quadratino è piccolo e con le righe attaccate è facile spuntare
 * quella sbagliata col dito. Lo spazio si mette come **altezza di riga
 * dell'intero campo**, non riga per riga: dentro la trasformazione non
 * si può: né uno `ParagraphStyle` con `lineHeight`, né un carattere
 * invisibile ingrandito. Provati tutti e due, e il campo **smetteva di
 * disegnare**: righe vuote, nessun errore — e quella vuotezza finiva
 * nel database, cancellando il testo. Vedi "Strade già tentate".
 *
 * **Il cursore di scrittura è alto quanto la riga**, e questo non si
 * può separare: il cursore lo disegna il campo di testo prendendo il
 * riquadro della riga. Più spazio fra le caselle vuol dire cursore più
 * alto, meno spazio vuol dire cursore normale. Con 34 si notava; qui si
 * sta appena sopra il naturale (24), che lascia il cursore di misura
 * normale e un po' d'aria fra una casella e l'altra.
 */
private val CHECKBOX_LINE_HEIGHT = 27.sp

/**
 * L'altezza di una riga di testo scorrevole senza caselle da spuntare.
 *
 * È quella naturale del testo (16 su 24), ma **scritta a mano invece di
 * lasciarla al carattere**: così non dipende dal segno più grande che
 * la riga contiene. Ventiquattro bastano anche al pallino, che è il
 * segno più grande a 20 e in altezza ne occupa poco più di 23.
 */
private val RUN_LINE_HEIGHT = 24.sp

/**
 * Il testo del campo unito comincia sempre con un a-capo in più, che non
 * appartiene a nessun blocco e non viene mai mostrato né salvato. Serve a
 * dare al backspace premuto a inizio della PRIMA riga un carattere da
 * cancellare: senza, quel gesto lascia il testo identico e non c'è nulla
 * da osservare, perché gli eventi-tasto non arrivano (vedi README).
 *
 * **Deve essere un a-capo**, non uno zero-width space: l'a-capo spezza le
 * parole con certezza, quindi la tastiera non può inglobarlo nella parola
 * che sta componendo. Con lo zero-width space succedeva, e il risultato
 * era una lotta con la tastiera ad ogni tasto — vedi README, "Strade già
 * tentate".
 */
/**
 * Il margine laterale dei blocchi di una pagina.
 *
 * Lo applica ogni blocco per conto suo invece della lista che li
 * contiene: così il database incorporato può non averlo e usare tutta
 * la larghezza, come fa a schermo intero.
 */
private val PAGE_SIDE_PADDING = 20.dp

private const val RUN_LEAD = "\n"

/**
 * Quante voci della lista vengono prima dei blocchi: l'intestazione
 * (copertina, icona, titolo), che è una sola.
 *
 * Serve perché la posizione di un blocco fra i `renderItems` e la sua
 * posizione nella lista non coincidono più. Sono due numeri diversi e
 * confonderli porterebbe la pagina a scorrere sulla riga sbagliata.
 */
private const val HEADER_ITEM_COUNT = 1

/**
 * La barra sopra la tastiera: quanto è alto un pulsante e quanto è
 * grande la sua icona.
 *
 * Di serie un `IconButton` è 48 e porta la barra a quasi sessanta punti
 * — su un telefono è una fascia che si mangia una riga e mezzo di
 * testo. Qui i pulsanti sono 40 e le icone 22: restano comodi da
 * centrare col dito (il minimo consigliato è 48 per un bersaglio
 * isolato, ma qui sono dieci in fila e il dito trova comunque quello
 * giusto), e la barra scende sotto i cinquanta.
 */
private val BAR_BUTTON_SIZE = 40.dp

/**
 * Il triangolino di un toggle e lo stacco fra lui e il titolo.
 *
 * Il rientro delle righe dentro un toggle è **la somma dei due**: così
 * il testo di un figlio comincia esattamente sotto il testo del titolo,
 * come su Notion. Prima il rientro era un 20 fisso, un soffio più corto
 * della freccia, e i figli stavano storti di quattro punti a sinistra.
 */
/**
 * I pulsanti sospesi sopra la pagina (indietro e tre puntini): il
 * cerchio, l'icona dentro, lo stacco dai bordi.
 */
private val FLOATING_BUTTON_SIZE = 36.dp
private val FLOATING_ICON_SIZE = 22.dp
private val FLOATING_BUTTON_MARGIN = 10.dp
/** Lo stacco fra indietro e barra laterale, quando ci sono tutti e due. */
private val FLOATING_BUTTON_GAP = 8.dp

/**
 * Quanto spazio lasciare in cima a una pagina **senza copertina**.
 *
 * Con la copertina i pulsanti le stanno sopra ed è proprio quello che
 * si vuole. Senza, sotto ci sarebbero l'icona della pagina e "Add
 * cover", e il pulsante indietro ci finirebbe sopra: si lascia la fascia
 * che occupano, e non di più.
 */
private val FLOATING_BAR_CLEARANCE = FLOATING_BUTTON_SIZE + FLOATING_BUTTON_MARGIN * 2

/**
 * Un pulsante tondo sospeso sopra la pagina.
 *
 * Il fondo è un grigio **semitrasparente**: pieno sarebbe una toppa
 * sulla copertina, del tutto trasparente sparirebbe sulle copertine
 * chiare. Così si legge su qualunque immagine e la lascia intravedere.
 */
@Composable
private fun FloatingPageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(FLOATING_BUTTON_SIZE)
            .clip(CircleShape)
            .background(FloatingButtonBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private val TOGGLE_ARROW_SIZE = 24.dp
private val TOGGLE_ARROW_GAP = 4.dp
private val NESTED_INDENT = TOGGLE_ARROW_SIZE + TOGGLE_ARROW_GAP
private val BAR_ICON_SIZE = 22.dp

/**
 * Un pulsante della barra sopra la tastiera. Tutti uguali per
 * costruzione: scriverne uno a mano con una misura diversa è il modo in
 * cui una barra diventa storta.
 */
@Composable
private fun BarButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(BAR_BUTTON_SIZE)
    ) {
        content()
    }
}

/** Quanto può essere larga la voce del font nella barra: "Times New Roman" si accorcia coi puntini. */
private val FONT_CHIP_MAX_WIDTH = 132.dp

/**
 * Una voce della barra Aa che mostra un valore e apre un elenco: il font
 * e il corpo della pagina, **prima di B come su OneNote**. Il valore si
 * legge sempre — il nome del font in uso, il numero del corpo — e la
 * freccetta dice che si tocca per cambiarlo.
 */
@Composable
private fun BarValueChip(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(BAR_BUTTON_SIZE - 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, NotionWhite.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .clickable(onClickLabel = contentDescription, onClick = onClick)
            .padding(start = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = NotionWhite,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = NotionWhite,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * **Il menu non prende il fuoco** (`focusable = false`), e non è un
 * dettaglio: un menu che lo prende chiude la tastiera, con la tastiera
 * se ne va la barra (è appoggiata sopra) e con la barra la voce da cui
 * il menu è partito. Così invece si sceglie e si continua a scrivere
 * dove si era, col cursore al suo posto.
 */
private val BarMenuProperties = PopupProperties(focusable = false)

/**
 * Il font della pagina, dalla barra Aa. Nella barra c'è solo il nome
 * chiesto dall'utente ("Calibri"); qui nell'elenco c'è anche, fra
 * parentesi, il font libero che lo disegna davvero ("Calibri (Carlito)"),
 * come ha chiesto l'utente. In cima "Predefinito", per tornare al font
 * di sistema; poi i tre gruppi separati da un divisore: occidentali,
 * cinesi, giapponesi.
 *
 * I nomi occidentali sono scritti ognuno **col suo font**, come nei menu
 * di Word: sono dentro l'app e si vedono subito. Quelli cinesi e
 * giapponesi no — farli vedere vorrebbe dire scaricarli tutti e sei,
 * decine di megabyte, solo per aprire il menu.
 */
@Composable
private fun FontPickerChip(
    current: PageFont?,
    onPick: (PageFont?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        BarValueChip(
            text = current?.label ?: EditorStrings.defaultFont,
            contentDescription = EditorStrings.font,
            onClick = { open = true },
            modifier = Modifier.widthIn(max = FONT_CHIP_MAX_WIDTH)
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            properties = BarMenuProperties,
            modifier = Modifier.background(DarkSheet)
        ) {
            FontMenuItem(
                label = EditorStrings.defaultFont,
                selected = current == null,
                fontFamily = null,
                onClick = {
                    open = false
                    onPick(null)
                }
            )
            PageFontGroup.entries.forEach { group ->
                HorizontalDivider()
                PageFont.entries.filter { it.group == group }.forEach { font ->
                    FontMenuItem(
                        label = "${font.label} (${font.lookalike})",
                        selected = current == font,
                        fontFamily = if (group == PageFontGroup.LATIN) font.fontFamily() else null,
                        onClick = {
                            open = false
                            onPick(font)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FontMenuItem(
    label: String,
    selected: Boolean,
    fontFamily: FontFamily?,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = NotionWhite,
                fontFamily = fontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        trailingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, tint = NotionWhite) }
        } else {
            null
        },
        onClick = onClick
    )
}

/**
 * Il corpo del testo della pagina nella barra Aa: il numero in uso, e
 * toccandolo si apre `FontSizeDialog`, dove lo si scrive.
 *
 * La finestra **non sta qui dentro** ma in fondo alla schermata, accesa
 * da `onClick`: mentre si scrive il numero la tastiera passa alla sua
 * casella, nessuna riga della pagina ha più il cursore, e la barra —
 * che esiste solo quando una riga ce l'ha — se ne va. Una finestra nata
 * dentro la barra se ne andrebbe con lei, a metà numero.
 */
@Composable
private fun FontSizeChip(
    current: Int,
    onClick: () -> Unit
) {
    BarValueChip(
        text = current.toString(),
        contentDescription = EditorStrings.fontSize,
        onClick = onClick
    )
}

/**
 * La finestrella dove si **scrive a mano** il corpo del testo, da 5 a 72,
 * come ha chiesto l'utente dopo aver visto la prima versione (un elenco
 * da cui sceglierlo).
 *
 * Si apre col numero in uso già selezionato, così scrivendo lo si
 * sostituisce senza doverlo cancellare, e con la tastiera dei numeri.
 * Accetta solo cifre, al massimo due; "OK" (o il tasto Fatto della
 * tastiera) si accende solo se il numero sta fra 5 e 72, e sotto la
 * casella c'è scritto quali sono i limiti — diventa rosso se si esce.
 *
 * Chiusa la finestra il cursore non torna da solo nella pagina: si
 * tocca il punto dove si vuole riprendere a scrivere. Rimetterlo da qui
 * vorrebbe dire spostare il fuoco a comando mentre la tastiera cambia
 * da numeri a lettere, il passaggio che con la tastiera Samsung dà più
 * problemi (vedi "Peculiarità dell'ambiente di test" nel README).
 */
@Composable
private fun FontSizeDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val start = current.toString()
    var field by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    val value = field.text.toIntOrNull()
    val valid = value != null && value in MIN_PAGE_FONT_SIZE..MAX_PAGE_FONT_SIZE
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(EditorStrings.fontSize, color = NotionWhite) },
        text = {
            Column {
                OutlinedTextField(
                    value = field,
                    onValueChange = { typed ->
                        val digits = typed.text.filter { it.isDigit() }.take(2)
                        field = if (digits == typed.text) {
                            typed
                        } else {
                            TextFieldValue(digits, TextRange(digits.length))
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = NotionWhite),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (valid) onConfirm(value!!)
                    }),
                    isError = field.text.isNotEmpty() && !valid,
                    modifier = Modifier
                        .width(96.dp)
                        .focusRequester(focus)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = EditorStrings.fontSizeRange(MIN_PAGE_FONT_SIZE, MAX_PAGE_FONT_SIZE),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (field.text.isEmpty() || valid) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(value!!) }) { Text(Strings.done) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

/**
 * Diagnostica temporanea per i difetti delle caselle da spuntare che
 * **non si riescono a riprodurre da computer**: il cursore che salta a
 * inizio riga e l'Invio che cancella quello che si è scritto.
 *
 * Dipendono dalla tastiera vera — `adb shell input text` scrive senza
 * passare dall'IME, quindi la composizione delle parole, la maiuscola
 * automatica e i tempi reali non entrano mai in gioco. L'unico modo di
 * vederli è che li riproduca una persona col dito e che qui resti
 * scritto cosa è arrivato davvero al campo.
 *
 * Si legge con `adb logcat -d -s NOTE:D`. **Da togliere** quando i due
 * difetti sono chiusi.
 */
private fun noteLog(message: String) {
    android.util.Log.d("NOTE", message)
}

/** Il testo con l'a-capo nascosto reso visibile, per il registro. */
private fun String.forLog(): String = replace("\n", "\\n")

/**
 * Il campo invisibile su cui posare la tastiera per un istante quando
 * la casella di scrittura che ha il fuoco sta per essere distrutta.
 * Vedi il commento dove viene creato, in `PageEditorScreen`.
 */
private val LocalFocusPark = staticCompositionLocalOf<FocusRequester?> { null }

/**
 * Se la pagina aperta è bloccata (la voce "Lock page" del menu): il
 * contenuto si legge e si seleziona, ma non si scrive.
 *
 * Passa da qui e non da un parametro perché i campi di scrittura
 * stanno in fondo a una catena lunga di composable, e infilare lo
 * stesso `Boolean` in otto firme di seguito è un modo sicuro di
 * dimenticarselo in una.
 */
private val LocalPageLocked = staticCompositionLocalOf { false }

/**
 * Quanto aspettare, dopo che la tastiera si è chiusa, prima di
 * spegnere il cursore. La tastiera Samsung sparisce e riappare per un
 * istante durante i cambi di fuoco: più lungo del `IME_SETTLE_DELAY_MS`
 * del ViewModel, che di quei cambi è la durata.
 *
 * Alzato da 250 a 450 quando è arrivato il cambio di specie delle
 * caselle da spuntare: lì il fuoco viene tolto e rimesso di proposito,
 * e la tastiera ci mette di più a tornare. Il prezzo è che, chiusa la
 * tastiera a mano, il cursore resta acceso due decimi in più.
 */
private const val IME_CLOSE_SETTLE_MS = 450L

/**
 * Quanto la barra sopra la tastiera aspetta, perso il fuoco, prima di
 * sparire: il tempo che il cursore ci mette ad arrivare sulla riga nuova
 * quando quella di prima cambia specie. Misurato fra 100 e 150 ms (la
 * scrittura nel database più `IME_SETTLE_DELAY_MS`); il doppio per stare
 * larghi. Il prezzo è che passando al titolo della pagina la barra
 * resta tre decimi di secondo in più.
 */
private const val BAR_HANDOFF_GRACE_MS = 300L

/** Quanto è alta la copertina, e quanto grande l'icona-immagine. */
private val COVER_HEIGHT = 160.dp
private val PAGE_ICON_SIZE = 56.dp

// Il quadratino delle caselle da spuntare: grande quanto le lettere
// che gli stanno accanto (una riga di testo è alta 24, le maiuscole
// ne occupano circa 17). La zona di tocco è più larga del disegno,
// perché un bersaglio di 17dp sarebbe scomodo da centrare.
private val CHECKBOX_SIZE = 16.dp
private val CHECKBOX_TOUCH_SIZE = 24.dp

// Il quadrato va alzato per sembrare in riga con le lettere.
// Centrarlo sulla riga di testo non basta: dentro la sua riga alta 24
// il testo non sta in mezzo, perché sotto la linea di base resta lo
// spazio per le code di g e p, e quindi le lettere appaiono più in
// alto. Misurato ingrandendo lo schermo: il centro del quadrato
// restava 1,5dp sotto il centro ottico delle maiuscole, da cui i 4,5
// di adesso.
private val CHECKBOX_BASELINE_LIFT = 4.5.dp

// Entro quanto una divisione va considerata un'eco della tastiera e
// non un secondo Invio: più larga quando torna indietro lo stesso
// pezzo di testo (segno inequivocabile), stretta quando a ripetersi è
// solo la stessa modifica, perché lì bisogna solo escludere che sia
// stato un dito — e un dito due volte in 100ms non ci arriva.
private const val SPLIT_ECHO_MS = 400L
private const val DUPLICATE_EDIT_MS = 100L

/**
 * L'ultima divisione fatta da un campo, per riconoscerne l'eco:
 * il pezzo di testo passato alla riga nuova, la modifica che l'ha
 * prodotta (testo prima → testo dopo) e quando.
 */
private data class SplitEcho(
    val payload: String,
    val transition: Pair<String, String>,
    val at: Long
)

/** Il testo del campo unito per un gruppo di blocchi: a-capo iniziale + una riga per blocco. */
private fun runFieldText(runBlocks: List<BlockEntity>, viewModel: PageEditorViewModel): String =
    RUN_LEAD + runBlocks.joinToString("\n") { viewModel.plainTextOf(it) }

/** Il testo del campo senza l'a-capo iniziale: quello che corrisponde davvero ai blocchi. */
private fun bodyOf(fieldText: String): String =
    if (fieldText.startsWith(RUN_LEAD)) fieldText.substring(1) else fieldText

/**
 * Riconosce il gesto "backspace a inizio riga" da come è cambiato il
 * testo: è sparito esattamente un a-capo, ed è sparito proprio dove si
 * trova ora il cursore. Restituisce l'indice della riga che stava per
 * essere fusa con quella sopra, o -1 se il cambiamento è un altro
 * qualsiasi.
 */
/** Dove comincia la riga e da che numero deve partire l'elenco appena richiesto scrivendo "5.". */
private data class NumberedListPrefix(val lineIndex: Int, val startNumber: Int, val lineStart: Int)

/**
 * Riconosce "ho appena digitato il punto di un `5.` a inizio blocco".
 * Perché scatti devono valere tutte queste condizioni: il cambiamento è
 * l'inserimento di un solo punto, prima del punto ci sono solo cifre, e
 * quelle cifre cominciano esattamente a inizio riga e sono l'unica cosa
 * che c'è. Scrivere un punto dopo una qualsiasi altra cosa — anche
 * `abc 1.` o il punto decimale di `1.5` dopo del testo — non converte
 * niente.
 */
private fun numberedListPrefixJustTyped(oldBody: String, newBody: String, caret: Int): NumberedListPrefix? {
    if (newBody.length != oldBody.length + 1) return null
    if (caret < 2 || caret > newBody.length) return null
    if (newBody[caret - 1] != '.') return null
    // Dopo il punto la riga deve finire: il numero dev'essere tutto
    // quello che c'è scritto.
    if (caret < newBody.length && newBody[caret] != '\n') return null
    if (newBody.removeRange(caret - 1, caret) != oldBody) return null

    val lineStart = newBody.lastIndexOf('\n', caret - 2) + 1
    val digits = newBody.substring(lineStart, caret - 1)
    // Il tetto sulle cifre evita di sforare la capacità di un Int su un
    // incollaggio assurdo.
    //
    // **Lo zero vale.** Di suo un elenco parte da 1, ma chi scrive
    // "0." lo sta chiedendo apposta — si numera così quando c'è un
    // passo zero, un preambolo prima del primo vero punto — e da lì in
    // poi il conteggio prosegue normale: 0, 1, 2.
    if (digits.isEmpty() || digits.length > 9 || !digits.all { it.isDigit() }) return null
    val startNumber = digits.toInt()

    return NumberedListPrefix(
        lineIndex = newBody.take(lineStart).count { it == '\n' },
        startNumber = startNumber,
        lineStart = lineStart
    )
}

/**
 * Dove stanno le due coppie di barre appena chiuse, nel testo che le
 * contiene ancora: `open` è la prima delle quattro, `contentStart` e
 * `contentEnd` delimitano quello che va coperto.
 */
private data class SpoilerMarkup(val open: Int, val contentStart: Int, val contentEnd: Int)

/**
 * Riconosce "ho appena chiuso un `||qualcosa||`", come su Discord e
 * Telegram.
 *
 * Scatta sulla **seconda barra della chiusura**, cioè sul carattere che
 * completa la coppia, e solo se: il cambiamento è l'inserimento di
 * quella sola barra; subito prima ce n'è un'altra; più indietro **sulla
 * stessa riga** c'è una coppia di apertura; e in mezzo c'è almeno un
 * carattere che non sia a sua volta una barra.
 *
 * L'apertura si cerca **dalla più vicina**: scrivendo `||a|| e ||b||` la
 * seconda chiusura deve legarsi alla seconda apertura, non alla prima —
 * altrimenti coprirebbe anche il testo in chiaro che sta nel mezzo.
 */
private fun spoilerJustClosed(oldBody: String, newBody: String, caret: Int): SpoilerMarkup? {
    // `||x||`: cinque caratteri, il minimo perché ci sia qualcosa da
    // coprire.
    if (newBody.length != oldBody.length + 1) return null
    if (caret < 5 || caret > newBody.length) return null
    if (newBody[caret - 1] != '|' || newBody[caret - 2] != '|') return null
    if (newBody.removeRange(caret - 1, caret) != oldBody) return null

    val open = newBody.lastIndexOf("||", startIndex = caret - 5)
    if (open < 0) return null
    val content = newBody.substring(open + 2, caret - 2)
    if (content.isEmpty() || content.contains('|') || content.contains('\n')) return null

    return SpoilerMarkup(open = open, contentStart = open + 2, contentEnd = caret - 2)
}

private fun backspacedLineStart(oldBody: String, newBody: String, caret: Int): Int {
    if (newBody.length != oldBody.length - 1) return -1
    if (caret < 0 || caret >= oldBody.length) return -1
    if (oldBody[caret] != '\n') return -1
    if (oldBody.removeRange(caret, caret + 1) != newBody) return -1
    return oldBody.take(caret).count { it == '\n' } + 1
}

private sealed class RenderItem {
    data class Run(val blocks: List<BlockEntity>) : RenderItem()
    data class Island(val block: BlockEntity) : RenderItem()
}

/** Raggruppa una lista piatta di blocchi in sequenze di testo scorrevole consecutive e isole singole, preservando l'ordine. */
private fun groupIntoRenderItems(blocks: List<BlockEntity>): List<RenderItem> {
    val result = mutableListOf<RenderItem>()
    var currentRun = mutableListOf<BlockEntity>()
    for (block in blocks) {
        if (block.type in FLOWING_TYPES) {
            currentRun.add(block)
        } else {
            if (currentRun.isNotEmpty()) {
                result.add(RenderItem.Run(currentRun.toList()))
                currentRun = mutableListOf()
            }
            result.add(RenderItem.Island(block))
        }
    }
    if (currentRun.isNotEmpty()) {
        result.add(RenderItem.Run(currentRun.toList()))
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PageEditorScreen(
    pageId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUpdates: (String) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    onOpenSidebar: () -> Unit = {},
    viewModel: PageEditorViewModel = viewModel(factory = factory)
) {
    LaunchedEffect(pageId) { viewModel.load(pageId) }

    val page by viewModel.page.collectAsStateWithLifecycle()
    val blocks by viewModel.blocks.collectAsStateWithLifecycle()
    val focusRequestBlockId by viewModel.focusRequestBlockId.collectAsStateWithLifecycle()
    val formatRequest by viewModel.formatRequest.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showFormatBar by remember { mutableStateOf(false) }

    // Il pennello: quale riga colorare, se il colore va davanti o
    // dietro, e l'ultimo scelto — riaprendo la finestra si riparte da
    // lì invece che dal rosso, perché colorare due pezzi dello stesso
    // colore è la cosa che si fa più spesso.
    var showColorPicker by remember { mutableStateOf(false) }
    // La finestra dove si scrive il corpo del testo: sta qui e non nella
    // barra per la ragione spiegata in `FontSizeChip`.
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var colorForBlockId by remember { mutableStateOf<String?>(null) }
    var colorOnBackground by remember { mutableStateOf(false) }
    var lastPickedHex by remember { mutableStateOf<String?>(null) }

    // Il menu dei tre puntini e le due cose che può aprire.
    var showPageOptions by remember { mutableStateOf(false) }
    var moveDestinations by remember { mutableStateOf<List<PageEntity>?>(null) }
    var confirmTrash by remember { mutableStateOf(false) }
    var showDuplicate by remember { mutableStateOf(false) }

    // **Il cursore che cambia riga chiude la modifica in corso.**
    //
    // La cronologia raggruppa per sessione di scrittura, e una sessione
    // finisce quando si va a scrivere da un'altra parte. Il ViewModel se
    // ne accorge da solo quando l'altra riga viene **scritta**, ma non
    // quando ci si limita a spostare il cursore: lì la modifica di prima
    // sarebbe rimasta in sospeso fino alla successiva, e "Updates"
    // avrebbe sempre avuto un pezzo di ritardo.
    LaunchedEffect(focusedBlockId) { viewModel.closeEditSession() }

    val imeVisible = WindowInsets.isImeVisible

    // Una pagina aperta dal cestino si legge e basta, come una bloccata:
    // in cima c'è la barra per ripristinarla o cancellarla per sempre.
    val isTrashed = page?.trashedAt != null
    val readOnlyPage = page?.isLocked == true || isTrashed

    // **La barra resta mentre il cursore passa da una riga all'altra.**
    //
    // Quando una riga cambia specie (un divisore, un toggle, una casella
    // che torna testo) la sua casella di scrittura sparisce, e il fuoco
    // si posa un istante sul campo invisibile prima di arrivare alla
    // riga nuova. In quell'istante nessuna riga ha il fuoco, e la barra
    // — che si vede solo se una riga ce l'ha — spariva e tornava: nel
    // video, fotogrammi interi senza barra con la tastiera ferma al suo
    // posto. Qui, perso il fuoco, la barra aspetta un attimo prima di
    // andarsene; se nel frattempo una riga lo riprende, non si è mossa.
    //
    // Chiudendo la tastiera la barra se ne va subito lo stesso: guarda
    // anche `imeVisible`, e l'attesa vale solo a tastiera aperta.
    var barHeld by remember { mutableStateOf(false) }
    LaunchedEffect(focusedBlockId) {
        if (focusedBlockId != null) {
            barHeld = true
        } else {
            delay(BAR_HANDOFF_GRACE_MS)
            barHeld = false
        }
    }

    // Pagina bloccata: niente barra. Tutto quello che ci sta sopra —
    // rientri, spostamenti, "+" e formattazione — modifica, e offrirla
    // spenta sarebbe solo un modo più lento di dire di no.
    val showBar = !readOnlyPage &&
        (((focusedBlockId != null || barHeld) && imeVisible) || showTypeMenu)

    val isRootPage = pageId == PageEntity.ROOT_PAGE_ID
    var confirmDeleteForever by remember { mutableStateOf(false) }

    val onFocusChangedCallback: (String, Boolean) -> Unit = { id, focused ->
        if (focused) {
            focusedBlockId = id
        } else if (focusedBlockId == id) {
            focusedBlockId = null
        }
    }

    // Chiusa la tastiera, il cursore si spegne.
    //
    // Di suo il gesto indietro chiude solo la tastiera e lascia il
    // cursore che lampeggia dentro il campo: sembra che si stia ancora
    // scrivendo lì. E **non basta un `BackHandler`**: con la tastiera
    // aperta il tasto indietro lo riceve per prima la tastiera, che se
    // lo mangia per chiudersi, e all'app non arriva niente. Verificato
    // col log — il gesto non passava proprio.
    //
    // Quindi il segnale giusto non è il gesto ma la tastiera che si
    // chiude, comunque lo si faccia. L'attesa prima di spegnere il
    // cursore serve perché la tastiera Samsung sparisce e riappare per
    // un istante durante i cambi di fuoco: se in quell'istante
    // togliessimo il fuoco, romperemmo l'Invio che passa da un blocco
    // al successivo. Se la tastiera torna, l'effetto viene annullato e
    // rilanciato, e il cursore resta dov'è.
    //
    // `hasFocus` guarda tutto quello che sta dentro la pagina — il
    // titolo, il testo, le celle del database incorporato — quindi
    // vale ovunque, non solo sui blocchi.
    // Icona e copertina: quale delle due si sta cambiando, e dove
    // finiscono i file.
    val context = LocalContext.current
    val imageStore = remember(context) { PageImageStore(context) }
    var editingImage by remember { mutableStateOf<PageImageTarget?>(null) }

    // Spostare la copertina. L'inquadratura di prova vive qui finché
    // non si tocca "Save": scrivere nel database ad ogni millimetro di
    // dito vorrebbe dire non poter più annullare, e far scrivere il
    // disco sessanta volte al secondo.
    var movingCover by remember { mutableStateOf(false) }
    var liveCoverScale by remember { mutableStateOf(1f) }
    var liveCoverOffset by remember { mutableStateOf(Offset.Zero) }
    // Misure che servono a tenere l'immagine dentro la striscia: la
    // striscia stessa e l'immagine come è stata letta dal disco.
    var coverBox by remember { mutableStateOf(IntSize.Zero) }
    var coverImageSize by remember { mutableStateOf(IntSize.Zero) }

    var anythingFocused by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible, anythingFocused, focusRequestBlockId) {
        // Se il cursore lo stiamo spostando noi — c'è una richiesta di
        // fuoco in viaggio — la tastiera chiusa non vuol dire che
        // l'utente ha finito di scrivere: vuol dire che è in mezzo a un
        // cambio di campo. Spegnere lì il cursore era uno dei modi in
        // cui la tastiera spariva da sola.
        if (focusRequestBlockId != null) return@LaunchedEffect
        if (!imeVisible && anythingFocused) {
            delay(IME_CLOSE_SETTLE_MS)
            focusManager.clearFocus()
        }
    }
    // Resta per il caso in cui il cursore è acceso ma la tastiera è
    // già chiusa: lì il gesto indietro arriva davvero.
    BackHandler(enabled = anythingFocused) { focusManager.clearFocus() }

    // **Niente barra in alto: la pagina comincia subito sotto quella del
    // telefono.** La barra occupava una fascia vuota fra l'orologio e la
    // copertina, che serviva solo a ospitare due icone. Adesso la
    // copertina sale fin sotto l'ultimo pixel della barra del telefono, e
    // indietro e tre puntini stanno **sospesi sopra**, in un cerchietto
    // grigio che si legge su qualunque copertina — vedi `FloatingPageButton`.
    //
    // Senza barra, lo Scaffold riserva in alto solo lo spazio della barra
    // del telefono: è esattamente il margine che si vuole.
    Scaffold { padding ->
        val layoutDirection = LocalLayoutDirection.current
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Del riquadro dello Scaffold si prende tutto **tranne
                // il fondo**: là sotto ci pensa la riga qui sotto.
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection)
                )
                .background(DarkBackground)
                .onFocusChanged { anythingFocused = it.hasFocus }
                // **La barra di navigazione oppure la tastiera, non le
                // due sommate.** Prima lo spazio per la barra di
                // navigazione veniva dallo Scaffold e quello per la
                // tastiera da `imePadding`: a tastiera aperta si
                // sommavano, e la barra della formattazione restava
                // sospesa di quei pochi punti sopra i tasti. Con
                // l'unione vince la più alta delle due, che è sempre
                // quella giusta: a tastiera aperta la barra di
                // navigazione le sta dietro e non occupa niente.
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            if (isTrashed) {
                TrashBanner(
                    onBack = onBack,
                    onRestore = {
                        viewModel.restoreFromTrash()
                        Toast.makeText(context, Strings.restoredToMainMenu, Toast.LENGTH_SHORT).show()
                    },
                    onDeleteForever = { confirmDeleteForever = true }
                )
            }
            val topLevelBlocks = blocks.filter { it.parentBlockId == null }
            val renderItems = remember(topLevelBlocks) { groupIntoRenderItems(topLevelBlocks) }
            val listState = rememberLazyListState()
            val focusPark = remember { FocusRequester() }

            // **Una riga che non si vede non esiste.** `LazyColumn`
            // costruisce solo le righe dentro lo schermo: il blocco
            // nato premendo Invio in fondo alla parte visibile
            // spuntava dietro la tastiera, non veniva mai costruito, e
            // quindi non poteva prendersi il cursore. Il risultato era
            // il bug per cui Invio sembrava non fare niente e le
            // lettere successive finivano tutte nella riga di prima,
            // attaccate. Qui, prima ancora del fuoco, portiamo la riga
            // dentro lo schermo: costruita quella, il fuoco la trova.
            LaunchedEffect(focusRequestBlockId, renderItems) {
                val wanted = focusRequestBlockId ?: return@LaunchedEffect
                val found = renderItems.indexOfFirst { item ->
                    when (item) {
                        is RenderItem.Run -> item.blocks.any { it.id == wanted }
                        is RenderItem.Island -> item.block.id == wanted
                    }
                }
                if (found < 0) return@LaunchedEffect
                // La prima voce della lista è l'intestazione (copertina,
                // icona, titolo): i blocchi cominciano da quella dopo.
                val index = found + HEADER_ITEM_COUNT
                // Due fotogrammi di respiro: subito dopo un cambio di
                // struttura le misure della lista sono ancora quelle di
                // prima, e scorrere su misure vecchie porta fuori
                // schermo roba che stava benissimo dov'era.
                withFrameNanos { }
                withFrameNanos { }
                val info = listState.layoutInfo
                val visible = info.visibleItemsInfo
                if (visible.isEmpty()) return@LaunchedEffect
                val row = visible.find { it.index == index }
                // **Basta che la riga esista, non che si veda tutta.**
                // Una riga di testo scorrevole è alta quanto tutto il
                // gruppo che contiene — mezza pagina — e pretendere di
                // vederla intera faceva scorrere via quello che stava
                // sopra: sembrava che le caselle sparissero. Di dove
                // finisce esattamente il cursore si occupa già il campo
                // di testo, che si porta dietro la pagina mentre si
                // scrive. Qui si interviene solo quando la riga è
                // **tutta** fuori, sotto o sopra.
                val extraRoom = (visible.first().size) / 2
                when {
                    row != null && row.offset >= info.viewportEndOffset ->
                        listState.animateScrollBy(
                            (row.offset - info.viewportEndOffset + extraRoom).toFloat()
                        )
                    row != null && row.offset + row.size <= info.viewportStartOffset ->
                        listState.animateScrollBy(
                            (row.offset - info.viewportStartOffset).toFloat()
                        )
                    row != null -> Unit // si vede almeno in parte: va bene così
                    index > visible.last().index -> {
                        val last = visible.last()
                        val rowHeight = visible.maxOf { it.size }
                        val below = last.offset + last.size - info.viewportEndOffset
                        listState.animateScrollBy(
                            ((index - last.index) * rowHeight + below + extraRoom).toFloat()
                        )
                    }
                    index < visible.first().index -> listState.animateScrollToItem(index)
                }
            }

            // **Il posteggio della tastiera.**
            //
            // Un campo di testo alto un punto e invisibile, che esiste
            // sempre perché sta fuori dalla lista a scorrimento (dentro,
            // sparirebbe appena esce dallo schermo).
            //
            // Serve nei due momenti in cui un blocco cambia specie — da
            // riga a sé a testo scorrevole — e la sua casella di
            // scrittura viene distrutta. Distruggerla mentre ha il fuoco
            // fa chiudere l'app: la tastiera continua a chiederle dov'è
            // il cursore e non la trova (`LayoutCoordinate ...
            // isAttached`, difetto di Compose 1.6). Riprodotto a comando
            // venti volte su venti.
            //
            // Prima si staccava la tastiera, e infatti l'app non si
            // chiudeva più — ma la tastiera **spariva e tornava**
            // (misurato: `onHidden`, e `onShown` 315 ms dopo). Qui
            // invece il fuoco si posa un istante su questo campo: la
            // tastiera resta aperta perché sotto di sé ha sempre un
            // campo vero, e riparte sul blocco nuovo appena esiste.
            BasicTextField(
                value = "",
                onValueChange = { },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focusPark)
            )

            // **Col menu del "+" aperto, la pagina sotto non si tocca.**
            //
            // Toccando una riga di testo il tocco se lo prendeva il
            // campo di scrittura — il gestore qui sotto vede solo quello
            // che i figli non hanno già preso — quindi **il cursore si
            // spostava** e il menu restava aperto. Un velo invisibile
            // steso sopra la pagina intercetta il tocco prima di
            // chiunque: chiude il menu e non lascia passare altro.
            //
            // Copre solo la pagina, non la barra: così il "+" resta un
            // interruttore vero e lo si può ritoccare per chiudere.
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            CompositionLocalProvider(
                LocalFocusPark provides focusPark,
                LocalPageLocked provides readOnlyPage,
                // Il font e il corpo scelti dalla barra Aa, per tutto il
                // testo della pagina: vedi `PageTypography`.
                LocalPageTypography provides PageTypography(
                    fontFamily = page?.pageFont?.fontFamily(),
                    size = page?.pageFontSize ?: DEFAULT_PAGE_FONT_SIZE
                )
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    // Tocco sullo spazio vuoto sotto l'ultimo blocco per
                    // continuare a scrivere. Deve rispondere SOLO a tocchi
                    // veri: con Modifier.clickable rispondeva anche al tasto
                    // Invio (Compose lo tratta come attivazione quando un
                    // elemento interno ha il focus), quindi ogni a-capo
                    // faceva anche ripartire focusLastBlock, che riportava
                    // il cursore sull'ultimo blocco *noto prima* che l'a-capo
                    // creasse quello nuovo — il carattere successivo finiva
                    // sulla riga precedente.
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            // **Solo il vuoto sotto l'ultimo blocco.**
                            // Il gestore copre tutta la lista, quindi
                            // prendeva anche lo spazio accanto al
                            // titolo e i buchi fra una riga e l'altra:
                            // toccandoli, il cursore saltava in fondo
                            // alla pagina e si apriva la tastiera, che
                            // non è mai quello che si voleva fare.
                            val info = listState.layoutInfo
                            val last = info.visibleItemsInfo.lastOrNull()
                            if (last != null &&
                                last.index == info.totalItemsCount - 1 &&
                                tap.y > last.offset + last.size
                            ) {
                                viewModel.focusLastBlock()
                            }
                        }
                    }
            ) {
                // **La copertina e il titolo scorrono col resto.**
                //
                // Stavano fuori dalla lista, quindi restavano
                // inchiodati in cima mentre il testo scorreva
                // sotto: con una copertina alta si mangiavano
                // mezza schermata che non si poteva recuperare.
                // Messi qui dentro come prima voce se ne vanno
                // scorrendo, come su Notion.
                //
                // Sono **una voce sola** di proposito: gli indici
                // della lista servono altrove per portare in vista
                // la riga che prende il cursore, e uno scarto fisso
                // di uno e' molto piu' facile da tenere giusto di
                // uno variabile.
                item(key = "page-header") {
                    Column {
                // La copertina sta sopra a tutto, a tutta larghezza, e si
                // tocca per cambiarla. Senza copertina non occupa spazio:
                // per metterla c'è il pulsante accanto all'icona.
                val coverImage = page?.coverImage
                // Senza copertina, in cima si lascia la fascia dei
                // pulsanti sospesi: altrimenti indietro e tre puntini
                // finirebbero sopra l'icona e su "Add cover".
                // Nel cestino i pulsanti sospesi non ci sono (c'è la barra
                // del cestino), e la fascia resterebbe vuota.
                if (coverImage == null && !isTrashed) {
                    Spacer(modifier = Modifier.height(FLOATING_BAR_CLEARANCE))
                }
                if (coverImage != null) {
                    // Mentre si sposta comanda l'inquadratura di prova:
                    // quella salvata torna solo se si annulla. Così il
                    // dito muove l'immagine subito, senza scrivere nel
                    // database ad ogni millimetro.
                    val shownScale = if (movingCover) liveCoverScale else page?.coverScale ?: 1f
                    val shownOffset = if (movingCover) {
                        liveCoverOffset
                    } else {
                        Offset(page?.coverOffsetX ?: 0f, page?.coverOffsetY ?: 0f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(COVER_HEIGHT)
                            .onSizeChanged { coverBox = it }
                    ) {
                        CoverImage(
                            fileName = coverImage,
                            store = imageStore,
                            scale = shownScale,
                            offsetFraction = shownOffset,
                            onImageSize = { coverImageSize = it },
                            modifier = Modifier
                                .matchParentSize()
                                .then(
                                    if (movingCover) {
                                        Modifier.pointerInput(coverImage, coverImageSize, coverBox) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                val newScale = (liveCoverScale * zoom)
                                                    .coerceIn(1f, MAX_COVER_ZOOM)
                                                val limit = coverPanLimit(
                                                    coverBox,
                                                    coverImageSize.width,
                                                    coverImageSize.height,
                                                    newScale
                                                )
                                                val moved = Offset(
                                                    liveCoverOffset.x * coverBox.width + pan.x,
                                                    liveCoverOffset.y * coverBox.height + pan.y
                                                )
                                                liveCoverScale = newScale
                                                liveCoverOffset = Offset(
                                                    moved.x.coerceIn(-limit.x, limit.x) /
                                                        coverBox.width.coerceAtLeast(1),
                                                    moved.y.coerceIn(-limit.y, limit.y) /
                                                        coverBox.height.coerceAtLeast(1)
                                                )
                                            }
                                        }
                                    } else {
                                        Modifier.clickable(enabled = !isTrashed) { editingImage = PageImageTarget.COVER }
                                    }
                                )
                        )

                        if (movingCover) {
                            CoverMoveOverlay(
                                onCancel = { movingCover = false },
                                onSave = {
                                    viewModel.setCoverTransform(
                                        liveCoverScale,
                                        liveCoverOffset.x,
                                        liveCoverOffset.y
                                    )
                                    movingCover = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconImage = page?.iconImage
                    // **La pagina di una riga di database nasce senza
                    // icona**, come su Notion: niente foglietto 📄 di
                    // riserva, ma "Add icon". Così togliendo l'immagine
                    // l'icona sparisce davvero — e nel database, accanto al
                    // nome della riga, non resta niente. Le altre pagine
                    // tengono la loro emoji di sempre.
                    val rowPageWithoutIcon = iconImage == null && page?.isRowPage == true
                    if (rowPageWithoutIcon) {
                        if (!isTrashed) {
                            Text(
                                text = EditorStrings.addIcon,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { editingImage = PageImageTarget.ICON }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    } else if (iconImage != null) {
                        PageImage(
                            fileName = iconImage,
                            store = imageStore,
                            contentDescription = EditorStrings.icon,
                            modifier = Modifier
                                .size(PAGE_ICON_SIZE)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isTrashed) { editingImage = PageImageTarget.ICON }
                        )
                    } else {
                        Text(
                            text = page?.icon ?: "📄",
                            style = TextStyle(fontSize = 40.sp),
                            modifier = Modifier.clickable(enabled = !isTrashed) { editingImage = PageImageTarget.ICON }
                        )
                    }

                    if (coverImage == null && !isTrashed) {
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = EditorStrings.addCover,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { editingImage = PageImageTarget.COVER }
                                .padding(vertical = 6.dp)
                        )
                    }
                }

                // Mentre si scrive comanda il testo locale, non quello che
                // torna dal database: il salvataggio è immediato ma il giro
                // di ritorno no, e se nel frattempo è stato battuto un
                // altro tasto rimetterebbe il titolo com'era un attimo
                // prima, mangiandosi il carattere. Fuori dalla scrittura
                // comanda il database, così una rinomina fatta altrove si
                // vede appena si torna qui.
                var titleFocused by remember { mutableStateOf(false) }
                var titleText by remember { mutableStateOf(page?.title.orEmpty()) }
                LaunchedEffect(page?.title, titleFocused) {
                    if (!titleFocused) titleText = page?.title.orEmpty()
                }
                BasicTextField(
                    value = titleText,
                    // Un titolo sta su una riga sola: l'a capo digitato non
                    // ha senso e l'incolla di un testo che ne contiene va
                    // ripulito. La tastiera offre "fine" e basta: per
                    // scrivere nel corpo si tocca il corpo.
                    onValueChange = {
                        val singleLine = it.replace("\n", "")
                        titleText = singleLine
                        viewModel.updateTitle(singleLine)
                    },
                    singleLine = true,
                    // Pagina bloccata (o nel cestino): nemmeno il titolo si tocca.
                    readOnly = readOnlyPage,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    // Il titolo prende il font della pagina ma non il
                    // corpo: ha già una misura sua, e a 72 non ci
                    // starebbe più nello schermo.
                    textStyle = LocalPageTypography.current.fontOnly(
                        MaterialTheme.typography.titleLarge.copy(color = NotionWhite)
                    ),
                    cursorBrush = SolidColor(NotionWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .onFocusChanged {
                            titleFocused = it.isFocused
                            // Uscendo dal titolo la sua modifica si
                            // chiude, come per una riga qualsiasi.
                            if (!it.isFocused) viewModel.closeEditSession()
                        }
                )

                Spacer(modifier = Modifier.size(12.dp))

                    }
                }

                items(renderItems, key = { item ->
                    when (item) {
                        is RenderItem.Run -> "run:" + item.blocks.first().id
                        is RenderItem.Island -> "island:" + item.block.id
                    }
                }) { item ->
                    when (item) {
                        is RenderItem.Run -> MergedTextRunField(
                            runBlocks = item.blocks,
                            allBlocks = blocks,
                            viewModel = viewModel,
                            focusRequestBlockId = focusRequestBlockId,
                            formatRequest = formatRequest,
                            onFocusChanged = onFocusChangedCallback,
                            onSlashAction = { block, action ->
                                when (action) {
                                    is SlashAction.Type -> {
                                        // Diventando toggle la riga esce
                                        // dal testo condiviso: se era
                                        // l'unica, quel campo muore col
                                        // cursore dentro, che è il modo
                                        // in cui l'app si chiudeva. Il
                                        // fuoco si posa prima sul campo
                                        // invisibile, e da lì passa al
                                        // titolo del toggle.
                                        if (action.type == BlockType.TOGGLE) focusPark.requestFocus()
                                        viewModel.updateBlockType(block, action.type)
                                    }
                                    SlashAction.PageLink ->
                                        viewModel.convertToPageLink(block) { onNavigateToPage(it) }
                                    SlashAction.Divider -> {
                                        // Come per il toggle: se la riga
                                        // era la prima del suo gruppo di
                                        // testo, la casella in cui si sta
                                        // scrivendo sparisce. Senza il
                                        // posteggio il fuoco restava a
                                        // nessuno, la tastiera cominciava
                                        // a chiudersi e tornava, e la
                                        // pagina scorreva via fino al
                                        // database (visto nel video).
                                        focusPark.requestFocus()
                                        viewModel.convertToDividerAndFocusNext(block)
                                    }
                                    // Il database nasce dentro la
                                    // pagina; la vista gliela mettiamo
                                    // appena esiste, così "Calendar
                                    // view" dà un calendario e non una
                                    // tabella da cambiare a mano.
                                    is SlashAction.Database ->
                                        viewModel.convertToDatabaseLink(block) { databaseId ->
                                            action.layout?.let {
                                                viewModel.setDatabaseLayout(databaseId, it)
                                            }
                                        }
                                    SlashAction.NotYet -> Unit
                                }
                            }
                        )
                        is RenderItem.Island -> BlockRow(
                            block = item.block,
                            allBlocks = blocks,
                            depth = 0,
                            viewModel = viewModel,
                            factory = factory,
                            focusRequestBlockId = focusRequestBlockId,
                            formatRequest = formatRequest,
                            onFocusChanged = onFocusChangedCallback,
                            onNavigateToPage = onNavigateToPage,
                            onNavigateToDatabase = onNavigateToDatabase
                        )
                    }
                }
            }
            }

            if (showTypeMenu) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showTypeMenu = false }
                        }
                )
            }
            }

            // La barra sopra la tastiera.
            //
            // Ha un grigio **tutto suo**, più chiaro della pagina e
            // diverso dalla tastiera: prima era dello stesso nero della
            // pagina e non si capiva dove finisse una e cominciasse
            // l'altra. Ha due stati — la barra normale
            // (indenta/sposta/+/Aa) e, premendo Aa, quella di
            // formattazione (B/I/U/S) — che si sostituiscono a vicenda
            // nella stessa riga invece di impilarsi.
            if (showBar) {
                val focusedBlock = blocks.find { it.id == focusedBlockId }
                val aaAvailable = focusedBlock != null
                // **La barra se ne va insieme alla tastiera.**
                //
                // Il suo bordo inferiore sta appoggiato sopra la
                // tastiera, quindi mentre la tastiera scende la barra la
                // segue — ma si ferma al bordo dello schermo e resta lì
                // finché il sistema non dichiara la tastiera chiusa:
                // da fuori sembra che sparisca qualche istante dopo.
                //
                // Negli ultimi punti della discesa la si spinge giù di
                // quanto le manca per uscire, così arriva al bordo nello
                // stesso momento della tastiera. Quando poi il sistema
                // la smonta, è già invisibile e non si vede nessuno
                // scatto.
                val density = LocalDensity.current
                val imeBottomPx = WindowInsets.ime.getBottom(density)
                var barHeightPx by remember { mutableStateOf(0) }
                // Col menu "+" aperto la barra si vede anche senza
                // tastiera: lì non deve scivolare da nessuna parte.
                val slideAway = if (showTypeMenu) {
                    0
                } else {
                    (barHeightPx - imeBottomPx).coerceAtLeast(0)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { barHeightPx = it.height }
                        .offset { IntOffset(0, slideAway) }
                        .background(DarkSurfaceVariant)
                        .horizontalScroll(rememberScrollState())
                        // Nessun margine sopra e sotto: l'altezza la
                        // danno i pulsanti, e ogni punto in più qui è
                        // una fascia in più che copre il testo.
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showFormatBar && aaAvailable) {
                        BarButton(onClick = { showFormatBar = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        // Font e corpo del testo, prima di B come su
                        // OneNote. Valgono per **tutta la pagina**, non
                        // per il testo selezionato: vedi `PageTypography`.
                        // Con la pagina bloccata non si toccano, come il
                        // resto del contenuto.
                        if (!readOnlyPage) {
                            FontPickerChip(
                                current = page?.pageFont,
                                onPick = { viewModel.setPageFont(it) }
                            )
                            FontSizeChip(
                                current = page?.pageFontSize ?: DEFAULT_PAGE_FONT_SIZE,
                                onClick = { showFontSizeDialog = true }
                            )
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.BOLD) }
                        }) {
                            Text("B", color = NotionWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.ITALIC) }
                        }) {
                            Text("I", color = NotionWhite, fontStyle = FontStyle.Italic, fontSize = 18.sp)
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.UNDERLINE) }
                        }) {
                            Text("U", color = NotionWhite, textDecoration = TextDecoration.Underline, fontSize = 18.sp)
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.STRIKETHROUGH) }
                        }) {
                            Text("S", color = NotionWhite, textDecoration = TextDecoration.LineThrough, fontSize = 18.sp)
                        }
                        // Spoiler: copre il testo selezionato. L'occhio
                        // sbarrato è il segno che usano sia Discord sia
                        // Telegram per la stessa cosa.
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.SPOILER) }
                        }) {
                            Icon(
                                Icons.Filled.VisibilityOff,
                                contentDescription = EditorStrings.spoiler,
                                tint = NotionWhite,
                                modifier = Modifier.size(BAR_ICON_SIZE)
                            )
                        }
                    } else {
                        BarButton(onClick = { focusedBlock?.let { viewModel.outdentBlock(it) } }) {
                            Icon(Icons.Filled.FormatIndentDecrease, contentDescription = EditorStrings.outdent, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        BarButton(onClick = { focusedBlock?.let { viewModel.indentBlock(it) } }) {
                            Icon(Icons.Filled.FormatIndentIncrease, contentDescription = EditorStrings.indent, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        BarButton(onClick = { focusedBlock?.let { viewModel.moveBlockUp(it) } }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = EditorStrings.moveUp, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        BarButton(onClick = { focusedBlock?.let { viewModel.moveBlockDown(it) } }) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = EditorStrings.moveDown, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        Box {
                            // Il "+" fa da interruttore: aperto il menu,
                            // ritoccarlo lo richiude. È il gesto che
                            // viene da sé, e adesso funziona perché il
                            // menu non si chiude più da solo al tocco
                            // fuori — altrimenti questo tocco lo avrebbe
                            // chiuso e riaperto nello stesso istante.
                            BarButton(onClick = { showTypeMenu = !showTypeMenu }) {
                                Icon(Icons.Filled.Add, contentDescription = EditorStrings.addOrChangeBlock, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                            }
                            BlockTypeMenu(
                                expanded = showTypeMenu,
                                onDismiss = { showTypeMenu = false },
                                onSelect = { type ->
                                    showTypeMenu = false
                                    val block = focusedBlock ?: return@BlockTypeMenu
                                    when (type) {
                                        BlockType.PAGE_LINK ->
                                            viewModel.convertToPageLink(block) { onNavigateToPage(it) }
                                        // Il database appare dentro la
                                        // pagina, non ci si entra: è il
                                        // senso di averlo come blocco.
                                        BlockType.DATABASE_LINK ->
                                            viewModel.convertToDatabaseLink(block) {}
                                        BlockType.DIVIDER -> {
                                            // Vedi il gemello nel menu "/".
                                            focusPark.requestFocus()
                                            viewModel.convertToDividerAndFocusNext(block)
                                        }
                                        else -> {
                                            // Vedi il gemello nel menu "/":
                                            // il campo condiviso può morire
                                            // col cursore dentro.
                                            if (type == BlockType.TOGGLE) focusPark.requestFocus()
                                            viewModel.updateBlockType(block, type)
                                        }
                                    }
                                }
                            )
                        }
                        if (aaAvailable) {
                            BarButton(onClick = { showFormatBar = true }) {
                                Text("Aa", color = NotionWhite, fontWeight = FontWeight.Bold)
                            }
                            // Il pennello sta **accanto ad Aa e non
                            // dentro**: il colore non è un interruttore
                            // come grassetto e corsivo, apre una finestra
                            // sua, e metterlo in fila con quelli
                            // avrebbe promesso un comportamento che non
                            // ha.
                            BarButton(onClick = {
                                // La selezione si mette da parte
                                // **adesso**: aprendo la finestra il
                                // campo perde il fuoco e la selezione
                                // si chiude in un cursore, e fra un
                                // attimo non ci sarebbe più niente da
                                // colorare.
                                viewModel.captureSelectionForColor()
                                showColorPicker = true
                            }) {
                                Icon(
                                    Icons.Filled.Brush,
                                    contentDescription = EditorStrings.textAndBackgroundColor,
                                    tint = NotionWhite,
                                    modifier = Modifier.size(BAR_ICON_SIZE)
                                )
                            }
                        }
                        BarButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                            Icon(
                                Icons.Filled.Undo,
                                contentDescription = EditorStrings.undo,
                                tint = if (canUndo) NotionWhite else NotionWhite.copy(alpha = 0.3f),
                                modifier = Modifier.size(BAR_ICON_SIZE)
                            )
                        }
                        BarButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                            Icon(
                                Icons.Filled.Redo,
                                contentDescription = EditorStrings.redo,
                                tint = if (canRedo) NotionWhite else NotionWhite.copy(alpha = 0.3f),
                                modifier = Modifier.size(BAR_ICON_SIZE)
                            )
                        }
                        // Al posto di intercettare il tasto backspace sul
                        // campo di testo (che su alcune tastiere, Samsung
                        // inclusa, può disturbare la connessione con
                        // l'input, e comunque non sembra arrivare affatto
                        // per questo caso specifico), questo pulsante
                        // compare solo quando il blocco a fuoco è già
                        // vuoto — stessa azione, interazione a tocco.
                        // Per i blocchi di testo scorrevole ora uniti,
                        // però, backspace-tra-blocchi funziona già in modo
                        // nativo (vedi MergedTextRunField), quindi questo
                        // pulsante resta utile principalmente per le isole
                        // (checkbox/toggle).
                        if (focusedBlock != null && viewModel.plainTextOf(focusedBlock).isBlank()) {
                            BarButton(onClick = {
                                // Il campo di questa riga sta per morire col
                                // cursore dentro: il fuoco si posa prima sul
                                // campo invisibile, come in tutti gli altri
                                // punti in cui una riga sparisce sotto le dita.
                                focusPark.requestFocus()
                                viewModel.deleteBlockAndFocusPrevious(focusedBlock)
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = EditorStrings.deleteEmptyBlock, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                            }
                        }
                    }
                }
            }
        }

        // I pulsanti sospesi: stessa altezza, subito sotto la barra del
        // telefono. Ognuno è grande solo quanto il suo cerchio, così
        // intorno la pagina si tocca normalmente. Su una pagina nel
        // cestino non ci sono: al loro posto c'è la barra del cestino, che
        // ha il suo indietro.
        if (!isTrashed) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = padding.calculateTopPadding() + FLOATING_BUTTON_MARGIN, start = FLOATING_BUTTON_MARGIN),
                horizontalArrangement = Arrangement.spacedBy(FLOATING_BUTTON_GAP)
            ) {
                if (!isRootPage) {
                    FloatingPageButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = Strings.back,
                            tint = NotionWhite,
                            modifier = Modifier.size(FLOATING_ICON_SIZE)
                        )
                    }
                }
                // La barra laterale: stesso cerchio dei tre puntini, con le
                // tre lineette. Il cursore si spegne prima di aprirla,
                // altrimenti la tastiera resterebbe su sopra la barra.
                FloatingPageButton(onClick = {
                    focusManager.clearFocus()
                    onOpenSidebar()
                }) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = Strings.openSidebar,
                        tint = NotionWhite,
                        modifier = Modifier.size(FLOATING_ICON_SIZE)
                    )
                }
            }
            FloatingPageButton(
                onClick = { showPageOptions = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padding.calculateTopPadding() + FLOATING_BUTTON_MARGIN, end = FLOATING_BUTTON_MARGIN)
            ) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = Strings.pageOptions,
                    tint = NotionWhite,
                    modifier = Modifier.size(FLOATING_ICON_SIZE)
                )
            }
        }
        }
    }

    if (confirmDeleteForever) {
        DeleteForeverDialog(
            onDismiss = { confirmDeleteForever = false },
            onConfirm = {
                confirmDeleteForever = false
                viewModel.deletePermanently(imageStore) { onBack() }
            }
        )
    }

    val target = editingImage
    if (target != null) {
        PageImageSheet(
            target = target,
            hasImage = when (target) {
                PageImageTarget.ICON -> page?.iconImage != null
                PageImageTarget.COVER -> page?.coverImage != null
            },
            store = imageStore,
            onPicked = { fileName ->
                val replaced = when (target) {
                    PageImageTarget.ICON -> viewModel.setIconImage(fileName)
                    PageImageTarget.COVER -> viewModel.setCoverImage(fileName)
                }
                replaced
            },
            onReposition = if (target == PageImageTarget.COVER) {
                {
                    // Si riparte da dove si era rimasti, non da capo:
                    // ritoccare un'inquadratura già scelta è la cosa
                    // che si fa più spesso.
                    liveCoverScale = page?.coverScale ?: 1f
                    liveCoverOffset = Offset(
                        page?.coverOffsetX ?: 0f,
                        page?.coverOffsetY ?: 0f
                    )
                    movingCover = true
                }
            } else {
                null
            },
            onDismiss = { editingImage = null }
        )
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            current = page?.pageFontSize ?: DEFAULT_PAGE_FONT_SIZE,
            onDismiss = { showFontSizeDialog = false },
            onConfirm = { size ->
                showFontSizeDialog = false
                viewModel.setPageFontSize(size)
            }
        )
    }

    if (showColorPicker) {
        ColorPickerSheet(
            initialHex = lastPickedHex,
            background = colorOnBackground,
            onDismiss = { showColorPicker = false },
            onSwitchTarget = { colorOnBackground = it },
            onPick = { hex ->
                showColorPicker = false
                lastPickedHex = hex ?: lastPickedHex
                viewModel.applyCapturedColor(colorOnBackground, hex)
            }
        )
    }

    // --- Il menu dei tre puntini ---
    //
    // Il conteggio del testo per la voce "X words": si rifà ogni volta
    // che il menu si apre, e fino ad allora la voce non c'è — meglio che
    // mostrare per un istante il numero della volta prima.
    var textStats by remember { mutableStateOf<TextStats?>(null) }
    LaunchedEffect(showPageOptions) {
        textStats = null
        if (showPageOptions) viewModel.loadTextStats { textStats = it }
    }

    // Sta qui fuori dallo Scaffold e non dentro la barra in alto: una
    // finestra che sale dal basso non è figlia del pulsante che
    // l'apre, e messa lì dentro erediterebbe i margini della barra.
    page?.let { current ->
        if (showPageOptions) {
            PageOptionsSheet(
                page = current,
                textStats = textStats,
                onDismiss = { showPageOptions = false },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onSearch = {
                    showPageOptions = false
                    onOpenSearch()
                },
                onDuplicate = {
                    showPageOptions = false
                    showDuplicate = true
                },
                onMoveTo = {
                    showPageOptions = false
                    viewModel.loadMoveDestinations { moveDestinations = it }
                },
                onMoveToTrash = {
                    showPageOptions = false
                    confirmTrash = true
                },
                onToggleViewLock = { viewModel.toggleViewLocked() },
                onToggleLock = { viewModel.toggleLocked() },
                onUpdates = {
                    showPageOptions = false
                    onOpenUpdates(current.id)
                }
            )
        }

        moveDestinations?.let { destinations ->
            MoveToSheet(
                destinations = destinations,
                onDismiss = { moveDestinations = null },
                onPick = { destination ->
                    moveDestinations = null
                    // Spostata, questa pagina non sta più dov'era: si
                    // torna indietro, altrimenti si resterebbe su una
                    // schermata che "indietro" riporterebbe in un
                    // posto in cui la pagina non c'è più.
                    viewModel.movePageTo(destination.id) { onBack() }
                }
            )
        }

        if (showDuplicate) {
            DuplicateFlow(
                page = current,
                factory = factory,
                onDismiss = { showDuplicate = false },
                onChosen = { target, placeName ->
                    showDuplicate = false
                    // Alla copia ci si va subito: è la conferma che è
                    // stata fatta. Se è finita in un altro posto lo dice
                    // anche un avviso, perché da dentro la copia non si
                    // vede dove sta.
                    viewModel.duplicatePage(target, Strings.copySuffix, imageStore) { copyId ->
                        placeName?.let {
                            Toast.makeText(context, Strings.duplicatedInto(it), Toast.LENGTH_SHORT).show()
                        }
                        if (current.isDatabase) onNavigateToDatabase(copyId) else onNavigateToPage(copyId)
                    }
                }
            )
        }

        if (confirmTrash) {
            MoveToTrashDialog(
                pageTitle = current.title,
                onConfirm = {
                    confirmTrash = false
                    viewModel.moveToTrash { onBack() }
                },
                onDismiss = { confirmTrash = false }
            )
        }
    }
}

/** Quale delle due immagini di una pagina si sta cambiando. */
internal enum class PageImageTarget { ICON, COVER }

/**
 * Scegliere l'immagine di un'icona o di una copertina: dalla galleria
 * del telefono, oppure da un collegamento.
 *
 * Il collegamento **viene scaricato subito** e il file resta sul
 * telefono (vedi `PageImageStore`): è l'unico modo di avere i
 * collegamenti senza che l'app smetta di funzionare senza rete.
 * Mentre scarica il pulsante dice "Downloading…", perché un
 * collegamento lento senza nessun segno sembra un pulsante rotto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageImageSheet(
    target: PageImageTarget,
    hasImage: Boolean,
    store: PageImageStore,
    onPicked: (String?) -> String?,
    /** Null per l'icona: lì non c'è niente da inquadrare, è un quadratino. */
    onReposition: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Quando il nuovo file prende il posto del vecchio, il vecchio va
    // cancellato: resterebbe nella cartella per sempre senza che
    // nessuno possa più arrivarci.
    fun apply(fileName: String?) {
        val replaced = onPicked(fileName)
        scope.launch { store.delete(replaced) }
        onDismiss()
    }

    val gallery = rememberLauncherForActivityResult(
        // Il selettore di foto di sistema: non chiede il permesso di
        // leggere tutta la galleria, l'utente sceglie una foto e l'app
        // vede solo quella.
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        error = null
        scope.launch {
            val result = store.saveFromGallery(uri)
            busy = false
            result.fold(
                onSuccess = { apply(it) },
                onFailure = { error = EditorStrings.imageReadFailed }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = if (target == PageImageTarget.ICON) EditorStrings.icon else EditorStrings.cover,
                style = MaterialTheme.typography.titleMedium,
                color = NotionWhite,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                PageImageAction(
                    label = EditorStrings.upload,
                    enabled = !busy,
                    onClick = {
                        gallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Spacer(modifier = Modifier.size(12.dp))
                PageImageAction(
                    label = if (busy) EditorStrings.downloading else EditorStrings.link,
                    enabled = !busy && link.isNotBlank(),
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            val result = store.saveFromUrl(link)
                            busy = false
                            result.fold(
                                onSuccess = { apply(it) },
                                onFailure = {
                                    error = EditorStrings.linkFailed
                                }
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            BasicTextField(
                value = link,
                onValueChange = { link = it.replace("\n", "") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = NotionWhite),
                cursorBrush = SolidColor(NotionWhite),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        if (link.isEmpty()) {
                            Text(
                                text = EditorStrings.pasteImageLink,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )

            val message = error
            if (message != null) {
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (hasImage && onReposition != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Text(
                    text = EditorStrings.reposition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NotionWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) {
                            onReposition()
                            onDismiss()
                        }
                        .padding(vertical = 14.dp)
                )
            }

            if (hasImage) {
                Spacer(modifier = Modifier.size(20.dp))
                Text(
                    text = EditorStrings.remove,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) { apply(null) }
                        .padding(vertical = 14.dp)
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Quello che si vede sopra la copertina mentre la si sposta: come si
 * fa, e i due modi di uscirne.
 *
 * Il suggerimento c'è perché il trascinamento e la pizzicata non si
 * vedono: senza una riga che le nomini, una striscia che
 * improvvisamente non risponde più al tocco sembra rotta.
 */
@Composable
private fun CoverMoveOverlay(onCancel: () -> Unit, onSave: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = EditorStrings.dragToMove,
            style = MaterialTheme.typography.labelMedium,
            color = NotionWhite,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(DarkSheet.copy(alpha = 0.85f), RoundedCornerShape(percent = 50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageImageAction(label = Strings.cancel, enabled = true, onClick = onCancel)
            Spacer(modifier = Modifier.size(8.dp))
            PageImageAction(label = EditorStrings.save, enabled = true, onClick = onSave)
        }
    }
}

/** Uno dei due pulsanti della finestra dell'immagine. */
@Composable
private fun PageImageAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) NotionWhite else NotionWhite.copy(alpha = 0.35f),
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(percent = 50))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

/**
 * Quanto si può ingrandire una copertina, e quanto più fine la si
 * legge quando è ingrandita.
 *
 * `MAX_COVER_DETAIL` moltiplica la larghezza da chiedere al disco: a
 * ingrandimento 2 la striscia mostra metà immagine su tutta la sua
 * larghezza, e leggerla alla misura della striscia la farebbe vedere
 * sgranata. Il tetto vero lo mette comunque `PageImageStore`, che non
 * porta in memoria più di sei megapixel.
 */
private const val MAX_COVER_ZOOM = 4f
private const val MAX_COVER_DETAIL = 2

/**
 * La copertina, disegnata a mano invece che con `ContentScale.Crop`.
 *
 * Crop taglia l'immagine e tiene solo il riquadro; ingrandire dopo
 * quel risultato ingrandisce il ritaglio, e i lati già tagliati non
 * tornano più — spostarsi a destra e a sinistra non mostrerebbe
 * niente di nuovo. Qui l'immagine viene messa **più grande della
 * striscia** (la stessa misura che userebbe Crop, moltiplicata per
 * l'ingrandimento) e spostata dentro, con la striscia che ritaglia.
 * A ingrandimento 1 e senza spostamento si vede esattamente quello
 * che si vedeva prima.
 *
 * **Il file non viene mai toccato**: la risoluzione resta quella
 * originale, quello che cambia è soltanto cosa se ne guarda.
 *
 * `clampOffset` serve a chi mostra la stessa inquadratura in un
 * riquadro di **forma diversa** dalla striscia della pagina — le schede
 * della galleria. Lo spostamento è salvato in frazioni della striscia,
 * e in un riquadro più alto o più stretto la stessa frazione può
 * portare l'immagine oltre il bordo e scoprire un angolo vuoto: acceso,
 * lo spostamento si ferma dove l'immagine smette di coprire. Nella
 * pagina resta spento, perché lì il limite lo mette già il dito mentre
 * la si sposta, e la striscia è quella su cui l'inquadratura è nata.
 */
@Composable
internal fun CoverImage(
    fileName: String,
    store: PageImageStore,
    scale: Float,
    offsetFraction: Offset,
    onImageSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
    clampOffset: Boolean = false
) {
    var box by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
    val detail = ceil(scale).toInt().coerceIn(1, MAX_COVER_DETAIL)

    LaunchedEffect(fileName, box.width, detail) {
        if (box.width <= 0) return@LaunchedEffect
        val loaded = store.load(fileName, box.width * detail) ?: return@LaunchedEffect
        bitmap = loaded.asImageBitmap()
        onImageSize(IntSize(loaded.width, loaded.height))
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { box = it },
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image == null) {
            // Finché l'immagine si carica, il posto resta suo: senza,
            // il titolo salterebbe su e giù ad ogni apertura.
            Box(modifier = Modifier.matchParentSize().background(DarkSurface))
        } else {
            val density = LocalDensity.current
            val fit = coverFitFactor(box, image.width, image.height) * scale
            val drawnWidth = image.width * fit
            val drawnHeight = image.height * fit
            val limit = if (clampOffset) {
                coverPanLimit(box, image.width, image.height, scale)
            } else {
                null
            }
            Image(
                bitmap = image,
                contentDescription = EditorStrings.cover,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(
                        with(density) { drawnWidth.toDp() },
                        with(density) { drawnHeight.toDp() }
                    )
                    .offset {
                        var x = offsetFraction.x * box.width
                        var y = offsetFraction.y * box.height
                        if (limit != null) {
                            x = x.coerceIn(-limit.x, limit.x)
                            y = y.coerceIn(-limit.y, limit.y)
                        }
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
            )
        }
    }
}

/**
 * Di quanto va moltiplicata l'immagine perché copra tutta la striscia
 * senza lasciare bordi vuoti: è quello che fa `ContentScale.Crop`.
 */
private fun coverFitFactor(box: IntSize, imageWidth: Int, imageHeight: Int): Float {
    if (box.width <= 0 || box.height <= 0 || imageWidth <= 0 || imageHeight <= 0) return 1f
    return max(
        box.width.toFloat() / imageWidth,
        box.height.toFloat() / imageHeight
    )
}

/**
 * Di quanti pixel si può spostare la copertina prima di scoprire il
 * bordo. Senza questo limite si può trascinare l'immagine fuori dalla
 * striscia e restare a guardare un rettangolo vuoto.
 */
private fun coverPanLimit(
    box: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float
): Offset {
    if (imageWidth <= 0 || imageHeight <= 0) return Offset.Zero
    val fit = coverFitFactor(box, imageWidth, imageHeight) * scale
    return Offset(
        x = max(0f, (imageWidth * fit - box.width) / 2f),
        y = max(0f, (imageHeight * fit - box.height) / 2f)
    )
}

/**
 * Un'immagine di pagina già rimpicciolita a quanto serve davvero.
 *
 * La misura da raggiungere la dà il riquadro stesso: caricare una foto
 * da dodici megapixel per mostrarla larga un centimetro sono
 * quarantotto megabyte di memoria per niente, e l'app si chiude.
 */
@Composable
internal fun PageImage(
    fileName: String,
    store: PageImageStore,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var widthPx by remember(fileName) { mutableStateOf(0) }
    var bitmap by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(fileName, widthPx) {
        if (widthPx <= 0) return@LaunchedEffect
        bitmap = store.load(fileName, widthPx)?.asImageBitmap()
    }

    Box(modifier = modifier.onSizeChanged { widthPx = it.width }) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            // Finché l'immagine si carica, il posto resta suo: senza,
            // il titolo salterebbe su e giù ad ogni apertura.
            Box(modifier = Modifier.matchParentSize().background(DarkSurface))
        }
    }
}

/**
 * Menu "+" per scegliere/cambiare il tipo di blocco: griglia a due
 * colonne con icona e etichetta per ciascuna opzione, come il "+" di
 * Notion — non una lista verticale di voci di testo.
 */
@Composable
private fun BlockTypeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (BlockType) -> Unit
) {
    data class Entry(val type: BlockType, val label: String, val icon: @Composable () -> Unit)

    fun letterIcon(text: String): @Composable () -> Unit = {
        Text(text, color = NotionWhite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }

    fun vectorIcon(icon: ImageVector): @Composable () -> Unit = {
        Icon(icon, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(22.dp))
    }

    val entries = listOf(
        Entry(BlockType.PARAGRAPH, "Text", letterIcon("T")),
        Entry(BlockType.BULLET_LIST_ITEM, "Bulleted list", vectorIcon(Icons.Filled.FormatListBulleted)),
        Entry(BlockType.NUMBERED_LIST_ITEM, "Numbered list", vectorIcon(Icons.Filled.FormatListNumbered)),
        Entry(BlockType.CHECKBOX, "To-do list", vectorIcon(Icons.Filled.CheckBox)),
        Entry(BlockType.TOGGLE, "Toggle list", vectorIcon(Icons.Filled.ArrowRight)),
        Entry(BlockType.TABLE, "Table", vectorIcon(Icons.Filled.TableChart)),
        Entry(BlockType.PAGE_LINK, "Page", vectorIcon(Icons.Filled.Description)),
        Entry(BlockType.DATABASE_LINK, "Database", vectorIcon(Icons.Filled.List)),
        Entry(BlockType.DIVIDER, "Divider", {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(NotionWhite)
            )
        })
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        // **Non si chiude da solo al primo tocco fuori.** Lo chiudono
        // il "+" ritoccato e il tocco sul vuoto della pagina, e basta.
        //
        // La chiusura automatica c'era, e faceva una cosa sgradita: il
        // tocco fuori arrivava **anche** alla pagina sotto, che
        // spostava il cursore. Peggio, chi dei due arrivasse per primo
        // non era detto, quindi non bastava ignorare il tocco "mentre
        // il menu è aperto" — a volte il menu risultava già chiuso.
        // Chiudendolo noi, l'ordine lo decidiamo noi.
        //
        // `focusable = false` resta: un menu che prende il fuoco fa
        // chiudere la tastiera, e qui la tastiera deve restare dov'è.
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .background(DarkBackground)
                .padding(8.dp)
        ) {
            entries.chunked(2).forEach { rowEntries ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowEntries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelect(entry.type) }
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                entry.icon()
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            // Tradotti, certi nomi sono lunghi ("Elenco
                            // di cose da fare"): vanno a capo invece di
                            // spingere fuori la colonna accanto.
                            Text(
                                EditorStrings.blockType(entry.label),
                                color = NotionWhite,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (rowEntries.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Il menu che si apre scrivendo "/": tutte le cose che si possono
 * mettere in una pagina, divise per famiglia, filtrate da quello che
 * si continua a scrivere dopo la barra.
 *
 * Non è a fuoco (`focusable = false`) di proposito: la tastiera deve
 * restare aperta, perché il modo normale di usarlo è scrivere "/tab"
 * e vedere la lista stringersi.
 */
@Composable
private fun SlashMenu(
    query: String,
    onDismiss: () -> Unit,
    onSelect: (SlashEntry) -> Unit
) {
    // **Con qualcosa scritto, prima le voci che gli somigliano di più.**
    //
    // Prima il filtro teneva ogni voce che *contenesse* le lettere, e le
    // mostrava nell'ordine fisso del catalogo: scrivendo "/d" passavano
    // sedici voci — Heading, Bulleted, Numbered, Video, Code... tutte con
    // una "d" da qualche parte — e "Divider" era la settima, sotto il
    // bordo del riquadro.
    //
    // Adesso vince chi **comincia** con quelle lettere, poi chi ha una
    // **parola** che comincia così ("/list" trova "Bulleted list"), e
    // solo dopo chi le contiene soltanto. Con un filtro le famiglie non
    // si separano più: sarebbe tornato a mettere tutte le voci di base
    // davanti ai database anche quando si sta chiaramente cercando
    // "/data".
    //
    // Si cerca sia nel nome tradotto sia in quello inglese: in italiano
    // "/div" trova "Divisore", ma chi è abituato ai comandi di Notion può
    // continuare a scrivere "/divider".
    val language = AppSettings.language
    val matching = remember(query, language) {
        val q = query.trim()
        if (q.isEmpty()) {
            null
        } else {
            SLASH_ENTRIES
                .mapNotNull { entry ->
                    listOfNotNull(
                        slashMatchRank(EditorStrings.blockType(entry.label), q),
                        slashMatchRank(entry.label, q)
                    ).minOrNull()?.let { entry to it }
                }
                // A parità di somiglianza, quelle che funzionano davanti
                // a quelle ancora spente.
                .sortedWith(compareBy({ it.second }, { !it.first.enabled }))
                .map { it.first }
        }
    }

    // A ogni lettera la lista ricomincia dall'alto: la voce migliore è
    // la prima, e una lista rimasta scorsa dal filtro di prima la
    // nasconderebbe.
    val scroll = rememberScrollState()
    LaunchedEffect(query) { scroll.scrollTo(0) }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        // **Un tasto della tastiera non è un tocco "fuori".** Per Android
        // lo è: la tastiera è un'altra finestra, e un menu che si chiude
        // toccando fuori si chiudeva alla prima lettera battuta dopo la
        // barra. Scrivendo "/d" il menu spariva sulla "d", e il filtro non
        // si vedeva mai. Nelle prove era sfuggito perché la barra e le
        // lettere arrivavano da `adb input text`, che non tocca niente.
        //
        // Si chiude da solo lo stesso quando serve — scelta una voce,
        // tolta la barra, cursore spostato prima di lei o su un'altra
        // riga, uno spazio, fuoco perso: vedi `slashAnchor`.
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .heightIn(max = 360.dp)
                .background(DarkBackground)
                .verticalScroll(scroll)
                .padding(vertical = 8.dp)
        ) {
            if (matching == null) {
                SlashCategory.entries.forEach { category ->
                    Text(
                        text = EditorStrings.blockType(category.title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
                    )
                    SLASH_ENTRIES
                        .filter { it.category == category }
                        .forEach { entry -> SlashMenuRow(entry, onSelect) }
                }
            } else if (matching.isEmpty()) {
                Text(
                    text = EditorStrings.nothingWithThatName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            } else {
                matching.forEach { entry -> SlashMenuRow(entry, onSelect) }
            }
        }
    }
}

@Composable
private fun SlashMenuRow(entry: SlashEntry, onSelect: (SlashEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.enabled) { onSelect(entry) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            entry.icon,
            contentDescription = null,
            tint = if (entry.enabled) NotionWhite else NotionWhite.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = EditorStrings.blockType(entry.label),
            color = if (entry.enabled) NotionWhite else NotionWhite.copy(alpha = 0.3f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Quanto una voce del menu "/" somiglia a quello che si è scritto dopo
 * la barra: 0 se il nome comincia così, 1 se ci comincia una parola del
 * nome ("To-do list" conta "do" come parola), 2 se lo contiene soltanto,
 * null se non c'entra.
 */
private fun slashMatchRank(label: String, query: String): Int? = when {
    label.startsWith(query, ignoreCase = true) -> 0
    label.split(' ', '-').any { it.startsWith(query, ignoreCase = true) } -> 1
    label.contains(query, ignoreCase = true) -> 2
    else -> null
}

/** Le tre famiglie del menu "/", nell'ordine in cui si vedono. */
private enum class SlashCategory(val title: String) {
    BASIC("Basic blocks"),
    MEDIA("Media"),
    DATABASE("Database")
}

/**
 * Cosa fa una voce del menu "/" oltre a cambiare il tipo di blocco.
 * Le viste dei database creano un database dentro la pagina e gli
 * mettono subito la vista giusta, che è l'unica differenza fra loro.
 */
private sealed class SlashAction {
    data class Type(val type: BlockType) : SlashAction()
    object PageLink : SlashAction()
    object Divider : SlashAction()
    data class Database(val layout: DatabaseLayout?) : SlashAction()
    /** Elencata ma non ancora costruita: si vede spenta. */
    object NotYet : SlashAction()
}

private data class SlashEntry(
    val label: String,
    val category: SlashCategory,
    val action: SlashAction,
    val icon: ImageVector
) {
    val enabled: Boolean get() = action != SlashAction.NotYet
}

/**
 * Tutte le voci del menu "/", comprese **quelle che ancora non
 * esistono**.
 *
 * Elencarle spente è una scelta: dice cosa c'è e cosa manca senza
 * costringere a cercarlo, e quando una verrà costruita basterà
 * cambiarle l'azione. È l'unico posto dell'app dove una voce inerte è
 * meglio di una voce assente — altrove (le icone della barra di un
 * database, per dire) vale la regola opposta, perché lì l'utente non
 * sta scegliendo da un catalogo ma cercando un comando.
 */
private val SLASH_ENTRIES: List<SlashEntry> = listOf(
    SlashEntry("Text", SlashCategory.BASIC, SlashAction.Type(BlockType.PARAGRAPH), Icons.Filled.Notes),
    SlashEntry(
        "Bulleted list",
        SlashCategory.BASIC,
        SlashAction.Type(BlockType.BULLET_LIST_ITEM),
        Icons.Filled.FormatListBulleted
    ),
    SlashEntry(
        "Numbered list",
        SlashCategory.BASIC,
        SlashAction.Type(BlockType.NUMBERED_LIST_ITEM),
        Icons.Filled.FormatListNumbered
    ),
    SlashEntry("To-do list", SlashCategory.BASIC, SlashAction.Type(BlockType.CHECKBOX), Icons.Filled.CheckBox),
    SlashEntry("Toggle list", SlashCategory.BASIC, SlashAction.Type(BlockType.TOGGLE), Icons.Filled.ArrowRight),
    SlashEntry("Page", SlashCategory.BASIC, SlashAction.PageLink, Icons.Filled.Description),
    SlashEntry("Callout", SlashCategory.BASIC, SlashAction.NotYet, Icons.Filled.Info),
    SlashEntry("Quote", SlashCategory.BASIC, SlashAction.NotYet, Icons.Filled.FormatQuote),
    SlashEntry("Table", SlashCategory.BASIC, SlashAction.Type(BlockType.TABLE), Icons.Filled.TableChart),
    SlashEntry("Divider", SlashCategory.BASIC, SlashAction.Divider, Icons.Filled.Remove),
    SlashEntry("Link to page", SlashCategory.BASIC, SlashAction.NotYet, Icons.Filled.Link),

    SlashEntry("Image", SlashCategory.MEDIA, SlashAction.NotYet, Icons.Filled.Image),
    SlashEntry("Video", SlashCategory.MEDIA, SlashAction.NotYet, Icons.Filled.Videocam),
    SlashEntry("Audio", SlashCategory.MEDIA, SlashAction.NotYet, Icons.Filled.VolumeUp),
    SlashEntry("Code", SlashCategory.MEDIA, SlashAction.NotYet, Icons.Filled.Code),
    SlashEntry("File", SlashCategory.MEDIA, SlashAction.NotYet, Icons.Filled.AttachFile),
    SlashEntry("Web bookmark", SlashCategory.MEDIA, SlashAction.NotYet, Icons.Filled.Bookmark),

    SlashEntry(
        "Table view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.TABLE),
        Icons.Filled.TableChart
    ),
    SlashEntry(
        "Board view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.BOARD),
        Icons.Filled.ViewColumn
    ),
    SlashEntry(
        "Gallery view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.GALLERY),
        Icons.Filled.GridView
    ),
    SlashEntry(
        "List view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.LIST),
        Icons.Filled.List
    ),
    SlashEntry(
        "Calendar view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.CALENDAR),
        Icons.Filled.CalendarToday
    ),
    SlashEntry(
        "Timeline view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.TIMELINE),
        Icons.Filled.ViewTimeline
    ),
    SlashEntry(
        "Database - Inline",
        SlashCategory.DATABASE,
        SlashAction.Database(null),
        Icons.Filled.TableChart
    ),
    SlashEntry("Database - Full page", SlashCategory.DATABASE, SlashAction.NotYet, Icons.Filled.OpenInFull),
    SlashEntry("Simple database", SlashCategory.DATABASE, SlashAction.NotYet, Icons.Filled.GridOn),
    SlashEntry("Linked view of data source", SlashCategory.DATABASE, SlashAction.NotYet, Icons.Filled.Link)
)

/**
 * Mappa le posizioni tra il testo "vero" (quello che il campo modifica
 * davvero: solo il contenuto, senza prefissi) e il testo "mostrato"
 * (con • / numeri iniettati davanti a ciascuna riga, non modificabili
 * direttamente) — necessaria perché VisualTransformation aggiunge
 * caratteri che non esistono nel testo reale, non solo stili.
 * Un tocco dentro un prefisso si aggancia all'inizio del contenuto
 * vero di quella riga.
 *
 * `leading` è l'a-capo iniziale nascosto (vedi RUN_LEAD): la prima riga
 * comincia lì, non a zero. È il motivo per cui nessuna posizione
 * mostrata si traduce mai in zero — il cursore non può mettersi prima
 * di quell'a-capo, e proprio per questo "cursore a zero" è un segnale
 * inequivocabile che il backspace l'ha cancellato.
 */
private class MergedRunOffsetMapping(
    private val lineLengths: List<Int>,
    private val prefixLengths: List<Int>,
    private val leading: Int
) : OffsetMapping {
    private val realStarts: List<Int>
    private val dispStarts: List<Int>

    init {
        val rs = mutableListOf(leading)
        val ds = mutableListOf(0)
        for (i in lineLengths.indices) {
            rs.add(rs.last() + lineLengths[i] + 1)
            ds.add(ds.last() + prefixLengths[i] + lineLengths[i] + 1)
        }
        realStarts = rs
        dispStarts = ds
    }

    private fun lineIndexForReal(o: Int): Int {
        for (i in lineLengths.indices) {
            if (o <= realStarts[i] + lineLengths[i]) return i
        }
        return (lineLengths.size - 1).coerceAtLeast(0)
    }

    private fun lineIndexForDisplayed(d: Int): Int {
        for (i in lineLengths.indices) {
            if (d <= dispStarts[i] + prefixLengths[i] + lineLengths[i]) return i
        }
        return (lineLengths.size - 1).coerceAtLeast(0)
    }

    override fun originalToTransformed(offset: Int): Int {
        if (lineLengths.isEmpty()) return 0
        if (offset < leading) return 0
        val i = lineIndexForReal(offset)
        return dispStarts[i] + prefixLengths[i] + (offset - realStarts[i])
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (lineLengths.isEmpty()) return leading
        val i = lineIndexForDisplayed(offset)
        val withinPrefix = offset < dispStarts[i] + prefixLengths[i]
        return if (withinPrefix) realStarts[i] else realStarts[i] + (offset - dispStarts[i] - prefixLengths[i])
    }
}

/**
 * Il rientro è fatto di spazi dentro il prefisso, non con un
 * `ParagraphStyle`. Motivo: applicare uno stile di paragrafo a una parte
 * di un `AnnotatedString` spezza il testo in paragrafi separati, e
 * l'a-capo che finisce al confine fra due paragrafi viene reso come una
 * riga vuota in più — comparivano buchi verticali sopra e sotto ogni
 * riga rientrata. Con gli spazi il testo resta un paragrafo solo.
 */
private const val INDENT_SPACES = "    "

/**
 * I marcatori degli elenchi puntati, uno per livello di rientro, come
 * su OneNote: pallino pieno, pallino vuoto, quadratino pieno,
 * quadratino vuoto, rombo pieno, rombo vuoto, triangolino pieno,
 * triangolino vuoto — e poi si ricomincia.
 *
 * **Dipende solo dal livello di rientro**, non da cosa c'era prima:
 * così una riga riportata indietro ritrova il segno della colonna in
 * cui arriva, qualunque strada abbia fatto. Con un contatore
 * "prosegui da dove eri" due righe alla stessa altezza potrebbero
 * avere segni diversi, che è esattamente quello che non si vuole.
 *
 * Sono tutti caratteri delle varianti "piccole" dove esistono (▪ ▫ ▸
 * ▹): il quadratino vuoto deve restare distinguibile a colpo d'occhio
 * da una vera casella da spuntare, che è un riquadro disegnato e molto
 * più grande.
 */
private val BULLET_MARKERS = listOf("•", "◦", "▪", "▫", "◆", "◇", "▸", "▹")

/**
 * Quanto va scritto ogni segno perché si **vedano tutti della stessa
 * misura**.
 *
 * Questi caratteri sono disegnati molto diversi fra loro: un pallino
 * occupa meno di un terzo del suo spazio, un rombo quasi due terzi. Con
 * un'unica dimensione per tutti — com'era prima — i pallini venivano
 * minuscoli e i rombi delle macchie. Qui ognuno ha il corpo che lo
 * porta a occupare all'incirca la stessa altezza sullo schermo.
 *
 * **Nessuno supera i 20**, ed è una regola, non un caso. Il limite non
 * è la misura del testo (16) ma **l'altezza della riga**: il testo
 * scorrevole sta su righe da 24, e un carattere da 16 ne occupa in
 * altezza meno di 19, quindi c'è margine. Finché il marcatore resta
 * dentro quei 24 la riga non si allarga; oltre, si allargherebbero
 * solo le righe con il segno più grande e l'elenco verrebbe a scalini.
 */
private val BULLET_MARKER_SIZES = listOf(
    20.sp, // •  pallino pieno: il glifo è piccolo di suo, va scritto grande
    20.sp, // ◦  pallino vuoto
    18.sp, // ▪  quadratino pieno
    18.sp, // ▫  quadratino vuoto
    13.sp, // ◆  rombo pieno: il glifo è grande, va scritto piccolo
    13.sp, // ◇  rombo vuoto
    17.sp, // ▸  triangolino pieno
    17.sp  // ▹  triangolino vuoto
)

/**
 * Di quanto il segno scende rispetto alla riga di base del testo.
 *
 * È una frazione del corpo del segno, non una misura fissa: così i
 * pallini (scritti grandi) e i rombi (scritti piccoli) scendono in
 * proporzione e restano allineati fra loro. Negativo vuol dire verso il
 * basso.
 *
 * Non è un centraggio calcolato ma un ritocco a occhio, chiesto
 * guardando lo schermo: appoggiati esattamente sulla riga di base
 * sembravano un filo troppo in alto rispetto al testo.
 */
private val BULLET_MARKER_DROP = BaselineShift(-0.08f)

/**
 * Come si scrive il numero di un elenco numerato, un modo per livello
 * di rientro, come su OneNote: `1.`, `i.`, `1)`, `i)`, `a.`, `One.`,
 * `a)`, `First.` — e poi si ricomincia.
 *
 * Come per i pallini, **dipende solo dal livello**: una riga riportata
 * indietro ritrova il modo di scrivere della colonna in cui arriva.
 */
private val NUMBERED_STYLES: List<(Int) -> String> = listOf(
    { n -> "$n." },
    { n -> romanNumeral(n) + "." },
    { n -> "$n)" },
    { n -> romanNumeral(n) + ")" },
    { n -> letterOrdinal(n) + "." },
    { n -> cardinalWord(n) + "." },
    { n -> letterOrdinal(n) + ")" },
    { n -> ordinalWord(n) + "." }
)

/**
 * Il numero scritto come lo vuole il livello in cui si trova. `internal`
 * perché lo usa anche l'anteprima delle schede della galleria: un elenco
 * deve leggersi uguale nella pagina e nella scheda.
 */
internal fun numberedMarkerFor(indent: Int, ordinal: Int): String {
    val i = ((indent % NUMBERED_STYLES.size) + NUMBERED_STYLES.size) % NUMBERED_STYLES.size
    return NUMBERED_STYLES[i](ordinal.coerceAtLeast(1))
}

/**
 * `1` → `i`, `4` → `iv`, `9` → `ix`. Minuscoli, come nella foto di
 * OneNote.
 *
 * Oltre il 3999 i numeri romani non si scrivono più con le lettere
 * latine: lì si torna alle cifre, che è meglio di una riga di M.
 */
private fun romanNumeral(n: Int): String {
    if (n !in 1..3999) return n.toString()
    val values = listOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val signs = listOf("m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i")
    var left = n
    val out = StringBuilder()
    values.indices.forEach { i ->
        while (left >= values[i]) {
            out.append(signs[i])
            left -= values[i]
        }
    }
    return out.toString()
}

/**
 * `1` → `a`, `26` → `z`, `27` → `aa`: le lettere come le colonne di un
 * foglio di calcolo, così dopo la zeta si continua invece di fermarsi.
 */
private fun letterOrdinal(n: Int): String {
    if (n < 1) return "a"
    var left = n
    val out = StringBuilder()
    while (left > 0) {
        val rest = (left - 1) % 26
        out.append(('a' + rest))
        left = (left - 1) / 26
    }
    return out.reverse().toString()
}

private val CARDINAL_WORDS = listOf(
    "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
    "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
    "Eighteen", "Nineteen", "Twenty"
)

private val ORDINAL_WORDS = listOf(
    "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth",
    "Ninth", "Tenth", "Eleventh", "Twelfth", "Thirteenth", "Fourteenth", "Fifteenth",
    "Sixteenth", "Seventeenth", "Eighteenth", "Nineteenth", "Twentieth"
)

/**
 * `1` → `One`. Oltre il ventesimo si torna alla cifra: un elenco a
 * parole così lungo non si legge più, e "Thirtyseventh" occuperebbe
 * mezza riga.
 */
private fun cardinalWord(n: Int): String = CARDINAL_WORDS.getOrNull(n - 1) ?: n.toString()

/** `1` → `First`, con lo stesso limite di `cardinalWord`. */
private fun ordinalWord(n: Int): String = ORDINAL_WORDS.getOrNull(n - 1) ?: n.toString()

private fun bulletIndexFor(indent: Int): Int =
    ((indent % BULLET_MARKERS.size) + BULLET_MARKERS.size) % BULLET_MARKERS.size

/**
 * Il segno da mettere davanti a un elenco puntato rientrato di `indent`
 * livelli. `internal` per la stessa ragione di `numberedMarkerFor`.
 */
internal fun bulletMarkerFor(indent: Int): String = BULLET_MARKERS[bulletIndexFor(indent)]

/** Con che corpo va scritto quel segno. */
private fun bulletMarkerSizeFor(indent: Int): TextUnit = BULLET_MARKER_SIZES[bulletIndexFor(indent)]

/**
 * Cosa viene disegnato davanti a una riga del testo unito: gli spazi del
 * rientro e poi il marcatore (• per gli elenchi puntati, il numero per
 * quelli numerati, niente per tutto il resto). I due pezzi restano
 * distinti perché la zona toccabile deve coprire il solo marcatore, non
 * anche il rientro.
 */
private data class LinePrefix(val indent: String, val marker: String) {
    val text: String get() = indent + marker
}

/** Il tipo che erediterebbe una riga nuova creata sotto una di questo tipo: gli elenchi si propagano, i titoli no. */
private fun inheritedListType(previous: BlockType?): BlockType = when (previous) {
    BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM -> previous
    else -> BlockType.PARAGRAPH
}

// La previsione del numero per una riga appena creata non passa più da
// qui: si porta avanti il **conteggio** (vedi `carriedOrdinal` in
// `runPrefixes`) invece di provare a incrementare l'etichetta scritta.
// Con i numeri romani e "Two." non ci sarebbe niente da incrementare.

/**
 * I prefissi di tutte le righe di un gruppo.
 *
 * Le ultime righe possono non avere ancora un blocco corrispondente:
 * subito dopo un Invio la riga esiste già nel campo, ma il blocco è
 * ancora in viaggio verso il database. Per quelle il prefisso viene
 * previsto, ereditandolo dalla riga sopra con la stessa regola che usa
 * `updateRun`. Senza questa previsione la riga nuova comparirebbe per
 * un istante senza numero, con il cursore appoggiato al margine
 * sinistro, per poi saltare a destra appena il numero arriva.
 *
 * Sta qui, fuori dalla trasformazione, perché serve in due posti che
 * devono per forza essere d'accordo: chi disegna i prefissi e chi
 * calcola dove sono finiti sullo schermo per renderli toccabili. Se i
 * due divergessero, si toccherebbe un numero e ne risponderebbe un
 * altro.
 */
/**
 * Dove si trova il cursore nel testo MOSTRATO, cioè quello che
 * comprende anche i prefissi (rientri, • e numeri).
 *
 * Il layout del testo conosce solo le posizioni mostrate, mentre il
 * cursore vive in quelle vere: senza questa conversione non si può
 * sapere a che altezza dello schermo si sta scrivendo.
 */
private fun displayedCaretOffset(
    body: String,
    caretInBody: Int,
    prefixes: List<LinePrefix>
): Int {
    var bodyPos = 0
    var displayPos = 0
    body.split('\n').forEachIndexed { i, line ->
        val prefixLength = prefixes.getOrNull(i)?.text?.length ?: 0
        if (caretInBody <= bodyPos + line.length) {
            return displayPos + prefixLength + (caretInBody - bodyPos)
        }
        bodyPos += line.length + 1
        displayPos += prefixLength + line.length + 1
    }
    return displayPos
}

private fun runPrefixes(
    lines: List<String>,
    runBlocks: List<BlockEntity>,
    allBlocks: List<BlockEntity>,
    viewModel: PageEditorViewModel
): List<LinePrefix> {
    var carriedType: BlockType? = null
    var carriedIndent = 0
    var carriedOrdinal: Int? = null

    // **Dove è appena comparsa una riga che non ha ancora un blocco.**
    //
    // Subito dopo un Invio il campo ha una riga in più del database, e
    // da quel punto in giù righe e blocchi non si corrispondono più:
    // chiedendo "il blocco numero i" si otteneva il blocco della riga
    // *successiva*. Se quello era un paragrafo — il caso normale, un
    // elenco finisce quasi sempre con una riga vuota sotto — la riga
    // nuova veniva disegnata senza pallino, e il cursore si vedeva
    // scattare a inizio riga per poi tornare al suo posto appena il
    // database arrivava.
    //
    // Il punto d'inserimento si trova **dalla fine**: le righe in fondo
    // che combaciano ancora con i loro blocchi dicono quante ce ne sono
    // dopo quella nuova. Cercarlo dall'inizio non funziona, perché
    // andando a capo anche la riga divisa cambia testo.
    val insertedAt = if (lines.size == runBlocks.size + 1) {
        var matching = 0
        while (matching < runBlocks.size &&
            lines[lines.size - 1 - matching] ==
            viewModel.plainTextOf(runBlocks[runBlocks.size - 1 - matching])
        ) matching++
        lines.size - 1 - matching
    } else {
        -1
    }

    return lines.indices.map { i ->
        val block = when {
            insertedAt < 0 -> runBlocks.getOrNull(i)
            // La riga nuova: nessun blocco, il prefisso si prevede.
            i == insertedAt -> null
            // Sotto di lei tutto è sceso di uno.
            i > insertedAt -> runBlocks.getOrNull(i - 1)
            else -> runBlocks.getOrNull(i)
        }
        val type = block?.type ?: inheritedListType(carriedType)
        val indent = (block?.indentLevel ?: carriedIndent).coerceAtLeast(0)
        // A che numero è arrivata questa riga. Per una riga che il
        // database non conosce ancora — appena creata con un Invio — si
        // prevede: è quella dopo la riga sopra, se quella era un elenco
        // numerato allo stesso rientro.
        val ordinal = if (type == BlockType.NUMBERED_LIST_ITEM) {
            when {
                block != null -> viewModel.numberedListOrdinal(allBlocks, block)
                carriedOrdinal != null && carriedIndent == indent -> carriedOrdinal!! + 1
                else -> 1
            }
        } else {
            null
        }
        val label = ordinal?.let { numberedMarkerFor(indent, it) }

        carriedType = type
        carriedIndent = indent
        carriedOrdinal = ordinal

        val marker = when (type) {
            BlockType.BULLET_LIST_ITEM -> bulletMarkerFor(indent) + "  "
            BlockType.NUMBERED_LIST_ITEM -> "$label  "
            BlockType.CHECKBOX -> CHECKBOX_MARKER
            else -> ""
        }
        LinePrefix(indent = INDENT_SPACES.repeat(indent), marker = marker)
    }
}

/**
 * Colora/stila il testo unito di un gruppo di blocchi di testo
 * scorrevole: aggiunge il prefisso (• o numero) davanti a ciascuna
 * riga in base al tipo del blocco corrispondente, la dimensione/peso
 * per i titoli, e il rientro visivo (indentLevel) come stile di
 * paragrafo — tutto puramente a schermo, il testo vero sotto resta
 * semplice testo con newline a separare i blocchi.
 */
private class MergedRunVisualTransformation(
    private val runBlocks: List<BlockEntity>,
    private val allBlocks: List<BlockEntity>,
    private val viewModel: PageEditorViewModel,
    /**
     * Quanto il corpo scelto dalla barra Aa è più grande (o più piccolo)
     * del 16 di partenza: moltiplica le misure scritte qui dentro — i
     * titoli e i segni degli elenchi — che altrimenti resterebbero fisse
     * mentre il resto del testo cresce. Tocca solo `fontSize` negli
     * stili: nessun carattere in più o in meno, quindi la traduzione
     * delle posizioni non cambia.
     */
    private val scale: Float,
    /**
     * Dove sta il cursore, in posizioni del testo senza l'a-capo
     * nascosto, oppure `null` se il campo non ha il fuoco. Serve solo
     * agli spoiler: quello che il cursore tocca si scopre. Col campo
     * non a fuoco non c'è nessun cursore da guardare — la selezione
     * resta quella di prima, e senza questo `null` uno spoiler già
     * aperto sarebbe rimasto aperto anche dopo essere usciti.
     */
    private val caret: IntRange?
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val hasLead = text.text.startsWith(RUN_LEAD)
        val lines = bodyOf(text.text).split('\n')
        val prefixes = runPrefixes(lines, runBlocks, allBlocks, viewModel)
        val builder = AnnotatedString.Builder()
        val lineLengths = mutableListOf<Int>()
        val prefixLengths = mutableListOf<Int>()
        // Dove comincia la riga corrente nel testo vero, per tradurre le
        // posizioni degli span in posizioni del cursore.
        var lineBodyStart = 0

        lines.forEachIndexed { i, line ->
            val block = runBlocks.getOrNull(i)
            val prefix = prefixes[i]
            val lineStart = builder.length
            builder.append(prefix.text)
            val contentStart = builder.length
            // Il segno dell'elenco puntato è più piccolo del testo:
            // vedi `BULLET_MARKER_SIZE`. Si applica al solo marcatore,
            // non al rientro, e non cambia nessun conteggio di
            // caratteri — la traduzione delle posizioni resta intatta.
            val bulletAt = BULLET_MARKERS.indexOfFirst { prefix.marker.startsWith(it) }
            if (bulletAt >= 0) {
                // Solo il simbolo, non i due spazi che lo seguono:
                // toccando anche quelli lo stacco dal testo cambierebbe
                // e gli elenchi puntati non sarebbero più allineati con
                // quelli numerati.
                val symbolStart = lineStart + prefix.indent.length
                builder.addStyle(
                    SpanStyle(
                        fontSize = BULLET_MARKER_SIZES[bulletAt] * scale,
                        baselineShift = BULLET_MARKER_DROP
                    ),
                    symbolStart,
                    symbolStart + 1
                )
            }
            builder.append(line)
            lineLengths.add(line.length)
            prefixLengths.add(prefix.text.length)

            when (block?.type) {
                // Una casella spuntata ha il testo sbarrato e smorto,
                // come su Notion. Il quadratino invece lo disegna
                // `RunCheckboxOverlay` sopra al testo di riempimento.
                // Una casella spuntata ha il testo sbarrato e smorto,
                // come su Notion. Il quadratino invece lo disegna
                // `RunCheckboxOverlay` sopra al testo di riempimento.
                BlockType.CHECKBOX -> if (block.isChecked) {
                    builder.addStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = NotionGray400
                        ),
                        contentStart,
                        builder.length
                    )
                }
                else -> {}
            }

            // Formattazione inline (Aa): gli span di questo blocco,
            // posizionati rispetto all'inizio del suo contenuto vero
            // (dopo l'eventuale prefisso • o numero).
            //
            // **Gli span salvati sono sempre un giro indietro.** Il campo
            // ha già la lettera appena battuta, il database ce l'avrà fra
            // qualche decina di millisecondi. Prima, in quel fotogramma,
            // non si disegnava niente: per grassetto e corsivo non si
            // notava, ma uno spoiler che per un istante non viene
            // coperto **si legge** — è lo scatto per cui la parola
            // censurata riappariva a ogni tasto.
            //
            // Adesso, invece di rinunciare, si applica agli span la
            // stessa modifica che ha subito il testo: `applyTextEdit`
            // tocca solo il pezzo cambiato e restituisce span che
            // descrivono **esattamente** la riga mostrata, quindi le
            // posizioni non possono sfasarsi.
            //
            // Con un numero di righe diverso dai blocchi, però, no: lì è
            // appena nata o appena sparita una riga, la corrispondenza
            // riga-blocco non è più affidabile, e uno stile preso dal
            // blocco sbagliato sarebbe peggio di nessuno stile.
            if (block != null) {
                val stored = viewModel.spansOf(block)
                val storedText = stored.plainText()
                val blockSpans = when {
                    storedText == line -> stored
                    lines.size != runBlocks.size -> null
                    else -> applyTextEdit(stored, storedText, line)
                }
                if (blockSpans != null) {
                    var spanPos = 0
                    for (span in blockSpans) {
                        val s = contentStart + spanPos
                        val e = s + span.text.length
                        spanPos += span.text.length
                        if (span.bold || span.italic || span.underline || span.strikethrough) {
                            val decorations = mutableListOf<TextDecoration>()
                            if (span.underline) decorations.add(TextDecoration.Underline)
                            if (span.strikethrough) decorations.add(TextDecoration.LineThrough)
                            builder.addStyle(
                                SpanStyle(
                                    fontWeight = if (span.bold) FontWeight.Bold else null,
                                    fontStyle = if (span.italic) FontStyle.Italic else null,
                                    textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
                                ),
                                s,
                                e
                            )
                        }
                        // I colori **prima** dello spoiler: coperto, il
                        // velo deve vincere su tutto, altrimenti si
                        // leggerebbe il testo colorato attraverso.
                        colorStyleOf(span)?.let { builder.addStyle(it, s, e) }
                        if (span.spoiler) {
                            // Posizioni nel testo vero, che è quello in
                            // cui vive il cursore: il prefisso (• o
                            // numero) davanti alla riga esiste solo a
                            // schermo e qui va scontato.
                            val fromBody = lineBodyStart + (s - contentStart)
                            builder.addStyle(
                                spoilerStyle(revealed = touchesCaret(fromBody, fromBody + span.text.length)),
                                s,
                                e
                            )
                        }
                    }
                }
            }
            lineBodyStart += line.length + 1
            if (i < lines.size - 1) builder.append("\n")
        }
        return TransformedText(
            builder.toAnnotatedString(),
            MergedRunOffsetMapping(lineLengths, prefixLengths, if (hasLead) 1 else 0)
        )
    }

    /**
     * Vero se il cursore — o la selezione — sta **dentro** questo pezzo
     * di testo. Sui bordi no, di proposito: appena finito di scrivere un
     * `||...||` il cursore resta appoggiato subito dopo l'ultima
     * lettera, e contando anche il bordo lo spoiler sarebbe nato già
     * scoperto. Si vuole invece che si copra all'istante, come su
     * Discord. Per leggerlo basta toccarlo: un tocco cade dentro la
     * parola, non sul suo bordo.
     */
    private fun touchesCaret(start: Int, end: Int): Boolean {
        val c = caret ?: return false
        return c.first < end && c.last > start
    }
}

/**
 * Il velo che copre uno spoiler, o la traccia che resta quando è stato
 * scoperto.
 *
 * Da coperto le lettere si disegnano **trasparenti** invece di essere
 * tolte: restano dove sono, quindi la riga è larga uguale, va a capo
 * dove andava e il cursore si muove come sempre. Sopra ci passa la
 * fascia grigia, che è proprio quello che si vede su Discord.
 */
/**
 * Il colore del testo e quello dietro al testo, o null se questo pezzo
 * non ne ha nessuno dei due.
 *
 * Un esadecimale che non si riesce a leggere viene **ignorato** invece
 * di far saltare tutto: quei codici li scrive l'utente a mano, e una
 * lettera di troppo non deve far sparire il testo.
 */
private fun colorStyleOf(span: RichTextSpan): SpanStyle? {
    val text = hexToColor(span.color)
    val behind = hexToColor(span.background)
    if (text == null && behind == null) return null
    return SpanStyle(
        color = text ?: Color.Unspecified,
        background = behind ?: Color.Unspecified
    )
}

private fun spoilerStyle(revealed: Boolean): SpanStyle =
    if (revealed) {
        SpanStyle(background = SpoilerRevealed)
    } else {
        SpanStyle(color = Color.Transparent, background = SpoilerCover)
    }

/**
 * I quadratini delle caselle da spuntare, disegnati **sopra** il campo
 * di testo condiviso.
 *
 * Dentro un campo di testo non si possono mettere dei bottoni: il testo
 * riserva lo spazio con dei caratteri invisibili (`CHECKBOX_MARKER`) e
 * qui sopra ci si appoggia il quadratino vero, alla posizione che il
 * campo stesso dichiara per quel carattere. Così il quadratino resta
 * incollato alla sua riga anche quando il testo va a capo da solo o
 * cambia rientro, e ha una zona di tocco vera invece di essere un
 * carattere da centrare col dito.
 */
@Composable
private fun RunCheckboxOverlay(
    runBlocks: List<BlockEntity>,
    layout: TextLayoutResult?,
    onToggle: (BlockEntity) -> Unit
) {
    if (layout == null) return
    val density = LocalDensity.current

    // **Una fonte sola: il testo che è stato davvero misurato.**
    //
    // Prima le posizioni si ricalcolavano dal testo corrente del campo
    // e dai blocchi correnti, mentre le coordinate arrivavano dalla
    // misura del fotogramma precedente. Scrivendo, per un istante i due
    // non combaciavano: i quadratini sparivano e riapparivano spostati
    // di lato, ad ogni tasto. Cercando il marcatore dentro il testo che
    // il campo ha misurato, il disegno non può più sfasarsi.
    val laid = layout.layoutInput.text.text
    val markers = mutableListOf<Pair<Int, Int>>() // riga -> posizione del marcatore
    var lineIndex = 0
    var cursor = 0
    while (cursor <= laid.length) {
        val lineEnd = laid.indexOf('\n', cursor).let { if (it < 0) laid.length else it }
        val markerAt = laid.indexOf(CHECKBOX_MARKER, cursor)
        if (markerAt in cursor until lineEnd) markers.add(lineIndex to markerAt)
        lineIndex++
        cursor = lineEnd + 1
    }

    markers.forEach { (line, markerAt) ->
        val block = runBlocks.getOrNull(line) ?: return@forEach
        if (block.type != BlockType.CHECKBOX) return@forEach
        val box = runCatching { layout.getBoundingBox(markerAt) }.getOrNull()
            ?: return@forEach
        with(density) {
            // Centrato nel riquadro della riga. Funziona perché il
            // campo chiede al testo di **centrarsi** dentro la riga
            // alta (`lineHeightStyle`, vedi `CHECKBOX_LINE_HEIGHT`):
            // senza, lo spazio in più finirebbe tutto da una parte e il
            // quadratino scenderebbe rispetto alle lettere.
            Box(
                modifier = Modifier
                    .offset(
                        x = box.left.toDp(),
                        y = (box.top + (box.height - CHECKBOX_TOUCH_SIZE.toPx()) / 2).toDp()
                    )
                    .size(CHECKBOX_TOUCH_SIZE)
            ) {
                CheckboxMark(
                    checked = block.isChecked,
                    onToggle = { onToggle(block) },
                    lifted = false
                )
            }
        }
    }
}

/**
 * Un unico campo di testo condiviso per una sequenza consecutiva di
 * blocchi di testo scorrevole (paragrafi, titoli, elenchi puntati e
 * numerati). Selezionare e cancellare a cavallo di due blocchi qui è
 * semplicemente selezionare e cancellare del testo — lo stesso
 * meccanismo affidabile che gestisce qualsiasi modifica, non un
 * comportamento speciale da intercettare. onValueChange confronta le
 * righe prima/dopo con viewModel.updateRun, che decide se si tratta di
 * una modifica di testo semplice, di una divisione (Invio/incolla) o
 * di una fusione (backspace a inizio riga, o una selezione a cavallo
 * di più righe cancellata) — è quest'ultimo caso a far funzionare
 * backspace-tra-blocchi in modo nativo, senza intercettare nessun
 * tasto.
 *
 * Unica eccezione alla regola "il testo cambia, il resto segue": se il
 * backspace arriva a inizio di una riga che è un elemento di elenco, la
 * fusione viene rifiutata e al suo posto la riga perde il marcatore
 * (torna paragrafo), come in Notion. Il testo del campo torna quindi
 * quello di prima. Anche questo gesto è riconosciuto da come il testo è
 * cambiato, non da un evento-tasto: sulle righe dopo la prima perché
 * sparisce l'a-capo esattamente dov'è il cursore, sulla prima riga
 * perché sparisce il RUN_LEAD e il cursore finisce a zero — posizione
 * che nessun'altra azione può produrre.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MergedTextRunField(
    runBlocks: List<BlockEntity>,
    allBlocks: List<BlockEntity>,
    viewModel: PageEditorViewModel,
    focusRequestBlockId: String?,
    formatRequest: Pair<String, FormatType>?,
    onFocusChanged: (String, Boolean) -> Unit,
    /** Cosa fare con la voce scelta dal menu "/": lo sa la schermata, che può anche navigare. */
    onSlashAction: (BlockEntity, SlashAction) -> Unit
) {
    val runKey = runBlocks.first().id
    var fieldValue by remember(runKey) {
        mutableStateOf(TextFieldValue(runFieldText(runBlocks, viewModel), TextRange(1)))
    }
    var lastLineBlockId by remember(runKey) { mutableStateOf<String?>(null) }
    var fieldFocused by remember(runKey) { mutableStateOf(false) }

    // Se il contenuto "ufficiale" (dal database, via runBlocks) è
    // diverso da quello che il campo mostra localmente, risincronizza —
    // serve per Annulla/Ripristina, che cambia i blocchi da fuori
    // rispetto a questo campo. Nel caso comune (l'utente scrive, il
    // giro di andata e ritorno dal database riporta lo stesso testo
    // appena scritto) non fa nulla, lasciando intatta la selezione
    // corrente.
    // Il testo che ci aspettiamo di ricevere indietro dal database, cioè
    // l'ultimo che abbiamo inviato noi. Serve a distinguere due casi che
    // altrimenti si confondono:
    //  - il database è semplicemente INDIETRO (stiamo ancora scrivendo,
    //    il salvataggio è in viaggio): quello che torna è vecchio, va
    //    ignorato, altrimenti sovrascriverebbe quello appena digitato —
    //    è questo che faceva "perdere" un Invio, costringendo a premerlo
    //    due volte
    //  - il database è cambiato DA FUORI (Annulla/Ripristina, che
    //    riscrive i blocchi da un'altra parte): lì dobbiamo davvero
    //    adeguarci
    var pendingText by remember(runKey) { mutableStateOf<String?>(null) }

    // Annulla/Ripristina riscrive i blocchi da fuori: qualunque nostra
    // modifica in sospeso va scartata, altrimenti bloccherebbe
    // l'adeguamento al nuovo contenuto.
    val externalChangeTick by viewModel.externalChangeTick.collectAsStateWithLifecycle()
    LaunchedEffect(externalChangeTick) {
        pendingText = null
    }

    // Richiesta di formattazione dalla barra Aa: si applica se riguarda
    // uno qualsiasi dei blocchi di questo gruppo, usando la selezione
    // corrente del campo — che la barra non può conoscere, vivendo
    // fuori da qui.
    // **Dove sta la selezione, detto al ViewModel mentre si usa.**
    // Il pennello apre un pannello e il campo perde il fuoco: in quel
    // momento la selezione si chiude e non c'è più niente da colorare.
    // Segnalandola man mano, il pennello può metterla da parte
    // nell'istante in cui lo si tocca. Vedi `reportSelection`.
    LaunchedEffect(fieldValue.selection, fieldFocused, runBlocks) {
        if (!fieldFocused) return@LaunchedEffect
        val sel = fieldValue.selection
        viewModel.reportSelection(
            runBlocks.map { it.id },
            (sel.min - 1).coerceAtLeast(0),
            (sel.max - 1).coerceAtLeast(0)
        )
    }

    LaunchedEffect(formatRequest) {
        if (formatRequest != null && runBlocks.any { it.id == formatRequest.first }) {
            val sel = fieldValue.selection
            viewModel.applyFormatToRun(
                runBlocks.map { it.id },
                (sel.min - 1).coerceAtLeast(0),
                (sel.max - 1).coerceAtLeast(0),
                formatRequest.second
            )
            viewModel.consumeFormatRequest()
        }
    }

    LaunchedEffect(runBlocks) {
        val official = runFieldText(runBlocks, viewModel)
        when {
            // È arrivato indietro esattamente ciò che avevamo inviato:
            // siamo allineati, non c'è più nulla in sospeso.
            official == pendingText -> pendingText = null
            // C'è ancora una nostra modifica in viaggio e il database
            // non la riflette: quello che è arrivato è vecchio, ignoralo.
            pendingText != null -> {}
            // Nessuna modifica nostra in sospeso e il testo è diverso:
            // è cambiato davvero da fuori, adeguiamoci.
            official != fieldValue.text -> {
                val clampedCursor = fieldValue.selection.end.coerceIn(1, official.length)
                fieldValue = TextFieldValue(official, TextRange(clampedCursor))
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val focusPark = LocalFocusPark.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible

    // Serve a sapere dove sono finiti sullo schermo i numeri degli
    // elenchi, per poterli rendere toccabili (vedi più sotto), e dove
    // si trova il cursore per far scorrere la pagina fin lì.
    var textLayout by remember(runKey) { mutableStateOf<TextLayoutResult?>(null) }

    // Su quale numero è stato tenuto premuto: apre il menu della lista.
    var numberMenuFor by remember(runKey) { mutableStateOf<String?>(null) }

    // Il menu "/". `slashAnchor` è dove sta la barra dentro il testo:
    // finché c'è, quello che si scrive dopo filtra la lista. Si chiude
    // da solo quando la barra sparisce, quando il cursore va prima di
    // lei o quando si scrive uno spazio — perché a quel punto non si
    // sta più scegliendo un blocco, si sta scrivendo.
    var slashAnchor by remember(runKey) { mutableStateOf<Int?>(null) }
    var slashQuery by remember(runKey) { mutableStateOf("") }

    val caretIntoView = remember { BringIntoViewRequester() }

    // Fa scorrere la pagina dietro al cursore mentre si scrive.
    //
    // Non è ridondante: un campo di testo che **cresce in altezza**
    // invece di scorrere al proprio interno chiede di essere reso
    // visibile solo quando prende il fuoco, non ad ogni spostamento del
    // cursore. Questo campo è così, ed è alto quanto tutto il gruppo di
    // testo che contiene: scrivendo in fondo a una pagina lunga il
    // cursore finiva dietro la tastiera e la pagina restava ferma.
    //
    // Si chiede di mostrare il rettangolo del cursore, non l'intero
    // campo: quando il campo è più alto dello schermo, "mostra il
    // campo" non vuol dire niente.
    LaunchedEffect(fieldValue.selection, textLayout, imeVisible) {
        val layout = textLayout ?: return@LaunchedEffect
        // lastLineBlockId è valorizzato solo mentre questo campo ha il
        // fuoco: senza, faremmo scorrere la pagina anche per i campi su
        // cui nessuno sta scrivendo.
        if (lastLineBlockId == null) return@LaunchedEffect

        val body = bodyOf(fieldValue.text)
        val caretInBody = (fieldValue.selection.end - 1).coerceIn(0, body.length)
        val prefixes = runPrefixes(body.split('\n'), runBlocks, allBlocks, viewModel)
        val displayed = displayedCaretOffset(body, caretInBody, prefixes)
            .coerceIn(0, layout.layoutInput.text.length)
        caretIntoView.bringIntoView(layout.getCursorRect(displayed))
    }

    LaunchedEffect(focusRequestBlockId) {
        if (runBlocks.any { it.id == focusRequestBlockId }) {
            focusRequester.requestFocus()
            // **Sempre, non solo se la tastiera risulta chiusa.**
            // Quando il fuoco arriva qui dopo che è stato tolto al
            // campo di prima — è quello che succede quando una casella
            // da spuntare cambia specie — la tastiera sta ancora
            // scomparendo, e `imeVisible` dice "aperta" mentre in
            // realtà se ne sta andando: la richiesta veniva saltata e
            // la tastiera spariva in faccia a chi stava scrivendo.
            // Chiederla quando è già aperta non fa niente.
            keyboardController?.show()
            // Posiziona il cursore alla fine del blocco richiesto (il
            // caso comune: appena creato da uno split, o appena
            // raggiunto risalendo con focusLastBlock).
            val idx = runBlocks.indexOfFirst { it.id == focusRequestBlockId }
            if (idx >= 0) {
                var pos = 1
                for (i in 0 until idx) pos += viewModel.plainTextOf(runBlocks[i]).length + 1
                val lineLength = viewModel.plainTextOf(runBlocks[idx]).length
                // Chi ha chiesto il fuoco può aver chiesto anche **dove**:
                // un toggle appena tornato testo vuole il cursore a inizio
                // riga, dove si era premuto il backspace. Senza, andrebbe
                // in fondo e il backspace successivo mangerebbe l'ultima
                // lettera invece di unire la riga a quella sopra.
                val wanted = viewModel.pendingCaret.value
                    ?.takeIf { it.first == focusRequestBlockId }
                    ?.second
                pos += wanted?.coerceIn(0, lineLength) ?: lineLength
                if (wanted != null) viewModel.consumePendingCaret()
                fieldValue = fieldValue.copy(selection = TextRange(pos))
            }
            viewModel.consumeFocusRequest()
        }
    }

    fun lineIndexAt(text: String, offset: Int): Int {
        var pos = 0
        val lines = text.split('\n')
        for ((i, line) in lines.withIndex()) {
            if (offset <= pos + line.length) return i
            pos += line.length + 1
        }
        return (lines.size - 1).coerceAtLeast(0)
    }

    // Su quale blocco sta il cursore va ricalcolato anche quando cambia
    // la lista dei blocchi, non solo quando si scrive. Subito dopo un
    // Invio la riga nuova esiste già nel campo ma non ancora in
    // runBlocks: in quell'istante non si riesce ad associarla a nessun
    // blocco, e senza questo ricalcolo il "blocco a fuoco" resterebbe
    // quello di prima — così i pulsanti della barra (rientro, sposta su
    // e giù, elimina) agirebbero sulla riga sbagliata.
    LaunchedEffect(runBlocks, fieldValue.selection) {
        if (lastLineBlockId == null) return@LaunchedEffect
        val lineIndex = lineIndexAt(bodyOf(fieldValue.text), fieldValue.selection.end - 1)
        val id = runBlocks.getOrNull(lineIndex)?.id ?: return@LaunchedEffect
        if (id != lastLineBlockId) {
            lastLineBlockId?.let { onFocusChanged(it, false) }
            onFocusChanged(id, true)
            lastLineBlockId = id
        }
    }

    // Scelta una voce del menu "/", la barra e quello che le sta
    // dietro spariscono dal testo — è un comando, non roba scritta — e
    // poi si applica al blocco di quella riga.
    fun applySlashEntry(entry: SlashEntry) {
        val anchor = slashAnchor ?: return
        val body = bodyOf(fieldValue.text)
        if (anchor >= body.length) {
            slashAnchor = null
            return
        }
        val end = (anchor + 1 + slashQuery.length).coerceAtMost(body.length)
        val cleaned = body.removeRange(anchor, end)
        val lineIndex = lineIndexAt(cleaned, anchor)
        val block = runBlocks.getOrNull(lineIndex)

        val cleanedText = RUN_LEAD + cleaned
        fieldValue = TextFieldValue(cleanedText, TextRange(anchor + RUN_LEAD.length))
        pendingText = cleanedText
        viewModel.updateRun(
            runBlocks.map { it.id },
            cleaned.split('\n'),
            body.split('\n'),
            lineIndex
        )
        slashAnchor = null
        slashQuery = ""
        // Dopo la scrittura del testo, non prima: così l'ordine in cui
        // arrivano al database è quello giusto e il cambio di tipo non
        // viene riscritto sopra dal salvataggio del testo.
        if (block != null) onSlashAction(block, entry.action)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_SIDE_PADDING, vertical = 4.dp)
    ) {
    BasicTextField(
        value = fieldValue,
        onTextLayout = { textLayout = it },
        onValueChange = { newValue ->
            val oldText = fieldValue.text
            val oldBody = bodyOf(oldText)
            val newText = newValue.text
            val caretRaw = newValue.selection.end

            // Backspace a inizio della PRIMA riga. L'unico carattere che
            // può essere sparito è l'a-capo nascosto in testa, e il
            // cursore resta a zero — posizione che nessun'altra azione
            // può produrre, perché la mappatura non ci porta mai il
            // cursore. Il testo dei blocchi non è cambiato: rimettiamo
            // l'a-capo e togliamo il marcatore, se c'è.
            val atFirstLineStart = newValue.selection.collapsed &&
                caretRaw == 0 &&
                oldText.startsWith(RUN_LEAD) &&
                newText == oldBody

            if (atFirstLineStart) {
                val outcome = viewModel.backspaceAtLineStartDetailed(runBlocks.map { it.id }, 0)
                if (outcome == BackspaceOutcome.MERGED_INTO_ISLAND) {
                    // Questa riga è appena finita dentro la casella di
                    // sopra: sparisce, e con lei questo campo. Il fuoco
                    // si posa sul campo invisibile prima che succeda,
                    // altrimenti l'app si chiude (`LayoutCoordinate ...
                    // isAttached`). Era il caso più facile da incontrare:
                    // riga vuota sotto una casella, backspace.
                    focusPark?.requestFocus()
                }
                fieldValue = TextFieldValue(oldText, TextRange(1))
            } else {
                // L'a-capo iniziale dev'esserci sempre. Nei casi normali
                // c'è già e passiamo oltre il valore della tastiera così
                // com'è, senza toccarlo — importante per non disturbare
                // la composizione dell'IME.
                val normalizedText = if (newText.startsWith(RUN_LEAD)) newText else RUN_LEAD + newText
                val shift = normalizedText.length - newText.length
                val newBody = bodyOf(normalizedText)
                val caret = (caretRaw + shift - 1).coerceIn(0, newBody.length)

                // Il menu "/": si apre appena si scrive la barra, e da
                // lì in poi quello che si continua a scrivere filtra la
                // lista. Si chiude quando la barra non c'è più, quando
                // il cursore torna prima di lei o quando arriva uno
                // spazio: a quel punto non si sta più scegliendo un
                // blocco, si sta scrivendo una frase che contiene "/".
                val justTypedSlash = newBody.length == oldBody.length + 1 &&
                    caret >= 1 && caret <= newBody.length && newBody[caret - 1] == '/'
                if (justTypedSlash) {
                    slashAnchor = caret - 1
                    slashQuery = ""
                } else {
                    val anchor = slashAnchor
                    if (anchor != null) {
                        val stillOpen = anchor < newBody.length &&
                            newBody[anchor] == '/' &&
                            caret > anchor
                        val typed = if (stillOpen) {
                            newBody.substring(anchor + 1, caret.coerceAtMost(newBody.length))
                        } else {
                            ""
                        }
                        if (!stillOpen || typed.any { it == ' ' || it == '\n' }) {
                            slashAnchor = null
                            slashQuery = ""
                        } else {
                            slashQuery = typed
                        }
                    }
                }

                // "5." scritto da solo a inizio blocco: diventa un elenco
                // numerato che parte da quel numero, e il testo digitato
                // sparisce perché da lì in poi il numero lo disegna la
                // lista.
                // `||coperto||`: chiusa la seconda coppia di barre, le
                // quattro barre spariscono — erano un comando, non roba
                // scritta — e quello che stava in mezzo resta coperto.
                // Il cursore si ferma subito dopo, in chiaro.
                val spoiler = spoilerJustClosed(oldBody, newBody, caret)
                var spoilerBody: String? = null
                var spoilerCaret = 0
                if (spoiler != null) {
                    // Prima la coppia di chiusura, poi quella di
                    // apertura: togliendo l'apertura per prima, tutte le
                    // posizioni più avanti scalerebbero di due.
                    val stripped = newBody
                        .removeRange(spoiler.contentEnd, spoiler.contentEnd + 2)
                        .removeRange(spoiler.open, spoiler.open + 2)
                    val from = spoiler.open
                    val to = spoiler.contentEnd - 2
                    val lineStart = stripped.lastIndexOf('\n', from - 1) + 1
                    val lineEnd = stripped.indexOf('\n', from)
                        .let { if (it < 0) stripped.length else it }
                    val applied = viewModel.applySpoilerAtLine(
                        runBlocks.map { it.id },
                        stripped.take(lineStart).count { it == '\n' },
                        stripped.substring(lineStart, lineEnd),
                        from - lineStart,
                        to - lineStart
                    )
                    if (applied) {
                        spoilerBody = stripped
                        spoilerCaret = to
                    }
                }

                val listPrefix = numberedListPrefixJustTyped(oldBody, newBody, caret)
                val listStarted = listPrefix != null &&
                    viewModel.startNumberedListAtLine(
                        runBlocks.map { it.id },
                        listPrefix.lineIndex,
                        listPrefix.startNumber
                    )

                // Backspace a inizio di una riga successiva: lì il testo
                // cambia davvero (sparisce l'a-capo sopra). Se la riga ha
                // ancora un marcatore o un rientro da perdere, la fusione
                // va rifiutata in favore di quello.
                val backspacedLine = backspacedLineStart(oldBody, newBody, caret)
                val stepUndone = backspacedLine > 0 &&
                    viewModel.backspaceAtLineStartDetailed(
                        runBlocks.map { it.id },
                        backspacedLine
                    ) != BackspaceOutcome.NOTHING

                if (spoilerBody != null) {
                    val spoilerText = RUN_LEAD + spoilerBody
                    fieldValue = TextFieldValue(
                        spoilerText,
                        TextRange(RUN_LEAD.length + spoilerCaret)
                    )
                    pendingText = spoilerText
                } else if (listStarted && listPrefix != null) {
                    val strippedBody = newBody.removeRange(listPrefix.lineStart, caret)
                    val strippedText = RUN_LEAD + strippedBody
                    fieldValue = TextFieldValue(strippedText, TextRange(listPrefix.lineStart + 1))
                    pendingText = strippedText
                } else if (stepUndone) {
                    val oldLines = oldBody.split('\n')
                    var caretAtLineStart = 1
                    for (i in 0 until backspacedLine) caretAtLineStart += oldLines[i].length + 1
                    fieldValue = TextFieldValue(oldText, TextRange(caretAtLineStart))
                } else {
                    fieldValue = if (shift == 0) {
                        newValue
                    } else {
                        TextFieldValue(
                            normalizedText,
                            TextRange(newValue.selection.start + 1, caretRaw + 1)
                        )
                    }
                    pendingText = normalizedText
                    // L'indice riga si calcola sul testo appena digitato,
                    // non sulle lunghezze salvate nel database tramite
                    // runBlocks — altrimenti, mentre si scrive
                    // velocemente, "su quale riga è il cursore" può
                    // risultare sbagliato per una frazione di secondo,
                    // prima che il giro di andata e ritorno dal database
                    // si allinei di nuovo.
                    val lineIndex = lineIndexAt(newBody, caret)

                    viewModel.updateRun(
                        runBlocks.map { it.id },
                        newBody.split('\n'),
                        // Quello che c'era nel campo un attimo fa: è la
                        // sola versione che corrisponde a ciò che
                        // l'utente vedeva quando ha premuto il tasto.
                        oldBody.split('\n'),
                        // E dov'è il cursore: senza, una riga vuota
                        // inserita in mezzo ad altre righe vuote è
                        // indistinguibile da una aggiunta in fondo.
                        lineIndex
                    )

                    val currentLineId = runBlocks.getOrNull(lineIndex)?.id
                    if (currentLineId != null && currentLineId != lastLineBlockId) {
                        lastLineBlockId?.let { onFocusChanged(it, false) }
                        onFocusChanged(currentLineId, true)
                        lastLineBlockId = currentLineId
                    }
                }
            }
        },
        // Se nel gruppo c'è anche una sola casella da spuntare, le
        // righe stanno più larghe: il quadratino è piccolo e attaccate
        // è facile spuntare quella sbagliata. Vale per tutto il campo,
        // non riga per riga — vedi `CHECKBOX_LINE_HEIGHT` per il
        // perché.
        // Font e corpo della pagina (barra Aa) applicati al campo intero:
        // `apply` porta in proporzione anche l'altezza di riga qui sotto,
        // così a 16 tutto resta com'era e a ogni altro corpo le righe
        // crescono insieme al testo. È lo stesso posto — il `textStyle`
        // dell'intero campo — dove il README dice che le misure si
        // possono toccare senza rischi.
        textStyle = LocalPageTypography.current.apply(MaterialTheme.typography.bodyLarge.copy(
            color = NotionWhite,
            // **L'altezza della riga è sempre fissata.**
            //
            // Lasciandola decidere al carattere, ogni riga diventava
            // alta quanto il segno più grande che conteneva: le righe
            // coi pallini (scritti a 20) erano più alte di quelle coi
            // rombi (scritti a 13), e l'elenco veniva a passo
            // irregolare — i segni sembravano storti, ma a essere
            // storte erano le righe. Fissandola, tutte le righe sono
            // alte uguale e i segni si appoggiano tutti allo stesso
            // modo, qualunque corpo abbiano.
            lineHeight = if (runBlocks.any { it.type == BlockType.CHECKBOX }) {
                CHECKBOX_LINE_HEIGHT
            } else {
                RUN_LINE_HEIGHT
            },
            // **Il testo si centra nella riga alta.** Di suo Compose
            // spartisce lo spazio in più in proporzione a quanto le
            // lettere salgono e scendono, e il risultato è che il testo
            // non sta in mezzo: i quadratini, che invece stanno in
            // mezzo, risultavano disallineati. Chiedendo il centro,
            // testo e quadratino coincidono per costruzione, qualunque
            // sia l'altezza di riga.
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )),
        cursorBrush = SolidColor(NotionWhite),
        // Pagina bloccata: si legge e si seleziona, non si scrive.
        // `readOnly` e non `enabled = false` di proposito — disabilitato
        // il testo non si potrebbe nemmeno selezionare per copiarlo, e
        // bloccare una pagina serve a non rovinarla, non a non usarla.
        readOnly = LocalPageLocked.current,
        // Vedi il commento gemello sui blocchi isola: la maiuscola a
        // inizio frase va chiesta, altrimenti non arriva.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        visualTransformation = MergedRunVisualTransformation(
            runBlocks,
            allBlocks,
            viewModel,
            scale = LocalPageTypography.current.scale,
            // Il cursore serve solo a scoprire gli spoiler che tocca, e
            // senza fuoco non c'è nessun cursore da guardare.
            caret = if (fieldFocused) {
                (fieldValue.selection.min - 1).coerceAtLeast(0)..
                    (fieldValue.selection.max - 1).coerceAtLeast(0)
            } else {
                null
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(caretIntoView)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                fieldFocused = state.isFocused
                if (state.isFocused) {
                    val lineIndex = lineIndexAt(bodyOf(fieldValue.text), fieldValue.selection.end - 1)
                    val id = runBlocks.getOrNull(lineIndex)?.id ?: runBlocks.first().id
                    lastLineBlockId = id
                    onFocusChanged(id, true)
                } else {
                    lastLineBlockId?.let { onFocusChanged(it, false) }
                    lastLineBlockId = null
                    // Perso il fuoco non si sta più scegliendo niente.
                    slashAnchor = null
                }
            }
    )

    // **Dopo** il campo, non prima: dentro un `Box` i tocchi li riceve
    // per primo l'ultimo figlio, e messi prima i quadratini non
    // rispondevano — il tocco andava tutto al testo.
    RunCheckboxOverlay(
        runBlocks = runBlocks,
        layout = textLayout,
        onToggle = { viewModel.toggleCheckbox(it) }
    )

    if (slashAnchor != null) {
        SlashMenu(
            query = slashQuery,
            onDismiss = { slashAnchor = null },
            onSelect = { entry -> applySlashEntry(entry) }
        )
    }

    // Tenendo premuto sul numero di un elenco numerato si apre il menu
    // per rimaneggiare la lista (far ripartire il conteggio, rimetterlo
    // in fila, togliere il numero, passare al pallino) — le stesse
    // quattro voci di OneNote. Il numero è disegnato dalla
    // trasformazione visiva, quindi non è un elemento a sé che si possa
    // rendere toccabile: gli mettiamo sopra una zona invisibile,
    // posizionata dove il layout del testo dice che il numero sta.
    // Una zona sovrapposta riceve i tocchi solo dentro i propri confini,
    // quindi tutto il resto del campo continua a comportarsi come prima.
    val layout = textLayout
    if (layout != null) {
        val density = LocalDensity.current
        val lines = bodyOf(fieldValue.text).split('\n')
        val prefixes = runPrefixes(lines, runBlocks, allBlocks, viewModel)
        var displayStart = 0
        lines.forEachIndexed { i, line ->
            val block = runBlocks.getOrNull(i)
            val prefix = prefixes[i]
            val markerStart = displayStart + prefix.indent.length
            val markerEnd = displayStart + prefix.text.length
            if (block != null &&
                block.type == BlockType.NUMBERED_LIST_ITEM &&
                prefix.marker.isNotEmpty() &&
                markerEnd <= layout.layoutInput.text.length
            ) {
                val textLine = layout.getLineForOffset(markerStart)
                val left = layout.getHorizontalPosition(markerStart, true)
                val right = layout.getHorizontalPosition(markerEnd, true)
                val top = layout.getLineTop(textLine)
                val bottom = layout.getLineBottom(textLine)
                with(density) {
                    Box(
                        modifier = Modifier
                            .offset(x = left.toDp(), y = top.toDp())
                            .size(width = (right - left).toDp(), height = (bottom - top).toDp())
                            .pointerInput(block.id) {
                                // Solo il tocco prolungato. Prima un
                                // tocco semplice faceva ripartire la
                                // numerazione: comodo da scoprire,
                                // scomodo da subire, perché rinumerava
                                // tutta la lista per un dito appoggiato
                                // male. Ora quella è una delle voci del
                                // menu, dove la si sceglie apposta.
                                detectTapGestures(
                                    onLongPress = { numberMenuFor = block.id }
                                )
                            }
                    )
                }
            }
            displayStart += prefix.text.length + line.length + 1
        }
    }
    }

    val menuBlockId = numberMenuFor
    if (menuBlockId != null) {
        NumberedListMenu(
            hasReset = runBlocks.find { it.id == menuBlockId }?.numberStartsAt != null,
            onDismiss = { numberMenuFor = null },
            onBeginNewList = { viewModel.beginNewListHere(menuBlockId) },
            onContinuePrevious = { viewModel.continuePreviousList(menuBlockId) },
            onRemoveNumber = { viewModel.removeNumber(menuBlockId) },
            onChangeToBullet = { viewModel.changeToBullet(menuBlockId) }
        )
    }
}

/**
 * Il menu del numero di un elenco, tenendo premuto sopra: le stesse
 * quattro voci di OneNote.
 *
 * "Renumber as continuation" c'è anche quando non serve — cioè quando
 * la riga non fa ripartire niente — ma spenta: toglierla del tutto
 * farebbe ballare il menu fra un'apertura e l'altra, e una voce
 * grigia dice anche *perché* non si può fare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberedListMenu(
    hasReset: Boolean,
    onDismiss: () -> Unit,
    onBeginNewList: () -> Unit,
    onContinuePrevious: () -> Unit,
    onRemoveNumber: () -> Unit,
    onChangeToBullet: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = EditorStrings.blockType("Numbered list"),
                style = MaterialTheme.typography.titleMedium,
                color = NotionWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            NumberedListMenuItem(EditorStrings.beginNewList) {
                onBeginNewList()
                onDismiss()
            }
            NumberedListMenuItem(
                label = EditorStrings.renumberContinuation,
                enabled = hasReset
            ) {
                onContinuePrevious()
                onDismiss()
            }
            NumberedListMenuItem(EditorStrings.removeNumber) {
                onRemoveNumber()
                onDismiss()
            }
            NumberedListMenuItem(EditorStrings.changeToBullet) {
                onChangeToBullet()
                onDismiss()
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NumberedListMenuItem(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) NotionWhite else NotionWhite.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp)
    )
}

/**
 * Row for a single "island" block (checkbox, toggle and its children,
 * table, divider, page/database link) — renderizzazione a campo
 * singolo invariata rispetto a prima. Usata sia per le isole di primo
 * livello sia, ricorsivamente, per i figli di un TOGGLE (che per ora
 * restano blocchi separati anche se di tipo testo scorrevole — unirli
 * in un campo condiviso anche lì è un passo successivo).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockRow(
    block: BlockEntity,
    allBlocks: List<BlockEntity>,
    depth: Int,
    viewModel: PageEditorViewModel,
    factory: ViewModelFactory,
    focusRequestBlockId: String?,
    formatRequest: Pair<String, FormatType>?,
    onFocusChanged: (String, Boolean) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit
) {
    if (block.type == BlockType.TABLE) {
        TableBlockContent(block = block, depth = depth, viewModel = viewModel)
        return
    }

    // Un database dentro una pagina si vede e si modifica lì, senza
    // aprirlo: è il blocco stesso. Per vederlo a schermo intero c'è il
    // menu "..." accanto al suo nome.
    val databasePageId = block.linkedPageId
    if (block.type == BlockType.DATABASE_LINK && databasePageId != null) {
        var confirmDatabaseDeletion by remember(block.id) { mutableStateOf(false) }

        DatabaseContent(
            pageId = databasePageId,
            factory = factory,
            embedded = true,
            onOpenRowPage = onNavigateToPage,
            onOpenFullPage = { onNavigateToDatabase(databasePageId) },
            onDelete = { confirmDatabaseDeletion = true },
            // "Turn into page": il blocco diventa un collegamento e il
            // database si apre subito a schermo intero, che è come lo si
            // vedrà da qui in poi. Per rimetterlo dentro la pagina c'è
            // "Turn into database" nei tre puntini, da lì.
            onTurnIntoPage = {
                viewModel.turnDatabaseIntoPage(block) { onNavigateToDatabase(databasePageId) }
            },
            // Nessun margine laterale, a differenza degli altri blocchi:
            // una tabella deve poter usare tutta la larghezza, e il
            // rientro della pagina la farebbe partire staccata dal bordo
            // mentre a schermo intero è attaccata.
            modifier = Modifier.padding(start = (depth * 20).dp, top = 8.dp, bottom = 8.dp)
        )

        if (confirmDatabaseDeletion) {
            AlertDialog(
                onDismissRequest = { confirmDatabaseDeletion = false },
                title = { Text(EditorStrings.deleteDatabaseTitle) },
                text = { Text(EditorStrings.deleteDatabaseText) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteDatabaseBlock(block)
                        confirmDatabaseDeletion = false
                    }) { Text(Strings.delete) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDatabaseDeletion = false }) { Text(Strings.cancel) }
                }
            )
        }
        return
    }

    if (block.type == BlockType.PAGE_LINK || block.type == BlockType.DATABASE_LINK) {
        PageLinkBlockContent(
            block = block,
            depth = depth,
            viewModel = viewModel,
            onNavigateToPage = onNavigateToPage,
            onNavigateToDatabase = onNavigateToDatabase
        )
        return
    }

    if (block.type == BlockType.DIVIDER) {
        DividerBlockContent(block = block, depth = depth, viewModel = viewModel)
        return
    }

    // Il testo del campo comincia con un a-capo nascosto, come il campo
    // unito (vedi `RUN_LEAD`): è l'unico modo per accorgersi del
    // backspace premuto a inizio riga. La tastiera non manda eventi
    // tasto che si possano intercettare — provato e documentato — ma
    // se il backspace si mangia quell'a-capo il testo cambia, e un
    // cambiamento si vede. Il carattere dev'essere un a-capo e non
    // qualcosa di invisibile: uno zero-width space la tastiera se lo
    // ingloba nella parola che sta componendo.
    var editState by remember(block.id) {
        val spans = viewModel.spansOf(block)
        val body = spans.plainText()
        mutableStateOf(
            BlockEditState(
                spans,
                TextFieldValue(RUN_LEAD + body, TextRange(RUN_LEAD.length + body.length))
            )
        )
    }
    // L'ultima divisione fatta da questo campo: serve a riconoscere
    // l'eco della tastiera (vedi il commento dentro onValueChange).
    var lastSplit by remember(block.id) { mutableStateOf<SplitEcho?>(null) }
    var fieldFocused by remember(block.id) { mutableStateOf(false) }
    // Su quale numero è stato tenuto premuto: apre il menu della lista.
    var numberMenuFor by remember(block.id) { mutableStateOf<String?>(null) }

    // Quando il testo del blocco cambia da fuori — la riga di sotto che
    // ci si fonde dentro col backspace — il campo deve rileggerlo.
    // Solo mentre non ha il fuoco: risincronizzarlo mentre si scrive
    // vorrebbe dire litigare con la tastiera ad ogni carattere.
    // Anche quando cambia **solo la formattazione** e non il testo: è
    // il caso del colore messo dal pennello, che scrive nel database
    // mentre questo campo non ha il fuoco. Guardando il solo testo la
    // riga sarebbe rimasta del colore di prima fino alla battuta
    // successiva.
    val storedText = viewModel.plainTextOf(block)
    val storedSpans = viewModel.spansOf(block)
    LaunchedEffect(block.id, storedText, storedSpans, fieldFocused) {
        if (fieldFocused) return@LaunchedEffect
        if (bodyOf(editState.fieldValue.text) == storedText &&
            editState.spans == storedSpans
        ) {
            return@LaunchedEffect
        }
        val spans = storedSpans
        val caret = viewModel.pendingCaret.value
            ?.takeIf { it.first == block.id }
            ?.second
            ?: storedText.length
        editState = BlockEditState(
            spans,
            TextFieldValue(
                RUN_LEAD + storedText,
                TextRange(RUN_LEAD.length + caret.coerceIn(0, storedText.length))
            )
        )
        viewModel.consumePendingCaret()
    }
    val focusRequester = remember { FocusRequester() }
    val focusPark = LocalFocusPark.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(focusRequestBlockId) {
        if (focusRequestBlockId == block.id) {
            // Se chi chiede il fuoco dice anche dove mettere il cursore —
            // salendo col backspace da una riga vuota, in fondo a questa —
            // va messo prima di prendere il fuoco: dopo, la tastiera
            // partirebbe dal punto vecchio e lo si vedrebbe saltare.
            viewModel.pendingCaret.value
                ?.takeIf { it.first == block.id }
                ?.let { (_, at) ->
                    val body = bodyOf(editState.fieldValue.text)
                    editState = editState.copy(
                        fieldValue = editState.fieldValue.copy(
                            selection = TextRange(RUN_LEAD.length + at.coerceIn(0, body.length))
                        )
                    )
                    viewModel.consumePendingCaret()
                }
            focusRequester.requestFocus()
            // Vedi il commento gemello nel campo unito: va chiesta
            // sempre, perché subito dopo un cambio di fuoco la
            // tastiera risulta ancora aperta mentre sta sparendo.
            keyboardController?.show()
            viewModel.consumeFocusRequest()
        }
    }

    // Vedi il gemello nel campo unito: la selezione va segnalata mentre
    // la si ha, perché il pennello se la porta via il fuoco.
    LaunchedEffect(editState.fieldValue.selection, fieldFocused) {
        if (!fieldFocused) return@LaunchedEffect
        val sel = editState.fieldValue.selection
        viewModel.reportSelection(
            listOf(block.id),
            (sel.min - RUN_LEAD.length).coerceAtLeast(0),
            (sel.max - RUN_LEAD.length).coerceAtLeast(0)
        )
    }

    LaunchedEffect(formatRequest) {
        if (formatRequest?.first == block.id) {
            // **Meno l'a-capo nascosto.** Gli span contano il testo vero,
            // il campo conta anche il carattere in testa che non si
            // vede: senza questo scarto la formattazione cadeva su una
            // lettera prima di quella selezionata.
            val sel = editState.fieldValue.selection
            val from = (sel.min - RUN_LEAD.length).coerceAtLeast(0)
            val to = (sel.max - RUN_LEAD.length).coerceAtLeast(0)
            val newSpans = when (formatRequest.second) {
                FormatType.BOLD -> toggleFormatInRange(editState.spans, from, to, { it.bold }) { s, v -> s.copy(bold = v) }
                FormatType.ITALIC -> toggleFormatInRange(editState.spans, from, to, { it.italic }) { s, v -> s.copy(italic = v) }
                FormatType.UNDERLINE -> toggleFormatInRange(editState.spans, from, to, { it.underline }) { s, v -> s.copy(underline = v) }
                FormatType.STRIKETHROUGH -> toggleFormatInRange(editState.spans, from, to, { it.strikethrough }) { s, v -> s.copy(strikethrough = v) }
                FormatType.SPOILER -> toggleFormatInRange(editState.spans, from, to, { it.spoiler }) { s, v -> s.copy(spoiler = v) }
            }
            editState = editState.copy(spans = newSpans)
            viewModel.updateBlockSpans(block, newSpans)
            viewModel.consumeFormatRequest()
        }
    }

    // Font e corpo della pagina (barra Aa), come nel testo condiviso.
    val pageTypography = LocalPageTypography.current
    val textStyle = pageTypography.apply(MaterialTheme.typography.bodyLarge).let { base ->
        if (block.type == BlockType.CHECKBOX && block.isChecked) {
            base.copy(textDecoration = TextDecoration.LineThrough)
        } else base
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_SIDE_PADDING + NESTED_INDENT * depth, end = PAGE_SIDE_PADDING, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        when (block.type) {
            BlockType.TOGGLE -> {
                // **Triangolino verso destra da chiuso, verso il basso da
                // aperto**, come su Notion. Prima erano le frecce a
                // parentesi di Material, e da aperto puntava **in su**:
                // il segno diceva l'opposto di quello che si vedeva.
                IconButton(
                    onClick = { viewModel.toggleExpanded(block) },
                    modifier = Modifier.size(TOGGLE_ARROW_SIZE)
                ) {
                    Icon(
                        imageVector = if (block.isExpanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowRight,
                        contentDescription = if (block.isExpanded) Strings.collapse else Strings.expand,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(TOGGLE_ARROW_GAP))
            }

            BlockType.BULLET_LIST_ITEM -> {
                // Stessa sequenza del testo condiviso: un punto dentro
                // un toggle non deve avere un segno diverso da uno
                // fuori, allo stesso rientro.
                Text(
                    bulletMarkerFor(block.indentLevel),
                    style = textStyle.copy(
                        fontSize = pageTypography.scaled(bulletMarkerSizeFor(block.indentLevel)),
                        baselineShift = BULLET_MARKER_DROP
                    ),
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
            }

            BlockType.NUMBERED_LIST_ITEM -> {
                Text(
                    // Stesso modo di scrivere del testo condiviso: un
                    // numero dentro un toggle non deve essere scritto
                    // diversamente da uno fuori, allo stesso rientro.
                    numberedMarkerFor(
                        block.indentLevel,
                        viewModel.numberedListOrdinal(allBlocks, block)
                    ),
                    style = textStyle,
                    modifier = Modifier
                        .padding(end = 8.dp, top = 2.dp)
                        // Stesso menu del numero nel testo unito: qui
                        // ci si arriva solo per un elenco numerato
                        // dentro un toggle, ma il gesto dev'essere
                        // quello, non un altro.
                        .pointerInput(block.id) {
                            detectTapGestures(
                                onLongPress = { numberMenuFor = block.id }
                            )
                        }
                )
            }

            BlockType.CHECKBOX -> {
                CheckboxMark(
                    checked = block.isChecked,
                    onToggle = { viewModel.toggleCheckbox(block) }
                )
                Spacer(modifier = Modifier.size(8.dp))
            }

            else -> { /* PARAGRAPH has no prefix */ }
        }

        BasicTextField(
            value = editState.fieldValue,
            onValueChange = { newFieldValue ->
                val oldText = bodyOf(editState.fieldValue.text)
                noteLog(
                    "casella ${block.id.takeLast(4)} scrive: " +
                        "'${oldText.forLog()}' -> '${bodyOf(newFieldValue.text).forLog()}' " +
                        "cursore=${newFieldValue.selection.start}..${newFieldValue.selection.end} " +
                        "composizione=${newFieldValue.composition} fuoco=$fieldFocused"
                )

                if (!newFieldValue.text.startsWith(RUN_LEAD)) {
                    // È sparito l'a-capo nascosto: significa backspace a
                    // inizio riga, l'unica cosa che può mangiarselo.
                    // Sulla casella da spuntare toglie la casella e
                    // lascia il testo; da lì in poi la riga è testo
                    // normale e si fonde con quella sopra come tutte.
                    val unmade = viewModel.backspaceAtIslandStart(block)
                    editState = editState.copy(
                        fieldValue = TextFieldValue(
                            RUN_LEAD + oldText,
                            TextRange(RUN_LEAD.length)
                        )
                    )
                    if (unmade) {
                        // Il blocco cambia specie e questa casella di
                        // scrittura sparisce. Distruggerla mentre ha il
                        // fuoco fa chiudere l'app, quindi il fuoco va
                        // tolto — ma **non staccando la tastiera**:
                        // `clearFocus()` la chiude e la si vede sparire
                        // e tornare. Si posa invece sul campo
                        // invisibile, che non muore mai, e riparte da lì
                        // sul blocco nuovo.
                        focusPark?.requestFocus()
                    }
                    return@BasicTextField
                }

                val newText = bodyOf(newFieldValue.text)
                if (newText.contains('\n')) {
                    val lines = newText.split('\n')
                    val movedAway = lines.drop(1).joinToString("\n")
                    val now = System.currentTimeMillis()
                    val echo = lastSplit
                    // L'eco della tastiera. Appena diviso il blocco gli
                    // togliamo da sotto il testo che è passato alla riga
                    // nuova; la tastiera Samsung, che aveva ancora quel
                    // testo in mano, a volte lo rimanda indietro con
                    // dentro lo stesso a-capo. Preso alla lettera è un
                    // secondo Invio, e lasciava una riga vuota di troppo.
                    //
                    // Due modi di riconoscerlo, misurati sul telefono:
                    // torna **lo stesso pezzo** da spostare (un Invio
                    // vero non se lo riporterebbe dietro, quel testo è
                    // già altrove), oppure torna **la stessa identica
                    // modifica** a distanza di pochi millisecondi, che
                    // è meno di quanto ci metta un dito a premere due
                    // volte.
                    val transition = oldText to newText
                    val samePayload = movedAway.isNotEmpty() &&
                        echo?.payload == movedAway &&
                        now - echo.at < SPLIT_ECHO_MS
                    val sameEditTwice = echo?.transition == transition &&
                        now - echo.at < DUPLICATE_EDIT_MS
                    if (samePayload || sameEditTwice) {
                        val current = editState.spans.plainText()
                        editState = editState.copy(
                            fieldValue = TextFieldValue(
                                RUN_LEAD + current,
                                TextRange(RUN_LEAD.length + current.length)
                            )
                        )
                        return@BasicTextField
                    }
                    lastSplit = SplitEcho(movedAway, transition, now)
                    val firstLine = lines.first()
                    val spansForFirstLine = applyTextEdit(editState.spans, oldText, firstLine)
                    if (block.type == BlockType.TOGGLE && lines.size == 2) {
                        // **Invio nel titolo di un toggle**: la riga nuova
                        // va dentro (toggle aperto) o sotto (chiuso), e il
                        // titolo resta dov'è. Trattato come un paragrafo
                        // qualsiasi, il titolo finiva in una riga nuova
                        // sopra e il toggle restava vuoto. Vedi
                        // `enterInToggle`.
                        editState = BlockEditState(
                            spansForFirstLine,
                            TextFieldValue(
                                RUN_LEAD + firstLine,
                                TextRange(RUN_LEAD.length + firstLine.length)
                            )
                        )
                        viewModel.enterInToggle(block, spansForFirstLine, lines[1])
                    } else if (lines.size == 2) {
                        val outcome = viewModel.splitBlockAt(block, spansForFirstLine, lines[1])
                        // La riga nuova è finita sopra e questo campo
                        // resta vivo col fuoco: deve mostrare il testo
                        // dopo il cursore, col cursore all'inizio. È
                        // quello che si vede quando si va a capo.
                        noteLog(
                            "casella ${block.id.takeLast(4)} a-capo: esito=$outcome " +
                                "prima='${firstLine.forLog()}' dopo='${lines[1].forLog()}'"
                        )
                        editState = if (outcome == SplitOutcome.NOTHING) {
                            // Il ViewModel ha rifiutato la divisione
                            // perché il campo si credeva vuoto mentre nel
                            // database c'era del testo. Si rimette il
                            // campo com'è davvero, invece di lasciarlo
                            // vuoto: era così che il testo "spariva".
                            val real = viewModel.spansOf(block)
                            val realText = real.plainText()
                            BlockEditState(
                                real,
                                TextFieldValue(
                                    RUN_LEAD + realText,
                                    TextRange(RUN_LEAD.length + realText.length)
                                )
                            )
                        } else if (outcome == SplitOutcome.SPLIT_ABOVE) {
                            BlockEditState(
                                listOf(RichTextSpan(text = lines[1])),
                                TextFieldValue(RUN_LEAD + lines[1], TextRange(RUN_LEAD.length))
                            )
                        } else {
                            BlockEditState(
                                spansForFirstLine,
                                TextFieldValue(
                                    RUN_LEAD + firstLine,
                                    TextRange(RUN_LEAD.length + firstLine.length)
                                )
                            )
                        }
                        if (outcome == SplitOutcome.BECAME_PARAGRAPH) {
                            // Invio su una casella vuota: il blocco
                            // diventa un paragrafo e questa riga viene
                            // distrutta, esattamente come col backspace
                            // a inizio casella. Stesso rimedio: il fuoco
                            // si posa sul campo invisibile invece di
                            // staccare la tastiera.
                            focusPark?.requestFocus()
                        }
                    } else {
                        // Più righe in un colpo solo (un incolla): le
                        // altre diventano blocchi sotto e questo tiene
                        // la prima, come sempre.
                        editState = BlockEditState(
                            spansForFirstLine,
                            TextFieldValue(
                                RUN_LEAD + firstLine,
                                TextRange(RUN_LEAD.length + firstLine.length)
                            )
                        )
                        viewModel.splitBlockAtMultiple(block, lines)
                    }
                } else if (newText == oldText) {
                    editState = editState.copy(fieldValue = newFieldValue)
                } else {
                    // **Da qui in poi non c'è più nessuna eco da
                    // aspettarsi.** L'eco della tastiera, se arriva,
                    // arriva subito dopo la divisione e senza niente in
                    // mezzo: appena viene battuto un carattere vero, la
                    // divisione di prima è acqua passata.
                    //
                    // Senza questa riga il riconoscimento restava armato
                    // sul campo, che adesso sopravvive a tutte le
                    // divisioni di fila (prima il fuoco se ne andava su
                    // un campo nuovo e la memoria ripartiva da zero).
                    // Scrivendo in fretta due righe che finiscono con lo
                    // stesso testo, il secondo Invio veniva scambiato
                    // per un'eco e **non andava a capo**.
                    lastSplit = null
                    // Stessa scorciatoia del testo condiviso: `||x||`
                    // toglie le barre e copre quello che c'era in mezzo.
                    val spoiler = spoilerJustClosed(
                        oldText,
                        newText,
                        (newFieldValue.selection.end - RUN_LEAD.length).coerceAtLeast(0)
                    )
                    if (spoiler != null) {
                        val stripped = newText
                            .removeRange(spoiler.contentEnd, spoiler.contentEnd + 2)
                            .removeRange(spoiler.open, spoiler.open + 2)
                        val to = spoiler.contentEnd - 2
                        val newSpans = setFormatInRange(
                            applyTextEdit(editState.spans, oldText, stripped),
                            spoiler.open,
                            to
                        ) { s, _ -> s.copy(spoiler = true) }
                        editState = BlockEditState(
                            newSpans,
                            TextFieldValue(RUN_LEAD + stripped, TextRange(RUN_LEAD.length + to))
                        )
                        viewModel.updateBlockSpans(block, newSpans)
                    } else {
                        val newSpans = applyTextEdit(editState.spans, oldText, newText)
                        editState = BlockEditState(newSpans, newFieldValue)
                        viewModel.updateBlockSpans(block, newSpans)
                    }
                }
            },
            textStyle = textStyle.copy(color = NotionWhite),
            cursorBrush = SolidColor(NotionWhite),
            // Vedi il gemello nel campo unito: bloccata si legge, non si
            // scrive.
            readOnly = LocalPageLocked.current,
            // La maiuscola a inizio frase la mette la tastiera, ma solo
            // se glielo si chiede: senza, ogni casella da spuntare
            // cominciava minuscola mentre nel resto del telefono no.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            visualTransformation = RichTextVisualTransformation(
                editState.spans,
                caret = if (fieldFocused) {
                    (editState.fieldValue.selection.min - RUN_LEAD.length).coerceAtLeast(0)..
                        (editState.fieldValue.selection.max - RUN_LEAD.length).coerceAtLeast(0)
                } else {
                    null
                }
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    noteLog(
                        "casella ${block.id.takeLast(4)} fuoco -> ${state.isFocused} " +
                            "(cursore ora a ${editState.fieldValue.selection.start})"
                    )
                    fieldFocused = state.isFocused
                    onFocusChanged(block.id, state.isFocused)
                }
        )
    }

    val menuBlockId = numberMenuFor
    if (menuBlockId != null) {
        NumberedListMenu(
            hasReset = block.numberStartsAt != null,
            onDismiss = { numberMenuFor = null },
            onBeginNewList = { viewModel.beginNewListHere(menuBlockId) },
            onContinuePrevious = { viewModel.continuePreviousList(menuBlockId) },
            onRemoveNumber = { viewModel.removeNumber(menuBlockId) },
            onChangeToBullet = { viewModel.changeToBullet(menuBlockId) }
        )
    }

    val children = allBlocks
        .filter { it.parentBlockId == block.id }
        .sortedBy { it.orderIndex }
    val isOpenToggle = block.type == BlockType.TOGGLE && block.isExpanded
    val shouldShowChildren = children.isNotEmpty() &&
        (block.type != BlockType.TOGGLE || block.isExpanded)

    if (shouldShowChildren) {
        Column {
            children.forEach { child ->
                // **Ogni riga col suo `key`.** Senza, Compose riconosce le
                // righe dalla posizione e non da chi sono. L'Invio in una
                // riga dentro un toggle fa nascere la riga nuova **sopra**
                // (vedi `splitBlockAt`, è quello che tiene ferma la
                // tastiera): la riga col cursore scendeva di un posto, ma
                // il suo campo restava al posto di prima e si ritrovava a
                // mostrare la riga nuova. Il registro l'ha mostrato
                // chiaro: dopo l'Invio la lettera successiva finiva nella
                // riga di sopra, attaccata — "1", Invio, "2" dava "12".
                // Il testo di primo livello non aveva il problema perché
                // la lista che lo contiene le chiavi le ha già.
                key(child.id) {
                    BlockRow(
                        block = child,
                        allBlocks = allBlocks,
                        depth = depth + 1,
                        viewModel = viewModel,
                        factory = factory,
                        focusRequestBlockId = focusRequestBlockId,
                        formatRequest = formatRequest,
                        onFocusChanged = onFocusChanged,
                        onNavigateToPage = onNavigateToPage,
                        onNavigateToDatabase = onNavigateToDatabase
                    )
                }
            }
        }
    } else if (isOpenToggle && !LocalPageLocked.current) {
        // **Un toggle aperto e vuoto dice che è vuoto, e si tocca per
        // scriverci dentro.** Prima non mostrava niente, e il pulsante
        // per aggiungere compariva solo se c'era già almeno un figlio:
        // in un toggle nuovo non c'era nessun modo di entrare. È la
        // stessa riga grigia che mostra Notion.
        //
        // Con dei figli invece non c'è più nessun pulsante: si aggiunge
        // con l'Invio, dal titolo o dall'ultima riga, come su Notion.
        Text(
            text = EditorStrings.emptyToggle,
            style = LocalPageTypography.current.apply(MaterialTheme.typography.bodyLarge),
            color = NotionGray400,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.addFirstChild(block) }
                .padding(
                    start = PAGE_SIDE_PADDING + NESTED_INDENT * (depth + 1),
                    end = PAGE_SIDE_PADDING,
                    top = 4.dp,
                    bottom = 4.dp
                )
        )
    }
}

/**
 * Il quadratino di una casella da spuntare.
 *
 * Disegnato a mano invece di usare `Checkbox` di Material: quello
 * porta con sé una zona di tocco da 48dp e un quadrato da 20 che non
 * si possono rimpicciolire, e accanto a una riga di testo alta 24
 * risultava un bottone messo lì in mezzo, più grande delle lettere e
 * disallineato. Qui il quadrato è alto quanto le lettere e sta
 * **centrato sulla prima riga** del testo: la zona di tocco resta
 * larga 24 tutt'intorno, così resta comodo da centrare col dito.
 */
@Composable
private fun CheckboxMark(
    checked: Boolean,
    onToggle: () -> Unit,
    /**
     * Se alzare il quadratino per centrarlo sulla riga di testo. Serve
     * dentro `BlockRow`, dove sta in una riga accanto al testo; sopra il
     * campo condiviso no, perché lì la posizione arriva già dal testo.
     */
    lifted: Boolean = true
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .offset(y = if (lifted) -CHECKBOX_BASELINE_LIFT else 0.dp)
            .size(CHECKBOX_TOUCH_SIZE)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(CHECKBOX_SIZE)
                .clip(shape)
                .then(
                    if (checked) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/** Testo + selezione di un blocco isola, insieme agli span che li descrivono — un unico stato così i due non possono mai disallinearsi. */
private data class BlockEditState(val spans: List<RichTextSpan>, val fieldValue: TextFieldValue)

/**
 * Colora il testo visualizzato secondo gli span del blocco (grassetto,
 * corsivo, sottolineato, barrato), senza toccare il testo grezzo che il
 * campo sta effettivamente modificando — usata solo dai blocchi isola
 * (checkbox, toggle), che restano su span per blocco singolo.
 */
private class RichTextVisualTransformation(
    private val spans: List<RichTextSpan>,
    /** Vedi il gemello nel campo unito: serve a scoprire gli spoiler toccati dal cursore. */
    private val caret: IntRange? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // L'a-capo nascosto in testa non si deve vedere: quello che si
        // mostra è il testo senza, e le posizioni vanno tradotte di
        // uno. Se non c'è (un istante di disallineamento, o un campo
        // che non lo usa) la traduzione è l'identità.
        val hasLead = text.text.startsWith(RUN_LEAD)
        // Gli span sono la verità sulla formattazione, ma il testo del
        // campo è la verità su cosa c'è scritto: quando i due non
        // combaciano — succede per un fotogramma mentre si scrive — si
        // mostra il testo, altrimenti sparirebbe la lettera appena
        // battuta.
        val body = if (hasLead) text.text.substring(RUN_LEAD.length) else text.text
        val mapping = if (hasLead) LeadOffsetMapping else OffsetMapping.Identity
        // Vedi il gemello nel campo unito: quando i due divergono — per
        // un fotogramma, mentre si scrive — la stessa modifica si applica
        // agli span invece di rinunciare a disegnarli. Qui non c'è
        // nessuna corrispondenza fra righe e blocchi da poter sbagliare:
        // il blocco è uno solo.
        val storedText = spans.plainText()
        val effective = if (storedText == body) spans else applyTextEdit(spans, storedText, body)
        val builder = AnnotatedString.Builder()
        var pos = 0
        for (span in effective) {
            val start = pos
            builder.append(span.text)
            pos += span.text.length
            if (span.bold || span.italic || span.underline || span.strikethrough) {
                val decorations = mutableListOf<TextDecoration>()
                if (span.underline) decorations.add(TextDecoration.Underline)
                if (span.strikethrough) decorations.add(TextDecoration.LineThrough)
                builder.addStyle(
                    SpanStyle(
                        fontWeight = if (span.bold) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
                    ),
                    start,
                    pos
                )
            }
            colorStyleOf(span)?.let { builder.addStyle(it, start, pos) }
            if (span.spoiler) {
                // Dentro, non sui bordi: vedi il gemello nel campo unito.
                val revealed = caret != null && caret.first < pos && caret.last > start
                builder.addStyle(spoilerStyle(revealed), start, pos)
            }
        }
        return TransformedText(builder.toAnnotatedString(), mapping)
    }
}

/**
 * Le posizioni di un campo che comincia con l'a-capo nascosto: sullo
 * schermo tutto è spostato indietro di uno, e nessuna posizione
 * mostrata può corrispondere allo zero — il cursore non riesce a
 * mettersi prima di quell'a-capo, ed è proprio per questo che
 * "l'a-capo è sparito" vuol dire senza ambiguità "backspace a inizio
 * riga".
 */
private object LeadOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int =
        (offset - RUN_LEAD.length).coerceAtLeast(0)

    override fun transformedToOriginal(offset: Int): Int = offset + RUN_LEAD.length
}

@Composable
private fun PageLinkBlockContent(
    block: BlockEntity,
    depth: Int,
    viewModel: PageEditorViewModel,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit
) {
    val linkedPageId = block.linkedPageId
    var linkedPage by remember(linkedPageId) { mutableStateOf<PageEntity?>(null) }

    LaunchedEffect(linkedPageId) {
        if (linkedPageId != null) {
            linkedPage = viewModel.getPageInfo(linkedPageId)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING, top = 4.dp, bottom = 4.dp)
            .clickable(enabled = linkedPageId != null) {
                val id = linkedPageId ?: return@clickable
                // Un collegamento può puntare anche a un **database**: è
                // quello che lascia "Turn into page". Aprirlo come pagina
                // di testo mostrerebbe un foglio vuoto al posto delle
                // righe, quindi conta cosa c'è dall'altra parte, non solo
                // il tipo del blocco.
                if (block.type == BlockType.DATABASE_LINK || linkedPage?.isDatabase == true) {
                    onNavigateToDatabase(id)
                } else {
                    onNavigateToPage(id)
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (block.type == BlockType.DATABASE_LINK) {
            Icon(
                Icons.Filled.TableChart,
                contentDescription = null,
                tint = NotionWhite,
                modifier = Modifier.size(20.dp)
            )
        } else {
            // Se la pagina collegata ha un'immagine al posto
            // dell'emoji, qui si vede quella: altrimenti la stessa
            // pagina avrebbe due facce diverse a seconda di dove la si
            // guarda.
            val linkedIconImage = linkedPage?.iconImage
            if (linkedIconImage != null) {
                val linkContext = LocalContext.current
                PageImage(
                    fileName = linkedIconImage,
                    store = remember(linkContext) { PageImageStore(linkContext) },
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else if (linkedPage?.isDatabase == true) {
                // Un database senza icona sua non ha un'emoji da mostrare:
                // il 📄 scritto nel suo campo è solo il valore di riserva
                // delle pagine. Qui si vede il segno della tabella, come
                // quando il database sta dentro la pagina.
                Icon(
                    Icons.Filled.TableChart,
                    contentDescription = null,
                    tint = NotionWhite,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(linkedPage?.icon ?: "📄", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        // Il nome della pagina collegata si scrive col font e il corpo di
        // questa pagina: è una riga del suo testo, e con un corpo grande
        // resterebbe l'unica piccola.
        Text(
            text = linkedPage?.title?.ifBlank { Strings.untitled } ?: Strings.untitled,
            color = NotionWhite,
            style = LocalPageTypography.current.apply(MaterialTheme.typography.bodyLarge),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { viewModel.deleteBlock(block) }, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = EditorStrings.removeLink,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Il divisore non ha campo di testo, quindi niente backspace-elimina —
 * serve un'icona dedicata, come già per tabelle e link a pagina.
 */
@Composable
private fun DividerBlockContent(
    block: BlockEntity,
    depth: Int,
    viewModel: PageEditorViewModel
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING)
            .padding(vertical = 12.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showDeleteConfirm = true })
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(EditorStrings.deleteDividerTitle) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBlock(block)
                    showDeleteConfirm = false
                }) { Text(Strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(Strings.cancel) }
            }
        )
    }
}

@Composable
private fun TableBlockContent(
    block: BlockEntity,
    depth: Int,
    viewModel: PageEditorViewModel
) {
    // Null finché il database non ha risposto. Ogni casella tiene il suo
    // testo in uno stato locale nato col primo valore che vede: con una
    // lista vuota di partenza nasceva vuota e restava vuota, quindi il
    // testo scritto in una tabella spariva riaprendo la pagina (nel
    // database c'era ancora). La chiave `loaded` fa rinascere le caselle
    // una volta sola, quando le celle arrivano.
    val cells by remember(block.id) { viewModel.tableCellsFlow(block.id) }
        .collectAsStateWithLifecycle(initialValue = null)
    val loaded = cells != null
    val cellMap = remember(cells) {
        cells.orEmpty().associate { (it.rowIndex to it.colIndex) to it.text }
    }

    Column(
        modifier = Modifier
            .padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING, top = 8.dp, bottom = 8.dp)
    ) {
        for (r in 0 until block.tableRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until block.tableCols) {
                    key(r, c, loaded) {
                        val cellText = cellMap[r to c] ?: ""
                        var localCellText by remember { mutableStateOf(cellText) }

                        BasicTextField(
                            value = localCellText,
                            onValueChange = { newText ->
                                localCellText = newText
                                viewModel.setTableCell(block.id, r, c, newText)
                            },
                            // Font e corpo della pagina, come il resto del testo.
                            textStyle = LocalPageTypography.current.apply(
                                MaterialTheme.typography.bodyLarge.copy(color = NotionWhite)
                            ),
                            cursorBrush = SolidColor(NotionWhite),
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = { viewModel.resizeTable(block, block.tableRows + 1, block.tableCols) }) {
                Text("+ Row", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { viewModel.resizeTable(block, block.tableRows, block.tableCols + 1) }) {
                Text("+ Column", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { viewModel.deleteBlock(block) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = EditorStrings.deleteTable,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
