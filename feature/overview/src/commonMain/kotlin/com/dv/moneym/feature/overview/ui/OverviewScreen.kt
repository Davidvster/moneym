package com.dv.moneym.feature.overview.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.CumulativeChart
import com.dv.moneym.core.ui.DonutChart
import com.dv.moneym.core.ui.DonutSlice
import com.dv.moneym.core.ui.MiniCumulativeLine
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.overview.presentation.CategorySpend
import com.dv.moneym.feature.overview.presentation.CategoryTrend
import com.dv.moneym.feature.overview.presentation.OverviewIntent
import com.dv.moneym.feature.overview.presentation.OverviewPeriod
import com.dv.moneym.feature.overview.presentation.OverviewUiState
import com.dv.moneym.feature.overview.presentation.OverviewViewModel
import kotlinx.serialization.Serializable
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_avg_day
import moneym.feature.overview.generated.resources.overview_avg_month
import moneym.feature.overview.generated.resources.overview_cancel
import moneym.feature.overview.generated.resources.overview_cumulative_spend
import moneym.feature.overview.generated.resources.overview_daily_trend
import moneym.feature.overview.generated.resources.overview_dialog_select_month
import moneym.feature.overview.generated.resources.overview_dialog_select_year
import moneym.feature.overview.generated.resources.overview_label_expenses
import moneym.feature.overview.generated.resources.overview_label_income
import moneym.feature.overview.generated.resources.overview_label_total
import moneym.feature.overview.generated.resources.overview_month_apr
import moneym.feature.overview.generated.resources.overview_month_aug
import moneym.feature.overview.generated.resources.overview_month_dec
import moneym.feature.overview.generated.resources.overview_month_feb
import moneym.feature.overview.generated.resources.overview_month_jan
import moneym.feature.overview.generated.resources.overview_month_jul
import moneym.feature.overview.generated.resources.overview_month_jun
import moneym.feature.overview.generated.resources.overview_month_mar
import moneym.feature.overview.generated.resources.overview_month_may
import moneym.feature.overview.generated.resources.overview_month_nov
import moneym.feature.overview.generated.resources.overview_month_oct
import moneym.feature.overview.generated.resources.overview_month_sep
import moneym.feature.overview.generated.resources.overview_monthly_spending
import moneym.feature.overview.generated.resources.overview_monthly_trend
import moneym.feature.overview.generated.resources.overview_next_year_cd
import moneym.feature.overview.generated.resources.overview_no_expenses
import moneym.feature.overview.generated.resources.overview_now
import moneym.feature.overview.generated.resources.overview_ok
import moneym.feature.overview.generated.resources.overview_period_month
import moneym.feature.overview.generated.resources.overview_period_year
import moneym.feature.overview.generated.resources.overview_prev_year_cd
import moneym.feature.overview.generated.resources.overview_spending_by_category
import moneym.feature.overview.generated.resources.overview_through_day
import moneym.feature.overview.generated.resources.overview_title
import moneym.feature.overview.generated.resources.overview_tx_count_plural
import moneym.feature.overview.generated.resources.overview_tx_count_singular
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
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

// ─── Month names (localized) ──────────────────────────────────────────────────

@Composable
private fun localizedMonthNames(): List<String> = listOf(
    stringResource(Res.string.overview_month_jan),
    stringResource(Res.string.overview_month_feb),
    stringResource(Res.string.overview_month_mar),
    stringResource(Res.string.overview_month_apr),
    stringResource(Res.string.overview_month_may),
    stringResource(Res.string.overview_month_jun),
    stringResource(Res.string.overview_month_jul),
    stringResource(Res.string.overview_month_aug),
    stringResource(Res.string.overview_month_sep),
    stringResource(Res.string.overview_month_oct),
    stringResource(Res.string.overview_month_nov),
    stringResource(Res.string.overview_month_dec),
)

// ─── Root content ─────────────────────────────────────────────────────────────

