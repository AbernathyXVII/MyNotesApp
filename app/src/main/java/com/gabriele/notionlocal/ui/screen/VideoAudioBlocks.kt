package com.gabriele.notionlocal.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.launch

/**
 * **"Upload" o "Link"** (dal 06/10/2026), la finestrella delle foto di
 * Notion per video, audio e file: o si sceglie un file dal telefono, o si
 * incolla un indirizzo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaSourceSheet(
    title: String,
    uploadLabel: String,
    linkButton: String,
    linkHint: String?,
    onUpload: () -> Unit,
    onLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var linkTab by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = NotionWhite, modifier = Modifier.padding(bottom = 12.dp))
            Row {
                listOf(false to BlockStrings.uploadTab, true to BlockStrings.linkTab).forEach { (isLink, label) ->
                    val selected = linkTab == isLink
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { linkTab = isLink }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) NotionWhite else NotionGray400)
                        Spacer(modifier = Modifier.size(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .background(if (selected) NotionWhite else Color.Transparent)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
            if (!linkTab) {
                Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) { Text(uploadLabel) }
            } else {
                val submit = {
                    val url = normalizedLink(link)
                    if (url == null) wrong = true else onLink(url)
                }
                AutoCaps {
                    OutlinedTextField(
                        value = link,
                        onValueChange = {
                            link = it
                            wrong = false
                        },
                        singleLine = true,
                        isError = wrong,
                        placeholder = { Text(BlockStrings.pasteLink) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (wrong) {
                    Text(
                        BlockStrings.notALink,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Button(onClick = submit, enabled = link.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(linkButton) }
                if (linkHint != null) {
                    Text(linkHint, style = MaterialTheme.typography.bodySmall, color = NotionGray400, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/** Un indirizzo pulito: "https://" se manca, e almeno un punto nel nome del sito. Null se non è un link. */
internal fun normalizedLink(input: String): String? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(text)) text else "https://$text"
    val uri = runCatching { Uri.parse(withScheme) }.getOrNull() ?: return null
    val host = uri.host ?: return null
    if (uri.scheme?.lowercase() !in setOf("http", "https") || !host.contains('.')) return null
    return withScheme
}

/** Il nome del sito di un indirizzo, senza "www.": "youtube.com". */
internal fun linkHost(url: String): String =
    runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: url

/**
 * Le cose che un blocco-file fa da sé, chieste dal suo menu: salvare una
 * copia dove si sceglie, aprirlo con un'altra app o nel browser.
 */
@Stable
internal class FileActions(val saveCopy: () -> Unit, val openElsewhere: () -> Unit)

@Composable
internal fun rememberFileActions(attrs: BlockAttrs, store: PageImageStore): FileActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveAs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(attrs.mime ?: "application/octet-stream")
    ) { target ->
        val file = attrs.file
        if (target == null || file == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = BlockFiles.copyTo(context, store, file, target)
            Toast.makeText(context, if (saved) BlockStrings.fileSaved else BlockStrings.saveFailed, Toast.LENGTH_SHORT).show()
        }
    }
    return FileActions(
        saveCopy = {
            if (attrs.file != null) {
                val name = attrs.name ?: ("notE" + (BlockFiles.extensionFor(attrs.mime)?.let { ".$it" } ?: ""))
                saveAs.launch(name)
            }
        },
        openElsewhere = {
            val file = attrs.file
            val url = attrs.url
            val opened = when {
                file != null -> BlockFiles.openWithAnotherApp(context, store, file, attrs.mime)
                url != null -> BlockFiles.openUrl(context, url)
                else -> true
            }
            if (!opened) Toast.makeText(context, BlockStrings.noAppToOpen, Toast.LENGTH_SHORT).show()
        }
    )
}

/** Un file scelto dal telefono, copiato nell'app col suo nome, tipo e peso: per video, audio e file. */
internal suspend fun importFile(context: Context, store: PageImageStore, uri: Uri): BlockAttrs? {
    val name = BlockFiles.displayName(context, uri)
    val mime = BlockFiles.mimeType(context, uri, name)
    val extension = name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() } ?: BlockFiles.extensionFor(mime)
    val file = store.saveFile(uri, extension).getOrNull() ?: return null
    return BlockAttrs(file = file, name = name, mime = mime, size = store.fileFor(file).length())
}

/** La riga sotto un video da un link: il nome del sito e i tre puntini del menu del blocco. */
@Composable
private fun EmbedFooter(url: String, onMenu: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
        Text(linkHost(url), style = MaterialTheme.typography.labelMedium, color = NotionGray400, modifier = Modifier.weight(1f))
        if (onMenu != null) {
            IconButton(onClick = onMenu, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = EditorStrings.blockOptions, tint = NotionGray400)
            }
        }
    }
}

/**
 * Le richieste del menu dei sei puntini (vedi `PageEditorViewModel.mediaRequest`)
 * per un blocco di video, audio o file: le fa lui, che resta aperto.
 */
@Composable
internal fun HandleFileRequests(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    actions: FileActions,
    onPick: () -> Unit
) {
    val request by viewModel.mediaRequest.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        val asked = request?.takeIf { it.blockId == block.id } ?: return@LaunchedEffect
        viewModel.consumeMediaRequest()
        when (asked.kind) {
            MediaRequestKind.PICK -> if (!locked) onPick()
            MediaRequestKind.DOWNLOAD -> actions.saveCopy()
            MediaRequestKind.OPEN_ELSEWHERE -> actions.openElsewhere()
            MediaRequestKind.VIEW -> Unit
        }
    }
}

