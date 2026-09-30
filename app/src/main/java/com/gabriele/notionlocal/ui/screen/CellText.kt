package com.gabriele.notionlocal.ui.screen

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.gabriele.notionlocal.data.entity.CellAlignment

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(cellKey, cut, wrap, showsVisual) {
                        detectTapGestures(onTap = {
                            if (cut && !wrap && !showsVisual) {
                                expanded = true
                            } else {
                                fieldValue = TextFieldValue(localValue, TextRange(localValue.length))
                                everFocused = false
                                editing = true
                            }
                        })
                    },
                contentAlignment = alignment.box
            ) {
                if (showsVisual && idleVisual != null) {
                    idleVisual(localValue)
                } else {
                    Text(
                        text = if (localValue.isBlank()) localValue else shown(localValue),
                        style = textStyle,
                        maxLines = cellMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { cut = it.isCut() },
                        modifier = Modifier.fillMaxWidth()
                    )
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
