package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.repository.PageRepository.PageTreeNode
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotebookBand
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.theme.SidebarBackground

/**
 * Cosa fare quando si tocca una voce della barra laterale. Un oggetto solo
 * invece di dieci parametri in fila: chi apre la barra (la navigazione)
 * li passa tutti insieme, e un ordine sbagliato fra dieci lambda uguali
 * non lo segnalerebbe nessuno.
 */
internal class SidebarActions(
    val onMainMenu: () -> Unit,
    val onFavorites: () -> Unit,
    val onOpenNode: (PageTreeNode) -> Unit,
    val onToggleNode: (PageTreeNode) -> Unit,
    val onSearch: () -> Unit,
    val onStartup: () -> Unit,
    val onTrash: () -> Unit,
    val onSettings: () -> Unit
)

/**
 * La barra laterale, nell'ordine chiesto: i widget, il Quaderno a parte,
 * il menu principale, i preferiti, l'albero delle pagine e poi le voci
 * di servizio. Scorre tutta insieme: con l'albero aperto le voci sotto
 * scendono, come nella barra di Notion.
 *
 * Le voci ancora da costruire ci sono già, grigie e ferme: dicono cosa
 * arriverà senza far finta di funzionare.
 */
@Composable
internal fun SidebarContent(
    root: PageTreeNode?,
    children: Map<String, List<PageTreeNode>>,
    expanded: Set<String>,
    currentPageId: String?,
    actions: SidebarActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SidebarBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // 1. Widget: per ora solo il titolo e il suo spazio.
        SidebarSectionTitle(Strings.widgets)
        Spacer(modifier = Modifier.height(WIDGETS_SPACE))

        // 2. Il Quaderno: una fascia di un altro colore, staccata dal resto.
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NotebookBand)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = DisabledTint, modifier = Modifier.size(ITEM_ICON))
            Spacer(modifier = Modifier.width(14.dp))
            Text(Strings.notebook, color = DisabledTint, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 3. Menu principale, e sotto un divisore semplice.
        SidebarItem(Icons.Filled.Home, Strings.mainMenu, onClick = actions.onMainMenu)
        HorizontalDivider(
            color = NotionGray900,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // 4. Pagine preferite.
        SidebarItem(Icons.Filled.Star, Strings.favoritePages, onClick = actions.onFavorites)

        // 5. L'albero delle pagine, a partire da quella principale.
        SidebarSectionTitle(Strings.pages)
        if (root != null) {
            flattenTree(root, children, expanded).forEach { row ->
                TreeRow(
                    row = row,
                    isCurrent = row.node.pageId != null && row.node.pageId == currentPageId,
                    isExpanded = row.node.pageId in expanded,
                    onOpen = { actions.onOpenNode(row.node) },
                    onToggle = { actions.onToggleNode(row.node) }
                )
                val id = row.node.pageId
                if (id != null && id in expanded && row.node.hasChildren && children[id]?.isEmpty() == true) {
                    Text(
                        Strings.noPagesInside,
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
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 6–14. Le voci di servizio.
        SidebarItem(Icons.Filled.Search, Strings.search, onClick = actions.onSearch)
        SidebarItem(Icons.Filled.RocketLaunch, Strings.startupWindow, onClick = actions.onStartup)
        SidebarItem(Icons.Filled.Backup, Strings.backup, enabled = false)
        SidebarItem(Icons.Filled.Notifications, Strings.notifications, enabled = false)
        SidebarItem(Icons.Filled.Delete, Strings.trash, onClick = actions.onTrash)
        SidebarItem(Icons.Filled.FileDownload, Strings.import, enabled = false)
        SidebarItem(Icons.Filled.FileUpload, Strings.export, enabled = false)
        SidebarItem(Icons.Filled.Devices, Strings.connections, enabled = false)
        SidebarItem(Icons.Filled.Settings, Strings.settings, onClick = actions.onSettings)
    }
}

private val WIDGETS_SPACE = 72.dp
private val ITEM_ICON = 22.dp
internal val TREE_START = 12.dp
internal val TREE_INDENT = 16.dp
internal val TREE_ARROW = 24.dp

/** Il grigio delle voci spente: lo stesso ovunque nella barra. */
private val DisabledTint: Color get() = NotionGray400.copy(alpha = 0.55f)

@Composable
private fun SidebarSectionTitle(text: String) {
    Text(
        text = text,
        color = NotionGray400,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val tint = if (enabled) NotionWhite else DisabledTint
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(ITEM_ICON))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Una riga dell'albero già messa in fila, con quanto è rientrata.
 *
 * Questa, `flattenTree` e `TreeRow` servono anche alla scelta della pagina
 * in cui duplicarne un'altra (`DuplicateSheet`): lo stesso albero, così
 * le pagine si ritrovano dove si è abituati a vederle.
 */
internal class FlatTreeRow(val node: PageTreeNode, val depth: Int)

/**
 * L'albero messo in fila, rami aperti compresi.
 *
 * Una pagina che richiama una delle pagine sopra di lei (succede: basta
 * un collegamento all'indietro) si mostra ma non si riapre, altrimenti la
 * fila non finirebbe mai.
 */
internal fun flattenTree(
    root: PageTreeNode,
    children: Map<String, List<PageTreeNode>>,
    expanded: Set<String>
): List<FlatTreeRow> {
    val rows = mutableListOf<FlatTreeRow>()
    fun visit(node: PageTreeNode, depth: Int, ancestors: Set<String>) {
        rows += FlatTreeRow(node, depth)
        val id = node.pageId ?: return
        if (id !in expanded || id in ancestors) return
        children[id]?.forEach { visit(it, depth + 1, ancestors + id) }
    }
    visit(root, 0, emptySet())
    return rows
}

/**
 * Una riga dell'albero: il triangolino (solo se dentro c'è qualcosa, come
 * nelle cartelle di Esplora file), l'icona della pagina e il titolo.
 * Il triangolino apre e chiude; il resto della riga porta alla pagina.
 */
@Composable
internal fun TreeRow(
    row: FlatTreeRow,
    isCurrent: Boolean,
    isExpanded: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    val node = row.node
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent) DarkSurfaceVariant else Color.Transparent)
            .clickable(onClick = onOpen)
            .padding(start = TREE_START - 8.dp + TREE_INDENT * row.depth, end = 8.dp)
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(TREE_ARROW)
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = node.hasChildren, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (node.hasChildren) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowRight,
                    contentDescription = if (isExpanded) Strings.collapse else Strings.expand,
                    tint = NotionGray400
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        PageIconView(emoji = node.emoji, iconImage = node.iconImage, isDatabase = node.isDatabase, size = 18.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.title.ifBlank { Strings.untitled },
            color = NotionWhite,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Serve ai `WindowInsets` vuoti del foglio della barra: i margini li mette già il contenuto. */
internal val NoInsets = WindowInsets(0, 0, 0, 0)
