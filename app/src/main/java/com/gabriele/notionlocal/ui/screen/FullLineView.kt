@file:OptIn(ExperimentalFoundationApi::class)

package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.settings.AnimationLevel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.NotionGray400
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

/**
 * **"Full line view": le righe lunghe non vanno a capo** (dal 19/10/2026,
 * chiesto dall'utente: "posso scorrere in orizzontale finché finisce il testo
 * così ogni campo si distribuisce per intero senza andare a capo"; Settings →
 * "Full line view", acceso di partenza, come ha chiesto).
 *
 * Ogni campo di testo della pagina — i gruppi di righe, e nelle isole le righe
 * dei toggle, i riquadri, le citazioni, le notifiche e il codice — è una
 * **corsia** ([SideLane]): il testo si misura senza limite di larghezza, e la
 * corsia ne mostra il pezzo che sta nel suo spazio. Tutte le corsie scorrono
 * **insieme**, dello stesso tratto ([SideScroll.offset]): le righe restano
 * allineate fra loro come su un foglio largo. Database, immagini, il titolo e
 * il resto stanno fermi.
 *
 * Fatta per non avere i difetti di OneNote, che l'utente ha in mente:
 *  - il dito scorre in verticale **oppure** di lato, mai di sbieco: il primo
 *    verso in cui si muove decide per tutto il gesto (la pagina e le righe
 *    sono due `scrollable` diversi, e il primo che parte si prende il gesto);
 *  - di lato si va solo fin dove finisce la riga più lunga **fra quelle che
 *    si vedono** ([SideScroll.reach]), più un po' d'aria, mai oltre. Scorrendo
 *    in giù verso righe più corte la pagina torna di lato da sola, quanto
 *    basta, invece di lasciare lo schermo vuoto;
 *  - scrivendo, il cursore resta in vista anche di lato; andando a capo si
 *    torna all'inizio delle righe ([SideResponder]);
 *  - sotto, mentre si scorre di lato, una barretta dice dove si è.
 */
@Stable
internal class SideScroll(
    private val listState: LazyListState,
    /** Quanto si è scorso di lato, in px: tenuto dalla pagina, sopravvive a un giro altrove. */
    private val saved: MutableFloatState
) {
    /** Di quanto le righe sono spostate a sinistra, in px. */
    var offset: Float
        get() = saved.floatValue
        set(value) {
            saved.floatValue = value
        }

    /** Le corsie della pagina: vedi [rememberSideLane]. */
    internal val lanes = mutableStateListOf<SideLane>()

    /**
     * Fin dove si può scorrere di lato adesso: quanto la riga più lunga fra
     * quelle sullo schermo (anche solo in parte) esce dalla sua corsia, con
     * l'aria in fondo. Si ricalcola scorrendo in verticale e scrivendo.
     */
    val reach: Float by derivedStateOf {
        if (lanes.isEmpty()) return@derivedStateOf 0f
        val info = listState.layoutInfo
        val shown = HashMap<Any, Pair<Float, Float>>()
        for (item in info.visibleItemsInfo) {
            shown[item.key] = (info.viewportStartOffset - item.offset).toFloat() to
                (info.viewportEndOffset - item.offset).toFloat()
        }
        var best = 0f
        for (lane in lanes) {
            val (top, bottom) = shown[lane.itemKey] ?: continue
            best = maxOf(best, lane.overflow(top, bottom))
        }
        best
    }

    /** Il dito che scorre di lato: mai prima dell'inizio né oltre [reach]. */
    val scrollableState = ScrollableState { delta ->
        val limit = reach
        val start = offset.coerceIn(0f, limit)
        val next = (start + delta).coerceIn(0f, limit)
        offset = next
        next - start
    }

    /**
     * Torna di lato fino a [target], con un'animazione breve. Si ferma se nel
     * frattempo qualcun altro sposta le righe (il cursore che va portato in
     * vista), e la interrompe il dito.
     */
    suspend fun settleTo(target: Float) {
        scrollableState.scroll {
            if (AppSettings.animations == AnimationLevel.OFF) {
                offset = target
                return@scroll
            }
            var written = offset
            var stopped = false
            animate(offset, target, animationSpec = tween(SIDE_SETTLE_MS)) { value, _ ->
                if (stopped || offset != written) {
                    stopped = true
                } else {
                    offset = value
                    written = value
                }
            }
        }
    }
}

