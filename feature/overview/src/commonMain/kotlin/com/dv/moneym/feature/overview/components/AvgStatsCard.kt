package com.dv.moneym.feature.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.SectionLabel

@Composable
internal fun AvgStatsCard(
    inMonthMode: Boolean,
    avgDailyExpense: Double,
    avgMonthlyExpense: Double,
    avgDailyExpenseYear: Double,
    avgDayLabel: String,
    avgMonthLabel: String,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val space = MM.dimen
    if (inMonthMode && avgDailyExpense > 0) {
        MmCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = MM.dimen.padding_1x),
            padded = true,
            shape = MM.dimen.radius_1_5x,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            ) {
                SectionLabel(text = avgDayLabel, modifier = Modifier.weight(1f))
                MmMoney(
                    value = avgDailyExpense,
                    style = MM.type.amountMedium,
                    currency = currencyCode
                )
            }
        }
    }
    if (!inMonthMode && (avgMonthlyExpense > 0 || avgDailyExpenseYear > 0)) {
        MmCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = MM.dimen.padding_1x),
            padded = true,
            shape = MM.dimen.radius_1_5x,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    SectionLabel(text = avgMonthLabel, modifier = Modifier.weight(1f))
                    MmMoney(
                        value = avgMonthlyExpense,
                        style = MM.type.amountMedium,
                        currency = currencyCode
                    )
                }
                Spacer(Modifier.height(space.padding_1x))
                HorizontalDivider(color = MM.colors.divider, thickness = MM.dimen.strokeHairline)
                Spacer(Modifier.height(space.padding_1x))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    SectionLabel(text = avgDayLabel, modifier = Modifier.weight(1f))
                    MmMoney(
                        value = avgDailyExpenseYear,
                        style = MM.type.amountMedium,
                        currency = currencyCode
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AvgStatsCardPreview() {
    MoneyMTheme {
        AvgStatsCard(
            inMonthMode = true,
            avgDailyExpense = 42.75,
            avgMonthlyExpense = 1280.50,
            avgDailyExpenseYear = 39.20,
            avgDayLabel = "Avg / day",
            avgMonthLabel = "Avg / month",
            currencyCode = "EUR",
        )
    }
}
