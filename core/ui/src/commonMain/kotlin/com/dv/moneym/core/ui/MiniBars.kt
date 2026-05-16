package com.dv.moneym.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MiniBars(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1,
) {
    Canvas(
        modifier = modifier.defaultMinSize(minHeight = 26.dp),
    ) {
        if (data.isEmpty()) return@Canvas

        val maxValue = data.maxOrNull()?.coerceAtLeast(0.001) ?: 0.001
        val barCount = data.size
        val gapFraction = 0.2f
        val totalGap = size.width * gapFraction
        val barWidth = (size.width - totalGap) / barCount
        val gapWidth = totalGap / (barCount - 1).coerceAtLeast(1)
        val minBarHeight = 1.5.dp.toPx()
        val cornerRadius = CornerRadius(2.dp.toPx())

        data.forEachIndexed { i, value ->
            val isHighlight = i == highlightIndex
            val alpha = when {
                isHighlight -> 1.0f
                value == 0.0 -> 0.18f
                else -> 0.7f
            }

            val barHeight = if (value == 0.0) {
                minBarHeight
            } else {
                ((value / maxValue).toFloat() * size.height).coerceAtLeast(minBarHeight)
            }

            val x = i * (barWidth + gapWidth)
            val y = size.height - barHeight

            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius,
            )
        }
    }
}
