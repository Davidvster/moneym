package com.dv.moneym.feature.transactions.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.common.formatNumber
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.transactions.list.DayGroup
import com.dv.moneym.feature.transactions.list.TransactionUiModel
import kotlin.math.abs
import kotlinx.datetime.LocalDate

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
            .background(colors.surface)
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(
            text = group.label,
            color = colors.text2,
            modifier = Modifier.weight(1f),
        )
        if (showAmount) {
            Text(
                text = "$sign $currency $formattedDaily",
                style = type.captionSm.copy(color = colors.text2),
            )
        }
    }
}

internal fun formatDailyAmount(value: Double): String = formatNumber(value, 2)

@Preview
@Composable
private fun DayGroupHeaderPreview() {
    MoneyMTheme {
        DayGroupHeader(
            group = DayGroup(
                date = LocalDate(2026, 6, 10),
                label = "Today, Jun 10",
                transactions = listOf(
                    TransactionUiModel(
                        id = TransactionId(1L),
                        type = TransactionType.EXPENSE,
                        amountFormatted = "42.50",
                        amountMinorUnits = 4250L,
                        currency = "EUR",
                        isExpense = true,
                        categoryName = "Groceries",
                        categoryColorHex = "#4CAF50",
                        categoryIcon = Icon.Basket,
                        note = "Weekly shop",
                        occurredOn = LocalDate(2026, 6, 10),
                    ),
                    TransactionUiModel(
                        id = TransactionId(2L),
                        type = TransactionType.INCOME,
                        amountFormatted = "2500.00",
                        amountMinorUnits = 250000L,
                        currency = "EUR",
                        isExpense = false,
                        categoryName = "Salary",
                        categoryColorHex = "#66BB6A",
                        categoryIcon = Icon.Banknote,
                        note = "Monthly salary",
                        occurredOn = LocalDate(2026, 6, 10),
                    ),
                ),
            ),
        )
    }
}
