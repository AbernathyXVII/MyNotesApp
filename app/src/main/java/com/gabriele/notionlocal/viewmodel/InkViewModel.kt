package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.ink.EraserMode
import com.gabriele.notionlocal.data.ink.InkBounds
import com.gabriele.notionlocal.data.ink.InkPoint
import com.gabriele.notionlocal.data.ink.boundsOf
import com.gabriele.notionlocal.data.ink.decodeInkPoints
import com.gabriele.notionlocal.data.ink.encodeInkPoints
import com.gabriele.notionlocal.data.ink.eraseFromStroke
import com.gabriele.notionlocal.data.ink.strokeTouched
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Un tratto pronto da disegnare: quello salvato, coi punti già letti e il suo rettangolo (dp). */
data class InkStroke(val entity: InkStrokeEntity, val points: List<InkPoint>, val bounds: InkBounds) {
    val id: String get() = entity.id
}

fun InkStrokeEntity.toStroke(): InkStroke {
    val points = decodeInkPoints(points)
    return InkStroke(this, points, boundsOf(points, inkHalfWidth(tool, width)))
}

/** Metà della larghezza più grande che il tratto può avere: la penna premuta a fondo si allarga. */
fun inkHalfWidth(tool: InkTool, width: Float): Float = when (tool) {
    InkTool.PEN, InkTool.PENCIL -> width * MAX_PRESSURE_FACTOR / 2f
    InkTool.MARKER, InkTool.HIGHLIGHTER -> width / 2f
}

/** Quanto la pressione cambia la penna: da poco più di metà (appena appoggiata) a una volta e mezza. */
fun pressureFactor(p: Float): Float = 0.55f + 0.9f * p.coerceIn(0f, 1f)
const val MAX_PRESSURE_FACTOR = 1.45f

/**
 * **I disegni a mano di una pagina** (dal 25/09/2026). Tiene i tratti in
 * memoria — sono quelli che si vedono — e li scrive nel database man mano.
 *
 * **Annulla e Ripeti sono suoi**, separati da quelli del testo: un tratto
 * è una modifica, una passata di gomma è una modifica (anche se ha toccato
 * dieci tratti, spezzandone qualcuno).
 */
class InkViewModel(private val db: AppDatabase) : ViewModel() {

    private val dao get() = db.inkDao()

    private val _strokes = MutableStateFlow<List<InkStroke>>(emptyList())
    val strokes: StateFlow<List<InkStroke>> = _strokes

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private var pageId: String? = null

    /** Una modifica: cosa è sparito e cosa è comparso. Annullarla è fare il contrario. */
    private data class InkEdit(val removed: List<InkStroke>, val added: List<InkStroke>)

    private val undoStack = ArrayDeque<InkEdit>()
    private val redoStack = ArrayDeque<InkEdit>()

    fun load(pageId: String) {
        if (this.pageId == pageId) return
        this.pageId = pageId
        viewModelScope.launch {
            _strokes.value = dao.getForPageOnce(pageId).map { it.toStroke() }
        }
    }

    fun addStroke(anchor: String, tool: InkTool, color: Long, width: Float, points: List<InkPoint>) {
        val pageId = pageId ?: return
        if (points.isEmpty()) return
        val stroke = InkStrokeEntity(
            pageId = pageId,
            anchor = anchor,
            tool = tool,
            color = color,
            width = width,
            points = encodeInkPoints(points)
        ).toStroke()
        apply(InkEdit(removed = emptyList(), added = listOf(stroke)), record = true)
    }

    // --- La gomma: una passata è una modifica sola ---

    /** I tratti che c'erano prima della passata e che la gomma ha tolto (o spezzato). */
    private val erasedOriginals = LinkedHashMap<String, InkStroke>()

    /** I pezzi nati durante la passata e ancora vivi. */
    private val erasePieces = LinkedHashMap<String, InkStroke>()

    /**
     * La gomma in (cx, cy), raggio `radius` — tutto in dp, nelle coordinate
     * della schermata. `originOf` dice dove sta adesso l'angolo a cui è
     * attaccato ogni tratto (null se non si vede). Cambia subito quello che
     * si vede; nel database finisce a passata finita (`endErase`).
     */
    fun eraseAt(cx: Float, cy: Float, radius: Float, mode: EraserMode, originOf: (InkStroke) -> Pair<Float, Float>?) {
        var changed = false
        val next = ArrayList<InkStroke>(_strokes.value.size)
        for (stroke in _strokes.value) {
            val origin = originOf(stroke)
            if (origin == null) {
                next += stroke
                continue
            }
            val x = cx - origin.first
            val y = cy - origin.second
            val b = stroke.bounds
            if (x < b.left - radius || x > b.right + radius || y < b.top - radius || y > b.bottom + radius) {
                next += stroke
                continue
            }
            val halfWidth = inkHalfWidth(stroke.entity.tool, stroke.entity.width)
            val pieces: List<List<InkPoint>>? = when (mode) {
                EraserMode.STROKE -> if (strokeTouched(stroke.points, halfWidth, x, y, radius)) emptyList() else null
                EraserMode.POINT -> eraseFromStroke(stroke.points, halfWidth, x, y, radius)
            }
            if (pieces == null) {
                next += stroke
                continue
            }
            changed = true
            if (erasePieces.remove(stroke.id) == null) erasedOriginals[stroke.id] = stroke
            for (piece in pieces) {
                // Stessa ora del tratto da cui vengono: restano sopra e sotto gli stessi tratti.
                val entity = stroke.entity.copy(id = UUID.randomUUID().toString(), points = encodeInkPoints(piece))
                val pieceStroke = entity.toStroke()
                erasePieces[pieceStroke.id] = pieceStroke
                next += pieceStroke
            }
        }
        if (changed) _strokes.value = next
    }

    fun endErase() {
        if (erasedOriginals.isEmpty() && erasePieces.isEmpty()) return
        val edit = InkEdit(removed = erasedOriginals.values.toList(), added = erasePieces.values.toList())
        erasedOriginals.clear()
        erasePieces.clear()
        // Già applicata sullo schermo: qui solo database e cronologia.
        persist(edit)
        push(edit)
    }

    // --- Annulla e Ripeti ---

    fun undo() {
        val edit = undoStack.removeLastOrNull() ?: return
        apply(InkEdit(removed = edit.added, added = edit.removed), record = false)
        redoStack.addLast(edit)
        refreshFlags()
    }

    fun redo() {
        val edit = redoStack.removeLastOrNull() ?: return
        apply(edit, record = false)
        undoStack.addLast(edit)
        refreshFlags()
    }

    private fun apply(edit: InkEdit, record: Boolean) {
        val removedIds = edit.removed.mapTo(HashSet()) { it.id }
        _strokes.value = (_strokes.value.filter { it.id !in removedIds } + edit.added)
            .sortedBy { it.entity.createdAt }
        persist(edit)
        if (record) push(edit)
    }

    private fun push(edit: InkEdit) {
        undoStack.addLast(edit)
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        refreshFlags()
    }

    private fun persist(edit: InkEdit) {
        viewModelScope.launch {
            dao.replace(edit.removed.map { it.id }, edit.added.map { it.entity })
        }
    }

    private fun refreshFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private companion object {
        const val MAX_UNDO = 100
    }
}
