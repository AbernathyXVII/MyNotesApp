package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// **"Back" quando si è scesi molto** (dal 27/10/2026, chiesto dall'utente):
// in una pagina qualsiasi, scendendo per più di un paio di schermate compare
// in basso al centro lo stesso "Back" dell'indice (`LinkBackButton`), con la
// freccia in su; toccandolo si torna in cima. "Ovviamente l'app deve capire
// che, se vado manualmente verso l'alto, l'icona non deve apparire": conta
// l'ultimo verso in cui il dito ha fatto scorrere la pagina, e salendo se ne
// va. Mentre si scrive (tastiera aperta) non c'è: coprirebbe il testo.
// ---------------------------------------------------------------------------

internal const val BACK_TO_TOP_TAG = "backToTop"

/** Da quante schermate sotto la cima compare. */
internal const val BACK_TO_TOP_SCREENS = 1.5f

/** Il dito deve muoversi almeno di tanto (px) per cambiare verso: un tremolio non conta. */
private const val DIRECTION_SLOP_PX = 1f

/**
 * Il verso in cui il dito sta facendo scorrere la pagina, e quanto si è
 * scesi. Lo scorrimento arriva da `connection`, messa sulla lista: lì passa
 * solo quello del dito (trascinamento e lancio), non quello che fa l'app da
 * sé — il cursore portato in vista, il salto di un collegamento.
 */
@Stable
internal class BackToTop(private val list: LazyListState) {
    private val sizes = ItemSizes()

    /** L'ultimo verso del dito: true = si scende nella pagina. */
    var goingDown by mutableStateOf(false)

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Il dito che sale porta giù la pagina (y negativa), e viceversa.
            if (available.y < -DIRECTION_SLOP_PX) goingDown = true
            else if (available.y > DIRECTION_SLOP_PX) goingDown = false
            return Offset.Zero
        }
    }

    /** Lontano dalla cima: più di [BACK_TO_TOP_SCREENS] schermate, con la stima della barretta (`ItemSizes`). */
    fun farFromTop(): Boolean {
        val info = list.layoutInfo
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        if (viewport <= 0f || !list.canScrollBackward) return false
        sizes.record(info)
        val (scrolled, _) = sizes.extent(info.totalItemsCount, list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
        return scrolled > viewport * BACK_TO_TOP_SCREENS
    }
}

@Composable
internal fun rememberBackToTop(list: LazyListState): BackToTop = remember(list) { BackToTop(list) }

/**
 * Il pulsante, quando serve: lontani dalla cima, l'ultimo movimento del dito
 * verso il basso, la tastiera chiusa e [allowed] (niente selezione della
 * pagina, niente "Back" di un collegamento, niente disegno).
 */
@Composable
internal fun BackToTopButton(state: BackToTop, list: LazyListState, allowed: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    // Solo quando cambia sì/no: letta così la tastiera non ridisegna niente mentre sale.
    val keyboardOpen by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    val far by remember(state) { derivedStateOf { state.farFromTop() } }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = allowed && far && state.goingDown && !keyboardOpen,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        BackPill(
            upward = true,
            tag = BACK_TO_TOP_TAG,
            onClick = {
                state.goingDown = false
                scope.launch { list.animateScrollToItem(0) }
            }
        )
    }
}
