package com.dv.moneym.feature.overview.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
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
) {
    val space = MM.space
    if (inMonthMode && avgDailyExpense > 0) {
        MmCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = 6.dp),
            padded = true,
            shape = MM.radius.radius_1_5x,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel(text = avgDayLabel, modifier = Modifier.weight(1f))
                MmMoney(value = avgDailyExpense, size = 15.sp, weight = FontWeight.SemiBold, currency = currencyCode)
            }
        }
    }
    if (!inMonthMode && (avgMonthlyExpense > 0 || avgDailyExpenseYear > 0)) {
        MmCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = 6.dp),
            padded = true,
            shape = MM.radius.radius_1_5x,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionLabel(text = avgMonthLabel, modifier = Modifier.weight(1f))
                    MmMoney(value = avgMonthlyExpense, size = 15.sp, weight = FontWeight.SemiBold, currency = currencyCode)
                }
                Spacer(Modifier.height(space.padding_1x))
                HorizontalDivider(color = MM.colors.divider, thickness = 1.dp)
                Spacer(Modifier.height(space.padding_1x))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionLabel(text = avgDayLabel, modifier = Modifier.weight(1f))
                    MmMoney(value = avgDailyExpenseYear, size = 15.sp, weight = FontWeight.SemiBold, currency = currencyCode)
                }
            }
        }
    }
}
