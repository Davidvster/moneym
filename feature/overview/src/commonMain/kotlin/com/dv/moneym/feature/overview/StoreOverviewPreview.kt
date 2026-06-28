package com.dv.moneym.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.overview.components.OverviewHeader
import com.dv.moneym.feature.overview.components.OverviewPeriodBody
import com.dv.moneym.feature.overview.page.OverviewPageUiState
import com.dv.moneym.feature.overview.usecase.BudgetProgress

// Store screenshot — full overview dashboard, light theme, EUR, May 2026.
// Reassembles header + page body + tab bar with mock data so it renders without the
// pager's koinViewModel pages. Called directly by StoreScreenshotTest (no @Preview scan).
@Composable
internal fun StoreOverviewPreview() {
    val eur = CurrencyCode("EUR")
    val period = OverviewPeriod.Month(YearMonth(2026, 5))
    val todayIndex = 27
    val cumulative = List(31) { i -> 35.0 * (i + 1) + (i % 3) * 12 }
    val groceriesSeries = List(31) { i -> 18.0 + (i % 4) * 6 }

    // Render OverviewPeriodBody directly rather than via OverviewPageContent: the latter
    // wraps content in a Crossfade whose alpha animates from 0, which Paparazzi captures
    // mid-fade (blank). The body itself renders fully at frame 0.
    val pageState = OverviewPageUiState(
                    isLoading = false,
                    isEmpty = false,
                    period = period,
                    income = 2500.0,
                    expenses = 1284.0,
                    categoryBreakdown = listOf(
                        CategorySpend("Home", 0xFF26A69A, Icon.Home, 510.0, 40, avgPerDay = 16.5),
                        CategorySpend("Groceries", 0xFF4CAF50, Icon.Basket, 322.0, 25, avgPerDay = 10.4),
                        CategorySpend("Transport", 0xFF42A5F5, Icon.Car, 188.0, 15, avgPerDay = 6.1),
                        CategorySpend("Restaurants", 0xFFFF7043, Icon.Restaurant, 154.0, 12, avgPerDay = 5.0),
                        CategorySpend("Entertainment", 0xFFEC407A, Icon.PlayCircle, 110.0, 8, avgPerDay = 3.5),
                    ),
                    categoryIncomeBreakdown = listOf(
                        CategorySpend("Salary", 0xFF66BB6A, Icon.Banknote, 2500.0, 100),
                    ),
                    cumulativeTotals = cumulative,
                    todayIndex = todayIndex,
                    categoryDailyTrend = listOf(
                        CategoryTrend(
                            categoryName = "Groceries",
                            categoryColor = 0xFF4CAF50,
                            categoryIcon = Icon.Basket,
                            totalAmount = 322.0,
                            txCount = 9,
                            series = groceriesSeries,
                            avgPerDay = 10.4,
                        ),
                    ),
        avgDailyExpense = 45.9,
        budgetProgress = listOf(
            BudgetProgress(
                budgetId = 1L,
                name = "Groceries",
                amount = Money(40000L, eur),
                spent = Money(32200L, eur),
                remaining = Money(7800L, eur),
                fraction = 0.80f,
                isOverrun = false,
                categoryName = "Groceries",
                categoryColor = 0xFF4CAF50,
            ),
        ),
    )

    CompositionLocalProvider(LocalInspectionMode provides true) {
        MoneyMTheme(darkTheme = false) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MM.colors.bg),
            ) {
                OverviewHeader(
                    period = period,
                    periodLabel = "May 2026",
                    transactionFilter = TransactionFilter.ByType(TransactionType.EXPENSE),
                    onTogglePeriod = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onShowPeriodPicker = {},
                    onShowDateRangePicker = {},
                    onShowCategoryFilter = {},
                    onTransactionFilterChanged = {},
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    OverviewPeriodBody(
                        state = pageState,
                        spendingFilter = SpendingFilter.Expenses,
                        currencyCode = "EUR",
                        onIntent = {},
                    )
                }
                MmTabBar(activeTab = TabRoute.Overview, onTabSelected = {})
            }
        }
    }
}
