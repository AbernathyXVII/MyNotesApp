package com.gabriele.notionlocal.ui.screen

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.gabriele.notionlocal.data.entity.CellAlignment
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet

/**
 * **"Wrap cells"** (dal 30/09/2026, nelle impostazioni di ogni tabella):
 * acceso, il testo delle celle va a capo e la riga si allunga quanto serve;
 * spento, quello che non ci sta finisce coi tre puntini, e toccando la cella
 * compare il testo intero (`FullTextPopup`). Lo dà la tabella; altrove (le
 * proprietà in cima alla pagina di una riga) è spento.
 */
internal val LocalWrapCells = staticCompositionLocalOf { false }

/** Quante righe può avere il testo di una cella: tutte se va a capo, una se no. */
internal val cellMaxLines: Int
    @Composable get() = if (LocalWrapCells.current) Int.MAX_VALUE else 1

/** Il margine sopra e sotto il testo che va a capo: una riga sola resta alta 44, come le altre. */
@Composable
internal fun Modifier.cellWrapPadding(): Modifier =
    if (LocalWrapCells.current) padding(vertical = WRAP_PADDING) else this

private val WRAP_PADDING = 10.dp

/**
 * **Quanto è larga una colonna** (dal 30/09/2026): quella scelta (col
 * segnalino, "Fit to content" o di sempre) — con "Widen the column" almeno
 * quanto il suo contenuto (`fitted`) — e mai sotto il suo minimo.
 */
internal fun shownColumnWidth(chosen: Dp, fitted: Dp?, widen: Boolean, min: Dp): Dp =
    (if (widen && fitted != null) maxOf(chosen, fitted) else chosen).coerceAtLeast(min)

/** Se il testo di una cella è stato tagliato coi tre puntini. */
internal fun TextLayoutResult.isCut(): Boolean =
    hasVisualOverflow || (lineCount > 0 && isLineEllipsized(lineCount - 1))

/** L'allineamento delle celle, per chi dispone i pezzi in orizzontale (i tag che vanno a capo). */
internal val CellAlignment.horizontal: Alignment.Horizontal
    get() = when (this) {
        CellAlignment.LEFT -> Alignment.Start
        CellAlignment.CENTER -> Alignment.CenterHorizontally
        CellAlignment.RIGHT -> Alignment.End
    }

/**
 * **Il testo intero di una cella** (dal 30/09/2026): toccando una cella il
 * cui testo finisce coi tre puntini, compare un riquadro grigio **centrato
 * sulla cella, proprio sopra di lei** (così l'ha voluto l'utente, dopo una
 * prima versione appoggiata all'angolo in alto a sinistra), col testo
 * intero che va a capo, e ci si scrive come nella cella. È largo almeno
 * quanto la cella, e almeno [FULL_TEXT_MIN_WIDTH]: in una colonna stretta il
 * testo sarebbe una colonnina. Si chiude toccando fuori, col gesto indietro
 * o con "Done" sulla tastiera. Con il database bloccato si legge e basta.
 *
 * Va messo dentro un riquadro centrato sulla cella: il suo centro è quello
 * del riquadro.
 */
