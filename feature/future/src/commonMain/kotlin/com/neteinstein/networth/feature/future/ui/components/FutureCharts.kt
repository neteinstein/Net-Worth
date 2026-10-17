package com.neteinstein.networth.feature.future.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The Future artboard's chart: savings over ten years with the return (solid) and without (dashed),
 * a marker at the selected [horizon], and grid lines labelled [topLabel] and [midLabel].
 */
@Composable
internal fun ProjectionChart(
    flat: List<Long>,
    withReturn: List<Long>,
    horizon: Int,
    max: Long,
    topLabel: String,
    midLabel: String,
    xLabels: List<Pair<Int, String>>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.surfaceContainer
    val axis = MaterialTheme.colorScheme.outline
    val marker = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val last = (flat.size - 1).coerceAtLeast(1)
    Column(modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                val left = 34.dp.toPx()
                val top = 6.dp.toPx()
                val bottom = size.height - 4.dp.toPx()

                fun x(i: Int) = left + (size.width - left) * i / last

                fun y(v: Long) = bottom - (bottom - top) * (v.coerceAtLeast(0).toFloat() / max.coerceAtLeast(1))
                drawLine(grid, Offset(left, top), Offset(size.width, top), 1.dp.toPx())
                drawLine(grid, Offset(left, (top + bottom) / 2), Offset(size.width, (top + bottom) / 2), 1.dp.toPx())
                drawLine(axis, Offset(left, bottom), Offset(size.width, bottom), 1.dp.toPx())
                val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                drawLine(marker, Offset(x(horizon), top), Offset(x(horizon), bottom), 1.dp.toPx(), pathEffect = dash)
                drawSeries(flat, ::x, ::y, muted, 2.dp.toPx(), PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
                drawSeries(withReturn, ::x, ::y, accent, 3.dp.toPx(), null)
                withReturn.getOrNull(horizon)?.let { dot(Offset(x(horizon), y(it)), accent, surface, 5.dp.toPx()) }
                flat.getOrNull(horizon)?.let { dot(Offset(x(horizon), y(it)), muted, surface, 4.dp.toPx()) }
            }
            Text(topLabel, style = MaterialTheme.typography.labelSmall, color = muted, modifier = Modifier.offset(y = 0.dp))
            Text(midLabel, style = MaterialTheme.typography.labelSmall, color = muted, modifier = Modifier.align(Alignment.CenterStart))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
            val left = 34.dp
            val width = maxWidth - left
            xLabels.forEach { (index, label) ->
                val labelWidth = 30.dp
                val center = left + width * (index.toFloat() / last)
                val start = (center - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth)
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(x = start).width(labelWidth),
                )
            }
        }
    }
}

private fun DrawScope.drawSeries(
    values: List<Long>,
    x: (Int) -> Float,
    y: (Long) -> Float,
    color: Color,
    width: Float,
    effect: PathEffect?,
) {
    if (values.isEmpty()) return
    val path = Path()
    values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))
}

private fun DrawScope.dot(center: Offset, color: Color, ring: Color, radius: Float) {
    drawCircle(ring, radius + 2f, center)
    drawCircle(color, radius, center)
}

data class ForecastBar(
    val label: String,
    val value: Long,
    val over: Boolean,
)

/** Year ahead's month-by-month bars, with a dashed line at [income] when it is shown. */
@Composable
internal fun ForecastBars(
    bars: List<ForecastBar>,
    income: Long?,
    overColor: Color,
    overLabelColor: Color,
    typicalColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val max = (bars.maxOfOrNull { it.value } ?: 0L).coerceAtLeast(income ?: 0L).coerceAtLeast(1L) * 1.1f
    val line = MaterialTheme.colorScheme.onSurface
    val axis = MaterialTheme.colorScheme.outlineVariant
    val chartHeight = 150.dp
    Column(modifier.fillMaxWidth().semantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(chartHeight)) {
            Row(
                Modifier.fillMaxWidth().height(chartHeight),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(chartHeight * (bar.value / max).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                            .background(if (bar.over) overColor else typicalColor),
                    )
                }
            }
            Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
                drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                if (income != null && income > 0) {
                    val y = size.height * (1f - income / max)
                    drawLine(
                        line,
                        Offset(0f, y),
                        Offset(size.width, y),
                        2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bar.over) overLabelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
