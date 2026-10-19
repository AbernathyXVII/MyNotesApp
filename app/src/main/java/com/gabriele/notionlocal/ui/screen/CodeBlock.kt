package com.gabriele.notionlocal.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.theme.CodeBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel

/**
 * L'a-capo nascosto in testa al campo, come nelle altre righe (vedi
 * `RUN_LEAD`): è l'unico modo di accorgersi del backspace a inizio codice.
 */
private const val CODE_LEAD = "\n"

/** Il testo mostrato senza l'a-capo nascosto, coi colori della lingua. */
private class CodeTransformation(private val language: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val code = text.text.removePrefix(CODE_LEAD)
        return TransformedText(highlightCode(code, language), CodeLeadMapping)
    }
}

private object CodeLeadMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = (offset - CODE_LEAD.length).coerceAtLeast(0)
    override fun transformedToOriginal(offset: Int): Int = offset + CODE_LEAD.length
}

/**
 * **Il blocco "Code"** (dal 06/10/2026, chiesto dall'utente): un riquadro col
 * carattere a larghezza fissa, il linguaggio in alto a sinistra — toccandolo si
 * sceglie fra i più usati — e il pulsante per copiare tutto il codice a
 * destra. Il codice si colora secondo il linguaggio (`highlightCode`).
 *
 * L'Invio va sempre a capo dentro il codice. La tastiera non mette
 * maiuscole né corregge le parole: nel codice farebbe solo danni. Backspace
 * a inizio di un codice vuoto lo fa tornare una riga di testo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CodeBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    focusRequestBlockId: String?,
    onFocusChanged: (String, Boolean) -> Unit,
    /** Pagina bloccata (o nel cestino): il codice si legge e si copia, non si scrive. */
    locked: Boolean,
    /** Il blocco è diventato una riga di testo e questo campo sta per sparire: il fuoco va posato altrove. */
    onUnmade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val language = block.attrs.language ?: DEFAULT_CODE_LANGUAGE
    val stored = viewModel.plainTextOf(block)
    var value by remember(block.id) {
        mutableStateOf(TextFieldValue(CODE_LEAD + stored, TextRange(CODE_LEAD.length + stored.length)))
    }
    var focused by remember(block.id) { mutableStateOf(false) }
    var choosingLanguage by remember(block.id) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val transformation = remember(language) { CodeTransformation(language) }
    // **Le righe intere** (dal 19/10/2026, "Full line view"): il codice scorre
    // di lato con il testo della pagina, e scrivendo il cursore resta in vista.
    val sideLane = rememberSideLane()
    val caretIntoView = remember { BringIntoViewRequester() }
    var codeLayout by remember(block.id) { mutableStateOf<TextLayoutResult?>(null) }
    LaunchedEffect(value.selection, codeLayout, focused) {
        if (sideLane == null || !focused) return@LaunchedEffect
        val layout = codeLayout ?: return@LaunchedEffect
        val at = (value.selection.end - CODE_LEAD.length).coerceIn(0, layout.layoutInput.text.length)
        caretIntoView.bringIntoView(layout.getCursorRect(at))
    }

    // Cambiato da fuori (Annulla, "Turn into"): il campo lo rilegge, ma solo
    // mentre non ci si scrive dentro.
    LaunchedEffect(stored, focused) {
        if (!focused && value.text.removePrefix(CODE_LEAD) != stored) {
            value = TextFieldValue(CODE_LEAD + stored, TextRange(CODE_LEAD.length + stored.length))
        }
    }
    LaunchedEffect(focusRequestBlockId) {
        if (focusRequestBlockId == block.id) {
            focusRequester.requestFocus()
            keyboard?.show()
            viewModel.consumeFocusRequest()
        }
    }

    Column(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CodeBackground)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = !locked) { choosingLanguage = true }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            ) {
                Text(language, style = MaterialTheme.typography.labelMedium, color = NotionGray400)
                if (!locked) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(value.text.removePrefix(CODE_LEAD)))
                    Toast.makeText(context, BlockStrings.codeCopied, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = BlockStrings.copyCode, tint = NotionGray400, modifier = Modifier.size(18.dp))
            }
        }
        AutoCaps {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    val oldCode = value.text.removePrefix(CODE_LEAD)
                    if (!new.text.startsWith(CODE_LEAD)) {
                        // Backspace a inizio codice: se è vuoto torna testo,
                        // se no non succede niente.
                        if (oldCode.isEmpty() && viewModel.backspaceAtIslandStart(block)) onUnmade()
                        value = TextFieldValue(CODE_LEAD + oldCode, TextRange(CODE_LEAD.length))
                        return@BasicTextField
                    }
                    // Il cursore non va prima dell'a-capo nascosto.
                    val selection = TextRange(
                        new.selection.start.coerceAtLeast(CODE_LEAD.length),
                        new.selection.end.coerceAtLeast(CODE_LEAD.length)
                    )
                    value = new.copy(selection = selection)
                    val newCode = new.text.removePrefix(CODE_LEAD)
                    if (newCode != oldCode) viewModel.updateBlockSpans(block, listOf(RichTextSpan(text = newCode)))
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = NotionWhite
                ),
                cursorBrush = SolidColor(NotionWhite),
                readOnly = locked,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Ascii
                ),
                visualTransformation = transformation,
                onTextLayout = { codeLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 21.dp)
                    .padding(end = 8.dp)
                    .sideLane(sideLane)
                    .bringIntoViewRequester(caretIntoView)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        onFocusChanged(block.id, it.isFocused)
                    }
                    .markFocusChanges()
            )
        }
    }

    if (choosingLanguage) {
        CodeLanguageSheet(
            current = language,
            onPick = { picked -> viewModel.updateBlockAttrs(block.id) { it.copy(language = picked) } },
            onDismiss = { choosingLanguage = false }
        )
    }
}

/** **Scegliere il linguaggio** del codice: l'elenco con la ricerca in cima, come su Notion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeLanguageSheet(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query) { CODE_LANGUAGES.filter { it.contains(query.trim(), ignoreCase = true) } }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
            AutoCaps {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(BlockStrings.searchLanguage) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.Top) {
                items(shown, key = { it }) { name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onPick(name)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyLarge, color = NotionWhite, modifier = Modifier.weight(1f))
                        if (name == current) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