@Composable
private fun OverviewContent(
    state: OverviewUiState,
    onIntent: (OverviewIntent) -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors
    val space = MM.space
    val monthNames = localizedMonthNames()
    val isMonthMode = state.period is OverviewPeriod.Month
    val periodLabel = when (val p = state.period) {
        is OverviewPeriod.Month -> "${monthNames[p.yearMonth.monthNumber - 1]} ${p.yearMonth.year}"
        is OverviewPeriod.Year -> p.year.toString()
    }
    val currencyCode = state.totalIncome.firstOrNull()?.currency?.value
        ?: state.totalExpense.firstOrNull()?.currency?.value
        ?: "EUR"

    var showPeriodPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = space.padding_2x),
        ) {
            item {
                OverviewHeader(
                    isMonthMode = isMonthMode,
                    periodLabel = periodLabel,
                    onTogglePeriod = { onIntent(OverviewIntent.TogglePeriod) },
                    onPreviousPeriod = { onIntent(OverviewIntent.PreviousPeriod) },
                    onNextPeriod = { onIntent(OverviewIntent.NextPeriod) },
                    onShowPeriodPicker = { showPeriodPicker = true },
                )
            }
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
                val currentPeriod = state.period as? OverviewPeriod.Month
                if (currentPeriod != null) {
                    OverviewMonthPickerDialog(
                        currentYear = currentPeriod.yearMonth.year,
                        currentMonth = currentPeriod.yearMonth.monthNumber,
                        onDismiss = { showPeriodPicker = false },
                        onConfirm = { year, month ->
                            onIntent(OverviewIntent.PeriodSelected(OverviewPeriod.Month(YearMonth(year, month))))
                            showPeriodPicker = false
                        },
                    )
                }
            } else {
                val currentPeriod = state.period as? OverviewPeriod.Year
                if (currentPeriod != null) {
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
        }
    }
}

// ─── OverviewHeader ───────────────────────────────────────────────────────────

@Composable
private fun OverviewHeader(
    isMonthMode: Boolean,
    periodLabel: String,
    onTogglePeriod: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onShowPeriodPicker: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val radius = MM.radius
    Column(
        Modifier.statusBarsPadding().padding(start = space.padding_2x, end = space.padding_2x, top = space.padding_0_5x, bottom = space.padding_2x),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.overview_title),
                style = type.title1,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            MmSegmented(
                options = listOf(stringResource(Res.string.overview_period_month), stringResource(Res.string.overview_period_year)),
                selectedIndex = if (isMonthMode) 0 else 1,
                onOptionSelected = { idx ->
                    if (idx == 0 && !isMonthMode) onTogglePeriod()
                    else if (idx == 1 && isMonthMode) onTogglePeriod()
                },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp),
        ) {
            MmIconButton(
                icon = MmIcons.chevronLeft,
                size = 32.dp,
                onClick = onPreviousPeriod,
            )
            Box(
                modifier = Modifier
                    .widthIn(min = 96.dp)
                    .clip(radius.radius_1x)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onShowPeriodPicker() }
                    .padding(horizontal = space.padding_0_5x, vertical = space.padding_0_25x),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = periodLabel,
                    style = type.body,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
            }
            MmIconButton(
                icon = MmIcons.chevronRight,
                size = 32.dp,
                onClick = onNextPeriod,
            )
        }
    }
}

// ─── OverviewPeriodBody ───────────────────────────────────────────────────────

@Composable
private fun OverviewPeriodBody(
    state: OverviewUiState,
    currencyCode: String,
) {
    val space = MM.space
    val periodOffset = state.periodOffset
    AnimatedContent(
        targetState = state.period,
        transitionSpec = {
            if (periodOffset != 0)
                slideInHorizontally(tween(280)) { it * periodOffset } togetherWith
                    slideOutHorizontally(tween(280)) { -it * periodOffset }
            else
                fadeIn(tween(180)) togetherWith fadeOut(tween(180))
        },
        label = "overview_period",
    ) { period ->
        val inMonthMode = period is OverviewPeriod.Month
        val avgDayLabel = stringResource(Res.string.overview_avg_day)
        val avgMonthLabel = stringResource(Res.string.overview_avg_month)
        Column(modifier = Modifier.fillMaxWidth()) {
            IncomeExpensesCard(
                income = state.income,
                expenses = state.expenses,
                currencyCode = currencyCode,
                modifier = Modifier.fillMaxWidth().padding(horizontal = space.padding_2x),
            )
            AvgStatsCard(
                inMonthMode = inMonthMode,
                avgDailyExpense = state.avgDailyExpense,
                avgMonthlyExpense = state.avgMonthlyExpense,
                avgDailyExpenseYear = state.avgDailyExpenseYear,
                avgDayLabel = avgDayLabel,
                avgMonthLabel = avgMonthLabel,
                currencyCode = currencyCode,
            )
            SpendingByCategoryCard(
                categories = state.categoryBreakdown,
                total = state.expenses,
                currencyCode = currencyCode,
                modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_1_5x),
            )
            if (inMonthMode) {
                CumulativeSpendCard(
                    cumulativeTotals = state.cumulativeTotals,
                    todayIndex = state.todayIndex,
                    currencyCode = currencyCode,
                )
                CategoryTrendsCard(
                    trends = state.categoryDailyTrend,
                    highlightIndex = state.todayIndex,
                    xLabels = listOf("1", "8", "15", "22", "31"),
                    title = stringResource(Res.string.overview_daily_trend),
                    modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_0_5x),
                )
            } else {
                MonthlySpendingBarChart(
                    monthlyTotals = state.monthlyTotals,
                    currentMonthIndex = state.currentMonthIndex,
                    currencyCode = currencyCode,
                )
                CategoryTrendsCard(
                    trends = state.categoryMonthlyTrend,
                    highlightIndex = state.currentMonthIndex,
                    xLabels = listOf("Jan", "Apr", "Jul", "Oct", "Dec"),
                    title = stringResource(Res.string.overview_monthly_trend),
                    modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_0_5x),
                )
            }
            Spacer(Modifier.height(space.padding_2x))
        }
    }
}

