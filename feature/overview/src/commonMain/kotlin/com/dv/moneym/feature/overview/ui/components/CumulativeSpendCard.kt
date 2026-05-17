package com.dv.moneym.feature.overview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.CumulativeChart
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_cumulative_spend
import moneym.feature.overview.generated.resources.overview_through_day
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CumulativeSpendCard(
    cumulativeTotals: List<Double>,
    todayIndex: Int,
    currencyCode: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    MmCard(
        modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_0_5x),
        padded = true,
        shape = MM.dimen.radius_1_5x,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.overview_cumulative_spend),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = currencyCode,
                    style = type.captionMono.copy(color = colors.text3),
                )
            }
            Spacer(Modifier.height(space.padding_1x))
            val todayTotal = cumulativeTotals.getOrElse(todayIndex) { 0.0 }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                MmMoney(value = todayTotal, size = 22.sp, weight = FontWeight.SemiBold)
                Text(
                    text = stringResource(Res.string.overview_through_day, todayIndex + 1),
                    style = type.caption.copy(color = colors.text2),
                )
            }
            Spacer(Modifier.height(space.padding_2x))
            if (cumulativeTotals.isNotEmpty()) {
                val maxVal = cumulativeTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                // Y-axis + chart in a Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .width(YAXIS_WIDTH)
                            .height(CHART_HEIGHT),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatAxisAmount(maxVal),
                            style = type.captionMono.copy(fontSize = 9.sp, color = colors.text3),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = formatAxisAmount(maxVal / 2),
                            style = type.captionMono.copy(fontSize = 9.sp, color = colors.text3),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "0",
                            style = type.captionMono.copy(fontSize = 9.sp, color = colors.text3),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(MM.dimen.padding_0_5x))
                    CumulativeChart(
                        values = cumulativeTotals,
                        todayIndex = todayIndex,
                        modifier = Modifier.weight(1f).height(CHART_HEIGHT),
                    )
                }
            }
            Spacer(Modifier.height(space.padding_0_5x))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("1", "8", "15", "22", "31").forEach { label ->
                    Text(
                        text = label,
                        style = type.captionMono.copy(fontSize = 10.sp, color = colors.text3),
                    )
                }
            }
        }
    }
}