@Composable
internal fun FullTextPopup(
    value: String,
    onValueChange: (String) -> Unit,
    /** Quanto è larga la cella. */
    cellWidth: Dp,
    keyboardOptions: KeyboardOptions,
    onDismiss: () -> Unit
) {
    val locked = LocalDatabaseLocked.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val width = maxOf(cellWidth, FULL_TEXT_MIN_WIDTH).coerceAtMost(screenWidth - 16.dp)
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
        val focusRequester = remember { FocusRequester() }
        Box(
            modifier = Modifier
                .width(width)
                .shadow(6.dp, RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = FULL_TEXT_PADDING_H, vertical = FULL_TEXT_PADDING_V)
        ) {
            AutoCaps {
                BasicTextField(
                    value = field,
                    onValueChange = { next ->
                        // Una cella sta su una riga: gli a capo incollati si tolgono.
                        val clean = next.text.replace("\n", "")
                        field = if (clean == next.text) next else TextFieldValue(clean, TextRange(clean.length))
                        if (clean != value) onValueChange(clean)
                    },
                    readOnly = locked,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = KeyboardActions(onDone = { onDismiss() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

internal val FULL_TEXT_MIN_WIDTH = 260.dp
internal val FULL_TEXT_PADDING_H = 12.dp
private val FULL_TEXT_PADDING_V = 10.dp

/**
 * **Una cella in cui si scrive**: testo, numero, collegamento, email,
 * telefono (dal 30/09/2026 com'è adesso). Finché non la si tocca è testo
 * semplice: con "Wrap cells" spento finisce coi tre puntini se non ci sta
 * (toccandola allora si apre `FullTextPopup`), acceso va a capo. Toccandola
 * quando ci sta tutto, si scrive nella cella stessa, col cursore in fondo.
 *
 * Prima era sempre un campo di scrittura su una riga: il testo lungo
 * spariva oltre il bordo senza nessun segno che continuasse.
 */
@Composable
internal fun TextLikeCell(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    /** Com'è scritto quando non lo si scrive: un numero nel formato della colonna. */
    shown: (String) -> String = { it },
    /** Al posto del testo finché non la si tocca (il numero come barra o anello); null = il testo. */
    idleVisual: (@Composable (String) -> Unit)? = null,
    /** Una cella URL: con un indirizzo dentro, il tocco lo apre e il tocco lungo dà il menu (vedi sotto). */
    isLink: Boolean = false,
    cellKey: String
) {
    val wrap = LocalWrapCells.current
    val alignment = LocalCellAlignment.current
    var localValue by remember(cellKey) { mutableStateOf(value) }
    var editing by remember(cellKey) { mutableStateOf(false) }
    var expanded by remember(cellKey) { mutableStateOf(false) }
    var cut by remember(cellKey) { mutableStateOf(false) }
    var everFocused by remember(cellKey) { mutableStateOf(false) }
    var fieldValue by remember(cellKey) { mutableStateOf(TextFieldValue(value)) }
    var textWidthPx by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    // Cambiato da fuori (Annulla, un'altra vista) mentre non lo si scrive: si rilegge.
    LaunchedEffect(value) { if (!editing && !expanded) localValue = value }
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = alignment.textAlign
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cellWrapPadding()
            .onSizeChanged { textWidthPx = it.width },
        contentAlignment = alignment.box
    ) {
        if (editing) {
            AutoCaps {
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { next ->
                        val clean = next.text.replace("\n", "")
                        fieldValue = if (clean == next.text) next else TextFieldValue(clean, TextRange(clean.length))
                        if (clean != localValue) {
                            localValue = clean
                            onValueChange(clean)
                        }
                    },
                    singleLine = !wrap,
                    readOnly = LocalDatabaseLocked.current,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = KeyboardActions(onDone = { editing = false }),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .markFocusChanges()
                        .onFocusChanged { state ->
                            if (state.isFocused) everFocused = true else if (everFocused) editing = false
                        }
                )
            }
            LaunchedEffect(Unit) {
                everFocused = false
                focusRequester.requestFocus()
            }
        } else {
            val showsVisual = idleVisual != null && localValue.isNotBlank()
            // **Un link** (dal 05/10/2026, chiesto dall'utente): una cella URL
            // con un indirizzo dentro non si scrive più toccandola. Il tocco
            // apre il link; il tocco lungo dà "Edit link", "Copy link" e
            // "Paste link" (che mette al suo posto quello copiato). Vuota, si
            // scrive come prima.
            val link = isLink && localValue.isNotBlank()
            val locked = LocalDatabaseLocked.current
            val context = LocalContext.current
            val haptics = LocalHapticFeedback.current
            var linkMenu by remember(cellKey) { mutableStateOf(false) }
            /** Si scrive nella cella; `inPlace` anche se il testo non ci sta, invece del riquadro del testo intero. */
            fun startEditing(inPlace: Boolean = false) {
                if (cut && !wrap && !showsVisual && !inPlace) {
                    expanded = true
                } else {
                    fieldValue = TextFieldValue(localValue, TextRange(localValue.length))
                    everFocused = false
                    editing = true
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(cellKey, cut, wrap, showsVisual, link) {
                        detectTapGestures(
                            onTap = { if (link) openLink(context, localValue) else startEditing() },
                            onLongPress = if (link) {
                                {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    linkMenu = true
                                }
                            } else {
                                null
                            }
                        )
                    },
                contentAlignment = alignment.box
            ) {
                if (showsVisual && idleVisual != null) {
                    idleVisual(localValue)
                } else {
                    Text(
                        text = if (localValue.isBlank()) localValue else shown(localValue),
                        style = if (link) textStyle.copy(textDecoration = TextDecoration.Underline) else textStyle,
                        maxLines = cellMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { cut = it.isCut() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                DropdownMenu(
                    expanded = linkMenu,
                    onDismissRequest = { linkMenu = false },
                    modifier = Modifier.background(DarkSheet)
                ) {
                    if (!locked) {
                        DropdownMenuItem(
                            text = { Text(DbStrings.editLink) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                linkMenu = false
                                // Nella cella stessa, col cursore in fondo: il riquadro del
                                // testo intero (`FullTextPopup`) è una finestra a parte, e una
                                // cella in basso nella pagina lo lasciava sotto la tastiera
                                // (trovato sul telefono il 05/10/2026). Il campo nella cella
                                // invece la pagina lo porta sopra la tastiera.
                                startEditing(inPlace = true)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(DbStrings.copyLink) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = {
                            linkMenu = false
                            copyLink(context, localValue)
                        }
                    )
                    if (!locked && hasClipText(context)) {
                        DropdownMenuItem(
                            text = { Text(DbStrings.pasteLink) },
                            leadingIcon = { Icon(Icons.Filled.ContentPaste, contentDescription = null) },
                            onClick = {
                                linkMenu = false
                                val pasted = pastedLink(context)
                                if (pasted != null && pasted != localValue) {
                                    localValue = pasted
                                    onValueChange(pasted)
                                }
                            }
                        )
                    }
                }
            }
        }
        if (expanded) {
            FullTextPopup(
                value = localValue,
                onValueChange = {
                    localValue = it
                    onValueChange(it)
                },
                // Il riquadro del testo sta in mezzo alla cella, coi suoi 12
                // punti per lato: il suo centro è quello della cella.
                cellWidth = with(LocalDensity.current) { textWidthPx.toDp() } + FULL_TEXT_PADDING_H * 2,
                keyboardOptions = keyboardOptions,
                onDismiss = { expanded = false }
            )
        }
    }
}

/**
 * L'indirizzo da aprire: com'è scritto se ha già il suo inizio
 * ("https://…", "mailto:…", "tel:…"), altrimenti con "https://" davanti —
 * "bulbapedia.bulbagarden.net/wiki/…" scritto a mano si apre lo stesso.
 */
internal fun linkUri(raw: String): String {
    val link = raw.trim()
    val hasScheme = link.contains("://") || listOf("mailto:", "tel:", "geo:").any { link.startsWith(it, ignoreCase = true) }
    return if (hasScheme) link else "https://$link"
}

/** Apre il link col browser (o l'app che lo sa aprire); se nessuna può, lo dice. */
internal fun openLink(context: Context, raw: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUri(raw))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, DbStrings.cantOpenLink, Toast.LENGTH_SHORT).show()
    }
}

/** Copia il link. Da Android 13 il "copiato" lo dice già il telefono: l'avviso dell'app solo prima. */
internal fun copyLink(context: Context, raw: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(DbStrings.copyLink, raw.trim()))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, DbStrings.linkCopied, Toast.LENGTH_SHORT).show()
    }
}

/** C'è del testo da incollare: senza leggerlo, così il telefono non avvisa per niente. */
internal fun hasClipText(context: Context): Boolean =
    context.getSystemService(ClipboardManager::class.java)?.hasPrimaryClip() == true

/** Il testo copiato, su una riga sola e senza spazi ai lati; null se non c'è. */
internal fun pastedLink(context: Context): String? {
    val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
        ?.replace("\n", " ")?.trim()
        ?.takeIf { it.isNotEmpty() }
}
