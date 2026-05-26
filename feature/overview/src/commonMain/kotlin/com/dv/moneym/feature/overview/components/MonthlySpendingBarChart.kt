package com.dv.moneym.feature.overview.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.localizedMonthNames
import com.dv.moneym.core.ui.MmMoney
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_monthly_spending
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MonthlySpendingBarChart(
    monthlyTotals: List<Double>,
    currentMonthIndex: Int,
    currencyCode: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val monthNames = localizedMonthNames().map { it.take(3) }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val maxVal = monthlyTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val avgVal = monthlyTotals.filter { it > 0 }.let { nonZero ->
        if (nonZero.isNotEmpty()) nonZero.average() else 0.0
    }
    val avgFraction = (avgVal / maxVal).toFloat().coerceIn(0f, 1f)

    MmCard(
        modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_0_5x),
        padded = true,
        shape = MM.dimen.radius_1_5x,
    ) {
        Column {
            // Header: title + currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.overview_monthly_spending),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = currencyCode,
                    style = type.captionMono.copy(color = colors.text3),
                )
            }

            // Show selected bar amount prominently
            selectedBarIndex?.let { barIndex ->
                val selVal = monthlyTotals.getOrElse(barIndex) { 0.0 }
                val selName = monthNames.getOrElse(barIndex) { "" }
                Spacer(Modifier.height(MM.dimen.padding_0_5x))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Text(
                        text = selName,
                        style = type.caption.copy(color = colors.text2),
                    )
                    MmMoney(
                        value = selVal,
                        style = MM.type.amountMedium,
                        currency = currencyCode,
                    )
                }
            }

            Spacer(Modifier.height(space.padding_2x))

            // Chart area: Y-axis + bars (fixed height so month labels don't overlap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Y-axis labels column
                Column(
                    modifier = Modifier
                        .width(YAXIS_WIDTH)
                        .height(CHART_HEIGHT),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatAxisAmount(maxVal),
                        style = type.captionXs.copy(color = colors.text3),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = formatAxisAmount(maxVal / 2),
                        style = type.captionXs.copy(color = colors.text3),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "0",
                        style = type.captionXs.copy(color = colors.text3),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.width(MM.dimen.padding_0_5x))

                // Bar chart area (with dotted avg line overlay)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(CHART_HEIGHT),
                ) {
                    // Dashed average line drawn with Canvas
                    if (avgVal > 0) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
                            val avgY = size.height * (1f - avgFraction)
                            drawLine(
                                color = colors.accent.copy(alpha = 0.45f),
                                start = Offset(0f, avgY),
                                end = Offset(size.width, avgY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                            )
                        }
                    }

                    // Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CHART_HEIGHT),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        monthlyTotals.forEachIndexed { i, value ->
                            val isCurrent = i == currentMonthIndex
                            val isSelected = i == selectedBarIndex
                            val barFraction = (value / maxVal).toFloat().coerceIn(0f, 1f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                // The bar itself
                                Box(
                                    modifier = Modifier
                                        .width(MM.dimen.padding_2x)
                                        .fillMaxHeight(barFraction.coerceAtLeast(0.01f))
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            when {
                                                isSelected -> colors.accent
                                                isCurrent -> colors.text
                                                else -> colors.borderStrong
                                            },
                                        )
                                        .alpha(if (value == 0.0) 0.3f else 1f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) { selectedBarIndex = if (isSelected) null else i },
                                )
                            }
                        }
                    }
                }
            }

            // Month name labels in their own row — completely separated from bars so no overlap
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = YAXIS_WIDTH + MM.dimen.padding_0_5x),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                monthNames.forEachIndexed { i, name ->
                    val isCurrent = i == currentMonthIndex
                    val isSelected = i == selectedBarIndex
                    Text(
                        text = name,
                        style = type.captionXs.copy(
                            color = when {
                                isSelected -> colors.accent
                                isCurrent -> colors.text
                                else -> colors.text3
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * A reusable bar chart canvas for per-category yearly data.
 * Draws bars with an avg dotted line overlay. Tapping a bar shows its value.
 */
@Composable
internal fun CategoryBarChart(
    monthlyTotals: List<Double>,
    currentMonthIndex: Int,
    barColor: Color,
    modifier: Modifier = Modifier,
    xLabels: List<String> = emptyList(),
) {
    val colors = MM.colors
    val type = MM.type

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val maxVal = monthlyTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val avgVal = monthlyTotals.filter { it > 0 }.let { nonZero ->
        if (nonZero.isNotEmpty()) nonZero.average() else 0.0
    }
    val avgFraction = (avgVal / maxVal).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier) {
        // Show selected bar amount prominently
        selectedBarIndex?.let { barIndex ->
            val selVal = monthlyTotals.getOrElse(barIndex) { 0.0 }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            ) {
                val monthNames = localizedMonthNames().map { it.take(3) }
                Text(
                    text = monthNames.getOrElse(barIndex) { "" },
                    style = type.caption.copy(color = colors.text2),
                )
                MmMoney(
                    value = selVal,
                    size = 13.sp,
                    weight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Y-axis labels column
            Column(
                modifier = Modifier
                    .width(YAXIS_WIDTH)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatAxisAmount(maxVal),
                    style = type.captionXs.copy(color = colors.text3),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = formatAxisAmount(maxVal / 2),
                    style = type.captionXs.copy(color = colors.text3),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "0",
                    style = type.captionXs.copy(color = colors.text3),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.width(MM.dimen.padding_0_5x))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // Dashed average line
                if (avgVal > 0) {
                    Canvas(modifier = Modifier.fillMaxWidth().matchParentSize()) {
                        val avgY = size.height * (1f - avgFraction)
                        drawLine(
                            color = barColor.copy(alpha = 0.50f),
                            start = Offset(0f, avgY),
                            end = Offset(size.width, avgY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                        )
                    }
                }

                // Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    monthlyTotals.forEachIndexed { i, value ->
                        val isCurrent = i == currentMonthIndex
                        val isSelected = i == selectedBarIndex
                        val barFraction = (value / maxVal).toFloat().coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(MM.dimen.padding_2x)
                                    .fillMaxHeight(barFraction.coerceAtLeast(0.01f))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            isSelected -> barColor
                                            isCurrent -> barColor.copy(alpha = 0.85f)
                                            else -> barColor.copy(alpha = 0.35f)
                                        },
                                    )
                                    .alpha(if (value == 0.0) 0.3f else 1f)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { selectedBarIndex = if (isSelected) null else i },
                            )
                        }
                    }
                }
            }
        }

        if (xLabels.isNotEmpty()) {
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = YAXIS_WIDTH + MM.dimen.padding_0_5x),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
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
