package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageLine
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite

/**
 * **"Move to line…"** (dal 20/10/2026, chiesto dall'utente: "concedimi di
 * spostare le immagini dove voglio"): le righe della pagina nell'ordine in
 * cui si leggono, rientrate come lì; toccandone una il blocco va subito sotto
 * di lei. In cima, "Top of the page". "Move up" e "Move down" lo spostano di
 * un posto alla volta, qui si va dove si vuole con un tocco.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoveToLineSheet(
    title: String,
    lines: List<PageLine>,
    /** Come si chiama una riga nell'elenco: il suo testo, o il suo tipo se non ne ha. */
    labelOf: (PageLine) -> String,
    /** La riga sotto cui mettere il blocco, o null per la cima della pagina. */
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = EditorStrings.moveToLineHint,
            style = MaterialTheme.typography.bodySmall,
            color = NotionGray400,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 8.dp)
                .navigationBarsPadding()
        ) {
            item(key = "top") {
                LineRow(level = 0, onClick = { onPick(null) }) {
                    Icon(Icons.Filled.VerticalAlignTop, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(EditorStrings.topOfPage, color = NotionWhite, fontWeight = FontWeight.SemiBold)
                }
            }
            items(lines, key = { it.block.id }) { line ->
                LineRow(level = line.depth, onClick = { onPick(line.block.id) }) {
                    Text(
                        text = labelOf(line),
                        color = NotionWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LineRow(level: Int, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(start = (8 + 16 * level.coerceAtMost(5)).dp, end = 8.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
