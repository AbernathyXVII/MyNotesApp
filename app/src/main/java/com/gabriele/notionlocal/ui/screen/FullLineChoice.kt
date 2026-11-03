package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.FULL_LINE_DEFAULT
import com.gabriele.notionlocal.data.entity.FULL_LINE_OFF
import com.gabriele.notionlocal.data.entity.FULL_LINE_ON
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400

/**
 * **"Full or page line"** (dal 27/10/2026, chiesto dall'utente): ogni pagina
 * sceglie dai tre puntini come vanno le righe lunghe. "Default" fa come
 * Settings ("Full line view"); "Full line" le lascia intere, da scorrere di
 * lato (vedi `FullLineView.kt`); "Page line" le manda a capo dentro lo
 * schermo. Settings resta com'era, e vale per le pagine lasciate su Default.
 */

/** Se in [page] le righe lunghe restano intere: la scelta della pagina, o quella di Settings. */
internal fun fullLinesOf(page: PageEntity?): Boolean = when (page?.fullLineMode) {
    FULL_LINE_ON -> true
    FULL_LINE_OFF -> false
    else -> AppSettings.fullLineView
}

private fun lineName(full: Boolean) = if (full) Strings.fullLine else Strings.pageLine

/** Cosa dice la voce nei tre puntini: "Default (Full line)", "Full line" o "Page line". */
internal fun fullLineSummary(page: PageEntity): String {
    val name = lineName(fullLinesOf(page))
    return if (page.fullLineMode == FULL_LINE_DEFAULT) Strings.fullLineDefault(name) else name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageFullLineSheet(
    page: PageEntity,
    onSet: (mode: Int) -> Unit,
    onDismiss: () -> Unit
) {
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
            Text(Strings.fullOrPageLine, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                Strings.fullOrPageLineNote,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            OptionsGroup {
                RadioItem(
                    title = Strings.fullLineDefault(lineName(AppSettings.fullLineView)),
                    selected = page.fullLineMode == FULL_LINE_DEFAULT,
                    detail = Strings.fullLineDefaultDetail,
                    onClick = { onSet(FULL_LINE_DEFAULT) }
                )
                RadioItem(
                    title = Strings.fullLine,
                    selected = page.fullLineMode == FULL_LINE_ON,
                    detail = Strings.fullLineViewNote,
                    onClick = { onSet(FULL_LINE_ON) }
                )
                RadioItem(
                    title = Strings.pageLine,
                    selected = page.fullLineMode == FULL_LINE_OFF,
                    detail = Strings.pageLineDetail,
                    onClick = { onSet(FULL_LINE_OFF) }
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
