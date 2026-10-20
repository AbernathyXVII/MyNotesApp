package com.gabriele.notionlocal.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.gabriele.notionlocal.data.entity.LinkTarget
import com.gabriele.notionlocal.data.entity.blockLink
import com.gabriele.notionlocal.data.entity.blockLinkTarget
import com.gabriele.notionlocal.ui.theme.NotionGray400
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.theme.TextLinkColor
import kotlinx.coroutines.withTimeoutOrNull

// ---------------------------------------------------------------------------
// **I collegamenti sul testo** (dal 17/10/2026, chiesto dall'utente): si
// seleziona del testo, si tocca l'icona del collegamento nella barra sopra
// la tastiera e si scrive l'indirizzo. Il testo diventa blu e sottolineato,
// e toccandolo si apre il link. L'indirizzo sta nel pezzo di testo
// (`RichTextSpan.link`).
// ---------------------------------------------------------------------------

/** L'etichetta con cui il testo mostrato porta l'indirizzo, per sapere cosa si è toccato. */
internal const val LINK_TAG = "text-link"

/** Il colore del testo con un collegamento; va messo **prima** del colore scelto col pennello, che vince. */
internal fun linkStyleOf(span: RichTextSpan): SpanStyle? =
    if (span.link != null) SpanStyle(color = TextLinkColor) else null

/** Segna l'indirizzo sul testo mostrato da [start] a [end], per `linkTaps`. */
internal fun AnnotatedString.Builder.markLink(span: RichTextSpan, start: Int, end: Int) {
    val link = span.link ?: return
    if (start < end) addStringAnnotation(LINK_TAG, link, start, end)
}

/**
 * L'indirizzo del testo sotto il dito, o null. Conta solo la lettera
 * davvero toccata: un tocco nello spazio vuoto dopo la fine della riga
 * non apre il link che la chiude.
 */
internal fun linkAt(layout: TextLayoutResult, position: Offset): String? {
    val text = layout.layoutInput.text
    // Si guardano le lettere dei link, una per una: sono poche, e il
    // riquadro di ogni lettera dice proprio se il dito ci è caduto sopra.
    for (mark in text.getStringAnnotations(LINK_TAG, 0, text.length)) {
        for (index in mark.start until mark.end.coerceAtMost(text.length)) {
            if (layout.getBoundingBox(index).contains(position)) return mark.item
        }
    }
    return null
}

/**
 * **Un tocco breve su un testo con un collegamento lo apre**, e il campo
 * sotto non lo riceve: niente cursore né tastiera, come un link in una
 * pagina web. Tenendo premuto, invece, il dito arriva al campo come sempre
 * e seleziona il testo (per cambiare o togliere il link).
 */
internal fun Modifier.linkTaps(
    layout: () -> TextLayoutResult?,
    onLink: (String) -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (linkAt(layout() ?: return@awaitEachGesture, down.position) == null) return@awaitEachGesture
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation(PointerEventPass.Initial)
        } ?: return@awaitEachGesture
        if ((up.position - down.position).getDistance() > viewConfiguration.touchSlop) return@awaitEachGesture
        val link = linkAt(layout() ?: return@awaitEachGesture, up.position) ?: return@awaitEachGesture
        // Preso qui, il tocco non arriva al campo: niente cursore.
        up.consume()
        onLink(link)
    }
}

/**
 * Apre un collegamento; se non si può, lo dice. Le pagine web si aprono nel
 * browser predefinito: vedi `BlockFiles.openUrl`. Un collegamento a una riga
 * della pagina (dal 20/10/2026) porta lì: vedi `LocalBlockLinkJump`.
 */