/** La pagina con le righe intere, o null se in Settings "Full line view" è spento. */
internal val LocalSideScroll = staticCompositionLocalOf<SideScroll?> { null }

/** La voce della lista della pagina in cui sta un campo: serve a sapere se si vede. */
internal val LocalSideItemKey = compositionLocalOf<Any?> { null }

/** L'aria dopo la fine di una riga, e dal bordo per il cursore che scrive. */
private val SIDE_END_ROOM = 32.dp

/** Quanto dura il ritorno di lato quando le righe sullo schermo diventano più corte. */
private const val SIDE_SETTLE_MS = 220

/** Quanto resta la barretta dopo che le righe si sono fermate. */
private const val SIDE_INDICATOR_LINGER_MS = 900L

/**
 * **Una corsia**: un campo di testo che scorre di lato con gli altri. La
 * misura la fa [sideLane]; chi la usa può dire fin dove arrivano le sue righe
 * in una fascia d'altezza ([rightEdge]), altrimenti conta tutta la larghezza.
 */
@Stable
internal class SideLane(val scroll: SideScroll, val itemKey: Any, private val endRoom: Float) {
    /** Lo spazio della corsia sullo schermo, in px. */
    var viewport by mutableIntStateOf(0)

    /** Quanto è largo il contenuto, in px: almeno [viewport]. */
    var contentWidth by mutableIntStateOf(0)

    /**
     * Fin dove arrivano le righe che stanno fra `top` e `bottom` (px, nelle
     * coordinate della voce della lista), nelle coordinate del contenuto della
     * corsia; 0 se ci stanno senza scorrere. Null per contare tutto il
     * contenuto. Vedi `MergedTextRunField`.
     */
    var rightEdge: ((top: Float, bottom: Float) -> Float)? = null

    /** Quanto questa corsia esce dal suo spazio, con l'aria in fondo; 0 se ci sta. */
    fun overflow(top: Float, bottom: Float): Float {
        val end = rightEdge?.invoke(top, bottom)
            ?: if (contentWidth > viewport) contentWidth.toFloat() else 0f
        if (end <= 0f) return 0f
        return (end + endRoom - viewport).coerceAtLeast(0f)
    }

    val responder = SideResponder(this, endRoom)
}

/**
 * Il cursore in vista anche di lato: quando un campo chiede di mostrare un
 * rettangolo stretto (il cursore, la fine della selezione), le righe scorrono
 * quanto basta per averlo a [margin] dal bordo. Un rettangolo più largo dello
 * spazio (tutto il campo, che chiede il fuoco) non le sposta: altrimenti
 * toccando una riga lunga a metà si tornerebbe al suo inizio.
 */
internal class SideResponder(private val lane: SideLane, private val margin: Float) : BringIntoViewResponder {
    override fun calculateRectForParent(localRect: Rect): Rect = localRect.translate(-shiftFor(localRect), 0f)

    override suspend fun bringChildIntoView(localRect: () -> Rect?) {
        val rect = localRect() ?: return
        val shift = shiftFor(rect)
        if (shift != 0f) lane.scroll.offset = (lane.scroll.offset + shift).coerceAtLeast(0f)
    }

    private fun shiftFor(rect: Rect): Float {
        val viewport = lane.viewport.toFloat()
        if (viewport <= 0f) return 0f
        val room = margin.coerceAtMost(viewport / 4)
        if (rect.width > viewport - 2 * room) return 0f
        val shift = when {
            rect.left < room -> rect.left - room
            rect.right > viewport - room -> rect.right - (viewport - room)
            else -> 0f
        }
        return shift.coerceAtLeast(-lane.scroll.offset)
    }
}

/** La corsia di un campo di testo della pagina, o null se non scorre di lato. */
@Composable
internal fun rememberSideLane(): SideLane? {
    val scroll = LocalSideScroll.current ?: return null
    val itemKey = LocalSideItemKey.current ?: return null
    val endRoom = with(LocalDensity.current) { SIDE_END_ROOM.toPx() }
    val lane = remember(scroll, itemKey, endRoom) { SideLane(scroll, itemKey, endRoom) }
    DisposableEffect(lane) {
        scroll.lanes.add(lane)
        onDispose { scroll.lanes.remove(lane) }
    }
    return lane
}

