package com.dv.moneym.feature.overview.page

import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.feature.overview.CategoryAvgSpend
import com.dv.moneym.feature.overview.CategorySpend
import com.dv.moneym.feature.overview.CategoryTrend
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.SpendingFilter

internal data class OverviewPageUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val period: OverviewPeriod? = null,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val categoryIncomeBreakdown: List<CategorySpend> = emptyList(),
    val dailyTotals: List<Double> = emptyList(),
    val cumulativeTotals: List<Double> = emptyList(),
    val todayIndex: Int = 0,
    val categoryDailyTrend: List<CategoryTrend> = emptyList(),
    val monthlyTotals: List<Double> = List(12) { 0.0 },
    val categoryMonthlyTrend: List<CategoryTrend> = emptyList(),
    val currentMonthIndex: Int = -1,
    val avgDailyExpense: Double = 0.0,
    val avgMonthlyExpense: Double = 0.0,
    val avgDailyExpenseYear: Double = 0.0,
    val categoryAvgSpend: List<CategoryAvgSpend> = emptyList(),
    val selectedSliceIndex: Int? = null,
)

// ─── Intents ────────────────────────────────────────────────────

internal sealed interface OverviewIntent {
    data object PreviousPeriod : OverviewIntent
    data object NextPeriod : OverviewIntent
    data object TogglePeriod : OverviewIntent
    data class PeriodSelected(val period: OverviewPeriod) : OverviewIntent
    data class DateRangeSelected(
        val startYear: Int,
        val startMonth: Int,
        val startDay: Int,
        val endYear: Int,
        val endMonth: Int,
        val endDay: Int,
    ) : OverviewIntent
    data class SpendingFilterChanged(val filter: SpendingFilter) : OverviewIntent
    data class MonthPagerSwiped(val yearMonth: YearMonth) : OverviewIntent
    data class YearPagerSwiped(val year: Int) : OverviewIntent
}

internal sealed interface OverviewPageIntent {
    data class SliceTapped(val index: Int?) : OverviewPageIntent
    data class CategoryFilterSelected(val id: CategoryId?) : OverviewPageIntent
}
