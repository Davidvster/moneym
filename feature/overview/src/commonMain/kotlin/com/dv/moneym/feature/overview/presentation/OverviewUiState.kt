package com.dv.moneym.feature.overview.presentation

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.YearMonth

sealed interface OverviewPeriod {
    data class Month(val yearMonth: YearMonth) : OverviewPeriod
    data class Year(val year: Int) : OverviewPeriod
}

data class OverviewUiState(
    val isLoading: Boolean = true,
    val period: OverviewPeriod = OverviewPeriod.Month(YearMonth(2026, 1)),
    val totalIncome: List<MoneyTotal> = emptyList(),
    val totalExpense: List<MoneyTotal> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val chartBars: List<BarEntry> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val selectedCategoryId: CategoryId? = null,
    val selectedSliceIndex: Int? = null,
    val isEmpty: Boolean = false,
)

data class MoneyTotal(val minorUnits: Long, val currency: CurrencyCode)

data class CategoryBreakdownItem(
    val id: CategoryId?,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val expenseMinorUnits: Long,
    val percentage: Float,
    val formattedAmount: String,
)

data class BarEntry(
    val label: String,
    val expenseMinorUnits: Long,
    val incomeMinorUnits: Long = 0L,
    val isHighlighted: Boolean = false,
)

sealed interface OverviewIntent {
    data object PreviousPeriod : OverviewIntent
    data object NextPeriod : OverviewIntent
    data object TogglePeriod : OverviewIntent
    data class CategoryFilterSelected(val id: CategoryId?) : OverviewIntent
    data class SliceTapped(val index: Int?) : OverviewIntent
}
