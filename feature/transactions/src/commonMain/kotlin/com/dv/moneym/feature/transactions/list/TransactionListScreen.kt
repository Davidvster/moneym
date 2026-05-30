package com.dv.moneym.feature.transactions.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmEmptyState
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmMonthPickerDialog
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.core.ui.TxRow
import com.dv.moneym.core.ui.WalletChip
import com.dv.moneym.core.ui.WalletSwitcherDialog
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.ui.monthLabel
import com.dv.moneym.feature.transactions.list.components.CategoryFilterSheet
import com.dv.moneym.feature.transactions.list.components.DayGroupHeader
import com.dv.moneym.feature.transactions.list.page.TransactionPageScreen
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.Serializable
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.transactions_add
import moneym.feature.transactions.generated.resources.transactions_cancel
import moneym.feature.transactions.generated.resources.transactions_close_search_cd
import moneym.feature.transactions.generated.resources.transactions_dialog_select_month
import moneym.feature.transactions.generated.resources.transactions_empty
import moneym.feature.transactions.generated.resources.transactions_filter_all
import moneym.feature.transactions.generated.resources.transactions_filter_expenses
import moneym.feature.transactions.generated.resources.transactions_filter_income
import moneym.feature.transactions.generated.resources.transactions_loading
import moneym.feature.transactions.generated.resources.transactions_net_label
import moneym.feature.transactions.generated.resources.transactions_next_month
import moneym.feature.transactions.generated.resources.transactions_next_year_cd
import moneym.feature.transactions.generated.resources.transactions_now
import moneym.feature.transactions.generated.resources.transactions_ok
import moneym.feature.transactions.generated.resources.transactions_prev_year_cd
import moneym.feature.transactions.generated.resources.transactions_previous_month
import moneym.feature.transactions.generated.resources.transactions_search_cd
import moneym.feature.transactions.generated.resources.transactions_search_placeholder
import moneym.feature.transactions.generated.resources.transactions_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object TransactionsKey : NavKey

fun EntryProviderScope<NavKey>.transactionsEntry(
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    onEditRecurring: (RecurringTransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit = {},
    metadata: Map<String, Any> = emptyMap(),
) = entry<TransactionsKey>(metadata = metadata) {
    TransactionListScreen(
        onAddTransaction = onAddTransaction,
        onEditTransaction = onEditTransaction,
        onEditRecurring = onEditRecurring,
        onTabSelected = onTabSelected,
    )
}

@Composable
private fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    onEditRecurring: (RecurringTransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit = {},
    viewModel: TransactionListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionListContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAddTransaction = onAddTransaction,
        onEditTransaction = onEditTransaction,
        onEditRecurring = onEditRecurring,
        onTabSelected = onTabSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListContent(
    state: TransactionListUiState,
    onIntent: (TransactionListIntent) -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    onEditRecurring: (RecurringTransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) {
    var initialScrollDone by remember { mutableStateOf(false) }

    val today = state.today ?: return  // wait for first VM emission
    val currentMonth = state.currentMonth ?: return

    val anchor = state.earliestMonth ?: YearMonth(today.year, today.month.number)

    if (state.showMonthPicker) {
        val minYear = state.earliestMonth?.year
        val minMonth = state.earliestMonth?.monthNumber
        MmMonthPickerDialog(
            currentYear = currentMonth.year,
            currentMonth = currentMonth.monthNumber,
            nowYear = today.year,
            nowMonth = today.month.number,
            title = stringResource(Res.string.transactions_dialog_select_month),
            nowLabel = stringResource(Res.string.transactions_now),
            okLabel = stringResource(Res.string.transactions_ok),
            cancelLabel = stringResource(Res.string.transactions_cancel),
            prevYearContentDescription = stringResource(Res.string.transactions_prev_year_cd),
            nextYearContentDescription = stringResource(Res.string.transactions_next_year_cd),
            minYear = minYear,
            minMonth = minMonth,
            onDismiss = { onIntent(TransactionListIntent.ShowMonthPicker(false)) },
            onConfirm = { year, month ->
                onIntent(TransactionListIntent.MonthSelected(YearMonth(year, month)))
                onIntent(TransactionListIntent.ShowMonthPicker(false))
            },
        )
    }

    if (state.showWalletSwitcher && state.availableAccounts.isNotEmpty()) {
        WalletSwitcherDialog(
            accounts = state.availableAccounts,
            selectedAccountId = state.selectedAccount?.id,
            onDismiss = { onIntent(TransactionListIntent.ShowWalletSwitcher(false)) },
            onSelect = { accountId ->
                onIntent(TransactionListIntent.AccountSelected(accountId))
                onIntent(TransactionListIntent.ShowWalletSwitcher(false))
            },
        )
    }

    if (state.showCategoryFilter) {
        CategoryFilterSheet(
            categories = state.availableCategories,
            selectedCategoryIds = state.selectedCategoryIds,
            onToggle = { onIntent(TransactionListIntent.CategoryFilterToggled(it)) },
            onClearAll = { onIntent(TransactionListIntent.CategoryFilterCleared) },
            onDismiss = { onIntent(TransactionListIntent.ShowCategoryFilter(false)) },
        )
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pageCount },
    )

    // Pager fully settled → tell VM which month is visible
    LaunchedEffect(pagerState.settledPage) {
        val newMonth = pageToYearMonth(pagerState.settledPage, anchor)
        if (newMonth != currentMonth) {
            onIntent(TransactionListIntent.MonthSelected(newMonth))
        }
    }

    // VM month changed (arrows / dialog) → scroll pager to match
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            if (initialScrollDone) {
                pagerState.animateScrollToPage(state.currentPage)
            } else {
                pagerState.scrollToPage(state.currentPage)
            }
        }
        initialScrollDone = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MM.colors.bg),
    ) {
        TransactionListHeader(
            state = state,
            isSearchActive = state.isSearchActive,
            onSearchActiveChange = { onIntent(TransactionListIntent.ToggleSearch(it)) },
            onShowMonthPicker = { onIntent(TransactionListIntent.ShowMonthPicker(true)) },
            onShowWalletSwitcher = { onIntent(TransactionListIntent.ShowWalletSwitcher(true)) },
            onShowCategoryFilter = { onIntent(TransactionListIntent.ShowCategoryFilter(true)) },
            onIntent = onIntent,
            onPreviousMonth = { onIntent(TransactionListIntent.PreviousMonth) },
            onNextMonth = { onIntent(TransactionListIntent.NextMonth) },
        )

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f),
        ) { page ->
            val yearMonth = pageToYearMonth(page, anchor)
            TransactionPageScreen(
                yearMonth = yearMonth,
                onEditTransaction = onEditTransaction,
                onEditRecurring = onEditRecurring,
            )
        }

        TransactionListFooter(
            onAddTransaction = onAddTransaction,
            onTabSelected = onTabSelected,
            dividerColor = MM.colors.border,
        )
    }
}

