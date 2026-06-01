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
import com.dv.moneym.core.ui.MmMonthPickerDialog
import com.dv.moneym.feature.overview.components.OverviewYearPickerDialog
import com.dv.moneym.feature.overview.components.daysInMonthUi
import com.dv.moneym.feature.overview.components.formatShortDate
import com.dv.moneym.core.ui.localizedMonthNames
import com.dv.moneym.feature.overview.page.OverviewIntent
import com.dv.moneym.feature.overview.page.OverviewPageScreen
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_cancel
import moneym.feature.overview.generated.resources.overview_dialog_select_month
import moneym.feature.overview.generated.resources.overview_next_year_cd
import moneym.feature.overview.generated.resources.overview_now
import moneym.feature.overview.generated.resources.overview_ok
import moneym.feature.overview.generated.resources.overview_prev_year_cd
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Serializable
data object OverviewKey : NavKey

fun EntryProviderScope<NavKey>.overviewEntry(
    onTabSelected: (TabRoute) -> Unit = {},
    onAnalyze: (year: Int, month: Int) -> Unit = { _, _ -> },
    metadata: Map<String, Any> = emptyMap(),
) = entry<OverviewKey>(metadata = metadata) {
    OverviewScreen(onTabSelected = onTabSelected, onAnalyze = onAnalyze)
}

@Composable
private fun OverviewScreen(
    viewModel: OverviewViewModel = koinViewModel(),
    onTabSelected: (TabRoute) -> Unit = {},
    onAnalyze: (year: Int, month: Int) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OverviewContent(
        state = state,
        onIntent = viewModel::onIntent,
        onTabSelected = onTabSelected,
        onAnalyze = onAnalyze,
    )
}

