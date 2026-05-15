package com.dv.moneym.feature.overview.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.categoryColor
import kotlin.math.PI
import kotlin.math.atan2

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
    selectedIndex: Int? = null,
    onSliceTapped: ((Int?) -> Unit)? = null,
) {
    val background = MaterialTheme.colorScheme.surfaceVariant
    val total = slices.sumOf { it.value.toDouble() }.toFloat()

    // Build cumulative angle list for hit testing
    val sweeps = buildList {
        var start = -90f
        slices.forEach { slice ->
            val sweep = if (total > 0f) 360f * (slice.value / total) else 0f
            add(start to sweep)
            start += sweep
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (onSliceTapped != null) Modifier.pointerInput(slices) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            var angleDeg = (atan2(dy, dx) * 180.0 / PI).toFloat()
                            angleDeg = ((angleDeg + 90f) + 360f) % 360f
                            val hitIndex = sweeps.indexOfFirst { (start, sweep) ->
                                val normStart = ((start + 90f) + 360f) % 360f
                                val normEnd = (normStart + sweep) % 360f
                                if (normStart <= normEnd) angleDeg in normStart..normEnd
                                else angleDeg >= normStart || angleDeg <= normEnd
                            }
                            onSliceTapped(if (hitIndex == selectedIndex) null else hitIndex.takeIf { it >= 0 })
                        }
                    } else Modifier,
                ),
        ) {
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
            slices.forEachIndexed { index, slice ->
                if (slice.value <= 0f) return@forEachIndexed
                val (startAngle, sweep) = sweeps[index]
                val alpha = when {
                    selectedIndex == null -> 1f
                    selectedIndex == index -> 1f
                    else -> 0.35f
                }
                drawArc(
                    color = categoryColor(slice.colorHex).copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = sweep - 1f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt),
                )
            }
        }
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
