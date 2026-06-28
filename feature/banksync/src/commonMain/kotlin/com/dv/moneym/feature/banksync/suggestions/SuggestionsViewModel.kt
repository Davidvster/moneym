package com.dv.moneym.feature.banksync.suggestions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.SuggestionSource
import com.dv.moneym.core.model.SyncDirection
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.feature.banksync.usecase.AcceptSuggestionUseCase
import com.dv.moneym.feature.banksync.usecase.FindDuplicateUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SuggestionsViewModel(
    private val sourceType: SuggestionSourceType,
    private val source: SuggestionSource,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val acceptSuggestion: AcceptSuggestionUseCase,
    private val findDuplicate: FindDuplicateUseCase,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(SuggestionsUiState()) }
    internal val state = _state.onStart { init() }.stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    sealed interface SuggestionsSingleUiEvent {
        data class RejectedWithUndo(val id: Long) : SuggestionsSingleUiEvent
    }

    private val _singleEvent = Channel<SuggestionsSingleUiEvent>(Channel.BUFFERED)
    val singleEvents = _singleEvent.receiveAsFlow()

    private val categoryOverrides = mutableMapOf<Long, Long>()
    private val accountOverrides = mutableMapOf<Long, Long>()

    private fun init() {
        _state.update { it.copy(canEditPendingRows = sourceType == SuggestionSourceType.WALLET) }
        viewModelScope.launch {
            combine(
                source.observePending(),
                source.observeRejected(),
                accountRepository.observeAll(),
                categoryRepository.observeActive(),
            ) { pending, rejected, accounts, categories ->
                Sources(pending, rejected, accounts.filterNot { it.archived }, categories)
            }.collect { sources ->
                val pendingRows = sources.pending.map { it.toRow(sources, withDuplicate = true) }
                val rejectedRows = sources.rejected.map { it.toRow(sources, withDuplicate = false) }
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        pending = pendingRows,
                        rejected = rejectedRows,
                        categories = sources.categories,
                        accounts = sources.accounts,
                        selectedIds = s.selectedIds.intersect(pendingRows.map { it.id }.toSet()),
                    )
                }
            }
        }
    }

    private suspend fun SuggestionRecord.toRow(sources: Sources, withDuplicate: Boolean): SuggestionRow {
        val isExpense = direction == SyncDirection.DEBIT
        val categoryId = categoryOverrides[id]
            ?: sources.categories.firstOrNull {
                (it.type == TransactionType.EXPENSE) == isExpense
            }?.id?.value
        val targetAccountId = accountOverrides[id] ?: suggestedAccountId
        val duplicate = if (withDuplicate) {
            findDuplicate(this)?.let { tx ->
                DuplicateInfo(
                    transactionId = tx.id.value,
                    note = tx.note,
                    categoryName = sources.categories.firstOrNull { it.id == tx.categoryId }?.name,
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
            externalId = externalId,
            description = description,
            counterparty = counterparty,
            dateIso = date.toString(),
            amountMinor = amountMinor,
            currency = currency,
            isExpense = isExpense,
            sourceLabel = sourceLabel.orEmpty(),
            targetAccountId = targetAccountId,
            targetAccountName = sources.accounts.firstOrNull { it.id.value == targetAccountId }?.name,
            categoryId = categoryId,
            categoryName = sources.categories.firstOrNull { it.id.value == categoryId }?.name,
            duplicate = duplicate,
        )
    }

    fun onIntent(intent: SuggestionsIntent) {
        when (intent) {
            is SuggestionsIntent.SetTab ->
                _state.update { it.copy(tab = intent.tab, selectedIds = emptySet(), categoryPickerForId = null) }

            is SuggestionsIntent.ToggleSelect -> _state.update {
                val selected = it.selectedIds.toMutableSet()
                if (!selected.remove(intent.id)) selected.add(intent.id)
                it.copy(selectedIds = selected)
            }

            SuggestionsIntent.ToggleSelectAll -> _state.update { s ->
                val filteredIds = s.filteredPending.map { it.id }.toSet()
                if (s.allSelected) s.copy(selectedIds = s.selectedIds - filteredIds)
                else s.copy(selectedIds = s.selectedIds + filteredIds)
            }

            SuggestionsIntent.ClearSelection -> _state.update { it.copy(selectedIds = emptySet()) }

            is SuggestionsIntent.RequestAccept ->
                _state.update { it.copy(acceptConfirmId = intent.id) }

            SuggestionsIntent.ConfirmAccept -> {
                val id = _state.value.acceptConfirmId
                _state.update { it.copy(acceptConfirmId = null) }
                if (id != null) acceptAll(listOf(id))
            }

            is SuggestionsIntent.Reject -> rejectIndividual(intent.id)

            SuggestionsIntent.RequestAcceptSelected ->
                _state.update { it.copy(showAcceptConfirm = true) }

            SuggestionsIntent.ConfirmAcceptSelected -> {
                _state.update { it.copy(showAcceptConfirm = false, filter = SuggestionFilter()) }
                acceptAll(_state.value.selectedIds.toList())
            }

            SuggestionsIntent.RequestRejectSelected ->
                _state.update { it.copy(showRejectConfirm = true) }

            SuggestionsIntent.ConfirmRejectSelected -> {
                _state.update { it.copy(showRejectConfirm = false, filter = SuggestionFilter()) }
                rejectAll(_state.value.selectedIds.toList())
            }

            SuggestionsIntent.DismissConfirm ->
                _state.update {
                    it.copy(showAcceptConfirm = false, showRejectConfirm = false, acceptConfirmId = null)
                }

            is SuggestionsIntent.UndoReject ->
                viewModelScope.launch { source.restoreToPending(intent.id) }

            is SuggestionsIntent.RestoreToPending ->
                viewModelScope.launch { source.restoreToPending(intent.id) }

            is SuggestionsIntent.ShowCategoryPicker ->
                _state.update { it.copy(categoryPickerForId = intent.id) }

            is SuggestionsIntent.SetCategory -> {
                categoryOverrides[intent.id] = intent.categoryId
                _state.update { s ->
                    s.copy(
                        categoryPickerForId = null,
                        pending = s.pending.map { row ->
                            if (row.id == intent.id) row.copy(
                                categoryId = intent.categoryId,
                                categoryName = s.categories.firstOrNull { it.id.value == intent.categoryId }?.name,
                            ) else row
                        },
                    )
                }
            }

            is SuggestionsIntent.ShowAccountPicker ->
                _state.update { it.copy(accountPickerForId = intent.id) }

            is SuggestionsIntent.SetAccount -> {
                accountOverrides[intent.id] = intent.accountId
                _state.update { s ->
                    s.copy(
                        accountPickerForId = null,
                        pending = s.pending.map { row ->
                            if (row.id == intent.id) row.copy(
                                targetAccountId = intent.accountId,
                                targetAccountName = s.accounts.firstOrNull { it.id.value == intent.accountId }?.name,
                            ) else row
                        },
                    )
                }
            }

            is SuggestionsIntent.ShowFilterSheet ->
                _state.update { it.copy(showFilterSheet = intent.show) }

            is SuggestionsIntent.SetFilterType ->
                _state.update { it.copy(filter = it.filter.copy(type = intent.type)) }

            is SuggestionsIntent.SetFilterMin ->
                _state.update { it.copy(filter = it.filter.copy(minText = intent.text)) }

            is SuggestionsIntent.SetFilterMax ->
                _state.update { it.copy(filter = it.filter.copy(maxText = intent.text)) }

            is SuggestionsIntent.SetFilterNote ->
                _state.update { it.copy(filter = it.filter.copy(note = intent.text)) }

            SuggestionsIntent.ClearFilter ->
                _state.update { it.copy(filter = SuggestionFilter()) }

            is SuggestionsIntent.ShowBatchCategoryPicker ->
                _state.update { it.copy(showBatchCategoryPicker = intent.show) }

            is SuggestionsIntent.SetCategoryForSelected -> {
                val category = _state.value.categories.firstOrNull { it.id.value == intent.categoryId }
                if (category != null) {
                    val catIsExpense = category.type == TransactionType.EXPENSE
                    _state.update { s ->
                        val targetIds = s.pending
                            .filter { it.id in s.selectedIds && it.isExpense == catIsExpense }
                            .map { it.id }
                            .toSet()
                        targetIds.forEach { categoryOverrides[it] = intent.categoryId }
                        s.copy(
                            showBatchCategoryPicker = false,
                            pending = s.pending.map { row ->
                                if (row.id in targetIds) row.copy(
                                    categoryId = intent.categoryId,
                                    categoryName = category.name,
                                ) else row
                            },
                        )
                    }
                } else {
                    _state.update { it.copy(showBatchCategoryPicker = false) }
                }
            }

            is SuggestionsIntent.ShowBatchAccountPicker ->
                _state.update { it.copy(showBatchAccountPicker = intent.show) }

            is SuggestionsIntent.SetAccountForSelected -> {
                val account = _state.value.accounts.firstOrNull { it.id.value == intent.accountId }
                _state.update { s ->
                    val targetIds = s.selectedIds
                    targetIds.forEach { accountOverrides[it] = intent.accountId }
                    s.copy(
                        showBatchAccountPicker = false,
                        pending = s.pending.map { row ->
                            if (row.id in targetIds) row.copy(
                                targetAccountId = intent.accountId,
                                targetAccountName = account?.name,
                            ) else row
                        },
                    )
                }
            }
        }
    }

    private fun rejectIndividual(id: Long) {
        viewModelScope.launch {
            source.reject(id, clock.now().toEpochMilliseconds())
            _state.update { it.copy(selectedIds = it.selectedIds - id) }
            _singleEvent.send(SuggestionsSingleUiEvent.RejectedWithUndo(id))
        }
    }

    private fun acceptAll(ids: List<Long>) {
        val rows = _state.value.pending.filter { it.id in ids }
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            for (row in rows) {
                val accountId = row.targetAccountId ?: continue
                val categoryId = row.categoryId ?: continue
                val record = source.getRecord(row.id) ?: continue
                acceptSuggestion(source, record, accountId = accountId, categoryId = categoryId)
            }
            _state.update { it.copy(selectedIds = emptySet(), isProcessing = false) }
        }
    }

    private fun rejectAll(ids: List<Long>) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            val now = clock.now().toEpochMilliseconds()
            ids.forEach { source.reject(it, now) }
            _state.update { it.copy(selectedIds = emptySet(), isProcessing = false) }
        }
    }

    private data class Sources(
        val pending: List<SuggestionRecord>,
        val rejected: List<SuggestionRecord>,
        val accounts: List<Account>,
        val categories: List<Category>,
    )
}
