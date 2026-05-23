package com.dv.moneym.feature.transactions.list

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

internal const val PAGE_OFFSET = 1200

internal fun yearMonthToPage(yearMonth: YearMonth, today: LocalDate): Int {
    val deltaMonths = (yearMonth.year - today.year) * 12 + (yearMonth.monthNumber - today.monthNumber)
    return PAGE_OFFSET + deltaMonths
}

internal fun pageToYearMonth(page: Int, today: LocalDate): YearMonth {
    val deltaMonths = page - PAGE_OFFSET
    val totalMonth0 = today.year * 12 + (today.monthNumber - 1) + deltaMonths
    val year = totalMonth0 / 12
    val month = totalMonth0 % 12 + 1
    return YearMonth(year, month)
}

@Serializable
internal data class TransactionListUiState(
    val currentMonth: YearMonth = YearMonth(2026, 1),
    val activeFilter: TransactionFilter = TransactionFilter.None,
    val availableCategories: List<Category> = emptyList(),
    val netAmount: Long = 0L,
    val netCurrency: String = "EUR",
    val searchQuery: String = "",
    val selectedAccount: Account? = null,
    val availableAccounts: List<Account> = emptyList(),
    // Page math — computed in VM, consumed by Screen
    val currentPage: Int = PAGE_OFFSET,
    val firstAvailablePage: Int = 0,
    val pageCount: Int = PAGE_OFFSET + 1,
    val today: LocalDate = LocalDate(2026, 1, 1),
    // Category filter — managed in VM via TransactionListEphemeralState
    val selectedCategoryIds: Set<CategoryId> = emptySet(),
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
)
