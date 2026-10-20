package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * **Le righe sullo schermo stanno in un campo solo** (dal 19/10/2026, per la
 * selezione che in Permanent notes si fermava a "Kor", l'ultima riga di un
 * pezzo): vedi `PageEditorViewModel.keepVisibleTogether`. Ogni gruppo di righe
 * della pagina dice quali delle sue righe si vedono; quando la pagina si ferma
 * e nessun dito la tocca, la pagina le passa al ViewModel.
 */
internal class VisibleRunLines {
    /**
     * Per ogni voce della lista (la sua chiave), le righe che stanno fra `top`
     * e `bottom`, in px nelle coordinate della voce. Non è uno stato: si legge
     * solo quando la pagina si è fermata.
     */
    val providers = HashMap<Any, (top: Float, bottom: Float) -> List<String>>()

    /** Le righe sullo schermo adesso, in ordine. */
    fun visibleIds(list: LazyListState): List<String> {
        val info = list.layoutInfo
        val ids = ArrayList<String>()
        for (item in info.visibleItemsInfo) {
            val lines = providers[item.key] ?: continue
            ids += lines((info.viewportStartOffset - item.offset).toFloat(), (info.viewportEndOffset - item.offset).toFloat())
        }
        return ids
    }
}

/** Le righe sullo schermo della pagina aperta: vedi [VisibleRunLines]. */
internal val LocalVisibleRunLines = staticCompositionLocalOf<VisibleRunLines?> { null }

/**
 * Se un dito tocca la pagina adesso. Non le maniglie della selezione, che
 * stanno in una finestra a parte: è proprio così che si distingue il dito che
 * trascina sul testo da quello che tiene una maniglia.
 */
internal val LocalPageFingerDown = staticCompositionLocalOf<State<Boolean>?> { null }

/** Tiene [down] aggiornato con le dita sulla pagina, senza prendere niente. */
internal fun Modifier.watchPageFingers(down: MutableState<Boolean>): Modifier =
    pointerInput(down) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.any { it.pressed }
                if (down.value != pressed) down.value = pressed
            }
        }
    }

/** Quanto la pagina deve restare ferma prima che i pezzi sullo schermo si uniscano. */
private const val KEEP_TOGETHER_DELAY_MS = 250L

/**
 * Quando la pagina si ferma, nessun dito la tocca e non si sta disegnando, le
 * righe sullo schermo diventano un campo solo. Si riprova quando cambiano i
 * pezzi o le righe, e dopo un attimo se il campo in cui si scriveva è stato
 * lasciato stare.
 */
@Composable
internal fun KeepVisibleLinesTogether(
    viewModel: PageEditorViewModel,
    list: LazyListState,
    lines: VisibleRunLines,
    fingerDown: State<Boolean>,
    chunkVersion: Int,
    blocksVersion: Any,
    drawing: Boolean,
    focusedBlockId: String?
) {
    val focused = rememberUpdatedState(focusedBlockId)
    val versions = rememberUpdatedState(chunkVersion to blocksVersion)
    LaunchedEffect(viewModel, list, lines, drawing) {
        if (drawing) return@LaunchedEffect
        snapshotFlow {
            val busy = list.isScrollInProgress || fingerDown.value
            // Anche le voci sullo schermo: si riprova quando cambiano.
            Triple(busy, list.layoutInfo.visibleItemsInfo.map { it.key }, versions.value)
        }.collectLatest { (busy, _, _) ->
            if (busy) return@collectLatest
            delay(KEEP_TOGETHER_DELAY_MS)
            while (viewModel.keepVisibleTogether(lines.visibleIds(list), focused.value)) {
                // Ci si stava scrivendo: si riprova quando si smette.
                delay(PageEditorViewModel.TYPING_QUIET_MS)
            }
        }
    }
}
