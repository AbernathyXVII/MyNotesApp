package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.statusBarsPadding
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDownCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.platform.LocalContext
import com.gabriele.notionlocal.data.PageImageStore
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.gabriele.notionlocal.data.dao.RowCover
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.GALLERY_DEFAULT_PREVIEW
import com.gabriele.notionlocal.data.entity.GALLERY_DEFAULT_SIZE
import com.gabriele.notionlocal.data.entity.GalleryCardPreview
import com.gabriele.notionlocal.data.entity.GalleryCardSize
import com.gabriele.notionlocal.data.repository.RowPreviewLine
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DateRange
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.MonthlyMode
import com.gabriele.notionlocal.data.entity.Recurrence
import com.gabriele.notionlocal.data.entity.RecurrenceCodec
import com.gabriele.notionlocal.data.entity.RecurrenceEnd
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.TAG_COLORS
import com.gabriele.notionlocal.data.entity.TimelineZoom
import androidx.compose.ui.text.style.TextAlign
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import com.gabriele.notionlocal.viewmodel.DatabaseViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

private val COLUMN_WIDTH = 150.dp
private val NAME_COLUMN_WIDTH = 200.dp
private val ADD_PROPERTY_WIDTH = 52.dp
private val BOARD_COLUMN_WIDTH = 260.dp

/**
 * Le misure del calendario.
 *
 * Nel mese l'altezza di una settimana è fissa: sei settimane devono
 * entrare in una schermata senza costringere a scorrere per vedere il
 * mese, e questo mette un tetto a quante barre ci stanno prima di
 * riassumere le altre con un "+N".
 *
 * Nella settimana invece l'altezza segue quante pagine ci sono davvero:
 * c'è una riga sola, lo spazio c'è, e nascondere pagine quando c'è posto
 * per mostrarle sarebbe gratuito.
 */
private val CALENDAR_MONTH_WEEK_HEIGHT = 84.dp
private val CALENDAR_WEEK_MIN_HEIGHT = 120.dp
private val CALENDAR_WEEK_HEADER = 20.dp
private val CALENDAR_BAR_HEIGHT = 18.dp

/**
 * Quante righe di pagine può riservare una settimana.
 *
 * Ogni pagina datata del database si tiene la sua riga per sempre, in
 * ogni settimana, anche in quelle in cui non compare: le righe sono
 * quindi tante quante le pagine datate. Questo è il tetto oltre il
 * quale si smette di riservarne, perché un database con centinaia di
 * pagine datate renderebbe altissima ogni settimana. Le pagine oltre il
 * tetto finiscono nel "+N" e si guardano dalla tabella.
 */
private const val CALENDAR_MAX_ROWS = 20

/**
 * Un giorno nella vista dell'anno: dodici mesi affiancati tre per riga
 * lasciano poco più di un centimetro a testa, quindi le misure sono
 * quelle minime in cui due cifre restano leggibili.
 */
private val MINI_DAY_SIZE = 13.dp
private val MINI_DAY_FONT = 8.sp

/**
 * Quante settimane si possono sfogliare col dito, e da quale si parte.
 * Il numero non è un limite vero: sono circa centonovant'anni per
 * parte, e le frecce e il pulsante Today portano ovunque comunque.
 */
private const val WEEK_PAGER_PAGES = 20001
private const val WEEK_PAGER_CENTER = WEEK_PAGER_PAGES / 2

/**
 * Le misure della linea del tempo.
 *
 * `TIMELINE_MAX_DAYS` è un tetto vero e non prudenza sprecata: le
 * colonne dei giorni non sono pigre, vengono composte tutte insieme,
 * quindi senza tetto una data sbagliata nell'anno tremila costruirebbe
 * un milione di caselle.
 */
private val TIMELINE_DAY_WIDTH = 40.dp
private val TIMELINE_ROW_HEIGHT = 34.dp
private val TIMELINE_NAME_WIDTH = 120.dp
private val TIMELINE_HEADER_HEIGHT = 36.dp
private val TIMELINE_HOUR_WIDTH = 52.dp
private val TIMELINE_MIN_BAR_WIDTH = 6.dp

/** Quanto passato lasciare in vista aprendo su adesso. */
private val TIMELINE_TODAY_LEAD_IN = 80.dp
private const val TIMELINE_MAX_TICKS = 400

/**
 * Quanto aspettare, dopo che la tastiera si è chiusa, prima di
 * spegnere il cursore. Lo stesso valore dell'editor, per la stessa
 * ragione: la tastiera Samsung sparisce e riappare per un istante
 * durante i cambi di fuoco.
 */
private const val IME_CLOSE_SETTLE_MS = 250L

/** Spessore delle linee della griglia, lo stesso che usa VerticalDivider. */
private val SEPARATOR_WIDTH = 1.dp
private val ROW_HEIGHT = 44.dp

/** L'icona della pagina accanto al nome di una riga: quanto una lettera maiuscola, poco di più. */
private val ROW_ICON_SIZE = 18.dp

/** La stessa icona dentro le barre del calendario e della linea temporale, che sono basse. */
private val BAR_ICON_SIZE = 12.dp

// **Le date seguono le impostazioni.** Il formato della data (e con lui
// quello di data e ora) è quello scelto in Settings → Date format; mesi e
// giorni si scrivono nella lingua dell'app. Sono letti a ogni uso e non
// fissati una volta: cambiando impostazione, la tabella si riscrive.
private val DATE_FORMAT: DateTimeFormatter get() = Formats.dateFormatter()
private val DATE_TIME_FORMAT: DateTimeFormatter get() = Formats.dateTimeFormatter()
private val MONTH_FORMAT: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("MMMM yyyy", AppSettings.language.locale)
private val DAY_MONTH_FORMAT: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("d MMM", AppSettings.language.locale)
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** La lingua in cui scrivere nomi di giorni e mesi: quella dell'app, non quella del telefono. */
private val displayLocale: Locale get() = AppSettings.language.locale

/**
 * Cosa sta modificando la finestra delle proprietà: una colonna nuova,
 * oppure una esistente.
 */
private sealed class PropertyTarget {
    /** `initialType` serve a chi la propone già sapendo che tipo serve (il calendario una data, la bacheca una selezione). */
    data class New(val initialType: ColumnType = ColumnType.TEXT) : PropertyTarget()
    data class Existing(val column: DatabaseColumnEntity) : PropertyTarget()
}

/**
 * Il database a schermo intero: una barra in alto per tornare indietro,
 * e sotto lo stesso contenuto che si vede quando il database è dentro
 * una pagina.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatabaseViewScreen(
    pageId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenRowPage: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUpdates: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenDatabase: (String) -> Unit,
    onOpenSidebar: () -> Unit = {}
) {
    // **Lo stesso ViewModel che usa il contenuto**, non un secondo:
    // stessa chiave, quindi è proprio la stessa istanza. Il menu dei
    // tre puntini sta nella barra in alto, che vive qui fuori, e le sue
    // voci cambiano la pagina — con due ViewModel diversi uno dei due
    // avrebbe sempre in mano una versione vecchia.
    val menuViewModel: DatabaseViewModel = viewModel(factory = factory, key = "database-$pageId")
    LaunchedEffect(pageId) { menuViewModel.load(pageId) }
    val menuPage by menuViewModel.page.collectAsStateWithLifecycle()

    var showPageOptions by remember { mutableStateOf(false) }
    var moveDestinations by remember { mutableStateOf<List<PageEntity>?>(null) }
    var confirmTrash by remember { mutableStateOf(false) }
    var showDuplicate by remember { mutableStateOf(false) }

    // Se questo database è richiamato come pagina da qualche parte:
    // solo allora "Turn into database" ha un posto in cui rimetterlo.
    // Si ricontrolla ogni volta che si apre il menu, perché nel frattempo
    // può essere cambiato dall'altra schermata.
    var shownAsPage by remember { mutableStateOf(false) }
    LaunchedEffect(pageId, showPageOptions) {
        shownAsPage = menuViewModel.isShownAsPage()
    }

    // Chiusa la tastiera, il cursore si spegne. Stessa cosa della
    // pagina, e per la stessa ragione: il tasto indietro con la
    // tastiera aperta se lo prende la tastiera e all'app non arriva,
    // quindi il segnale è la tastiera che si chiude. Vedi il commento
    // in `PageEditorScreen`.
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    var anythingFocused by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible, anythingFocused) {
        if (!imeVisible && anythingFocused) {
            delay(IME_CLOSE_SETTLE_MS)
            focusManager.clearFocus()
        }
    }
    BackHandler(enabled = anythingFocused) { focusManager.clearFocus() }

    // Aperto dal cestino: si guarda e basta, e al posto della barra in
    // alto c'è quella del cestino.
    val trashed = menuPage?.trashedAt != null
    var confirmDeleteForever by remember { mutableStateOf(false) }
    val trashContext = LocalContext.current

    Scaffold(
        topBar = {
            if (trashed) {
                Box(modifier = Modifier.statusBarsPadding()) {
                    TrashBanner(
                        onBack = onBack,
                        onRestore = {
                            menuViewModel.restoreFromTrash()
                            Toast.makeText(trashContext, Strings.restoredToMainMenu, Toast.LENGTH_SHORT).show()
                        },
                        onDeleteForever = { confirmDeleteForever = true }
                    )
                }
            } else {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        Row {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
                            }
                            // La barra laterale, accanto a indietro come
                            // nelle pagine.
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                onOpenSidebar()
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = Strings.openSidebar)
                            }
                        }
                    },
                    actions = {
                        // Stessa icona e stesso posto della lente di prima,
                        // come su ogni pagina: un database aperto a schermo
                        // intero **è** una pagina.
                        IconButton(onClick = { showPageOptions = true }) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = Strings.pageOptions)
                        }
                    }
                )
            }
        }
    ) { padding ->
        DatabaseContent(
            pageId = pageId,
            factory = factory,
            embedded = false,
            onOpenRowPage = onOpenRowPage,
            onOpenFullPage = null,
            modifier = Modifier
                .padding(padding)
                .onFocusChanged { anythingFocused = it.hasFocus }
        )
    }

    menuPage?.let { current ->
        if (showPageOptions) {
            PageOptionsSheet(
                page = current,
                onDismiss = { showPageOptions = false },
                onToggleFavorite = { menuViewModel.setFavorite(!current.isFavorite) },
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
                    menuViewModel.loadMoveDestinations { moveDestinations = it }
                },
                onMoveToTrash = {
                    showPageOptions = false
                    confirmTrash = true
                },
                onToggleViewLock = { menuViewModel.toggleViewLocked() },
                onToggleLock = { menuViewModel.toggleLocked() },
                onUpdates = {
                    showPageOptions = false
                    onOpenUpdates(current.id)
                },
                // Rimesso dentro la pagina, si torna lì: è lì che lo si
                // vuole rivedere, aperto come prima.
                onTurnIntoDatabase = if (shownAsPage) {
                    {
                        showPageOptions = false
                        menuViewModel.turnIntoDatabase { onBack() }
                    }
                } else {
                    null
                }
            )
        }

        moveDestinations?.let { destinations ->
            MoveToSheet(
                destinations = destinations,
                onDismiss = { moveDestinations = null },
                onPick = { destination ->
                    moveDestinations = null
                    menuViewModel.movePageTo(destination.id) { onBack() }
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
                    // Come nelle pagine: si apre la copia, e un avviso dice
                    // dov'è finita se non è accanto all'originale.
                    menuViewModel.duplicatePage(target, Strings.copySuffix, PageImageStore(trashContext)) { copyId ->
                        placeName?.let {
                            Toast.makeText(trashContext, Strings.duplicatedInto(it), Toast.LENGTH_SHORT).show()
                        }
                        onOpenDatabase(copyId)
                    }
                }
            )
        }

        if (confirmTrash) {
            MoveToTrashDialog(
                pageTitle = current.title,
                onConfirm = {
                    confirmTrash = false
                    menuViewModel.moveToTrash { onBack() }
                },
                onDismiss = { confirmTrash = false }
            )
        }
    }

    if (confirmDeleteForever) {
        DeleteForeverDialog(
            onDismiss = { confirmDeleteForever = false },
            onConfirm = {
                confirmDeleteForever = false
                menuViewModel.deletePermanently(PageImageStore(trashContext)) { onBack() }
            }
        )
    }
}

/**
 * Il database vero e proprio: titolo, barra della vista, contenuto.
 *
 * Lo stesso pezzo serve in due posti, ed è deliberato che sia uno solo:
 * a schermo intero e dentro una pagina, dove il database è un blocco
 * fra gli altri e si modifica lì senza aprirlo. Averne due copie
 * significherebbe correggere ogni cosa due volte.
 *
 * Le differenze fra i due casi sono due sole:
 *  - incorporato, il titolo è più piccolo e ha accanto il menu "..."
 *    per aprirlo a schermo intero;
 *  - incorporato, **non scorre per conto suo**: prende l'altezza che
 *    gli serve e lascia scorrere la pagina che lo contiene. Due aree
 *    che scorrono una dentro l'altra si rubano il gesto a vicenda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseContent(
    pageId: String,
    factory: ViewModelFactory,
    embedded: Boolean,
    onOpenRowPage: (String) -> Unit,
    onOpenFullPage: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
    /** Solo dentro una pagina: lo passa il blocco che mostra il database. */
    onTurnIntoPage: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Un ViewModel per database: la chiave li tiene distinti quando in
    // una stessa pagina ce n'è più di uno.
    val viewModel: DatabaseViewModel = viewModel(factory = factory, key = "database-$pageId")

    LaunchedEffect(pageId) { viewModel.load(pageId) }

    val state by viewModel.tableState.collectAsStateWithLifecycle()
    val page by viewModel.page.collectAsStateWithLifecycle()
    val rowIcons by viewModel.rowIcons.collectAsStateWithLifecycle()
    // Copertine e testo delle pagine delle righe: arrivano solo quando la
    // vista è la galleria (vedi `DatabaseViewModel.load`).
    val rowCovers by viewModel.rowCovers.collectAsStateWithLifecycle()
    val rowPreviews by viewModel.rowPreviews.collectAsStateWithLifecycle()
    // La riga di cui si sta scegliendo l'icona, dalla finestra delle azioni.
    var iconForRow by remember { mutableStateOf<DatabaseRowEntity?>(null) }

    // "Lock view": si continua a lavorare sulle righe, ma
    // **l'impaginazione non si tocca** — vista, ordine, filtro,
    // raggruppamento, colonne. È proprio il senso della voce:
    // proteggere come è messo insieme il database, non i suoi dati.
    // Serve sia alla barra della vista sia alla finestra delle
    // impostazioni, quindi sta qui in cima e non dentro una delle due.
    val viewLocked = page?.isViewLocked == true

    // L'icona del database: dove stanno i file delle immagini, e se la
    // finestra per sceglierla è aperta. È la stessa finestra delle
    // pagine (Upload / Link / Remove), non una copia.
    val iconContext = LocalContext.current
    val imageStore = remember(iconContext) { PageImageStore(iconContext) }
    var editingIcon by remember { mutableStateOf(false) }

    var propertyTarget by remember { mutableStateOf<PropertyTarget?>(null) }
    var rowActionsFor by remember { mutableStateOf<DatabaseRowEntity?>(null) }
    var rowPendingDeletion by remember { mutableStateOf<DatabaseRowEntity?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    // Su quale schermata aprire le impostazioni: il pulsante della vista
    // porta dritto al selettore, l'icona dei cursori all'elenco completo.
    var settingsInitialPage by remember { mutableStateOf(SettingsPage.ROOT) }
    // Cresce ad ogni pagina creata dal pulsante in fondo. La bacheca e
    // il calendario lo osservano per portare in vista quella nuova.
    var newRowTick by remember { mutableStateOf(0) }

    // Aprire una finestra mentre un campo di testo ha il fuoco lascia
    // sullo schermo i pallini della selezione, che restano sopra alla
    // finestra: togliamo il fuoco prima di aprirla.
    val focusManager = LocalFocusManager.current

    // **Database semplice**: le righe sono solo testo. Toccarne una, in
    // qualunque vista, apre la sua **scheda** — nome e proprietà — invece
    // di una pagina, che per queste righe non esiste e non nasce mai.
    // Negli altri database il tocco apre la pagina della riga, come
    // sempre. Una funzione sola per le sei viste, così nessuna può
    // restare indietro.
    val simple = page?.isSimpleDatabase == true
    val openRow: (DatabaseRowEntity) -> Unit = if (simple) {
        { row ->
            focusManager.clearFocus()
            rowActionsFor = row
        }
    } else {
        { row -> viewModel.openRow(row) { onOpenRowPage(it) } }
    }

    CompositionLocalProvider(
        LocalDatabaseLocked provides (page?.isLocked == true || page?.trashedAt != null),
        LocalRowIcons provides rowIcons,
        LocalSimpleDatabase provides simple
    ) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (embedded) Modifier else Modifier.fillMaxSize())
    ) {
        // Il titolo si può nascondere, ma **solo dentro una pagina**:
        // lì la pagina ha già un suo titolo e un database che si
        // chiama "Untitled" sopra al contenuto è solo una riga di
        // rumore. A schermo intero il titolo c'è sempre, perché è
        // l'unica cosa che dice dove si è finiti.
        val showTitle = !embedded || page?.showEmbeddedTitle != false
        // Mentre si scrive comanda il testo locale, non quello che
        // torna dal database: il salvataggio è immediato ma il giro di
        // ritorno no, e se nel frattempo è stato battuto un altro tasto
        // rimetterebbe il titolo com'era un attimo prima, mangiandosi
        // il carattere. Fuori dalla scrittura comanda il database, così
        // una rinomina fatta nell'altra schermata si vede appena si
        // torna qui.
        var titleFocused by remember { mutableStateOf(false) }
        var titleText by remember { mutableStateOf(page?.title.orEmpty()) }
        LaunchedEffect(page?.title, titleFocused) {
            if (!titleFocused) titleText = page?.title.orEmpty()
        }
        if (showTitle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // **L'icona del database, prima del nome.** Solo se c'è:
            // senza, il nome parte dal margine come prima e non resta
            // nessun buco. Toccandola si cambia o si toglie; per
            // metterne una la prima volta c'è la voce "Icon" nelle
            // impostazioni. Più piccola dentro una pagina, dove anche
            // il nome lo è.
            val databaseIcon = page?.iconImage
            if (databaseIcon != null) {
                PageImage(
                    fileName = databaseIcon,
                    store = imageStore,
                    contentDescription = EditorStrings.icon,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(if (embedded) 26.dp else 40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .let { base ->
                            if (page?.isLocked == true) base else base.clickable { editingIcon = true }
                        }
                )
            }
            // Il titolo sta nel contenuto e non nella barra in alto,
            // come su Notion; incorporato è più contenuto, perché lì
            // il titolo della pagina è un altro.
            BasicTextField(
                value = titleText,
                onValueChange = {
                    val singleLine = it.replace("\n", "")
                    titleText = singleLine
                    viewModel.updateTitle(singleLine)
                },
                singleLine = true,
                // Pagina bloccata: nemmeno il titolo si tocca.
                readOnly = page?.isLocked == true,
                // Un titolo non ha righe: la tastiera deve offrire
                // "fine", non un invio che non farebbe niente.
                keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                textStyle = if (embedded) {
                    MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onFocusChanged { titleFocused = it.isFocused }
            )

        }
        }

        val layout = page?.databaseLayout ?: DatabaseLayout.TABLE

        ViewToolbar(
            layout = layout,
            sortActive = page?.sortColumnId != null,
            filterActive = viewModel.filterColumn() != null,
            onOpenFullPage = onOpenFullPage,
            onOpenFilter = if (viewLocked) {
                null
            } else {
                {
                    focusManager.clearFocus()
                    showFilter = true
                }
            },
            onOpenSort = if (viewLocked) {
                null
            } else {
                {
                    focusManager.clearFocus()
                    showSort = true
                }
            },
            onOpenLayoutPicker = if (viewLocked) {
                null
            } else {
                {
                    focusManager.clearFocus()
                    settingsInitialPage = SettingsPage.LAYOUT
                    showSettings = true
                }
            },
            onOpenSettings = {
                focusManager.clearFocus()
                settingsInitialPage = SettingsPage.ROOT
                showSettings = true
            }
        )

        Spacer(modifier = Modifier.size(8.dp))

        Column(
            modifier = if (embedded) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            }
        ) {
            when (layout) {
                DatabaseLayout.TABLE -> TableLayout(
                    state = state,
                    viewLocked = viewLocked,
                    onEditProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.Existing(it)
                    },
                    onMoveProperty = { column, delta -> viewModel.moveColumn(column.id, delta) },
                    onHideProperty = { viewModel.setColumnHidden(it.id, true) },
                    onToggleCenterProperty = {
                        viewModel.setColumnCentered(it.id, !it.centerContent)
                    },
                    onAddProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New()
                    },
                    onTitleChange = { row, title -> viewModel.updateRowTitle(row, title) },
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    onSetCellValue = { row, column, value ->
                        viewModel.setCellValue(row, column, value)
                    },
                    onAddOption = { column, label ->
                        viewModel.addSelectOption(column.id, label)
                    },
                    onRenameOption = { columnId, old, new ->
                        viewModel.renameSelectOption(columnId, old, new)
                    },
                    onSetOptionColor = { columnId, label, hex ->
                        viewModel.setSelectOptionColor(columnId, label, hex)
                    },
                    onDeleteOption = { columnId, label ->
                        viewModel.deleteSelectOption(columnId, label)
                    },
                    groupColumn = viewModel.tableGroupColumn(),
                    hideEmptyGroups = page?.hideEmptyGroups != false,
                    onAddRowInGroup = { value ->
                        viewModel.tableGroupColumn()?.let { column ->
                            viewModel.addRowInGroup(column.id, value)
                        }
                        newRowTick++
                    }
                )

                DatabaseLayout.BOARD -> BoardLayout(
                    state = state,
                    groupColumn = viewModel.boardGroupColumn(),
                    newRowTick = newRowTick,
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    onAddRowInGroup = { groupValue ->
                        viewModel.boardGroupColumn()?.let { column ->
                            viewModel.addRowInGroup(column.id, groupValue)
                        }
                    },
                    onAddGroupProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New(ColumnType.SELECT)
                    }
                )

                DatabaseLayout.CALENDAR -> CalendarLayout(
                    state = state,
                    dateColumn = viewModel.calendarDateColumn(),
                    mode = page?.calendarMode ?: CalendarMode.MONTH,
                    newRowTick = newRowTick,
                    onSetMode = { viewModel.setCalendarMode(it) },
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    onAddRowOnDate = { millis ->
                        viewModel.addRowOnDateCreatingColumn(millis)
                    },
                    onAddDateProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New(ColumnType.DATE)
                    }
                )

                DatabaseLayout.TIMELINE -> TimelineLayout(
                    state = state,
                    dateColumn = viewModel.calendarDateColumn(),
                    zoom = page?.timelineZoom ?: TimelineZoom.DAY,
                    newRowTick = newRowTick,
                    onSetZoom = { viewModel.setTimelineZoom(it) },
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    onAddDateProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New(ColumnType.DATE)
                    }
                )

                DatabaseLayout.LIST -> ListLayout(
                    state = state,
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    }
                )

                DatabaseLayout.GALLERY -> GalleryLayout(
                    state = state,
                    // Copertina e testo sono della pagina della riga: in
                    // un database semplice le pagine non ci sono, e le
                    // schede hanno solo nome e proprietà.
                    preview = if (simple) {
                        GalleryCardPreview.NONE
                    } else {
                        page?.galleryCardPreview ?: GALLERY_DEFAULT_PREVIEW
                    },
                    size = page?.galleryCardSize ?: GALLERY_DEFAULT_SIZE,
                    covers = rowCovers,
                    contentPreviews = rowPreviews,
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    }
                )
            }

            // Se la vista non ha la proprietà su cui si regge, non
            // mostra nulla: un pulsante che crea pagine invisibili è
            // peggio di nessun pulsante. Il messaggio sopra dice cosa
            // manca e offre di crearlo.
            val canShowNewRows = page?.isLocked != true && when (layout) {
                // Il calendario non ha bisogno di una proprietà data
                // già pronta: se manca, la crea la pagina stessa
                // appena la si mette su un giorno.
                DatabaseLayout.TIMELINE -> viewModel.calendarDateColumn() != null
                DatabaseLayout.BOARD -> viewModel.boardGroupColumn() != null
                else -> true
            }

            if (canShowNewRows) {
            TextButton(
                onClick = {
                    // Una pagina creata da qui deve comparire dove la si
                    // sta guardando. Nella tabella e nell'elenco basta
                    // aggiungerla in fondo; nel calendario finirebbe fra
                    // quelle senza data, e nella bacheca nella colonna in
                    // fondo a destra — cioè fuori da quello che si ha
                    // davanti, che è esattamente il motivo per cui
                    // sembrava non venisse creata.
                    if (layout == DatabaseLayout.CALENDAR) {
                        // Datata oggi, creando la proprietà data se il
                        // database non ne ha ancora una.
                        viewModel.addRowOnDateCreatingColumn(todayAtStartOfDay())
                    } else if (layout == DatabaseLayout.TIMELINE) {
                        val dateColumn = viewModel.calendarDateColumn()
                        if (dateColumn != null) {
                            viewModel.addRowOnDate(dateColumn.id, todayAtStartOfDay())
                        } else {
                            viewModel.addRow()
                        }
                    } else {
                        viewModel.addRow()
                    }
                    newRowTick++
                },
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (simple) DbStrings.newRow else DbStrings.newPage)
            }
            }
        }
    }
    }

    if (editingIcon) {
        PageImageSheet(
            target = PageImageTarget.ICON,
            hasImage = page?.iconImage != null,
            store = imageStore,
            onPicked = { viewModel.setIconImage(it) },
            // Un'icona è un quadratino: non c'è niente da inquadrare.
            onReposition = null,
            onDismiss = { editingIcon = false }
        )
    }

    if (showSettings) {
        SettingsSheet(
            hasIcon = page?.iconImage != null,
            onEditIcon = if (page?.isLocked == true) {
                null
            } else {
                {
                    showSettings = false
                    editingIcon = true
                }
            },
            columns = state.columns,
            layout = page?.databaseLayout ?: DatabaseLayout.TABLE,
            groupColumn = viewModel.boardGroupColumn(),
            dateColumn = viewModel.calendarDateColumn(),
            tableGroupColumn = viewModel.tableGroupColumn(),
            hideEmptyGroups = page?.hideEmptyGroups != false,
            onSetTableGroupColumn = { viewModel.setTableGroupColumn(it) },
            onSetHideEmptyGroups = { viewModel.setHideEmptyGroups(it) },
            onSetColumnHidden = { columnId, hidden ->
                viewModel.setColumnHidden(columnId, hidden)
            },
            onSetAllColumnsHidden = { viewModel.setAllColumnsHidden(it) },
            filterColumn = viewModel.filterColumn(),
            filterValue = page?.filterValue.orEmpty(),
            onOpenFilter = {
                showSettings = false
                showFilter = true
            },
            initialPage = settingsInitialPage,
            onDismiss = { showSettings = false },
            // Scelta la vista, la finestra si chiude: è l'unica cosa
            // che si era venuti a fare.
            onSetLayout = {
                viewModel.setLayout(it)
                showSettings = false
            },
            onSetGroupColumn = { viewModel.setBoardGroupColumn(it.id) },
            onSetDateColumn = { viewModel.setCalendarDateColumn(it.id) },
            galleryPreview = page?.galleryCardPreview ?: GALLERY_DEFAULT_PREVIEW,
            gallerySize = page?.galleryCardSize ?: GALLERY_DEFAULT_SIZE,
            simple = simple,
            onSetGalleryPreview = { viewModel.setGalleryCardPreview(it) },
            onSetGallerySize = { viewModel.setGalleryCardSize(it) },
            onEditProperty = { column ->
                showSettings = false
                propertyTarget = PropertyTarget.Existing(column)
            },
            onAddProperty = {
                showSettings = false
                propertyTarget = PropertyTarget.New()
            },
            // L'interruttore del titolo esiste solo per il database
            // dentro una pagina: `embedded` è la stessa condizione per
            // cui il titolo si può nascondere.
            onSetShowTitle = if (embedded) {
                { show -> viewModel.setShowEmbeddedTitle(show) }
            } else {
                null
            },
            showTitle = page?.showEmbeddedTitle != false,
            isFavorite = page?.isFavorite == true,
            onSetFavorite = { viewModel.setFavorite(it) },
            viewLocked = viewLocked,
            onTurnIntoPage = onTurnIntoPage?.let {
                {
                    showSettings = false
                    it()
                }
            },
            // Cancellare stava nei tre puntini accanto al titolo: due
            // menu diversi per le cose di uno stesso database erano un
            // posto in più dove cercare.
            onDelete = onDelete?.let {
                {
                    showSettings = false
                    it()
                }
            }
        )
    }

    if (showFilter) {
        FilterSheet(
            columns = state.columns,
            filterColumnId = page?.filterColumnId,
            filterValue = page?.filterValue.orEmpty(),
            onSetFilter = { columnId, value -> viewModel.setFilter(columnId, value) },
            onClear = { viewModel.clearFilter() },
            onDismiss = { showFilter = false }
        )
    }

    if (showSort) {
        SortSheet(
            columns = state.columns,
            sortColumnId = page?.sortColumnId,
            descending = page?.sortDescending == true,
            onPick = { columnId ->
                // Scegliendo una proprietà si parte sempre dal
                // crescente: è il verso che ci si aspetta, e per
                // l'altro basta un tocco in più.
                viewModel.setSort(columnId, descending = false)
                showSort = false
            },
            onToggleDirection = {
                viewModel.setSort(page?.sortColumnId, descending = page?.sortDescending != true)
            },
            onClear = {
                viewModel.setSort(null, descending = false)
                showSort = false
            },
            onDismiss = { showSort = false }
        )
    }

    propertyTarget?.let { target ->
        PropertySheet(
            target = target,
            onDismiss = { propertyTarget = null },
            onSave = { name, type, optionsJson ->
                when (target) {
                    is PropertyTarget.New -> viewModel.addColumn(name, type, optionsJson)
                    is PropertyTarget.Existing ->
                        viewModel.updateColumn(target.column.id, name, type, optionsJson)
                }
                propertyTarget = null
            },
            onDelete = {
                if (target is PropertyTarget.Existing) viewModel.deleteColumn(target.column)
                propertyTarget = null
            }
        )
    }

    rowActionsFor?.let { row ->
        // La riga arriva dallo stato aggiornato, non dalla copia
        // catturata al momento del tocco: altrimenti le date in fondo
        // e i valori delle proprietà resterebbero quelli di allora.
        val live = state.rows.find { it.id == row.id } ?: row
        RowActionsSheet(
            row = live,
            columns = state.columns,
            cellValues = state.cellValues,
            onDismiss = { rowActionsFor = null },
            onSetCellValue = { column, value -> viewModel.setCellValue(live, column, value) },
            onAddOption = { column, label -> viewModel.addSelectOption(column.id, label) },
            onRenameOption = { columnId, old, new ->
                viewModel.renameSelectOption(columnId, old, new)
            },
            onSetOptionColor = { columnId, label, hex ->
                viewModel.setSelectOptionColor(columnId, label, hex)
            },
            onDeleteOption = { columnId, label -> viewModel.deleteSelectOption(columnId, label) },
            hasIcon = rowIcons[live.id] != null,
            iconEditable = page?.isLocked != true && page?.trashedAt == null,
            simple = simple,
            onTitleChange = { title -> viewModel.updateRowTitle(live, title) },
            onEditIcon = {
                rowActionsFor = null
                iconForRow = live
            },
            onDelete = {
                rowActionsFor = null
                rowPendingDeletion = live
            }
        )
    }

    // L'icona della pagina di una riga: la stessa finestra delle pagine e
    // dei database (Upload / Link / Remove). La pagina della riga, se non
    // c'era ancora, nasce qui: l'icona sta nella pagina.
    iconForRow?.let { row ->
        PageImageSheet(
            target = PageImageTarget.ICON,
            hasImage = rowIcons[row.id] != null,
            store = imageStore,
            onPicked = { fileName -> viewModel.setRowIcon(row, fileName) },
            onReposition = null,
            onDismiss = { iconForRow = null }
        )
    }

    rowPendingDeletion?.let { row ->
        AlertDialog(
            onDismissRequest = { rowPendingDeletion = null },
            title = { Text(DbStrings.deletePageTitle) },
            text = { Text(DbStrings.deleteRowText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRow(row)
                    rowPendingDeletion = null
                }) { Text(Strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { rowPendingDeletion = null }) { Text(Strings.cancel) }
            }
        )
    }
}

