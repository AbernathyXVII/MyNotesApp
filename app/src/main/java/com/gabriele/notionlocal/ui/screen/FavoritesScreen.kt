package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.FavoritesSort
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.FavoritesViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * Le pagine preferite, una sotto l'altra, con quando sono state aggiunte.
 *
 * L'ordine si sceglie in cima — per data di aggiunta o per nome, nei due
 * versi — e resta salvato: riaprendo l'elenco lo si ritrova come lo si
 * era lasciato. Le pagine aggiunte prima che la data si salvasse non
 * hanno una data vera: in ordine di data vengono per prime, come le più
 * vecchie, ed è onesto — lo sono.
 */
@Composable
fun FavoritesScreen(
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenPage: (PageEntity) -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = factory)
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val sort = AppSettings.favoritesSort
    val descending = AppSettings.favoritesDescending

    val sorted = remember(favorites, sort, descending) {
        val ascending = when (sort) {
            FavoritesSort.DATE_ADDED -> favorites.sortedWith(
                compareBy<PageEntity> { it.favoritedAt ?: Long.MIN_VALUE }.thenBy { it.title.lowercase() }
            )
            FavoritesSort.ALPHABETICAL -> favorites.sortedBy { it.title.lowercase() }
        }
        if (descending) ascending.reversed() else ascending
    }

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.favoritePages, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Strings.sortBy, color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
                SortChip(Strings.dateAdded, sort == FavoritesSort.DATE_ADDED) {
                    AppSettings.favoritesSort = FavoritesSort.DATE_ADDED
                }
                SortChip(Strings.alphabetical, sort == FavoritesSort.ALPHABETICAL) {
                    AppSettings.favoritesSort = FavoritesSort.ALPHABETICAL
                }
                // Il verso è un interruttore solo: toccato, si gira.
                FilterChip(
                    selected = true,
                    onClick = { AppSettings.favoritesDescending = !descending },
                    label = { Text(if (descending) Strings.descending else Strings.ascending) },
                    leadingIcon = {
                        Icon(
                            if (descending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = chipColors()
                )
            }

            if (sorted.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        Strings.noFavorites,
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sorted, key = { it.id }) { page ->
                        FavoriteRow(page = page, onClick = { onOpenPage(page) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = chipColors()
    )
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = DarkBackground,
    labelColor = NotionGray400,
    iconColor = NotionGray400,
    selectedContainerColor = DarkSurfaceVariant,
    selectedLabelColor = NotionWhite,
    selectedLeadingIconColor = NotionWhite
)

@Composable
private fun FavoriteRow(page: PageEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = page.isDatabase, size = 24.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                page.title.ifBlank { Strings.untitled },
                color = NotionWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                page.favoritedAt?.let { Strings.addedOn(Formats.dateTime(it)) } ?: Strings.dateAddedUnknown,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
