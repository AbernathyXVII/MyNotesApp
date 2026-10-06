package com.gabriele.notionlocal.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.DEFAULT_CALLOUT_ICON
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.launch

/**
 * Le emoji pronte per l'icona di un riquadro: la famiglia "Callout" del
 * selettore di Notion, nello stesso ordine (seconda foto dell'utente, 06/10/2026).
 */
internal val CALLOUT_EMOJIS = listOf(
    "💡", "👉", "☝️", "👌", "🔑", "🚧", "⚠️", "🔥", "📌", "✂️", "❓", "🚫", "⛔",
    "⏰", "☎️", "🚨", "♻️", "✅", "🔒", "📎", "📖", "🗣️", "➡️", "📢", "🛠️", "⚙️"
)

/** Se il riquadro ha un'icona da mostrare: "Remove" la toglie del tutto. */
internal val BlockAttrs.hasCalloutIcon: Boolean get() = iconImage != null || icon != ""

/**
 * **L'icona di un riquadro**: l'immagine caricata, o l'emoji (la lampadina,
 * se non se n'è scelta una). Toccandola si cambia.
 */
@Composable
internal fun CalloutIcon(
    attrs: BlockAttrs,
    store: PageImageStore,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        val image = attrs.iconImage
        if (image != null) {
            PageImage(
                fileName = image,
                store = store,
                contentDescription = EditorStrings.icon,
                modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
            )
        } else {
            Text(attrs.icon ?: DEFAULT_CALLOUT_ICON, fontSize = 17.sp)
        }
    }
}

/**
 * **Scegliere l'icona di un riquadro** (dal 06/10/2026): le emoji della
 * famiglia "Callout" di Notion, un'emoji qualsiasi scritta con la tastiera,
 * o un'immagine caricata dal telefono come per le icone delle pagine.
 * "Remove" lascia il riquadro senza icona.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalloutIconSheet(
    store: PageImageStore,
    onEmoji: (String) -> Unit,
    onImage: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var typed by remember { mutableStateOf("") }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        error = null
        scope.launch {
            val result = store.saveFromGallery(uri)
            busy = false
            result.fold(
                onSuccess = {
                    onImage(it)
                    onDismiss()
                },
                onFailure = { error = EditorStrings.imageReadFailed }
            )
        }
    }

    NoteBottomSheet(
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
            Text(
                text = EditorStrings.icon,
                style = MaterialTheme.typography.titleMedium,
                color = NotionWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(BlockStrings.calloutEmojis, style = MaterialTheme.typography.labelMedium, color = NotionGray400)
            Spacer(modifier = Modifier.size(8.dp))
            CALLOUT_EMOJIS.chunked(EMOJIS_PER_ROW).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !busy) {
                                    onEmoji(emoji)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                    // L'ultima fila, più corta, tiene le colonne delle altre.
                    repeat(EMOJIS_PER_ROW - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            // Qualsiasi altra emoji, presa dalla tastiera.
            Spacer(modifier = Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AutoCaps {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(MAX_TYPED_EMOJI) },
                    singleLine = true,
                    placeholder = { Text(BlockStrings.otherEmoji) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (typed.isNotBlank()) {
                            onEmoji(typed.trim())
                            onDismiss()
                        }
                    }),
                    modifier = Modifier.weight(1f)
                )
                }
                TextButton(
                    enabled = typed.isNotBlank() && !busy,
                    onClick = {
                        onEmoji(typed.trim())
                        onDismiss()
                    }
                ) { Text(BlockStrings.useEmoji) }
            }

            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = EditorStrings.upload,
                style = MaterialTheme.typography.labelLarge,
                color = NotionWhite,
                modifier = Modifier
                    .background(DarkSurface, RoundedCornerShape(percent = 50))
                    .clickable(enabled = !busy) {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
            error?.let {
                Spacer(modifier = Modifier.size(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = EditorStrings.remove,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !busy) {
                        onRemove()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp)
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

private const val EMOJIS_PER_ROW = 7
private const val MAX_TYPED_EMOJI = 16