// ─── IncomeExpensesCard ───────────────────────────────────────────────────────

@Composable
private fun IncomeExpensesCard(
    income: Double,
    expenses: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val space = MM.space
    MmCard(
        modifier = modifier,
        padded = true,
        shape = MM.radius.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = MmIcons.arrowDown,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(12.dp),
            )
            SectionLabel(
                text = stringResource(Res.string.overview_label_income),
                modifier = Modifier.weight(1f),
            )
            MmMoney(
                value = income,
                size = 17.sp,
                weight = FontWeight.SemiBold,
                color = colors.accent,
                currency = currencyCode,
            )
        }
        Spacer(Modifier.height(space.padding_1_25x))
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Spacer(Modifier.height(space.padding_1_25x))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = MmIcons.arrowUp,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(12.dp),
            )
            SectionLabel(
                text = stringResource(Res.string.overview_label_expenses),
                modifier = Modifier.weight(1f),
            )
            MmMoney(
                value = expenses,
                size = 17.sp,
                weight = FontWeight.SemiBold,
                currency = currencyCode,
            )
        }
    }
}

// ─── AvgStatsCard ─────────────────────────────────────────────────────────────

@Composable
private fun AvgStatsCard(
    inMonthMode: Boolean,
    avgDailyExpense: Double,
    avgMonthlyExpense: Double,
    avgDailyExpenseYear: Double,
    avgDayLabel: String,
    avgMonthLabel: String,
    currencyCode: String,
) {
    val space = MM.space
    if (inMonthMode && avgDailyExpense > 0) {
        MmCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = 6.dp),
            padded = true,
            shape = MM.radius.md,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel(text = avgDayLabel, modifier = Modifier.weight(1f))
                MmMoney(value = avgDailyExpense, size = 15.sp, weight = FontWeight.SemiBold, currency = currencyCode)
            }
        }
    }
    if (!inMonthMode && (avgMonthlyExpense > 0 || avgDailyExpenseYear > 0)) {
        MmCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = 6.dp),
            padded = true,
            shape = MM.radius.md,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionLabel(text = avgMonthLabel, modifier = Modifier.weight(1f))
                    MmMoney(value = avgMonthlyExpense, size = 15.sp, weight = FontWeight.SemiBold, currency = currencyCode)
                }
                Spacer(Modifier.height(space.padding_1x))
                HorizontalDivider(color = MM.colors.divider, thickness = 1.dp)
                Spacer(Modifier.height(space.padding_1x))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionLabel(text = avgDayLabel, modifier = Modifier.weight(1f))
                    MmMoney(value = avgDailyExpenseYear, size = 15.sp, weight = FontWeight.SemiBold, currency = currencyCode)
                }
            }
        }
    }
}

// ─── CumulativeSpendCard ──────────────────────────────────────────────────────

