package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.entity.ChartHeight
import com.gabriele.notionlocal.data.entity.ChartLabels
import com.gabriele.notionlocal.data.entity.ChartPoint
import com.gabriele.notionlocal.data.entity.ChartSettings
import com.gabriele.notionlocal.data.entity.ChartType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.NumberSettings
import com.gabriele.notionlocal.data.entity.chartPoints
import com.gabriele.notionlocal.data.entity.chartAxis
import com.gabriele.notionlocal.data.entity.compactNumber
import com.gabriele.notionlocal.data.entity.dateSettings
import com.gabriele.notionlocal.data.entity.decorate
import com.gabriele.notionlocal.data.entity.numberSettings
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.ChartStrings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * I colori di "Color: Auto", uno per barra, come su Notion: azzurro,
 * giallo, verde, viola, e poi gli altri a giro. Sono anche i colori fra cui
 * scegliere quando se ne vuole uno solo.
 */
internal val CHART_PALETTE = listOf(
    "Blue" to 0xFF529CCA,
    "Yellow" to 0xFFE5B85C,
    "Green" to 0xFF6BB38A,
    "Purple" to 0xFFB287D1,
    "Red" to 0xFFE0736B,
    "Orange" to 0xFFE89C5C,
    "Pink" to 0xFFD97DAE,
    "Teal" to 0xFF5FB8B8,
    "Brown" to 0xFFA88B72,
    "Gray" to 0xFF9B9A97
)

internal fun chartColorOf(hex: String?): Color? =
    hex?.removePrefix("#")?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }

internal fun chartHex(argb: Long): String = "#%06X".format(argb and 0xFFFFFF)

private fun ChartHeight.dp(): Dp = when (this) {
    ChartHeight.SMALL -> 180.dp
    ChartHeight.MEDIUM -> 260.dp
    ChartHeight.LARGE -> 340.dp
    ChartHeight.EXTRA_LARGE -> 440.dp
}

/**
 * **La vista Chart** (dal 28/09/2026): le righe del database, filtrate come
 * dice la vista, diventano barre verticali o orizzontali, una linea o una
 * ciambella, secondo `ChartSettings`. I conti stanno in `chartPoints`.
 */
