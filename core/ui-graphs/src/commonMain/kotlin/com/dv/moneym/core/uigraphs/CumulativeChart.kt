package com.dv.moneym.core.uigraphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

@Composable
fun CumulativeChart(
    values: List<Double>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
    xLabels: List<String> = emptyList(),
    lineColor: Color = MM.colors.text,
) {
    val colors = MM.colors
    val type = MM.type
    val gridColor = colors.text3
    val todayLineColor = colors.text3
    val seekLineColor = colors.accent

    var seekIndex by remember { mutableStateOf<Int?>(null) }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) }
    val areaPath = remember { Path() }
    val linePath = remember { Path() }
    val areaBrush = remember(seekLineColor) {
        Brush.verticalGradient(
            listOf(seekLineColor.copy(alpha = 0.22f), seekLineColor.copy(alpha = 0f)),
        )
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(values, todayIndex) {
                        fun resolve(x: Float): Int? {
                            if (values.size < 2) return null
                            return ((x / size.width) * (values.size - 1))
                                .toInt()
                                .coerceIn(0, values.size - 1)
                        }
                        detectDragGestures(
                            onDragStart = { seekIndex = resolve(it.x) },
                            onDrag = { change, _ ->
                                change.consume()
                                seekIndex = resolve(change.position.x)
                            },
                            onDragEnd = { seekIndex = null },
                            onDragCancel = { seekIndex = null },
                        )
                    },
            ) {
                if (values.isEmpty()) return@Canvas

                val displayCount = (todayIndex + 1).coerceIn(1, values.size)
                val displayValues = values.subList(0, displayCount)
                val maxVal = displayValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val totalPoints = values.size.coerceAtLeast(2)

                fun xAt(index: Int): Float = (index.toFloat() / (totalPoints - 1)) * size.width
                fun yAt(value: Double): Float =
                    size.height - (value / maxVal).toFloat() * size.height

                val gridLevels = floatArrayOf(0.25f, 0.50f, 0.75f)
                for (level in gridLevels) {
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

                // Gradient area under the line.
                areaPath.reset()
                areaPath.moveTo(xAt(0), size.height)
                areaPath.lineTo(xAt(0), yAt(displayValues[0]))
                displayValues.forEachIndexed { i, v -> areaPath.lineTo(xAt(i), yAt(v)) }
                areaPath.lineTo(xAt(displayValues.size - 1), size.height)
                areaPath.close()
                drawPath(path = areaPath, brush = areaBrush)

                linePath.reset()
                displayValues.forEachIndexed { i, v ->
                    if (i == 0) linePath.moveTo(xAt(i), yAt(v)) else linePath.lineTo(xAt(i), yAt(v))
                }
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                )

                // Dashed "today" marker + dot.
                val todayX = xAt(todayIndex.coerceIn(0, values.size - 1))
                val todayY = yAt(values.getOrElse(todayIndex) { 0.0 })
                drawLine(
                    color = todayLineColor,
                    start = Offset(todayX, 0f),
                    end = Offset(todayX, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )
                drawCircle(color = lineColor, radius = 2.dp.toPx(), center = Offset(todayX, todayY))

                // Seek marker snaps to the finger.
                val si = seekIndex
                if (si != null && si <= displayCount - 1) {
                    val seekX = xAt(si)
                    val seekY = yAt(displayValues[si])
                    drawLine(
                        color = seekLineColor,
                        start = Offset(seekX, 0f),
                        end = Offset(seekX, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                    drawCircle(color = seekLineColor, radius = 2.5.dp.toPx(), center = Offset(seekX, seekY))
                }
            }

            // Floating value bubble while seeking.
            val si = seekIndex
            if (si != null) {
                val seekValue = values.getOrElse(si) { 0.0 }
                Box(
                    modifier = Modifier
                        .align(if (si >= values.size / 2) Alignment.TopStart else Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surface.copy(alpha = 0.92f))
                        .border(MM.dimen.strokeHairline, colors.border, RoundedCornerShape(6.dp))
                        .padding(horizontal = MM.dimen.padding_1x, vertical = MM.dimen.padding_0_5x),
                ) {
                    Text(
                        text = "Day ${si + 1} · ${formatSeekValue(seekValue)}",
                        style = type.captionSm,
                        color = colors.text,
                    )
                }
            }
        }

        if (xLabels.isNotEmpty()) {
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                xLabels.forEach { label ->
                    Text(
                        text = label,
                        style = type.captionXs.copy(color = colors.text3),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun formatSeekValue(value: Double): String {
    val intPart = value.toLong()
    val decPart = kotlin.math.round((value - intPart) * 100).toInt()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}

@Preview
@Composable
private fun CumulativeChartPreview() {
    MoneyMTheme {
        CumulativeChart(
            values = listOf(2.0, 5.0, 7.0, 8.0, 12.0, 18.0, 21.0, 24.0, 30.0, 33.0),
            todayIndex = 6,
            modifier = Modifier.padding(MM.dimen.padding_2x).fillMaxWidth().height(120.dp),
        )
    }
}
