package com.dv.moneym.feature.transactions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.transactions.presentation.DayGroup
import com.dv.moneym.feature.transactions.presentation.TransactionListIntent
import com.dv.moneym.feature.transactions.presentation.TransactionListUiState
import com.dv.moneym.feature.transactions.presentation.TransactionListViewModel
import com.dv.moneym.feature.transactions.presentation.TransactionUiModel
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    viewModel: TransactionListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    TransactionListContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAddTransaction = onAddTransaction,
        onEditTransaction = onEditTransaction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListContent(
    state: TransactionListUiState,
    onIntent: (TransactionListIntent) -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
) {
    val monthLabel = monthLabel(state.currentMonth.year, state.currentMonth.monthNumber)
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onIntent(TransactionListIntent.PreviousMonth) }) {
                        Icon(MoneyMIcons.ChevronLeft, contentDescription = stringResource(Res.string.transactions_previous_month))
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onIntent(TransactionListIntent.NextMonth) }) {
                        Icon(MoneyMIcons.ChevronRight, contentDescription = stringResource(Res.string.transactions_next_month))
                    }
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(MoneyMIcons.Add, contentDescription = stringResource(Res.string.transactions_add))
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            FilterRow(
                activeFilter = state.activeFilter,
                summary = state.monthlySummary,
                onFilterChanged = { onIntent(TransactionListIntent.FilterChanged(it)) },
            )
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.transactions_loading))
                }
                state.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.transactions_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> TransactionDayList(
                    dayGroups = state.dayGroups,
                    onTransactionClick = onEditTransaction,
                )
            }
        }
    }
}

@Composable
private fun monthLabel(year: Int, month: Int): String {
    val names = listOf(
        stringResource(Res.string.transactions_month_jan),
        stringResource(Res.string.transactions_month_feb),
        stringResource(Res.string.transactions_month_mar),
        stringResource(Res.string.transactions_month_apr),
        stringResource(Res.string.transactions_month_may),
        stringResource(Res.string.transactions_month_jun),
        stringResource(Res.string.transactions_month_jul),
        stringResource(Res.string.transactions_month_aug),
        stringResource(Res.string.transactions_month_sep),
        stringResource(Res.string.transactions_month_oct),
        stringResource(Res.string.transactions_month_nov),
        stringResource(Res.string.transactions_month_dec),
    )
    return "${names[month - 1]} $year"
}

@Composable
private fun FilterRow(
    activeFilter: TransactionFilter,
    summary: String,
    onFilterChanged: (TransactionFilter) -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Row(
        modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.xs),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = activeFilter == TransactionFilter.None,
            onClick = { onFilterChanged(TransactionFilter.None) },
            label = { Text(stringResource(Res.string.transactions_filter_all)) },
            modifier = Modifier.padding(end = sp.sm),
        )
        FilterChip(
            selected = activeFilter is TransactionFilter.ByType && activeFilter.type == TransactionType.EXPENSE,
            onClick = { onFilterChanged(TransactionFilter.ByType(TransactionType.EXPENSE)) },
            label = { Text(stringResource(Res.string.transactions_filter_expenses)) },
            modifier = Modifier.padding(end = sp.sm),
        )
        FilterChip(
            selected = activeFilter is TransactionFilter.ByType && activeFilter.type == TransactionType.INCOME,
            onClick = { onFilterChanged(TransactionFilter.ByType(TransactionType.INCOME)) },
            label = { Text(stringResource(Res.string.transactions_filter_income)) },
        )
        if (summary.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            Text(
                text = summary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = if (summary.startsWith("+")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun TransactionDayList(
    dayGroups: List<DayGroup>,
    onTransactionClick: (TransactionId) -> Unit,
) {
    val sp = MoneyMTheme.spacing
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        dayGroups.forEach { group ->
            stickyHeader(key = group.date.toString()) {
                DayHeaderItem(label = group.label)
            }
            items(group.transactions, key = { it.id.value }) { txn ->
                TransactionRowItem(txn = txn, onClick = { onTransactionClick(txn.id) })
            }
        }
    }
}

@Composable
private fun DayHeaderItem(label: String) {
    val sp = MoneyMTheme.spacing
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = sp.lg, vertical = sp.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransactionRowItem(
    txn: TransactionUiModel,
    onClick: () -> Unit,
) {
    val sp = MoneyMTheme.spacing
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = sp.md)
                    .background(
                        color = categoryColor(txn.categoryColorHex),
                        shape = MaterialTheme.shapes.small,
                    )
                    .height(40.dp)
                    .padding(horizontal = sp.xs),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(txn.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (!txn.note.isNullOrBlank()) {
                    Text(txn.note, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = if (txn.isExpense) "-${txn.amountFormatted}" else "+${txn.amountFormatted}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (txn.isExpense) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
