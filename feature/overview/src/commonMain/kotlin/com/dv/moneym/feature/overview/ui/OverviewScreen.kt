package com.dv.moneym.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.overview.presentation.OverviewIntent
import com.dv.moneym.feature.overview.presentation.OverviewPeriod
import com.dv.moneym.feature.overview.presentation.OverviewUiState
import com.dv.moneym.feature.overview.presentation.OverviewViewModel
import com.dv.moneym.feature.overview.ui.components.DateRangePickerDialog
import com.dv.moneym.feature.overview.ui.components.OverviewHeader
import com.dv.moneym.feature.overview.ui.components.OverviewMonthPickerDialog
import com.dv.moneym.feature.overview.ui.components.OverviewPeriodBody
import com.dv.moneym.feature.overview.ui.components.OverviewYearPickerDialog
import com.dv.moneym.feature.overview.ui.components.daysInMonthUi
import com.dv.moneym.feature.overview.ui.components.formatShortDate
import com.dv.moneym.feature.overview.ui.components.localizedMonthNames
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
fun OverviewScreen(
    viewModel: OverviewViewModel = koinViewModel(),
    onTabSelected: (TabRoute) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    OverviewContent(
        state = state,
        onIntent = viewModel::onIntent,
        onTabSelected = onTabSelected,
    )
}

// ─── Swipe-to-navigate modifier ───────────────────────────────────────────────

private fun Modifier.onHorizontalSwipe(
    thresholdDp: Float = 60f,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = this.pointerInput(Unit) {
    val thresholdPx = thresholdDp * density
    var totalX = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalX = 0f },
        onDragEnd = {
            when {
                totalX > thresholdPx -> onSwipeRight()
                totalX < -thresholdPx -> onSwipeLeft()
            }
            totalX = 0f
        },
        onHorizontalDrag = { change, delta ->
            change.consume()
            totalX += delta
        },
    )
}

// ─── Root content ─────────────────────────────────────────────────────────────

@Composable
private fun OverviewContent(
    state: OverviewUiState,
    onIntent: (OverviewIntent) -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val monthNames = localizedMonthNames()
    val isMonthMode = state.period is OverviewPeriod.Month
    val periodLabel = when (val p = state.period) {
        is OverviewPeriod.Month -> "${monthNames[p.yearMonth.monthNumber - 1]} ${p.yearMonth.year}"
        is OverviewPeriod.Year -> p.year.toString()
        is OverviewPeriod.DateRange -> "${
            formatShortDate(
                p.startYear,
                p.startMonth,
                p.startDay
            )
        } – ${formatShortDate(p.endYear, p.endMonth, p.endDay)}"
    }
    val currencyCode = state.totalIncome.firstOrNull()?.currency?.value
        ?: state.totalExpense.firstOrNull()?.currency?.value
        ?: "EUR"

    var showPeriodPicker by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Fixed header — stays pinned at top during scroll
        OverviewHeader(
            period = state.period,
            periodLabel = periodLabel,
            onTogglePeriod = { onIntent(OverviewIntent.TogglePeriod) },
            onPreviousPeriod = { onIntent(OverviewIntent.PreviousPeriod) },
            onNextPeriod = { onIntent(OverviewIntent.NextPeriod) },
            onShowPeriodPicker = { showPeriodPicker = true },
            onShowDateRangePicker = { showDateRangePicker = true },
        )

        // Scrollable body with swipe gesture
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .onHorizontalSwipe(
                    onSwipeLeft = { onIntent(OverviewIntent.NextPeriod) },
                    onSwipeRight = { onIntent(OverviewIntent.PreviousPeriod) },
                ),
            contentPadding = PaddingValues(bottom = space.padding_2x),
        ) {
            item {
                OverviewPeriodBody(
                    state = state,
                    currencyCode = currencyCode,
                )
            }
        }

        MmTabBar(
            activeTab = TabRoute.Overview,
            onTabSelected = onTabSelected,
        )

        if (showPeriodPicker) {
            if (isMonthMode) {
                val currentPeriod = state.period
                OverviewMonthPickerDialog(
                    currentYear = currentPeriod.yearMonth.year,
                    currentMonth = currentPeriod.yearMonth.monthNumber,
                    onDismiss = { showPeriodPicker = false },
                    onConfirm = { year, month ->
                        onIntent(
                            OverviewIntent.PeriodSelected(
                                OverviewPeriod.Month(
                                    YearMonth(
                                        year,
                                        month
                                    )
                                )
                            )
                        )
                        showPeriodPicker = false
                    },
                )
            } else if (state.period is OverviewPeriod.Year) {
                val currentPeriod = state.period
                OverviewYearPickerDialog(
                    currentYear = currentPeriod.year,
                    onDismiss = { showPeriodPicker = false },
                    onConfirm = { year ->
                        onIntent(OverviewIntent.PeriodSelected(OverviewPeriod.Year(year)))
                        showPeriodPicker = false
                    },
                )
            }
        }

        if (showDateRangePicker) {
            val initStart = when (val p = state.period) {
                is OverviewPeriod.DateRange -> Triple(p.startYear, p.startMonth, p.startDay)
                is OverviewPeriod.Month -> Triple(p.yearMonth.year, p.yearMonth.monthNumber, 1)
                is OverviewPeriod.Year -> Triple(p.year, 1, 1)
            }
            val initEnd = when (val p = state.period) {
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
            state = OverviewUiState(isLoading = false, isEmpty = true),
            onIntent = {},
            onTabSelected = {},
        )
    }
}
