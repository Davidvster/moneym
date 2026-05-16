package com.dv.moneym.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
fun CumulativeChart(
    values: List<Double>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val lineColor = colors.text
    val gridColor = colors.text3
    val todayLineColor = colors.text3

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val displayCount = (todayIndex + 1).coerceIn(1, values.size)
        val displayValues = values.take(displayCount)

        val minVal = 0.0
        val maxVal = displayValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val totalPoints = values.size.coerceAtLeast(2)

        fun xAt(index: Int): Float = (index.toFloat() / (totalPoints - 1)) * size.width
        fun yAt(value: Double): Float = size.height - ((value - minVal) / (maxVal - minVal)).toFloat() * size.height

        // 3 dashed horizontal gridlines at 25/50/75%
        val gridLevels = listOf(0.25f, 0.50f, 0.75f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        gridLevels.forEach { level ->
            val y = size.height * (1f - level)
            drawLine(
                color = gridColor.copy(alpha = 0.25f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect,
            )
        }

        if (displayValues.size < 2) return@Canvas

        // Build area path
        val areaPath = Path()
        areaPath.moveTo(xAt(0), size.height)
        areaPath.lineTo(xAt(0), yAt(displayValues[0]))
        displayValues.forEachIndexed { i, v ->
            areaPath.lineTo(xAt(i), yAt(v))
        }
        areaPath.lineTo(xAt(displayValues.size - 1), size.height)
        areaPath.close()

        drawPath(
            path = areaPath,
            color = lineColor.copy(alpha = 0.06f),
        )

        // Build line path
        val linePath = Path()
        displayValues.forEachIndexed { i, v ->
            if (i == 0) linePath.moveTo(xAt(i), yAt(v))
            else linePath.lineTo(xAt(i), yAt(v))
        }

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        // Dashed "today" vertical line
        val todayX = xAt(todayIndex.coerceIn(0, values.size - 1))
        val todayValue = values.getOrElse(todayIndex) { 0.0 }
        val todayY = yAt(todayValue)

        drawLine(
            color = todayLineColor,
            start = androidx.compose.ui.geometry.Offset(todayX, 0f),
            end = androidx.compose.ui.geometry.Offset(todayX, size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect,
        )

        // 4dp dot at today's value
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx() / 2f,
            center = androidx.compose.ui.geometry.Offset(todayX, todayY),
        )
    }
}
