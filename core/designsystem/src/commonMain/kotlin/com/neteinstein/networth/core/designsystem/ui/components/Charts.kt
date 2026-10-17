package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BarSegment(
    val weight: Float,
    val color: Color,
)

/** One horizontal bar split into coloured parts ("Where it went"). */
@Composable
fun StackedBar(
    segments: List<BarSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    description: String? = null,
) {
    val visible = segments.filter { it.weight > 0f }
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics { if (description != null) contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        visible.forEach { Box(Modifier.weight(it.weight).fillMaxHeight().background(it.color)) }
    }
}

/** A thin progress-style bar, e.g. a category's share of the largest one. */
@Composable
fun ProportionBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    track: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(color))
    }
}

data class ColumnBar(
    val label: String,
    val valueLabel: String,
    val value: Float,
    val highlighted: Boolean = false,
)

/** Vertical bars with a dashed average line ("Last 6 months" vs the 12-month average). */
@Composable
fun ColumnBarChart(
    bars: List<ColumnBar>,
    modifier: Modifier = Modifier,
    average: Float? = null,
    chartHeight: Dp = 130.dp,
    highlight: Color = MaterialTheme.colorScheme.primary,
    muted: Color = com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme.financeColors.chartMuted,
    description: String? = null,
) {
    val max = (bars.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(average ?: 0f).coerceAtLeast(1f)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.semantics { if (description != null) contentDescription = description }) {
        Box(Modifier.fillMaxWidth().height(chartHeight + 18.dp)) {
            Row(
                Modifier.fillMaxWidth().height(chartHeight + 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            bar.valueLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(chartHeight * (bar.value / max))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(if (bar.highlighted) highlight else muted),
                        )
                    }
                }
            }
            if (average != null && average > 0f) {
                Canvas(Modifier.fillMaxWidth().height(chartHeight).align(Alignment.BottomStart)) {
                    val y = size.height * (1f - average / max)
                    drawLine(
                        lineColor,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            bars.forEach {
                Text(
                    it.label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Small rounded colour square used as a legend key for a category. */
@Composable
fun ColorKey(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    Box(modifier.size(size).clip(RoundedCornerShape(size / 3)).background(color))
}
