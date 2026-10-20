package com.gabriele.notionlocal.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.launch

/** Quanto può stringersi un'immagine: un quarto della pagina. */
private const val MIN_IMAGE_WIDTH = 0.25f

/**
 * **Il riquadro di un blocco-file ancora vuoto** (dal 06/10/2026), come su
 * Notion: l'icona grigia e "Add an image", "Embed or upload a video"…
 * Toccandolo si sceglie il file; tenendolo premuto si apre il menu del blocco.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaPlaceholder(
    icon: ImageVector,
    text: String,
    busy: Boolean,
    onClick: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .combinedClickable(
                enabled = !busy,
                onClick = { onClick?.invoke() },
                onLongClick = onLongPress
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(icon, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = NotionGray400)
    }
}

/**
 * Un'immagine intera, larga quanto il suo riquadro e alta quanto vuole la
 * sua forma. Letta già rimpicciolita a quanto serve (vedi `PageImageStore.load`).
 */
@Composable
private fun WholeImage(fileName: String, store: PageImageStore, description: String?, modifier: Modifier = Modifier) {
    // Un'immagine molto alta (la foto del Web Clipper) si mostra a strisce: vedi `StripImage`.
    val size = remember(fileName) { store.imageSize(fileName) }
    if (size != null && isTallImage(size.first, size.second)) {
        StripImage(store.fileFor(fileName), size.first, size.second, description, modifier)
        return
    }
    var widthPx by remember(fileName) { mutableIntStateOf(0) }
    var bitmap by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(fileName, widthPx) {
        if (widthPx <= 0) return@LaunchedEffect
        // Due volte la larghezza: ingrandita a tutto schermo resta nitida.
        bitmap = store.load(fileName, widthPx * 2)?.asImageBitmap()
    }
    Box(modifier = modifier.fillMaxWidth().onSizeChanged { widthPx = it.width }) {
        val image = bitmap
        if (image == null) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(DarkSurface))
        } else {
            Image(
                bitmap = image,
                contentDescription = description,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height.coerceAtLeast(1))
            )
        }
    }
}

/**
 * **Il blocco "Image"** (dal 06/10/2026, chiesto dall'utente): un'immagine
 * caricata dal telefono — solo così, come ha chiesto — larga quanto la
 * pagina di partenza e centrata.
 *
 * - **Toccandola** si apre a tutto schermo: lì la si ingrandisce con due
 *   dita e la si scarica nella Galleria;
 * - le **maniglie ai lati** la stringono o la allargano trascinandole;
 * - **tenendola premuta** si apre il menu: guardarla, scaricarla,
 *   sostituirla, spostarla in fondo a un'altra pagina, copiarla, toglierla.
 *
 * Il menu chiede, il blocco fa (vedi `PageEditorViewModel.mediaRequest`).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ImageBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember(context) { PageImageStore(context) }
    val attrs = remember(block.attrsJson) { block.attrs }
    val scope = rememberCoroutineScope()
    var busy by remember(block.id) { mutableStateOf(false) }
    var viewing by remember(block.id) { mutableStateOf(false) }
    val request by viewModel.mediaRequest.collectAsStateWithLifecycle()

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val name = BlockFiles.displayName(context, uri)
            store.saveFromGallery(uri).fold(
                onSuccess = { file -> viewModel.updateBlockAttrs(block.id) { it.copy(file = file, name = name) } },
                onFailure = { Toast.makeText(context, EditorStrings.imageReadFailed, Toast.LENGTH_SHORT).show() }
            )
            busy = false
        }
    }
    val pick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val download: () -> Unit = {
        attrs.file?.let { file ->
            scope.launch {
                val saved = BlockFiles.saveImageToGallery(context, store, file, attrs.name)
                Toast.makeText(context, if (saved) BlockStrings.imageSaved else BlockStrings.saveFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(request) {
        val asked = request?.takeIf { it.blockId == block.id } ?: return@LaunchedEffect
        viewModel.consumeMediaRequest()
        when (asked.kind) {
            MediaRequestKind.PICK -> if (!locked) pick()
            MediaRequestKind.VIEW -> if (attrs.file != null) viewing = true
            MediaRequestKind.DOWNLOAD -> download()
            MediaRequestKind.OPEN_ELSEWHERE -> attrs.file?.let {
                BlockFiles.openWithAnotherApp(context, store, it, BlockFiles.imageMime(store, it))
            }
        }
    }

    val file = attrs.file
    if (file == null) {
        MediaPlaceholder(
            icon = Icons.Outlined.Image,
            text = BlockStrings.addImage,
            busy = busy,
            onClick = if (locked) null else pick,
            onLongPress = onLongPress,
            modifier = modifier
        )
    } else {
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val fullPx = with(density) { maxWidth.toPx() }
            // **Uno stato solo, aggiornato.** Rifatto a ogni larghezza salvata,
            // le maniglie — che si tengono il primo — avrebbero scritto in
            // quello vecchio, e dal secondo trascinamento non si muoveva più.
            var fraction by remember(block.id) { mutableFloatStateOf(attrs.width ?: 1f) }
            LaunchedEffect(attrs.width) { fraction = attrs.width ?: 1f }
            Box(modifier = Modifier.width(maxWidth * fraction)) {
                WholeImage(
                    fileName = file,
                    store = store,
                    description = attrs.name ?: EditorStrings.blockType("Image"),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .combinedClickable(onClick = { viewing = true }, onLongClick = onLongPress)
                )
                if (!locked) {
                    // Le due maniglie: l'immagine resta al centro, quindi
                    // ognuna sposta tutti e due i lati.
                    listOf(Alignment.CenterStart to -1f, Alignment.CenterEnd to 1f).forEach { (side, sign) ->
                        Box(
                            modifier = Modifier
                                .align(side)
                                .width(28.dp)
                                .height(64.dp)
                                .semantics { contentDescription = BlockStrings.resizeImage }
                                .pointerInput(fullPx) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = { viewModel.updateBlockAttrs(block.id) { it.copy(width = fraction) } }
                                    ) { change, dx ->
                                        change.consume()
                                        val widthPx = fraction * fullPx + 2 * dx * sign
                                        fraction = (widthPx / fullPx).coerceIn(MIN_IMAGE_WIDTH, 1f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
    }

    if (viewing && file != null) {
        val size = remember(file) { store.imageSize(file) }
        if (size != null && isTallImage(size.first, size.second)) {
            TallImageViewer(
                file = store.fileFor(file),
                imageWidth = size.first,
                imageHeight = size.second,
                title = attrs.name ?: EditorStrings.blockType("Image"),
                onDismiss = { viewing = false },
                onDownload = download
            )
        } else {
            FullScreenImage(
                fileName = file,
                store = store,
                title = attrs.name ?: EditorStrings.blockType("Image"),
                onDismiss = { viewing = false },
                onDownload = download
            )
        }
    }
}
