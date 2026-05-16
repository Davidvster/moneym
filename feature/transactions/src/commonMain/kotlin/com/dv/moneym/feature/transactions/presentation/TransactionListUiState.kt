package com.dv.moneym.feature.transactions.presentation

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import kotlinx.datetime.LocalDate

data class TransactionListUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth(2026, 1),
    val dayGroups: List<DayGroup> = emptyList(),
    val activeFilter: TransactionFilter = TransactionFilter.None,
    val availableCategories: List<Category> = emptyList(),
    val isEmpty: Boolean = false,
    val monthlySummary: String = "",
    val netAmount: Long = 0L,
    val netCurrency: String = "EUR",
    val txDisplayPrefs: TxDisplayPrefs = TxDisplayPrefs(),
    val searchQuery: String = "",
)

data class DayGroup(
    val date: LocalDate,
    val label: String,
    val transactions: List<TransactionUiModel>,
)

data class TransactionUiModel(
    val id: TransactionId,
    val type: TransactionType,
    val amountFormatted: String,
    val amountMinorUnits: Long = 0L,
    val currency: String = "EUR",
    val isExpense: Boolean,
    val categoryName: String,
    val categoryColorHex: String,
    val categoryIconKey: String,
    val note: String?,
    val occurredOn: LocalDate,
)
