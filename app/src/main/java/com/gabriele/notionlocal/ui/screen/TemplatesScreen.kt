package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.gabriele.notionlocal.ui.theme.DarkSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.TemplateStrings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.TemplatesViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * **I template** (dal 26/09/2026, la voce "Templates" della barra laterale):
 * l'elenco di quelli creati, e "New template" per farne uno. Toccato, un
 * template si apre e si scrive come una pagina qualsiasi; i tre puntini lo
 * mandano nel cestino, da cui si recupera.
 *
 * Sono gli stessi che si vedono in una pagina nuova di un database ("Add
 * existing template"), e un template creato da lì compare anche qui: sono
 * uno solo per tutti i database.
 */
@Composable
fun TemplatesScreen(
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    viewModel: TemplatesViewModel = viewModel(factory = factory)
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { WorkspaceTopBar(title = TemplateStrings.templates, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item(key = "new-template") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.create(onOpenTemplate) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(TemplateStrings.newTemplate, color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
            }
            if (templates.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            TemplateStrings.noTemplates,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            TemplateStrings.noTemplatesHint,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(templates, key = { it.id }) { template ->
                    TemplateRow(
                        template = template,
                        onClick = { onOpenTemplate(template.id) },
                        onMoveToTrash = { viewModel.moveToTrash(template.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(template: PageEntity, onClick: () -> Unit, onMoveToTrash: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TemplateIcon(template, size = 24.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            template.title.ifBlank { TemplateStrings.untitledTemplate },
            color = if (template.title.isBlank()) NotionGray400 else NotionWhite,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = TemplateStrings.options, tint = NotionGray400)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(Strings.moveToTrash) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onMoveToTrash()
                    }
                )
            }
        }
    }
}

/**
 * L'icona di un template: la sua immagine, se gliene è stata data una, o il
 * foglio con le righe, come quello dei template di Notion. L'emoji di
 * riserva delle pagine (📄) non si mostra: non è stata scelta da nessuno.
 */
@Composable
internal fun TemplateIcon(template: PageEntity, size: androidx.compose.ui.unit.Dp) {
    if (template.iconImage != null || template.icon != "📄") {
        PageIconView(emoji = template.icon, iconImage = template.iconImage, isDatabase = false, size = size)
    } else {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(size))
    }
}

/**
 * **Le voci grigie di una pagina nuova di un database** (dal 26/09/2026,
 * come su Notion): "+ New template" e "Add existing template", sotto una
 * riga che spiega cosa fare. Grigie apposta, perché non sono testo della
 * pagina ma una proposta: se ne vanno toccando un punto qualsiasi della
 * pagina, o appena la pagina ha qualcosa scritto.
 */
@Composable
internal fun TemplatePicker(
    onNewTemplate: () -> Unit,
    onAddExisting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            TemplateStrings.pickerHint,
            color = NotionGray400,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.size(6.dp))
        TemplatePickerEntry(Icons.Filled.Add, TemplateStrings.newTemplate, onNewTemplate)
        TemplatePickerEntry(Icons.Outlined.Description, TemplateStrings.addExistingTemplate, onAddExisting)
    }
}

@Composable
private fun TemplatePickerEntry(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * "Add existing template": l'elenco dei template, per scriverne uno nella
 * pagina con un tocco. Senza template, lo dice e propone di crearne uno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplateChooserSheet(
    factory: ViewModelFactory,
    onDismiss: () -> Unit,
    onPick: (PageEntity) -> Unit,
    onNewTemplate: () -> Unit
) {
    val templates by remember(factory) { factory.pageRepository.observeTemplates() }
        .collectAsStateWithLifecycle(initialValue = null)

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                TemplateStrings.addExistingTemplate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val list = templates
            if (list != null && list.isEmpty()) {
                Text(
                    TemplateStrings.noTemplates,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    TemplateStrings.noTemplatesHint,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            list.orEmpty().forEach { template ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(template) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TemplateIcon(template, size = 22.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        template.title.ifBlank { TemplateStrings.untitledTemplate },
                        color = if (template.title.isBlank()) NotionGray400 else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNewTemplate)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(TemplateStrings.newTemplate, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