@Composable
internal fun ChartLayout(state: DatabaseTableState, settings: ChartSettings) {
    val valueColumn = settings.valueColumnId?.let { id -> state.columns.find { it.id == id && it.type == ColumnType.NUMBER } }
    val labels = ChartLabels(
        untitled = Strings.untitled,
        empty = ChartStrings.empty,
        checked = ChartStrings.checked,
        unchecked = ChartStrings.unchecked,
        day = { day -> Formats.date(day) }
    )
    val points = remember(state.rows, state.columns, state.cellValues, settings, AppSettings.zoneId) {
        chartPoints(state.rows, state.columns, state.cellValues, settings, labels, AppSettings.zoneId)
    }
    val formatter = remember(valueColumn, settings.decimals, settings.aggregation) { ChartFormatter(valueColumn, settings) }
    val seriesName = valueColumn?.let { DbStrings.columnName(it.name) } ?: ChartStrings.count

    if (points.isEmpty()) {
        Text(
            ChartStrings.nothingToShow,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        when (settings.type) {
            ChartType.VERTICAL, ChartType.LINE -> ColumnChart(points, settings, formatter)
            ChartType.HORIZONTAL -> BarChart(points, settings, formatter)
            ChartType.DONUT -> DonutChart(points, settings, formatter, seriesName)
        }
        if (settings.showLegend && settings.type == ChartType.LINE) {
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(2.dp)
                        .background(chartColorOf(settings.color) ?: Color(CHART_PALETTE[0].second))
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(seriesName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Come si scrivono i numeri del grafico: col formato della proprietà (valuta, %), e le cifre scelte. */
internal class ChartFormatter(private val valueColumn: DatabaseColumnEntity?, settings: ChartSettings) {
    private val number: NumberSettings? = valueColumn?.numberSettings()?.let { base ->
        if (settings.decimals != null) base.copy(decimals = settings.decimals) else base
    }
    private val decimals = settings.decimals

    fun format(value: Double): String {
        val plain = plain(value)
        return if (number == null) {
            Formats.number(plain)
        } else {
            formatNumberValue(plain, number)
        }
    }

    /** "$1K": il totale al centro della ciambella. */
    fun compact(value: Double): String {
        val short = compactNumber(value)
        return number?.format?.decorate(short) ?: short
    }

    private fun plain(value: Double): String {
        // Una media può avere infinite cifre: senza una scelta, al massimo due.
        val scale = decimals ?: 2
        return BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).let {
            if (decimals == null) it.stripTrailingZeros() else it
        }.toPlainString()
    }
}

private fun paletteColor(index: Int, settings: ChartSettings): Color =
    chartColorOf(settings.color) ?: Color(CHART_PALETTE[index % CHART_PALETTE.size].second)

/**
 * Le fette della ciambella: con un colore scelto ognuna ne prende una
 * sfumatura diversa, verso lo sfondo, se no sarebbero tutte uguali.
 */
private fun sliceColor(index: Int, settings: ChartSettings, background: Color): Color {
    val base = chartColorOf(settings.color) ?: return paletteColor(index, settings)
    return lerp(base, background, (index % 5) * 0.16f)
}

/**
 * Un numero sopra una barra o un punto, con attorno un bordino del colore
 * dello sfondo: la griglia e la linea che ci passano sotto non lo tagliano.
 */
private fun DrawScope.drawLabel(text: TextLayoutResult, topLeft: Offset, halo: Color) {
    drawText(text, color = halo, topLeft = topLeft, drawStyle = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round))
    // Colore e riempimento detti di nuovo: il pennello si ricorda quelli del bordino.
    drawText(text, color = text.layoutInput.style.color, topLeft = topLeft, drawStyle = Fill)
}

/** Barre verticali e linea: gruppi in basso, numeri a sinistra. */
@Composable
private fun ColumnChart(points: List<ChartPoint>, settings: ChartSettings, formatter: ChartFormatter) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val referenceColor = Color(0xFFE89C5C)
    val small = TextStyle(fontSize = 11.sp, color = labelColor)
    val halo = MaterialTheme.colorScheme.background
    val density = LocalDensity.current

    val (axisLow, axisHigh, ticks) = chartAxis(points, settings)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val minSlot = 44.dp
        val tickLabels = ticks.map { measurer.measure(formatter.format(it), small) }
        val axisWidthPx = (tickLabels.maxOf { it.size.width } + with(density) { 8.dp.toPx() }).toInt()
        val available = maxWidth
        val needed = with(density) { axisWidthPx.toDp() } + minSlot * points.size
        val width = if (needed > available) needed else available
        val scroll = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            Canvas(modifier = Modifier.width(width).height(settings.height.dp())) {
                val top = 22.dp.toPx()
                val bottom = size.height - 26.dp.toPx()
                val left = axisWidthPx.toFloat()
                val right = size.width - 4.dp.toPx()
                val span = (axisHigh - axisLow).takeIf { it > 0 } ?: 1.0
                fun y(v: Double) = (bottom - (v.coerceIn(axisLow, axisHigh) - axisLow) / span * (bottom - top)).toFloat()

                // Tacche, numeri a sinistra e griglia.
                ticks.forEachIndexed { i, tick ->
                    if (tick < axisLow || tick > axisHigh) return@forEachIndexed
                    val ty = y(tick)
                    val layout = tickLabels[i]
                    drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), ty - layout.size.height / 2))
                    if (settings.gridLines || tick == axisLow) {
                        drawLine(
                            gridColor, Offset(left, ty), Offset(right, ty),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (tick == axisLow) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                        )
                    }
                }

                val slot = (right - left) / points.size
                val centers = points.indices.map { left + slot * (it + 0.5f) }
                val baseline = y(0.0.coerceIn(axisLow, axisHigh))

                if (settings.type == ChartType.LINE) {
                    val color = chartColorOf(settings.color) ?: Color(CHART_PALETTE[0].second)
                    val pts = points.mapIndexed { i, p -> Offset(centers[i], y(p.value)) }
                    val line = linePath(pts, settings.smoothLine)
                    val area = Path().apply {
                        addPath(line)
                        lineTo(pts.last().x, baseline)
                        lineTo(pts.first().x, baseline)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f)), startY = top, endY = baseline))
                    drawPath(line, color, style = Stroke(width = 2.dp.toPx()))
                    if (settings.dataLabels) {
                        points.forEachIndexed { i, p ->
                            val text = measurer.measure(formatter.format(p.value), small)
                            drawLabel(text, Offset(pts[i].x - text.size.width / 2, pts[i].y - text.size.height - 4.dp.toPx()), halo)
                        }
                    }
                } else {
                    val barWidth = min(slot * 0.4f, 22.dp.toPx())
                    points.forEachIndexed { i, p ->
                        val valueY = y(p.value)
                        val topY = min(valueY, baseline)
                        val height = abs(baseline - valueY)
                        drawRoundRect(
                            color = paletteColor(p.colorIndex, settings),
                            topLeft = Offset(centers[i] - barWidth / 2, topY),
                            size = Size(barWidth, max(height, 1f)),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                        if (settings.dataLabels) {
                            val text = measurer.measure(formatter.format(p.value), small)
                            val ly = if (p.value >= 0) topY - text.size.height - 3.dp.toPx() else topY + height + 3.dp.toPx()
                            drawLabel(text, Offset(centers[i] - text.size.width / 2, ly), halo)
                        }
                    }
                }

                // I nomi dei gruppi sotto, tagliati se non ci stanno.
                points.forEachIndexed { i, p ->
                    val text = measureOneLine(measurer, p.label, small, (slot - 4.dp.toPx()).toInt())
                    drawText(text, topLeft = Offset(centers[i] - text.size.width / 2, bottom + 6.dp.toPx()))
                }

                referenceLines(settings, measurer, small.copy(color = referenceColor), referenceColor, horizontal = true, left, right, top, bottom) { y(it) }
            }
        }
    }
}

