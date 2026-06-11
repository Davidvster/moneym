package com.dv.moneym.core.uigraphs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.uigraphs.internal.ChartDurations
import com.dv.moneym.core.uigraphs.internal.rememberChartEntryProgress
import kotlin.math.PI
import kotlin.math.atan2

data class DonutSlice(val color: Color, val fraction: Float)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = MM.dimen.donutWidth,
    selectedIndex: Int? = null,
    onSliceClick: ((Int) -> Unit)? = null,
) {
    val trackColor = MM.colors.surface2
    val entry = rememberChartEntryProgress(slices)
    // Animated selection emphasis: 0f = no selection, 1f = a slice is selected.
    val selectionEmphasis by animateFloatAsState(
        targetValue = if (selectedIndex != null) 1f else 0f,
        animationSpec = tween(ChartDurations.Select),
        label = "donut_selection",
    )

    val gapDegrees = 2f
    val total = slices.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.001f)
    val availableDegrees = 360f - gapDegrees * slices.size

    Canvas(
        modifier = modifier.then(
            if (onSliceClick != null) {
                Modifier.pointerInput(slices) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        var angleDeg = (atan2(
                            (offset.y - cy).toDouble(),
                            (offset.x - cx).toDouble(),
                        ) * 180.0 / PI).toFloat() + 90f
                        if (angleDeg < 0) angleDeg += 360f

                        var cursor = 0f
                        slices.forEachIndexed { i, slice ->
                            val sweep = (slice.fraction / total) * availableDegrees
                            if (angleDeg >= cursor && angleDeg < cursor + sweep) {
                                onSliceClick(i)
                                return@detectTapGestures
                            }
                            cursor += sweep + gapDegrees
                        }
                    }
                }
            } else Modifier,
        ),
    ) {
        val strokePx = strokeWidth.toPx()
        val selectedStrokePx = strokePx * 1.45f

        val diameter = minOf(size.width, size.height) - selectedStrokePx
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        // Faint full-ring track for depth.
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        // Sequential reveal: arcs sweep in clockwise, each slice starting
        // where the previous one finished.
        val revealedDegrees = entry * availableDegrees
        var cumulativeStart = 0f
        var startAngle = -90f
        slices.forEachIndexed { i, slice ->
            val fullSweep = (slice.fraction / total) * availableDegrees
            val sweep = (revealedDegrees - cumulativeStart).coerceIn(0f, fullSweep)
            cumulativeStart += fullSweep
            val isSelected = i == selectedIndex
            // Non-selected slices fade toward 0.35 as selection emphasis rises.
            val alpha = if (isSelected) 1f else lerp(1f, 0.35f, selectionEmphasis)
            val width = if (isSelected) {
                strokePx + (selectedStrokePx - strokePx) * selectionEmphasis
            } else {
                strokePx
            }
            drawArc(
                color = slice.color.copy(alpha = alpha),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
            startAngle += fullSweep + gapDegrees
        }
    }
}

@Preview
@Composable
private fun DonutChartPreview() {
    MoneyMTheme {
        DonutChart(
            slices = listOf(
                DonutSlice(MM.colors.accent, 0.45f),
                DonutSlice(MM.colors.danger, 0.30f),
                DonutSlice(MM.colors.text2, 0.25f),
            ),
            selectedIndex = 0,
            modifier = Modifier.padding(MM.dimen.padding_2x).size(140.dp),
        )
    }
}