@Composable
private fun CumulativeSpendCard(
    cumulativeTotals: List<Double>,
    todayIndex: Int,
    currencyCode: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    MmCard(
        modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_0_5x),
        padded = true,
        shape = MM.radius.md,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.overview_cumulative_spend),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = currencyCode,
                    style = type.captionMono.copy(color = colors.text3),
                )
            }
            Spacer(Modifier.height(space.padding_1x))
            val todayTotal = cumulativeTotals.getOrElse(todayIndex) { 0.0 }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                MmMoney(value = todayTotal, size = 22.sp, weight = FontWeight.SemiBold)
                Text(
                    text = stringResource(Res.string.overview_through_day, todayIndex + 1),
                    style = type.caption.copy(color = colors.text2),
                )
            }
            Spacer(Modifier.height(space.padding_2x))
            if (cumulativeTotals.isNotEmpty()) {
                CumulativeChart(
                    values = cumulativeTotals,
                    todayIndex = todayIndex,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
            }
            Spacer(Modifier.height(space.padding_0_5x))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("1", "8", "15", "22", "31").forEach { label ->
                    Text(
                        text = label,
                        style = type.captionMono.copy(fontSize = 10.sp, color = colors.text3),
                    )
                }
            }
        }
    }
}

// ─── MonthlySpendingBarChart ──────────────────────────────────────────────────

private val CHART_HEIGHT = 120.dp
private val YAXIS_WIDTH = 44.dp

@Composable
private fun MonthlySpendingBarChart(
    monthlyTotals: List<Double>,
    currentMonthIndex: Int,
    currencyCode: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val monthNames = localizedMonthNames().map { it.take(3) }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val maxVal = monthlyTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val avgVal = monthlyTotals.filter { it > 0 }.let { nonZero ->
        if (nonZero.isNotEmpty()) nonZero.average() else 0.0
    }
    val avgFraction = (avgVal / maxVal).toFloat().coerceIn(0f, 1f)

    MmCard(
        modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_0_5x),
        padded = true,
        shape = MM.radius.md,
    ) {
        Column {
            // Header: title + currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.overview_monthly_spending),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = currencyCode,
                    style = type.captionMono.copy(color = colors.text3),
                )
            }
            Spacer(Modifier.height(space.padding_2x))

            // Chart area: Y-axis + bars (fixed height so month labels don't overlap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Y-axis labels column
                Column(
                    modifier = Modifier
                        .width(YAXIS_WIDTH)
                        .height(CHART_HEIGHT),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatAxisAmount(maxVal),
                        style = type.captionMono.copy(fontSize = 9.sp, color = colors.text3),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = formatAxisAmount(maxVal / 2),
                        style = type.captionMono.copy(fontSize = 9.sp, color = colors.text3),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "0",
                        style = type.captionMono.copy(fontSize = 9.sp, color = colors.text3),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Bar chart area (with dotted avg line overlay)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(CHART_HEIGHT),
                ) {
                    // Dashed average line drawn with Canvas
                    if (avgVal > 0) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
                            val avgY = size.height * (1f - avgFraction)
                            drawLine(
                                color = colors.accent.copy(alpha = 0.45f),
                                start = Offset(0f, avgY),
                                end = Offset(size.width, avgY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                            )
                        }
                    }

                    // Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CHART_HEIGHT),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        monthlyTotals.forEachIndexed { i, value ->
                            val isCurrent = i == currentMonthIndex
                            val isSelected = i == selectedBarIndex
                            val barFraction = (value / maxVal).toFloat().coerceIn(0f, 1f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                // Value label above bar when tapped
                                if (isSelected && value > 0) {
                                    Text(
                                        text = formatBarAmount(value),
                                        style = type.captionMono.copy(fontSize = 8.sp),
                                        color = colors.text,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(bottom = 2.dp),
                                        maxLines = 1,
                                    )
                                }
                                // The bar itself
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .fillMaxHeight(barFraction.coerceAtLeast(0.01f))
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            when {
                                                isSelected -> colors.accent
                                                isCurrent -> colors.text
                                                else -> colors.borderStrong
                                            },
                                        )
                                        .alpha(if (value == 0.0) 0.3f else 1f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) { selectedBarIndex = if (isSelected) null else i },
                                )
                            }
                        }
                    }
                }
            }

            // Month name labels in their own row — completely separated from bars so no overlap
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = YAXIS_WIDTH + 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                monthNames.forEachIndexed { i, name ->
                    val isCurrent = i == currentMonthIndex
                    val isSelected = i == selectedBarIndex
                    Text(
                        text = name,
                        style = type.captionMono.copy(
                            fontSize = 9.sp,
                            color = when {
                                isSelected -> colors.accent
                                isCurrent -> colors.text
                                else -> colors.text3
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ─── SpendingByCategoryCard ────────────────────────────────────────────────────

@Composable
private fun SpendingByCategoryCard(
    categories: List<CategorySpend>,
    total: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space

    var showPercent by remember { mutableStateOf(true) }

    MmCard(modifier = modifier, padded = true, shape = MM.radius.lg) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.overview_spending_by_category),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                MmSegmented(
                    options = listOf("%", currencyCode),
                    selectedIndex = if (showPercent) 0 else 1,
                    onOptionSelected = { showPercent = it == 0 },
                    size = MmSegmentedSize.Sm,
                )
            }

            Spacer(Modifier.height(space.padding_2x))

            if (categories.isEmpty()) {
                Text(
                    text = stringResource(Res.string.overview_no_expenses),
                    style = type.caption,
                    color = colors.text3,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.padding_2x),
                ) {
                    val slices = categories.map {
                        DonutSlice(
                            color = Color(it.categoryColor),
                            fraction = it.percent.toFloat() / 100f,
                        )
                    }
                    DonutChart(
                        slices = slices,
                        modifier = Modifier.size(130.dp),
                        strokeWidth = 18.dp,
                    )
                    SpendingByCategoryLegend(
                        categories = categories,
                        total = total,
                        currencyCode = currencyCode,
                        showPercent = showPercent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ─── SpendingByCategoryLegend ─────────────────────────────────────────────────

@Composable
private fun SpendingByCategoryLegend(
    categories: List<CategorySpend>,
    total: Double,
    currencyCode: String,
    showPercent: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(stringResource(Res.string.overview_label_total))
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.divider),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (showPercent) "100%" else "$currencyCode ${formatAmount(total)}",
                style = type.captionMono,
                color = colors.text,
            )
        }

        Spacer(Modifier.height(6.dp))

        categories.forEach { cat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(cat.categoryColor)),
                )
                Text(
                    text = cat.categoryName,
                    style = type.caption,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (showPercent) {
                        "${cat.percent}%"
                    } else {
                        "$currencyCode ${formatAmount(cat.amount)}"
                    },
                    style = type.captionMono,
                    color = colors.text2,
                )
            }
        }
    }
}

