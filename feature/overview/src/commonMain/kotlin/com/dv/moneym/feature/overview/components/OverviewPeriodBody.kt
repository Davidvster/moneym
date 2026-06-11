package com.dv.moneym.feature.overview.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.currencyDisplay
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.LocalUseCurrencySymbol
import com.dv.moneym.core.uigraphs.CumulativeChart
import com.dv.moneym.core.uigraphs.DonutChart
import com.dv.moneym.core.uigraphs.DonutSlice
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmEmptyState
import com.dv.moneym.core.ui.mmStaggeredAppear
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.overview.CategorySpend
import com.dv.moneym.feature.overview.CategoryTrend
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.page.OverviewPageIntent
import com.dv.moneym.feature.overview.page.OverviewPageUiState
import com.dv.moneym.feature.overview.usecase.BudgetProgress
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_avg_day
import moneym.feature.overview.generated.resources.overview_avg_month
import moneym.feature.overview.generated.resources.overview_cat_avg_day
import moneym.feature.overview.generated.resources.overview_cat_avg_month
import moneym.feature.overview.generated.resources.overview_daily_trend
import moneym.feature.overview.generated.resources.overview_month_x_labels
import moneym.feature.overview.generated.resources.overview_monthly_trend
import moneym.feature.overview.generated.resources.overview_no_expenses
import moneym.feature.overview.generated.resources.overview_spending_by_category
import moneym.feature.overview.generated.resources.overview_tx_count_plural
import moneym.feature.overview.generated.resources.overview_tx_count_singular
import moneym.feature.overview.generated.resources.overview_year_x_labels
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OverviewPeriodBody(
    state: OverviewPageUiState,
    spendingFilter: SpendingFilter,
    currencyCode: String,
    onIntent: (OverviewPageIntent) -> Unit = {},
) {
    val space = MM.dimen
    val period = state.period
    val inMonthMode = period is OverviewPeriod.Month
    val inYearMode = period is OverviewPeriod.Year
    val avgDayLabel = stringResource(Res.string.overview_avg_day)
    val avgMonthLabel = stringResource(Res.string.overview_avg_month)
    Column(modifier = Modifier.fillMaxWidth()) {
        IncomeExpensesCard(
            income = state.income,
            expenses = state.expenses,
            currencyCode = currencyCode,
            filter = spendingFilter,
            modifier = Modifier
                .mmStaggeredAppear(0)
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x),
        )
        if (state.budgetProgress.isNotEmpty()) {
            BudgetBreakdownCard(
                progress = state.budgetProgress,
                modifier = Modifier
                    .mmStaggeredAppear(1)
                    .fillMaxWidth()
                    .padding(horizontal = space.padding_2x, vertical = space.padding_1_5x),
            )
        }
        AvgStatsCard(
            inMonthMode = inMonthMode,
            avgDailyExpense = state.avgDailyExpense,
            avgMonthlyExpense = state.avgMonthlyExpense,
            avgDailyExpenseYear = state.avgDailyExpenseYear,
            avgDayLabel = avgDayLabel,
            avgMonthLabel = avgMonthLabel,
            currencyCode = currencyCode,
            modifier = Modifier.mmStaggeredAppear(2),
        )
        SpendingByCategoryCard(
            expenseCategories = state.categoryBreakdown,
            incomeCategories = state.categoryIncomeBreakdown,
            totalExpenses = state.expenses,
            totalIncome = state.income,
            currencyCode = currencyCode,
            selectedSliceIndex = state.selectedSliceIndex,
            inYearMode = inYearMode,
            filter = spendingFilter,
            onSliceTapped = { onIntent(OverviewPageIntent.SliceTapped(it)) },
            modifier = Modifier
                .mmStaggeredAppear(3)
                .fillMaxWidth()
                .padding(
                    horizontal = space.padding_2x,
                    vertical = space.padding_1_5x
                ),
        )
        if (inMonthMode) {
            CumulativeSpendCard(
                cumulativeTotals = state.cumulativeTotals,
                todayIndex = state.todayIndex,
                currencyCode = currencyCode,
                modifier = Modifier.mmStaggeredAppear(4),
            )
            CategoryTrendsCard(
                trends = state.categoryDailyTrend,
                highlightIndex = state.todayIndex,
                xLabels = stringArrayResource(Res.array.overview_month_x_labels),
                title = stringResource(Res.string.overview_daily_trend),
                showBars = false,
                currencyCode = currencyCode,
                modifier = Modifier
                    .mmStaggeredAppear(5)
                    .padding(
                        horizontal = space.padding_2x,
                        vertical = space.padding_0_5x
                    ),
            )
        } else if (inYearMode) {
            MonthlySpendingBarChart(
                monthlyTotals = state.monthlyTotals,
                currentMonthIndex = state.currentMonthIndex,
                currencyCode = currencyCode,
                modifier = Modifier.mmStaggeredAppear(4),
            )
            CategoryTrendsCard(
                trends = state.categoryMonthlyTrend,
                highlightIndex = state.currentMonthIndex,
                xLabels = stringArrayResource(Res.array.overview_year_x_labels),
                title = stringResource(Res.string.overview_monthly_trend),
                showBars = true,
                currencyCode = currencyCode,
                modifier = Modifier
                    .mmStaggeredAppear(5)
                    .padding(
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
                    currencyCode = currencyCode,
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


@Composable
private fun SpendingByCategoryCard(
    expenseCategories: List<CategorySpend>,
    incomeCategories: List<CategorySpend>,
    totalExpenses: Double,
    totalIncome: Double,
    currencyCode: String,
    selectedSliceIndex: Int?,
    inYearMode: Boolean,
    filter: SpendingFilter,
    onSliceTapped: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    var showPercent by remember { mutableStateOf(true) }

    val total = when (filter) {
        SpendingFilter.All -> totalExpenses + totalIncome
        SpendingFilter.Expenses -> totalExpenses
        SpendingFilter.Income -> totalIncome
    }

    MmCard(modifier = modifier, padded = true, shape = MM.dimen.radius_2x) {
        Column {
            Text(
                text = stringResource(Res.string.overview_spending_by_category),
                style = type.title3,
                color = colors.text,
            )

            Spacer(Modifier.height(space.padding_2x))

            AnimatedContent(
                targetState = filter,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "spending_filter",
            ) { activeFilter ->
                val animCats =
                    resolveAnimCats(activeFilter, expenseCategories, incomeCategories, total)
                SpendingByCategoryCardBody(
                    animCats = animCats,
                    total = total,
                    currencyCode = currencyCode,
                    selectedSliceIndex = selectedSliceIndex,
                    inYearMode = inYearMode,
                    showPercent = showPercent,
                    onTogglePercent = { showPercent = !showPercent },
                    onSliceTapped = onSliceTapped,
                )
            }
        }
    }
}

private fun resolveAnimCats(
    activeFilter: SpendingFilter,
    expenseCategories: List<CategorySpend>,
    incomeCategories: List<CategorySpend>,
    total: Double,
): List<CategorySpend> = when (activeFilter) {
    SpendingFilter.All -> {
        val combined = expenseCategories + incomeCategories
        if (total > 0) combined.map { it.copy(percent = ((it.amount / total) * 100).toInt()) } else combined
    }

    SpendingFilter.Expenses -> expenseCategories
    SpendingFilter.Income -> incomeCategories
}

@Composable
private fun SpendingByCategoryCardBody(
    animCats: List<CategorySpend>,
    total: Double,
    currencyCode: String,
    selectedSliceIndex: Int?,
    inYearMode: Boolean,
    showPercent: Boolean,
    onTogglePercent: () -> Unit,
    onSliceTapped: (Int?) -> Unit,
) {
    val space = MM.dimen
    Column {
        if (animCats.isEmpty()) {
            MmEmptyState(
                message = stringResource(Res.string.overview_no_expenses),
                icon = Icon.Chart.imageVector,
                fillSize = false,
            )
        } else {
            DonutChartWithSelection(
                animCats = animCats,
                currencyCode = currencyCode,
                selectedSliceIndex = selectedSliceIndex,
                onSliceTapped = onSliceTapped,
            )

            Spacer(Modifier.height(space.padding_2x))

            SpendingByCategoryLegend(
                categories = animCats,
                total = total,
                currencyCode = currencyCode,
                showPercent = showPercent,
                inYearMode = inYearMode,
                onTogglePercent = onTogglePercent,
                selectedIndex = selectedSliceIndex,
                onRowClick = { i -> onSliceTapped(if (i == selectedSliceIndex) null else i) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DonutChartWithSelection(
    animCats: List<CategorySpend>,
    currencyCode: String,
    selectedSliceIndex: Int?,
    onSliceTapped: (Int?) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val displayCurrencyCode = currencyDisplay(currencyCode, LocalUseCurrencySymbol.current)
    val slices = animCats.map {
        DonutSlice(
            color = Color(it.categoryColor),
            fraction = it.percent.toFloat() / 100f,
        )
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        DonutChart(
            slices = slices,
            modifier = Modifier.size(180.dp),
            strokeWidth = MM.dimen.donutWidth,
            selectedIndex = selectedSliceIndex,
            onSliceClick = { i -> onSliceTapped(if (i == selectedSliceIndex) null else i) },
        )
        val selIdx = selectedSliceIndex
        if (selIdx != null && selIdx < animCats.size) {
            val cat = animCats[selIdx]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = cat.categoryName,
                    style = type.caption.copy(color = colors.text2),
                )
                Text(
                    text = "${cat.percent}%",
                    style = type.bodyMono.copy(color = colors.text),
                )
                Text(
                    text = "$displayCurrencyCode ${formatAmount(cat.amount)}",
                    style = type.captionMono.copy(color = colors.text2),
                )
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
    inYearMode: Boolean,
    onTogglePercent: () -> Unit,
    selectedIndex: Int? = null,
    onRowClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        LegendTotalToggle(
            currencyCode = currencyCode,
            showPercent = showPercent,
            onTogglePercent = onTogglePercent,
        )

        Spacer(Modifier.height(MM.dimen.padding_1x))
        HorizontalDivider(color = colors.divider, thickness = MM.dimen.strokeHairline)

        LegendHeaderRow(
            currencyCode = currencyCode,
            showPercent = showPercent,
            inYearMode = inYearMode,
        )

        HorizontalDivider(color = colors.divider, thickness = MM.dimen.strokeHairline)

        categories.forEachIndexed { i, cat ->
            LegendDataRow(
                cat = cat,
                index = i,
                selectedIndex = selectedIndex,
                showPercent = showPercent,
                inYearMode = inYearMode,
                onClick = { onRowClick(i) },
            )

            if (i < categories.lastIndex) {
                HorizontalDivider(color = colors.divider.copy(alpha = 0.5f), thickness = MM.dimen.strokeHairline)
            }
        }
    }
}

@Composable
private fun LegendTotalToggle(
    currencyCode: String,
    showPercent: Boolean,
    onTogglePercent: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        MmSegmented(
            options = listOf("%", currencyCode),
            selectedIndex = if (showPercent) 0 else 1,
            onOptionSelected = { if ((it == 0) != showPercent) onTogglePercent() },
            size = MmSegmentedSize.Sm,
        )
    }
}

@Composable
private fun LegendVDivider() {
    val colors = MM.colors
    Box(
        modifier = Modifier
            .width(MM.dimen.strokeHairline)
            .height(16.dp)
            .background(colors.divider),
    )
}

@Composable
private fun LegendHeaderRow(
    currencyCode: String,
    showPercent: Boolean,
    inYearMode: Boolean,
) {
    val colors = MM.colors
    val type = MM.type
    val colStyle = type.captionMono.copy(color = colors.text3)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MM.dimen.padding_0_5x),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
    ) {
        Spacer(Modifier.weight(0.3f))
        LegendVDivider()
        Text(
            text = if (showPercent) "%" else currencyCode,
            style = colStyle,
            modifier = Modifier.weight(0.25f),
            textAlign = TextAlign.End,
        )
        if (inYearMode) {
            LegendVDivider()
            Text(
                text = stringResource(Res.string.overview_cat_avg_month),
                style = colStyle,
                modifier = Modifier.weight(0.225f),
                textAlign = TextAlign.End
            )
        }
        LegendVDivider()
        Text(
            text = stringResource(Res.string.overview_cat_avg_day),
            style = colStyle,
            modifier = Modifier.weight(0.225f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LegendDataRow(
    cat: CategorySpend,
    index: Int,
    selectedIndex: Int?,
    showPercent: Boolean,
    inYearMode: Boolean,
    onClick: () -> Unit = {},
) {
    val colors = MM.colors
    val type = MM.type
    val isSelected = selectedIndex == index
    val hasSelection = selectedIndex != null
    val textColor = if (hasSelection && !isSelected) colors.text3 else colors.text
    val numColor = if (hasSelection && !isSelected) colors.text3 else colors.text2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(vertical = MM.dimen.padding_0_5x),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
    ) {
        Row(
            modifier = Modifier.weight(0.3f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(cat.categoryColor).copy(alpha = if (hasSelection && !isSelected) 0.4f else 1f)),
            )
            Text(
                text = cat.categoryName,
                style = type.caption,
                color = textColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LegendVDivider()
        Text(
            text = if (showPercent) "${cat.percent}%" else formatAmount(cat.amount),
            style = type.captionMono,
            color = numColor,
            modifier = Modifier.weight(0.25f),
            textAlign = TextAlign.End,
        )
        if (inYearMode) {
            LegendVDivider()
            Text(
                text = if (cat.avgPerMonth > 0) formatAmount(cat.avgPerMonth) else "—",
                style = type.captionMono,
                color = numColor,
                modifier = Modifier.weight(0.225f),
                textAlign = TextAlign.End,
            )
        }
        LegendVDivider()
        Text(
            text = if (cat.avgPerDay > 0) formatAmount(cat.avgPerDay) else "—",
            style = type.captionMono,
            color = numColor,
            modifier = Modifier.weight(0.225f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CategoryTrendsCard(
    trends: List<CategoryTrend>,
    highlightIndex: Int,
    xLabels: List<String>,
    title: String,
    showBars: Boolean = false,
    currencyCode: String = "",
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
                    TrendsHeader(trend = trend)
                    Spacer(Modifier.height(space.padding_1x))
                    if (showBars) {
                        TrendsBarSection(
                            trend = trend,
                            highlightIndex = highlightIndex,
                            xLabels = xLabels,
                            currencyCode = currencyCode,
                        )
                    } else {
                        TrendsCumulativeSection(
                            trend = trend,
                            highlightIndex = highlightIndex,
                            xLabels = xLabels,
                            currencyCode = currencyCode,
                        )
                    }
                }

                if (index < trends.lastIndex) {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = MM.dimen.strokeHairline,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendsHeader(trend: CategoryTrend) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
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
                    stringResource(Res.string.overview_tx_count_singular, trend.txCount)
                } else {
                    stringResource(Res.string.overview_tx_count_plural, trend.txCount)
                },
                style = type.caption.copy(color = colors.text2),
            )
        }
        MmMoney(
            value = trend.totalAmount,
        )
    }
}

@Composable
private fun TrendsBarSection(
    trend: CategoryTrend,
    highlightIndex: Int,
    xLabels: List<String>,
    currencyCode: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    if (trend.avgPerMonth > 0 || trend.avgPerDay > 0) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (trend.avgPerMonth > 0) {
                Text(
                    text = "avg/mo: ${formatAmount(trend.avgPerMonth)} $currencyCode",
                    style = type.caption.copy(color = colors.text2),
                )
            }
            if (trend.avgPerDay > 0) {
                Text(
                    text = "avg/day: ${formatAmount(trend.avgPerDay)} $currencyCode",
                    style = type.caption.copy(color = colors.text2),
                )
            }
        }
        Spacer(Modifier.height(space.padding_1x))
    }
    CategoryBarChart(
        monthlyTotals = trend.series,
        currentMonthIndex = if (highlightIndex >= 0) highlightIndex else -1,
        barColor = Color(trend.categoryColor),
        xLabels = xLabels,
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT),
    )
}

@Composable
private fun TrendsCumulativeSection(
    trend: CategoryTrend,
    highlightIndex: Int,
    xLabels: List<String>,
    currencyCode: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    if (trend.avgPerDay > 0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "${stringResource(Res.string.overview_cat_avg_day)}: " +
                        "${formatAmount(trend.avgPerDay)} " +
                        currencyCode,
                style = type.caption.copy(color = colors.text2),
            )
        }
        Spacer(Modifier.height(space.padding_1x))
    }
    val cumulativeData = trend.series
        .runningFold(0.0) { acc, v -> acc + v }
        .drop(1)
    val todayIndex = if (highlightIndex >= 0) highlightIndex
    else cumulativeData.lastIndex.coerceAtLeast(0)
    val displayCount = (todayIndex + 1).coerceIn(1, cumulativeData.size.coerceAtLeast(1))
    val maxVal = cumulativeData.take(displayCount).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier
                .width(YAXIS_WIDTH)
                .height(CHART_HEIGHT),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatAxisAmount(maxVal),
                style = type.captionXs.copy(color = colors.text3),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = formatAxisAmount(maxVal / 2),
                style = type.captionXs.copy(color = colors.text3),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "0",
                style = type.captionXs.copy(color = colors.text3),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(MM.dimen.padding_0_5x))
        CumulativeChart(
            values = cumulativeData,
            todayIndex = todayIndex,
            modifier = Modifier
                .weight(1f)
                .height(CHART_HEIGHT),
        )
    }
    if (xLabels.isNotEmpty()) {
        Spacer(Modifier.height(space.padding_0_5x))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(YAXIS_WIDTH + MM.dimen.padding_0_5x))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                xLabels.forEach { label ->
                    Text(
                        text = label,
                        style = type.captionXs.copy(color = colors.text3),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun OverviewPeriodBodyPreview() {
    val eur = CurrencyCode("EUR")
    MoneyMTheme {
        OverviewPeriodBody(
            state = OverviewPageUiState(
                isLoading = false,
                isEmpty = false,
                period = OverviewPeriod.Month(YearMonth(2026, 6)),
                income = 2500.0,
                expenses = 1850.0,
                categoryBreakdown = listOf(
                    CategorySpend(
                        categoryName = "Groceries",
                        categoryColor = 0xFF4CAF50,
                        categoryIcon = Icon.Basket,
                        amount = 620.0,
                        percent = 34,
                        avgPerDay = 20.6,
                        avgPerMonth = 620.0,
                    ),
                    CategorySpend(
                        categoryName = "Restaurants",
                        categoryColor = 0xFFFF7043,
                        categoryIcon = Icon.Restaurant,
                        amount = 410.0,
                        percent = 22,
                        avgPerDay = 13.6,
                        avgPerMonth = 410.0,
                    ),
                    CategorySpend(
                        categoryName = "Transport",
                        categoryColor = 0xFF42A5F5,
                        categoryIcon = Icon.Car,
                        amount = 280.0,
                        percent = 15,
                        avgPerDay = 9.3,
                        avgPerMonth = 280.0,
                    ),
                ),
                categoryIncomeBreakdown = listOf(
                    CategorySpend(
                        categoryName = "Salary",
                        categoryColor = 0xFF66BB6A,
                        categoryIcon = Icon.Banknote,
                        amount = 2500.0,
                        percent = 100,
                    ),
                ),
                cumulativeTotals = listOf(
                    45.0, 92.0, 138.0, 210.0, 265.0, 310.0, 388.0, 420.0, 475.0, 540.0,
                    612.0, 658.0, 700.0, 745.0, 810.0, 865.0, 920.0, 980.0, 1040.0, 1095.0,
                    1150.0, 1210.0, 1270.0, 1330.0,
                ),
                todayIndex = 23,
                categoryDailyTrend = listOf(
                    CategoryTrend(
                        categoryName = "Groceries",
                        categoryColor = 0xFF4CAF50,
                        categoryIcon = Icon.Basket,
                        totalAmount = 620.0,
                        txCount = 12,
                        series = listOf(
                            45.0, 92.0, 138.0, 210.0, 265.0, 310.0, 388.0, 420.0, 475.0, 540.0,
                            612.0, 658.0, 700.0, 745.0, 810.0, 865.0, 920.0, 980.0, 1040.0, 1095.0,
                            1150.0, 1210.0, 1270.0, 1330.0,
                        ),
                        avgPerDay = 25.8,
                    ),
                ),
                avgDailyExpense = 77.1,
                budgetProgress = listOf(
                    BudgetProgress(
                        budgetId = 1L,
                        name = "Groceries",
                        amount = Money(70000L, eur),
                        spent = Money(62000L, eur),
                        remaining = Money(8000L, eur),
                        fraction = 0.89f,
                        isOverrun = false,
                        categoryName = "Groceries",
                        categoryColor = 0xFF4CAF50,
                    ),
                ),
            ),
            spendingFilter = SpendingFilter.All,
            currencyCode = "EUR",
        )
    }
}