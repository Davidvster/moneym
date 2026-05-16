package com.dv.moneym.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DonutSlice(val color: Color, val fraction: Float)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp,
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val gapDegrees = 2f

        val diameter = minOf(size.width, size.height) - strokePx
        val topLeft = androidx.compose.ui.geometry.Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f,
        )
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
        val total = slices.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.001f)

        // Total gap degrees
        val totalGapDegrees = gapDegrees * slices.size
        val availableDegrees = 360f - totalGapDegrees

        var startAngle = -90f

        slices.forEach { slice ->
            val sweep = (slice.fraction / total) * availableDegrees
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Butt,
                ),
            )
            startAngle += sweep + gapDegrees
        }
    }
}
