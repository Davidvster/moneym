package com.dv.moneym.feature.overview.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.CumulativeChart
import com.dv.moneym.core.ui.DonutChart
import com.dv.moneym.core.ui.DonutSlice
import com.dv.moneym.core.ui.MiniBars
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
import moneym.feature.overview.generated.resources.overview_cumulative_spend
import moneym.feature.overview.generated.resources.overview_daily_trend
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
import moneym.feature.overview.generated.resources.overview_no_expenses
import moneym.feature.overview.generated.resources.overview_period_month
import moneym.feature.overview.generated.resources.overview_period_year
import moneym.feature.overview.generated.resources.overview_spending_by_category
import moneym.feature.overview.generated.resources.overview_through_day
import moneym.feature.overview.generated.resources.overview_title
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
    val type = MM.type
    val monthNames = localizedMonthNames()

    val isMonthMode = state.period is OverviewPeriod.Month

    val periodLabel = when (val p = state.period) {
        is OverviewPeriod.Month -> "${monthNames[p.yearMonth.monthNumber - 1]} ${p.yearMonth.year}"
        is OverviewPeriod.Year -> p.year.toString()
    }

    // Currency code from the first income or expense entry, fallback to EUR
    val currencyCode = state.totalIncome.firstOrNull()?.currency?.value
        ?: state.totalExpense.firstOrNull()?.currency?.value
        ?: "EUR"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {

        // ── Header ────────────────────────────────────────────────
        item {
            Column(
                Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
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
                            if (idx == 0 && !isMonthMode) onIntent(OverviewIntent.TogglePeriod)
                            else if (idx == 1 && isMonthMode) onIntent(OverviewIntent.TogglePeriod)
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
                        onClick = { onIntent(OverviewIntent.PreviousPeriod) },
                    )
                    Text(
                        text = periodLabel,
                        style = type.body,
                        color = colors.text,
                        modifier = Modifier.widthIn(min = 96.dp),
                        textAlign = TextAlign.Center,
                    )
                    MmIconButton(
                        icon = MmIcons.chevronRight,
                        size = 32.dp,
                        onClick = { onIntent(OverviewIntent.NextPeriod) },
                    )
                }
            }
        }

        // ── Period-animated body ──────────────────────────────────
        item {
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Income + Expenses cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MmCard(modifier = Modifier.weight(1f), padded = true, shape = MM.radius.md) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = MmIcons.arrowDown,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    SectionLabel(stringResource(Res.string.overview_label_income))
                                }
                                Spacer(Modifier.height(6.dp))
                                MmMoney(
                                    value = state.income,
                                    size = 20.sp,
                                    weight = FontWeight.SemiBold,
                                    color = colors.accent,
                                )
                            }
                        }
                        MmCard(modifier = Modifier.weight(1f), padded = true, shape = MM.radius.md) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = MmIcons.arrowUp,
                                        contentDescription = null,
                                        tint = colors.text,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    SectionLabel(stringResource(Res.string.overview_label_expenses))
                                }
                                Spacer(Modifier.height(6.dp))
                                MmMoney(
                                    value = state.expenses,
                                    size = 20.sp,
                                    weight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    // Spending by category
                    SpendingByCategoryCard(
                        categories = state.categoryBreakdown,
                        total = state.expenses,
                        currencyCode = currencyCode,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )

                    if (inMonthMode) {
                        // Cumulative spend
                        MmCard(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                                Spacer(Modifier.height(8.dp))
                                val todayTotal = state.cumulativeTotals.getOrElse(state.todayIndex) { 0.0 }
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    MmMoney(value = todayTotal, size = 22.sp, weight = FontWeight.SemiBold)
                                    Text(
                                        text = stringResource(Res.string.overview_through_day, state.todayIndex + 1),
                                        style = type.caption.copy(color = colors.text2),
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                if (state.cumulativeTotals.isNotEmpty()) {
                                    CumulativeChart(
                                        values = state.cumulativeTotals,
                                        todayIndex = state.todayIndex,
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
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

                        // Daily trend by category
                        CategoryTrendsCard(
                            trends = state.categoryDailyTrend,
                            highlightIndex = state.todayIndex,
                            xLabels = listOf("1", "8", "15", "22", "31"),
                            title = stringResource(Res.string.overview_daily_trend),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    } else {
                        // Monthly spending bar chart
                        MmCard(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            padded = true,
                            shape = MM.radius.md,
                        ) {
                            Column {
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
                                Spacer(Modifier.height(16.dp))
                                val maxVal = state.monthlyTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                                val monthNames2 = listOf(
                                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    state.monthlyTotals.forEachIndexed { i, value ->
                                        val isCurrent = i == state.currentMonthIndex
                                        val barFraction = (value / maxVal).toFloat().coerceIn(0f, 1f)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxHeight(),
                                        ) {
                                            Spacer(Modifier.weight(1f))
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .fillMaxHeight(barFraction.coerceAtLeast(0.015f))
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (isCurrent) colors.text else colors.borderStrong)
                                                    .alpha(if (value == 0.0) 0.4f else 1f),
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = monthNames2[i],
                                                style = type.captionMono.copy(
                                                    fontSize = 10.sp,
                                                    color = if (isCurrent) colors.text else colors.text3,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Monthly trend by category
                        CategoryTrendsCard(
                            trends = state.categoryMonthlyTrend,
                            highlightIndex = state.currentMonthIndex,
                            xLabels = listOf("Jan", "Apr", "Jul", "Oct", "Dec"),
                            title = stringResource(Res.string.overview_monthly_trend),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

    }
    MmTabBar(
        activeTab = TabRoute.Overview,
        onTabSelected = onTabSelected,
    )
    } // end outer Column
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

    var showPercent by remember { mutableStateOf(true) }

    MmCard(modifier = modifier, padded = true, shape = MM.radius.lg) {
        Column {
            // Title row + toggle
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

            Spacer(Modifier.height(16.dp))

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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Donut chart
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

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        // TOTAL row
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

                        // Per-category rows
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                // Colored dot
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

    if (trends.isEmpty()) return

    MmCard(modifier = modifier, padded = false, shape = MM.radius.sm) {
        Column {
            // Header
            Text(
                text = title,
                style = type.title3,
                color = colors.text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )

            trends.forEachIndexed { index, trend ->
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                text = "${trend.txCount} transaction${if (trend.txCount == 1) "" else "s"}",
                                style = type.caption.copy(color = colors.text2),
                            )
                        }
                        MmMoney(
                            value = trend.totalAmount,
                            size = 15.sp,
                            weight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    MiniBars(
                        data = trend.series,
                        color = Color(trend.categoryColor),
                        highlightIndex = if (highlightIndex >= 0) highlightIndex else -1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                    )
                }

                // Divider between rows (not after last)
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