/**
 * La visualizzazione a tabella: la griglia classica, con le colonne che
 * scorrono in orizzontale.
 */
@Composable
private fun TableLayout(
    state: DatabaseTableState,
    /** Null quando non si raggruppa: la tabella piatta di sempre. */
    groupColumn: DatabaseColumnEntity?,
    hideEmptyGroups: Boolean,
    /** "Lock view": le colonne non si spostano, e le due voci restano grigie. */
    viewLocked: Boolean,
    onEditProperty: (DatabaseColumnEntity) -> Unit,
    onMoveProperty: (DatabaseColumnEntity, Int) -> Unit,
    onHideProperty: (DatabaseColumnEntity) -> Unit,
    onToggleCenterProperty: (DatabaseColumnEntity) -> Unit,
    onAddProperty: () -> Unit,
    onTitleChange: (DatabaseRowEntity, String) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onSetCellValue: (DatabaseRowEntity, DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    /** Crea una pagina già dentro il gruppo: riceve il valore del gruppo. */
    onAddRowInGroup: (String) -> Unit
) {
    // Dentro un contenitore che scorre in orizzontale la larghezza
    // disponibile è illimitata: una linea che chiede "tutta la
    // larghezza" ne ottiene zero e sparisce. Le linee orizzontali vanno
    // quindi disegnate larghe quanto la tabella, calcolandolo.
    // Le colonne nascoste non occupano spazio: la larghezza si conta
    // su quelle che si vedono, altrimenti la tabella resterebbe larga
    // com'era e nasconderne una non servirebbe a niente.
    val shownColumns = state.visibleColumns
    val tableWidth = NAME_COLUMN_WIDTH + SEPARATOR_WIDTH +
        (COLUMN_WIDTH + SEPARATOR_WIDTH) * shownColumns.size +
        ADD_PROPERTY_WIDTH

    // Quali gruppi sono chiusi. Sta qui e non nel database perché è
    // come si sta guardando la tabella adesso, non una proprietà del
    // database: riaprendo la pagina si riparte con tutto aperto.
    val collapsed = remember(groupColumn?.id) { mutableStateMapOf<String, Boolean>() }

    // Intestazione e righe scorrono insieme in orizzontale, così le
    // colonne restano allineate.
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        if (groupColumn == null) {
            TableHeaderRow(
                columns = shownColumns,
                viewLocked = viewLocked,
                onEditProperty = onEditProperty,
                onMoveProperty = onMoveProperty,
                onHideProperty = onHideProperty,
                onToggleCenterProperty = onToggleCenterProperty,
                onAddProperty = onAddProperty
            )
            HorizontalDivider(modifier = Modifier.width(tableWidth))
            state.rows.forEach { row ->
                TableBodyRow(
                    row = row,
                    state = state,
                    columns = shownColumns,
                    onTitleChange = onTitleChange,
                    onOpenRow = onOpenRow,
                    onRowLongPress = onRowLongPress,
                    onSetCellValue = onSetCellValue,
                    onAddOption = onAddOption,
                    onRenameOption = onRenameOption,
                    onSetOptionColor = onSetOptionColor,
                    onDeleteOption = onDeleteOption
                )
                HorizontalDivider(modifier = Modifier.width(tableWidth))
            }
            return@Column
        }

        val groups = buildRowGroups(state, groupColumn, hideEmptyGroups)
        groups.forEach { group ->
            val isCollapsed = collapsed[group.key] == true
            GroupHeaderRow(
                group = group,
                collapsed = isCollapsed,
                width = tableWidth,
                onToggle = { collapsed[group.key] = !isCollapsed }
            )
            if (!isCollapsed) {
                TableHeaderRow(
                    columns = shownColumns,
                    viewLocked = viewLocked,
                    onEditProperty = onEditProperty,
                    onMoveProperty = onMoveProperty,
                    onHideProperty = onHideProperty,
                    onToggleCenterProperty = onToggleCenterProperty,
                    onAddProperty = onAddProperty
                )
                HorizontalDivider(modifier = Modifier.width(tableWidth))
                group.rows.forEach { row ->
                    TableBodyRow(
                        row = row,
                        state = state,
                        columns = shownColumns,
                        onTitleChange = onTitleChange,
                        onOpenRow = onOpenRow,
                        onRowLongPress = onRowLongPress,
                        onSetCellValue = onSetCellValue,
                        onAddOption = onAddOption,
                        onRenameOption = onRenameOption,
                        onSetOptionColor = onSetOptionColor,
                        onDeleteOption = onDeleteOption
                    )
                    HorizontalDivider(modifier = Modifier.width(tableWidth))
                }
                // Una pagina creata da dentro un gruppo nasce già con
                // quel valore: altrimenti comparirebbe nel gruppo
                // "senza valore", cioè da tutt'altra parte rispetto a
                // dove la si è chiesta.
                TextButton(
                    onClick = { onAddRowInGroup(group.key) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        if (LocalSimpleDatabase.current) DbStrings.newRow else DbStrings.newPage,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

/**
 * L'intestazione delle colonne, ripetuta sopra ogni gruppo come su Notion.
 * `columns` sono quelle che si vedono, quindi la posizione in questa lista
 * è la posizione nella tabella: la prima non va a sinistra, l'ultima non
 * va a destra.
 */
@Composable
private fun TableHeaderRow(
    columns: List<DatabaseColumnEntity>,
    viewLocked: Boolean,
    onEditProperty: (DatabaseColumnEntity) -> Unit,
    onMoveProperty: (DatabaseColumnEntity, Int) -> Unit,
    onHideProperty: (DatabaseColumnEntity) -> Unit,
    onToggleCenterProperty: (DatabaseColumnEntity) -> Unit,
    onAddProperty: () -> Unit
) {
    Row {
        NameHeaderCell()
        CellSeparator()
        columns.forEachIndexed { index, column ->
            PropertyHeaderCell(
                column = column,
                canMoveLeft = !viewLocked && index > 0,
                canMoveRight = !viewLocked && index < columns.lastIndex,
                onClick = { onEditProperty(column) },
                onEdit = { onEditProperty(column) },
                onMove = { delta -> onMoveProperty(column, delta) },
                onHide = { onHideProperty(column) },
                onToggleCenter = { onToggleCenterProperty(column) }
            )
            CellSeparator()
        }
        AddPropertyCell(onClick = onAddProperty)
    }
}

/** Una riga della tabella: nome, celle, e lo spazio sotto la colonna "+". */
@Composable
private fun TableBodyRow(
    row: DatabaseRowEntity,
    state: DatabaseTableState,
    /** Solo le colonne che si vedono: le stesse dell'intestazione. */
    columns: List<DatabaseColumnEntity>,
    onTitleChange: (DatabaseRowEntity, String) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onSetCellValue: (DatabaseRowEntity, DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit
) {
    Row {
        NameCell(
            row = row,
            onTitleChange = { onTitleChange(row, it) },
            onOpen = { onOpenRow(row) },
            onLongPress = { onRowLongPress(row) }
        )
        CellSeparator()
        columns.forEach { column ->
            CellEditor(
                row = row,
                column = column,
                value = state.cellValues[row.id to column.id] ?: "",
                onValueChange = { onSetCellValue(row, column, it) },
                onAddOption = { onAddOption(column, it) },
                onRenameOption = onRenameOption,
                onSetOptionColor = onSetOptionColor,
                onDeleteOption = onDeleteOption
            )
            CellSeparator()
        }
        Box(modifier = Modifier.width(ADD_PROPERTY_WIDTH).height(ROW_HEIGHT))
    }
}

/**
 * L'intestazione di un gruppo: la freccia per chiuderlo, il valore
 * (come tag se la proprietà è a tag) e quante pagine contiene.
 */
@Composable
private fun GroupHeaderRow(
    group: RowGroup,
    collapsed: Boolean,
    width: Dp,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(width)
            .clickable { onToggle() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (collapsed) {
                Icons.Filled.KeyboardArrowRight
            } else {
                Icons.Filled.KeyboardArrowDown
            },
            contentDescription = if (collapsed) DbStrings.expandGroup else DbStrings.collapseGroup,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        if (group.color != null) {
            TagChip(label = group.label, color = group.color)
        } else {
            Text(
                text = group.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (group.key.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = group.rows.size.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Un gruppo di righe della tabella: il valore che lo definisce, come
 * si legge, il colore se è un tag, e le righe che ci stanno dentro.
 */
private data class RowGroup(
    val key: String,
    val label: String,
    val color: String?,
    val rows: List<DatabaseRowEntity>
)

/**
 * Divide le righe in gruppi secondo la proprietà scelta.
 *
 * L'ordine dei gruppi non è casuale: per i tag è quello in cui sono
 * stati creati, lo stesso che si vede modificando la proprietà, così
 * spostandosi fra tabella e bacheca non cambia niente. Per tutto il
 * resto è l'ordine naturale del valore. **Il gruppo "senza valore" sta
 * sempre in fondo**: davanti ruberebbe il primo sguardo alle righe già
 * smistate, che sono il contenuto vero.
 *
 * Con la selezione multipla una riga compare in **tutti** i gruppi dei
 * suoi tag: è quello che ci si aspetta da un raggruppamento, ed è
 * anche il motivo per cui la bacheca invece non la sostiene — lì una
 * scheda dovrebbe stare in due colonne insieme.
 */
private fun buildRowGroups(
    state: DatabaseTableState,
    column: DatabaseColumnEntity,
    hideEmpty: Boolean
): List<RowGroup> {
    fun valueOf(row: DatabaseRowEntity) = state.cellValues[row.id to column.id].orEmpty()

    val options = parseSelectOptionsFull(column.optionsJson)
    fun colorOf(label: String) = options.find { it.label == label }?.color

    val groups = mutableListOf<RowGroup>()
    val emptyLabel = DbStrings.noValue(DbStrings.columnName(column.name))

    when (column.type) {
        ColumnType.SELECT, ColumnType.MULTI_SELECT -> {
            val multi = column.type == ColumnType.MULTI_SELECT
            fun tagsOf(row: DatabaseRowEntity): List<String> = if (multi) {
                valueOf(row).split(MULTI_VALUE_SEPARATOR).filter { it.isNotEmpty() }
            } else {
                listOfNotNull(valueOf(row).takeIf { it.isNotEmpty() })
            }

            val declared = options.map { it.label }
            // Un valore rimasto nelle celle dopo che l'opzione è stata
            // cancellata non deve far sparire le sue righe: gli si dà
            // comunque un gruppo, in fondo a quelli dichiarati.
            val orphans = state.rows.flatMap { tagsOf(it) }.distinct().filter { it !in declared }
            (declared + orphans).forEach { label ->
                groups += RowGroup(
                    key = label,
                    label = label,
                    color = colorOf(label),
                    rows = state.rows.filter { label in tagsOf(it) }
                )
            }
            groups += RowGroup("", emptyLabel, null, state.rows.filter { tagsOf(it).isEmpty() })
        }

        ColumnType.CHECKBOX -> {
            // Niente gruppo "senza valore": una casella o è spuntata o
            // non lo è, e "vuota" vuol dire già "non spuntata".
            groups += RowGroup(
                "true",
                DbStrings.checked,
                null,
                state.rows.filter { valueOf(it) == "true" }
            )
            groups += RowGroup(
                "false",
                DbStrings.unchecked,
                null,
                state.rows.filter { valueOf(it) != "true" }
            )
        }

        ColumnType.DATE -> {
            // Un gruppo per giorno: raggruppare per istante esatto
            // darebbe un gruppo per riga.
            val byDay = state.rows.groupBy { row ->
                parseDateRange(valueOf(row))?.start?.toLocalDate()
            }
            byDay.keys.filterNotNull().sorted().forEach { day ->
                groups += RowGroup(
                    key = day.toEpochDay().toString(),
                    label = day.format(DATE_FORMAT),
                    color = null,
                    rows = byDay[day].orEmpty()
                )
            }
            groups += RowGroup("", emptyLabel, null, byDay[null].orEmpty())
        }

        else -> {
            val byValue = state.rows.groupBy { valueOf(it) }
            val comparator = if (column.type == ColumnType.NUMBER) {
                compareBy<String> { it.toDoubleOrNull() ?: Double.MAX_VALUE }
            } else {
                compareBy(String.CASE_INSENSITIVE_ORDER) { it }
            }
            byValue.keys.filter { it.isNotEmpty() }.sortedWith(comparator).forEach { value ->
                groups += RowGroup(value, value, null, byValue[value].orEmpty())
            }
            groups += RowGroup("", emptyLabel, null, byValue[""].orEmpty())
        }
    }

    // Un gruppo "senza valore" vuoto non si mostra mai: non è una
    // scelta dell'utente, è semplicemente che non c'è niente da dire.
    return groups.filter { it.rows.isNotEmpty() || (!hideEmpty && it.key.isNotEmpty()) }
}

/**
 * La visualizzazione a bacheca: una colonna per ogni valore della
 * proprietà scelta, con le righe come schede dentro la colonna del loro
 * valore.
 *
 * Le righe senza valore non spariscono: finiscono in una colonna
 * dedicata in fondo a destra, dopo quelle con un tag. Davanti
 * ruberebbero il primo sguardo a quelle già smistate, che sono il
 * contenuto vero della bacheca.
 */
@Composable
private fun BoardLayout(
    state: DatabaseTableState,
    groupColumn: DatabaseColumnEntity?,
    newRowTick: Int,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddRowInGroup: (String) -> Unit,
    onAddGroupProperty: () -> Unit
) {
    if (groupColumn == null) {
        // Senza una proprietà a selezione singola non ci sono colonne
        // da costruire. Meglio dirlo che mostrare una schermata vuota
        // lasciando credere che sia rotta.
        MissingPropertyNotice(
            message = DbStrings.boardNeedsSelect,
            actionLabel = DbStrings.addSelectProperty,
            onAction = onAddGroupProperty
        )
        return
    }

    val groups = parseSelectOptions(groupColumn.optionsJson) + ""

    val boardScroll = rememberScrollState()

    // Una pagina creata dal pulsante in fondo nasce senza tag, quindi
    // nell'ultima colonna: senza scorrere fin lì sembra non essere stata
    // creata affatto.
    LaunchedEffect(newRowTick) {
        if (newRowTick > 0) boardScroll.animateScrollTo(boardScroll.maxValue)
    }

    Row(
        modifier = Modifier.horizontalScroll(boardScroll),
        verticalAlignment = Alignment.Top
    ) {
        groups.forEach { groupValue ->
            val rowsInGroup = state.rows.filter {
                (state.cellValues[it.id to groupColumn.id] ?: "") == groupValue
            }

            Column(
                modifier = Modifier
                    .width(BOARD_COLUMN_WIDTH)
                    .padding(horizontal = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = groupValue.ifBlank { DbStrings.noValue(DbStrings.columnName(groupColumn.name)) },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = rowsInGroup.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                rowsInGroup.forEach { row ->
                    BoardCard(
                        row = row,
                        state = state,
                        groupColumnId = groupColumn.id,
                        onOpen = { onOpenRow(row) },
                        onLongPress = { onRowLongPress(row) }
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                TextButton(onClick = { onAddRowInGroup(groupValue) }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(DbStrings.newShort, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Il messaggio di una vista che non può disegnarsi perché manca la
 * proprietà su cui si regge, **con il pulsante per crearla**.
 *
 * Il pulsante non è un di più: in queste viste non esiste un "+ Add
 * property" (sta nell'intestazione della tabella), quindi un messaggio
 * che si limitasse a dire cosa manca manderebbe a cercare qualcosa che
 * da lì non si vede.
 */
@Composable
private fun MissingPropertyNotice(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(8.dp))
        TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(actionLabel)
        }
    }
}

/** Una scheda della bacheca: il nome della riga e i valori delle altre proprietà. */
@Composable
private fun BoardCard(
    row: DatabaseRowEntity,
    state: DatabaseTableState,
    groupColumnId: String,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .pointerInput(row.id) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIconBadge(row.id, ROW_ICON_SIZE)
            Text(
                text = row.title.ifBlank { Strings.untitled },
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.title.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
            )
        }

        // La proprietà che fa da colonna non si ripete sulle schede:
        // è già scritta in cima alla colonna che le contiene.
        state.visibleColumns
            .filter { it.id != groupColumnId }
            .forEach { column ->
                val value = state.cellValues[row.id to column.id].orEmpty()
                if (value.isNotBlank()) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = listCellLabel(column, value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
    }
}

/**
 * La visualizzazione a calendario: un anno, un mese o una settimana per
 * volta, con le pagine disegnate come barre lunghe quanto i giorni che
 * occupano.
 *
 * Il periodo mostrato non viene salvato — riaprendo si torna a oggi,
 * che è quasi sempre quello che interessa — mentre la scelta fra anno,
 * mese e settimana sì, perché è un modo di lavorare e non una
 * posizione.
 */
@Composable
private fun CalendarLayout(
    state: DatabaseTableState,
    dateColumn: DatabaseColumnEntity?,
    mode: CalendarMode,
    newRowTick: Int,
    onSetMode: (CalendarMode) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddRowOnDate: (Long) -> Unit,
    onAddDateProperty: () -> Unit
) {
    // **Senza proprietà data il calendario si vede lo stesso**, vuoto,
    // sul mese corrente. Prima al suo posto c'era un cartello che
    // chiedeva di creare la proprietà: corretto ma inutile, perché un
    // calendario vuoto si capisce da solo, e quello che si vuole fare
    // — toccare un giorno e scriverci una pagina — funziona comunque:
    // la proprietà data viene creata al volo al primo tocco.

    // Il primo giorno della settimana segue le convenzioni locali:
    // lunedì in Italia, domenica altrove.
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val today = LocalDate.now()

    // Il periodo mostrato è sempre individuato da un giorno che ci sta
    // dentro, così spostarsi avanti e indietro è la stessa operazione
    // per il mese e per la settimana.
    var anchor by remember { mutableStateOf(today) }

    // Una pagina creata dal pulsante in fondo nasce datata oggi: se si
    // stava guardando un altro periodo, bisogna tornare lì per vederla.
    LaunchedEffect(newRowTick) {
        if (newRowTick > 0) anchor = LocalDate.now()
    }

    val monthGridStart = startOfWeek(anchor.withDayOfMonth(1), firstDayOfWeek)
    val monthWeekCount = (
        ChronoUnit.DAYS.between(monthGridStart, anchor.withDayOfMonth(1)).toInt() +
            anchor.lengthOfMonth() + 6
        ) / 7
    val shownWeekStart = startOfWeek(anchor, firstDayOfWeek)

    // Il pezzo di tempo che si sta guardando. Le ripetizioni si
    // calcolano solo qui dentro: una regola senza fine ("ogni lunedì")
    // genererebbe occorrenze all'infinito, e di quelle del 2043 non se
    // ne fa niente nessuno finché non ci si arriva.
    val windowStart: LocalDate
    val windowEnd: LocalDate
    when (mode) {
        CalendarMode.YEAR -> {
            windowStart = LocalDate.of(anchor.year, 1, 1)
            windowEnd = LocalDate.of(anchor.year, 12, 31)
        }
        CalendarMode.MONTH -> {
            windowStart = monthGridStart
            windowEnd = monthGridStart.plusWeeks(monthWeekCount.toLong())
        }
        CalendarMode.WEEK -> {
            // Una settimana per lato: la settimana si scorre col dito e
            // quelle accanto sono già montate.
            windowStart = shownWeekStart.minusWeeks(1)
            windowEnd = shownWeekStart.plusWeeks(2).plusDays(6)
        }
    }

    // Le pagine con la loro data, calcolate una volta sola: ogni
    // settimana le riusa per capire quali la attraversano. Senza
    // proprietà data non ce n'è nessuna, e il calendario resta vuoto.
    //
    // **Una pagina che si ripete resta una pagina sola.** Qui una
    // regola diventa tante comparse quante ne servono per il periodo
    // mostrato, ma sono tutte la stessa riga del database: aprirne una
    // qualsiasi apre quella pagina, e modificarla cambia tutte le
    // volte, perché la cosa modificata è una sola.
    val dated = remember(state.rows, state.cellValues, dateColumn?.id, windowStart, windowEnd) {
        val column = dateColumn ?: return@remember emptyList()
        state.rows.flatMap { row ->
            val range = parseDateRange(state.cellValues[row.id to column.id].orEmpty())
                ?: return@flatMap emptyList()
            val rule = range.recurrence ?: return@flatMap listOf(row to range)
            val firstDay = range.start.toLocalDate()
            val startTime = range.start.toLocalDateTime().toLocalTime()
            // Un impegno di tre giorni che si ripete dura tre giorni
            // tutte le volte: la durata se la porta dietro.
            val lengthDays = range.end?.toLocalDate()
                ?.let { ChronoUnit.DAYS.between(firstDay, it) }
            rule.occurrences(firstDay, windowStart, windowEnd).map { day ->
                val occurrenceStart = LocalDateTime.of(day, startTime).toEpochMillis()
                row to DateRange(
                    start = occurrenceStart,
                    end = lengthDays?.let {
                        LocalDateTime.of(
                            day.plusDays(it),
                            range.end!!.toLocalDateTime().toLocalTime()
                        ).toEpochMillis()
                    },
                    hasTime = range.hasTime,
                    recurrence = rule
                )
            }
        }
    }
    val undated = remember(state.rows, state.cellValues, dateColumn?.id) {
        val column = dateColumn ?: return@remember state.rows
        state.rows.filter { state.cellValues[it.id to column.id].isNullOrBlank() }
    }

    val onAddOnDate: (LocalDate) -> Unit = { date ->
        onAddRowOnDate(date.atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli())
    }

    // Le righe si contano su tutto il database, una volta sola: non
    // dipendono da cosa si sta guardando, quindi non si rinumerano mai.
    //
    // Si contano sulle date **di partenza**, non sulle comparse
    // generate dalle ripetizioni: quelle cambiano da un mese all'altro
    // e farebbero ballare la riga di una pagina a seconda di dove ci
    // si trova, che è esattamente quello che questa riga fissa vuole
    // evitare.
    val baseDated = remember(state.rows, state.cellValues, dateColumn?.id) {
        val column = dateColumn ?: return@remember emptyList()
        state.rows.mapNotNull { row ->
            parseDateRange(state.cellValues[row.id to column.id].orEmpty())?.let { row to it }
        }
    }
    val rowIndexes = remember(baseDated) { calendarRowIndexes(baseDated) }
    // Quante righe riserva ogni settimana. È lo stesso numero ovunque —
    // la griglia delle righe è una cosa sola, non cambia da una
    // settimana all'altra — e il tetto serve solo a non far diventare
    // smisurato un database con moltissime pagine datate.
    val reservedRows = rowIndexes.size.coerceAtMost(CALENDAR_MAX_ROWS)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    anchor = when (mode) {
                        CalendarMode.YEAR -> anchor.minusYears(1)
                        CalendarMode.MONTH -> anchor.minusMonths(1)
                        CalendarMode.WEEK -> anchor.minusWeeks(1)
                    }
                }
            ) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = DbStrings.previous)
            }
            Text(
                text = when (mode) {
                    CalendarMode.YEAR -> anchor.year.toString()
                    CalendarMode.MONTH -> anchor.format(MONTH_FORMAT)
                    CalendarMode.WEEK -> {
                        val start = startOfWeek(anchor, firstDayOfWeek)
                        "${start.format(DAY_MONTH_FORMAT)} – ${start.plusDays(6).format(DATE_FORMAT)}"
                    }
                },
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    anchor = when (mode) {
                        CalendarMode.YEAR -> anchor.plusYears(1)
                        CalendarMode.MONTH -> anchor.plusMonths(1)
                        CalendarMode.WEEK -> anchor.plusWeeks(1)
                    }
                }
            ) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = DbStrings.next)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            Row(modifier = Modifier.align(Alignment.Center)) {
                CalendarMode.entries.forEachIndexed { index, option ->
                    if (index > 0) Spacer(modifier = Modifier.size(8.dp))
                    CalendarModeChip(
                        label = option.displayName(),
                        selected = option == mode,
                        onClick = { onSetMode(option) }
                    )
                }
            }

            // Riporta a oggi da qualunque distanza, in qualunque delle
            // tre modalità: allontanandosi di mesi o di anni tornare
            // indietro con le frecce diventa un lavoro.
            //
            // Sta lì sempre, anche quando si è già su oggi e non
            // farebbe niente: un pulsante che compare e sparisce
            // costringe a cercarlo ogni volta, e la riga cambierebbe
            // aspetto da sola mentre si sfoglia.
            Text(
                text = DbStrings.today,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(DarkSurface, RoundedCornerShape(percent = 50))
                    .clickable { anchor = today }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        if (mode == CalendarMode.YEAR) {
            CalendarYearGrid(
                year = anchor.year,
                today = today,
                firstDayOfWeek = firstDayOfWeek,
                dated = dated,
                // Un mese in miniatura si guarda, non ci si lavora:
                // toccarlo porta a vederlo per intero.
                onOpenMonth = { month ->
                    anchor = month.atDay(1)
                    onSetMode(CalendarMode.MONTH)
                }
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { offset ->
                    Text(
                        text = firstDayOfWeek.plus(offset.toLong())
                            .getDisplayName(TextStyle.SHORT, displayLocale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                    )
                }
            }
            HorizontalDivider()
        }

        if (mode == CalendarMode.WEEK) {
            CalendarWeekSwiper(
                weekStart = shownWeekStart,
                today = today,
                dated = dated,
                rowIndexes = rowIndexes,
                reservedRows = reservedRows,
                onOpenRow = onOpenRow,
                onRowLongPress = onRowLongPress,
                onAddRowOnDate = onAddOnDate,
                onWeekChange = { anchor = it }
            )
            HorizontalDivider()
        } else if (mode == CalendarMode.MONTH) {
            // La griglia parte dal primo giorno della settimana che
            // contiene il primo del mese, così le colonne restano
            // allineate ai giorni.
            val shownMonth = YearMonth.from(anchor)

            // Tutte le settimane riservano le stesse righe: è quello
            // che fa restare una barra sempre alla stessa altezza
            // scendendo da una settimana all'altra.
            val monthWeekHeight = (CALENDAR_WEEK_HEADER + CALENDAR_BAR_HEIGHT * reservedRows)
                .coerceAtLeast(CALENDAR_MONTH_WEEK_HEIGHT)

            repeat(monthWeekCount) { index ->
                val weekStart = monthGridStart.plusWeeks(index.toLong())
                CalendarWeekRow(
                    weekStart = weekStart,
                    shownMonth = shownMonth,
                    today = today,
                    segments = calendarSegments(weekStart, dated),
                    rowIndexes = rowIndexes,
                    maxRows = CALENDAR_MAX_ROWS,
                    height = monthWeekHeight,
                    onOpenRow = onOpenRow,
                    onRowLongPress = onRowLongPress,
                    onAddRowOnDate = onAddOnDate
                )
                HorizontalDivider()
            }
        }

        // Le righe senza data non hanno un giorno in cui stare, ma non
        // devono sparire: il calendario sarebbe l'unico posto da cui
        // non si riesce più a raggiungerle.
        if (undated.isNotEmpty()) {
            Text(
                text = DbStrings.noDate(undated.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )
            undated.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(row.id) {
                            detectTapGestures(
                                onTap = { onOpenRow(row) },
                                onLongPress = { onRowLongPress(row) }
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RowIconBadge(row.id, ROW_ICON_SIZE)
                    Text(
                        text = row.title.ifBlank { Strings.untitled },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Il primo giorno della settimana che contiene questa data. */
private fun startOfWeek(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate {
    val shift = ((date.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    return date.minusDays(shift.toLong())
}

private fun CalendarMode.displayName(): String = DbStrings.calendarModeName(this)

/** Quanto è alta una settimana quando è l'unica cosa sullo schermo. */
private fun weekViewHeight(barCount: Int): Dp =
    (CALENDAR_WEEK_HEADER + CALENDAR_BAR_HEIGHT * barCount)
        .coerceAtLeast(CALENDAR_WEEK_MIN_HEIGHT)

/**
 * La settimana che si sposta col dito.
 *
 * Lo scorrimento è un `HorizontalPager`, cioè lo stesso componente con
 * cui si sfogliano le pagine ovunque su Android. **Prima era scritto a
 * mano**: tre settimane affiancate, uno scostamento seguito col dito e
 * una molla per farlo atterrare. Funzionava sulla carta e nelle misure
 * fatte con trascinamenti sintetici risultava perfettamente fluido, ma
 * sotto il dito vero restava uno scatto che tre tentativi non hanno
 * tolto. Rifarsi la fisica dello scorrimento vuol dire rifare anche il
 * lancio, l'aggancio alla pagina, il gesto afferrato a metà corsa e la
 * velocità al rilascio: ognuna di quelle cose è un modo di sbagliare, e
 * quella giusta esiste già.
 *
 * Il pager conta le settimane da `origin`, che resta ferma per tutta la
 * vita del componente: così il numero di pagina di una settimana non
 * cambia mai sotto i piedi dello scorrimento.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarWeekSwiper(
    weekStart: LocalDate,
    today: LocalDate,
    dated: List<Pair<DatabaseRowEntity, DateRange>>,
    rowIndexes: Map<String, Int>,
    reservedRows: Int,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddRowOnDate: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit
) {
    val origin = remember { weekStart }
    fun pageOf(date: LocalDate) =
        WEEK_PAGER_CENTER + ChronoUnit.WEEKS.between(origin, date).toInt()

    fun weekOf(page: Int) = origin.plusWeeks((page - WEEK_PAGER_CENTER).toLong())

    val pagerState = rememberPagerState(
        initialPage = pageOf(weekStart),
        pageCount = { WEEK_PAGER_PAGES }
    )

    // Chi guarda deve sapere dove siamo arrivati, ma solo a scorrimento
    // finito: aggiornarlo mentre il dito è ancora giù rifarebbe i conti
    // delle righe a metà gesto.
    LaunchedEffect(pagerState.settledPage) {
        val settled = weekOf(pagerState.settledPage)
        if (settled != weekStart) onWeekChange(settled)
    }

    // E viceversa: se la settimana cambia da fuori (le frecce, Today)
    // il pager ci va senza animazione, perché il salto può essere di
    // mesi.
    LaunchedEffect(weekStart) {
        val target = pageOf(weekStart)
        if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(target)
        }
    }

    // Tutte le settimane sono alte uguali: se ognuna prendesse la
    // propria altezza, sfogliando si vedrebbe la riga crescere e
    // rimpicciolirsi. L'altezza tiene conto delle righe **riservate**,
    // non di quante barre ci sono davvero in una settimana: una riga
    // lasciata vuota da una pagina finita occupa spazio come le altre.
    val height = weekViewHeight(reservedRows)

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(height),
        verticalAlignment = Alignment.Top,
        // Le settimane ai lati vengono preparate **prima** che il dito
        // si muova. Senza, la settimana accanto viene composta e
        // misurata nell'istante in cui comincia il trascinamento — una
        // ventina di testi da impaginare proprio nel fotogramma in cui
        // serve tutto il tempo per scorrere, ed è lì che si vedeva lo
        // scatto.
        beyondBoundsPageCount = 1
    ) { page ->
        val start = weekOf(page)
        // Ricalcolare i pezzi ad ogni fotogramma di scorrimento è la
        // stessa scansione fatta sessanta volte al secondo per niente:
        // dipende solo dalla settimana e dalle date.
        val segments = remember(start, dated) { calendarSegments(start, dated) }
        CalendarWeekRow(
            weekStart = start,
            shownMonth = null,
            today = today,
            segments = segments,
            rowIndexes = rowIndexes,
            maxRows = reservedRows,
            height = height,
            onOpenRow = onOpenRow,
            onRowLongPress = onRowLongPress,
            onAddRowOnDate = onAddRowOnDate
        )
    }
}

/**
 * L'anno intero: dodici mesi in miniatura, tre per riga.
 *
 * A questa scala le barre non si leggerebbero, quindi non ci sono: i
 * giorni con almeno una pagina sono cerchiati, e basta a far vedere a
 * colpo d'occhio dove si concentra il lavoro. Toccando un mese lo si
 * apre da vicino, che è l'unica cosa sensata da fare con un mese alto
 * un centimetro.
 */
@Composable
private fun CalendarYearGrid(
    year: Int,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    dated: List<Pair<DatabaseRowEntity, DateRange>>,
    onOpenMonth: (YearMonth) -> Unit
) {
    val busyDays = remember(year, dated) { busyDaysOfYear(year, dated) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
        repeat(4) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) { columnIndex ->
                    val month = YearMonth.of(year, rowIndex * 3 + columnIndex + 1)
                    MiniMonth(
                        month = month,
                        today = today,
                        firstDayOfWeek = firstDayOfWeek,
                        busyDays = busyDays,
                        onClick = { onOpenMonth(month) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * I giorni dell'anno coperti da almeno una pagina.
 *
 * Tagliare ogni intervallo sull'anno mostrato mette da sé un tetto al
 * ciclo — al massimo trecentosessantasei passi per pagina — quindi una
 * data digitata per sbaglio nell'anno tremila non blocca niente.
 */
private fun busyDaysOfYear(
    year: Int,
    dated: List<Pair<DatabaseRowEntity, DateRange>>
): Set<LocalDate> {
    val yearStart = LocalDate.of(year, 1, 1)
    val yearEnd = LocalDate.of(year, 12, 31)
    val days = mutableSetOf<LocalDate>()
    dated.forEach { (_, range) ->
        val rangeStart = range.start.toLocalDate()
        var day = maxOf(rangeStart, yearStart)
        val last = minOf(range.end?.toLocalDate() ?: rangeStart, yearEnd)
        while (!day.isAfter(last)) {
            days.add(day)
            day = day.plusDays(1)
        }
    }
    return days
}

/** Un mese in miniatura dentro la vista dell'anno. */
@Composable
private fun MiniMonth(
    month: YearMonth,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    busyDays: Set<LocalDate>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridStart = startOfWeek(month.atDay(1), firstDayOfWeek)
    Column(
        modifier = modifier
            .pointerInput(month) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 2.dp, vertical = 5.dp)
    ) {
        Text(
            text = month.month.getDisplayName(TextStyle.SHORT, displayLocale),
            style = MaterialTheme.typography.labelSmall,
            color = if (YearMonth.from(today) == month) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
        )
        repeat(6) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayOfWeek ->
                    val date = gridStart.plusDays((week * 7 + dayOfWeek).toLong())
                    Box(
                        modifier = Modifier.weight(1f).height(MINI_DAY_SIZE),
                        contentAlignment = Alignment.Center
                    ) {
                        // I giorni del mese prima e dopo restano vuoti:
                        // dodici griglie affiancate diventano illeggibili
                        // se ognuna sconfina in quella accanto.
                        if (YearMonth.from(date) == month) {
                            Box(
                                modifier = Modifier
                                    .size(MINI_DAY_SIZE)
                                    .background(
                                        when {
                                            date == today -> MaterialTheme.colorScheme.primary
                                            date in busyDays -> DarkSurfaceVariant
                                            else -> Color.Transparent
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = MINI_DAY_FONT,
                                    lineHeight = MINI_DAY_FONT,
                                    color = if (date == today) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Il pulsantino Anno/Mese/Settimana sopra il calendario. */
@Composable
private fun CalendarModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .background(
                if (selected) DarkSurfaceVariant else Color.Transparent,
                RoundedCornerShape(percent = 50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/**
 * Un pezzo di pagina dentro una settimana: da quale colonna a quale
 * colonna arriva, e se prosegue oltre i bordi della settimana.
 *
 * Una pagina lunga due settimane produce due pezzi, uno per settimana:
 * è così che una barra può "spezzarsi" andando a capo e restare
 * riconoscibile come una cosa sola.
 */
@Immutable
private data class CalendarSegment(
    val row: DatabaseRowEntity,
    /** La data vera della pagina, non il giorno in cui la barra entra nella settimana. */
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val startColumn: Int,
    val endColumn: Int,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean
)

/** I pezzi di pagina che attraversano questa settimana. */
private fun calendarSegments(
    weekStart: LocalDate,
    dated: List<Pair<DatabaseRowEntity, DateRange>>
): List<CalendarSegment> {
    val weekEnd = weekStart.plusDays(6)

    return dated.mapNotNull { (row, range) ->
        val first = range.start.toLocalDate()
        val last = range.end?.toLocalDate() ?: first
        if (last.isBefore(weekStart) || first.isAfter(weekEnd)) return@mapNotNull null
        CalendarSegment(
            row = row,
            rangeStart = first,
            rangeEnd = last,
            startColumn = maxOf(0, ChronoUnit.DAYS.between(weekStart, first).toInt()),
            endColumn = minOf(6, ChronoUnit.DAYS.between(weekStart, last).toInt()),
            continuesBefore = first.isBefore(weekStart),
            continuesAfter = last.isAfter(weekEnd)
        )
    }
}

/**
 * A ogni pagina datata del database la sua riga, **sua per sempre**:
 * tutte le pagine ordinate per data, la prima in alto.
 *
 * Le pagine si comportano come blocchi impilati. Una pagina tiene la
 * sua riga in ogni settimana, in ogni mese e in ogni anno, e quando la
 * sua data finisce quella riga resta **vuota** per sempre invece di
 * essere occupata da un'altra: così le barre non si rimescolano mai e
 * seguirne una con l'occhio è immediato.
 *
 * Le righe si contano su **tutto il database** e non sul periodo che si
 * sta guardando — è quella la differenza, ed è una scelta esplicita
 * dell'utente. Contarle sul periodo le farebbe ricompattare ad ogni
 * cambio di mese: nessuna pagina ruberebbe la riga di un'altra, ma la
 * stessa pagina si troverebbe a un'altezza diversa a seconda di cosa
 * c'è attorno. Il prezzo è lo spazio vuoto: la decima pagina in ordine
 * di data sta alla decima riga anche nelle settimane in cui è l'unica,
 * con nove righe vuote sopra.
 *
 * A parità di data l'ordine è comunque sempre lo stesso, altrimenti due
 * pagine dello stesso giorno si scambierebbero di posto ad ogni
 * ridisegno.
 */
private fun calendarRowIndexes(
    dated: List<Pair<DatabaseRowEntity, DateRange>>
): Map<String, Int> {
    data class Placed(val row: DatabaseRowEntity, val start: Long, val end: Long)

    // L'ordine guarda l'istante e non il giorno: due pagine dello
    // stesso giorno con un'ora diversa hanno un ordine naturale, ed è
    // quello.
    return dated.map { (row, range) ->
        Placed(row, range.start, range.end ?: range.start)
    }.sortedWith(
        compareBy<Placed> { it.start }
            .thenBy { it.end }
            .thenBy { it.row.title }
            .thenBy { it.row.id }
    ).withIndex().associate { (index, placed) -> placed.row.id to index }
}

/**
 * La riga di una barra: la barra alla sua colonna, con gli spazi vuoti
 * prima e dopo.
 *
 * Le larghezze sono pesi e non misure fisse, così una barra di tre
 * giorni occupa esattamente tre settimi della riga qualunque sia la
 * larghezza dello schermo — che è il senso di "rispettare la lunghezza
 * dei giorni".
 */
@Composable
private fun CalendarBarRow(
    segment: CalendarSegment,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(CALENDAR_BAR_HEIGHT)) {
        if (segment.startColumn > 0) {
            Spacer(modifier = Modifier.weight(segment.startColumn.toFloat()))
        }
        val span = segment.endColumn - segment.startColumn + 1
        CalendarBar(
            segment = segment,
            onOpen = { onOpenRow(segment.row) },
            onLongPress = { onRowLongPress(segment.row) },
            modifier = Modifier.weight(span.toFloat())
        )
        val after = 6 - segment.endColumn
        if (after > 0) {
            Spacer(modifier = Modifier.weight(after.toFloat()))
        }
    }
}

/** La barra di una pagina: squadrata sui lati dove prosegue oltre la settimana. */
@Composable
private fun CalendarBar(
    segment: CalendarSegment,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val round = 4.dp
    val flat = 0.dp
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .background(
                DarkSurface,
                RoundedCornerShape(
                    topStart = if (segment.continuesBefore) flat else round,
                    bottomStart = if (segment.continuesBefore) flat else round,
                    topEnd = if (segment.continuesAfter) flat else round,
                    bottomEnd = if (segment.continuesAfter) flat else round
                )
            )
            .pointerInput(segment.row.id) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIconBadge(segment.row.id, BAR_ICON_SIZE)
            Text(
                text = segment.row.title.ifBlank { Strings.untitled },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}

/**
 * Una settimana: i numeri dei giorni e sopra di loro le barre.
 *
 * I giorni stanno sotto e le barre sopra, in due strati sovrapposti:
 * gli spazi vuoti fra una barra e l'altra non intercettano il tocco,
 * quindi toccare un punto libero della settimana crea una pagina in
 * quel giorno, mentre toccare una barra apre la sua pagina.
 */
@Composable
private fun CalendarWeekRow(
    weekStart: LocalDate,
    shownMonth: YearMonth?,
    today: LocalDate,
    segments: List<CalendarSegment>,
    rowIndexes: Map<String, Int>,
    maxRows: Int,
    height: Dp,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddRowOnDate: (LocalDate) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(7) { dayIndex ->
                val date = weekStart.plusDays(dayIndex.toLong())
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(date) {
                            detectTapGestures(onTap = { onAddRowOnDate(date) })
                        }
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayIndex ->
                    val date = weekStart.plusDays(dayIndex.toLong())
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = when {
                            date == today -> MaterialTheme.colorScheme.primary
                            shownMonth == null || YearMonth.from(date) == shownMonth ->
                                MaterialTheme.colorScheme.onBackground
                            // I giorni del mese prima e dopo restano
                            // visibili ma smorzati: servono a non
                            // spezzare la griglia.
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                    )
                }
            }

            // Ogni pagina alla riga che le spetta su tutto il periodo.
            // Le righe di chi in questa settimana non c'è restano vuote:
            // è quello che tiene le barre allineate da una settimana
            // all'altra.
            // Il posto di ogni barra dipende solo dai pezzi e dalle
            // righe: rifarlo ad ogni fotogramma di scorrimento è lavoro
            // buttato proprio quando il tempo serve tutto per scorrere.
            val placed = remember(segments, rowIndexes) {
                segments.mapNotNull { segment ->
                    rowIndexes[segment.row.id]?.let { index -> index to segment }
                }
            }
            val visible = placed.filter { it.first < maxRows }.toMap()
            val rowsToDraw = (visible.keys.maxOrNull()?.plus(1)) ?: 0
            repeat(rowsToDraw) { rowIndex ->
                val segment = visible[rowIndex]
                if (segment != null) {
                    CalendarBarRow(
                        segment = segment,
                        onOpenRow = onOpenRow,
                        onRowLongPress = onRowLongPress
                    )
                } else {
                    Spacer(modifier = Modifier.height(CALENDAR_BAR_HEIGHT))
                }
            }
            val hidden = placed.size - visible.size
            if (hidden > 0) {
                Text(
                    text = "+$hidden",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * La visualizzazione a elenco: le stesse righe senza la griglia, con il
 * nome in evidenza e i valori delle proprietà di fianco, come su Notion.
 *
 * Qui il nome non si scrive sul posto: si tocca la riga e si apre la
 * pagina, che è dove in questa vista si lavora. Il tocco prolungato
 * apre le azioni, come nella tabella.
 */
@Composable
private fun ListLayout(
    state: DatabaseTableState,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        state.rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(row.id) {
                        detectTapGestures(
                            onTap = { onOpenRow(row) },
                            onLongPress = { onRowLongPress(row) }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowIconBadge(row.id, ROW_ICON_SIZE)
                Text(
                    text = row.title.ifBlank { Strings.untitled },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (row.title.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // A destra i valori che ci stanno, come fa Notion: solo
                // quelli valorizzati, altrimenti la riga si riempirebbe
                // di trattini.
                state.visibleColumns.take(2).forEach { column ->
                    val value = state.cellValues[row.id to column.id].orEmpty()
                    if (value.isNotBlank()) {
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = listCellLabel(column, value),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

/**
 * Le misure della galleria.
 *
 * Le schede non hanno una larghezza fissa ma **una larghezza minima**
 * per ogni dimensione: in una riga ne entrano quante ci stanno, e si
 * allargano insieme a riempirla. Su un telefono in verticale sono tre
 * piccole, due medie o una grande per riga; girandolo, o su uno schermo
 * più largo, ne entrano di più invece di diventare enormi.
 */
private val GALLERY_MIN_CARD_SMALL = 100.dp
private val GALLERY_MIN_CARD_MEDIUM = 150.dp
private val GALLERY_MIN_CARD_LARGE = 280.dp
private val GALLERY_GAP = 10.dp

/**
 * Larghezza su altezza della parte alta di una scheda. È più bassa della
 * striscia di una copertina di pagina in proporzione, ma non troppo:
 * dentro deve ancora capirsi cos'è l'immagine, e l'anteprima del testo
 * deve avere posto per qualche riga.
 */
private const val GALLERY_PREVIEW_RATIO = 1.6f

/** Quante schede stanno in una riga larga `width`, con la dimensione scelta. */
private fun galleryColumnsFor(width: Dp, size: GalleryCardSize): Int {
    val minCard = when (size) {
        GalleryCardSize.SMALL -> GALLERY_MIN_CARD_SMALL
        GalleryCardSize.MEDIUM -> GALLERY_MIN_CARD_MEDIUM
        GalleryCardSize.LARGE -> GALLERY_MIN_CARD_LARGE
    }
    // Fra n schede ci sono n - 1 spazi: aggiungerne uno alla larghezza
    // disponibile rende il conto giusto con una divisione sola.
    return ((width + GALLERY_GAP) / (minCard + GALLERY_GAP)).toInt().coerceAtLeast(1)
}

/**
 * La visualizzazione a galleria: una griglia di schede, una per pagina,
 * con in alto la copertina della pagina o l'inizio del suo testo e sotto
 * il nome e le proprietà. Come la bacheca e l'elenco, si tocca una
 * scheda per aprire la pagina e la si tiene premuta per le azioni.
 *
 * **Non è una griglia pigra** (`LazyVerticalGrid`), come non lo sono le
 * altre viste: il database dentro una pagina sta in una lista che già
 * scorre, e una griglia che scorre per conto suo lì dentro non si può
 * mettere. Il prezzo è che tutte le schede si costruiscono insieme,
 * immagini comprese — vedi "Limiti noti" nel README.
 *
 * Le schede di una stessa riga sono **alte uguali**: una con tre
 * proprietà accanto a una senza lascerebbe un gradino a metà della
 * griglia, e la riga dopo sembrerebbe cominciare storta.
 */
@Composable
private fun GalleryLayout(
    state: DatabaseTableState,
    preview: GalleryCardPreview,
    size: GalleryCardSize,
    covers: Map<String, RowCover>,
    contentPreviews: Map<String, List<RowPreviewLine>>,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val columns = galleryColumnsFor(maxWidth, size)
        Column(verticalArrangement = Arrangement.spacedBy(GALLERY_GAP)) {
            state.rows.chunked(columns).forEach { cardsInRow ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(GALLERY_GAP)
                ) {
                    cardsInRow.forEach { row ->
                        GalleryCard(
                            row = row,
                            state = state,
                            preview = preview,
                            size = size,
                            cover = covers[row.id],
                            contentLines = contentPreviews[row.id],
                            onOpen = { onOpenRow(row) },
                            onLongPress = { onRowLongPress(row) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    // L'ultima riga, se non è piena, tiene le schede
                    // larghe come le altre invece di allargarle a
                    // riempire lo spazio: una scheda grande il doppio in
                    // fondo sembrerebbe più importante delle altre.
                    repeat(columns - cardsInRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Una scheda della galleria: l'anteprima in alto, poi nome e proprietà.
 *
 * L'anteprima occupa il suo posto **anche quando è vuota** — una pagina
 * senza copertina, o senza testo — così le schede restano allineate e
 * si vede dove comparirebbe. Con "None" invece non c'è proprio.
 */
@Composable
private fun GalleryCard(
    row: DatabaseRowEntity,
    state: DatabaseTableState,
    preview: GalleryCardPreview,
    size: GalleryCardSize,
    cover: RowCover?,
    contentLines: List<RowPreviewLine>?,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember(context) { PageImageStore(context) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .pointerInput(row.id) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
    ) {
        if (preview != GalleryCardPreview.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(GALLERY_PREVIEW_RATIO)
                    .background(DarkSurfaceVariant)
            ) {
                if (preview == GalleryCardPreview.PAGE_COVER && cover != null) {
                    // La stessa inquadratura scelta nella pagina con
                    // "Reposition", tenuta dentro i bordi: la scheda ha
                    // un'altra forma della striscia della pagina.
                    CoverImage(
                        fileName = cover.coverImage,
                        store = store,
                        scale = cover.coverScale,
                        offsetFraction = Offset(cover.coverOffsetX, cover.coverOffsetY),
                        onImageSize = {},
                        clampOffset = true,
                        modifier = Modifier.matchParentSize()
                    )
                }
                if (preview == GalleryCardPreview.PAGE_CONTENT && !contentLines.isNullOrEmpty()) {
                    GalleryContentPreview(
                        lines = contentLines,
                        size = size,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIconBadge(row.id, ROW_ICON_SIZE)
                Text(
                    text = row.title.ifBlank { Strings.untitled },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (row.title.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Le proprietà come sulle schede della bacheca: solo quelle
            // che si vedono e che hanno un valore, una per riga.
            state.visibleColumns.forEach { column ->
                val value = state.cellValues[row.id to column.id].orEmpty()
                val label = listCellLabel(column, value)
                if (label.isNotBlank()) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * L'inizio del testo della pagina, in piccolo, dentro la parte alta
 * della scheda: "Page content" di Notion.
 *
 * Ogni riga si scrive come nella pagina — gli elenchi col loro segno (lo stesso della pagina, che cambia a ogni
 * rientro), le caselle spuntate o no, i toggle con la freccia — perché
 * l'anteprima serve a riconoscere la pagina, e una pagina si riconosce
 * anche dalla sua forma. Quello che non ci sta viene tagliato dal bordo
 * della scheda, non riassunto.
 */
@Composable
private fun GalleryContentPreview(
    lines: List<RowPreviewLine>,
    size: GalleryCardSize,
    modifier: Modifier = Modifier
) {
    // Il corpo del testo segue la scheda: in quella piccola ci stanno
    // tre dita di larghezza, e un testo normale mostrerebbe due parole.
    val bodySize = when (size) {
        GalleryCardSize.SMALL -> 8.sp
        GalleryCardSize.MEDIUM -> 10.sp
        GalleryCardSize.LARGE -> 12.sp
    }
    val indentStep = when (size) {
        GalleryCardSize.SMALL -> 6.dp
        GalleryCardSize.MEDIUM -> 8.dp
        GalleryCardSize.LARGE -> 12.dp
    }
    Column(
        modifier = modifier
            .clipToBounds()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        lines.forEach { line ->
            val marker = when (line.type) {
                BlockType.BULLET_LIST_ITEM -> bulletMarkerFor(line.indentLevel) + " "
                BlockType.NUMBERED_LIST_ITEM -> numberedMarkerFor(line.indentLevel, line.ordinal) + " "
                BlockType.CHECKBOX -> if (line.isChecked) "☑ " else "☐ "
                BlockType.TOGGLE -> "▸ "
                else -> ""
            }
            // Il rientro vale solo per il testo scorrevole, come nella
            // pagina: i toggle e le caselle non hanno livelli.
            val indent = when (line.type) {
                BlockType.CHECKBOX, BlockType.TOGGLE -> 0
                else -> line.indentLevel
            }
            Text(
                text = marker + line.text,
                fontSize = bodySize,
                lineHeight = bodySize * 1.3f,
                color = if (line.type == BlockType.CHECKBOX && line.isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                textDecoration = if (line.type == BlockType.CHECKBOX && line.isChecked) {
                    TextDecoration.LineThrough
                } else {
                    null
                },
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = indentStep * indent.coerceAtMost(4))
            )
        }
    }
}

// `DateRange`, `parseDateRange` e `encodeDateRange` stanno in
// `data/entity/DateValue.kt`: il formato del valore serve anche ai
// filtri, che girano nel ViewModel.

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(AppSettings.zoneId).toLocalDate()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(AppSettings.zoneId).toLocalDateTime()

private fun LocalDateTime.toEpochMillis(): Long =
    atZone(AppSettings.zoneId).toInstant().toEpochMilli()

/**
 * I millisecondi che il selettore di Material usa: mezzanotte **UTC**
 * del giorno scelto, non mezzanotte locale. Confonderli sposta la data
 * di un giorno in tutti i fusi a occidente di Greenwich, quindi la
 * conversione si fa sempre passando da qui.
 */
private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.pickerMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** "12 Jan 2026", "12 Jan 2026, 09:30" oppure con la freccia e la fine. */
private fun DateRange.label(): String {
    val format = if (hasTime) DATE_TIME_FORMAT else DATE_FORMAT
    val startText = Instant.ofEpochMilli(start)
        .atZone(AppSettings.zoneId).format(format)
    val endText = end?.let {
        Instant.ofEpochMilli(it).atZone(AppSettings.zoneId).format(format)
    }
    return if (endText == null) startText else "$startText → $endText"
}

/** La mezzanotte di oggi, come la salvano le celle di tipo data. */
private fun todayAtStartOfDay(): Long =
    LocalDate.now().atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli()

/** Il valore di una cella reso leggibile in una riga sola, per la vista a elenco. */
private fun listCellLabel(column: DatabaseColumnEntity, value: String): String = when (column.type) {
    ColumnType.CHECKBOX -> if (value == "true") "✓" else ""
    ColumnType.DATE -> parseDateRange(value)?.label() ?: value
    ColumnType.MULTI_SELECT -> value.split(MULTI_VALUE_SEPARATOR)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    else -> value
}

/**
 * Quanto sono grandi le icone della barra della vista: 20dp invece dei
 * 24 di serie, dentro un bottone da 40 invece che da 48. Sono numeri
 * unici apposta — le icone lì sopra devono restare tutte della stessa
 * misura, comprese quelle che si aggiungeranno (ricerca, filtri), e
 * scriverle ogni volta a mano è il modo sicuro perché prima o poi una
 * sia diversa dalle altre. Per aggiungerne una si usa
 * `ToolbarIconButton`, non un `IconButton` nudo.
 */
private val TOOLBAR_ICON_SIZE = 20.dp
private val TOOLBAR_BUTTON_SIZE = 40.dp

/** Un'icona della barra della vista. Vedi `TOOLBAR_ICON_SIZE`. */
@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(onClick = onClick, modifier = Modifier.size(TOOLBAR_BUTTON_SIZE)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(TOOLBAR_ICON_SIZE),
            tint = tint
        )
    }
}

/**
 * La riga sotto al titolo: a sinistra la vista attiva, a destra filtro,
 * ordinamento e impostazioni del database. Su Notion qui c'è anche la
 * ricerca, che manca perché non è ancora stata costruita, e un pulsante
 * blu per aggiungere una pagina, tolto perché faceva la stessa identica
 * cosa di "+ New page" in fondo alla tabella.
 */
@Composable
private fun ViewToolbar(
    layout: DatabaseLayout,
    sortActive: Boolean,
    filterActive: Boolean,
    /** Null quando il database è già a schermo intero: non c'è dove aprirlo. */
    onOpenFullPage: (() -> Unit)?,
    /**
     * Null quando la vista è bloccata ("Lock view"): il comando resta
     * **al suo posto, spento**, invece di sparire. Un pulsante che
     * scompare fa pensare a un guasto; uno spento dice che c'è ed è
     * stato messo a riposo apposta.
     */
    onOpenFilter: (() -> Unit)?,
    onOpenSort: (() -> Unit)?,
    onOpenLayoutPicker: (() -> Unit)?,
    onOpenSettings: () -> Unit
) {
    val lockedTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(percent = 50)
                )
                .let { base ->
                    if (onOpenLayoutPicker == null) base else base.clickable { onOpenLayoutPicker() }
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chipTint = if (onOpenLayoutPicker == null) {
                lockedTint
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                layout.icon(),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = chipTint
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                layout.displayName(),
                style = MaterialTheme.typography.labelLarge,
                color = chipTint
            )
            // La freccia dice che si può toccare: prima sembrava
            // un'etichetta e infatti non faceva niente. Con la vista
            // bloccata resta, spenta come il resto: sparendo cambierebbe
            // la larghezza del riquadro a ogni giro di chiave.
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = DbStrings.changeLayout,
                modifier = Modifier.size(18.dp),
                tint = chipTint
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Il filtro sta prima dell'ordinamento perché è l'ordine in
            // cui agiscono: prima si sceglie **quali** pagine si
            // vedono, poi in che ordine.
            ToolbarIconButton(
                icon = Icons.Filled.FilterList,
                contentDescription = DbStrings.filter,
                onClick = onOpenFilter ?: {},
                // Acceso quando un filtro c'è: senza, una tabella con
                // metà delle pagine sembrerebbe una tabella che ha
                // perso delle pagine. Con la vista bloccata invece si
                // spegne, filtro o non filtro: lì non si cambia niente.
                tint = when {
                    onOpenFilter == null -> lockedTint
                    filterActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            ToolbarIconButton(
                icon = Icons.Filled.Sort,
                contentDescription = DbStrings.sort,
                onClick = onOpenSort ?: {},
                // Acceso quando un ordinamento c'è: altrimenti non si
                // saprebbe che le righe non sono più nell'ordine in
                // cui sono state create.
                tint = when {
                    onOpenSort == null -> lockedTint
                    sortActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            // Aprire a schermo intero è una cosa che si fa spesso e di
            // fretta — la tabella dentro la pagina sta stretta — quindi
            // sta qui e non dentro una finestra. Cancellare no: quella
            // resta nelle impostazioni, dove non la si tocca per
            // sbaglio.
            if (onOpenFullPage != null) {
                ToolbarIconButton(
                    icon = Icons.Filled.OpenInFull,
                    contentDescription = DbStrings.openFullPage,
                    onClick = onOpenFullPage
                )
            }
            ToolbarIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = DbStrings.databaseSettings,
                onClick = onOpenSettings
            )
        }
    }
}

/** Una riga delle impostazioni che accende e spegne invece di aprire qualcosa. */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * La finestra dell'ordinamento: prima quello attivo, poi le proprietà
 * fra cui scegliere.
 *
 * Il verso si cambia toccando l'ordinamento attivo, non con due voci
 * separate per crescente e decrescente: sono due stati della stessa
 * cosa, e un interruttore dice meglio di una lista che si può
 * rovesciare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    columns: List<DatabaseColumnEntity>,
    sortColumnId: String?,
    descending: Boolean,
    onPick: (String) -> Unit,
    onToggleDirection: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = DbStrings.sort,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(12.dp))

            if (sortColumnId != null) {
                val activeName = if (sortColumnId == SORT_BY_NAME) {
                    DbStrings.name
                } else {
                    columns.find { it.id == sortColumnId }?.name?.let(DbStrings::columnName) ?: DbStrings.name
                }
                SheetGroup {
                    SettingsRow(
                        icon = if (descending) {
                            Icons.Filled.ArrowDownward
                        } else {
                            Icons.Filled.ArrowUpward
                        },
                        label = activeName,
                        value = if (descending) Strings.descending else Strings.ascending,
                        onClick = onToggleDirection
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Filled.Delete,
                        label = DbStrings.removeSort,
                        value = "",
                        onClick = onClear,
                        danger = true
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    text = DbStrings.sortByAnother,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(8.dp))
            }

            SheetGroup {
                // Il nome della pagina è una colonna della tabella ma
                // non una proprietà: va elencato a mano, e per primo,
                // perché è quello per cui si ordina più spesso.
                SettingsRow(
                    icon = Icons.Filled.Notes,
                    label = DbStrings.name,
                    value = "",
                    onClick = { onPick(SORT_BY_NAME) }
                )
                columns.forEach { column ->
                    HorizontalDivider()
                    SettingsRow(
                        icon = column.type.icon(),
                        label = DbStrings.columnName(column.name),
                        value = "",
                        onClick = { onPick(column.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Le proprietà su cui si può filtrare: tag, date, numeri e caselle da
 * spuntare.
 *
 * Restano fuori il testo libero, l'indirizzo, la mail e il telefono:
 * lì la domanda sarebbe "contiene queste lettere", cioè una ricerca, e
 * la ricerca è un'altra cosa che ancora non c'è. Restano fuori anche
 * "creata il" e "modificata il", che non sono celle ma proprietà della
 * riga.
 */
private fun DatabaseColumnEntity.isFilterable(): Boolean =
    type == ColumnType.SELECT ||
        type == ColumnType.MULTI_SELECT ||
        type == ColumnType.DATE ||
        type == ColumnType.NUMBER ||
        type == ColumnType.CHECKBOX

/**
 * Il filtro detto a parole: "Status: Done, In corso" oppure
 * "Date: 12 Jan 2026 → 15 Jan 2026".
 */
private fun filterSummary(column: DatabaseColumnEntity?, value: String): String {
    if (column == null || value.isBlank()) return DbStrings.none
    when (column.type) {
        ColumnType.DATE -> {
            val range = parseDateRange(value) ?: return DbStrings.none
            return "${DbStrings.columnName(column.name)}: ${range.label()}"
        }
        ColumnType.NUMBER -> {
            val parts = value.split(MULTI_VALUE_SEPARATOR)
            val min = parts.getOrNull(0).orEmpty()
            val max = parts.getOrNull(1).orEmpty()
            val text = when {
                min.isNotBlank() && max.isNotBlank() -> "${Formats.number(min)} – ${Formats.number(max)}"
                min.isNotBlank() -> "≥ ${Formats.number(min)}"
                max.isNotBlank() -> "≤ ${Formats.number(max)}"
                else -> return DbStrings.none
            }
            return "${DbStrings.columnName(column.name)}: $text"
        }
        ColumnType.CHECKBOX -> {
            val chosen = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
            if (chosen.isEmpty()) return DbStrings.none
            return "${DbStrings.columnName(column.name)}: " + chosen.joinToString(", ") {
                if (it == "true") DbStrings.checked else DbStrings.unchecked
            }
        }
        else -> {
            val labels = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
            if (labels.isEmpty()) return DbStrings.none
            return "${DbStrings.columnName(column.name)}: ${labels.joinToString(", ")}"
        }
    }
}

/**
 * La finestra del filtro: prima si sceglie la proprietà, poi i valori.
 *
 * È una sola schermata raggiunta da due posti — l'icona nella barra e
 * la voce nelle impostazioni — invece di due schermate gemelle: due
 * copie si sarebbero separate al primo cambiamento, e sarebbe stato il
 * tipo di differenza che si nota solo usandole.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    columns: List<DatabaseColumnEntity>,
    filterColumnId: String?,
    filterValue: String,
    onSetFilter: (String?, String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    // Quale proprietà si sta componendo. Parte da quella già filtrata,
    // così riaprendo il filtro si torna dove lo si era lasciato invece
    // che all'elenco delle proprietà.
    var chosenId by remember(filterColumnId) { mutableStateOf(filterColumnId) }
    val chosen = columns.find { it.id == chosenId }
    val filterable = columns.filter { it.isFilterable() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chosen != null) {
                    IconButton(onClick = { chosenId = null }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = Strings.back)
                    }
                }
                Text(
                    text = DbStrings.filter,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Il cestino c'è solo quando c'è davvero un filtro da
                // buttare: sempre visibile sarebbe un pulsante che per
                // lo più non fa niente.
                if (filterColumnId != null) {
                    IconButton(onClick = {
                        onClear()
                        chosenId = null
                    }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = DbStrings.removeFilter,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))

            if (chosen == null) {
                if (filterable.isEmpty()) {
                    Text(
                        text = DbStrings.filterNeedsProperty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    SheetGroup {
                        filterable.forEachIndexed { index, column ->
                            if (index > 0) HorizontalDivider()
                            SettingsRow(
                                icon = column.type.icon(),
                                label = DbStrings.columnName(column.name),
                                value = if (column.id == filterColumnId) "✓" else null,
                                onClick = { chosenId = column.id }
                            )
                        }
                    }
                }
            } else if (chosen.type == ColumnType.DATE) {
                DateFilterEditor(
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            } else if (chosen.type == ColumnType.NUMBER) {
                NumberFilterEditor(
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            } else if (chosen.type == ColumnType.CHECKBOX) {
                CheckboxFilterEditor(
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            } else {
                TagFilterEditor(
                    column = chosen,
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * La scelta dei tag da far passare: si spuntano, e se ne possono
 * spuntare più d'uno.
 */
@Composable
private fun TagFilterEditor(
    column: DatabaseColumnEntity,
    value: String,
    onChange: (String) -> Unit
) {
    val chosen = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.toSet()
    val options = parseSelectOptionsFull(column.optionsJson)

    if (options.isEmpty()) {
        Text(
            text = DbStrings.noOptionsYet(DbStrings.columnName(column.name)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        return
    }

    Text(
        text = DbStrings.pagesWithAny,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    SheetGroup {
        options.forEachIndexed { index, option ->
            if (index > 0) HorizontalDivider()
            val isChosen = option.label in chosen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = if (isChosen) chosen - option.label else chosen + option.label
                        onChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TagChip(label = option.label, color = option.color)
                }
                if (isChosen) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * L'intervallo di numeri da far passare: da x a y.
 *
 * I due estremi sono facoltativi uno per volta — solo "da" vuol dire
 * "da lì in su", solo "a" vuol dire "fino a lì" — perché metà delle
 * domande vere sono aperte da un lato ("sopra i 100") e obbligare a
 * scrivere un secondo numero inventato le renderebbe sbagliate.
 */
@Composable
private fun NumberFilterEditor(
    value: String,
    onChange: (String) -> Unit
) {
    val parts = value.split(MULTI_VALUE_SEPARATOR)
    val min = parts.getOrNull(0).orEmpty()
    val max = parts.getOrNull(1).orEmpty()

    Text(
        text = DbStrings.pagesWithNumberRange,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = min,
            onValueChange = { onChange(it.filterNumeric() + MULTI_VALUE_SEPARATOR + max) },
            label = { Text(DbStrings.from) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = max,
            onValueChange = { onChange(min + MULTI_VALUE_SEPARATOR + it.filterNumeric()) },
            label = { Text(DbStrings.to) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.size(8.dp))
    Text(
        text = DbStrings.numberRangeHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Tiene solo quello che può far parte di un numero.
 *
 * La tastiera numerica di Android ha lo stesso delle lettere su certi
 * modelli, e un incolla può portare qualunque cosa: un estremo che non
 * è un numero non filtrerebbe niente, e non si capirebbe perché.
 */
private fun String.filterNumeric(): String =
    filterIndexed { index, c ->
        c.isDigit() || c == '.' || (c == '-' && index == 0)
    }

/** Spuntate, non spuntate, o tutte e due. */
@Composable
private fun CheckboxFilterEditor(
    value: String,
    onChange: (String) -> Unit
) {
    val chosen = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.toSet()

    Text(
        text = DbStrings.pagesWithBox,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    SheetGroup {
        listOf("true" to DbStrings.checked, "false" to DbStrings.unchecked).forEachIndexed { index, (key, label) ->
            if (index > 0) HorizontalDivider()
            val isChosen = key in chosen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = if (isChosen) chosen - key else chosen + key
                        onChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (key == "true") {
                        Icons.Filled.CheckBox
                    } else {
                        Icons.Filled.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(14.dp))
                Text(label, modifier = Modifier.weight(1f))
                if (isChosen) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * La scelta dei giorni da far passare: un giorno solo, oppure da un
 * giorno a un altro.
 *
 * "Fino a" è facoltativo apposta: il caso normale è un giorno solo, e
 * chiedere due date per filtrarne una sarebbe un passaggio in più ogni
 * volta.
 */
@Composable
private fun DateFilterEditor(
    value: String,
    onChange: (String) -> Unit
) {
    val range = parseDateRange(value)
    var picking by remember { mutableStateOf<DateFilterEdge?>(null) }

    Text(
        text = DbStrings.pagesOnDays,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    SheetGroup {
        SettingsRow(
            icon = Icons.Filled.CalendarToday,
            label = if (range?.end == null) DbStrings.day else DbStrings.from,
            value = range?.start?.toLocalDate()?.format(DATE_FORMAT) ?: DbStrings.pickADay,
            onClick = { picking = DateFilterEdge.START }
        )
        // La fine si può scegliere solo quando c'è un inizio: "fino al
        // 5" senza un "dal" non vuol dire niente.
        if (range != null) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsRow(
                        icon = Icons.Filled.Schedule,
                        label = DbStrings.to,
                        value = range.end?.toLocalDate()?.format(DATE_FORMAT) ?: DbStrings.sameDay,
                        onClick = { picking = DateFilterEdge.END }
                    )
                }
                if (range.end != null) {
                    IconButton(onClick = { onChange(encodeDateRange(range.start)) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = DbStrings.filterSingleDay,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    picking?.let { edge ->
        val initial = when (edge) {
            DateFilterEdge.START -> range?.start?.toLocalDate() ?: LocalDate.now()
            DateFilterEdge.END -> range?.end?.toLocalDate()
                ?: range?.start?.toLocalDate()
                ?: LocalDate.now()
        }
        DayPickerDialog(
            initial = initial,
            onPick = { day ->
                val millis = day.atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli()
                when (edge) {
                    DateFilterEdge.START -> {
                        // Spostando l'inizio oltre la fine, la fine
                        // sparisce invece di restare dietro: un
                        // intervallo al contrario non filtrerebbe più
                        // niente, e non si capirebbe perché.
                        val end = range?.end?.takeIf { it >= millis }
                        onChange(encodeDateRange(millis, end))
                    }
                    DateFilterEdge.END -> {
                        val start = range?.start ?: millis
                        onChange(encodeDateRange(start, millis.takeIf { it >= start }))
                    }
                }
            },
            onDismiss = { picking = null }
        )
    }
}

/** Quale dei due estremi del filtro per data si sta scegliendo. */
private enum class DateFilterEdge { START, END }

/** La linea verticale fra due celle della tabella. */
@Composable
private fun CellSeparator() {
    VerticalDivider(modifier = Modifier.height(ROW_HEIGHT))
}

@Composable
private fun NameHeaderCell() {
    Row(
        modifier = Modifier
            .width(NAME_COLUMN_WIDTH)
            .height(ROW_HEIGHT)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Aa",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            DbStrings.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * L'intestazione di una colonna. Toccarla apre la finestra della
 * proprietà, come su Notion; **tenendola premuta** compare il menu con,
 * nell'ordine, "Move to left", "Move to right", "Edit property",
 * "Center" (solo per le caselle) e "Hide" per togliere di mezzo la
 * colonna senza cancellarla.
 *
 * Gli spostamenti che non si possono fare restano **grigi** invece di
 * sparire: la prima colonna dopo Name non va più a sinistra, l'ultima non
 * va più a destra, e con "Lock view" non si sposta niente. Una voce che
 * sparisce e ricompare a seconda della colonna farebbe cambiare posto
 * alle altre sotto il dito.
 *
 * Nascondere sta qui e non solo nelle impostazioni perché è una cosa
 * che si decide guardando la tabella — "questa non mi serve adesso" —
 * e cercarla in un menu a tre livelli vorrebbe dire non usarla mai.
 * Rimostrarla invece sta nelle impostazioni: una colonna nascosta non
 * ha più un'intestazione da tenere premuta.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PropertyHeaderCell(
    column: DatabaseColumnEntity,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onHide: () -> Unit,
    onToggleCenter: () -> Unit
) {
    var menuOpen by remember(column.id) { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .width(COLUMN_WIDTH)
                .height(ROW_HEIGHT)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuOpen = true }
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                column.type.icon(),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = DbStrings.columnName(column.name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.background(DarkSheet)
        ) {
            // Gli spostamenti stanno **in cima**, sopra Edit property:
            // l'ha chiesto l'utente, perché spostare è la cosa per cui si
            // tiene premuta un'intestazione (per modificarla basta
            // toccarla). In cima e sempre nello stesso posto, grigie
            // quando non si può, si trovano senza leggere il menu.
            DropdownMenuItem(
                text = { Text(DbStrings.moveLeft) },
                leadingIcon = {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null)
                },
                enabled = canMoveLeft,
                onClick = {
                    menuOpen = false
                    onMove(-1)
                }
            )
            DropdownMenuItem(
                text = { Text(DbStrings.moveRight) },
                leadingIcon = {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
                },
                enabled = canMoveRight,
                onClick = {
                    menuOpen = false
                    onMove(1)
                }
            )
            DropdownMenuItem(
                text = { Text(DbStrings.editProperty) },
                leadingIcon = {
                    Icon(Icons.Filled.Tune, contentDescription = null)
                },
                onClick = {
                    menuOpen = false
                    onEdit()
                }
            )
            // Solo per le caselle da spuntare: è l'unico contenuto di
            // cella che è un segno solo, dove l'allineamento si nota.
            // Un testo o una data centrati renderebbero la colonna
            // illeggibile, perché le righe non partirebbero più tutte
            // dallo stesso punto.
            if (column.type == ColumnType.CHECKBOX) {
                DropdownMenuItem(
                    text = {
                        Text(if (column.centerContent) DbStrings.alignLeft else DbStrings.center)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (column.centerContent) {
                                Icons.Filled.FormatAlignLeft
                            } else {
                                Icons.Filled.FormatAlignCenter
                            },
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleCenter()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(DbStrings.hide) },
                leadingIcon = {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = null)
                },
                onClick = {
                    menuOpen = false
                    onHide()
                }
            )
        }
    }
}

@Composable
private fun AddPropertyCell(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(ADD_PROPERTY_WIDTH)
            .height(ROW_HEIGHT)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = DbStrings.addProperty,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * La cella del nome: il titolo della riga, più il pulsante OPEN che
 * apre la riga come pagina vera. Il tocco prolungato apre le azioni
 * della riga — su Notion è lo stesso gesto.
 *
 * Il nome è testo semplice finché non lo si tocca, e solo allora
 * diventa un campo di scrittura. Non è un vezzo: un campo di testo si
 * prende lui il tocco prolungato per selezionare le parole, quindi
 * finché ce n'era sempre uno il gesto non arrivava mai qui e comparivano
 * invece i pallini della selezione. Così tocco e tocco prolungato sono
 * gestiti da un unico rilevatore e non possono contendersi il gesto.
 */
@Composable
private fun NameCell(
    row: DatabaseRowEntity,
    onTitleChange: (String) -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    var editing by remember(row.id) { mutableStateOf(false) }
    var localTitle by remember(row.id) { mutableStateOf(row.title) }
    val focusRequester = remember { FocusRequester() }
    // Il campo appena creato non ha ancora il fuoco, e `onFocusChanged`
    // scatta subito dicendo proprio quello: senza questa memoria la
    // cella si richiuderebbe nell'istante in cui la si apre, e il tocco
    // sembrerebbe non fare niente.
    var everFocused by remember(row.id) { mutableStateOf(false) }

    // Se il titolo cambia da fuori (un'altra schermata, Annulla) mentre
    // non lo stiamo scrivendo noi, adeguiamoci.
    LaunchedEffect(row.title) {
        if (!editing) localTitle = row.title
    }

    Row(
        modifier = Modifier
            .width(NAME_COLUMN_WIDTH)
            .height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // L'icona della pagina, se c'è, sta fuori dal campo del nome: resta
        // al suo posto anche mentre il nome si scrive.
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowIconBadge(row.id, ROW_ICON_SIZE)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (editing) {
                    BasicTextField(
                        value = localTitle,
                        onValueChange = { newTitle ->
                            // Niente a capo nei nomi delle righe. `singleLine`
                            // fa già mostrare alla tastiera "fine" al posto di
                            // invio, ma non copre l'incolla di un testo che
                            // contiene a capo: quelli li togliamo qui.
                            val singleLineTitle = newTitle.replace("\n", "")
                            localTitle = singleLineTitle
                            onTitleChange(singleLineTitle)
                        },
                        singleLine = true,
                        // Database bloccato: il nome della riga si legge e
                        // si seleziona, non si riscrive.
                        readOnly = LocalDatabaseLocked.current,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { editing = false }),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    everFocused = true
                                } else if (everFocused) {
                                    editing = false
                                }
                            }
                    )
                    LaunchedEffect(Unit) {
                        everFocused = false
                        focusRequester.requestFocus()
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(row.id) {
                                detectTapGestures(
                                    onTap = { editing = true },
                                    onLongPress = { onLongPress() }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = localTitle.ifBlank { Strings.untitled },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (localTitle.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
        // Niente OPEN in un database semplice: la riga non ha una pagina
        // da aprire. Il nome si scrive qui, toccandolo, e il resto dalla
        // scheda che si apre tenendolo premuto.
        if (!LocalSimpleDatabase.current) {
            Text(
                text = DbStrings.open,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onOpen() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** La cella nella tabella: il contenuto, dentro un riquadro della misura della colonna. */
@Composable
private fun CellEditor(
    row: DatabaseRowEntity,
    column: DatabaseColumnEntity,
    value: String,
    onValueChange: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit
) {
    Box(
        modifier = Modifier
            .width(COLUMN_WIDTH)
            .height(ROW_HEIGHT)
            // **La cella si mangia i tocchi che non colpiscono niente.**
            // Senza, il tocco sullo spazio vuoto accanto a una casella
            // da spuntare attraversava la tabella e arrivava alla
            // pagina che le sta dietro, che lo intende come "scrivi
            // qui": il cursore finiva sotto al database e si apriva la
            // tastiera. I figli vengono serviti per primi, quindi la
            // casella continua a rispondere normalmente.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = if (column.centerContent) {
            Alignment.Center
        } else {
            Alignment.CenterStart
        }
    ) {
        // Il margine laterale lo mette il contenuto, non questo
        // riquadro: per la data deve stare **dentro** la zona toccabile
        // (vedi CellContent), altrimenti i dodici punti ai lati non
        // aprono il calendario.
        CellContent(
            row = row,
            column = column,
            value = value,
            cellPadding = 12.dp,
            onValueChange = onValueChange,
            onAddOption = onAddOption,
            onRenameOption = onRenameOption,
            onSetOptionColor = onSetOptionColor,
            onDeleteOption = onDeleteOption
        )
    }
}

/**
 * Il contenuto di una cella, che cambia in base al tipo della colonna.
 * Non impone una misura: lo usano sia la tabella (dentro un riquadro
 * della larghezza della colonna) sia la finestra delle azioni di una
 * riga, dove le proprietà stanno una sotto l'altra a tutta larghezza.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CellContent(
    row: DatabaseRowEntity,
    column: DatabaseColumnEntity,
    value: String,
    onValueChange: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    /** Il margine laterale dentro la cella: nella tabella c'è, nella finestra delle proprietà no. */
    cellPadding: Dp = 0.dp
) {
    val cellKey = "${row.id}:${column.id}"

    Box(
        // La data se lo mette da sé, dentro la propria zona toccabile.
        modifier = if (column.type == ColumnType.DATE) {
            Modifier
        } else {
            Modifier.padding(horizontal = cellPadding)
        },
        contentAlignment = Alignment.CenterStart
    ) {
        when (column.type) {
            ColumnType.CHECKBOX -> {
                Checkbox(
                    checked = value == "true",
                    onCheckedChange = { onValueChange(it.toString()) },
                    modifier = Modifier.size(24.dp)
                )
            }

            ColumnType.SELECT -> TagCell(
                column = column,
                value = value,
                allowMultiple = false,
                onValueChange = onValueChange,
                onAddOption = onAddOption,
                onRenameOption = onRenameOption,
                onSetOptionColor = onSetOptionColor,
                onDeleteOption = onDeleteOption
            )

            ColumnType.MULTI_SELECT -> TagCell(
                column = column,
                value = value,
                allowMultiple = true,
                onValueChange = onValueChange,
                onAddOption = onAddOption,
                onRenameOption = onRenameOption,
                onSetOptionColor = onSetOptionColor,
                onDeleteOption = onDeleteOption
            )

            ColumnType.DATE -> DateCell(
                value = value,
                onValueChange = onValueChange,
                contentPadding = cellPadding
            )

            ColumnType.CREATED_TIME, ColumnType.LAST_EDITED_TIME -> {
                // Calcolate dalla riga stessa, non modificabili a mano.
                // Una riga mai toccata dopo la creazione, o creata prima
                // che esistesse questa colonna, mostra la data di
                // creazione invece di restare vuota.
                val instant = if (column.type == ColumnType.CREATED_TIME) {
                    row.createdAt
                } else {
                    row.updatedAt ?: row.createdAt
                }
                Text(
                    text = Instant.ofEpochMilli(instant)
                        .atZone(AppSettings.zoneId)
                        .format(DATE_TIME_FORMAT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            else -> {
                // TEXT, NUMBER, URL, EMAIL, PHONE: un campo libero, che
                // cambia solo il tipo di tastiera proposta.
                var localValue by remember(cellKey) { mutableStateOf(value) }
                var editing by remember(cellKey) { mutableStateOf(false) }
                BasicTextField(
                    value = localValue,
                    onValueChange = { newValue ->
                        localValue = newValue
                        onValueChange(newValue)
                    },
                    singleLine = true,
                    // Un numero si legge nel formato delle impostazioni
                    // (1.000.000 o 1,000,000), ma **si scrive grezzo**:
                    // mentre la cella ha il cursore il formato sparisce,
                    // altrimenti i separatori comparirebbero e si
                    // sposterebbero sotto le dita a ogni cifra.
                    visualTransformation = if (column.type == ColumnType.NUMBER && !editing) {
                        NumberDisplayTransformation
                    } else {
                        VisualTransformation.None
                    },
                    // Database bloccato: la cella si legge, non si
                    // riscrive.
                    readOnly = LocalDatabaseLocked.current,
                    // Anche qui l'invio non avrebbe niente da fare: una
                    // cella sta su una riga sola.
                    keyboardOptions = KeyboardOptions(
                        keyboardType = column.type.keyboardType(),
                        capitalization = column.type.capitalization(),
                        imeAction = ImeAction.Done
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { editing = it.isFocused }
                )
            }
        }
    }
}

/**
 * Mostra un numero nel formato delle impostazioni senza cambiare il valore
 * salvato. Si usa solo quando la cella non ha il cursore, quindi le
 * posizioni non contano: tutto si ancora in fondo.
 */
private object NumberDisplayTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = Formats.number(text.text)
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = shown.length
                override fun transformedToOriginal(offset: Int): Int = text.length
            }
        )
    }
}

/**
 * Una cella a tag: mostra le etichette scelte come pastiglie colorate e,
 * toccandola, apre la finestra per sceglierne o crearne.
 *
 * Una cella vuota resta vuota, senza trattini o altri segnaposto: lo
 * spazio libero si capisce da solo, e un simbolo lì fa credere che ci
 * sia già scritto qualcosa.
 *
 * `allowMultiple` distingue Select da Multi-select: con la selezione
 * singola scegliere chiude la finestra, con quella multipla resta
 * aperta perché di solito se ne spuntano più di uno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagCell(
    column: DatabaseColumnEntity,
    value: String,
    allowMultiple: Boolean,
    onValueChange: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val options = parseSelectOptionsFull(column.optionsJson)
    val columnId = column.id
    val selected = if (allowMultiple) {
        value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
    } else {
        listOfNotNull(value.takeIf { it.isNotBlank() })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        selected.forEach { label ->
            TagChip(label = label, color = options.find { it.label == label }?.color)
            Spacer(modifier = Modifier.size(4.dp))
        }
    }

    if (showSheet) {
        TagPickerSheet(
            options = options,
            selected = selected,
            allowMultiple = allowMultiple,
            onDismiss = { showSheet = false },
            onToggle = { label ->
                val updated = when {
                    !allowMultiple -> if (label in selected) emptyList() else listOf(label)
                    label in selected -> selected - label
                    else -> selected + label
                }
                onValueChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                if (!allowMultiple) showSheet = false
            },
            onCreate = { label ->
                onAddOption(label)
                val updated = if (allowMultiple) selected + label else listOf(label)
                onValueChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                if (!allowMultiple) showSheet = false
            },
            onRenameOption = { old, new -> onRenameOption(columnId, old, new) },
            onSetOptionColor = { label, hex -> onSetOptionColor(columnId, label, hex) },
            onDeleteOption = { label -> onDeleteOption(columnId, label) }
        )
    }
}

/**
 * La schermata per modificare un tag: nome, eliminazione e colore.
 *
 * Il nome si applica mentre si scrive, non a un pulsante di conferma:
 * rinominare tocca anche le righe che usano quel tag, e vederlo
 * cambiare sotto le dita è più chiaro di un salvataggio invisibile.
 */
@Composable
private fun TagOptionEditor(
    option: SelectOption,
    onDone: () -> Unit,
    onRename: (String) -> Unit,
    onSetColor: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(option.label) { mutableStateOf(option.label) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TagChip(label = option.label, color = option.color)
            }
            TextButton(onClick = onDone) { Text(Strings.done) }
        }
        Spacer(modifier = Modifier.size(12.dp))

        SheetTextField(
            value = name,
            onValueChange = { newName ->
                name = newName.replace("\n", "")
                onRename(name)
            },
            placeholder = DbStrings.tagName,
            leading = Icons.Filled.Notes
        )

        Spacer(modifier = Modifier.size(12.dp))
        SheetGroup {
            SheetAction(Icons.Filled.Delete, Strings.delete, isDestructive = true) { onDelete() }
        }

        Spacer(modifier = Modifier.size(20.dp))
        Text(
            DbStrings.colors,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(8.dp))
        SheetGroup {
            TAG_COLORS.forEachIndexed { index, paletteColor ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetColor(paletteColor.hex) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(tagColor(paletteColor.hex), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.size(14.dp))
                    Text(DbStrings.tagColorName(paletteColor.name), modifier = Modifier.weight(1f))
                    if (paletteColor.hex.equals(option.color, ignoreCase = true)) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = DbStrings.selected,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(24.dp))
    }
}

/** La pastiglia colorata di un tag. */
@Composable
private fun TagChip(label: String, color: String?) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = NotionWhite,
        maxLines = 1,
        modifier = Modifier
            .background(tagColor(color), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * La finestra per scegliere un tag: in cima il campo per crearne uno
 * nuovo, sotto quelli che esistono già.
 *
 * Il campo serve anche da ricerca, ma l'etichetta dice "Create a tag"
 * perché è quello che si fa più spesso quando la si apre; filtrare
 * l'elenco è il vantaggio che si ottiene scrivendo, non lo scopo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPickerSheet(
    options: List<SelectOption>,
    selected: List<String>,
    allowMultiple: Boolean,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRenameOption: (String, String) -> Unit,
    onSetOptionColor: (String, String) -> Unit,
    onDeleteOption: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    // Quale tag si sta modificando. La schermata di modifica vive
    // dentro questa stessa finestra invece che in una sopra l'altra:
    // due finestre sovrapposte si contendono la chiusura col gesto di
    // trascinamento.
    var editing by remember { mutableStateOf<String?>(null) }
    val trimmed = query.trim()
    val matching = options.filter { it.label.contains(trimmed, ignoreCase = true) }
    val exactExists = options.any { it.label.equals(trimmed, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        val optionBeingEdited = options.find { it.label == editing }
        if (optionBeingEdited != null) {
            TagOptionEditor(
                option = optionBeingEdited,
                onDone = { editing = null },
                onRename = { newLabel ->
                    onRenameOption(optionBeingEdited.label, newLabel)
                    editing = newLabel
                },
                onSetColor = { hex -> onSetOptionColor(optionBeingEdited.label, hex) },
                onDelete = {
                    onDeleteOption(optionBeingEdited.label)
                    editing = null
                }
            )
            return@ModalBottomSheet
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = DbStrings.typeName(if (allowMultiple) ColumnType.MULTI_SELECT else ColumnType.SELECT),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(16.dp))

            SheetTextField(
                value = query,
                onValueChange = { query = it.replace("\n", "") },
                placeholder = DbStrings.createATag,
                leading = Icons.Filled.Add
            )

            if (trimmed.isNotEmpty() && !exactExists) {
                Spacer(modifier = Modifier.size(12.dp))
                SheetGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = ""; onCreate(trimmed) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            DbStrings.create,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        TagChip(label = trimmed, color = null)
                    }
                }
            }

            Spacer(modifier = Modifier.size(20.dp))
            Text(
                text = DbStrings.selectExistingOption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(8.dp))

            if (matching.isEmpty()) {
                Text(
                    text = if (options.isEmpty()) {
                        DbStrings.noTagsYet
                    } else {
                        DbStrings.noTagMatches
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                SheetGroup {
                    matching.forEachIndexed { index, option ->
                        if (index > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(option.label) }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagChip(label = option.label, color = option.color)
                            Spacer(modifier = Modifier.weight(1f))
                            if (option.label in selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = DbStrings.selected,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Icon(
                                Icons.Filled.MoreHoriz,
                                contentDescription = DbStrings.editTag,
                                modifier = Modifier
                                    .clickable { editing = option.label }
                                    .padding(4.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * La cella di una data. Permette un intervallo, non una data sola:
 * scegliendo un secondo giorno la pagina dura da lì a lì, e il
 * calendario la mostrerà su tutti i giorni che copre.
 *
 * Scegliere solo il primo giorno e confermare resta valido: è il caso
 * normale di una scadenza.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCell(
    value: String,
    onValueChange: (String) -> Unit,
    contentPadding: Dp = 0.dp
) {
    var showPicker by remember { mutableStateOf(false) }
    val range = parseDateRange(value)

    // La zona toccabile è **tutta la cella**, non il testo.
    //
    // Una cella vuota mostra un trattino largo pochi punti: se il tocco
    // stesse solo lì, ovunque altro cadrebbe sulla pagina sotto il
    // database, che risponde mettendosi a scrivere in fondo. Il margine
    // laterale sta dentro il riquadro, così non lascia fuori nemmeno i
    // bordi.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable { showPicker = true }
            .padding(horizontal = contentPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = range?.label() ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }

    if (showPicker) {
        DateEditorSheet(
            range = range,
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * La finestra della data, sul modello di quella di Notion: il
 * calendario, e sotto gli interruttori per quello che la data può
 * avere in più.
 *
 * L'ora è **facoltativa** e resta spenta finché non la si accende.
 * Accesa, vale sia per l'inizio che per la fine; spenta, la data
 * continua a valere il giorno intero — che è quello che serve alla
 * grandissima parte delle pagine.
 *
 * Quello che Notion ha e qui non c'è — formato della data, fuso
 * orario, promemoria — non è stato dimenticato: il formato e il fuso
 * sono preferenze che qui seguono il telefono, e il promemoria
 * vorrebbe le notifiche, che sono un lavoro a sé.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateEditorSheet(
    range: DateRange?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = range?.start?.toLocalDate()?.toPickerMillis(),
        initialSelectedEndDateMillis = range?.end?.toLocalDate()?.toPickerMillis()
    )
    var includeTime by remember { mutableStateOf(range?.hasTime == true) }
    var startTime by remember {
        mutableStateOf(
            range?.takeIf { it.hasTime }?.start?.toLocalDateTime()?.toLocalTime()
                ?: LocalTime.of(9, 0)
        )
    }
    var endTime by remember {
        mutableStateOf(
            range?.takeIf { it.hasTime }?.end?.toLocalDateTime()?.toLocalTime()
                ?: LocalTime.of(17, 0)
        )
    }
    // Quale ora si sta scegliendo: null = nessuna finestra aperta.
    var editingTime by remember { mutableStateOf<Boolean?>(null) }

    // La ripetizione: la regola scelta, l'elenco dei modi rapidi e la
    // schermata per costruirne una su misura.
    var recurrence by remember { mutableStateOf(range?.recurrence) }
    var showRepeatPresets by remember { mutableStateOf(false) }
    var showCustomRecurrence by remember { mutableStateOf(false) }
    // Le ripetizioni si descrivono a partire dal giorno scelto qui
    // sopra ("ogni lunedì" se si è scelto un lunedì): se non è stato
    // scelto ancora niente si parla di oggi.
    val selectedStartDate = pickerState.selectedStartDateMillis?.pickerMillisToLocalDate()
        ?: LocalDate.now()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        // I pulsanti stanno in testa e non in fondo: sotto al
        // calendario e agli interruttori finirebbero fuori schermo, e
        // si sceglierebbe una data senza poterla confermare. Stessa
        // lezione della finestra delle proprietà.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DbStrings.typeName(ColumnType.DATE),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            TextButton(onClick = {
                val startDate = pickerState.selectedStartDateMillis?.pickerMillisToLocalDate()
                if (startDate == null) {
                    // Confermare senza aver scelto niente svuota la cella.
                    onConfirm("")
                    return@TextButton
                }
                val endDate = pickerState.selectedEndDateMillis?.pickerMillisToLocalDate()
                val startAt = LocalDateTime.of(
                    startDate,
                    if (includeTime) startTime else LocalTime.MIDNIGHT
                )
                val endAt = endDate?.let {
                    LocalDateTime.of(it, if (includeTime) endTime else LocalTime.MIDNIGHT)
                }
                onConfirm(
                    encodeDateRange(
                        start = startAt.toEpochMillis(),
                        end = endAt?.toEpochMillis(),
                        hasTime = includeTime,
                        recurrence = recurrence
                    )
                )
            }) { Text(Strings.done) }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            DateRangePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                modifier = Modifier.heightIn(max = 420.dp)
            )

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { includeTime = !includeTime }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DbStrings.includeTime,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = includeTime, onCheckedChange = { includeTime = it })
            }

            if (includeTime) {
                HorizontalDivider()
                DateEditorTimeRow(
                    label = DbStrings.startTime,
                    time = startTime,
                    onClick = { editingTime = true }
                )
                if (pickerState.selectedEndDateMillis != null) {
                    HorizontalDivider()
                    DateEditorTimeRow(
                        label = DbStrings.endTime,
                        time = endTime,
                        onClick = { editingTime = false }
                    )
                }
            }

            // La ripetizione sta sotto all'ora perché è la stessa
            // famiglia di domande — *quando* capita — e perché
            // riguarda la data di partenza scelta qui sopra.
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRepeatPresets = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = DbStrings.repeat,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = recurrenceSummary(recurrence, selectedStartDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (recurrence == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

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

    val editing = editingTime
    if (editing != null) {
        TimePickerDialog(
            initial = if (editing) startTime else endTime,
            onPick = { picked -> if (editing) startTime = picked else endTime = picked },
            onDismiss = { editingTime = null }
        )
    }

    if (showRepeatPresets) {
        RepeatPresetDialog(
            start = selectedStartDate,
            current = recurrence,
            onDismiss = { showRepeatPresets = false },
            onPick = { picked ->
                recurrence = picked
                showRepeatPresets = false
            },
            onCustom = {
                showRepeatPresets = false
                showCustomRecurrence = true
            }
        )
    }

    if (showCustomRecurrence) {
        CustomRecurrenceDialog(
            start = selectedStartDate,
            initial = recurrence,
            onDismiss = { showCustomRecurrence = false },
            onDone = { rule ->
                recurrence = rule
                showCustomRecurrence = false
            }
        )
    }
}

/**
 * I modi rapidi di ripetere qualcosa, costruiti sulla data scelta:
 * "ogni lunedì" se si è scelto un lunedì, "il terzo martedì del mese"
 * se si è scelto il terzo martedì. Sono le stesse scorciatoie di
 * Google Calendar, e coprono quasi tutti i casi senza aprire niente.
 */
@Composable
private fun RepeatPresetDialog(
    start: LocalDate,
    current: Recurrence?,
    onDismiss: () -> Unit,
    onPick: (Recurrence?) -> Unit,
    onCustom: () -> Unit
) {
    val weekdaysOnly = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
    val presets: List<Pair<String, Recurrence?>> = listOf(
        DbStrings.doesNotRepeat to null,
        DbStrings.daily to Recurrence(RecurrenceFreq.DAILY),
        DbStrings.weeklyOn(dayName(start.dayOfWeek)) to
            Recurrence(RecurrenceFreq.WEEKLY, weekdays = setOf(start.dayOfWeek)),
        DbStrings.monthlyOn(DbStrings.nthWeekday(start, dayName(start.dayOfWeek))) to
            Recurrence(RecurrenceFreq.MONTHLY, monthlyMode = MonthlyMode.NTH_WEEKDAY),
        DbStrings.annuallyOn(start.format(DATE_FORMAT)) to Recurrence(RecurrenceFreq.YEARLY),
        DbStrings.everyWeekday to
            Recurrence(RecurrenceFreq.WEEKLY, weekdays = weekdaysOnly)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
        title = { Text(DbStrings.repeat) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                presets.forEach { (label, rule) ->
                    val selected = recurrenceSummary(rule, start) ==
                        recurrenceSummary(current, start)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(rule) }
                            .padding(vertical = 12.dp)
                    )
                }
                HorizontalDivider()
                Text(
                    text = DbStrings.custom,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustom() }
                        .padding(vertical = 12.dp)
                )
            }
        }
    )
}

/**
 * La schermata per costruire una ripetizione qualsiasi: ogni quanto,
 * in quali giorni, e fino a quando. Ricalca quella di Google
 * Calendar, che è la più chiara in circolazione per una cosa che di
 * suo si presta a diventare un modulo fiscale.
 *
 * Le parti che non servono non si vedono: i sette pallini dei giorni
 * solo per le settimane, la scelta "giorno del mese / ennesimo
 * lunedì" solo per i mesi.
 */
@Composable
private fun CustomRecurrenceDialog(
    start: LocalDate,
    initial: Recurrence?,
    onDismiss: () -> Unit,
    onDone: (Recurrence) -> Unit
) {
    var freq by remember { mutableStateOf(initial?.freq ?: RecurrenceFreq.WEEKLY) }
    var interval by remember { mutableStateOf((initial?.interval ?: 1).toString()) }
    var weekdays by remember {
        mutableStateOf(initial?.weekdays?.ifEmpty { setOf(start.dayOfWeek) } ?: setOf(start.dayOfWeek))
    }
    var monthlyMode by remember {
        mutableStateOf(initial?.monthlyMode ?: MonthlyMode.DAY_OF_MONTH)
    }
    var endKind by remember {
        mutableStateOf(
            when (initial?.end) {
                is RecurrenceEnd.OnDate -> 1
                is RecurrenceEnd.AfterCount -> 2
                else -> 0
            }
        )
    }
    var endDate by remember {
        mutableStateOf(
            (initial?.end as? RecurrenceEnd.OnDate)?.let { LocalDate.ofEpochDay(it.epochDay) }
                ?: start.plusMonths(3)
        )
    }
    var endCount by remember {
        mutableStateOf(((initial?.end as? RecurrenceEnd.AfterCount)?.count ?: 13).toString())
    }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var freqMenuOpen by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(DbStrings.customRecurrence) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
        confirmButton = {
            TextButton(onClick = {
                onDone(
                    Recurrence(
                        freq = freq,
                        interval = interval.toIntOrNull()?.coerceIn(1, 999) ?: 1,
                        weekdays = if (freq == RecurrenceFreq.WEEKLY) weekdays else emptySet(),
                        monthlyMode = monthlyMode,
                        end = when (endKind) {
                            1 -> RecurrenceEnd.OnDate(endDate.toEpochDay())
                            2 -> RecurrenceEnd.AfterCount(
                                endCount.toIntOrNull()?.coerceIn(1, 999) ?: 1
                            )
                            else -> RecurrenceEnd.Never
                        }
                    )
                )
            }) { Text(Strings.done) }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DbStrings.repeatEvery, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedTextField(
                        value = interval,
                        onValueChange = { typed ->
                            interval = typed.filter { it.isDigit() }.take(3)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { freqMenuOpen = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DbStrings.unit(freq, plural = (interval.toIntOrNull() ?: 1) > 1),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = DbStrings.changeUnit,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = freqMenuOpen,
                            onDismissRequest = { freqMenuOpen = false }
                        ) {
                            RecurrenceFreq.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(DbStrings.unit(option, plural = false))
                                    },
                                    onClick = {
                                        freq = option
                                        freqMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (freq == RecurrenceFreq.WEEKLY) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(DbStrings.repeatOn, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.size(8.dp))
                    Row {
                        // L'ordine dei giorni segue le convenzioni
                        // locali, come il calendario: lunedì in Italia.
                        val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
                        (0..6).forEach { offset ->
                            val day = first.plus(offset.toLong())
                            val on = day in weekdays
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(
                                        if (on) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable {
                                        // Almeno un giorno deve restare
                                        // acceso: "ogni settimana in
                                        // nessun giorno" non vuol dire
                                        // niente e non genererebbe mai.
                                        weekdays = if (on && weekdays.size > 1) {
                                            weekdays - day
                                        } else {
                                            weekdays + day
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayInitial(day),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (on) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                if (freq == RecurrenceFreq.MONTHLY) {
                    Spacer(modifier = Modifier.size(12.dp))
                    RecurrenceChoiceRow(
                        selected = monthlyMode == MonthlyMode.DAY_OF_MONTH,
                        label = DbStrings.onDayOfMonth(start.dayOfMonth),
                        onSelect = { monthlyMode = MonthlyMode.DAY_OF_MONTH }
                    )
                    RecurrenceChoiceRow(
                        selected = monthlyMode == MonthlyMode.NTH_WEEKDAY,
                        label = DbStrings.nthWeekday(start, dayName(start.dayOfWeek), capitalized = true),
                        onSelect = { monthlyMode = MonthlyMode.NTH_WEEKDAY }
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))
                Text(DbStrings.ends, style = MaterialTheme.typography.bodyMedium)
                RecurrenceChoiceRow(
                    selected = endKind == 0,
                    label = DbStrings.never,
                    onSelect = { endKind = 0 }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = endKind == 1, onClick = { endKind = 1 })
                    Text(DbStrings.on, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = endDate.format(DATE_FORMAT),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (endKind == 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clickable {
                                endKind = 1
                                showEndDatePicker = true
                            }
                            .padding(vertical = 8.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = endKind == 2, onClick = { endKind = 2 })
                    Text(DbStrings.after, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedTextField(
                        value = endCount,
                        onValueChange = { typed ->
                            endCount = typed.filter { it.isDigit() }.take(3)
                            endKind = 2
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(DbStrings.times, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    )

    if (showEndDatePicker) {
        DayPickerDialog(
            initial = endDate,
            onPick = { picked ->
                endDate = picked
                endKind = 1
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/** Una riga con il pallino di scelta, usata dentro la ripetizione. */
@Composable
private fun RecurrenceChoiceRow(selected: Boolean, label: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Il calendarietto per scegliere un giorno solo: serve a "finisce
 * il..." delle ripetizioni e ai due estremi del filtro per data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toPickerMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.pickerMillisToLocalDate()?.let(onPick)
                onDismiss()
            }) { Text(DbStrings.ok) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    ) {
        DatePicker(state = state, title = null, showModeToggle = false)
    }
}

/**
 * La regola di ripetizione detta a parole, come la scriverebbe
 * qualcuno: "Ogni tre settimane il lunedì e il mercoledì, 13 volte".
 *
 * Serve nella riga "Repeat" della finestra della data: una regola
 * riassunta in una riga si controlla a colpo d'occhio, mentre per
 * capire "FREQ=WEEKLY;INT=3" bisogna riaprire la schermata.
 */
private fun recurrenceSummary(rule: Recurrence?, start: LocalDate): String {
    if (rule == null) return DbStrings.doesNotRepeat
    val every = rule.interval
    val head = when (rule.freq) {
        RecurrenceFreq.DAILY ->
            if (every == 1) DbStrings.everyDay else DbStrings.everyNDays(every)

        RecurrenceFreq.WEEKLY -> {
            val days = rule.weekdays.ifEmpty { setOf(start.dayOfWeek) }
            val isWeekdays = days == setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
            val names = if (isWeekdays) {
                DbStrings.everyWeekdayShort
            } else {
                days.sortedBy { it.value }.joinToString(", ") { dayName(it) }
            }
            if (every == 1) DbStrings.weeklyOn(names) else DbStrings.everyNWeeksOn(every, names)
        }

        RecurrenceFreq.MONTHLY -> {
            // "primo", "secondo"... oppure "ultimo" quando dopo non ce n'è
            // un altro nello stesso mese: chi dice "l'ultima domenica"
            // intende sempre l'ultima, non la quarta quando ce ne sono
            // cinque. Vedi `DbStrings.nthWeekday`.
            val what = when (rule.monthlyMode) {
                MonthlyMode.DAY_OF_MONTH -> DbStrings.dayOfMonth(start.dayOfMonth)
                MonthlyMode.NTH_WEEKDAY -> DbStrings.nthWeekday(start, dayName(start.dayOfWeek))
            }
            if (every == 1) DbStrings.monthlyOn(what) else DbStrings.everyNMonthsOn(every, what)
        }

        RecurrenceFreq.YEARLY -> {
            val what = start.format(DATE_FORMAT)
            if (every == 1) DbStrings.annuallyOn(what) else DbStrings.everyNYearsOn(every, what)
        }
    }
    return when (val end = rule.end) {
        RecurrenceEnd.Never -> head
        is RecurrenceEnd.OnDate ->
            head + DbStrings.until(LocalDate.ofEpochDay(end.epochDay).format(DATE_FORMAT))
        is RecurrenceEnd.AfterCount ->
            head + DbStrings.nTimes(end.count)
    }
}

private fun dayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, displayLocale)

private fun dayInitial(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.NARROW, displayLocale)

/** Una riga "Start time 09:00" dentro la finestra della data. */
@Composable
private fun DateEditorTimeRow(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
            text = time.format(TIME_FORMAT),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Il campo per scegliere un'ora.
 *
 * È `TimeInput`, le due caselle ore/minuti, e non il quadrante
 * dell'orologio: il quadrante vuole più di trecento punti di
 * larghezza e dentro una finestra di dialogo, su un telefono, resta
 * schiacciato. Metterlo in una finestra a scomparsa non si può — ce
 * n'è già una aperta sotto, e due si contendono il gesto di chiusura
 * (stessa ragione per cui l'editor dei tag vive dentro la sua).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        // Ventiquattro ore come il resto dell'app, che scrive le date
        // con "HH:mm" dappertutto.
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(DbStrings.ok) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeInput(state = state)
            }
        }
    )
}

/**
 * Se il database aperto è bloccato nel contenuto (la voce "Lock page"):
 * righe e celle si leggono ma non si scrivono.
 *
 * Passa da qui e non da un parametro perché i campi di scrittura di una
 * tabella stanno in fondo a una catena lunga di composable — riga,
 * cella, editor — e infilare lo stesso `Boolean` in tutte le firme è un
 * modo sicuro di dimenticarselo in una.
 */
private val LocalDatabaseLocked = staticCompositionLocalOf { false }

/**
 * Se il database aperto è **semplice** (`PageEntity.isSimpleDatabase`):
 * le sue righe sono solo testo e non diventano mai pagine. Passa da qui
 * per la stessa ragione di `LocalDatabaseLocked` — lo guardano la cella
 * del nome (niente OPEN) e i pulsanti per aggiungere ("Nuova riga" invece
 * di "Nuova pagina"), in fondo a catene che non hanno altro motivo di
 * saperlo.
 */
private val LocalSimpleDatabase = staticCompositionLocalOf { false }

/**
 * Le icone delle pagine delle righe del database aperto: riga → file
 * dell'immagine. Passa da qui per la stessa ragione di `LocalDatabaseLocked`:
 * il titolo di una riga si disegna in sette viste diverse, in fondo a
 * catene di composable che non hanno altro motivo per saperlo.
 */
private val LocalRowIcons = compositionLocalOf<Map<String, String>> { emptyMap() }

/**
 * L'icona della pagina di una riga, prima del suo titolo — solo se ne ha
 * una: una riga senza icona parte dal margine, senza un segnaposto che
 * sembrerebbe un'icona. Va usata dentro una `Row`: disegna l'immagine e lo
 * stacco dal testo.
 */
@Composable
private fun RowIconBadge(rowId: String, size: Dp) {
    val fileName = LocalRowIcons.current[rowId] ?: return
    val context = LocalContext.current
    val store = remember(context) { PageImageStore(context) }
    PageImage(
        fileName = fileName,
        store = store,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
    )
    Spacer(modifier = Modifier.size(6.dp))
}

/** Le schermate dentro le finestre a scomparsa, come su Notion. */
private enum class SettingsPage {
    ROOT,
    EDIT_PROPERTIES,
    LAYOUT,
    GROUP_BY,
    /** Su quale proprietà raggruppa la **tabella**: `GROUP_BY` è della bacheca. */
    TABLE_GROUP,
    /** Quali colonne si vedono nella tabella e quali no. */
    PROPERTY_VISIBILITY,
    DATE_PROPERTY,
    /** Cosa mostra la parte alta delle schede della galleria. */
    CARD_PREVIEW,
    /** Quanto sono grandi le schede della galleria. */
    CARD_SIZE
}

/** Le schermate dentro la finestra delle azioni di una riga. */
private enum class RowActionsPage { ROOT, PROPERTIES }

/**
 * Le azioni su una singola riga, tenendo premuto sul suo nome.
 *
 * C'è l'icona della pagina, come l'"Edit icon" di Notion. Su Notion
 * questa finestra ha anche Add to Favorites, Comment, Copy link,
 * Duplicate e Move to: mancano perché sarebbero etichette senza niente
 * dietro — i commenti non esistono nell'app, un collegamento da copiare
 * non esiste senza indirizzi, e spostare una riga in un altro database
 * ne perderebbe i valori, visto che le colonne sono diverse. "Delete"
 * cancella la riga per davvero, senza passare dal cestino.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowActionsSheet(
    row: DatabaseRowEntity,
    columns: List<DatabaseColumnEntity>,
    cellValues: Map<Pair<String, String>, String>,
    onDismiss: () -> Unit,
    onSetCellValue: (DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    /** Se la pagina della riga ha già un'icona: la voce dice "Change" invece di "None". */
    hasIcon: Boolean,
    /** Spenta con la pagina bloccata o nel cestino: l'icona fa parte del contenuto. */
    iconEditable: Boolean,
    /**
     * Database semplice: la finestra diventa **la scheda della riga** —
     * il nome da scrivere in cima e subito sotto tutte le proprietà, senza
     * la voce dell'icona (una riga senza pagina non ha dove metterla).
     * È quello che si apre toccando una riga, in qualunque vista.
     */
    simple: Boolean = false,
    onTitleChange: (String) -> Unit = {},
    onEditIcon: () -> Unit,
    onDelete: () -> Unit
) {
    var page by remember(row.id) { mutableStateOf(RowActionsPage.ROOT) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (page != RowActionsPage.ROOT) {
                    IconButton(onClick = { page = RowActionsPage.ROOT }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = Strings.back)
                    }
                }
                Text(
                    text = when {
                        page != RowActionsPage.ROOT -> DbStrings.editProperty
                        simple -> DbStrings.row
                        else -> DbStrings.actions
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(12.dp))

            when (page) {
                RowActionsPage.ROOT -> if (simple) {
                    SimpleRowNameField(
                        row = row,
                        editable = iconEditable,
                        onTitleChange = onTitleChange
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    RowPropertiesList(
                        row = row,
                        columns = columns,
                        cellValues = cellValues,
                        onSetCellValue = onSetCellValue,
                        onAddOption = onAddOption,
                        onRenameOption = onRenameOption,
                        onSetOptionColor = onSetOptionColor,
                        onDeleteOption = onDeleteOption
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    SheetGroup {
                        SheetAction(Icons.Filled.Delete, Strings.delete, isDestructive = true) {
                            onDelete()
                        }
                    }
                    Spacer(modifier = Modifier.size(20.dp))
                    Text(
                        text = DbStrings.created(Formats.dateTime(row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DbStrings.lastEdited(Formats.dateTime(row.updatedAt ?: row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    SheetGroup {
                        // Come l'"Edit icon" delle azioni di una riga su
                        // Notion: l'icona della pagina senza doverla aprire.
                        SettingsRow(
                            icon = Icons.Filled.Image,
                            label = EditorStrings.icon,
                            value = if (hasIcon) DbStrings.change else DbStrings.none,
                            enabled = iconEditable,
                            onClick = onEditIcon
                        )
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Filled.FormatListBulleted,
                            label = DbStrings.editProperty,
                            value = columns.size.toString(),
                            onClick = { page = RowActionsPage.PROPERTIES }
                        )
                        HorizontalDivider()
                        SheetAction(Icons.Filled.Delete, Strings.delete, isDestructive = true) {
                            onDelete()
                        }
                    }

                    Spacer(modifier = Modifier.size(20.dp))
                    Text(
                        text = DbStrings.created(Formats.dateTime(row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DbStrings.lastEdited(Formats.dateTime(row.updatedAt ?: row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RowActionsPage.PROPERTIES -> RowPropertiesList(
                    row = row,
                    columns = columns,
                    cellValues = cellValues,
                    onSetCellValue = onSetCellValue,
                    onAddOption = onAddOption,
                    onRenameOption = onRenameOption,
                    onSetOptionColor = onSetOptionColor,
                    onDeleteOption = onDeleteOption
                )
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Le proprietà di una riga, una per riga, ciascuna modificabile sul posto
 * come nella sua cella. Serve in due posti: "Edit property" delle azioni
 * di una riga, e la scheda di una riga di un database semplice.
 */
@Composable
private fun RowPropertiesList(
    row: DatabaseRowEntity,
    columns: List<DatabaseColumnEntity>,
    cellValues: Map<Pair<String, String>, String>,
    onSetCellValue: (DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit
) {
    Column {
    if (columns.isEmpty()) {
        Text(
            DbStrings.noPropertiesYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
    columns.forEach { column ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                column.type.icon(),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = DbStrings.columnName(column.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(110.dp),
                maxLines = 1
            )
            Spacer(modifier = Modifier.size(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                CellContent(
                    row = row,
                    column = column,
                    value = cellValues[row.id to column.id] ?: "",
                    onValueChange = { onSetCellValue(column, it) },
                    onAddOption = { onAddOption(column, it) },
                    onRenameOption = onRenameOption,
                    onSetOptionColor = onSetOptionColor,
                    onDeleteOption = onDeleteOption
                )
            }
        }
    }
    }
}

/**
 * Il nome di una riga di un database semplice, in cima alla sua scheda.
 *
 * Nelle altre viste il nome non si scrive sul posto — in un database
 * normale si apre la pagina e lo si scrive lì — e una riga senza pagina
 * ha bisogno di un altro posto: questo. Mentre si scrive comanda il testo
 * locale, non quello che torna dal database, per la stessa ragione del
 * titolo del database: il giro di ritorno arriva dopo il tasto
 * successivo, e riscrivendo il campo si mangerebbe il carattere.
 */
@Composable
private fun SimpleRowNameField(
    row: DatabaseRowEntity,
    editable: Boolean,
    onTitleChange: (String) -> Unit
) {
    var name by remember(row.id) { mutableStateOf(row.title) }
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = name,
        onValueChange = { typed ->
            // Niente a capo nel nome, come nella cella della tabella:
            // l'Invio chiude, e un a capo incollato si toglie qui.
            val singleLine = typed.replace("\n", "")
            name = singleLine
            onTitleChange(singleLine)
        },
        label = { Text(DbStrings.name) },
        placeholder = { Text(Strings.untitled) },
        singleLine = true,
        readOnly = !editable,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Le impostazioni del database, nella finestra che scorre dal basso.
 *
 * Su Notion qui ci sono una dozzina di voci (Layout, Filter, Sort,
 * Group, Automations...). Ci sono solo quelle che funzionano davvero:
 * le altre arrivano man mano che vengono costruite, perché una voce che
 * non fa niente è peggio di una voce assente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    /** Se il database ha già un'icona: la voce dice "Change" invece di DbStrings.none. */
    hasIcon: Boolean = false,
    /** Null con la pagina bloccata: l'icona fa parte del contenuto. */
    onEditIcon: (() -> Unit)? = null,
    columns: List<DatabaseColumnEntity>,
    layout: DatabaseLayout,
    groupColumn: DatabaseColumnEntity?,
    dateColumn: DatabaseColumnEntity?,
    /** Su cosa raggruppa la tabella, e se i gruppi vuoti si nascondono. */
    tableGroupColumn: DatabaseColumnEntity? = null,
    hideEmptyGroups: Boolean = true,
    onSetTableGroupColumn: ((String?) -> Unit)? = null,
    onSetHideEmptyGroups: ((Boolean) -> Unit)? = null,
    /** Nasconde o rimostra una colonna, e tutte insieme. */
    onSetColumnHidden: (String, Boolean) -> Unit,
    onSetAllColumnsHidden: (Boolean) -> Unit,
    /** Il filtro attivo, da riassumere; toccarlo apre la sua finestra. */
    filterColumn: DatabaseColumnEntity? = null,
    filterValue: String = "",
    onOpenFilter: () -> Unit,
    initialPage: SettingsPage,
    onDismiss: () -> Unit,
    onSetLayout: (DatabaseLayout) -> Unit,
    onSetGroupColumn: (DatabaseColumnEntity) -> Unit,
    onSetDateColumn: (DatabaseColumnEntity) -> Unit,
    /** Anteprima e dimensione delle schede: contano solo per la galleria. */
    galleryPreview: GalleryCardPreview = GALLERY_DEFAULT_PREVIEW,
    gallerySize: GalleryCardSize = GALLERY_DEFAULT_SIZE,
    onSetGalleryPreview: (GalleryCardPreview) -> Unit = {},
    onSetGallerySize: (GalleryCardSize) -> Unit = {},
    /**
     * Database semplice: niente "Card preview" nella galleria (copertina
     * e testo sono delle pagine delle righe, che qui non esistono), e in
     * fondo una riga che dice che cos'è.
     */
    simple: Boolean = false,
    onEditProperty: (DatabaseColumnEntity) -> Unit,
    onAddProperty: () -> Unit,
    /** Null quando il database è a schermo intero: lì il titolo c'è sempre. */
    onSetShowTitle: ((Boolean) -> Unit)? = null,
    showTitle: Boolean = true,
    isFavorite: Boolean = false,
    onSetFavorite: ((Boolean) -> Unit)? = null,
    /**
     * Vista bloccata ("Lock view"): le voci che cambiano
     * l'impaginazione si vedono ma non rispondono. Cancellare invece
     * resta possibile — il lucchetto protegge come è messo insieme il
     * database, non la sua esistenza, e un database che non si può
     * buttare finché non si ricorda dove si era girata la chiave
     * sarebbe una trappola.
     */
    viewLocked: Boolean = false,
    /**
     * Null a schermo intero: "Turn into page" riguarda il database
     * **dentro una pagina**, e aperto da solo è già una pagina.
     */
    onTurnIntoPage: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var page by remember(initialPage) { mutableStateOf(initialPage) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (page != SettingsPage.ROOT) {
                    IconButton(onClick = { page = SettingsPage.ROOT }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = Strings.back)
                    }
                }
                Text(
                    text = when (page) {
                        SettingsPage.ROOT -> Strings.settings
                        SettingsPage.EDIT_PROPERTIES -> DbStrings.editProperties
                        SettingsPage.LAYOUT -> DbStrings.layout
                        SettingsPage.GROUP_BY -> DbStrings.groupBy
                        SettingsPage.TABLE_GROUP -> DbStrings.group
                        SettingsPage.PROPERTY_VISIBILITY -> DbStrings.propertyVisibility
                        SettingsPage.DATE_PROPERTY -> DbStrings.dateProperty
                        SettingsPage.CARD_PREVIEW -> DbStrings.cardPreview
                        SettingsPage.CARD_SIZE -> DbStrings.cardSize
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(12.dp))

            when (page) {
                SettingsPage.ROOT -> {
                    SheetGroup {
                        SettingsRow(
                            icon = layout.icon(),
                            label = DbStrings.layout,
                            enabled = !viewLocked,
                            value = layout.displayName(),
                            onClick = { page = SettingsPage.LAYOUT }
                        )
                        // Il filtro non è una schermata dentro le
                        // impostazioni ma **la stessa finestra**
                        // dell'icona nella barra: due copie si
                        // sarebbero separate al primo cambiamento.
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Filled.FilterList,
                            label = DbStrings.filter,
                            enabled = !viewLocked,
                            value = filterSummary(filterColumn, filterValue),
                            onClick = onOpenFilter
                        )
                        // "Group by" riguarda solo la bacheca: nelle
                        // altre viste non avrebbe alcun effetto.
                        if (layout == DatabaseLayout.BOARD) {
                            HorizontalDivider()
                            SettingsRow(
                                icon = Icons.Filled.ViewColumn,
                                label = DbStrings.groupBy,
                            enabled = !viewLocked,
                                value = groupColumn?.name?.let(DbStrings::columnName) ?: DbStrings.none,
                                onClick = { page = SettingsPage.GROUP_BY }
                            )
                        }
                        // Il raggruppamento della tabella: una voce
                        // diversa da "Group by" della bacheca, perché
                        // sono due impostazioni distinte e cambiare
                        // l'una non deve rimescolare l'altra.
                        if (layout == DatabaseLayout.TABLE) {
                            HorizontalDivider()
                            SettingsRow(
                                icon = Icons.Filled.Segment,
                                label = DbStrings.group,
                            enabled = !viewLocked,
                                value = tableGroupColumn?.name?.let(DbStrings::columnName) ?: DbStrings.none,
                                onClick = { page = SettingsPage.TABLE_GROUP }
                            )
                            // L'interruttore compare solo quando i
                            // gruppi ci sono: senza raggruppamento non
                            // avrebbe niente da nascondere.
                            if (tableGroupColumn != null && onSetHideEmptyGroups != null) {
                                HorizontalDivider()
                                SettingsToggleRow(
                                    icon = Icons.Filled.VisibilityOff,
                                    label = DbStrings.hideEmptyGroups,
                                    checked = hideEmptyGroups,
                                    onCheckedChange = onSetHideEmptyGroups
                                )
                            }
                        }
                        // Come "Group by": riguarda solo il calendario.
                        if (layout == DatabaseLayout.CALENDAR) {
                            HorizontalDivider()
                            SettingsRow(
                                icon = Icons.Filled.CalendarToday,
                                label = DbStrings.dateProperty,
                            enabled = !viewLocked,
                                value = dateColumn?.name?.let(DbStrings::columnName) ?: DbStrings.none,
                                onClick = { page = SettingsPage.DATE_PROPERTY }
                            )
                        }
                        // Le due voci della galleria, dove le mette Notion:
                        // subito sotto la vista. Sono impaginazione, quindi
                        // "Lock view" le spegne come le altre.
                        if (layout == DatabaseLayout.GALLERY) {
                            if (!simple) {
                                HorizontalDivider()
                                SettingsRow(
                                    icon = galleryPreview.icon(),
                                    label = DbStrings.cardPreview,
                                    enabled = !viewLocked,
                                    value = DbStrings.galleryPreviewName(galleryPreview),
                                    onClick = { page = SettingsPage.CARD_PREVIEW }
                                )
                            }
                            HorizontalDivider()
                            SettingsRow(
                                icon = gallerySize.icon(),
                                label = DbStrings.cardSize,
                                enabled = !viewLocked,
                                value = DbStrings.gallerySizeName(gallerySize),
                                onClick = { page = SettingsPage.CARD_SIZE }
                            )
                        }
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Filled.FormatListBulleted,
                            label = DbStrings.editProperties,
                            enabled = !viewLocked,
                            value = columns.size.toString(),
                            onClick = { page = SettingsPage.EDIT_PROPERTIES }
                        )
                        // Il numero accanto dice quante colonne sono
                        // sparite: è l'unico posto da cui ritrovarle,
                        // e senza quel numero non si saprebbe nemmeno
                        // che c'è qualcosa da ritrovare.
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Filled.Visibility,
                            label = DbStrings.propertyVisibility,
                            enabled = !viewLocked,
                            value = columns.count { it.hidden }
                                .let { if (it == 0) DbStrings.allShown else DbStrings.nHidden(it) },
                            onClick = { page = SettingsPage.PROPERTY_VISIBILITY }
                        )
                        // **L'icona del database.** Sta qui perché un
                        // database senza icona non ha niente da toccare
                        // accanto al nome: questa voce è il modo di
                        // mettergliene una. Quando c'è, si cambia e si
                        // toglie anche toccandola direttamente.
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Filled.Image,
                            label = EditorStrings.icon,
                            value = if (hasIcon) DbStrings.change else DbStrings.none,
                            onClick = onEditIcon ?: {},
                            enabled = onEditIcon != null
                        )
                        // I preferiti stanno qui e non fra le icone
                        // della barra: questa finestra è l'unico posto
                        // che si raggiunge sia col database dentro una
                        // pagina sia aperto a schermo intero, quindi è
                        // anche l'unico dove l'interruttore non va
                        // messo due volte. Per le pagine normali invece
                        // è una stellina nella barra in alto: lì di
                        // impostazioni non ce n'è nessun'altra.
                        if (onSetFavorite != null) {
                            HorizontalDivider()
                            SettingsToggleRow(
                                icon = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                label = Strings.favorite,
                                checked = isFavorite,
                                onCheckedChange = onSetFavorite
                            )
                        }
                        // Solo dentro una pagina: a schermo intero il
                        // titolo non è negoziabile, e offrire un
                        // interruttore che non fa niente sarebbe
                        // peggio che non offrirlo.
                        if (onSetShowTitle != null) {
                            HorizontalDivider()
                            SettingsToggleRow(
                                icon = Icons.Filled.Title,
                                label = DbStrings.showTitle,
                                checked = showTitle,
                                onCheckedChange = onSetShowTitle
                            )
                        }
                    }

                    // Un database semplice si riconosce solo da quello
                    // che non fa: una riga qui lo dice, così chi tocca
                    // una riga e non vede aprirsi una pagina sa perché.
                    if (simple) {
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = DbStrings.simpleDatabaseNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Cancellare è una cosa che si fa al database
                    // intero, non a una delle sue parti: sta in un
                    // gruppo a sé, staccato dalle impostazioni della
                    // vista. Aprire a schermo intero invece è
                    // diventata un'icona nella barra, perché è una
                    // cosa che si fa spesso e di fretta.
                    //
                    // "Turn into page" sta nello stesso gruppo per la
                    // stessa ragione: cambia **dove vive** il database,
                    // non come è impaginato. Non tocca righe né colonne,
                    // quindi non è rossa come Delete.
                    if (onTurnIntoPage != null || onDelete != null) {
                        Spacer(modifier = Modifier.size(12.dp))
                        SheetGroup {
                            if (onTurnIntoPage != null) {
                                SettingsRow(
                                    icon = Icons.Filled.Description,
                                    label = Strings.turnIntoPage,
                                    value = "",
                                    onClick = onTurnIntoPage
                                )
                            }
                            if (onTurnIntoPage != null && onDelete != null) HorizontalDivider()
                            if (onDelete != null) {
                                SettingsRow(
                                    icon = Icons.Filled.Delete,
                                    label = Strings.delete,
                                    value = "",
                                    onClick = onDelete,
                                    danger = true
                                )
                            }
                        }
                    }
                }

                SettingsPage.DATE_PROPERTY -> {
                    val dateColumns = columns.filter { it.type == ColumnType.DATE }
                    if (dateColumns.isEmpty()) {
                        Text(
                            text = DbStrings.calendarNeedsDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        SheetGroup {
                            dateColumns.forEachIndexed { index, column ->
                                if (index > 0) HorizontalDivider()
                                SettingsRow(
                                    icon = column.type.icon(),
                                    label = DbStrings.columnName(column.name),
                                    value = if (column.id == dateColumn?.id) "✓" else null,
                                    onClick = { onSetDateColumn(column) }
                                )
                            }
                        }
                    }
                }

                // Scelta una voce la finestra resta aperta sulla stessa
                // schermata, come per la proprietà data e "Group by": il
                // segno di spunta si sposta, e la galleria dietro cambia
                // subito, così si vede l'effetto prima di tornare indietro.
                SettingsPage.CARD_PREVIEW -> {
                    SheetGroup {
                        GalleryCardPreview.entries.forEachIndexed { index, option ->
                            if (index > 0) HorizontalDivider()
                            SettingsRow(
                                icon = option.icon(),
                                label = DbStrings.galleryPreviewName(option),
                                value = if (option == galleryPreview) "✓" else null,
                                onClick = { onSetGalleryPreview(option) }
                            )
                        }
                    }
                }

                SettingsPage.CARD_SIZE -> {
                    SheetGroup {
                        GalleryCardSize.entries.forEachIndexed { index, option ->
                            if (index > 0) HorizontalDivider()
                            SettingsRow(
                                icon = option.icon(),
                                label = DbStrings.gallerySizeName(option),
                                value = if (option == gallerySize) "✓" else null,
                                onClick = { onSetGallerySize(option) }
                            )
                        }
                    }
                }

                SettingsPage.TABLE_GROUP -> {
                    // Si può raggruppare per quasi tutto: quello che
                    // conta è che il valore stia in una cella. "Creata
                    // il" e "Modificata il" non ci sono perché non sono
                    // celle ma proprietà della riga, e un gruppo per
                    // istante esatto sarebbe un gruppo per riga.
                    val groupable = columns.filter {
                        it.type != ColumnType.CREATED_TIME &&
                            it.type != ColumnType.LAST_EDITED_TIME
                    }
                    SheetGroup {
                        SettingsRow(
                            icon = Icons.Filled.Block,
                            label = DbStrings.none,
                            value = if (tableGroupColumn == null) "✓" else null,
                            onClick = { onSetTableGroupColumn?.invoke(null) }
                        )
                        groupable.forEach { column ->
                            HorizontalDivider()
                            SettingsRow(
                                icon = column.type.icon(),
                                label = DbStrings.columnName(column.name),
                                value = if (column.id == tableGroupColumn?.id) "✓" else null,
                                onClick = { onSetTableGroupColumn?.invoke(column.id) }
                            )
                        }
                    }
                    if (groupable.isEmpty()) {
                        Text(
                            text = DbStrings.groupingNeedsProperty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                SettingsPage.PROPERTY_VISIBILITY -> {
                    val shown = columns.filterNot { it.hidden }
                    val hidden = columns.filter { it.hidden }

                    if (columns.isEmpty()) {
                        Text(
                            text = DbStrings.noPropertiesYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    // DbStrings.name non compare in nessuna delle due liste: è
                    // il titolo della pagina, non una proprietà, e una
                    // riga senza nome non si saprebbe più aprire.
                    if (shown.isNotEmpty()) {
                        VisibilitySectionHeader(
                            title = DbStrings.shownInTable,
                            action = DbStrings.hideAll,
                            onAction = { onSetAllColumnsHidden(true) }
                        )
                        SheetGroup {
                            shown.forEachIndexed { index, column ->
                                if (index > 0) HorizontalDivider()
                                PropertyVisibilityRow(
                                    column = column,
                                    hidden = false,
                                    onToggle = { onSetColumnHidden(column.id, true) }
                                )
                            }
                        }
                    }

                    if (hidden.isNotEmpty()) {
                        VisibilitySectionHeader(
                            title = DbStrings.hiddenInTable,
                            action = DbStrings.showAll,
                            onAction = { onSetAllColumnsHidden(false) }
                        )
                        SheetGroup {
                            hidden.forEachIndexed { index, column ->
                                if (index > 0) HorizontalDivider()
                                PropertyVisibilityRow(
                                    column = column,
                                    hidden = true,
                                    onToggle = { onSetColumnHidden(column.id, false) }
                                )
                            }
                        }
                    }

                    if (columns.isNotEmpty() && hidden.isEmpty()) {
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = DbStrings.nothingHidden,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsPage.GROUP_BY -> {
                    val selectColumns = columns.filter { it.type == ColumnType.SELECT }
                    if (selectColumns.isEmpty()) {
                        Text(
                            text = DbStrings.boardNeedsSelectSettings,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        SheetGroup {
                            selectColumns.forEachIndexed { index, column ->
                                if (index > 0) HorizontalDivider()
                                SettingsRow(
                                    icon = column.type.icon(),
                                    label = DbStrings.columnName(column.name),
                                    value = if (column.id == groupColumn?.id) "✓" else null,
                                    onClick = { onSetGroupColumn(column) }
                                )
                            }
                        }
                    }
                }

                SettingsPage.LAYOUT -> {
                    // Una griglia di riquadri come su Notion. Ci sono solo
                    // le visualizzazioni costruite davvero: le altre
                    // compaiono man mano, perché un riquadro che non
                    // cambia niente sarebbe una bugia.
                    DatabaseLayout.entries.chunked(3).forEach { rowLayouts ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowLayouts.forEach { candidate ->
                                LayoutChoice(
                                    layout = candidate,
                                    isSelected = candidate == layout,
                                    onClick = { onSetLayout(candidate) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowLayouts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                SettingsPage.EDIT_PROPERTIES -> {
                    SheetGroup {
                        columns.forEach { column ->
                            SettingsRow(
                                icon = column.type.icon(),
                                label = DbStrings.columnName(column.name),
                                value = column.type.displayName(),
                                onClick = { onEditProperty(column) }
                            )
                            HorizontalDivider()
                        }
                        SettingsRow(
                            icon = Icons.Filled.Add,
                            label = DbStrings.newProperty,
                            value = null,
                            onClick = onAddProperty
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/** Un riquadro del selettore di visualizzazione, come su Notion. */
@Composable
private fun LayoutChoice(
    layout: DatabaseLayout,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(
        modifier = modifier
            .padding(4.dp)
            .border(1.dp, accent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            layout.icon(),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = accent
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            layout.displayName(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Il titolo di una delle due liste di "Property visibility", con la
 * scorciatoia per spostarle tutte di là in un colpo solo.
 */
@Composable
private fun VisibilitySectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit
) {
    Spacer(modifier = Modifier.size(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = action,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onAction() }
                .padding(vertical = 6.dp, horizontal = 4.dp)
        )
    }
    Spacer(modifier = Modifier.size(6.dp))
}

/**
 * Una proprietà dentro "Property visibility": il suo nome e l'occhio
 * che la sposta nell'altra lista.
 *
 * Tutta la riga è toccabile, non solo l'occhio: su un telefono un
 * bersaglio da venti punti in fondo a destra si sbaglia, e qui non c'è
 * niente di irreversibile da proteggere con la mira.
 */
@Composable
private fun PropertyVisibilityRow(
    column: DatabaseColumnEntity,
    hidden: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            column.type.icon(),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            text = DbStrings.columnName(column.name),
            color = if (hidden) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                Color.Unspecified
            },
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (hidden) {
                DbStrings.showInTable(DbStrings.columnName(column.name))
            } else {
                DbStrings.hideFromTable(DbStrings.columnName(column.name))
            },
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Il riquadro arrotondato che raggruppa più voci dentro una finestra,
 * come su Notion: le voci affini stanno insieme su uno sfondo un po'
 * più chiaro di quello della finestra, separate da linee sottili.
 */
@Composable
private fun SheetGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
    /** Rosso, per le voci che tolgono qualcosa e non si possono annullare. */
    danger: Boolean = false,
    /**
     * Spenta: si vede ma non risponde. La usa "Lock view" per le voci
     * che cambiano l'impaginazione — restano in elenco, così si capisce
     * che ci sono e che è la chiave a tenerle ferme.
     */
    enabled: Boolean = true
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        danger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable { onClick() } else it }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            text = label,
            color = when {
                !enabled -> tint
                danger -> MaterialTheme.colorScheme.error
                else -> Color.Unspecified
            },
            modifier = Modifier.weight(1f)
        )
        if (!value.isNullOrEmpty()) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
    }
}

/**
 * La finestra che scorre dal basso per creare o modificare una
 * proprietà: nome in alto, poi l'elenco dei tipi con la ricerca, come
 * su Notion. Una sola finestra per entrambi i casi, così creazione e
 * modifica non possono allontanarsi col tempo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertySheet(
    target: PropertyTarget,
    onDismiss: () -> Unit,
    onSave: (String, ColumnType, String) -> Unit,
    onDelete: () -> Unit
) {
    val existing = (target as? PropertyTarget.Existing)?.column

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var selectedType by remember(existing?.id) {
        mutableStateOf(existing?.type ?: (target as? PropertyTarget.New)?.initialType ?: ColumnType.TEXT)
    }
    var typeQuery by remember(existing?.id) { mutableStateOf("") }
    // I tag esistenti si portano dietro etichette e colori, e da qui non
    // si toccano: si salvano così come sono.
    val optionsJson = existing?.optionsJson ?: "[]"

    // Il nome può restare vuoto: in quel caso la proprietà prende il
    // nome del tipo, come su Notion. Prima il pulsante di conferma in
    // quel caso non faceva niente, in silenzio.
    val effectiveName = name.ifBlank { DbStrings.englishTypeName(selectedType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Intestazione fissa, fuori dalla parte che scorre: l'elenco
            // dei tipi è lungo, e con i pulsanti in fondo si arrivava a
            // scegliere un tipo senza più vedere come confermare.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) DbStrings.newProperty else DbStrings.editProperty,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
                TextButton(
                    onClick = { onSave(effectiveName, selectedType, optionsJson) }
                ) { Text(if (existing == null) DbStrings.add else EditorStrings.save) }
            }
            HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.size(16.dp))

            SheetTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = DbStrings.propertyName,
                leading = Icons.Filled.Notes
            )

            // Qui non si scrivono le opzioni di un tag. Prima c'era un
            // campo per elencarle separate da virgola, ed era una
            // trappola: salvando, le opzioni venivano ricostruite da
            // quel testo, quindi si perdevano i colori assegnati e
            // sparivano i tag creati nel frattempo dalle celle. I tag
            // si creano e si modificano dalla cella, che è l'unico
            // posto in cui esiste anche il loro colore.

            Spacer(modifier = Modifier.size(20.dp))
            Text(
                DbStrings.type,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(8.dp))
            SheetTextField(
                value = typeQuery,
                onValueChange = { typeQuery = it },
                placeholder = Strings.search,
                leading = Icons.Filled.Search
            )
            Spacer(modifier = Modifier.size(8.dp))

            ColumnType.entries
                .filter { it.displayName().contains(typeQuery, ignoreCase = true) }
                .forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(type.displayName(), modifier = Modifier.weight(1f))
                        if (type == selectedType) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = DbStrings.selected,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

            // Qui non si sposta la colonna: "Move to left" e "Move to
            // right" stanno in cima al menu che si apre tenendo premuta
            // l'intestazione, e averle in due posti voleva dire due
            // versioni della stessa cosa (in questa finestra, per di
            // più, comparivano e sparivano a seconda della colonna).
            if (existing != null) {
                Spacer(modifier = Modifier.size(20.dp))
                HorizontalDivider()
                SheetAction(Icons.Filled.Delete, DbStrings.deleteProperty, isDestructive = true) {
                    onDelete()
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
        }
    }
}

@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            leading,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Spacer(modifier = Modifier.size(12.dp))
        Text(label, color = tint)
    }
}

private fun DatabaseLayout.displayName(): String = DbStrings.layoutName(this)

private fun DatabaseLayout.icon(): ImageVector = when (this) {
    DatabaseLayout.TABLE -> Icons.Filled.TableChart
    DatabaseLayout.BOARD -> Icons.Filled.ViewColumn
    DatabaseLayout.CALENDAR -> Icons.Filled.CalendarToday
    DatabaseLayout.TIMELINE -> Icons.Filled.ViewTimeline
    DatabaseLayout.LIST -> Icons.Filled.FormatListBulleted
    // La stessa icona della voce "Gallery view" nel menu "/".
    DatabaseLayout.GALLERY -> Icons.Filled.GridView
}

private fun GalleryCardPreview.icon(): ImageVector = when (this) {
    GalleryCardPreview.NONE -> Icons.Filled.Block
    GalleryCardPreview.PAGE_COVER -> Icons.Filled.Image
    GalleryCardPreview.PAGE_CONTENT -> Icons.Filled.Notes
}

private fun GalleryCardSize.icon(): ImageVector = when (this) {
    GalleryCardSize.SMALL -> Icons.Filled.ViewComfy
    GalleryCardSize.MEDIUM -> Icons.Filled.GridView
    GalleryCardSize.LARGE -> Icons.Filled.ViewAgenda
}

/** Il nome del tipo nella lingua dell'app. Per il nome che si salva vedi `DbStrings.englishTypeName`. */
private fun ColumnType.displayName(): String = DbStrings.typeName(this)

private fun ColumnType.icon(): ImageVector = when (this) {
    ColumnType.TEXT -> Icons.Filled.Notes
    ColumnType.NUMBER -> Icons.Filled.Tag
    ColumnType.SELECT -> Icons.Filled.ArrowDropDownCircle
    ColumnType.MULTI_SELECT -> Icons.Filled.Checklist
    ColumnType.DATE -> Icons.Filled.CalendarToday
    ColumnType.CHECKBOX -> Icons.Filled.CheckBox
    ColumnType.URL -> Icons.Filled.Link
    ColumnType.EMAIL -> Icons.Filled.AlternateEmail
    ColumnType.PHONE -> Icons.Filled.Call
    ColumnType.CREATED_TIME -> Icons.Filled.Schedule
    ColumnType.LAST_EDITED_TIME -> Icons.Filled.Update
}

/**
 * La maiuscola automatica vale per il testo libero, non per indirizzi e
 * numeri: in un'email o in un URL una maiuscola iniziale messa dalla
 * tastiera è quasi sempre da cancellare.
 */
private fun ColumnType.capitalization(): KeyboardCapitalization = when (this) {
    ColumnType.TEXT -> KeyboardCapitalization.Sentences
    else -> KeyboardCapitalization.None
}

private fun ColumnType.keyboardType(): KeyboardType = when (this) {
    ColumnType.NUMBER -> KeyboardType.Number
    ColumnType.URL -> KeyboardType.Uri
    ColumnType.EMAIL -> KeyboardType.Email
    ColumnType.PHONE -> KeyboardType.Phone
    else -> KeyboardType.Text
}

/** Le opzioni di una colonna a selezione, lette dal JSON salvato. */
private fun parseSelectOptionsFull(optionsJson: String): List<SelectOption> {
    return try {
        Json.decodeFromString<List<SelectOption>>(optionsJson)
    } catch (e: Exception) {
        emptyList()
    }
}

/** Solo le etichette, per chi non ha bisogno dei colori. */
private fun parseSelectOptions(optionsJson: String): List<String> =
    parseSelectOptionsFull(optionsJson).map { it.label }

/** Il colore di un tag; se manca o è scritto male, il grigio di base. */
private fun tagColor(hex: String?): Color {
    if (hex == null) return DarkSurfaceVariant
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        DarkSurfaceVariant
    }
}

/**
 * Quanto tempo sta in una schermata della linea del tempo, e con che
 * passo è segnato l'asse.
 *
 * Il modello è uno solo per tutti gli ingrandimenti: **la larghezza di
 * un giorno**. Da lì discende tutto — dove comincia una barra, quanto è
 * lunga, dove cade oggi — e le caselle dell'asse sono larghe quanti
 * giorni durano. Così un mese di ventotto giorni è più stretto di uno
 * di trentuno e le barre restano incollate alle loro date, invece di
 * scivolare rispetto alle etichette come succederebbe dando a ogni mese
 * la stessa larghezza.
 */
private enum class TimelineTick { HOUR, DAY, WEEK, MONTH }

@Immutable
private data class TimelineScale(
    val dayWidth: Dp,
    val tick: TimelineTick,
    /** Quanto respiro lasciare oltre le date che ci sono. */
    val padDays: Long
)

private fun TimelineZoom.scale(): TimelineScale = when (this) {
    // Un giorno largo quanto ventiquattro caselle da 52: alle ore
    // serve tutta questa larghezza, ed è il motivo per cui l'asse a ore
    // copre pochi giorni e non tutto il database.
    TimelineZoom.HOURS -> TimelineScale(TIMELINE_HOUR_WIDTH * 24, TimelineTick.HOUR, 1)
    TimelineZoom.DAY -> TimelineScale(40.dp, TimelineTick.DAY, 7)
    TimelineZoom.WEEK -> TimelineScale(22.dp, TimelineTick.DAY, 14)
    TimelineZoom.BI_WEEK -> TimelineScale(14.dp, TimelineTick.DAY, 21)
    TimelineZoom.MONTH -> TimelineScale(9.dp, TimelineTick.DAY, 31)
    TimelineZoom.QUARTER -> TimelineScale(3.2.dp, TimelineTick.WEEK, 90)
    TimelineZoom.YEAR -> TimelineScale(0.9.dp, TimelineTick.MONTH, 365)
}

private fun TimelineZoom.displayName(): String = DbStrings.zoomName(this)

/** Una casella dell'asse: quanto è larga e cosa c'è scritto sopra. */
@Immutable
private data class TimelineTickCell(
    val width: Dp,
    val label: String,
    val weekend: Boolean,
    val isToday: Boolean
)

/**
 * La visualizzazione a linea del tempo: un asse del tempo che scorre in
 * orizzontale, e una riga per pagina con la barra lunga quanto i giorni
 * che copre.
 *
 * È il calendario srotolato. Nel calendario il tempo va a capo ogni
 * settimana e una pagina lunga si spezza in più pezzi; qui il tempo è
 * una riga sola e la barra è intera, che è tutto il punto di una linea
 * del tempo: confrontare a colpo d'occhio durate e sovrapposizioni.
 *
 * L'ordine delle righe è lo stesso del calendario — le pagine per data,
 * la prima in alto — così passando da una vista all'altra non si perde
 * il filo. Qui però ogni riga ha per forza la sua pagina, quindi non
 * restano righe vuote.
 *
 * I nomi stanno in una colonna ferma a sinistra: scorrendo di mesi le
 * barre se ne vanno, e senza i nomi accanto non si saprebbe più di chi
 * è quella che si sta guardando.
 */
@Composable
private fun TimelineLayout(
    state: DatabaseTableState,
    dateColumn: DatabaseColumnEntity?,
    zoom: TimelineZoom,
    newRowTick: Int,
    onSetZoom: (TimelineZoom) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddDateProperty: () -> Unit
) {
    if (dateColumn == null) {
        MissingPropertyNotice(
            message = DbStrings.timelineNeedsDate,
            actionLabel = DbStrings.addDateProperty,
            onAction = onAddDateProperty
        )
        return
    }

    val today = LocalDate.now()
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    val dated = remember(state.rows, state.cellValues, dateColumn.id) {
        state.rows.mapNotNull { row ->
            parseDateRange(state.cellValues[row.id to dateColumn.id].orEmpty())
                ?.let { row to it }
        }
    }
    val undated = remember(state.rows, state.cellValues, dateColumn.id) {
        state.rows.filter { state.cellValues[it.id to dateColumn.id].isNullOrBlank() }
    }

    // Le pagine in ordine di data: lo stesso ordine del calendario, e la
    // stessa regola per i pareggi.
    val bars = remember(dated) {
        dated.map { (row, range) -> timelineBar(row, range) }
            .sortedWith(
                compareBy<TimelineBarData> { it.startAt }
                    .thenBy { it.endAt }
                    .thenBy { it.row.title }
                    .thenBy { it.row.id }
            )
    }

    var showZoomSheet by remember { mutableStateOf(false) }
    val scale = zoom.scale()

    if (bars.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = DbStrings.noPageHasDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            TimelineUndatedRows(undated, onOpenRow, onRowLongPress)
        }
        return
    }

    // L'asse comincia e finisce dove serve: la prima e l'ultima data,
    // oggi sempre compreso, e il respiro che chiede l'ingrandimento.
    // Poi viene allineato all'inizio della sua unità, altrimenti le
    // caselle dei mesi comincerebbero a metà mese.
    val axis = remember(bars, today, zoom, firstDayOfWeek) {
        timelineAxis(
            first = bars.minOf { it.startAt }.toLocalDate(),
            last = bars.maxOf { it.endAt }.toLocalDate(),
            today = today,
            scale = scale,
            firstDayOfWeek = firstDayOfWeek
        )
    }

    val cells = remember(axis, zoom, today, firstDayOfWeek) {
        timelineTickCells(axis, scale, today, firstDayOfWeek)
    }

    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val dayWidthPx = with(density) { scale.dayWidth.toPx() }

    // Si apre su oggi, non all'inizio dell'asse: la pagina più vecchia
    // del database è raramente quella che interessa. Si aspetta che il
    // contenuto sia stato misurato, altrimenti lo spostamento verrebbe
    // tagliato a zero perché la lunghezza non si conosce ancora.
    // Il bersaglio è l'**istante** di adesso, non la mezzanotte di
    // oggi: a ingrandimento "ore" mezzanotte è mezza giornata di
    // distanza, e si aprirebbe lontano da quello che sta succedendo.
    // Il rientro fisso lascia un po' di passato in vista, così adesso
    // non è incollato al bordo sinistro.
    val leadInPx = with(density) { TIMELINE_TODAY_LEAD_IN.toPx() }
    fun todayTarget(): Int =
        (timelineNowOffsetDays(axis.start) * dayWidthPx - leadInPx)
            .toInt().coerceAtLeast(0)

    LaunchedEffect(axis, zoom, newRowTick) {
        snapshotFlow { scroll.maxValue }.first { it > 0 }
        scroll.scrollTo(todayTarget())
    }

    val rowsShown = bars.size.coerceAtMost(CALENDAR_MAX_ROWS)
    val axisWidth = scale.dayWidth * axis.totalDays
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        // L'intestazione ferma: a sinistra dove siamo, a destra come si
        // guarda e come tornare a oggi.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelinePeriodTitle(modifier = Modifier.weight(1f)) {
                val day = (scroll.value / dayWidthPx).toInt()
                    .coerceIn(0, (axis.totalDays - 1).coerceAtLeast(0))
                val date = axis.start.plusDays(day.toLong())
                when (zoom) {
                    TimelineZoom.HOURS -> date.format(DATE_FORMAT)
                    TimelineZoom.YEAR -> date.year.toString()
                    else -> date.format(MONTH_FORMAT)
                }
            }
            TimelineHeaderChip(
                label = zoom.displayName(),
                trailingIcon = Icons.Filled.KeyboardArrowDown,
                onClick = { showZoomSheet = true }
            )
            Spacer(modifier = Modifier.size(8.dp))
            TimelineHeaderChip(
                label = DbStrings.today,
                trailingIcon = null,
                onClick = { scope.launch { scroll.animateScrollTo(todayTarget()) } }
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // La colonna dei nomi non scorre con l'asse.
            Column(modifier = Modifier.width(TIMELINE_NAME_WIDTH)) {
                Spacer(modifier = Modifier.height(TIMELINE_HEADER_HEIGHT))
                HorizontalDivider()
                bars.take(rowsShown).forEach { bar ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TIMELINE_ROW_HEIGHT)
                            .pointerInput(bar.row.id) {
                                detectTapGestures(
                                    onTap = { onOpenRow(bar.row) },
                                    onLongPress = { onRowLongPress(bar.row) }
                                )
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RowIconBadge(bar.row.id, BAR_ICON_SIZE)
                            Text(
                                text = bar.row.title.ifBlank { Strings.untitled },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            VerticalDivider()

            Column(modifier = Modifier.horizontalScroll(scroll)) {
                TimelineAxisHeader(cells = cells)
                HorizontalDivider(modifier = Modifier.width(axisWidth))

                Box {
                    Column {
                        bars.take(rowsShown).forEach { bar ->
                            TimelineBarRow(
                                bar = bar,
                                axisStart = axis.start,
                                totalDays = axis.totalDays,
                                dayWidth = scale.dayWidth,
                                onOpenRow = onOpenRow,
                                onRowLongPress = onRowLongPress
                            )
                        }
                    }
                    // La riga di oggi attraversa tutte le barre: è il
                    // riferimento che dice cosa è in ritardo e cosa no.
                    // Sta all'ora esatta, non a mezzanotte, perché a
                    // ingrandimento "ore" la differenza si vede tutta.
                    val nowOffset = timelineNowOffsetDays(axis.start)
                    if (nowOffset >= 0f && nowOffset <= axis.totalDays) {
                        Box(
                            modifier = Modifier
                                .offset(x = scale.dayWidth * nowOffset)
                                .width(1.dp)
                                .height(TIMELINE_ROW_HEIGHT * rowsShown)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        if (bars.size > rowsShown) {
            Text(
                text = "+${bars.size - rowsShown} more, in the table",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
        }

        TimelineUndatedRows(undated, onOpenRow, onRowLongPress)
    }

    if (showZoomSheet) {
        TimelineZoomSheet(
            current = zoom,
            onPick = { onSetZoom(it) },
            onDismiss = { showZoomSheet = false }
        )
    }
}

/** L'asse: da quando, per quanti giorni. */
@Immutable
private data class TimelineAxis(val start: LocalDate, val totalDays: Int)

/**
 * I confini dell'asse, allineati all'unità dell'ingrandimento.
 *
 * Il tetto sulle caselle non è prudenza sprecata: non sono pigre,
 * vengono composte tutte insieme. Quando le date sono più larghe di
 * quanto il tetto consenta, la finestra si stringe **attorno a oggi**,
 * che è il punto da cui si guarda quasi sempre.
 */
private fun timelineAxis(
    first: LocalDate,
    last: LocalDate,
    today: LocalDate,
    scale: TimelineScale,
    firstDayOfWeek: DayOfWeek
): TimelineAxis {
    val rawStart = minOf(first, today).minusDays(scale.padDays)
    val rawEnd = maxOf(last, today).plusDays(scale.padDays)

    fun align(date: LocalDate) = when (scale.tick) {
        TimelineTick.HOUR, TimelineTick.DAY -> date
        TimelineTick.WEEK -> startOfWeek(date, firstDayOfWeek)
        TimelineTick.MONTH -> date.withDayOfMonth(1)
    }

    val maxDays = when (scale.tick) {
        TimelineTick.HOUR -> TIMELINE_MAX_TICKS / 24
        TimelineTick.DAY -> TIMELINE_MAX_TICKS
        TimelineTick.WEEK -> TIMELINE_MAX_TICKS * 7
        TimelineTick.MONTH -> TIMELINE_MAX_TICKS * 31
    }.coerceAtLeast(2)

    var start = align(rawStart)
    var end = rawEnd
    if (ChronoUnit.DAYS.between(start, end) > maxDays) {
        start = align(today.minusDays(maxDays / 3L))
        end = start.plusDays(maxDays.toLong())
    }
    val days = (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
    return TimelineAxis(start, days)
}

/** Le caselle dell'asse, una per unità dell'ingrandimento. */
private fun timelineTickCells(
    axis: TimelineAxis,
    scale: TimelineScale,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek
): List<TimelineTickCell> {
    val cells = mutableListOf<TimelineTickCell>()
    when (scale.tick) {
        TimelineTick.HOUR -> {
            val hourWidth = scale.dayWidth / 24
            repeat(axis.totalDays) { dayIndex ->
                val date = axis.start.plusDays(dayIndex.toLong())
                repeat(24) { hour ->
                    cells += TimelineTickCell(
                        width = hourWidth,
                        label = hour.toString().padStart(2, '0'),
                        weekend = false,
                        isToday = date == today
                    )
                }
            }
        }

        TimelineTick.DAY -> {
            // Con le caselle strette il numero non ci sta su tutte: si
            // scrive una ogni tot, e le altre restano segni muti.
            val every = timelineLabelEvery(scale.dayWidth)
            repeat(axis.totalDays) { index ->
                val date = axis.start.plusDays(index.toLong())
                cells += TimelineTickCell(
                    width = scale.dayWidth,
                    label = if (index % every == 0) date.dayOfMonth.toString() else "",
                    weekend = date.dayOfWeek == DayOfWeek.SATURDAY ||
                        date.dayOfWeek == DayOfWeek.SUNDAY,
                    isToday = date == today
                )
            }
        }

        TimelineTick.WEEK -> {
            // Solo il giorno, e non su tutte le settimane: "13 Sep"
            // vuole il doppio della larghezza di una casella da una
            // settimana, e scritto su ognuna diventa una fila di
            // parole sovrapposte. Il mese lo dice l'intestazione ferma
            // sopra l'asse.
            val cellWidth = scale.dayWidth * 7
            val every = if (cellWidth >= 40.dp) 1 else 2
            var date = axis.start
            var index = 0
            val end = axis.start.plusDays(axis.totalDays.toLong() - 1)
            while (!date.isAfter(end)) {
                val weekEnd = date.plusDays(6)
                cells += TimelineTickCell(
                    width = cellWidth,
                    label = if (index % every == 0) date.dayOfMonth.toString() else "",
                    weekend = false,
                    isToday = !today.isBefore(date) && !today.isAfter(weekEnd)
                )
                date = date.plusWeeks(1)
                index++
            }
        }

        TimelineTick.MONTH -> {
            var month = YearMonth.from(axis.start)
            val end = axis.start.plusDays(axis.totalDays.toLong() - 1)
            while (!month.atDay(1).isAfter(end)) {
                cells += TimelineTickCell(
                    // Larga quanti giorni ha davvero: è così che le
                    // barre restano incollate alle loro date.
                    width = scale.dayWidth * month.lengthOfMonth(),
                    label = month.month
                        .getDisplayName(TextStyle.SHORT, displayLocale),
                    weekend = false,
                    isToday = YearMonth.from(today) == month
                )
                month = month.plusMonths(1)
            }
        }
    }
    return cells
}

/** Ogni quante caselle si scrive il numero, perché due cifre ci stiano. */
private fun timelineLabelEvery(dayWidth: Dp): Int =
    when {
        dayWidth >= 20.dp -> 1
        dayWidth >= 12.dp -> 2
        else -> 5
    }

/** Quanti giorni (con la frazione) separano l'inizio dell'asse da adesso. */
private fun timelineNowOffsetDays(axisStart: LocalDate): Float {
    val now = LocalDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(axisStart.atStartOfDay(), now)
    return minutes / 1440f
}

/**
 * Una pagina sulla linea del tempo: da quando a quando, con l'ora.
 *
 * La fine è **esclusiva**. Una pagina di un giorno senza ora va da
 * mezzanotte a mezzanotte del giorno dopo, cioè occupa il giorno
 * intero: se la fine fosse inclusiva sarebbe lunga zero. Una pagina
 * con l'ora e senza fine è invece un istante, e resta lunga zero —
 * la si vede lo stesso perché le barre hanno una larghezza minima.
 */
@Immutable
private data class TimelineBarData(
    val row: DatabaseRowEntity,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)

private fun timelineBar(row: DatabaseRowEntity, range: DateRange): TimelineBarData {
    val startAt = if (range.hasTime) {
        range.start.toLocalDateTime()
    } else {
        range.start.toLocalDate().atStartOfDay()
    }
    val endAt = when {
        range.end != null && range.hasTime -> range.end.toLocalDateTime()
        range.end != null -> range.end.toLocalDate().plusDays(1).atStartOfDay()
        range.hasTime -> startAt
        else -> range.start.toLocalDate().plusDays(1).atStartOfDay()
    }
    return TimelineBarData(row, startAt, maxOf(endAt, startAt))
}

/**
 * L'asse: una casella per unità, con il fine settimana in ombra.
 *
 * Il periodo non è scritto qui ma nell'intestazione ferma sopra la
 * linea del tempo: scritto sull'asse starebbe all'inizio del suo tratto
 * e scorrendo sparirebbe a sinistra, lasciando una fila di numeri senza
 * sapere di quale mese sono.
 */
@Composable
private fun TimelineAxisHeader(cells: List<TimelineTickCell>) {
    Row {
        cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .width(cell.width)
                    .height(TIMELINE_HEADER_HEIGHT)
                    .background(if (cell.weekend) DarkSurface else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (cell.label.isNotEmpty()) {
                    Text(
                        text = cell.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cell.isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        maxLines = 1,
                        // Agli ingrandimenti larghi la casella di un
                        // giorno è più stretta di due cifre: senza
                        // questo il numero viene tagliato a metà e si
                        // legge "2" dove c'è scritto "25". Qui il testo
                        // si misura libero e sborda sulle caselle
                        // accanto, che tanto sono mute.
                        modifier = Modifier.wrapContentWidth(
                            align = Alignment.CenterHorizontally,
                            unbounded = true
                        )
                    )
                }
            }
        }
    }
}

/**
 * Il periodo che si sta guardando, sopra la linea del tempo.
 *
 * Prende il testo da una funzione invece che da un valore: così è
 * questo pezzetto a leggere la posizione dello scorrimento, ed è
 * l'unico a doversi ridisegnare mentre il dito trascina l'asse. Se lo
 * leggesse chi sta sopra, ogni giorno scorso rifarebbe tutta la linea
 * del tempo.
 */
@Composable
private fun TimelinePeriodTitle(modifier: Modifier = Modifier, label: () -> String) {
    Text(
        text = label(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        modifier = modifier
    )
}

/** Un pulsantino dell'intestazione della linea del tempo. */
@Composable
private fun TimelineHeaderChip(
    label: String,
    trailingIcon: ImageVector?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(percent = 50))
            .clickable { onClick() }
            .padding(start = 12.dp, end = if (trailingIcon == null) 12.dp else 6.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        if (trailingIcon != null) {
            Icon(
                trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** La finestra per scegliere quanto tempo far stare in una schermata. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineZoomSheet(
    current: TimelineZoom,
    onPick: (TimelineZoom) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
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
                text = DbStrings.selectZoomLevel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text(Strings.done) }
        }
        HorizontalDivider()
        TimelineZoom.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPick(option)
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (option == current) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider()
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** La riga di una pagina: lo spazio prima, poi la barra. */
@Composable
private fun TimelineBarRow(
    bar: TimelineBarData,
    axisStart: LocalDate,
    totalDays: Int,
    dayWidth: Dp,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    // La posizione è in giorni **con la frazione**, non in giorni
    // interi: è quello che fa valere l'ora. A ingrandimento "ore" una
    // pagina che comincia alle 14:30 deve stare a metà pomeriggio, non
    // a mezzanotte.
    //
    // L'intervallo viene tagliato sull'asse: una data oltre il bordo
    // non deve spingere la barra fuori da quello che è disegnato.
    val axisStartAt = axisStart.atStartOfDay()
    val from = (ChronoUnit.MINUTES.between(axisStartAt, bar.startAt) / 1440f)
        .coerceIn(0f, totalDays.toFloat())
    val to = (ChronoUnit.MINUTES.between(axisStartAt, bar.endAt) / 1440f)
        .coerceIn(from, totalDays.toFloat())

    Box(
        modifier = Modifier
            .width(dayWidth * totalDays)
            .height(TIMELINE_ROW_HEIGHT)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = dayWidth * from)
                // Agli ingrandimenti larghi un giorno solo sarebbe meno
                // di un pixel: la barra non deve sparire, deve restare
                // un segno che si può toccare.
                .width((dayWidth * (to - from)).coerceAtLeast(TIMELINE_MIN_BAR_WIDTH))
                .height(TIMELINE_ROW_HEIGHT - 10.dp)
                .padding(horizontal = 1.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                .pointerInput(bar.row.id) {
                    detectTapGestures(
                        onTap = { onOpenRow(bar.row) },
                        onLongPress = { onRowLongPress(bar.row) }
                    )
                }
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIconBadge(bar.row.id, BAR_ICON_SIZE)
                Text(
                    text = bar.row.title.ifBlank { Strings.untitled },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Le pagine senza data, in fondo. Non hanno un posto sull'asse ma non
 * devono sparire: sarebbero raggiungibili solo cambiando vista.
 */
@Composable
private fun TimelineUndatedRows(
    undated: List<DatabaseRowEntity>,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    if (undated.isEmpty()) return
    Text(
        text = DbStrings.noDate(undated.size),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
    undated.forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(row.id) {
                    detectTapGestures(
                        onTap = { onOpenRow(row) },
                        onLongPress = { onRowLongPress(row) }
                    )
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowIconBadge(row.id, ROW_ICON_SIZE)
            Text(
                text = row.title.ifBlank { Strings.untitled },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}
