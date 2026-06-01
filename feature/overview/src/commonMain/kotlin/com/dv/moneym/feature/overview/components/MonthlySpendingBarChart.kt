package com.dv.moneym.feature.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.localizedMonthNames
import com.dv.moneym.core.uigraphs.BarChart
import com.dv.moneym.core.uigraphs.BarColors
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
    val avgVal = monthlyTotals.filter { it > 0 }.let { if (it.isNotEmpty()) it.average() else 0.0 }

    MmCard(
        modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_0_5x),
        padded = true,
        shape = MM.dimen.radius_1_5x,
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.overview_monthly_spending),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(text = currencyCode, style = type.captionMono.copy(color = colors.text3))
            }

            selectedBarIndex?.let { barIndex ->
                Spacer(Modifier.height(MM.dimen.padding_0_5x))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Text(text = monthNames.getOrElse(barIndex) { "" }, style = type.caption.copy(color = colors.text2))
                    MmMoney(
                        value = monthlyTotals.getOrElse(barIndex) { 0.0 },
                        style = MM.type.amountMedium,
                        currency = currencyCode,
                    )
                }
            }

            Spacer(Modifier.height(space.padding_2x))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                AxisLabels(maxVal, fillHeight = false)
                Spacer(Modifier.width(MM.dimen.padding_0_5x))
                BarChart(
                    values = monthlyTotals,
                    barColors = BarColors(
                        selected = colors.accent,
                        current = colors.text,
                        other = colors.borderStrong,
                        avg = colors.accent.copy(alpha = 0.45f),
                    ),
                    currentIndex = currentMonthIndex,
                    selectedIndex = selectedBarIndex,
                    onSelect = { selectedBarIndex = it },
                    avgValue = avgVal,
                    modifier = Modifier.weight(1f).height(CHART_HEIGHT),
                )
            }

            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = YAXIS_WIDTH + MM.dimen.padding_0_5x),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                monthNames.forEachIndexed { i, name ->
                    Text(
                        text = name,
                        style = type.captionXs.copy(
                            color = when {
                                i == selectedBarIndex -> colors.accent
                                i == currentMonthIndex -> colors.text
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
 * Per-category bar chart for yearly trend rows. Tapping a bar shows its value.
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
    val avgVal = monthlyTotals.filter { it > 0 }.let { if (it.isNotEmpty()) it.average() else 0.0 }

    Column(modifier = modifier) {
        selectedBarIndex?.let { barIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            ) {
                val monthNames = localizedMonthNames().map { it.take(3) }
                Text(text = monthNames.getOrElse(barIndex) { "" }, style = type.caption.copy(color = colors.text2))
                MmMoney(value = monthlyTotals.getOrElse(barIndex) { 0.0 }, size = 13.sp, weight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.Bottom) {
            AxisLabels(maxVal, fillHeight = true)
            Spacer(Modifier.width(MM.dimen.padding_0_5x))
            BarChart(
                values = monthlyTotals,
                barColors = BarColors(
                    selected = colors.accent,
                    current = barColor.copy(alpha = 0.85f),
                    other = barColor.copy(alpha = 0.35f),
                    avg = barColor.copy(alpha = 0.50f),
                ),
                currentIndex = currentMonthIndex,
                selectedIndex = selectedBarIndex,
                onSelect = { selectedBarIndex = it },
                avgValue = avgVal,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        if (xLabels.isNotEmpty()) {
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = YAXIS_WIDTH + MM.dimen.padding_0_5x),
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

@Composable
private fun AxisLabels(maxVal: Double, fillHeight: Boolean) {
    val colors = MM.colors
    val type = MM.type
    Column(
        modifier = Modifier.width(YAXIS_WIDTH).then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.height(CHART_HEIGHT)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(maxVal, maxVal / 2, 0.0).forEachIndexed { idx, v ->
            Text(
                text = if (idx == 2) "0" else formatAxisAmount(v),
                style = type.captionXs.copy(color = colors.text3),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