/**
 * Il campo dentro la sua corsia: misurato senza limite di larghezza (almeno
 * quanto lo spazio che ha), spostato di [SideScroll.offset] e tagliato ai
 * bordi. Lo spostamento si legge solo al momento di posare il campo: scorrere
 * di lato non rifà né il testo né la sua misura.
 */
internal fun Modifier.sideLane(lane: SideLane?): Modifier = if (lane == null) {
    this
} else {
    this
        .clipToBounds()
        .bringIntoViewResponder(lane.responder)
        .layout { measurable, constraints ->
            val viewport = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
            val placeable = measurable.measure(constraints.copy(minWidth = viewport, maxWidth = Constraints.Infinity))
            if (lane.viewport != viewport) lane.viewport = viewport
            if (lane.contentWidth != placeable.width) lane.contentWidth = placeable.width
            layout(viewport, placeable.height) {
                placeable.place(-lane.scroll.offset.roundToInt(), 0)
            }
        }
}

/**
 * Il campo largo almeno quanto la sua corsia, meno [insetPx]: dentro un
 * riquadro che non passa la larghezza minima ai figli, così si tocca anche
 * accanto alle righe corte, come prima.
 */
internal fun Modifier.atLeastLane(lane: SideLane?, insetPx: Int): Modifier = if (lane == null) {
    this
} else {
    layout { measurable, constraints ->
        val min = maxOf(constraints.minWidth, lane.viewport - insetPx).coerceIn(0, constraints.maxWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = min))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

/** Il dito che scorre di lato sulla pagina; spento mentre si disegna. */
internal fun Modifier.sideScrollGestures(scroll: SideScroll?, enabled: Boolean): Modifier = if (scroll == null) {
    this
} else {
    scrollable(scroll.scrollableState, Orientation.Horizontal, enabled = enabled, reverseDirection = true)
}

/**
 * Sulle isole (database, immagini…): quello che il loro scorrere di lato
 * avanza, arrivate in fondo, non passa alla pagina. Una tabella arrivata al
 * bordo non si trascina dietro il testo intorno.
 */
internal object KeepSidewaysInside : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        Offset(available.x, 0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(available.x, 0f)
}

/**
 * Quello che fa la pagina da sé: quando le righe sullo schermo diventano più
 * corte di dove si è, torna di lato quanto basta; mentre si disegna torna
 * all'inizio delle righe, così i tratti nuovi si attaccano al testo dov'è.
 */
@Composable
internal fun SideScrollEffects(scroll: SideScroll, drawing: Boolean) {
    LaunchedEffect(scroll, drawing) {
        if (drawing) {
            if (scroll.offset > 0f) scroll.settleTo(0f)
            return@LaunchedEffect
        }
        snapshotFlow { scroll.reach }.collectLatest { reach ->
            if (scroll.offset > reach + 0.5f) scroll.settleTo(reach)
        }
    }
}

/**
 * La barretta in fondo alla pagina che dice dove si è di lato: compare mentre
 * le righe scorrono e svanisce poco dopo che si sono fermate. **Blu, su una
 * traccia lunga quanto lo schermo**: grigia e corta, appena sopra la barra di
 * sistema del telefono, sembrava una seconda barra di sistema (visto sul
 * telefono il 19/10/2026).
 */
@Composable
internal fun SideScrollIndicator(scroll: SideScroll, modifier: Modifier = Modifier) {
    var moving by remember(scroll) { mutableStateOf(false) }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.offset }.drop(1).collectLatest {
            moving = true
            delay(SIDE_INDICATOR_LINGER_MS)
            moving = false
        }
    }
    val alpha by animateFloatAsState(if (moving) 1f else 0f, tween(200), label = "side-indicator")
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        val reach = scroll.reach
        if (alpha <= 0f || reach <= 0.5f) return@Canvas
        val track = size.width
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(color = NotionGray400.copy(alpha = 0.25f * alpha), size = size, cornerRadius = corner)
        val thumb = (track * track / (track + reach)).coerceIn(24.dp.toPx().coerceAtMost(track), track)
        val x = (track - thumb) * (scroll.offset.coerceIn(0f, reach) / reach)
        drawRoundRect(
            color = AccentBlue.copy(alpha = alpha),
            topLeft = Offset(x, 0f),
            size = Size(thumb, size.height),
            cornerRadius = corner
        )
    }
}
