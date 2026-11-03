package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/** Quanto spazio resta fra la cella su cui si scrive e la tastiera (o il bordo in alto). */
internal val KEYBOARD_CLEARANCE = 24.dp

/** Il campo col fuoco, e dove sta il riquadro che lo contiene. */
private class FocusedField {
    /** Uno stato, perché lo si legge in `snapshotFlow`: cambia solo quando il fuoco passa a un altro campo. */
    var field by mutableStateOf<LayoutCoordinates?>(null)
    var container: LayoutCoordinates? = null
}

/**
 * **La cella su cui si scrive non resta mai dietro la tastiera** (dal
 * 14/10/2026, chiesto dall'utente: in Food, a pagina intera, le celle
 * "Order" in fondo finivano sotto la tastiera e non si vedeva cosa si
 * scriveva).
 *
 * Va messo **dentro** quello che scorre (dopo `verticalScroll`, o su una
 * riga di una `LazyColumn`), e vale per ogni campo di scrittura che c'è
 * sotto: celle, titoli, ricerca. Il lavoro è diviso in due.
 *
 * - **Il riquadro che scorre si accorcia** insieme alla tastiera (vedi
 *   `imePadding` a pagina intera, e l'editor delle pagine), e quando taglia
 *   il campo col fuoco Compose lo insegue con un'animazione. Da solo però
 *   porta in vista solo il testo del campo, al pelo della tastiera: il
 *   bordo della cella restava sotto, e nell'editor si vedeva mezza riga.
 * - **Qui, a ogni fotogramma in cui la tastiera sale** (e passando a
 *   un'altra cella con la tastiera aperta), si chiede di vedere il campo
 *   con [clearance] di spazio sopra e sotto. Compose aggiunge la richiesta
 *   all'animazione che sta già facendo, che cambia meta senza fermarsi:
 *   un movimento solo, che accelera e rallenta. La prima versione chiedeva
 *   il margine a tastiera ferma, e fra la salita e l'ultimo tratto c'erano
 *   due fotogrammi di pausa.
 * - Il margine non supera il contenuto che c'è sotto (e sopra) il campo:
 *   chiedere di scorrere oltre la fine fa interrompere a Compose tutta
 *   l'animazione, compreso l'inseguimento.
 *
 * Un campo più alto di un terzo dello schermo (un testo lungo che va a
 * capo) si lascia stare: "mostralo tutto" lì non vuol dire niente, e
 * sposterebbe la pagina lontano dal cursore.
 *
 * La tastiera si legge **fuori dal disegno** (`snapshotFlow`): letta nel
 * disegno, ridisegnerebbe tutto il database a ogni fotogramma della sua
 * animazione, ed è proprio lì che lo scorrimento comincerebbe a scattare.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
internal fun Modifier.keepFocusedAboveKeyboard(clearance: Dp = KEYBOARD_CLEARANCE): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val focused = remember { FocusedField() }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val tallest = LocalConfiguration.current.screenHeightDp.dp / 3
    LaunchedEffect(focused, density) {
        snapshotFlow {
            // Con la tastiera che sale o è su: il campo, e dov'è arrivata
            // (così si riparte a ogni suo fotogramma). Con la tastiera che
            // scende niente: la richiesta in sospeso si lascia cadere.
            val field = focused.field
            if (field != null && imeTarget.getBottom(density) > 0) field to ime.getBottom(density) else null
        }.collectLatest { now ->
            val field = now?.first ?: return@collectLatest
            val container = focused.container ?: return@collectLatest
            if (!field.isAttached || !container.isAttached) return@collectLatest
            val bounds = container.localBoundingBoxOf(field, clipBounds = false)
            if (bounds.height > with(density) { tallest.toPx() }) return@collectLatest
            val margin = with(density) { clearance.toPx() }
            val above = bounds.top.coerceIn(0f, margin)
            val below = (container.size.height - bounds.bottom).coerceIn(0f, margin)
            requester.bringIntoView(Rect(bounds.left, bounds.top - above, bounds.right, bounds.bottom + below))
        }
    }
    this
        .onPlaced { focused.container = it }
        .bringIntoViewRequester(requester)
        .onFocusedBoundsChanged { focused.field = it }
}
