package com.dv.moneym.feature.banksync.suggestions

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.SpendingFilter
import kotlin.math.abs
import kotlinx.serialization.Serializable

enum class SuggestionSourceType { BANK, WALLET }

enum class SuggestionsTab { PENDING, REJECTED }

@Serializable
data class SuggestionFilter(
    val type: SpendingFilter = SpendingFilter.All,
    val minText: String = "",
    val maxText: String = "",
    val note: String = "",
) {
    val isActive: Boolean
        get() = type != SpendingFilter.All || minText.isNotBlank() ||
            maxText.isNotBlank() || note.isNotBlank()

    fun matches(row: SuggestionRow): Boolean {
        val typeOk = when (type) {
            SpendingFilter.All -> true
            SpendingFilter.Expenses -> row.isExpense
            SpendingFilter.Income -> !row.isExpense
        }
        if (!typeOk) return false

        val amount = abs(row.amountMinor)
        minText.toDoubleOrNull()?.let { if (amount < it * 100) return false }
        maxText.toDoubleOrNull()?.let { if (amount > it * 100) return false }

        if (note.isNotBlank()) {
            val haystack = listOfNotNull(row.description, row.counterparty).joinToString(" ")
            if (!haystack.contains(note, ignoreCase = true)) return false
        }
        return true
    }
}

@Serializable
data class DuplicateInfo(
    val transactionId: Long,
    val note: String?,
    val categoryName: String?,
    val dateIso: String,
    val amountMinor: Long,
    val currency: String,
)

@Serializable
data class SuggestionRow(
    val id: Long,
    val description: String?,
    val counterparty: String?,
    val dateIso: String,
    val amountMinor: Long,
    val currency: String,
    val isExpense: Boolean,
    val sourceLabel: String,
    val targetAccountId: Long?,
    val targetAccountName: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val duplicate: DuplicateInfo? = null,
)

@Serializable
data class SuggestionsUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val tab: SuggestionsTab = SuggestionsTab.PENDING,
    val pending: List<SuggestionRow> = emptyList(),
    val rejected: List<SuggestionRow> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categoryPickerForId: Long? = null,
    val accountPickerForId: Long? = null,
    val showAcceptConfirm: Boolean = false,
    val showRejectConfirm: Boolean = false,
    val acceptConfirmId: Long? = null,
    val filter: SuggestionFilter = SuggestionFilter(),
    val showFilterSheet: Boolean = false,
    val showBatchCategoryPicker: Boolean = false,
    val showBatchAccountPicker: Boolean = false,
) {
    val filteredPending: List<SuggestionRow> get() = pending.filter { filter.matches(it) }
    val rows: List<SuggestionRow> get() = if (tab == SuggestionsTab.PENDING) filteredPending else rejected
    val allSelected: Boolean
        get() = filteredPending.isNotEmpty() && filteredPending.all { it.id in selectedIds }
}

sealed interface SuggestionsIntent {
    data class SetTab(val tab: SuggestionsTab) : SuggestionsIntent
    data class ToggleSelect(val id: Long) : SuggestionsIntent
    data object ToggleSelectAll : SuggestionsIntent
    data object ClearSelection : SuggestionsIntent
    data class RequestAccept(val id: Long) : SuggestionsIntent
    data object ConfirmAccept : SuggestionsIntent
    data class Reject(val id: Long) : SuggestionsIntent
    data object RequestAcceptSelected : SuggestionsIntent
    data object ConfirmAcceptSelected : SuggestionsIntent
    data object RequestRejectSelected : SuggestionsIntent
    data object ConfirmRejectSelected : SuggestionsIntent
    data object DismissConfirm : SuggestionsIntent
    data class UndoReject(val id: Long) : SuggestionsIntent
    data class RestoreToPending(val id: Long) : SuggestionsIntent
    data class ShowCategoryPicker(val id: Long?) : SuggestionsIntent
    data class SetCategory(val id: Long, val categoryId: Long) : SuggestionsIntent
    data class ShowAccountPicker(val id: Long?) : SuggestionsIntent
    data class SetAccount(val id: Long, val accountId: Long) : SuggestionsIntent
    data class ShowFilterSheet(val show: Boolean) : SuggestionsIntent
    data class SetFilterType(val type: SpendingFilter) : SuggestionsIntent
    data class SetFilterMin(val text: String) : SuggestionsIntent
    data class SetFilterMax(val text: String) : SuggestionsIntent
    data class SetFilterNote(val text: String) : SuggestionsIntent
    data object ClearFilter : SuggestionsIntent
    data class ShowBatchCategoryPicker(val show: Boolean) : SuggestionsIntent
    data class SetCategoryForSelected(val categoryId: Long) : SuggestionsIntent
    data class ShowBatchAccountPicker(val show: Boolean) : SuggestionsIntent
    data class SetAccountForSelected(val accountId: Long) : SuggestionsIntent
}
