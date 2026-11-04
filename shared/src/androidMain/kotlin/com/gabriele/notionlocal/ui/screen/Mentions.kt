package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository.Backlink
import com.gabriele.notionlocal.ui.i18n.MentionStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import java.time.LocalDate

// ---------------------------------------------------------------------------
// **Le menzioni "@" e i backlink** (dal 03/11/2026, chiesto dall'utente:
// "Collegamenti all'indietro (backlink), come Obsidian e Notion. In fondo a
// ogni pagina vedresti quali altre pagine la richiamano. Insieme, le menzioni
// @ dentro il testo per citare una pagina o una data al volo. Oggi i rimandi
// vanno in un verso solo"). Come sono fatte le menzioni: `PageMentions.kt`.
// ---------------------------------------------------------------------------

/** Apre la pagina di una menzione toccata (l'id); la dà la schermata della pagina. */
val LocalPageMentionOpen = staticCompositionLocalOf<((String) -> Unit)?> { null }

/** Una data menzionata toccata (il suo collegamento): si cambia. La dà la schermata della pagina. */
val LocalDateMentionTap = staticCompositionLocalOf<((String) -> Unit)?> { null }

const val MENTION_MENU_TAG = "mention_menu"
const val BACKLINKS_TAG = "backlinks"

/** Oltre questi caratteri dopo l'"@" non si sta più cercando una pagina. */
const val MENTION_QUERY_MAX = 40

/**
 * **Un "@" appena scritto apre il menu** se sta a inizio riga o dopo uno
 * spazio o una parentesi: dentro una parola ("mario@gmail.com") è un
 * indirizzo, non una menzione.
 */
fun mentionJustTyped(oldBody: String, newBody: String, caret: Int): Boolean {
    if (newBody.length != oldBody.length + 1 || caret < 1 || caret > newBody.length || newBody[caret - 1] != '@') return false
    val before = newBody.getOrNull(caret - 2) ?: return true
    return before.isWhitespace() || before in "([{\"'«“"
}

/**
 * Quello che si è scritto dopo l'"@" in [anchor], o null se il menu va
 * chiuso: l'"@" tolto, il cursore tornato prima, un a-capo, uno spazio subito
 * dopo l'"@" o una ricerca troppo lunga.
 */
fun mentionQueryAt(body: String, anchor: Int, caret: Int): String? {
    if (anchor >= body.length || body[anchor] != '@' || caret <= anchor) return null
    val typed = body.substring(anchor + 1, caret.coerceAtMost(body.length))
    if ('\n' in typed || typed.startsWith(" ") || typed.length > MENTION_QUERY_MAX) return null
    return typed
}

/** Una voce del menu "@": una pagina, un giorno, o "Pick a date…" ([date] null). */
sealed interface MentionChoice {
    data class Page(val page: PageEntity) : MentionChoice
    data class Day(val label: String, val date: LocalDate?) : MentionChoice
}

/** I giorni del menu "@" che somigliano a [query]: oggi, domani, ieri e "Pick a date…". */
fun mentionDays(query: String, today: LocalDate = LocalDate.now()): List<MentionChoice.Day> {
    val all = listOf(
        MentionChoice.Day(MentionStrings.today, today) to "today",
        MentionChoice.Day(MentionStrings.tomorrow, today.plusDays(1)) to "tomorrow",
        MentionChoice.Day(MentionStrings.yesterday, today.minusDays(1)) to "yesterday",
        MentionChoice.Day(MentionStrings.pickDate, null) to "date"
    )
    val q = query.trim()
    if (q.isEmpty()) return all.map { it.first }
    return all.filter { (day, english) ->
        listOf(day.label, english).any { name -> name.split(' ').any { it.startsWith(q, ignoreCase = true) } }
    }.map { it.first }
}

/**
 * **Il menu "@"**: le pagine col nome che somiglia a quello che si scrive
 * dopo la chiocciola (senza niente scritto, le toccate per ultime), poi i
 * giorni. Come il menu "/", non prende il fuoco: si continua a scrivere e la
 * lista si filtra.
 */
@Composable
fun MentionMenu(
    query: String,
    candidates: suspend (String) -> List<PageEntity>,
    onDismiss: () -> Unit,
    onPick: (MentionChoice) -> Unit
) {
    var pages by remember { mutableStateOf<List<PageEntity>?>(null) }
    LaunchedEffect(query) { pages = candidates(query) }
    val days = remember(query) { mentionDays(query) }
    val scroll = rememberScrollState()
    LaunchedEffect(query) { scroll.scrollTo(0) }
    // Niente che somigli e uno spazio scritto: si sta scrivendo una frase.
    LaunchedEffect(pages, days, query) {
        if (pages?.isEmpty() == true && days.isEmpty() && query.endsWith(' ')) onDismiss()
    }
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        // Come il menu "/": la tastiera non è un tocco fuori (vedi `SlashMenu`).
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .heightIn(max = 360.dp)
                .background(DarkBackground)
                .verticalScroll(scroll)
                .padding(vertical = 8.dp)
                .testTag(MENTION_MENU_TAG)
        ) {
            MenuHeader(MentionStrings.pages)
            val found = pages
            if (found != null && found.isEmpty()) {
                Text(
                    text = MentionStrings.noPages,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            found.orEmpty().forEach { page ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(MentionChoice.Page(page)) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PageIconView(page.icon, page.iconImage, page.isDatabase, 20.dp, page.iconSymbol)
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = page.title.ifBlank { Strings.untitled },
                        color = NotionWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (days.isNotEmpty()) {
                MenuHeader(MentionStrings.dates)
                days.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(day) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (day.date == null) Icons.Filled.CalendarMonth else Icons.Filled.Event,
                            contentDescription = null,
                            tint = NotionWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(day.label, color = NotionWhite, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        day.date?.let {
                            Text(MentionStrings.dateText(it), color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
    )
}

/**
 * **I backlink, in fondo alla pagina**: le pagine che la richiamano, con la
 * riga in cui lo fanno; un tocco le apre. Il titolo della sezione la chiude
 * e la riapre. Non c'è quando nessuna pagina la richiama.
 *
 * Lo spazio sopra la sezione fa quello che fa il vuoto sotto l'ultima riga:
 * un tocco lì porta il cursore in fondo alla pagina ([onTapAbove]).
 */
@Composable
fun BacklinksSection(
    backlinks: List<Backlink>,
    sidePadding: Dp,
    onOpen: (PageEntity) -> Unit,
    onTapAbove: () -> Unit
) {
    var open by rememberSaveable { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().testTag(BACKLINKS_TAG)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(Unit) { detectTapGestures { onTapAbove() } }
        )
        HorizontalDivider(
            color = NotionGray400.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = sidePadding)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = sidePadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                "${MentionStrings.backlinks} · ${backlinks.size}",
                color = NotionGray400,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (open) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NotionGray400,
                modifier = Modifier.size(20.dp)
            )
        }
        if (open) {
            backlinks.forEach { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(link.page) }
                        .padding(horizontal = sidePadding, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    PageIconView(link.page.icon, link.page.iconImage, link.page.isDatabase, 20.dp, link.page.iconSymbol)
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            link.page.title.ifBlank { Strings.untitled },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            link.snippet?.takeIf { it.isNotBlank() } ?: MentionStrings.linkToPage,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
