package com.dv.moneym.core.uigraphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.uigraphs.internal.rememberChartEntryProgress

/**
 * Compact cumulative line chart for category trend rows. Shows running total as a
 * gradient-filled area + line that reveals left->right on data change.
 *
 * @param data       Raw daily/monthly values (converted to cumulative internally)
 * @param color      Line and fill color
 * @param upToIndex  How many values to show (inclusive, 0-based). -1 = show all.
 * @param seekIndex  Optional index to show a seek indicator (vertical line + dot).
 */
@Composable
fun MiniCumulativeLine(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    upToIndex: Int = -1,
    seekIndex: Int? = null,
) {
    val entry = rememberChartEntryProgress(data)
    val cumulative = remember(data) {
        var running = 0.0
        data.map { running += it; running }
    }
    val areaPath = remember { Path() }
    val linePath = remember { Path() }
    val trimmed = remember { Path() }
    val pathMeasure = remember { PathMeasure() }
    val areaBrush = remember(color) {
        Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f)))
    }

    Canvas(modifier = modifier) {
        if (cumulative.isEmpty() || cumulative.all { it == 0.0 }) return@Canvas

        val displayCount = if (upToIndex >= 0) (upToIndex + 1).coerceIn(1, cumulative.size) else cumulative.size
        val displayValues = cumulative.subList(0, displayCount)
        if (displayValues.size < 2) return@Canvas

        val maxVal = displayValues.maxOrNull()?.coerceAtLeast(0.001) ?: 0.001
        val totalPoints = cumulative.size.coerceAtLeast(2)

        fun xAt(index: Int): Float = (index.toFloat() / (totalPoints - 1)) * size.width
        fun yAt(value: Double): Float = size.height - (value / maxVal).toFloat() * size.height

        areaPath.reset()
        areaPath.moveTo(xAt(0), size.height)
        areaPath.lineTo(xAt(0), yAt(displayValues[0]))
        displayValues.forEachIndexed { i, v -> areaPath.lineTo(xAt(i), yAt(v)) }
        areaPath.lineTo(xAt(displayValues.size - 1), size.height)
        areaPath.close()
        drawPath(path = areaPath, brush = areaBrush, alpha = entry)

        linePath.reset()
        displayValues.forEachIndexed { i, v ->
            if (i == 0) linePath.moveTo(xAt(i), yAt(v)) else linePath.lineTo(xAt(i), yAt(v))
        }
        pathMeasure.setPath(linePath, false)
        trimmed.reset()
        pathMeasure.getSegment(0f, pathMeasure.length * entry, trimmed, true)
        drawPath(path = trimmed, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))

        if (entry >= 1f) {
            val lastX = xAt(displayValues.size - 1)
            val lastY = yAt(displayValues.last())
            drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(lastX, lastY))
        }

        if (seekIndex != null && seekIndex in cumulative.indices) {
            val seekX = xAt(seekIndex)
            val seekY = yAt(cumulative[seekIndex])
            drawLine(
                color = color.copy(alpha = 0.75f),
                start = Offset(seekX, 0f),
                end = Offset(seekX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(seekX, seekY))
        }
    }
}

@Preview
@Composable
private fun MiniCumulativeLinePreview() {
    MoneyMTheme {
        MiniCumulativeLine(
            data = listOf(2.0, 1.5, 3.0, 0.5, 4.0, 2.5, 1.0, 3.5, 2.0, 1.5),
            color = MM.colors.accent,
            modifier = Modifier.padding(MM.dimen.padding_2x).width(180.dp).height(56.dp),
        )
    }
}
