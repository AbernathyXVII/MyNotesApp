package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * La cronologia delle modifiche di una pagina: la voce "Updates" del
 * menu dei tre puntini.
 *
 * Una riga per modifica, dalla più recente: data e ora, poi cosa c'era
 * scritto **sbarrato** e cosa c'è scritto adesso, separati da una
 * freccia. È la forma chiesta:
 *
 *     22.10.2026 19:41 = ~~Esmepoi~~ → Esempio
 *
 * Le modifiche le registra `PageEditorViewModel` una per sessione di
 * scrittura, non una per tasto premuto: vedi `PageEditEntity`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    pageId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit
) {
    // Lo stesso ViewModel dell'editor, ma con una chiave sua: così
    // legge la pagina e la sua cronologia senza rubare lo stato (fuoco,
    // annulla) a quello che sta scrivendo di là.
    val viewModel: PageEditorViewModel = viewModel(factory = factory, key = "updates-$pageId")
    LaunchedEffect(pageId) { viewModel.load(pageId) }

    val edits by viewModel.edits.collectAsStateWithLifecycle()
    val page by viewModel.page.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.updates) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {
            if (edits.isEmpty()) {
                Text(
                    text = EditorStrings.noChangesYet(page?.title.orEmpty().ifBlank { EditorStrings.thisPage }),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotionGray400,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(edits, key = { it.id }) { edit ->
                        UpdateRow(edit)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateRow(edit: PageEditEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = formatEditMoment(edit.at),
            style = MaterialTheme.typography.labelMedium,
            color = NotionGray400
        )
        Text(
            text = editSummary(edit),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (edit.blockId == PageEditEntity.TITLE_BLOCK_ID) {
            Text(
                text = EditorStrings.editTitle,
                style = MaterialTheme.typography.labelSmall,
                color = NotionGray400,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Il prima sbarrato, la freccia, il dopo.
 *
 * Quando uno dei due è vuoto si scrive cosa è successo invece di
 * lasciare il vuoto: una riga con la freccia e niente da una parte non
 * si capirebbe.
 */
private fun editSummary(edit: PageEditEntity): AnnotatedString = buildAnnotatedString {
    when {
        edit.before.isBlank() -> {
            withStyle(SpanStyle(color = NotionGray400)) { append(EditorStrings.editAdded + "  ") }
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(edit.after) }
        }
        edit.after.isBlank() -> {
            withStyle(SpanStyle(color = NotionGray400)) { append(EditorStrings.editDeleted + "  ") }
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(edit.before) }
        }
        else -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = NotionGray400)) {
                append(edit.before)
            }
            append("  →  ")
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(edit.after) }
        }
    }
}

/** `22.10.2026 19:41` come chiesto all'inizio: oggi nel formato scelto nelle impostazioni. */
private fun formatEditMoment(at: Long): String =
    Formats.dateTime(at)
