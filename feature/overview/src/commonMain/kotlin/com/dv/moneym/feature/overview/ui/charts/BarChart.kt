package com.dv.moneym.feature.overview.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.dv.moneym.feature.overview.presentation.MonthTrend

private val incomeColor = Color(0xFF4A7A56)
private val expenseColor = Color(0xFFB0623B)
private val currentMonthAlpha = 1f
private val otherMonthAlpha = 0.5f

@Composable
fun TrendBarChart(
    months: List<MonthTrend>,
    modifier: Modifier = Modifier,
) {
    if (months.isEmpty()) return
    val maxVal = months.maxOf { maxOf(it.incomeMinorUnits, it.expenseMinorUnits) }
        .coerceAtLeast(1L).toFloat()

    Canvas(modifier = modifier.fillMaxWidth()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val count = months.size
        val totalGap = size.width * 0.3f
        val barGroupWidth = (size.width - totalGap) / count
        val barWidth = barGroupWidth * 0.4f
        val groupGap = totalGap / (count + 1)

        months.forEachIndexed { i, month ->
            val alpha = if (month.isCurrentMonth) currentMonthAlpha else otherMonthAlpha
            val x = groupGap + i * (barGroupWidth + groupGap)

            val incomeH = size.height * (month.incomeMinorUnits / maxVal)
            if (incomeH > 0f) {
                drawRect(
                    color = incomeColor.copy(alpha = alpha),
                    topLeft = Offset(x, size.height - incomeH),
                    size = Size(barWidth, incomeH),
                )
            }

            val expenseH = size.height * (month.expenseMinorUnits / maxVal)
            if (expenseH > 0f) {
                drawRect(
                    color = expenseColor.copy(alpha = alpha),
                    topLeft = Offset(x + barWidth, size.height - expenseH),
                    size = Size(barWidth, expenseH),
                )
            }
        }
    }
}
