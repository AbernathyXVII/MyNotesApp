package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.reorderedAs
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant

/**
 * **I tag da riordinare** (dal 20/10/2026, chiesto dall'utente: le maniglie a
 * tre lineette, come in molte app). Si trascina la maniglia a destra di un
 * tag, e i vicini gli fanno posto appena il tag arriva a metà di uno di loro.
 * L'ordine si salva a ogni rilascio: chiudere la finestra non perde niente.
 *
 * Vicino al bordo di sopra o di sotto della finestra l'elenco scorre da solo,
 * per portare un tag oltre quelli che non si vedono. `viewport` è il riquadro
 * della finestra che scorre, in coordinate dello schermo.
 */
@Composable
internal fun TagReorderList(
    options: List<SelectOption>,
    onMove: (List<String>) -> Unit,
    scrollState: ScrollState,
    viewport: () -> Rect?
) {
    val labels = options.map { it.label }
    // L'ordine sullo schermo vale più di quello del database, che dopo un
    // rilascio arriva un attimo dopo: se lo seguisse, il tag tornerebbe per
    // un attimo dov'era.
    var order by remember { mutableStateOf(labels) }
    LaunchedEffect(labels) { order = options.reorderedAs(order).map { it.label } }

    var dragged by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var orderAtStart by remember { mutableStateOf(order) }
    // Dove sta ogni tag senza lo spostamento del dito, e quanto è alto.
    val tops = remember { HashMap<String, Float>() }
    val heights = remember { HashMap<String, Int>() }
    val currentOnMove by rememberUpdatedState(onMove)

    val density = LocalDensity.current
    val dividerPx = with(density) { DividerDefaults.Thickness.toPx() }
    val edgePx = with(density) { 56.dp.toPx() }
    val maxScrollPx = with(density) { 12.dp.toPx() }

    fun settle() {
        val label = dragged ?: return
        val (newOrder, rest) = moveWhileDragging(order, label, offset) { (heights[it] ?: 0) + dividerPx }
        order = newOrder
        offset = rest
    }

    fun finish() {
        if (dragged == null) return
        dragged = null
        offset = 0f
        if (order != orderAtStart) currentOnMove(order)
    }

    LaunchedEffect(dragged) {
        val label = dragged ?: return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val top = (tops[label] ?: continue) + offset
            val bottom = top + (heights[label] ?: 0)
            val box = viewport() ?: continue
            val step = when {
                bottom > box.bottom - edgePx -> maxScrollPx * ((bottom - box.bottom + edgePx) / edgePx).coerceAtMost(1f)
                top < box.top + edgePx -> -maxScrollPx * ((box.top + edgePx - top) / edgePx).coerceAtMost(1f)
                else -> 0f
            }
            if (step != 0f) {
                // Il contenuto sale (o scende) sotto al dito: il tag lo segue.
                offset += scrollState.scrollBy(step)
                settle()
            }
        }
    }

    SheetGroup {
        order.forEachIndexed { index, label ->
            val option = options.find { it.label == label } ?: return@forEachIndexed
            key(label) {
                if (index > 0) HorizontalDivider()
                val lifted = label == dragged
                Box(
                    modifier = Modifier
                        .zIndex(if (lifted) 1f else 0f)
                        .onGloballyPositioned {
                            tops[label] = it.positionInRoot().y
                            heights[label] = it.size.height
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = if (lifted) offset else 0f
                                shadowElevation = if (lifted) 8.dp.toPx() else 0f
                                shape = RoundedCornerShape(6.dp)
                            }
                            .then(if (lifted) Modifier.background(DarkSurfaceVariant, RoundedCornerShape(6.dp)) else Modifier)
                            .padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TagChip(label = option.label, option = option)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = DbStrings.dragToReorder,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .pointerInput(label) {
                                    detectDragGestures(
                                        onDragStart = {
                                            orderAtStart = order
                                            offset = 0f
                                            dragged = label
                                        },
                                        onDragEnd = { finish() },
                                        onDragCancel = { finish() }
                                    ) { change, amount ->
                                        change.consume()
                                        offset += amount.y
                                        settle()
                                    }
                                }
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dove finisce il tag trascinato: passa oltre ogni vicino di cui ha superato
 * la metà. Restituisce l'ordine nuovo e quanto il tag resta spostato dal suo
 * posto nuovo. `slot` è l'altezza di un tag col suo filo divisorio.
 */
internal fun moveWhileDragging(
    order: List<String>,
    dragged: String,
    offset: Float,
    slot: (String) -> Float
): Pair<List<String>, Float> {
    val list = order.toMutableList()
    var i = list.indexOf(dragged)
    if (i < 0) return order to offset
    var rest = offset
    while (rest > 0 && i < list.lastIndex) {
        val step = slot(list[i + 1])
        if (rest < step / 2) break
        list[i] = list[i + 1]
        list[i + 1] = dragged
        rest -= step
        i++
    }
    while (rest < 0 && i > 0) {
        val step = slot(list[i - 1])
        if (-rest < step / 2) break
        list[i] = list[i - 1]
        list[i - 1] = dragged
        rest += step
        i--
    }
    return list to rest
}
