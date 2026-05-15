package com.dv.moneym.feature.overview.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.categoryColor

data class DonutSlice(
    val colorHex: String,
    val value: Float,
    val label: String,
)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerLabel: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 24.dp,
) {
    val background = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        Canvas(modifier = Modifier.matchParentSize()) {
            if (total <= 0f) {
                drawArc(
                    color = background,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt),
                )
                return@Canvas
            }
            var startAngle = -90f
            slices.forEach { slice ->
                if (slice.value <= 0f) return@forEach
                val sweep = 360f * (slice.value / total)
                drawArc(
                    color = categoryColor(slice.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweep - 1f, // 1° gap between segments
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
