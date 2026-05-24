package com.dv.moneym.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.overview.components.DateRangePickerDialog
import com.dv.moneym.feature.overview.components.OverviewHeader
import com.dv.moneym.feature.overview.components.OverviewMonthPickerDialog
import com.dv.moneym.feature.overview.components.OverviewYearPickerDialog
import com.dv.moneym.feature.overview.components.daysInMonthUi
import com.dv.moneym.feature.overview.components.formatShortDate
import com.dv.moneym.feature.overview.components.localizedMonthNames
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object OverviewKey : NavKey

fun EntryProviderScope<NavKey>.overviewEntry(
    onTabSelected: (TabRoute) -> Unit = {},
) = entry<OverviewKey> {
    OverviewScreen(onTabSelected = onTabSelected)
}

@Composable
private fun OverviewScreen(
    viewModel: OverviewViewModel = koinViewModel(),
    onTabSelected: (TabRoute) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OverviewContent(
        state = state,
        onIntent = viewModel::onIntent,
        onTabSelected = onTabSelected,
    )
}

@Composable
private fun OverviewContent(
    state: OverviewUiState,
    onIntent: (OverviewIntent) -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors
    val monthNames = localizedMonthNames()
    val isMonthMode = state.currentPeriod is OverviewPeriod.Month
    val isYearMode = state.currentPeriod is OverviewPeriod.Year
    val periodLabel = when (val p = state.currentPeriod) {
        is OverviewPeriod.Month -> "${monthNames[p.yearMonth.monthNumber - 1]} ${p.yearMonth.year}"
        is OverviewPeriod.Year -> p.year.toString()
        is OverviewPeriod.DateRange -> "${
            formatShortDate(p.startYear, p.startMonth, p.startDay)
        } – ${formatShortDate(p.endYear, p.endMonth, p.endDay)}"
    }

    var showPeriodPicker by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var initialMonthScrollDone by remember { mutableStateOf(false) }
    var initialYearScrollDone by remember { mutableStateOf(false) }

    val monthPagerState = rememberPagerState(
        initialPage = state.monthCurrentPage,
        pageCount = { state.monthPageCount },
    )
    val yearPagerState = rememberPagerState(
        initialPage = state.yearCurrentPage,
        pageCount = { state.yearPageCount },
    )

    // Month pager fully settled → tell VM which month is visible
    LaunchedEffect(monthPagerState.settledPage) {
        if (!isMonthMode) return@LaunchedEffect
        val newMonth = pageToYearMonth(monthPagerState.settledPage, state.monthAnchor)
        if (OverviewPeriod.Month(newMonth) != state.currentPeriod) {
            onIntent(OverviewIntent.MonthPagerSwiped(newMonth))
        }
    }

    // VM month changed (arrows / dialog) → scroll month pager to match
    LaunchedEffect(state.monthCurrentPage) {
        if (monthPagerState.currentPage != state.monthCurrentPage) {
            if (initialMonthScrollDone) {
                monthPagerState.animateScrollToPage(state.monthCurrentPage)
            } else {
                monthPagerState.scrollToPage(state.monthCurrentPage)
            }
        }
        initialMonthScrollDone = true
    }

    // Year pager fully settled → tell VM which year is visible
    LaunchedEffect(yearPagerState.settledPage) {
        if (!isYearMode) return@LaunchedEffect
        val newYear = pageToYear(yearPagerState.settledPage, state.yearAnchor)
        if (OverviewPeriod.Year(newYear) != state.currentPeriod) {
            onIntent(OverviewIntent.YearPagerSwiped(newYear))
        }
    }

    // VM year changed (arrows / dialog) → scroll year pager to match
    LaunchedEffect(state.yearCurrentPage) {
        if (yearPagerState.currentPage != state.yearCurrentPage) {
            if (initialYearScrollDone) {
                yearPagerState.animateScrollToPage(state.yearCurrentPage)
            } else {
                yearPagerState.scrollToPage(state.yearCurrentPage)
            }
        }
        initialYearScrollDone = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        OverviewHeader(
            period = state.currentPeriod,
            periodLabel = periodLabel,
            spendingFilter = state.spendingFilter,
            onTogglePeriod = { onIntent(OverviewIntent.TogglePeriod) },
            onPreviousPeriod = { onIntent(OverviewIntent.PreviousPeriod) },
            onNextPeriod = { onIntent(OverviewIntent.NextPeriod) },
            onShowPeriodPicker = { showPeriodPicker = true },
            onShowDateRangePicker = { showDateRangePicker = true },
            onSpendingFilterChanged = { onIntent(OverviewIntent.SpendingFilterChanged(it)) },
            canGoBack = state.canGoBack,
        )

        when {
            isMonthMode -> HorizontalPager(
                state = monthPagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f),
            ) { page ->
                OverviewPageScreen(
                    period = OverviewPeriod.Month(pageToYearMonth(page, state.monthAnchor)),
                    spendingFilter = state.spendingFilter,
                    currencyCode = state.currency,
                )
            }
            isYearMode -> HorizontalPager(
                state = yearPagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f),
            ) { page ->
                OverviewPageScreen(
                    period = OverviewPeriod.Year(pageToYear(page, state.yearAnchor)),
                    spendingFilter = state.spendingFilter,
                    currencyCode = state.currency,
                )
            }
            else -> OverviewPageScreen(
                period = state.currentPeriod,
                spendingFilter = state.spendingFilter,
                currencyCode = state.currency,
                modifier = Modifier.weight(1f),
            )
        }

        MmTabBar(
            activeTab = TabRoute.Overview,
            onTabSelected = onTabSelected,
        )

        if (showPeriodPicker) {
            if (isMonthMode) {
                val currentPeriod = state.currentPeriod
                OverviewMonthPickerDialog(
                    currentYear = currentPeriod.yearMonth.year,
                    currentMonth = currentPeriod.yearMonth.monthNumber,
                    minYear = state.minSelectableDateIso?.let { LocalDate.parse(it).year },
                    minMonth = state.minSelectableDateIso?.let { LocalDate.parse(it).monthNumber },
                    onDismiss = { showPeriodPicker = false },
                    onConfirm = { year, month ->
                        onIntent(
                            OverviewIntent.PeriodSelected(
                                OverviewPeriod.Month(YearMonth(year, month))
                            )
                        )
                        showPeriodPicker = false
                    },
                )
            } else if (state.currentPeriod is OverviewPeriod.Year) {
                val currentPeriod = state.currentPeriod
                OverviewYearPickerDialog(
                    currentYear = currentPeriod.year,
                    minYear = state.minSelectableDateIso?.let { LocalDate.parse(it).year },
                    onDismiss = { showPeriodPicker = false },
                    onConfirm = { year ->
                        onIntent(OverviewIntent.PeriodSelected(OverviewPeriod.Year(year)))
                        showPeriodPicker = false
                    },
                )
            }
        }

        if (showDateRangePicker) {
            val initStart = when (val p = state.currentPeriod) {
                is OverviewPeriod.DateRange -> Triple(p.startYear, p.startMonth, p.startDay)
                is OverviewPeriod.Month -> Triple(p.yearMonth.year, p.yearMonth.monthNumber, 1)
                is OverviewPeriod.Year -> Triple(p.year, 1, 1)
            }
            val initEnd = when (val p = state.currentPeriod) {
                is OverviewPeriod.DateRange -> Triple(p.endYear, p.endMonth, p.endDay)
                is OverviewPeriod.Month -> Triple(
                    p.yearMonth.year,
                    p.yearMonth.monthNumber,
                    daysInMonthUi(p.yearMonth.year, p.yearMonth.monthNumber),
                )
                is OverviewPeriod.Year -> Triple(p.year, 12, 31)
            }
            DateRangePickerDialog(
                initStartYear = initStart.first,
                initStartMonth = initStart.second,
                initStartDay = initStart.third,
                initEndYear = initEnd.first,
                initEndMonth = initEnd.second,
                initEndDay = initEnd.third,
                minSelectableDateIso = state.minSelectableDateIso,
                maxSelectableDateIso = state.maxSelectableDateIso,
                onDismiss = { showDateRangePicker = false },
                onConfirm = { sy, sm, sd, ey, em, ed ->
                    onIntent(
                        OverviewIntent.DateRangeSelected(sy, sm, sd, ey, em, ed)
                    )
                    showDateRangePicker = false
                },
            )
        }
    }
}

@Preview
@Composable
private fun OverviewScreenPreview() {
    MoneyMTheme {
        OverviewContent(
            state = OverviewUiState(),
            onIntent = {},
            onTabSelected = {},
        )
    }
}
