package com.dv.moneym.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.dv.moneym.core.designsystem.MM
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
    Canvas(
        modifier = modifier.then(
            if (onSliceClick != null) {
                Modifier.pointerInput(slices) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        // Convert to angle [0, 360) starting from top (-90 deg offset)
                        var angleDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat() + 90f
                        if (angleDeg < 0) angleDeg += 360f

                        val total = slices.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.001f)
                        val gapDegrees = 2f
                        val availableDegrees = 360f - gapDegrees * slices.size
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
            } else Modifier
        ),
    ) {
        val strokePx = strokeWidth.toPx()
        val selectedStrokePx = strokePx * 1.45f
        val gapDegrees = 2f

        val diameter = minOf(size.width, size.height) - selectedStrokePx
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)
        val total = slices.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.001f)

        val totalGapDegrees = gapDegrees * slices.size
        val availableDegrees = 360f - totalGapDegrees

        var startAngle = -90f

        slices.forEachIndexed { i, slice ->
            val sweep = (slice.fraction / total) * availableDegrees
            val isSelected = i == selectedIndex
            val hasSelection = selectedIndex != null
            val alpha = when {
                !hasSelection -> 1f
                isSelected -> 1f
                else -> 0.35f
            }
            drawArc(
                color = slice.color.copy(alpha = alpha),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = if (isSelected) selectedStrokePx else strokePx,
                    cap = StrokeCap.Butt,
                ),
            )
            startAngle += sweep + gapDegrees
        }
    }
}
