package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val seekLineColor = colors.accent

    var seekIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(values, todayIndex) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (values.size >= 2) {
                                val totalPoints = values.size
                                val idx = ((offset.x / size.width.toFloat()) * (totalPoints - 1))
                                    .toInt()
                                    .coerceIn(0, totalPoints - 1)
                                seekIndex = idx
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (values.size >= 2) {
                                val totalPoints = values.size
                                val idx = ((change.position.x / size.width.toFloat()) * (totalPoints - 1))
                                    .toInt()
                                    .coerceIn(0, totalPoints - 1)
                                seekIndex = idx
                            }
                        },
                        onDragEnd = { seekIndex = null },
                        onDragCancel = { seekIndex = null },
                    )
                },
        ) {
            if (values.isEmpty()) return@Canvas

            val displayCount = (todayIndex + 1).coerceIn(1, values.size)
            val displayValues = values.take(displayCount)

            val minVal = 0.0
            val maxVal = displayValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val totalPoints = values.size.coerceAtLeast(2)

            fun xAt(index: Int): Float = (index.toFloat() / (totalPoints - 1)) * size.width
            fun yAt(value: Double): Float =
                size.height - ((value - minVal) / (maxVal - minVal)).toFloat() * size.height

            // 3 dashed horizontal gridlines at 25/50/75%
            val gridLevels = listOf(0.25f, 0.50f, 0.75f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            gridLevels.forEach { level ->
                val y = size.height * (1f - level)
                drawLine(
                    color = gridColor.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
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
                start = Offset(todayX, 0f),
                end = Offset(todayX, size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect,
            )

            // 4dp dot at today's value
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx() / 2f,
                center = Offset(todayX, todayY),
            )

            // Seek line and dot
            val si = seekIndex
            if (si != null && si <= displayCount - 1) {
                val seekX = xAt(si)
                val seekValue = values.getOrElse(si) { 0.0 }
                val seekY = yAt(seekValue)

                // Solid vertical seek line in accent color
                drawLine(
                    color = seekLineColor,
                    start = Offset(seekX, 0f),
                    end = Offset(seekX, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )

                // Dot at seek position
                drawCircle(
                    color = seekLineColor,
                    radius = 5.dp.toPx() / 2f,
                    center = Offset(seekX, seekY),
                )
            }
        }

        // Seek value label overlay
        val si = seekIndex
        if (si != null && si <= (todayIndex + 1).coerceIn(1, values.size) - 1) {
            val seekValue = values.getOrElse(si) { 0.0 }
            val dayLabel = si + 1
            val valueText = "Day $dayLabel · ${formatSeekValue(seekValue)}"
            val isRightHalf = si >= values.size / 2
            Box(
                modifier = Modifier
                    .align(if (isRightHalf) Alignment.TopStart else Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MM.colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, MM.colors.border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = valueText,
                    style = MM.type.captionMono.copy(fontSize = 11.sp),
                    color = MM.colors.text,
                )
            }
        }
    }
}

private fun formatSeekValue(value: Double): String {
    val intPart = value.toLong()
    val decPart = kotlin.math.round((value - intPart) * 100).toInt()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}
