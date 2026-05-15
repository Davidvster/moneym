package com.dv.moneym.feature.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.format
import com.dv.moneym.feature.overview.presentation.CategoryBreakdownItem
import com.dv.moneym.feature.overview.presentation.MoneyTotal
import com.dv.moneym.feature.overview.presentation.OverviewIntent
import com.dv.moneym.feature.overview.presentation.OverviewUiState
import com.dv.moneym.feature.overview.presentation.OverviewViewModel
import com.dv.moneym.feature.overview.ui.charts.DonutChart
import com.dv.moneym.feature.overview.ui.charts.DonutSlice
import com.dv.moneym.feature.overview.ui.charts.TrendBarChart
import org.koin.compose.viewmodel.koinViewModel

private val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun",
    "Jul","Aug","Sep","Oct","Nov","Dec")

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    OverviewContent(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewContent(
    state: OverviewUiState,
    onIntent: (OverviewIntent) -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onIntent(OverviewIntent.PreviousMonth) }) {
                        Icon(MoneyMIcons.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = "${monthNames[state.currentMonth.monthNumber - 1]} ${state.currentMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onIntent(OverviewIntent.NextMonth) }) {
                        Icon(MoneyMIcons.ChevronRight, contentDescription = "Next month")
                    }
                }
            })
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
            return@Scaffold
        }
        if (state.isEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No transactions this month",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = sp.lg),
            verticalArrangement = Arrangement.spacedBy(sp.lg),
        ) {
            // Summary cards
            item {
                SummaryRow(income = state.totalIncome, expense = state.totalExpense)
            }

            // Donut chart
            if (state.categoryBreakdown.isNotEmpty()) {
                item {
                    SectionTitle("Spending by category")
                    Spacer(Modifier.height(sp.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(sp.lg),
                    ) {
                        val totalExpenseFormatted = state.totalExpense
                            .firstOrNull()
                            ?.let { Money(it.minorUnits, it.currency).format() } ?: "–"
                        DonutChart(
                            slices = state.categoryBreakdown.map { DonutSlice(it.colorHex, it.expenseMinorUnits.toFloat(), it.name) },
                            centerLabel = totalExpenseFormatted,
                            modifier = Modifier.size(120.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                            state.categoryBreakdown.take(5).forEach { item ->
                                CategoryLegendRow(item)
                            }
                            if (state.categoryBreakdown.size > 5) {
                                Text(
                                    "+ ${state.categoryBreakdown.size - 5} more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // 6-month trend bar chart
            if (state.trendMonths.any { it.incomeMinorUnits > 0 || it.expenseMinorUnits > 0 }) {
                item {
                    SectionTitle("6-month trend")
                    Spacer(Modifier.height(sp.sm))
                    TrendBarChart(
                        months = state.trendMonths,
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                    )
                    Spacer(Modifier.height(sp.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        state.trendMonths.forEach { m ->
                            Text(
                                m.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (m.isCurrentMonth) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (m.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // Category breakdown list
            if (state.categoryBreakdown.isNotEmpty()) {
                item { SectionTitle("All categories") }
                items(state.categoryBreakdown, key = { it.name }) { item ->
                    CategoryBreakdownRow(item)
                }
            }

            item { Spacer(Modifier.height(sp.xxl)) }
        }
    }
}

@Composable
private fun SummaryRow(income: List<MoneyTotal>, expense: List<MoneyTotal>) {
    val sp = MoneyMTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        SummaryCard(
            label = "Income",
            totals = income,
            modifier = Modifier.weight(1f),
            isPositive = true,
        )
        SummaryCard(
            label = "Expenses",
            totals = expense,
            modifier = Modifier.weight(1f),
            isPositive = false,
        )
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
private fun CategoryBreakdownRow(item: CategoryBreakdownItem) {
    val sp = MoneyMTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = sp.xs),
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

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
