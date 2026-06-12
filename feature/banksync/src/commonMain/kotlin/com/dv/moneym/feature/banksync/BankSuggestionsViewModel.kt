package com.dv.moneym.feature.banksync

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.banksync.BankAccountLink
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.feature.banksync.usecase.AcceptSuggestionUseCase
import com.dv.moneym.feature.banksync.usecase.FindDuplicateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BankSuggestionsViewModel(
    private val bankSyncRepository: BankSyncRepository,
    private val categoryRepository: CategoryRepository,
    private val acceptSuggestion: AcceptSuggestionUseCase,
    private val findDuplicate: FindDuplicateUseCase,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(BankSuggestionsUiState()) }
    internal val state = _state.onStart { init() }.stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val categoryOverrides = mutableMapOf<Long, Long>()

    private fun init() {
        viewModelScope.launch {
            combine(
                bankSyncRepository.observePendingSuggestions(),
                bankSyncRepository.observeRejectedSuggestions(),
                bankSyncRepository.observeAccounts(),
                categoryRepository.observeActive(),
            ) { pending, rejected, accounts, categories ->
                Sources(pending, rejected, accounts, categories.map {
                    CategoryOption(
                        id = it.id.value,
                        name = it.name,
                        isExpense = it.type == TransactionType.EXPENSE,
                    )
                })
            }.collect { sources ->
                val pendingRows = sources.pending.map { it.toRow(sources, withDuplicate = true) }
                val rejectedRows = sources.rejected.map { it.toRow(sources, withDuplicate = false) }
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        pending = pendingRows,
                        rejected = rejectedRows,
                        categories = sources.categories,
                        selectedIds = s.selectedIds.intersect(pendingRows.map { it.id }.toSet()),
                    )
                }
            }
        }
    }

    private suspend fun BankSuggestion.toRow(sources: Sources, withDuplicate: Boolean): SuggestionRow {
        val account = sources.accounts.firstOrNull { it.uid == bankAccountUid }
        val isExpense = direction == EbDirection.DEBIT
        val categoryId = categoryOverrides[id]
            ?: sources.categories.firstOrNull { it.isExpense == isExpense }?.id
        val duplicate = if (withDuplicate) {
            findDuplicate(this)?.let { tx ->
                DuplicateInfo(
                    transactionId = tx.id.value,
                    note = tx.note,
                    categoryName = sources.categories.firstOrNull { it.id == tx.categoryId.value }?.name,
                    dateIso = tx.occurredOn.toString(),
                    amountMinor = tx.amount.minorUnits,
                    currency = tx.amount.currency.value,
                )
            }
        } else {
            null
        }
        return SuggestionRow(
            id = id,
            description = description,
            counterparty = counterparty,
            dateIso = bookingDate.toString(),
            amountMinor = amountMinor,
            currency = currency,
            isExpense = isExpense,
            bankName = account?.bankName.orEmpty(),
            targetAccountId = account?.localAccountId,
            targetAccountName = null,
            categoryId = categoryId,
            categoryName = sources.categories.firstOrNull { it.id == categoryId }?.name,
            duplicate = duplicate,
        )
    }

    fun onIntent(intent: BankSuggestionsIntent) {
        when (intent) {
            is BankSuggestionsIntent.SetTab ->
                _state.update { it.copy(tab = intent.tab, selectedIds = emptySet(), categoryPickerForId = null) }

            is BankSuggestionsIntent.ToggleSelect -> _state.update {
                val selected = it.selectedIds.toMutableSet()
                if (!selected.remove(intent.id)) selected.add(intent.id)
                it.copy(selectedIds = selected)
            }

            BankSuggestionsIntent.SelectAll ->
                _state.update { s -> s.copy(selectedIds = s.pending.map { it.id }.toSet()) }

            BankSuggestionsIntent.ClearSelection -> _state.update { it.copy(selectedIds = emptySet()) }

            is BankSuggestionsIntent.Accept -> acceptAll(listOf(intent.id))

            is BankSuggestionsIntent.Reject -> rejectAll(listOf(intent.id))

            BankSuggestionsIntent.AcceptSelected -> acceptAll(_state.value.selectedIds.toList())

            BankSuggestionsIntent.RejectSelected -> rejectAll(_state.value.selectedIds.toList())

            is BankSuggestionsIntent.RestoreToPending ->
                viewModelScope.launch { bankSyncRepository.restoreToPending(intent.id) }

            is BankSuggestionsIntent.ShowCategoryPicker ->
                _state.update { it.copy(categoryPickerForId = intent.id) }

            is BankSuggestionsIntent.SetCategory -> {
                categoryOverrides[intent.id] = intent.categoryId
                _state.update { s ->
                    s.copy(
                        categoryPickerForId = null,
                        pending = s.pending.map { row ->
                            if (row.id == intent.id) row.copy(
                                categoryId = intent.categoryId,
                                categoryName = s.categories.firstOrNull { it.id == intent.categoryId }?.name,
                            ) else row
                        },
                    )
                }
            }
        }
    }

    private fun acceptAll(ids: List<Long>) {
        val rows = _state.value.pending.filter { it.id in ids }
        viewModelScope.launch {
            for (row in rows) {
                val accountId = row.targetAccountId ?: continue
                val categoryId = row.categoryId ?: continue
                val suggestion = bankSyncRepository.getSuggestion(row.id) ?: continue
                acceptSuggestion(suggestion, accountId = accountId, categoryId = categoryId)
            }
            _state.update { it.copy(selectedIds = emptySet()) }
        }
    }

    private fun rejectAll(ids: List<Long>) {
        viewModelScope.launch {
            val now = clock.now().toEpochMilliseconds()
            ids.forEach { bankSyncRepository.reject(it, now) }
            _state.update { it.copy(selectedIds = emptySet()) }
        }
    }

    private data class Sources(
        val pending: List<BankSuggestion>,
        val rejected: List<BankSuggestion>,
        val accounts: List<BankAccountLink>,
        val categories: List<CategoryOption>,
    )
}
