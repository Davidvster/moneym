package com.dv.moneym.feature.banksync.suggestions

import kotlinx.serialization.Serializable

enum class SuggestionsTab { PENDING, REJECTED }

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
data class CategoryOption(
    val id: Long,
    val name: String,
    val isExpense: Boolean,
)

@Serializable
data class BankSuggestionsUiState(
    val isLoading: Boolean = true,
    val tab: SuggestionsTab = SuggestionsTab.PENDING,
    val pending: List<SuggestionRow> = emptyList(),
    val rejected: List<SuggestionRow> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val categories: List<CategoryOption> = emptyList(),
    val categoryPickerForId: Long? = null,
) {
    val rows: List<SuggestionRow> get() = if (tab == SuggestionsTab.PENDING) pending else rejected
}

sealed interface BankSuggestionsIntent {
    data class SetTab(val tab: SuggestionsTab) : BankSuggestionsIntent
    data class ToggleSelect(val id: Long) : BankSuggestionsIntent
    data object SelectAll : BankSuggestionsIntent
    data object ClearSelection : BankSuggestionsIntent
    data class Accept(val id: Long) : BankSuggestionsIntent
    data class Reject(val id: Long) : BankSuggestionsIntent
    data object AcceptSelected : BankSuggestionsIntent
    data object RejectSelected : BankSuggestionsIntent
    data class RestoreToPending(val id: Long) : BankSuggestionsIntent
    data class ShowCategoryPicker(val id: Long?) : BankSuggestionsIntent
    data class SetCategory(val id: Long, val categoryId: Long) : BankSuggestionsIntent
}