// ─── CategoryTrendsCard ───────────────────────────────────────────────────────

@Composable
private fun CategoryTrendsCard(
    trends: List<CategoryTrend>,
    highlightIndex: Int,
    xLabels: List<String>,
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space

    if (trends.isEmpty()) return

    MmCard(modifier = modifier, padded = false, shape = MM.radius.sm) {
        Column {
            Text(
                text = title,
                style = type.title3,
                color = colors.text,
                modifier = Modifier.padding(horizontal = space.padding_3x, vertical = 14.dp),
            )

            trends.forEachIndexed { index, trend ->
                Column(
                    modifier = Modifier.padding(horizontal = space.padding_3x, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space.padding_1_25x),
                    ) {
                        CategoryIconTile(
                            categoryName = trend.categoryName,
                            categoryColor = Color(trend.categoryColor),
                            categoryIcon = iconForKey(trend.categoryIcon),
                            size = 32.dp,
                            variant = IndicatorStyle.IconTile,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trend.categoryName,
                                style = type.body,
                                color = colors.text,
                            )
                            Text(
                                text = if (trend.txCount == 1) {
                                    stringResource(Res.string.overview_tx_count_singular, trend.txCount)
                                } else {
                                    stringResource(Res.string.overview_tx_count_plural, trend.txCount)
                                },
                                style = type.caption.copy(color = colors.text2),
                            )
                        }
                        MmMoney(
                            value = trend.totalAmount,
                            size = 15.sp,
                            weight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.height(space.padding_1x))
                    MiniCumulativeLine(
                        data = trend.series,
                        color = Color(trend.categoryColor),
                        upToIndex = if (highlightIndex >= 0) highlightIndex else -1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                    )
                }

                if (index < trends.lastIndex) {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 1.dp,
                    )
                }
            }
        }
    }
}

// ─── Overview Period Picker Dialogs ───────────────────────────────────────────