/** Barre orizzontali: gruppi a sinistra, numeri in basso. */
@Composable
private fun BarChart(points: List<ChartPoint>, settings: ChartSettings, formatter: ChartFormatter) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val referenceColor = Color(0xFFE89C5C)
    val small = TextStyle(fontSize = 11.sp, color = labelColor)
    val halo = MaterialTheme.colorScheme.background

    val (axisLow, axisHigh, ticks) = chartAxis(points, settings)
    val rowHeight = 34.dp
    val chartHeight = max(settings.height.dp().value, (rowHeight * points.size + 30.dp).value).dp

    Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
        val maxLabel = (size.width * 0.3f).toInt()
        val names = points.map { measureOneLine(measurer, it.label, small, maxLabel) }
        val left = (names.maxOf { it.size.width } + 10.dp.toPx())
        // Spazio a destra per il numero in fondo all'ultima barra.
        val right = size.width - 44.dp.toPx()
        val top = 4.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val span = (axisHigh - axisLow).takeIf { it > 0 } ?: 1.0
        fun x(v: Double) = (left + (v.coerceIn(axisLow, axisHigh) - axisLow) / span * (right - left)).toFloat()

        ticks.forEach { tick ->
            if (tick < axisLow || tick > axisHigh) return@forEach
            val tx = x(tick)
            val text = measurer.measure(formatter.format(tick), small)
            drawText(text, topLeft = Offset(tx - text.size.width / 2, bottom + 6.dp.toPx()))
            if (settings.gridLines || tick == axisLow) {
                drawLine(
                    gridColor, Offset(tx, top), Offset(tx, bottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (tick == axisLow) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                )
            }
        }

        val slot = (bottom - top) / points.size
        val thickness = min(slot * 0.5f, 16.dp.toPx())
        val zero = x(0.0.coerceIn(axisLow, axisHigh))
        points.forEachIndexed { i, p ->
            val cy = top + slot * (i + 0.5f)
            val name = names[i]
            drawText(name, topLeft = Offset(left - name.size.width - 8.dp.toPx(), cy - name.size.height / 2))
            val end = x(p.value)
            drawRoundRect(
                color = paletteColor(p.colorIndex, settings),
                topLeft = Offset(min(zero, end), cy - thickness / 2),
                size = Size(max(abs(end - zero), 1f), thickness),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            if (settings.dataLabels) {
                val text = measurer.measure(formatter.format(p.value), small)
                drawLabel(text, Offset(max(zero, end) + 4.dp.toPx(), cy - text.size.height / 2), halo)
            }
        }

        referenceLines(settings, measurer, small.copy(color = referenceColor), referenceColor, horizontal = false, left, right, top, bottom) { x(it) }
    }
}

