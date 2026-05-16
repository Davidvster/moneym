package com.dv.moneym.feature.overview.presentation

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.YearMonth

// ─── Period / Mode ─────────────────────────────────────────────

sealed interface OverviewPeriod {
    data class Month(val yearMonth: YearMonth) : OverviewPeriod
    data class Year(val year: Int) : OverviewPeriod
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

// ─── Main UiState ──────────────────────────────────────────────

data class OverviewUiState(
    // Loading / empty
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,

    // Period / mode — period encodes both month and year info
    val period: OverviewPeriod = OverviewPeriod.Month(YearMonth(2026, 1)),

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
}