@Composable
private fun TransactionListHeader(
    state: TransactionListUiState,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onShowMonthPicker: () -> Unit,
    onShowWalletSwitcher: () -> Unit,
    onShowCategoryFilter: () -> Unit,
    onIntent: (TransactionListIntent) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val selectedFilterIndex = when (val f = state.activeFilter) {
        is TransactionFilter.None -> 0
        is TransactionFilter.ByType -> if (f.type == TransactionType.EXPENSE) 1 else 2
        else -> 0
    }

    Column(
        modifier = Modifier.statusBarsPadding().padding(
            start = MM.dimen.padding_2x,
            end = MM.dimen.padding_2x,
            top = MM.dimen.padding_0_5x,
            bottom = MM.dimen.padding_0_5x,
        ),
    ) {
        if (isSearchActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = MM.dimen.padding_0_5x),
            ) {
                MmField(
                    value = state.searchQuery,
                    onValueChange = { onIntent(TransactionListIntent.SearchQueryChanged(it)) },
                    placeholder = stringResource(Res.string.transactions_search_placeholder),
                    prefix = {
                        Icon(
                            imageVector = Icon.Search.imageVector,
                            contentDescription = null,
                            tint = colors.text3,
                            modifier = Modifier.size(MM.dimen.icon_1x),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                MmIconButton(
                    icon = Icon.Close.imageVector,
                    onClick = { onSearchActiveChange(false) },
                    contentDescription = stringResource(Res.string.transactions_close_search_cd),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.transactions_title),
                    style = type.title1,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                if (state.availableAccounts.size > 1) {
                    val selected = state.selectedAccount ?: state.availableAccounts.firstOrNull()
                    WalletChip(
                        name = selected?.name ?: "",
                        colorHex = selected?.colorHex,
                        onClick = onShowWalletSwitcher,
                    )
                }
                if (state.availableCategories.isNotEmpty()) {
                    Box {
                        MmIconButton(
                            icon = Icon.Sliders.imageVector,
                            onClick = onShowCategoryFilter,
                            contentDescription = stringResource(Res.string.transactions_search_cd),
                        )
                        if (state.selectedCategoryIds.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(MM.dimen.padding_1x)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                                    .align(Alignment.TopEnd),
                            )
                        }
                    }
                }
                MmIconButton(
                    icon = Icon.Search.imageVector,
                    onClick = { onSearchActiveChange(true) },
                    contentDescription = stringResource(Res.string.transactions_search_cd),
                )
            }
        }

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
            size = MmSegmentedSize.Md,
            modifier = Modifier.fillMaxWidth().padding(top = MM.dimen.padding_0_5x),
        )

        MonthNavRow(
            state = state,
            onShowMonthPicker = onShowMonthPicker,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
        )
    }
}

