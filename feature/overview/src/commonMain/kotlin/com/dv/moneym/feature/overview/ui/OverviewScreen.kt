package com.dv.moneym.feature.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.format
import com.dv.moneym.feature.overview.presentation.BarEntry
import com.dv.moneym.feature.overview.presentation.CategoryBreakdownItem
import com.dv.moneym.feature.overview.presentation.MoneyTotal
import com.dv.moneym.feature.overview.presentation.OverviewIntent
import com.dv.moneym.feature.overview.presentation.OverviewPeriod
import com.dv.moneym.feature.overview.presentation.OverviewUiState
import com.dv.moneym.feature.overview.presentation.OverviewViewModel
import com.dv.moneym.feature.overview.ui.charts.DonutChart
import com.dv.moneym.feature.overview.ui.charts.DonutSlice
import com.dv.moneym.feature.overview.ui.charts.PeriodBarChart
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    OverviewContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
private fun monthNames(): List<String> = listOf(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewContent(
    state: OverviewUiState,
    onIntent: (OverviewIntent) -> Unit,
) {
    val sp = MoneyMTheme.spacing
    val months = monthNames()
    val periodLabel = when (val p = state.period) {
        is OverviewPeriod.Month -> "${months[p.yearMonth.monthNumber - 1]} ${p.yearMonth.year}"
        is OverviewPeriod.Year -> p.year.toString()
    }
    val isYearMode = state.period is OverviewPeriod.Year

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onIntent(OverviewIntent.PreviousPeriod) }) {
                        Icon(MoneyMIcons.ChevronLeft, contentDescription = stringResource(Res.string.overview_previous))
                    }
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onIntent(OverviewIntent.NextPeriod) }) {
                        Icon(MoneyMIcons.ChevronRight, contentDescription = stringResource(Res.string.overview_next))
                    }
                    IconButton(onClick = { onIntent(OverviewIntent.TogglePeriod) }) {
                        Icon(
                            if (isYearMode) MoneyMIcons.CalendarMonth else MoneyMIcons.CalendarYear,
                            contentDescription = if (isYearMode) stringResource(Res.string.overview_switch_to_month)
                                                 else stringResource(Res.string.overview_switch_to_year),
                        )
                    }
                }
            })
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.overview_loading))
            }
            return@Scaffold
        }
        if (state.isEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.overview_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = sp.lg, vertical = sp.md),
            verticalArrangement = Arrangement.spacedBy(sp.lg),
        ) {
            item {
                SummaryRow(
                    income = state.totalIncome,
                    expense = state.totalExpense,
                    incomeLabel = stringResource(Res.string.overview_income),
                    expenseLabel = stringResource(Res.string.overview_expenses),
                )
            }

            if (state.categoryBreakdown.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(Res.string.overview_by_category))
                    Spacer(Modifier.height(sp.sm))

                    val selectedSlice = state.selectedSliceIndex
                    val centerLabel = if (selectedSlice != null && selectedSlice < state.categoryBreakdown.size) {
                        val item = state.categoryBreakdown[selectedSlice]
                        "${item.name}\n${item.formattedAmount}"
                    } else {
                        state.totalExpense.firstOrNull()
                            ?.let { Money(it.minorUnits, it.currency).format() } ?: "–"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(sp.lg),
                    ) {
                        DonutChart(
                            slices = state.categoryBreakdown.map {
                                DonutSlice(it.colorHex, it.expenseMinorUnits.toFloat(), it.name)
                            },
                            centerLabel = centerLabel,
                            modifier = Modifier.size(120.dp),
                            selectedIndex = state.selectedSliceIndex,
                            onSliceTapped = { idx -> onIntent(OverviewIntent.SliceTapped(idx)) },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                            state.categoryBreakdown.take(5).forEach { item ->
                                CategoryLegendRow(item)
                            }
                            if (state.categoryBreakdown.size > 5) {
                                Text(
                                    stringResource(Res.string.overview_more, state.categoryBreakdown.size - 5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (state.availableCategories.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(sp.sm),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedCategoryId == null,
                                    onClick = { onIntent(OverviewIntent.CategoryFilterSelected(null)) },
                                    label = { Text(stringResource(Res.string.overview_all)) },
                                )
                            }
                            items(state.availableCategories, key = { it.id.value }) { cat ->
                                FilterChip(
                                    selected = state.selectedCategoryId == cat.id,
                                    onClick = { onIntent(OverviewIntent.CategoryFilterSelected(cat.id)) },
                                    label = { Text(cat.name) },
                                )
                            }
                        }
                    }
                }
            }

            if (state.chartBars.any { it.expenseMinorUnits > 0 || it.incomeMinorUnits > 0 }) {
                item {
                    val chartTitle = if (isYearMode) stringResource(Res.string.overview_monthly_breakdown)
                                     else stringResource(Res.string.overview_daily_breakdown)
                    SectionTitle(chartTitle)
                    Spacer(Modifier.height(sp.sm))
                    PeriodBarChart(
                        bars = state.chartBars,
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                    )
                    Spacer(Modifier.height(sp.xs))
                    if (isYearMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            state.chartBars.forEach { bar ->
                                Text(
                                    bar.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bar.isHighlighted) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (bar.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            if (state.categoryBreakdown.isNotEmpty()) {
                item { SectionTitle(stringResource(Res.string.overview_all_categories)) }
                itemsIndexed(state.categoryBreakdown, key = { _, item -> item.name }) { index, item ->
                    CategoryBreakdownRow(
                        item = item,
                        isSelected = state.selectedSliceIndex == index,
                        onClick = { onIntent(OverviewIntent.SliceTapped(index)) },
                    )
                }
            }

            item { Spacer(Modifier.height(sp.xxl)) }
        }
    }
}

@Composable
private fun SummaryRow(
    income: List<MoneyTotal>,
    expense: List<MoneyTotal>,
    incomeLabel: String,
    expenseLabel: String,
) {
    val sp = MoneyMTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        SummaryCard(label = incomeLabel, totals = income, modifier = Modifier.weight(1f), isPositive = true)
        SummaryCard(label = expenseLabel, totals = expense, modifier = Modifier.weight(1f), isPositive = false)
    }
}

@Composable
private fun SummaryCard(
    label: String,
    totals: List<MoneyTotal>,
    modifier: Modifier = Modifier,
    isPositive: Boolean,
) {
    val sp = MoneyMTheme.spacing
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(sp.md)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (totals.isEmpty()) {
                Text("–", style = MaterialTheme.typography.titleMedium)
            } else {
                totals.forEach { t ->
                    Text(
                        Money(t.minorUnits, t.currency).format(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendRow(item: CategoryBreakdownItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            iconForKey(item.iconKey),
            contentDescription = null,
            tint = categoryColor(item.colorHex),
            modifier = Modifier.size(14.dp).padding(end = MoneyMTheme.spacing.xs),
        )
        Text(
            item.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${(item.percentage * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdownItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val sp = MoneyMTheme.spacing
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(vertical = sp.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            Icon(
                iconForKey(item.iconKey),
                contentDescription = null,
                tint = categoryColor(item.colorHex),
                modifier = Modifier.size(20.dp),
            )
            Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(item.formattedAmount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