/**
 * Il file scelto dal telefono, copiato nell'app e messo nel blocco; se non
 * si riesce a leggerlo, lo dice.
 */
@Composable
internal fun rememberImport(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    store: PageImageStore,
    onBusy: (Boolean) -> Unit
): (Uri?) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return { uri ->
        if (uri != null) {
            onBusy(true)
            scope.launch {
                val imported = importFile(context, store, uri)
                if (imported != null) {
                    viewModel.updateBlockAttrs(block.id) { imported }
                } else {
                    Toast.makeText(context, BlockStrings.fileReadFailed, Toast.LENGTH_SHORT).show()
                }
                onBusy(false)
            }
        }
    }
}

/**
 * **Il blocco "Video"** (dal 06/10/2026, chiesto dall'utente): un video
 * caricato dal telefono o preso da un link.
 *
 * - **Un file, o il link a un video** (.mp4…): si guarda qui, col lettore
 *   dell'app — play, pausa, la barra col pallino per andare avanti e indietro;
 * - **YouTube e Vimeo**: nel loro lettore, dentro la pagina;
 * - dal **menu del blocco** (tenendolo premuto, o i tre puntini sotto un
 *   video da un link): aprirlo nel browser o nell'app di YouTube, o il file
 *   con un altro lettore; scaricarlo; sostituirlo; copiarlo; toglierlo.
 */
@Composable
internal fun VideoBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember(context) { PageImageStore(context) }
    val attrs = remember(block.attrsJson) { block.attrs }
    var busy by remember(block.id) { mutableStateOf(false) }
    var choosing by remember(block.id) { mutableStateOf(false) }
    val actions = rememberFileActions(attrs, store)
    val import = rememberImport(block, viewModel, store) { busy = it }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { import(it) }
    HandleFileRequests(block, viewModel, locked, actions) { choosing = true }

    val file = attrs.file
    val url = attrs.url
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        when {
            file != null -> InlineVideoPlayer(Uri.fromFile(store.fileFor(file)), onLongPress = onLongPress)
            url != null -> {
                val youTube = youTubeId(url)
                val vimeo = vimeoId(url)
                val embed = when {
                    youTube != null -> "https://www.youtube-nocookie.com/embed/$youTube?playsinline=1&rel=0"
                    vimeo != null -> "https://player.vimeo.com/video/$vimeo"
                    else -> null
                }
                Column {
                    if (embed != null) EmbeddedVideoPlayer(embed) else InlineVideoPlayer(Uri.parse(url), onLongPress = onLongPress)
                    EmbedFooter(url, onLongPress)
                }
            }
            else -> MediaPlaceholder(
                icon = Icons.Outlined.OndemandVideo,
                text = BlockStrings.addVideo,
                busy = busy,
                onClick = if (locked) null else { { choosing = true } },
                onLongPress = onLongPress
            )
        }
    }

    if (choosing) {
        MediaSourceSheet(
            title = EditorStrings.blockType("Video"),
            uploadLabel = BlockStrings.chooseVideo,
            linkButton = BlockStrings.embedVideo,
            linkHint = BlockStrings.videoLinkHint,
            onUpload = {
                choosing = false
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            onLink = { link ->
                choosing = false
                viewModel.updateBlockAttrs(block.id) { BlockAttrs(url = link) }
            },
            onDismiss = { choosing = false }
        )
    }
}

/**
 * **Il blocco "Audio"** (dal 06/10/2026, chiesto dall'utente): un file audio
 * caricato dal telefono o preso da un link, che si ascolta qui — play,
 * pausa, la barra col pallino (settima foto). Tenendolo premuto: aprirlo con
 * un'altra app o nel browser, scaricarlo, sostituirlo, copiarlo, toglierlo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AudioBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember(context) { PageImageStore(context) }
    val attrs = remember(block.attrsJson) { block.attrs }
    var busy by remember(block.id) { mutableStateOf(false) }
    var choosing by remember(block.id) { mutableStateOf(false) }
    val actions = rememberFileActions(attrs, store)
    val import = rememberImport(block, viewModel, store) { busy = it }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { import(it) }
    HandleFileRequests(block, viewModel, locked, actions) { choosing = true }

    val file = attrs.file
    val url = attrs.url
    if (file == null && url == null) {
        MediaPlaceholder(
            icon = Icons.Outlined.Audiotrack,
            text = BlockStrings.addAudio,
            busy = busy,
            onClick = if (locked) null else { { choosing = true } },
            onLongPress = onLongPress,
            modifier = modifier
        )
    } else {
        val source = if (file != null) Uri.fromFile(store.fileFor(file)) else Uri.parse(url)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            InlineAudioPlayer(source, title = attrs.name ?: url?.let { linkHost(it) } ?: EditorStrings.blockType("Audio"))
        }
    }

    if (choosing) {
        MediaSourceSheet(
            title = EditorStrings.blockType("Audio"),
            uploadLabel = BlockStrings.chooseFile,
            linkButton = BlockStrings.embedAudio,
            linkHint = null,
            onUpload = {
                choosing = false
                documents.launch(arrayOf("audio/*"))
            },
            onLink = { link ->
                choosing = false
                viewModel.updateBlockAttrs(block.id) { BlockAttrs(url = link, name = Uri.parse(link).lastPathSegment) }
            },
            onDismiss = { choosing = false }
        )
    }
}
