package com.dv.moneym.feature.banksync.suggestions

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.SpendingFilter
import kotlin.math.abs
import kotlinx.serialization.Serializable

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
    val bankName: String,
    val targetAccountId: Long?,
    val targetAccountName: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val duplicate: DuplicateInfo? = null,
)

@Serializable
data class BankSuggestionsUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val tab: SuggestionsTab = SuggestionsTab.PENDING,
    val pending: List<SuggestionRow> = emptyList(),
    val rejected: List<SuggestionRow> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val categories: List<Category> = emptyList(),
    val categoryPickerForId: Long? = null,
    val showAcceptConfirm: Boolean = false,
    val showRejectConfirm: Boolean = false,
    val filter: SuggestionFilter = SuggestionFilter(),
    val showFilterSheet: Boolean = false,
    val showBatchCategoryPicker: Boolean = false,
) {
    val filteredPending: List<SuggestionRow> get() = pending.filter { filter.matches(it) }
    val rows: List<SuggestionRow> get() = if (tab == SuggestionsTab.PENDING) filteredPending else rejected
    val allSelected: Boolean
        get() = filteredPending.isNotEmpty() && filteredPending.all { it.id in selectedIds }
}

sealed interface BankSuggestionsIntent {
    data class SetTab(val tab: SuggestionsTab) : BankSuggestionsIntent
    data class ToggleSelect(val id: Long) : BankSuggestionsIntent
    data object ToggleSelectAll : BankSuggestionsIntent
    data object ClearSelection : BankSuggestionsIntent
    data class Accept(val id: Long) : BankSuggestionsIntent
    data class Reject(val id: Long) : BankSuggestionsIntent
    data object RequestAcceptSelected : BankSuggestionsIntent
    data object ConfirmAcceptSelected : BankSuggestionsIntent
    data object RequestRejectSelected : BankSuggestionsIntent
    data object ConfirmRejectSelected : BankSuggestionsIntent
    data object DismissConfirm : BankSuggestionsIntent
    data class UndoReject(val id: Long) : BankSuggestionsIntent
    data class RestoreToPending(val id: Long) : BankSuggestionsIntent
    data class ShowCategoryPicker(val id: Long?) : BankSuggestionsIntent
    data class SetCategory(val id: Long, val categoryId: Long) : BankSuggestionsIntent
    data class ShowFilterSheet(val show: Boolean) : BankSuggestionsIntent
    data class SetFilterType(val type: SpendingFilter) : BankSuggestionsIntent
    data class SetFilterMin(val text: String) : BankSuggestionsIntent
    data class SetFilterMax(val text: String) : BankSuggestionsIntent
    data class SetFilterNote(val text: String) : BankSuggestionsIntent
    data object ClearFilter : BankSuggestionsIntent
    data class ShowBatchCategoryPicker(val show: Boolean) : BankSuggestionsIntent
    data class SetCategoryForSelected(val categoryId: Long) : BankSuggestionsIntent
}