/** La ciambella: una fetta per gruppo, il totale al centro, i nomi nella legenda. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonutChart(points: List<ChartPoint>, settings: ChartSettings, formatter: ChartFormatter, seriesName: String) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strong = MaterialTheme.colorScheme.onBackground
    val small = TextStyle(fontSize = 11.sp, color = labelColor)
    val halo = MaterialTheme.colorScheme.background
    // Solo i valori sopra zero fanno una fetta: una fetta negativa non esiste.
    val slices = points.filter { it.value > 0 }
    val total = slices.sumOf { it.value }

    Canvas(modifier = Modifier.fillMaxWidth().height(settings.height.dp())) {
        val labelRoom = if (settings.dataLabels) 34.dp.toPx() else 6.dp.toPx()
        val radius = (min(size.width, size.height) / 2 - labelRoom).coerceAtLeast(20.dp.toPx())
        val ring = min(22.dp.toPx(), radius * 0.35f)
        val center = Offset(size.width / 2, size.height / 2)
        var angle = -90f
        slices.forEach { p ->
            val sweep = (p.value / total * 360).toFloat()
            val color = sliceColor(p.colorIndex, settings, halo)
            drawArc(
                color = color,
                startAngle = angle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = ring)
            )
            if (settings.dataLabels) {
                val mid = Math.toRadians((angle + sweep / 2).toDouble())
                val edge = radius + ring / 2
                val from = Offset(center.x + (edge * cos(mid)).toFloat(), center.y + (edge * sin(mid)).toFloat())
                val to = Offset(center.x + ((edge + 10.dp.toPx()) * cos(mid)).toFloat(), center.y + ((edge + 10.dp.toPx()) * sin(mid)).toFloat())
                drawLine(labelColor.copy(alpha = 0.5f), from, to, strokeWidth = 1.dp.toPx())
                val percent = BigDecimal(p.value / total * 100).setScale(0, RoundingMode.HALF_UP).toPlainString()
                val text = measurer.measure("${formatter.format(p.value)} ($percent%)", small)
                val tx = if (cos(mid) >= 0) to.x + 3.dp.toPx() else to.x - text.size.width - 3.dp.toPx()
                drawText(text, topLeft = Offset(tx, to.y - text.size.height / 2))
            }
            angle += sweep
        }
        if (settings.showTotal) {
            val big = measurer.measure(formatter.compact(total), TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = strong))
            val caption = measureOneLine(measurer, ChartStrings.total(seriesName), small, ((radius - ring) * 1.8f).toInt())
            val totalHeight = big.size.height + caption.size.height
            drawText(big, topLeft = Offset(center.x - big.size.width / 2, center.y - totalHeight / 2))
            drawText(caption, topLeft = Offset(center.x - caption.size.width / 2, center.y - totalHeight / 2 + big.size.height))
        }
    }
    if (settings.showLegend) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            slices.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(sliceColor(p.colorIndex, settings, halo), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(p.label, style = MaterialTheme.typography.labelMedium, color = labelColor, maxLines = 1)
                }
            }
        }
    }
}

/** La linea che passa per i punti: spezzata, o morbida con "Smooth line". */
private fun linePath(points: List<Offset>, smooth: Boolean): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        if (smooth) {
            val midX = (prev.x + cur.x) / 2
            cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
        } else {
            lineTo(cur.x, cur.y)
        }
    }
}

/** Le linee di riferimento, tratteggiate, col loro nome in fondo. */
private fun DrawScope.referenceLines(
    settings: ChartSettings,
    measurer: TextMeasurer,
    style: TextStyle,
    color: Color,
    horizontal: Boolean,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    position: (Double) -> Float
) {
    settings.referenceLines.forEach { line ->
        val at = position(line.value)
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
        if (horizontal) {
            drawLine(color, Offset(left, at), Offset(right, at), strokeWidth = 1.5.dp.toPx(), pathEffect = dash)
        } else {
            drawLine(color, Offset(at, top), Offset(at, bottom), strokeWidth = 1.5.dp.toPx(), pathEffect = dash)
        }
        if (line.label.isNotBlank()) {
            val text = measurer.measure(line.label, style)
            val topLeft = if (horizontal) {
                Offset(right - text.size.width, at - text.size.height - 2.dp.toPx())
            } else {
                Offset(at + 4.dp.toPx(), top)
            }
            drawText(text, topLeft = topLeft)
        }
    }
}

/** Un testo su una riga sola, tagliato coi puntini oltre `maxWidth`. */
private fun measureOneLine(measurer: TextMeasurer, text: String, style: TextStyle, maxWidth: Int): TextLayoutResult =
    measurer.measure(
        text,
        style,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth.coerceAtLeast(1))
    )
