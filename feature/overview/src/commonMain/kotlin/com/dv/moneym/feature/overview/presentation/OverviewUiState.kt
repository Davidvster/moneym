package com.dv.moneym.feature.overview.presentation

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.YearMonth

// ─── Period / Mode ─────────────────────────────────────────────

sealed interface OverviewPeriod {
    data class Month(val yearMonth: YearMonth) : OverviewPeriod
    data class Year(val year: Int) : OverviewPeriod
    /**
     * Custom date range. Dates are stored as individual Int fields for KMP
     * serialization simplicity (avoids expect/actual for LocalDate serialization).
     */
    data class DateRange(
        val startYear: Int,
        val startMonth: Int,
        val startDay: Int,
        val endYear: Int,
        val endMonth: Int,
        val endDay: Int,
    ) : OverviewPeriod
}

enum class OverviewMode { Month, Year }

// ─── New breakdown types ────────────────────────────────────────

/**
 * A single category's spend for the selected period,
 * used in the donut chart and legend.
 */
data class CategorySpend(
    val categoryName: String,
    val categoryColor: Long,   // ARGB long — convert with Color(it)
    val categoryIcon: String,  // icon key, resolve via iconForKey()
    val amount: Double,        // major units (e.g. 12.50 EUR)
    val percent: Int,          // 0–100
    /** Average spend per day for this category in the current period. 0 if not applicable. */
    val avgPerDay: Double = 0.0,
    /** Average spend per month for this category in the current period. 0 if not applicable. */
    val avgPerMonth: Double = 0.0,
)

/**
 * Per-category trend series: daily (31 elements) or monthly (12 elements).
 */
data class CategoryTrend(
    val categoryName: String,
    val categoryColor: Long,
    val categoryIcon: String,
    val totalAmount: Double,
    val txCount: Int,
    val series: List<Double>,  // 31 values (month mode) or 12 values (year mode)
)

/**
 * Per-category average spend: avg per day (month mode) or avg per month (year mode).
 */
data class CategoryAvgSpend(
    val categoryName: String,
    val categoryColor: Long,
    val categoryIcon: String,
    val avgAmount: Double,
)

// ─── Main UiState ──────────────────────────────────────────────

data class OverviewUiState(
    // Loading / empty
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,

    // Period / mode — period encodes both month and year info
    val period: OverviewPeriod = OverviewPeriod.Month(YearMonth(2026, 1)),

    // Slide direction: +1 = forward in time, -1 = backward, 0 = no animation
    val periodOffset: Int = 0,

    // ── Legacy list-based totals (kept for existing tests) ──────
    val totalIncome: List<MoneyTotal> = emptyList(),
    val totalExpense: List<MoneyTotal> = emptyList(),

    // ── New Double-based summary (used by redesigned screen) ─────
    val income: Double = 0.0,
    val expenses: Double = 0.0,

    // ── Category breakdown ───────────────────────────────────────
    val categoryBreakdown: List<CategorySpend> = emptyList(),

    // ── Month-view data ──────────────────────────────────────────
    val dailyTotals: List<Double> = emptyList(),        // up to 31 values (expense amounts)
    val cumulativeTotals: List<Double> = emptyList(),   // running prefix sum of dailyTotals
    val todayIndex: Int = 0,                             // current day - 1 (0-indexed)
    val categoryDailyTrend: List<CategoryTrend> = emptyList(),

    // ── Year-view data ───────────────────────────────────────────
    val monthlyTotals: List<Double> = List(12) { 0.0 }, // index 0 = Jan
    val categoryMonthlyTrend: List<CategoryTrend> = emptyList(),
    val currentMonthIndex: Int = -1,                    // current month - 1, or -1 if not current year

    // ── Average stats ────────────────────────────────────────────
    val avgDailyExpense: Double = 0.0,       // month view: avg expense per day (elapsed days)
    val avgMonthlyExpense: Double = 0.0,     // year view: avg expense per month (elapsed months)
    val avgDailyExpenseYear: Double = 0.0,   // year view: avg expense per day (elapsed days in year)

    // ── Per-category average spend ───────────────────────────────
    val categoryAvgSpend: List<CategoryAvgSpend> = emptyList(),

    // ── Legacy chart bars (kept for existing tests) ──────────────
    val chartBars: List<BarEntry> = emptyList(),

    // ── Category filter UI (legacy) ──────────────────────────────
    val availableCategories: List<Category> = emptyList(),
    val selectedCategoryId: CategoryId? = null,
    val selectedSliceIndex: Int? = null,
)

// ─── Legacy types — kept so existing tests compile ─────────────

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
    data class PeriodSelected(val period: OverviewPeriod) : OverviewIntent
    data class DateRangeSelected(
        val startYear: Int,
        val startMonth: Int,
        val startDay: Int,
        val endYear: Int,
        val endMonth: Int,
        val endDay: Int,
    ) : OverviewIntent
}
