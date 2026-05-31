package com.dv.moneym.core.uigraphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.uigraphs.internal.rememberChartEntryProgress
import com.dv.moneym.core.uigraphs.internal.staggeredProgress

@Composable
fun MiniBars(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1,
) {
    val entry = rememberChartEntryProgress(data)
    Canvas(modifier = modifier.defaultMinSize(minHeight = 26.dp)) {
        if (data.isEmpty()) return@Canvas

        val maxValue = data.maxOrNull()?.coerceAtLeast(0.001) ?: 0.001
        val barCount = data.size
        val gapFraction = 0.2f
        val totalGap = size.width * gapFraction
        val barWidth = (size.width - totalGap) / barCount
        val gapWidth = totalGap / (barCount - 1).coerceAtLeast(1)
        val minBarHeight = 1.5.dp.toPx()
        val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val path = Path()

        data.forEachIndexed { i, value ->
            val isHighlight = i == highlightIndex
            val alpha = when {
                isHighlight -> 1.0f
                value == 0.0 -> 0.18f
                else -> 0.7f
            }
            val local = staggeredProgress(entry, i, barCount)
            val barHeight = if (value == 0.0) {
                minBarHeight
            } else {
                ((value / maxValue).toFloat() * size.height * local).coerceAtLeast(minBarHeight)
            }
            val x = i * (barWidth + gapWidth)
            val y = size.height - barHeight
            path.reset()
            path.addRoundRect(
                RoundRect(
                    rect = Rect(Offset(x, y), Size(barWidth, barHeight)),
                    topLeft = radius,
                    topRight = radius,
                    bottomLeft = CornerRadius.Zero,
                    bottomRight = CornerRadius.Zero,
                ),
            )
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.75f)),
                    startY = y,
                    endY = size.height,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun MiniBarsPreview() {
    MoneyMTheme {
        MiniBars(
            data = listOf(1.0, 3.0, 2.0, 5.0, 4.0, 0.0, 6.0, 3.5),
            color = MM.colors.accent,
            highlightIndex = 3,
            modifier = Modifier.padding(MM.dimen.padding_2x).width(160.dp).height(48.dp),
        )
    }
}
