package com.dv.moneym.feature.overview.presentation

import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.YearMonth

data class OverviewUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth(2026, 1),
    val totalIncome: List<MoneyTotal> = emptyList(),
    val totalExpense: List<MoneyTotal> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val trendMonths: List<MonthTrend> = emptyList(),
    val isEmpty: Boolean = false,
)

data class MoneyTotal(val minorUnits: Long, val currency: CurrencyCode)

data class CategoryBreakdownItem(
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val expenseMinorUnits: Long,
    val percentage: Float,
    val formattedAmount: String,
)

data class MonthTrend(
    val label: String,
    val incomeMinorUnits: Long,
    val expenseMinorUnits: Long,
    val isCurrentMonth: Boolean,
)

sealed interface OverviewIntent {
    data object PreviousMonth : OverviewIntent
    data object NextMonth : OverviewIntent
}
