package com.gabriele.notionlocal.ui.screen

import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
private const val LINK_TAG = "text-link"

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
 * Apre un collegamento; se non si può, lo dice. Le pagine web si aprono in
 * una Custom Tab, e "indietro" torna a notE: vedi `BlockFiles.openUrl`.
 */
@Composable
internal fun rememberLinkOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            if (!BlockFiles.openUrl(context, url)) {
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
 */
@Composable
internal fun LinkDialog(
    current: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRemove: () -> Unit
) {
    val start = current.orEmpty()
    var field by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    var wrong by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
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
                if (wrong) {
                    Text(
                        BlockStrings.notALink,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = field.text.isNotBlank(), onClick = submit) { Text(Strings.done) }
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
