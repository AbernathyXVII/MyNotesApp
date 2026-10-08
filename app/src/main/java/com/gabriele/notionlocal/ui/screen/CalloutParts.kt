package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.DEFAULT_CALLOUT_ICON
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings

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
 * **Scegliere l'icona di un riquadro**: il selettore delle icone di tutta
 * l'app (dal 07/10/2026, `IconPickerSheet`), con in cima la famiglia
 * "Callout" di Notion che c'era dal 06/10/2026. "Remove" lascia il riquadro
 * senza icona.
 *
 * L'immagine di prima non si cancella: la freccia indietro del riquadro la
 * rimette, e deve ritrovarla.
 */
@Composable
internal fun CalloutIconSheet(
    store: PageImageStore,
    hasIcon: Boolean,
    onEmoji: (String) -> Unit,
    onImage: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    IconPickerSheet(
        store = store,
        hasIcon = hasIcon,
        onPick = { choice ->
            when (choice) {
                is IconChoice.Emoji -> onEmoji(choice.emoji)
                is IconChoice.Image -> onImage(choice.fileName)
                IconChoice.None -> onRemove()
            }
            null
        },
        onDismiss = onDismiss,
        suggestionsTitle = BlockStrings.calloutEmojis,
        suggestions = CALLOUT_EMOJIS
    )
}
