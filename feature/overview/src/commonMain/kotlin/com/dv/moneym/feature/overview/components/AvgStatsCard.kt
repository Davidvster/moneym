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
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.SectionLabel
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_avg_day
import moneym.feature.overview.generated.resources.overview_avg_expense_day
import moneym.feature.overview.generated.resources.overview_avg_expense_month
import moneym.feature.overview.generated.resources.overview_avg_income_day
import moneym.feature.overview.generated.resources.overview_avg_income_month
import moneym.feature.overview.generated.resources.overview_avg_month
import moneym.feature.overview.generated.resources.overview_avg_net_day
import moneym.feature.overview.generated.resources.overview_avg_net_month
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AvgStatsCard(
    inMonthMode: Boolean,
    spendingFilter: SpendingFilter,
    avgDailyExpense: Double,
    avgMonthlyExpense: Double,
    avgDailyExpenseYear: Double,
    avgDailyIncome: Double,
    avgMonthlyIncome: Double,
    avgDailyIncomeYear: Double,
    avgDailyNet: Double,
    avgMonthlyNet: Double,
    avgDailyNetYear: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val rows: List<Pair<String, Double>> = if (inMonthMode) {
        when (spendingFilter) {
            SpendingFilter.Expenses ->
                if (avgDailyExpense > 0) {
                    listOf(stringResource(Res.string.overview_avg_day) to avgDailyExpense)
                } else emptyList()

            SpendingFilter.Income ->
                if (avgDailyIncome > 0) {
                    listOf(stringResource(Res.string.overview_avg_income_day) to avgDailyIncome)
                } else emptyList()

            SpendingFilter.All ->
                if (avgDailyExpense > 0 || avgDailyIncome > 0) {
                    listOf(
                        stringResource(Res.string.overview_avg_expense_day) to avgDailyExpense,
                        stringResource(Res.string.overview_avg_income_day) to avgDailyIncome,
                        stringResource(Res.string.overview_avg_net_day) to avgDailyNet,
                    )
                } else emptyList()
        }
    } else {
        when (spendingFilter) {
            SpendingFilter.Expenses ->
                if (avgMonthlyExpense > 0 || avgDailyExpenseYear > 0) {
                    listOf(
                        stringResource(Res.string.overview_avg_month) to avgMonthlyExpense,
                        stringResource(Res.string.overview_avg_day) to avgDailyExpenseYear,
                    )
                } else emptyList()

            SpendingFilter.Income ->
                if (avgMonthlyIncome > 0 || avgDailyIncomeYear > 0) {
                    listOf(
                        stringResource(Res.string.overview_avg_income_month) to avgMonthlyIncome,
                        stringResource(Res.string.overview_avg_income_day) to avgDailyIncomeYear,
                    )
                } else emptyList()

            SpendingFilter.All ->
                if (avgMonthlyExpense > 0 || avgDailyExpenseYear > 0 ||
                    avgMonthlyIncome > 0 || avgDailyIncomeYear > 0
                ) {
                    listOf(
                        stringResource(Res.string.overview_avg_expense_month) to avgMonthlyExpense,
                        stringResource(Res.string.overview_avg_income_month) to avgMonthlyIncome,
                        stringResource(Res.string.overview_avg_net_month) to avgMonthlyNet,
                        stringResource(Res.string.overview_avg_expense_day) to avgDailyExpenseYear,
                        stringResource(Res.string.overview_avg_income_day) to avgDailyIncomeYear,
                        stringResource(Res.string.overview_avg_net_day) to avgDailyNetYear,
                    )
                } else emptyList()
        }
    }

    if (rows.isEmpty()) return

    val space = MM.dimen
    MmCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = space.padding_2x, vertical = MM.dimen.padding_1x),
        padded = true,
        shape = MM.dimen.radius_1_5x,
    ) {
        Column {
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    SectionLabel(text = label, modifier = Modifier.weight(1f))
                    MmMoney(
                        value = value,
                        style = MM.type.amountMedium,
                        currency = currencyCode,
                    )
                }
                if (index < rows.lastIndex) {
                    Spacer(Modifier.height(space.padding_1x))
                    HorizontalDivider(
                        color = MM.colors.divider,
                        thickness = MM.dimen.strokeHairline,
                    )
                    Spacer(Modifier.height(space.padding_1x))
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
            spendingFilter = SpendingFilter.Expenses,
            avgDailyExpense = 42.75,
            avgMonthlyExpense = 1280.50,
            avgDailyExpenseYear = 39.20,
            avgDailyIncome = 80.0,
            avgMonthlyIncome = 2400.0,
            avgDailyIncomeYear = 78.9,
            avgDailyNet = 37.25,
            avgMonthlyNet = 1119.5,
            avgDailyNetYear = 39.7,
            currencyCode = "EUR",
        )
    }
}
