package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository.LinkableDatabase
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite

/**
 * **"Linked view of data source"**: quale database mostrare nella vista
 * collegata. È l'elenco "Existing data sources" di Notion: tutti i
 * database dell'app, con sotto la pagina in cui sta ciascuno — due
 * database possono chiamarsi uguale — e un campo per cercarli per nome.
 *
 * `databases` è null finché l'elenco si sta leggendo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkedSourcePickerSheet(
    databases: List<LinkableDatabase>?,
    onDismiss: () -> Unit,
    onPick: (PageEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(PICKER_HEIGHT)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = DbStrings.linkedViewTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = DbStrings.linkedViewHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(Strings.search, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it.replace("\n", "") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                        cursorBrush = SolidColor(NotionWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))

            when {
                databases == null -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                databases.isEmpty() -> Text(
                    text = DbStrings.noDatabasesToLink,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                else -> {
                    val wanted = query.trim()
                    val shown = if (wanted.isEmpty()) {
                        databases
                    } else {
                        databases.filter { it.page.title.contains(wanted, ignoreCase = true) }
                    }
                    if (shown.isEmpty()) {
                        Text(
                            text = Strings.noResults,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(shown, key = { it.page.id }) { database ->
                            LinkableDatabaseRow(database, onClick = { onPick(database.page) })
                        }
                    }
                }
            }
        }
    }
}

/** Un database dell'elenco: icona, nome, e sotto dove sta. */
@Composable
private fun LinkableDatabaseRow(database: LinkableDatabase, onClick: () -> Unit) {
    val page = database.page
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = true, size = 20.dp)
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = page.title.ifBlank { Strings.untitled },
                color = NotionWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = database.place?.let { Strings.inPlace(it.ifBlank { Strings.untitled }) } ?: Strings.mainMenu,
                color = NotionGray400,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Quanto sale la finestra: quasi tutto lo schermo, perché i database possono essere tanti. */
private const val PICKER_HEIGHT = 0.9f