@Composable
private fun OverviewMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    val todayDate = remember {
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val nowYear = todayDate.year
    val nowMonth = todayDate.monthNumber

    val monthNames = localizedMonthNames().map { it.take(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.overview_dialog_select_month), style = type.title3, color = colors.text)
        },
        text = {
            OverviewMonthPickerContent(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                nowYear = nowYear,
                nowMonth = nowMonth,
                monthNames = monthNames,
                onYearDecrement = { selectedYear-- },
                onYearIncrement = { selectedYear++ },
                onMonthSelected = { selectedMonth = it },
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.padding_0_5x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onConfirm(nowYear, nowMonth) }) {
                    Text(stringResource(Res.string.overview_now), color = colors.text2)
                }
                TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                    Text(stringResource(Res.string.overview_ok), color = colors.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.overview_cancel), color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}

@Composable
private fun OverviewMonthPickerContent(
    selectedYear: Int,
    selectedMonth: Int,
    nowYear: Int,
    nowMonth: Int,
    monthNames: List<String>,
    onYearDecrement: () -> Unit,
    onYearIncrement: () -> Unit,
    onMonthSelected: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val radius = MM.radius
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space.padding_2x),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MmIconButton(
                icon = MmIcons.chevronLeft,
                onClick = onYearDecrement,
                size = 32.dp,
                contentDescription = stringResource(Res.string.overview_prev_year_cd),
            )
            Text(
                text = selectedYear.toString(),
                style = type.body,
                color = if (selectedYear == nowYear) colors.accent else colors.text,
                modifier = Modifier.widthIn(min = 64.dp),
                textAlign = TextAlign.Center,
            )
            MmIconButton(
                icon = MmIcons.chevronRight,
                onClick = onYearIncrement,
                size = 32.dp,
                contentDescription = stringResource(Res.string.overview_next_year_cd),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
            for (row in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0..2) {
                        val m = row * 3 + col + 1
                        val isSelected = m == selectedMonth
                        val isNow = m == nowMonth && selectedYear == nowYear
                        Box(
                            modifier = Modifier
                                .clip(radius.radius_1x)
                                .background(
                                    if (isSelected) colors.accent else Color.Transparent,
                                )
                                .then(
                                    if (isNow && !isSelected) {
                                        Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), radius.radius_1x)
                                    } else Modifier
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { onMonthSelected(m) }
                                .padding(horizontal = space.padding_1_5x, vertical = space.padding_1x),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = monthNames[m - 1],
                                style = type.body,
                                color = when {
                                    isSelected -> colors.bg
                                    isNow -> colors.accent
                                    else -> colors.text
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewYearPickerDialog(
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val nowYear = remember {
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.year
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.overview_dialog_select_year), style = type.title3, color = colors.text)
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MmIconButton(
                    icon = MmIcons.chevronLeft,
                    onClick = { selectedYear-- },
                    size = 32.dp,
                    contentDescription = stringResource(Res.string.overview_prev_year_cd),
                )
                Text(
                    text = selectedYear.toString(),
                    style = type.body,
                    color = if (selectedYear == nowYear) colors.accent else colors.text,
                    modifier = Modifier.widthIn(min = 80.dp),
                    textAlign = TextAlign.Center,
                )
                MmIconButton(
                    icon = MmIcons.chevronRight,
                    onClick = { selectedYear++ },
                    size = 32.dp,
                    contentDescription = stringResource(Res.string.overview_next_year_cd),
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.padding_0_5x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onConfirm(nowYear) }) {
                    Text(stringResource(Res.string.overview_now), color = colors.text2)
                }
                TextButton(onClick = { onConfirm(selectedYear) }) {
                    Text(stringResource(Res.string.overview_ok), color = colors.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.overview_cancel), color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}

// ─── Utilities ────────────────────────────────────────────────────────────────

private fun formatAmount(value: Double): String {
    val abs = kotlin.math.abs(value)
    val intPart = abs.toLong()
    val decPart = kotlin.math.round((abs - intPart) * 100).toInt()
    val intFormatted = buildString {
        val s = intPart.toString()
        var count = 0
        for (i in s.indices.reversed()) {
            if (count > 0 && count % 3 == 0) insert(0, ',')
            insert(0, s[i])
            count++
        }
    }
    return "$intFormatted.${decPart.toString().padStart(2, '0')}"
}

private fun formatAxisAmount(value: Double): String {
    return if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
}

private fun formatBarAmount(value: Double): String {
    return if (value >= 1000) {
        val k = (value / 100).toInt() / 10.0
        "${k}k"
    } else {
        value.toInt().toString()
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
