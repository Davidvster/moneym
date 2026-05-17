package com.dv.moneym.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A compact cumulative line chart used in category trend rows.
 * Shows running total as a filled area + line.
 * No today marker, no drag interaction (interaction is handled externally via seekIndex).
 *
 * @param data       Raw daily/monthly values (converted to cumulative internally)
 * @param color      Line and fill color
 * @param upToIndex  How many values to show (inclusive, 0-based). -1 = show all.
 * @param seekIndex  Optional index to show a seek indicator (vertical line + dot). Set externally.
 */
@Composable
fun MiniCumulativeLine(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    upToIndex: Int = -1,
    seekIndex: Int? = null,
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        // Build cumulative values
        val cumulative = mutableListOf<Double>()
        var running = 0.0
        data.forEach { v -> running += v; cumulative.add(running) }

        // Check if all values are zero
        if (cumulative.all { it == 0.0 }) return@Canvas

        val displayCount = if (upToIndex >= 0) {
            (upToIndex + 1).coerceIn(1, cumulative.size)
        } else {
            cumulative.size
        }
        val displayValues = cumulative.take(displayCount)

        if (displayValues.size < 2) return@Canvas

        val minVal = 0.0
        val maxVal = displayValues.maxOrNull()?.coerceAtLeast(0.001) ?: 0.001
        val totalPoints = cumulative.size.coerceAtLeast(2)

        fun xAt(index: Int): Float = (index.toFloat() / (totalPoints - 1)) * size.width
        fun yAt(value: Double): Float =
            size.height - ((value - minVal) / (maxVal - minVal)).toFloat() * size.height

        // Area fill path
        val areaPath = Path()
        areaPath.moveTo(xAt(0), size.height)
        areaPath.lineTo(xAt(0), yAt(displayValues[0]))
        displayValues.forEachIndexed { i, v -> areaPath.lineTo(xAt(i), yAt(v)) }
        areaPath.lineTo(xAt(displayValues.size - 1), size.height)
        areaPath.close()

        drawPath(path = areaPath, color = color.copy(alpha = 0.12f))

        // Line path
        val linePath = Path()
        displayValues.forEachIndexed { i, v ->
            if (i == 0) linePath.moveTo(xAt(i), yAt(v))
            else linePath.lineTo(xAt(i), yAt(v))
        }
        drawPath(path = linePath, color = color, style = Stroke(width = 1.5.dp.toPx()))

        // End dot
        val lastX = xAt(displayValues.size - 1)
        val lastY = yAt(displayValues.last())
        drawCircle(
            color = color,
            radius = 3.dp.toPx() / 2f,
            center = Offset(lastX, lastY),
        )

        // Seek indicator: vertical line + dot at seekIndex
        if (seekIndex != null && seekIndex in cumulative.indices) {
            val seekVal = cumulative[seekIndex]
            val seekX = xAt(seekIndex)
            val seekY = yAt(seekVal)

            drawLine(
                color = color.copy(alpha = 0.75f),
                start = Offset(seekX, 0f),
                end = Offset(seekX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(
                color = color,
                radius = 4.dp.toPx() / 2f,
                center = Offset(seekX, seekY),
            )
        }
    }
}