@Composable
private fun MonthNavRow(
    state: TransactionListUiState,
    onShowMonthPicker: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val currentMonth = state.currentMonth ?: return
    val label = monthLabel(currentMonth.year, currentMonth.monthNumber)
    val canGoBack = state.canGoBack

    val displayAmount: Double
    val displayLabel: String
    val displayColor: Color
    val displaySign: String
    when (val f = state.activeFilter) {
        is TransactionFilter.ByType -> if (f.type == TransactionType.EXPENSE) {
            displayAmount = state.totalExpenses / 100.0
            displayLabel = stringResource(Res.string.transactions_filter_expenses)
            displayColor = colors.text
            displaySign = ""
        } else {
            displayAmount = state.totalIncome / 100.0
            displayLabel = stringResource(Res.string.transactions_filter_income)
            displayColor = colors.accent
            displaySign = ""
        }

        else -> {
            displayAmount = state.netAmount / 100.0
            displayLabel = stringResource(Res.string.transactions_net_label)
            displayColor = if (state.netAmount >= 0) colors.accent else colors.text
            displaySign = if (state.netAmount >= 0) "+" else "−"
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = MM.dimen.padding_1_5x, bottom = 16.dp),
    ) {
        if (canGoBack) {
            MmIconButton(
                icon = Icon.ChevronLeft.imageVector,
                onClick = onPreviousMonth,
                size = MM.dimen.padding_4x,
                contentDescription = stringResource(Res.string.transactions_previous_month),
            )
        } else {
            Spacer(Modifier.width(MM.dimen.padding_4x))
        }
        Box(
            modifier = Modifier
                .widthIn(min = 96.dp)
                .clip(RoundedCornerShape(MM.dimen.padding_1x))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onShowMonthPicker() }
                .padding(horizontal = MM.dimen.padding_0_5x, vertical = MM.dimen.padding_0_25x),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = type.body,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
        }
        MmIconButton(
            icon = Icon.ChevronRight.imageVector,
            onClick = onNextMonth,
            size = MM.dimen.padding_4x,
            contentDescription = stringResource(Res.string.transactions_next_month),
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = displayLabel,
                style = type.micro.copy(color = colors.text3),
            )
            MmMoney(
                value = displayAmount,
                sign = displaySign,
                style = MM.type.amountLarge,
                color = displayColor,
                currency = state.netCurrency,
            )
        }
    }
}

@Composable
internal fun TransactionListBody(
    dayGroups: List<DayGroup>,
    txDisplayPrefs: TxDisplayPrefs,
    isLoading: Boolean,
    isEmpty: Boolean,
    onEditTransaction: (TransactionId) -> Unit,
    onEditRecurring: (RecurringTransactionId) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    when {
        isLoading -> {
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.transactions_loading),
                    style = type.body,
                    color = colors.text2,
                )
            }
        }

        isEmpty -> {
            MmEmptyState(
                message = stringResource(Res.string.transactions_empty),
                icon = Icon.List.imageVector,
                modifier = modifier,
            )
        }

        else -> {
            LazyColumn(modifier = modifier) {
                dayGroups.forEach { group ->
                    stickyHeader(key = "header_${group.date}") {
                        DayGroupHeader(group = group, showAmount = txDisplayPrefs.showDailySums)
                    }
                    items(items = group.transactions, key = { it.rowKey }) { tx ->
                        val resolvedColor = categoryColor(tx.categoryColorHex)
                        val resolvedIcon = tx.categoryIcon.imageVector
                        TxRow(
                            categoryName = tx.categoryName,
                            categoryColor = resolvedColor,
                            categoryIcon = resolvedIcon,
                            note = tx.note,
                            isExpense = tx.isExpense,
                            amountValue = tx.amountMinorUnits / 100.0,
                            currency = tx.currency,
                            prefs = txDisplayPrefs,
                            paymentModeName = tx.paymentModeName,
                            onClick = if (tx.isPending) {
                                tx.recurringId?.let { rid -> ({ onEditRecurring(rid) }) }
                            } else ({ onEditTransaction(tx.id) }),
                            divider = tx != group.transactions.last(),
                            isPending = tx.isPending,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionListFooter(
    onAddTransaction: () -> Unit,
    onTabSelected: (TabRoute) -> Unit,
    dividerColor: Color,
) {
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
                .background(MM.colors.bg)
                .padding(start = 16.dp, end = 16.dp, top = MM.dimen.padding_1_5x, bottom = 16.dp),
        ) {
            MmButton(
                text = stringResource(Res.string.transactions_add),
                onClick = onAddTransaction,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                leadingIcon = Icon.Plus.imageVector,
                fullWidth = true,
            )
        }
        MmTabBar(
            activeTab = TabRoute.Transactions,
            onTabSelected = onTabSelected,
        )
    }
}

@Preview
@Composable
private fun TransactionListScreenPreview() {
    MoneyMTheme {
        TransactionListContent(
            state = TransactionListUiState(
                currentMonth = YearMonth(2026, 1),
                today = LocalDate(2026, 1, 1),
            ),
            onIntent = {},
            onAddTransaction = {},
            onEditTransaction = {},
            onEditRecurring = {},
            onTabSelected = {},
        )
    }
}
