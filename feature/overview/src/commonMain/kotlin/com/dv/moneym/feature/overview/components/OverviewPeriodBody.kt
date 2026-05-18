package com.dv.moneym.feature.overview.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.DonutChart
import com.dv.moneym.core.ui.DonutSlice
import com.dv.moneym.core.ui.MiniBars
import com.dv.moneym.core.ui.MiniCumulativeLine
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.overview.CategorySpend
import com.dv.moneym.feature.overview.CategoryTrend
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.OverviewUiState
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_avg_day
import moneym.feature.overview.generated.resources.overview_avg_month
import moneym.feature.overview.generated.resources.overview_daily_trend
import moneym.feature.overview.generated.resources.overview_label_total
import moneym.feature.overview.generated.resources.overview_monthly_trend
import moneym.feature.overview.generated.resources.overview_no_expenses
import moneym.feature.overview.generated.resources.overview_spending_by_category
import moneym.feature.overview.generated.resources.overview_tx_count_plural
import moneym.feature.overview.generated.resources.overview_tx_count_singular
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OverviewPeriodBody(
    state: OverviewUiState,
    currencyCode: String,
) {
    val space = MM.dimen
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
        val inYearMode = period is OverviewPeriod.Year
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
                modifier = Modifier.padding(
                    horizontal = space.padding_2x,
                    vertical = space.padding_1_5x
                ),
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
                    showBars = false,
                    modifier = Modifier.padding(
                        horizontal = space.padding_2x,
                        vertical = space.padding_0_5x
                    ),
                )
            } else if (inYearMode) {
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
                    showBars = true,
                    modifier = Modifier.padding(
                        horizontal = space.padding_2x,
                        vertical = space.padding_0_5x
                    ),
                )
            } else {
                // DateRange mode — show category breakdown only, no time-series charts
                if (state.categoryDailyTrend.isNotEmpty()) {
                    CategoryTrendsCard(
                        trends = state.categoryDailyTrend,
                        highlightIndex = -1,
                        xLabels = emptyList(),
                        title = stringResource(Res.string.overview_daily_trend),
                        showBars = true,
                        modifier = Modifier.padding(
                            horizontal = space.padding_2x,
                            vertical = space.padding_0_5x
                        ),
                    )
                }
            }
            Spacer(Modifier.height(space.padding_2x))
        }
    }
}


@Composable
private fun SpendingByCategoryCard(
    categories: List<CategorySpend>,
    total: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    var showPercent by remember { mutableStateOf(true) }

    MmCard(modifier = modifier, padded = true, shape = MM.dimen.radius_2x) {
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
                        strokeWidth = MM.dimen.donutWidth,
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
    val space = MM.dimen
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

@Composable
private fun CategoryTrendsCard(
    trends: List<CategoryTrend>,
    highlightIndex: Int,
    xLabels: List<String>,
    title: String,
    showBars: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    if (trends.isEmpty()) return

    MmCard(modifier = modifier, padded = false, shape = MM.dimen.radius_1x) {
        Column {
            Text(
                text = title,
                style = type.title3,
                color = colors.text,
                modifier = Modifier.padding(
                    horizontal = space.padding_3x,
                    vertical = MM.dimen.padding_2x
                ),
            )

            trends.forEachIndexed { index, trend ->
                Column(
                    modifier = Modifier.padding(
                        horizontal = space.padding_3x,
                        vertical = MM.dimen.padding_2x
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space.padding_1_25x),
                    ) {
                        CategoryIconTile(
                            categoryName = trend.categoryName,
                            categoryColor = Color(trend.categoryColor),
                            categoryIcon = trend.categoryIcon.imageVector,
                            size = MM.dimen.padding_4x,
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
                                    stringResource(
                                        Res.string.overview_tx_count_singular,
                                        trend.txCount
                                    )
                                } else {
                                    stringResource(
                                        Res.string.overview_tx_count_plural,
                                        trend.txCount
                                    )
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
                    if (showBars) {
                        MiniBars(
                            data = trend.series,
                            color = Color(trend.categoryColor),
                            highlightIndex = if (highlightIndex >= 0) highlightIndex else -1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MM.dimen.padding_4x),
                        )
                    } else {
                        MiniCumulativeLine(
                            data = trend.series,
                            color = Color(trend.categoryColor),
                            upToIndex = if (highlightIndex >= 0) highlightIndex else -1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MM.dimen.padding_4x),
                        )
                    }
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