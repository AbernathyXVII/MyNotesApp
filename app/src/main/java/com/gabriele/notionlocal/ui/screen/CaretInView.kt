// `ModifierLocalFocusedBoundsObserver` è interno a Compose (vedi `CaretBoundsNode`):
// lo si usa lo stesso, sapendo che un aggiornamento di Compose può romperlo
// (lo direbbe la compilazione).
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(ExperimentalFoundationApi::class)

package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ModifierLocalFocusedBoundsObserver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * **La pagina segue il cursore, non il pezzo di testo** (dal 19/10/2026,
 * chiesto dall'utente: la pagina "si sposta da sola senza senso").
 *
 * Di suo Compose, quando un campo di testo prende il fuoco o la tastiera
 * accorcia lo schermo, porta in vista **tutto il campo**. Finché il testo di
 * una pagina era un campo solo, alto quanto la pagina, non succedeva niente;
 * coi pezzi di 50 righe (v0.181) "tutto il campo" vuol dire mettere l'inizio
 * del pezzo in cima allo schermo (toccando a metà pagina, la pagina saltava
 * su) o la sua fine appena sopra la tastiera (unendo col backspace una riga a
 * quella del pezzo sopra, la pagina saltava giù).
 *
 * Qui, per un pezzo di testo, a chi fa scorrere la pagina si dice dove sta il
 * **cursore** ([caretRect]): sia quando qualcuno chiede di portare in vista
 * qualcosa del campo (il fuoco, la selezione, il cursore che si muove), sia
 * quando la tastiera sale e la pagina tiene in vista "quello che ha il fuoco".
 * Così la pagina si muove solo se il cursore finirebbe sotto la tastiera o
 * sotto i pulsanti in alto, e solo di quanto basta.
 */
internal class CaretInView {
    /**
     * Il cursore, coi margini sopra e sotto, nelle coordinate del contenuto del
     * riquadro che porta [followCaretOnly]; null se non si sa ancora (il campo
     * non è stato misurato).
     */
    var caretRect: () -> Rect? = { null }

    /** Dove sta adesso il segnaposto del cursore ([CaretAnchor]). */
    internal var anchor: LayoutCoordinates? = null
}

/**
 * Sul riquadro che contiene il campo, **dopo** i suoi margini: le richieste dei
 * figli diventano il cursore, e il fuoco del campo si misura sul cursore.
 * Dentro, accanto al campo, va il [CaretAnchor].
 */
internal fun Modifier.followCaretOnly(caret: CaretInView): Modifier =
    this
        .bringIntoViewResponder(object : BringIntoViewResponder {
            // Chiunque chieda (il fuoco chiede tutto il campo), si porta in vista il cursore.
            override fun calculateRectForParent(localRect: Rect): Rect = caret.caretRect() ?: localRect

            // Il campo non scorre per conto suo: c'è solo da passare la richiesta in su.
            override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
        })
        .then(CaretBoundsElement(caret))

/**
 * Un riquadro invisibile, grande quanto [CaretInView.caretRect] e messo lì: la
 * pagina, quando la tastiera sale, tiene in vista lui invece di tutto il campo.
 * Non occupa spazio e non prende tocchi.
 */
@Composable
internal fun CaretAnchor(caret: CaretInView) {
    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val rect = caret.caretRect()
                val placeable = measurable.measure(
                    Constraints.fixed(
                        (rect?.width ?: 1f).roundToInt().coerceAtLeast(1),
                        (rect?.height ?: 1f).roundToInt().coerceAtLeast(1)
                    )
                )
                layout(0, 0) {
                    if (rect != null) placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
                }
            }
            .onPlaced { caret.anchor = it }
    )
}

private class CaretBoundsElement(private val caret: CaretInView) : ModifierNodeElement<CaretBoundsNode>() {
    override fun create() = CaretBoundsNode(caret)

    override fun update(node: CaretBoundsNode) {
        node.caret = caret
    }

    override fun hashCode(): Int = caret.hashCode()

    override fun equals(other: Any?): Boolean = other is CaretBoundsElement && other.caret === caret

    override fun InspectorInfo.inspectableProperties() {
        name = "followCaretOnly"
    }
}

/**
 * Fra il campo e la pagina che scorre: quando il campo dice "ho il fuoco, sono
 * qui", la pagina sente invece dove sta il segnaposto del cursore. È lo stesso
 * giro di `onFocusedBoundsChanged` di Compose, che però passa il campo così
 * com'è.
 */
private class CaretBoundsNode(var caret: CaretInView) : Modifier.Node(), ModifierLocalModifierNode {
    private val parentObserver: ((LayoutCoordinates?) -> Unit)?
        get() = if (isAttached) ModifierLocalFocusedBoundsObserver.current else null

    private val observer: (LayoutCoordinates?) -> Unit = { focused ->
        if (isAttached) {
            val shown = if (focused == null) null else caret.anchor?.takeIf { it.isAttached } ?: focused
            parentObserver?.invoke(shown)
        }
    }

    override val providedValues: ModifierLocalMap =
        modifierLocalMapOf(entry = ModifierLocalFocusedBoundsObserver to observer)
}
