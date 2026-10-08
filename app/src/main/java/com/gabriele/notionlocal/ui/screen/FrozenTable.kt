package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gabriele.notionlocal.ui.theme.DarkBackground

/**
 * **Prima colonna e prima riga ferme nella tabella** (dal 08/10/2026,
 * chiesto dall'utente): "se vado troppo a destra la prima colonna la vedo
 * comunque, e se vado troppo sotto la prima riga la vedo comunque", per
 * sapere sempre di che riga e di che proprietà è la casella che si spunta.
 *
 * - **La prima colonna** scorre con la tabella, ma è spostata a destra di
 *   quanto la tabella è scorsa: resta al bordo sinistro, sopra le altre
 *   (`frozenFirstCell`). È la prima che si vede, Name o un'altra.
 * - **La prima riga**, quella dei titoli delle colonne, scende quanto
 *   serve per restare in cima al contenitore che scorre — il database a
 *   pagina intera, o la pagina che lo contiene — finché resta dentro la sua
 *   tabella o il suo gruppo (`PinnedHeaderSection`).
 */

/**
 * **Il contenitore che scorre in verticale** intorno a una tabella, e un
 * segnale che cambia quando scorre: l'intestazione lo legge mentre si
 * disegna, così si ridisegna a ogni passo dello scorrimento.
 *
 * Il punto è sapere **dove sta adesso** la tabella rispetto al bordo alto,
 * nel fotogramma che si sta disegnando, senza restare indietro di uno:
 *
 * - con uno `ScrollState` (il database a pagina intera, `forScroll`) lo
 *   scorrimento cambia prima che il contenuto venga spostato. Si misura
 *   allora dove sta la tabella **nel contenuto** (non cambia scorrendo) e si
 *   toglie quanto si è scorso, che è sempre quello di adesso. Va messo
 *   **dopo** `verticalScroll`, sul contenuto;
 * - in una `LazyColumn` (la pagina, `forList`) le righe vengono spostate
 *   subito, appena si scorre, e lo stato cambia dopo: basta la posizione
 *   rispetto al riquadro. Va messo **sulla** `LazyColumn`.
 */
internal class StickyViewport private constructor(
    val scrollSignal: () -> Int,
    /** Quanto sotto il bordo alto fermarsi: nella pagina, sotto i pulsanti sospesi. */
    val topInset: Dp,
    /** Quanto è scorso il contenuto, per `forScroll`; null per `forList`. */
    private val scrolled: (() -> Int)?
) {
    var coordinates: LayoutCoordinates? = null

    /** Dove sta adesso il bordo alto di `box` sotto quello del riquadro; null se non si sa. */
    fun topOf(box: LayoutCoordinates): Float? {
        val reference = coordinates?.takeIf { it.isAttached } ?: return null
        if (!box.isAttached) return null
        val y = reference.localPositionOf(box, Offset.Zero).y
        return if (scrolled != null) y - scrolled.invoke() else y
    }

    companion object {
        fun forScroll(state: ScrollState, topInset: Dp = 0.dp) =
            StickyViewport({ state.value }, topInset, { state.value })

        fun forList(state: LazyListState, topInset: Dp = 0.dp) =
            StickyViewport({ state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset }, topInset, null)
    }
}

internal val LocalStickyViewport = staticCompositionLocalOf<StickyViewport?> { null }

/** Dove sta il contenitore: vedi `StickyViewport` per dove metterlo. */
internal fun Modifier.stickyViewport(viewport: StickyViewport): Modifier =
    this.onPlaced { viewport.coordinates = it }

/** Lo scorrimento orizzontale della tabella, quando la prima colonna è ferma. */
internal class FrozenColumn(val scroll: ScrollState)

internal val LocalFrozenColumn = staticCompositionLocalOf<FrozenColumn?> { null }

/**
 * La prima cella di una riga quando la prima colonna è ferma: spostata di
 * quanto la tabella è scorsa, sopra le celle che le passano sotto, col fondo
 * della pagina. `found` aggiunge l'evidenziazione della ricerca, che il fondo
 * altrimenti coprirebbe.
 */
internal fun Modifier.frozenFirstCell(frozen: FrozenColumn, found: Modifier = Modifier): Modifier =
    this
        .zIndex(1f)
        .offset { IntOffset(frozen.scroll.value, 0) }
        .background(DarkBackground)
        .then(found)

/** Dove sta la tabella (o il gruppo) sotto cui l'intestazione resta ferma. */
private class SectionBounds {
    var coordinates: LayoutCoordinates? = null
    /** Quante volte è stata piazzata: si scrive nello stato senza leggerlo, per non rifare il piazzamento. */
    var placements = 0
}

/**
 * Di quanto scendere: quanto la tabella è salita oltre il bordo alto
 * (più il margine), ma mai oltre il fondo della tabella stessa.
 */
private fun pinnedShift(section: SectionBounds, viewport: StickyViewport, insetPx: Float, headerHeight: Float): Float {
    val box = section.coordinates ?: return 0f
    val top = viewport.topOf(box) ?: return 0f
    val room = (box.size.height - headerHeight).coerceAtLeast(0f)
    return (insetPx - top).coerceIn(0f, room)
}

/**
 * **L'intestazione che resta in cima** e quello che c'è sotto di lei (righe,
 * calcoli, "New page"). Spenta, o senza un contenitore che scorre, è come
 * prima: intestazione e righe una sotto l'altra.
 *
 * Accesa, l'intestazione scende quanto serve per restare al bordo alto e ci
 * disegna sopra un fondo pieno fin dove sarebbe stata, così le righe che
 * passano lì sotto non si vedono. È sopra le righe anche per i tocchi.
 */
@Composable
internal fun ColumnScope.PinnedHeaderSection(
    pin: Boolean,
    header: @Composable () -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    val viewport = LocalStickyViewport.current
    if (!pin || viewport == null) {
        header()
        body()
        return
    }
    val section = remember { SectionBounds() }
    // Cresce ogni volta che la tabella si sposta sullo schermo: scorrendo, o
    // senza scorrere (una riga aggiunta, del testo scritto sopra). Android lo
    // dice dopo aver piazzato tutto e prima di disegnare, così l'intestazione
    // si rimette al suo posto nello stesso fotogramma, dove sta davvero la
    // tabella — nella pagina lo stato dello scorrimento può arrivare prima.
    val placed = remember { mutableIntStateOf(0) }
    val insetPx = with(LocalDensity.current) { viewport.topInset.toPx() }
    val background = DarkBackground
    Column(
        modifier = Modifier.onGloballyPositioned {
            section.coordinates = it
            placed.intValue = ++section.placements
        }
    ) {
        Column(
            modifier = Modifier
                .zIndex(1f)
                .graphicsLayer {
                    viewport.scrollSignal()
                    placed.intValue
                    translationY = pinnedShift(section, viewport, insetPx, size.height)
                }
                .drawBehind {
                    viewport.scrollSignal()
                    placed.intValue
                    val shift = pinnedShift(section, viewport, insetPx, size.height)
                    if (shift > 0f) drawRect(background, topLeft = Offset(0f, -shift), size = Size(size.width, shift))
                }
                .background(background)
        ) {
            header()
        }
        body()
    }
}
