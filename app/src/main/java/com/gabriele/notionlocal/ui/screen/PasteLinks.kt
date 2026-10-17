package com.gabriele.notionlocal.ui.screen

import android.content.ClipData
import android.content.Context
import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.gabriele.notionlocal.data.entity.LinkRange

// ---------------------------------------------------------------------------
// **Incollare coi link** (dal 17/10/2026, chiesto dall'utente): il testo
// copiato da un'altra app — OneNote, un browser — arriva coi suoi
// collegamenti, sulle stesse parole.
//
// Le altre app mettono negli appunti il testo semplice e, accanto, lo stesso
// testo in HTML (o un testo coi link dentro). L'incolla di Compose prende
// solo il testo semplice: `LinkClipboard` gli dà lo stesso testo con i link
// segnati sopra (`PASTED_LINK_TAG`), che arrivano nel valore del campo
// (`pastedLinks`) e diventano link veri nel testo ricco (`applyLinks`).
//
// Vale per "Paste" del menu che si apre tenendo premuto (e Ctrl+V); quello
// che la tastiera incolla dal suo pannello degli appunti arriva come testo
// semplice, e lì i link non ci sono.
// ---------------------------------------------------------------------------

private const val PASTED_LINK_TAG = "pasted-link"

/** Gli appunti dell'editor: come quelli di sempre, ma il testo incollato porta i suoi link. */
internal class LinkClipboard(private val context: Context, private val base: ClipboardManager) : ClipboardManager by base {

    override fun getText(): AnnotatedString? {
        val item = runCatching {
            context.getSystemService(android.content.ClipboardManager::class.java)?.primaryClip
                ?.takeIf { it.itemCount > 0 }?.getItemAt(0)
        }.getOrNull() ?: return base.getText()
        val raw: CharSequence = item.text ?: item.htmlText?.let { htmlToSpanned(it).toString().trimEnd() } ?: return base.getText()
        val plain = raw.toString()
        val links = runCatching { clipLinks(item, plain) }.getOrDefault(emptyList())
        val builder = AnnotatedString.Builder(plain)
        links.forEach { builder.addStringAnnotation(PASTED_LINK_TAG, it.url, it.start, it.end) }
        return builder.toAnnotatedString()
    }

    /** I link di un elemento degli appunti, nelle posizioni di [plain]. */
    private fun clipLinks(item: ClipData.Item, plain: String): List<LinkRange> {
        // Un testo coi link dentro: le posizioni sono già quelle giuste.
        (item.text as? Spanned)?.let { spanned ->
            val found = spanned.getSpans(0, spanned.length, URLSpan::class.java)
                .sortedBy { spanned.getSpanStart(it) }
                .map { LinkRange(spanned.getSpanStart(it), spanned.getSpanEnd(it), it.url.orEmpty()) }
                .filter { it.end <= plain.length && usableLink(it.url) }
            if (found.isNotEmpty()) return found
        }
        // L'HTML accanto al testo semplice: le parole dei link si ritrovano
        // nel testo, in ordine.
        val html = item.htmlText ?: return emptyList()
        val spanned = htmlToSpanned(html)
        val anchors = spanned.getSpans(0, spanned.length, URLSpan::class.java)
            .sortedBy { spanned.getSpanStart(it) }
            .map { spanned.subSequence(spanned.getSpanStart(it), spanned.getSpanEnd(it)).toString() to it.url.orEmpty() }
            .filter { usableLink(it.second) }
        return matchAnchors(anchors, plain)
    }
}

private fun htmlToSpanned(html: String): Spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)

/** Niente link che eseguono codice, né vuoti. */
private fun usableLink(url: String): Boolean = url.isNotBlank() && !url.trim().startsWith("javascript:", ignoreCase = true)

/**
 * **Le parole dei link ritrovate nel testo semplice**, una dopo l'altra:
 * ognuna si cerca da dove finisce la precedente, così due link con la stessa
 * parola vanno ognuno al suo posto. Gli spazi speciali dell'HTML (quello che
 * non va a capo) valgono come spazi normali.
 */
internal fun matchAnchors(anchors: List<Pair<String, String>>, plain: String): List<LinkRange> {
    val haystack = plain.replace(' ', ' ')
    var from = 0
    return anchors.mapNotNull { (anchor, url) ->
        val needle = anchor.replace(' ', ' ').trim()
        if (needle.isEmpty()) return@mapNotNull null
        val at = haystack.indexOf(needle, from)
        if (at < 0) return@mapNotNull null
        from = at + needle.length
        LinkRange(at, at + needle.length, url.trim())
    }
}

/** Gli appunti coi link per i campi dell'editor. */
@Composable
internal fun rememberLinkClipboard(): ClipboardManager {
    val context = LocalContext.current
    val base = LocalClipboardManager.current
    return remember(context, base) { if (base is LinkClipboard) base else LinkClipboard(context, base) }
}

/**
 * I link arrivati con un incolla nel valore del campo, nelle posizioni del
 * testo vero: senza i primi [lead] caratteri (l'a-capo nascosto in testa).
 */
internal fun TextFieldValue.pastedLinks(lead: Int): List<LinkRange> =
    annotatedString.getStringAnnotations(PASTED_LINK_TAG, 0, text.length)
        .map { LinkRange(it.start - lead, it.end - lead, it.item) }
        .filter { it.start >= 0 && it.end > it.start }

/**
 * Lo stesso valore senza i segni dei link incollati: rimasti nel campo,
 * tornerebbero con ogni incolla successivo. Senza segni resta com'è (per
 * non disturbare la tastiera mentre si scrive).
 */
internal fun TextFieldValue.withoutPastedLinks(): TextFieldValue =
    if (annotatedString.getStringAnnotations(PASTED_LINK_TAG, 0, text.length).isEmpty()) {
        this
    } else {
        copy(annotatedString = AnnotatedString(text))
    }
