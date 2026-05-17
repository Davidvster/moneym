package com.dv.moneym.feature.transactions.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.core.ui.TxRow
import com.dv.moneym.core.ui.wallet
import com.dv.moneym.feature.transactions.list.components.CategoryFilterSheet
import com.dv.moneym.feature.transactions.list.components.DayGroupHeader
import com.dv.moneym.feature.transactions.list.components.MonthPickerDialog
import com.dv.moneym.feature.transactions.list.components.WalletSwitcherDialog
import com.dv.moneym.feature.transactions.list.components.monthLabel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.transactions_add
import moneym.feature.transactions.generated.resources.transactions_close_search_cd
import moneym.feature.transactions.generated.resources.transactions_empty
import moneym.feature.transactions.generated.resources.transactions_filter_all
import moneym.feature.transactions.generated.resources.transactions_filter_expenses
import moneym.feature.transactions.generated.resources.transactions_filter_income
import moneym.feature.transactions.generated.resources.transactions_loading
import moneym.feature.transactions.generated.resources.transactions_net_label
import moneym.feature.transactions.generated.resources.transactions_next_month
import moneym.feature.transactions.generated.resources.transactions_previous_month
import moneym.feature.transactions.generated.resources.transactions_search_cd
import moneym.feature.transactions.generated.resources.transactions_search_placeholder
import moneym.feature.transactions.generated.resources.transactions_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val PAGE_COUNT = 2400
private const val PAGE_OFFSET = 1200

private fun YearMonth.toPageIndex(): Int {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val deltaMonths = (year - today.year) * 12 + (monthNumber - today.monthNumber)
    return PAGE_OFFSET + deltaMonths
}

private fun pageIndexToYearMonth(page: Int): YearMonth {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val deltaMonths = page - PAGE_OFFSET
    val totalMonth0 = today.year * 12 + (today.monthNumber - 1) + deltaMonths
    return YearMonth(totalMonth0 / 12, totalMonth0 % 12 + 1)
}

@Serializable
data object TransactionsKey : NavKey

fun EntryProviderScope<NavKey>.transactionsEntry(
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit = {},
) = entry<TransactionsKey> {
    TransactionListScreen(
        onAddTransaction = onAddTransaction,
        onEditTransaction = onEditTransaction,
        onTabSelected = onTabSelected,
    )
}

@Composable
private fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionId) -> Unit,
    onTabSelected: (TabRoute) -> Unit = {},
    viewModel: TransactionListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionListContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAddTransaction = onAddTransaction,
        onEditTransaction = onEditTransaction,
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
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors

    var isSearchActive by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showWalletSwitcher by remember { mutableStateOf(false) }
    var showCategoryFilter by remember { mutableStateOf(false) }
    // Local (non-persisted) category filter: set of selected category IDs
    var selectedCategoryIds by remember { mutableStateOf(emptySet<CategoryId>()) }

    if (showMonthPicker) {
        MonthPickerDialog(
            currentYear = state.currentMonth.year,
            currentMonth = state.currentMonth.monthNumber,
            onDismiss = { showMonthPicker = false },
            onConfirm = { year, month ->
                onIntent(TransactionListIntent.MonthSelected(YearMonth(year, month)))
                showMonthPicker = false
            },
        )
    }

    if (showWalletSwitcher && state.availableAccounts.isNotEmpty()) {
        WalletSwitcherDialog(
            accounts = state.availableAccounts,
            selectedAccountId = state.selectedAccount?.id,
            onDismiss = { showWalletSwitcher = false },
            onSelect = { accountId ->
                onIntent(TransactionListIntent.AccountSelected(accountId))
                showWalletSwitcher = false
            },
        )
    }

    if (showCategoryFilter) {
        CategoryFilterSheet(
            categories = state.availableCategories,
            selectedCategoryIds = selectedCategoryIds,
            onToggle = { categoryId ->
                selectedCategoryIds = if (categoryId in selectedCategoryIds) {
                    selectedCategoryIds - categoryId
                } else {
                    selectedCategoryIds + categoryId
                }
            },
            onClearAll = { selectedCategoryIds = emptySet() },
            onDismiss = { showCategoryFilter = false },
        )
    }

    // Apply local category filter on top of the VM-provided dayGroups
    val filteredDayGroups = remember(state.dayGroups, selectedCategoryIds) {
        if (selectedCategoryIds.isEmpty()) {
            state.dayGroups
        } else {
            state.dayGroups.mapNotNull { group ->
                val matchingTxns = group.transactions.filter { tx ->
                    state.availableCategories.any { cat ->
                        cat.id in selectedCategoryIds && cat.name == tx.categoryName
                    }
                }
                if (matchingTxns.isEmpty()) null
                else group.copy(transactions = matchingTxns)
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentMonth.toPageIndex(),
        pageCount = { PAGE_COUNT },
    )

    LaunchedEffect(pagerState.settledPage) {
        val newMonth = pageIndexToYearMonth(pagerState.settledPage)
        if (newMonth != state.currentMonth) {
            onIntent(TransactionListIntent.MonthSelected(newMonth))
        }
    }

    LaunchedEffect(state.currentMonth) {
        val targetPage = state.currentMonth.toPageIndex()
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        TransactionListHeader(
            state = state,
            isSearchActive = isSearchActive,
            hasActiveFilter = selectedCategoryIds.isNotEmpty(),
            onSearchActiveChange = { isSearchActive = it },
            onShowMonthPicker = { showMonthPicker = true },
            onShowWalletSwitcher = { showWalletSwitcher = true },
            onShowCategoryFilter = { showCategoryFilter = true },
            onIntent = onIntent,
            onPreviousMonth = { onIntent(TransactionListIntent.PreviousMonth) },
            onNextMonth = { onIntent(TransactionListIntent.NextMonth) },
        )

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val pageMonth = pageIndexToYearMonth(page)
            val groups = if (pageMonth == state.currentMonth) filteredDayGroups else emptyList()
            TransactionListBody(
                dayGroups = groups,
                txDisplayPrefs = state.txDisplayPrefs,
                isLoading = state.isLoading && pageMonth == state.currentMonth,
                isEmpty = groups.isEmpty() && !(state.isLoading && pageMonth == state.currentMonth),
                onEditTransaction = onEditTransaction,
            )
        }

        TransactionListFooter(
            onAddTransaction = onAddTransaction,
            onTabSelected = onTabSelected,
            dividerColor = colors.border,
        )
    }
}