@Composable
internal fun rememberLinkOpener(): (String) -> Unit {
    val context = LocalContext.current
    val jump = LocalBlockLinkJump.current
    return remember(context, jump) {
        { url ->
            val target = blockLinkTarget(url)
            if (target != null && jump != null) {
                jump(target)
            } else if (!BlockFiles.openUrl(context, url)) {
                Toast.makeText(context, EditorStrings.cantOpenLink, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private val EMAIL = Regex("""^[^@\s/:]+@[^@\s/]+\.[^@\s/]+$""")

/**
 * L'indirizzo scritto dall'utente, pronto da aprire: "google.com" diventa
 * "https://google.com", un indirizzo email diventa "mailto:…"; `mailto:` e
 * `tel:` scritti per intero restano come sono. Null se non sembra un link.
 */
internal fun textLinkTarget(input: String): String? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val lower = text.lowercase()
    if (lower.startsWith("mailto:") || lower.startsWith("tel:")) {
        return text.takeIf { it.substringAfter(':').isNotEmpty() }
    }
    if (EMAIL.matches(text)) return "mailto:$text"
    return normalizedLink(text)
}

/**
 * La finestrella del collegamento: l'indirizzo da scrivere o incollare, e
 * "Remove link" se il testo ne ha già uno. [current] è quello che c'è già.
 *
 * **"In this page"** (dal 20/10/2026): la seconda scheda elenca le righe della
 * pagina a cui ci si può collegare ([targets], vedi `linkTargetsOf`); un
 * tocco su una di loro fa il collegamento.
 */
@Composable
internal fun LinkDialog(
    current: String?,
    targets: List<LinkTarget>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRemove: () -> Unit
) {
    val currentTarget = blockLinkTarget(current)
    var inPage by remember { mutableStateOf(currentTarget != null) }
    val start = if (currentTarget != null) "" else current.orEmpty()
    var field by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    var wrong by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val submit = {
        val url = textLinkTarget(field.text)
        if (url == null) wrong = true else onSave(url)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(EditorStrings.link, color = NotionWhite) },
        text = {
            Column {
                TabRow(selectedTabIndex = if (inPage) 1 else 0, containerColor = DarkSheet) {
                    Tab(selected = !inPage, onClick = { inPage = false }, text = { Text(EditorStrings.linkWeb) }, unselectedContentColor = NotionGray400)
                    Tab(selected = inPage, onClick = { inPage = true }, text = { Text(EditorStrings.linkInThisPage) }, unselectedContentColor = NotionGray400)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (inPage) {
                    LinkTargetList(targets, currentTarget) { onSave(blockLink(it)) }
                } else {
                // Un campo per indirizzi: `AutoCaps` lo lascia com'è, niente maiuscola.
                AutoCaps {
                    OutlinedTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            wrong = false
                        },
                        singleLine = true,
                        isError = wrong,
                        placeholder = { Text(EditorStrings.linkHint) },
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                    )
                }
                // Qui, accanto al campo: chiesto prima che esista, il fuoco non ha dove andare.
                LaunchedEffect(Unit) { focus.requestFocus() }
                if (wrong) {
                    Text(
                        BlockStrings.notALink,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                }
            }
        },
        confirmButton = {
            // Nella pagina basta toccare la riga.
            if (!inPage) TextButton(enabled = field.text.isNotBlank(), onClick = submit) { Text(Strings.done) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = onRemove) {
                        Text(EditorStrings.removeLink, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            }
        }
    )
}

/**
 * Le righe a cui collegarsi, come le voci di un indice: rientrate come nella
 * pagina, col segnalibro quelle col pin e la "T" quelle scritte in grande.
 * Quella a cui il testo porta già è colorata.
 */
@Composable
private fun LinkTargetList(targets: List<LinkTarget>, current: String?, onPick: (String) -> Unit) {
    if (targets.isEmpty()) {
        Text(EditorStrings.noLinkTargets, style = MaterialTheme.typography.bodyMedium, color = NotionGray400)
        return
    }
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
        targets.forEach { target ->
            val chosen = target.blockId == current
            val tint = if (chosen) MaterialTheme.colorScheme.primary else NotionGray400
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onPick(target.blockId) }
                    .padding(start = (4 + 14 * target.level.coerceAtMost(4)).dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (target.pinned) Icons.Filled.Bookmark else Icons.Filled.Title,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tint
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    target.label.ifBlank { EditorStrings.untitledLine },
                    color = if (chosen) MaterialTheme.colorScheme.primary else NotionWhite,
                    fontWeight = if (target.pinned) null else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
