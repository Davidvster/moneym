package com.dv.moneym.feature.transactions.list

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// anchor = earliest transaction month (falls back to today when no transactions)
// page 0 = anchor month, increasing pages = later months
internal fun yearMonthToPage(yearMonth: YearMonth, anchor: YearMonth): Int =
    (yearMonth.year - anchor.year) * 12 + (yearMonth.monthNumber - anchor.monthNumber)

internal fun pageToYearMonth(page: Int, anchor: YearMonth): YearMonth {
    val total = anchor.year * 12 + (anchor.monthNumber - 1) + page
    return YearMonth(total / 12, total % 12 + 1)
}

@Serializable
internal data class TransactionListUiState(
    val currentMonth: YearMonth? = null,
    val activeFilter: TransactionFilter = TransactionFilter.None,
    val availableCategories: List<Category> = emptyList(),
    val netAmount: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpenses: Long = 0L,
    val netCurrency: String = "EUR",
    val searchQuery: String = "",
    val selectedAccount: Account? = null,
    val availableAccounts: List<Account> = emptyList(),
    // Page math — computed in VM, consumed by Screen
    val currentPage: Int = 0,
    val pageCount: Int = 121,
    val canGoBack: Boolean = false,
    // null = no transactions yet → no min-date restriction in MonthPicker
    val earliestMonth: YearMonth? = null,
    val today: LocalDate? = null,
    // Category filter — managed in VM via TransactionListEphemeralState
    val selectedCategoryIds: Set<CategoryId> = emptySet(),
    val isSearchActive: Boolean = false,
    val showMonthPicker: Boolean = false,
    val showWalletSwitcher: Boolean = false,
    val showCategoryFilter: Boolean = false,
    val isSyncInProgress: Boolean = false,
    val pendingDeletionCount: Int = 0,
)

@Serializable
internal data class DayGroup(
    val date: LocalDate,
    val label: String,
    val transactions: List<TransactionUiModel>,
)

@Serializable
internal data class TransactionUiModel(
    val id: TransactionId,
    val type: TransactionType,
    val amountFormatted: String,
    val amountMinorUnits: Long = 0L,
    val currency: String = "EUR",
    val isExpense: Boolean,
    val categoryName: String,
    val categoryColorHex: String,
    val categoryIcon: Icon,
    val note: String?,
    val occurredOn: LocalDate,
    val paymentModeName: String? = null,
    val isPending: Boolean = false,
    val recurringId: RecurringTransactionId? = null,
    val rowKey: String = id.value.toString(),
)
