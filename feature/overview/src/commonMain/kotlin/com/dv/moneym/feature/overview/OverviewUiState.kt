package com.dv.moneym.feature.overview

import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.YearMonth
import kotlinx.serialization.Serializable

// ─── Period / Mode ─────────────────────────────────────────────

@Serializable
sealed interface OverviewPeriod {
    @Serializable data class Month(val yearMonth: YearMonth) : OverviewPeriod
    @Serializable data class Year(val year: Int) : OverviewPeriod

    @Serializable
    data class DateRange(
        val startYear: Int,
        val startMonth: Int,
        val startDay: Int,
        val endYear: Int,
        val endMonth: Int,
        val endDay: Int,
    ) : OverviewPeriod
}

@Serializable
internal data class CategorySpend(
    val categoryName: String,
    val categoryColor: Long,
    val categoryIcon: Icon,
    val amount: Double,
    val percent: Int,
    val avgPerDay: Double = 0.0,
    val avgPerMonth: Double = 0.0,
)

@Serializable
internal data class CategoryTrend(
    val categoryName: String,
    val categoryColor: Long,
    val categoryIcon: Icon,
    val totalAmount: Double,
    val txCount: Int,
    val series: List<Double>,
    val avgPerDay: Double = 0.0,
    val avgPerMonth: Double = 0.0,
)

@Serializable
internal data class CategoryAvgSpend(
    val categoryName: String,
    val categoryColor: Long,
    val categoryIcon: Icon,
    val avgAmount: Double,
)

@Serializable
internal enum class SpendingFilter { All, Expenses, Income }

// ─── Page math ─────────────────────────────────────────────────

// anchor = earliest transaction month (falls back to today when no transactions)
// page 0 = anchor month, increasing pages = later months
internal fun yearMonthToPage(yearMonth: YearMonth, anchor: YearMonth): Int =
    (yearMonth.year - anchor.year) * 12 + (yearMonth.monthNumber - anchor.monthNumber)

internal fun pageToYearMonth(page: Int, anchor: YearMonth): YearMonth {
    val total = anchor.year * 12 + (anchor.monthNumber - 1) + page
    return YearMonth(total / 12, total % 12 + 1)
}

// anchor = earliest transaction year (falls back to today when no transactions)
// page 0 = anchor year, increasing pages = later years
internal fun yearToPage(year: Int, anchorYear: Int): Int = year - anchorYear
internal fun pageToYear(page: Int, anchorYear: Int): Int = anchorYear + page

// ─── Parent UiState (navigation + pager math only) ─────────────

@Serializable
internal data class OverviewUiState(
    val currentPeriod: OverviewPeriod = OverviewPeriod.Month(YearMonth(2026, 1)),
    val canGoBack: Boolean = true,
    val spendingFilter: SpendingFilter = SpendingFilter.Expenses,
    // Month pager
    val monthAnchor: YearMonth = YearMonth(2026, 1),
    val monthCurrentPage: Int = 0,
    val monthPageCount: Int = 121,
    // Year pager
    val yearAnchor: Int = 2026,
    val yearCurrentPage: Int = 0,
    val yearPageCount: Int = 11,
    // Date pickers
    val minSelectableDateIso: String? = null,
    val maxSelectableDateIso: String? = null,
    val transactionDateIsos: Set<String> = emptySet(),
    val currency: String = "EUR",
)

// ─── Page UiState (per-period chart data) ──────────────────────

internal data class OverviewPageUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val period: OverviewPeriod = OverviewPeriod.Month(YearMonth(2026, 1)),
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
