package com.dv.moneym.feature.transactionedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.toMinorUnits
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.transactionedit.domain.DeleteTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.GetTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

class TransactionEditViewModel(
    private val editingId: TransactionId?,
    private val getTransaction: GetTransactionUseCase,
    private val upsertTransaction: UpsertTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val isNewTransaction = editingId == null

    private val today = clock.today()

    private val _state by savedStateHandle.saved {
        MutableStateFlow(
            TransactionEditUiState(
                isLoading = editingId != null,
                isEditMode = editingId != null,
                date = today,
                isToday = true,
            )
        )
    }
    internal val state: StateFlow<TransactionEditUiState> = _state
        .onStart { init() }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<TransactionEditEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    private suspend fun init() {
        if (isNewTransaction) {
            val defaultType = appSettingsRepository.observeDefaultTransactionType().first()
            _state.update { it.copy(type = defaultType) }
        }
        viewModelScope.launch {
            // Observe payment mode enabled setting + available modes
            combine(
                appSettingsRepository.observePaymentModeEnabled(),
                paymentModeRepository.observeAll(),
            ) { enabled, modes -> enabled to modes }
                .collect { (enabled, modes) ->
                    _state.update { s ->
                        s.copy(
                            showPaymentMode = enabled,
                            paymentModes = modes,
                        )
                    }
                }
        }
        if (isNewTransaction) {
            val persistedAccId = appSettingsRepository.observeSelectedAccountId().first()
            if (persistedAccId > 0L) {
                _state.update { it.copy(selectedAccountId = AccountId(persistedAccId)) }
            }
        }
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
                                cats.firstOrNull { it.type == s.type }?.id
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
                            selectedPaymentModeId = txn.paymentModeId,
                        )
                    }
                }
            }
        }
    }

    internal fun onIntent(intent: TransactionEditIntent) {
        when (intent) {
            is TransactionEditIntent.TypeChanged -> _state.update {
                it.copy(
                    type = intent.type,
                    selectedCategoryId = null
                )
            }

            is TransactionEditIntent.AmountChanged -> _state.update {
                it.copy(
                    amountText = filterAmountInput(intent.text),
                    amountError = false,
                )
            }

            is TransactionEditIntent.DateChanged -> _state.update {
                it.copy(
                    date = intent.date,
                    isToday = intent.date == today
                )
            }

            is TransactionEditIntent.YesterdayTodayClicked -> {
                val newDate =
                    if (_state.value.isToday == true) today.minus(DatePeriod(days = 1)) else today
                _state.update { it.copy(date = newDate, isToday = newDate == today) }
            }

            is TransactionEditIntent.CategorySelected -> _state.update {
                it.copy(
                    selectedCategoryId = intent.id,
                    categoryError = false
                )
            }

            is TransactionEditIntent.AccountSelected -> _state.update { it.copy(selectedAccountId = intent.id) }
            is TransactionEditIntent.NoteChanged -> {
                _state.update { it.copy(note = intent.note) }
                updateNoteSuggestions(intent.note)
            }

            is TransactionEditIntent.NoteSelected -> {
                val note = intent.note
                val currentType = _state.value.type
                viewModelScope.launch {
                    val allTxns = withContext(dispatchers.io) {
                        transactionRepository.observeAll().first()
                    }
                    val matchTxn = allTxns
                        .filter { it.note == note && it.type == currentType }
                        .maxByOrNull { it.occurredOn }
                    _state.update { s ->
                        s.copy(
                            note = note,
                            noteSuggestions = emptyList(),
                            selectedCategoryId = matchTxn?.categoryId ?: s.selectedCategoryId,
                            categoryError = false,
                        )
                    }
                }
            }

            is TransactionEditIntent.PaymentModeSelected ->
                _state.update { it.copy(selectedPaymentModeId = intent.id) }

            TransactionEditIntent.SaveRequested -> save()
            TransactionEditIntent.DeleteRequested -> _state.update { it.copy(showDeleteConfirm = true) }
            TransactionEditIntent.DeleteConfirmed -> delete()
            TransactionEditIntent.DeleteCancelled -> _state.update { it.copy(showDeleteConfirm = false) }
            is TransactionEditIntent.ShowDeleteDialog ->
                _state.update { it.copy(showDeleteDialog = intent.visible) }
        }
    }

    private fun filterAmountInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val dotIndex = filtered.indexOf('.')
        return if (dotIndex == -1) {
            filtered
        } else {
            val before = filtered.substring(0, dotIndex)
            val after = filtered.substring(dotIndex + 1)
                .filter { it.isDigit() }
                .take(2)
            "$before.$after"
        }
    }

    private fun updateNoteSuggestions(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _state.update { it.copy(noteSuggestions = emptyList()) }
                return@launch
            }
            val allTxns = withContext(dispatchers.io) {
                transactionRepository.observeAll().first()
            }
            val noteCounts = allTxns
                .mapNotNull { it.note }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()

            val q = query.lowercase()
            val prefixMatches = noteCounts.entries
                .filter { it.key.lowercase().startsWith(q) && it.key != query }
                .sortedByDescending { it.value }
                .map { it.key }
            val containsMatches = noteCounts.entries
                .filter {
                    it.key.lowercase().contains(q) && !it.key.lowercase()
                        .startsWith(q) && it.key != query
                }
                .sortedByDescending { it.value }
                .map { it.key }

            _state.update { it.copy(noteSuggestions = (prefixMatches + containsMatches).take(5)) }
        }
    }

    private fun save() {
        val s = _state.value
        val minorUnits = s.amountText.toMinorUnits()
        val catId = s.selectedCategoryId
        val accId = s.selectedAccountId
        val date = s.date

        if (minorUnits == null || minorUnits <= 0) {
            _state.update { it.copy(amountError = true) }; return
        }
        if (catId == null) {
            _state.update { it.copy(categoryError = true) }; return
        }
        if (date == null || accId == null) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val account = s.availableAccounts.firstOrNull { it.id == accId }
            val currency = account?.currency ?: CurrencyCode("USD")
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
                paymentModeId = if (s.showPaymentMode) s.selectedPaymentModeId else null,
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

private fun AccountId.toCurrencyCode(accounts: List<Account>): CurrencyCode =
    accounts.firstOrNull { it.id == this }?.currency ?: CurrencyCode("USD")
