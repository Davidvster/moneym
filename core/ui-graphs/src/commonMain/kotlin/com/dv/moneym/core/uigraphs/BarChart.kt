package com.dv.moneym.core.uigraphs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.uigraphs.internal.ChartDurations
import com.dv.moneym.core.uigraphs.internal.rememberChartEntryProgress
import com.dv.moneym.core.uigraphs.internal.staggeredProgress

/** Per-bar colors. [current] is the live month/period; [other] every remaining bar. */
data class BarColors(
    val selected: Color,
    val current: Color,
    val other: Color,
    val avg: Color,
)

/**
 * Generic animated bar chart. Bars grow from the baseline with a refined
 * left->right cascade; selecting a bar dims the rest and an optional average
 * line draws in with the entrance.
 */
@Composable
fun BarChart(
    values: List<Double>,
    barColors: BarColors,
    modifier: Modifier = Modifier,
    currentIndex: Int = -1,
    selectedIndex: Int? = null,
    onSelect: (Int?) -> Unit = {},
    avgValue: Double = 0.0,
    barWidth: Dp = MM.dimen.padding_2x,
) {
    val entry = rememberChartEntryProgress(values)
    val selectionEmphasis by animateFloatAsState(
        targetValue = if (selectedIndex != null) 1f else 0f,
        animationSpec = tween(ChartDurations.Select),
        label = "bar_selection",
    )
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f) }
    val barPath = remember { Path() }

    val maxVal = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val avgFraction = (avgValue / maxVal).toFloat().coerceIn(0f, 1f)

    Canvas(
        modifier = modifier.pointerInput(values.size) {
            detectTapGestures { offset ->
                if (values.isEmpty()) return@detectTapGestures
                val slot = size.width / values.size
                val i = (offset.x / slot).toInt().coerceIn(0, values.size - 1)
                onSelect(if (i == selectedIndex) null else i)
            }
        },
    ) {
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val barPx = barWidth.toPx().coerceAtMost(slot * 0.8f)
        val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val minBarPx = 1.dp.toPx()

        values.forEachIndexed { i, value ->
            val localEntry = staggeredProgress(entry, i, values.size)
            val fraction = (value / maxVal).toFloat().coerceIn(0f, 1f) * localEntry
            val barHeight = (fraction * size.height).coerceAtLeast(if (value > 0) minBarPx else 0f)
            if (barHeight <= 0f) return@forEachIndexed

            val isSelected = i == selectedIndex
            val isCurrent = i == currentIndex
            val baseColor = when {
                isSelected -> barColors.selected
                isCurrent -> barColors.current
                else -> barColors.other
            }
            // Dim non-selected bars as a selection takes hold.
            val color = if (isSelected) baseColor else lerp(baseColor, barColors.other, selectionEmphasis * 0.6f)
            val alpha = if (value == 0.0) 0.3f else 1f

            val left = i * slot + (slot - barPx) / 2f
            val top = size.height - barHeight
            barPath.reset()
            barPath.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(Offset(left, top), Size(barPx, barHeight)),
                    topLeft = radius,
                    topRight = radius,
                    bottomLeft = CornerRadius.Zero,
                    bottomRight = CornerRadius.Zero,
                ),
            )
            drawPath(
                path = barPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.78f)),
                    startY = top,
                    endY = size.height,
                ),
            )
        }

        if (avgValue > 0) {
            val avgY = size.height * (1f - avgFraction)
            drawLine(
                color = barColors.avg,
                start = Offset(0f, avgY),
                end = Offset(size.width * entry, avgY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect,
            )
        }
    }
}

@Preview
@Composable
private fun BarChartPreview() {
    MoneyMTheme {
        BarChart(
            values = listOf(3.0, 5.0, 2.0, 8.0, 0.0, 6.0, 4.0, 7.0, 5.0, 9.0, 3.0, 6.0),
            barColors = BarColors(
                selected = MM.colors.accent,
                current = MM.colors.text,
                other = MM.colors.borderStrong,
                avg = MM.colors.accent.copy(alpha = 0.45f),
            ),
            currentIndex = 9,
            selectedIndex = 3,
            avgValue = 5.0,
            modifier = Modifier.padding(MM.dimen.padding_2x).fillMaxWidth().height(120.dp),
        )
    }
}
