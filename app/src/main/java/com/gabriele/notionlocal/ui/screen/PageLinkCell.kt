package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * **"Page link"** (dal 29/09/2026): una proprietà che collega la riga a
 * un'altra pagina del Workspace — anche la pagina di un'altra riga dello
 * stesso database. Nella cella si vede la pagina (icona e titolo, sempre
 * quelli di adesso); toccandola la si apre. Toccando il resto della cella
 * si sceglie la pagina, dall'albero delle pagine o cercandola per nome, o
 * si toglie il collegamento. Il valore salvato è l'id della pagina.
 */

/** Cosa fa una cella "Page link": aprire la pagina, o sceglierne un'altra. */
@Stable
internal class PageLinkActions(
    val open: (String) -> Unit,
    /** La pagina di adesso (vuota se non c'è) e a chi dire quella scelta ("" = tolta). */
    val pick: (current: String, onPicked: (String) -> Unit) -> Unit
)

internal val LocalPageLinkActions = staticCompositionLocalOf<PageLinkActions?> { null }

/** Le pagine collegate dalle celle "Page link", per id: titolo e icona di adesso. */
internal val LocalLinkedPages = compositionLocalOf<Map<String, PageEntity>> { emptyMap() }

private class PageLinkRequest(val current: String, val onPicked: (String) -> Unit)

/**
 * Chi sa aprire le pagine e mostrare la scelta: la tabella e le proprietà
 * in cima alla pagina di una riga lo mettono intorno alle loro celle.
 */
@Composable
internal fun PageLinkHost(
    factory: ViewModelFactory,
    linkedPages: Map<String, PageEntity>,
    onOpenPage: (String) -> Unit,
    content: @Composable () -> Unit
) {
    var request by remember { mutableStateOf<PageLinkRequest?>(null) }
    val scope = rememberCoroutineScope()
    val open by rememberUpdatedState(onOpenPage)
    val actions = remember {
        PageLinkActions(
            open = { open(it) },
            pick = { current, onPicked -> request = PageLinkRequest(current, onPicked) }
        )
    }
    CompositionLocalProvider(
        LocalPageLinkActions provides actions,
        LocalLinkedPages provides linkedPages,
        content = content
    )
    request?.let { asked ->
        DestinationPickerSheet(
            factory = factory,
            title = DbStrings.typeName(com.gabriele.notionlocal.data.entity.ColumnType.PAGE_LINK),
            hint = DbStrings.pageLinkHint,
            viewModelKey = "page-link-destination",
            onDismiss = { request = null },
            onPick = { node ->
                request = null
                scope.launch {
                    // Una riga mai aperta si sceglie lo stesso: la sua pagina nasce adesso.
                    val pageId = node.pageId ?: node.rowId?.let { factory.pageRepository.ensureRowPage(it) }
                    if (pageId != null) asked.onPicked(pageId)
                }
            },
            onClear = if (asked.current.isNotEmpty()) {
                {
                    request = null
                    asked.onPicked("")
                }
            } else {
                null
            },
            clearLabel = DbStrings.removeLink
        )
    }
}

/**
 * La cella: la pagina collegata (icona e titolo sottolineato) da toccare
 * per aprirla; il resto della cella apre la scelta. `contentPadding` sta
 * dentro la zona toccabile, come per le date.
 */
@Composable
internal fun PageLinkCell(
    value: String,
    onValueChange: (String) -> Unit,
    contentPadding: Dp,
    alignment: Alignment = Alignment.CenterStart
) {
    val actions = LocalPageLinkActions.current
    val locked = LocalDatabaseLocked.current
    val page = value.takeIf { it.isNotEmpty() }?.let { LocalLinkedPages.current[it] }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(enabled = !locked && actions != null) { actions?.let { it.pick(value) { picked -> onValueChange(picked) } } }
            .padding(horizontal = contentPadding)
            .cellWrapPadding(),
        contentAlignment = alignment
    ) {
        when {
            value.isEmpty() -> Unit
            page == null -> Text(
                DbStrings.pageNotFound,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = cellMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            else -> Row(
                modifier = Modifier.clickable(enabled = actions != null) { actions?.let { it.open(page.id) } },
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = page.isDatabase, size = 16.dp)
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    page.title.ifBlank { Strings.untitled },
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                    color = if (page.trashedAt != null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    maxLines = cellMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
