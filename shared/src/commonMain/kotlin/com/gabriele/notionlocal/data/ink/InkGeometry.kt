package com.gabriele.notionlocal.data.ink

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * **I conti dei disegni a mano**, senza Android: come si salvano i punti,
 * cosa tocca la gomma, e a cosa è attaccato un tratto. Le coordinate sono
 * in dp, contate dall'angolo in alto a sinistra di quello a cui il tratto
 * è attaccato (vedi `InkStrokeEntity`).
 */

/** Un punto di un tratto. `p`: la pressione, fra 0 e 1 (0,5 per il dito, che non la dà). */
data class InkPoint(val x: Float, val y: Float, val p: Float = NEUTRAL_PRESSURE)

const val NEUTRAL_PRESSURE = 0.5f

/**
 * I punti in una stringa: "x,y,p;x,y,p", in dp con un decimale e la
 * pressione in centesimi. Più corta del JSON di cinque volte, e un tratto
 * ha centinaia di punti.
 */
fun encodeInkPoints(points: List<InkPoint>): String =
    points.joinToString(";") { "${oneDecimal(it.x)},${oneDecimal(it.y)},${(it.p * 100).roundToInt()}" }

fun decodeInkPoints(text: String): List<InkPoint> {
    if (text.isBlank()) return emptyList()
    return text.split(';').mapNotNull { part ->
        val values = part.split(',')
        val x = values.getOrNull(0)?.toFloatOrNull() ?: return@mapNotNull null
        val y = values.getOrNull(1)?.toFloatOrNull() ?: return@mapNotNull null
        val p = values.getOrNull(2)?.toIntOrNull()?.let { it / 100f } ?: NEUTRAL_PRESSURE
        InkPoint(x, y, p)
    }
}

private fun oneDecimal(value: Float): String {
    val tenths = (value * 10).roundToInt()
    val whole = tenths / 10
    val rest = kotlin.math.abs(tenths % 10)
    return if (rest == 0) whole.toString() else "${if (tenths < 0 && whole == 0) "-" else ""}$whole.$rest"
}

/** Il rettangolo che contiene un tratto, per saltare quelli fuori dallo schermo. */
data class InkBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

fun boundsOf(points: List<InkPoint>, halfWidth: Float): InkBounds {
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    for (point in points) {
        left = min(left, point.x)
        top = min(top, point.y)
        right = max(right, point.x)
        bottom = max(bottom, point.y)
    }
    return InkBounds(left - halfWidth, top - halfWidth, right + halfWidth, bottom + halfWidth)
}

/** La distanza di (px, py) dal segmento a-b. */
fun distanceToSegment(px: Float, py: Float, a: InkPoint, b: InkPoint): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0f) return hypot(px - a.x, py - a.y)
    val t = (((px - a.x) * dx + (py - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypot(px - (a.x + t * dx), py - (a.y + t * dy))
}

/**
 * **La gomma "Stroke Eraser"**: il tratto si cancella tutto se la gomma
 * (un cerchio di raggio `radius` in (cx, cy)) ne tocca un punto qualsiasi,
 * contando la sua larghezza.
 */
fun strokeTouched(points: List<InkPoint>, halfWidth: Float, cx: Float, cy: Float, radius: Float): Boolean {
    val reach = radius + halfWidth
    if (points.size == 1) return hypot(cx - points[0].x, cy - points[0].y) <= reach
    for (i in 1 until points.size) {
        if (distanceToSegment(cx, cy, points[i - 1], points[i]) <= reach) return true
    }
    return false
}

/**
 * **La gomma "Point Eraser"**: toglie dal tratto solo quello che sta sotto
 * la gomma, e il tratto si spezza nei pezzi che restano. Null se la gomma
 * non l'ha toccato (il tratto resta com'è); una lista vuota se l'ha
 * cancellato tutto.
 *
 * Prima il tratto si **infittisce** — un punto ogni `step` — altrimenti
 * fra due punti lontani (col dito i punti sono radi) la gomma potrebbe
 * passare in mezzo senza toglierne nessuno. Di un pezzo rimasto con un
 * punto solo non resterebbe che un puntino: si butta.
 */
fun eraseFromStroke(
    points: List<InkPoint>,
    halfWidth: Float,
    cx: Float,
    cy: Float,
    radius: Float,
    step: Float = max(0.5f, radius / 3f)
): List<List<InkPoint>>? {
    if (!strokeTouched(points, halfWidth, cx, cy, radius)) return null
    if (points.size == 1) return emptyList()
    val dense = densify(points, step)
    val pieces = mutableListOf<List<InkPoint>>()
    var current = mutableListOf<InkPoint>()
    for (point in dense) {
        if (hypot(point.x - cx, point.y - cy) <= radius) {
            if (current.size >= 2) pieces += current
            current = mutableListOf()
        } else {
            current += point
        }
    }
    if (current.size >= 2) pieces += current
    return pieces
}

/** Aggiunge punti in mezzo dove due punti sono più lontani di `step`. */
fun densify(points: List<InkPoint>, step: Float): List<InkPoint> {
    if (points.size < 2) return points
    val result = ArrayList<InkPoint>(points.size * 2)
    result += points[0]
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val distance = hypot(b.x - a.x, b.y - a.y)
        val parts = (distance / step).toInt()
        for (k in 1 until parts) {
            val t = k.toFloat() / parts
            result += InkPoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.p + (b.p - a.p) * t)
        }
        result += b
    }
    return result
}

/** Gli id di un ancoraggio (`InkStrokeEntity.anchor`). */
fun anchorIds(anchor: String): List<String> = anchor.split(',').filter { it.isNotEmpty() }

/**
 * **A cosa è attaccato un tratto adesso**: il primo dei suoi blocchi che
 * c'è ancora, fra quelli che la pagina mostra (`itemOfBlock`: blocco →
 * voce della lista). Null se non ce n'è più nessuno.
 */
fun resolveAnchor(anchor: String, itemOfBlock: Map<String, Int>): Int? {
    for (id in anchorIds(anchor)) {
        itemOfBlock[id]?.let { return it }
    }
    return null
}

/** L'ancoraggio di una copia della pagina: gli id dei blocchi tradotti in quelli della copia. */
fun remapAnchor(anchor: String, newIds: Map<String, String>): String =
    anchorIds(anchor).map { newIds[it] ?: it }.joinToString(",")
