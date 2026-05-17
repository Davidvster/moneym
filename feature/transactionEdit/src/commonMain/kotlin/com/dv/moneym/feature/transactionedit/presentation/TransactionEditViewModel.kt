package com.dv.moneym.feature.transactionedit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.toMinorUnits
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.transactionedit.domain.DeleteTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.GetTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionEditViewModel(
    private val editingId: TransactionId?,
    private val getTransaction: GetTransactionUseCase,
    private val upsertTransaction: UpsertTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
) : ViewModel() {

    private val isNewTransaction = editingId == null

    private val _state = MutableStateFlow(TransactionEditUiState(
        isLoading = editingId != null,
        isEditMode = editingId != null,
        date = clock.today(),
    ))
    val state: StateFlow<TransactionEditUiState> = _state.asStateFlow()

    private val _effects = Channel<TransactionEditEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Load categories + accounts
            combine(
                categoryRepository.observeActive(),
                accountRepository.observeAll(),
            ) { cats, accs -> cats to accs }
                .collect { (cats, accs) ->
                    _state.update { s ->
                        val defaultAcc = accs.firstOrNull { it.isDefault } ?: accs.firstOrNull()
                        s.copy(
                            availableCategories = cats,
                            availableAccounts = accs,
                            selectedAccountId = s.selectedAccountId ?: defaultAcc?.id,
                            // For new transactions, pre-select the first category if none selected yet
                            selectedCategoryId = if (isNewTransaction && s.selectedCategoryId == null) {
                                cats.firstOrNull()?.id
                            } else {
                                s.selectedCategoryId
                            },
                        )
                    }
                }
        }
        if (editingId != null) {
            viewModelScope.launch {
                val txn = withContext(dispatchers.io) { getTransaction(editingId) }
                if (txn != null) {
                    _state.update { s ->
                        s.copy(
                            isLoading = false,
                            existingId = txn.id,
                            type = txn.type,
                            amountText = txn.amount.minorUnits.toAmountText(),
                            date = txn.occurredOn,
                            selectedCategoryId = txn.categoryId,
                            selectedAccountId = txn.accountId,
                            note = txn.note ?: "",
                        )
                    }
                }
            }
        }
    }

    fun onIntent(intent: TransactionEditIntent) {
        when (intent) {
            is TransactionEditIntent.TypeChanged -> _state.update { it.copy(type = intent.type) }
            is TransactionEditIntent.AmountChanged -> _state.update { it.copy(amountText = intent.text, amountError = false) }
            is TransactionEditIntent.DateChanged -> _state.update { it.copy(date = intent.date) }
            is TransactionEditIntent.CategorySelected -> _state.update { it.copy(selectedCategoryId = intent.id, categoryError = false) }
            is TransactionEditIntent.AccountSelected -> _state.update { it.copy(selectedAccountId = intent.id) }
            is TransactionEditIntent.NoteChanged -> _state.update { it.copy(note = intent.note) }
            TransactionEditIntent.SaveRequested -> save()
            TransactionEditIntent.DeleteRequested -> _state.update { it.copy(showDeleteConfirm = true) }
            TransactionEditIntent.DeleteConfirmed -> delete()
            TransactionEditIntent.DeleteCancelled -> _state.update { it.copy(showDeleteConfirm = false) }
        }
    }

    private fun save() {
        val s = _state.value
        val minorUnits = s.amountText.toMinorUnits()
        val catId = s.selectedCategoryId
        val accId = s.selectedAccountId
        val date = s.date

        if (minorUnits == null || minorUnits <= 0) { _state.update { it.copy(amountError = true) }; return }
        if (catId == null) { _state.update { it.copy(categoryError = true) }; return }
        if (date == null || accId == null) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val account = s.availableAccounts.firstOrNull { it.id == accId }
            val currency = account?.currency ?: CurrencyCode("EUR")
            val txn = Transaction(
                id = s.existingId ?: UNSAVED_TRANSACTION_ID,
                type = s.type,
                amount = Money(minorUnits, currency),
                occurredOn = date,
                note = s.note.trim().ifEmpty { null },
                categoryId = catId,
                accountId = accId,
                createdAt = clock.now(),
                updatedAt = clock.now(),
            )
            withContext(dispatchers.io) { upsertTransaction(txn) }
            _effects.send(TransactionEditEffect.Saved)
        }
    }

    private fun delete() {
        val id = _state.value.existingId ?: return
        viewModelScope.launch {
            withContext(dispatchers.io) { deleteTransaction(id) }
            _effects.send(TransactionEditEffect.Deleted)
        }
    }
}

private fun Long.toAmountText(): String {
    val major = this / 100
    val cents = this % 100
    return "$major.${cents.toString().padStart(2, '0')}"
}

private fun AccountId.toCurrencyCode(accounts: List<com.dv.moneym.core.model.Account>): CurrencyCode =
    accounts.firstOrNull { it.id == this }?.currency ?: CurrencyCode("EUR")