@Composable
private fun TransactionListHeader(
    state: TransactionListUiState,
    isSearchActive: Boolean,
    hasActiveFilter: Boolean,
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
            top = 4.dp,
            bottom = 4.dp
        ),
    ) {
        // Title row — or search bar when active
        if (isSearchActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                MmField(
                    value = state.searchQuery,
                    onValueChange = { onIntent(TransactionListIntent.SearchQueryChanged(it)) },
                    placeholder = stringResource(Res.string.transactions_search_placeholder),
                    prefix = {
                        Icon(
                            imageVector = MmIcons.search,
                            contentDescription = null,
                            tint = colors.text3,
                            modifier = Modifier.size(MM.dimen.icon_1x),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                MmIconButton(
                    icon = MmIcons.close,
                    onClick = {
                        onSearchActiveChange(false)
                        onIntent(TransactionListIntent.SearchQueryChanged(""))
                    },
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
                // Wallet chip — show current wallet name if multiple wallets exist
                if (state.availableAccounts.size > 1) {
                    val walletName =
                        state.selectedAccount?.name ?: state.availableAccounts.firstOrNull()?.name
                        ?: ""
                    MmChip(
                        selected = false,
                        onClick = onShowWalletSwitcher,
                        leadingContent = {
                            Icon(
                                imageVector = MmIcons.wallet,
                                contentDescription = null,
                                tint = colors.text2,
                                modifier = Modifier.size(MM.dimen.padding_1_5x),
                            )
                        },
                    ) {
                        Text(
                            text = walletName,
                            style = type.caption,
                            color = colors.text,
                            maxLines = 1,
                        )
                    }
                }
                // Category filter button with active badge
                if (state.availableCategories.isNotEmpty()) {
                    Box {
                        MmIconButton(
                            icon = MmIcons.sliders,
                            onClick = onShowCategoryFilter,
                            contentDescription = stringResource(Res.string.transactions_search_cd),
                        )
                        if (hasActiveFilter) {
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
                    icon = MmIcons.search,
                    onClick = { onSearchActiveChange(true) },
                    contentDescription = stringResource(Res.string.transactions_search_cd),
                )
            }
        }

        MonthNavRow(
            state = state,
            onShowMonthPicker = onShowMonthPicker,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
        )

        // Segmented type filter
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
    val label = monthLabel(state.currentMonth.year, state.currentMonth.monthNumber)
    val netDouble = state.netAmount / 100.0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = MM.dimen.padding_1_5x, bottom = 16.dp),
    ) {
        MmIconButton(
            icon = MmIcons.chevronLeft,
            onClick = onPreviousMonth,
            size = MM.dimen.padding_4x,
            contentDescription = stringResource(Res.string.transactions_previous_month),
        )
        Box(
            modifier = Modifier
                .widthIn(min = 96.dp)
                .clip(RoundedCornerShape(MM.dimen.padding_1x))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onShowMonthPicker() }
                .padding(horizontal = 4.dp, vertical = 2.dp),
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
            icon = MmIcons.chevronRight,
            onClick = onNextMonth,
            size = MM.dimen.padding_4x,
            contentDescription = stringResource(Res.string.transactions_next_month),
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(Res.string.transactions_net_label),
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
}

@Composable
private fun TransactionListBody(
    dayGroups: List<DayGroup>,
    txDisplayPrefs: TxDisplayPrefs,
    isLoading: Boolean,
    isEmpty: Boolean,
    onEditTransaction: (TransactionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.transactions_loading),
                    style = type.body,
                    color = colors.text2,
                )
            }
        }

        isEmpty -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Icon(
                        imageVector = MmIcons.list,
                        contentDescription = null,
                        tint = colors.text3,
                        modifier = Modifier.size(MM.dimen.padding_5x),
                    )
                    Text(
                        text = stringResource(Res.string.transactions_empty),
                        style = type.body.copy(color = colors.text3),
                    )
                }
            }
        }

        else -> {
            LazyColumn(modifier = modifier) {
                dayGroups.forEach { group ->
                    stickyHeader(key = "header_${group.date}") {
                        DayGroupHeader(group = group)
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
                            prefs = txDisplayPrefs,
                            onClick = { onEditTransaction(tx.id) },
                            divider = tx != group.transactions.last(),
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

@Preview
@Composable
private fun TransactionListScreenPreview() {
    MoneyMTheme {
        TransactionListContent(
            state = TransactionListUiState(isLoading = false, isEmpty = true),
            onIntent = {},
            onAddTransaction = {},
            onEditTransaction = {},
            onTabSelected = {},
        )
    }
}
