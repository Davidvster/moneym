package com.dv.moneym.feature.transactions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.core.ui.TxRow
import com.dv.moneym.feature.transactions.presentation.DayGroup
import com.dv.moneym.feature.transactions.presentation.TransactionListIntent
import com.dv.moneym.feature.transactions.presentation.TransactionListUiState
import com.dv.moneym.feature.transactions.presentation.TransactionListViewModel
import com.dv.moneym.feature.transactions.presentation.TransactionUiModel
import kotlinx.serialization.Serializable
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object TransactionsKey : NavKey

fun EntryProviderScope<NavKey>.transactionsEntry(
    onAddTransaction: () -> Unit,
    onEditTransaction: (com.dv.moneym.core.model.TransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit = {},
) = entry<TransactionsKey> {
    TransactionListScreen(
        onAddTransaction = onAddTransaction,
        onEditTransaction = onEditTransaction,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit = {},
    viewModel: TransactionListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    TransactionListContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAddTransaction = onAddTransaction,
        onEditTransaction = onEditTransaction,
        onTabSelected = onTabSelected,
    )
}

@Composable
private fun TransactionListContent(
    state: TransactionListUiState,
    onIntent: (TransactionListIntent) -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val monthLabel = monthLabel(state.currentMonth.year, state.currentMonth.monthNumber)

    // Convert filter to segmented index: 0=All, 1=Expenses, 2=Income
    val selectedFilterIndex = when (val f = state.activeFilter) {
        is TransactionFilter.None -> 0
        is TransactionFilter.ByType -> if (f.type == TransactionType.EXPENSE) 1 else 2
        else -> 0
    }

    // Net amount as Double (from minor units, divide by 100)
    val netDouble = state.netAmount / 100.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Header
        Column(
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Transactions",
                    style = type.title1,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                MmIconButton(
                    icon = MmIcons.search,
                    onClick = { /* TODO search */ },
                    contentDescription = "Search",
                )
            }

            // Month navigation row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
            ) {
                MmIconButton(
                    icon = MmIcons.chevronLeft,
                    onClick = { onIntent(TransactionListIntent.PreviousMonth) },
                    size = 32.dp,
                    contentDescription = stringResource(Res.string.transactions_previous_month),
                )
                Text(
                    text = monthLabel,
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.widthIn(min = 96.dp),
                    textAlign = TextAlign.Center,
                )
                MmIconButton(
                    icon = MmIcons.chevronRight,
                    onClick = { onIntent(TransactionListIntent.NextMonth) },
                    size = 32.dp,
                    contentDescription = stringResource(Res.string.transactions_next_month),
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NET",
                        style = type.micro.copy(color = colors.text3),
                    )
                    MmMoney(
                        value = netDouble,
                        sign = if (state.netAmount >= 0) "+" else "−",
                        size = 17.sp,
                        weight = FontWeight.SemiBold,
                        color = if (state.netAmount >= 0) colors.accent else colors.text,
                        currency = state.netCurrency,
                    )
                }
            }

            // Segmented filter
            MmSegmented(
                options = listOf(
                    stringResource(Res.string.transactions_filter_all),
                    stringResource(Res.string.transactions_filter_expenses),
                    stringResource(Res.string.transactions_filter_income),
                ),
                selectedIndex = selectedFilterIndex,
                onOptionSelected = { idx ->
                    val filter = when (idx) {
                        1 -> TransactionFilter.ByType(TransactionType.EXPENSE)
                        2 -> TransactionFilter.ByType(TransactionType.INCOME)
                        else -> TransactionFilter.None
                    }
                    onIntent(TransactionListIntent.FilterChanged(filter))
                },
                fillWidth = true,
            )
        }

        // Body: list or empty/loading state
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.transactions_loading),
                        style = type.body,
                        color = colors.text2,
                    )
                }
            }
            state.isEmpty -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = MmIcons.list,
                            contentDescription = null,
                            tint = colors.text3,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = stringResource(Res.string.transactions_empty),
                            style = type.body.copy(color = colors.text3),
                        )
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    state.dayGroups.forEach { group ->
                        stickyHeader(key = "header_${group.date}") {
                            SectionLabel(
                                text = group.label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.bg)
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                            )
                        }
                        items(
                            items = group.transactions,
                            key = { it.id.value },
                        ) { tx ->
                            val resolvedColor = categoryColor(tx.categoryColorHex)
                            val resolvedIcon = iconForKey(tx.categoryIconKey)
                            TxRow(
                                categoryName = tx.categoryName,
                                categoryColor = resolvedColor,
                                categoryIcon = resolvedIcon,
                                note = tx.note,
                                isExpense = tx.isExpense,
                                amountValue = tx.amountMinorUnits / 100.0,
                                currency = tx.currency,
                                prefs = state.txDisplayPrefs,
                                onClick = { onEditTransaction(tx.id) },
                                divider = tx != group.transactions.last(),
                            )
                        }
                    }
                }
            }
        }

        // Pinned bottom area
        val dividerColor = colors.border
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, strokeWidth / 2),
                            end = Offset(size.width, strokeWidth / 2),
                            strokeWidth = strokeWidth,
                        )
                    }
                    .background(colors.bg)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            ) {
                MmButton(
                    text = "New transaction",
                    onClick = onAddTransaction,
                    variant = MmButtonVariant.Primary,
                    size = MmButtonSize.Lg,
                    leadingIcon = MmIcons.plus,
                    fullWidth = true,
                )
            }
            MmTabBar(
                activeTab = TabRoute.Transactions,
                onTabSelected = onTabSelected,
            )
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