@Composable
private fun OverviewContent(
    state: OverviewUiState,
    onIntent: (OverviewIntent) -> Unit,
    onTabSelected: (TabRoute) -> Unit,
    onAnalyze: (year: Int, month: Int) -> Unit = { _, _ -> },
) {
    val colors = MM.colors
    val monthNames = localizedMonthNames()
    val currentPeriod = state.currentPeriod ?: return
    val monthAnchor = state.monthAnchor ?: return
    val yearAnchor = state.yearAnchor ?: return
    val isMonthMode = currentPeriod is OverviewPeriod.Month
    val isYearMode = currentPeriod is OverviewPeriod.Year
    val periodLabel = when (val p = currentPeriod) {
        is OverviewPeriod.Month -> "${monthNames[p.yearMonth.monthNumber - 1]} ${p.yearMonth.year}"
        is OverviewPeriod.Year -> p.year.toString()
        is OverviewPeriod.DateRange -> "${
            formatShortDate(p.startYear, p.startMonth, p.startDay)
        } – ${formatShortDate(p.endYear, p.endMonth, p.endDay)}"
    }
    val analyzeYearMonth = when (val p = currentPeriod) {
        is OverviewPeriod.Month -> p.yearMonth.year to p.yearMonth.monthNumber
        is OverviewPeriod.Year -> p.year to 1
        is OverviewPeriod.DateRange -> p.startYear to p.startMonth
    }

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
        val newMonth = pageToYearMonth(monthPagerState.settledPage, monthAnchor)
        if (OverviewPeriod.Month(newMonth) != currentPeriod) {
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
        val newYear = pageToYear(yearPagerState.settledPage, yearAnchor)
        if (OverviewPeriod.Year(newYear) != currentPeriod) {
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
            period = currentPeriod,
            periodLabel = periodLabel,
            spendingFilter = state.spendingFilter,
            onTogglePeriod = { onIntent(OverviewIntent.TogglePeriod) },
            onPreviousPeriod = { onIntent(OverviewIntent.PreviousPeriod) },
            onNextPeriod = { onIntent(OverviewIntent.NextPeriod) },
            onShowPeriodPicker = { onIntent(OverviewIntent.ShowPeriodPicker(true)) },
            onShowDateRangePicker = { onIntent(OverviewIntent.ShowDateRangePicker(true)) },
            onSpendingFilterChanged = { onIntent(OverviewIntent.SpendingFilterChanged(it)) },
            canGoBack = state.canGoBack,
            accounts = state.accounts,
            selectedAccountId = state.selectedAccountId,
            onAccountSelected = { onIntent(OverviewIntent.AccountSelected(it)) },
            aiAvailable = state.aiAvailable,
            onAnalyzeClick = { onAnalyze(analyzeYearMonth.first, analyzeYearMonth.second) },
        )

        when {
            isMonthMode -> HorizontalPager(
                state = monthPagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f),
            ) { page ->
                OverviewPageScreen(
                    period = OverviewPeriod.Month(pageToYearMonth(page, monthAnchor)),
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
                    period = OverviewPeriod.Year(pageToYear(page, yearAnchor)),
                    spendingFilter = state.spendingFilter,
                    currencyCode = state.currency,
                )
            }

            else -> OverviewPageScreen(
                period = currentPeriod,
                spendingFilter = state.spendingFilter,
                currencyCode = state.currency,
                modifier = Modifier.weight(1f),
            )
        }

        MmTabBar(
            activeTab = TabRoute.Overview,
            onTabSelected = onTabSelected,
        )

        if (state.showPeriodPicker) {
            if (currentPeriod is OverviewPeriod.Month) {
                val todayDate = remember {
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                }
                MmMonthPickerDialog(
                    currentYear = currentPeriod.yearMonth.year,
                    currentMonth = currentPeriod.yearMonth.monthNumber,
                    nowYear = todayDate.year,
                    nowMonth = todayDate.month.number,
                    title = stringResource(Res.string.overview_dialog_select_month),
                    nowLabel = stringResource(Res.string.overview_now),
                    okLabel = stringResource(Res.string.overview_ok),
                    cancelLabel = stringResource(Res.string.overview_cancel),
                    prevYearContentDescription = stringResource(Res.string.overview_prev_year_cd),
                    nextYearContentDescription = stringResource(Res.string.overview_next_year_cd),
                    minYear = state.minSelectableDateIso?.let { LocalDate.parse(it).year },
                    minMonth = state.minSelectableDateIso?.let { LocalDate.parse(it).month.number },
                    onDismiss = { onIntent(OverviewIntent.ShowPeriodPicker(false)) },
                    onConfirm = { year, month ->
                        onIntent(
                            OverviewIntent.PeriodSelected(
                                OverviewPeriod.Month(YearMonth(year, month))
                            )
                        )
                        onIntent(OverviewIntent.ShowPeriodPicker(false))
                    },
                )
            } else if (currentPeriod is OverviewPeriod.Year) {
                OverviewYearPickerDialog(
                    currentYear = currentPeriod.year,
                    minYear = state.minSelectableDateIso?.let { LocalDate.parse(it).year },
                    onDismiss = { onIntent(OverviewIntent.ShowPeriodPicker(false)) },
                    onConfirm = { year ->
                        onIntent(OverviewIntent.PeriodSelected(OverviewPeriod.Year(year)))
                        onIntent(OverviewIntent.ShowPeriodPicker(false))
                    },
                )
            }
        }

        if (state.showDateRangePicker) {
            val initStart = when (val p = currentPeriod) {
                is OverviewPeriod.DateRange -> Triple(p.startYear, p.startMonth, p.startDay)
                is OverviewPeriod.Month -> Triple(p.yearMonth.year, p.yearMonth.monthNumber, 1)
                is OverviewPeriod.Year -> Triple(p.year, 1, 1)
            }
            val initEnd = when (val p = currentPeriod) {
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
                onDismiss = { onIntent(OverviewIntent.ShowDateRangePicker(false)) },
                onConfirm = { sy, sm, sd, ey, em, ed ->
                    onIntent(
                        OverviewIntent.DateRangeSelected(sy, sm, sd, ey, em, ed)
                    )
                    onIntent(OverviewIntent.ShowDateRangePicker(false))
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
