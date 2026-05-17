package com.dv.moneym.feature.transactions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
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

    var isSearchActive by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        TransactionListHeader(
            state = state,
            isSearchActive = isSearchActive,
            onSearchActiveChange = { isSearchActive = it },
            onShowMonthPicker = { showMonthPicker = true },
            onIntent = onIntent,
        )
        TransactionListBody(
            state = state,
            onEditTransaction = onEditTransaction,
            modifier = Modifier.weight(1f),
        )
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
    onSearchActiveChange: (Boolean) -> Unit,
    onShowMonthPicker: () -> Unit,
    onIntent: (TransactionListIntent) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val selectedFilterIndex = when (val f = state.activeFilter) {
        is TransactionFilter.None -> 0
        is TransactionFilter.ByType -> if (f.type == TransactionType.EXPENSE) 1 else 2
        else -> 0
    }

    Column(
        modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
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
                            modifier = Modifier.size(18.dp),
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
            onIntent = onIntent,
        )

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
}

@Composable
private fun MonthNavRow(
    state: TransactionListUiState,
    onShowMonthPicker: () -> Unit,
    onIntent: (TransactionListIntent) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val monthLabel = monthLabel(state.currentMonth.year, state.currentMonth.monthNumber)
    val netDouble = state.netAmount / 100.0

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
        Box(
            modifier = Modifier
                .widthIn(min = 96.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onShowMonthPicker() }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = monthLabel,
                style = type.body,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
        }
        MmIconButton(
            icon = MmIcons.chevronRight,
            onClick = { onIntent(TransactionListIntent.NextMonth) },
            size = 32.dp,
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
    state: TransactionListUiState,
    onEditTransaction: (TransactionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    when {
        state.isLoading -> {
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
        state.isEmpty -> {
            Box(
                modifier = modifier.fillMaxWidth(),
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
            LazyColumn(modifier = modifier) {
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
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
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

// ─── Month Picker Dialog ──────────────────────────────────────────────────────

@Composable
private fun MonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    val todayDate = remember {
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val nowYear = todayDate.year
    val nowMonth = todayDate.monthNumber

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.transactions_dialog_select_month),
                style = type.title3,
                color = colors.text,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Year selection row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MmIconButton(
                        icon = MmIcons.chevronLeft,
                        onClick = { selectedYear-- },
                        size = 32.dp,
                        contentDescription = stringResource(Res.string.transactions_prev_year_cd),
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
                        onClick = { selectedYear++ },
                        size = 32.dp,
                        contentDescription = stringResource(Res.string.transactions_next_year_cd),
                    )
                }

                MonthGrid(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    nowMonth = nowMonth,
                    nowYear = nowYear,
                    onMonthSelected = { selectedMonth = it },
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onConfirm(nowYear, nowMonth) }) {
                    Text(stringResource(Res.string.transactions_now), color = colors.text2)
                }
                TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                    Text(stringResource(Res.string.transactions_ok), color = colors.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.transactions_cancel), color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}

@Composable
private fun MonthGrid(
    selectedMonth: Int,
    selectedYear: Int,
    nowMonth: Int,
    nowYear: Int,
    onMonthSelected: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val monthNames = localizedMonthAbbreviations()

    // Month grid — 4 rows × 3 columns
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .then(
                                if (isNow && !isSelected) {
                                    Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                } else Modifier
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onMonthSelected(m) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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

// ─── Localized month abbreviations ────────────────────────────────────────────

@Composable
private fun localizedMonthAbbreviations(): List<String> = listOf(
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

// ─── Month label helper ────────────────────────────────────────────────────────

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

@Preview
@Composable
private fun TransactionListScreenPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        TransactionListContent(
            state = TransactionListUiState(isLoading = false, isEmpty = true),
            onIntent = {},
            onAddTransaction = {},
            onEditTransaction = {},
            onTabSelected = {},
        )
    }
}
