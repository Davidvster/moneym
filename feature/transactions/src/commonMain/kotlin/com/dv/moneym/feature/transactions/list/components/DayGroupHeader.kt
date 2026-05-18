package com.dv.moneym.feature.transactions.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.transactions.list.DayGroup
import com.dv.moneym.core.common.formatNumber
import kotlin.math.abs

@Composable
internal fun DayGroupHeader(group: DayGroup, showAmount: Boolean = true) {
    val colors = MM.colors
    val type = MM.type

    // Compute daily net: income - expenses (in minor units)
    val dailyExpenses = group.transactions.filter { it.isExpense }.sumOf { it.amountMinorUnits }
    val dailyIncome = group.transactions.filter { !it.isExpense }.sumOf { it.amountMinorUnits }
    val dailyNet = dailyIncome - dailyExpenses
    val currency = group.transactions.firstOrNull()?.currency ?: "EUR"
    val sign = if (dailyNet >= 0) "+" else "−"
    val formattedDaily = formatDailyAmount(abs(dailyNet).toDouble() / 100.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(
            text = group.label,
            modifier = Modifier.weight(1f),
        )
        if (showAmount) {
            Text(
                text = "$sign $currency $formattedDaily",
                style = type.caption.copy(fontSize = 11.sp, color = colors.text3),
            )
        }
    }
}

internal fun formatDailyAmount(value: Double): String = formatNumber(value, 2)
