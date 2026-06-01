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
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.transactionedit.domain.DeleteTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.GetTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import com.dv.moneym.feature.transactionedit.usecase.RecurrenceInput
import com.dv.moneym.feature.transactionedit.usecase.SuggestNotesUseCase
import com.dv.moneym.feature.transactionedit.usecase.ValidateAndBuildTransactionUseCase
import com.dv.moneym.feature.transactionedit.usecase.ValidationOutcome
import com.dv.moneym.feature.transactionedit.usecase.ComputeCategoryBudgetRemainingUseCase
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val validateAndBuildTransaction: ValidateAndBuildTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val computeBudgetRemaining: ComputeCategoryBudgetRemainingUseCase,
    private val suggestNotes: SuggestNotesUseCase,
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
        viewModelScope.launch {
            _state
                .map { s ->
                    listOf(
                        s.type.name,
                        s.selectedCategoryId?.value?.toString().orEmpty(),
                        s.date?.toString().orEmpty(),
                        s.amountText,
                        s.selectedAccountId?.value?.toString().orEmpty(),
                    )
                }
                .distinctUntilChanged()
                .collect {
                    val s = _state.value
                    val type = s.type
                    val catId = s.selectedCategoryId
                    val date = s.date
                    val accountId = s.selectedAccountId
                    if (type == com.dv.moneym.core.model.TransactionType.EXPENSE && catId != null && date != null) {
                        val remaining = withContext(dispatchers.io) {
                            computeBudgetRemaining(catId, date, s.existingId, accountId)
                        }
                        val parsedAmount = parseMinorFromText(s.amountText)
                        val projected = if (parsedAmount > 0L) withContext(dispatchers.io) {
                            computeBudgetRemaining(catId, date, s.existingId, accountId, additionalExpenseMinor = parsedAmount)
                        } else null
                        _state.update { it.copy(budgetRemaining = remaining, budgetProjected = projected) }
                    } else {
                        _state.update { it.copy(budgetRemaining = null, budgetProjected = null) }
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

            is TransactionEditIntent.AccountSelected -> {
                _state.update { it.copy(selectedAccountId = intent.id) }
                viewModelScope.launch { appSettingsRepository.setSelectedAccountId(intent.id.value) }
            }
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

            is TransactionEditIntent.RecurringToggled -> _state.update { s ->
                val startDate = s.date ?: today
                if (!intent.on) {
                    s.copy(isRecurring = false, recurrenceError = false)
                } else {
                    s.copy(
                        isRecurring = true,
                        recurrenceError = false,
                        weekDay = startDate.dayOfWeek.isoDayNumber,
                        monthDayKind = MonthlyDayKind.OnDay(minOf(startDate.day, 28).coerceAtLeast(1)),
                    )
                }
            }

            is TransactionEditIntent.FreqUnitChanged -> _state.update { s ->
                val startDate = s.date ?: today
                when (intent.unit) {
                    FreqUnit.WEEKS -> s.copy(
                        freqUnit = intent.unit,
                        weekDay = startDate.dayOfWeek.isoDayNumber,
                        recurrenceError = false,
                    )
                    FreqUnit.MONTHS -> s.copy(
                        freqUnit = intent.unit,
                        monthDayKind = MonthlyDayKind.OnDay(minOf(startDate.day, 28).coerceAtLeast(1)),
                        recurrenceError = false,
                    )
                    FreqUnit.DAYS -> s.copy(freqUnit = intent.unit, recurrenceError = false)
                }
            }

            is TransactionEditIntent.FreqIntervalChanged -> _state.update {
                it.copy(freqInterval = intent.value.coerceIn(1, 30), recurrenceError = false)
            }

            is TransactionEditIntent.WeekDayChanged -> _state.update {
                it.copy(weekDay = intent.day.coerceIn(1, 7), recurrenceError = false)
            }

            is TransactionEditIntent.MonthDayChanged -> _state.update {
                it.copy(monthDayKind = intent.kind, recurrenceError = false)
            }

            is TransactionEditIntent.EndKindChanged -> _state.update { s ->
                val startDate = s.date ?: today
                val defaultedEnd = if (intent.kind == EndKind.UNTIL && s.endDate == null) {
                    startDate.plus(kotlinx.datetime.DatePeriod(months = 1))
                } else s.endDate
                s.copy(endKind = intent.kind, endDate = defaultedEnd, recurrenceError = false)
            }

            is TransactionEditIntent.EndCountChanged -> _state.update {
                it.copy(endCount = intent.value.coerceAtLeast(1), recurrenceError = false)
            }

            is TransactionEditIntent.EndDateChanged -> _state.update {
                it.copy(endDate = intent.date, recurrenceError = false)
            }
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
            _state.update { it.copy(noteSuggestions = suggestNotes(allTxns, query, today)) }
        }
    }

    private fun save() {
        val s = _state.value
        val outcome = validateAndBuildTransaction(
            existingId = s.existingId,
            type = s.type,
            amountText = s.amountText,
            date = s.date,
            selectedCategoryId = s.selectedCategoryId,
            selectedAccountId = s.selectedAccountId,
            note = s.note,
            availableAccounts = s.availableAccounts,
            showPaymentMode = s.showPaymentMode,
            selectedPaymentModeId = s.selectedPaymentModeId,
            now = clock.now(),
            recurrence = RecurrenceInput(
                isRecurring = s.isRecurring,
                freqUnit = s.freqUnit,
                freqInterval = s.freqInterval,
                weekDay = s.weekDay,
                monthDayKind = s.monthDayKind,
                endKind = s.endKind,
                endCount = s.endCount,
                endDate = s.endDate,
            ),
        )
        when (outcome) {
            is ValidationOutcome.Invalid -> when (outcome.reason) {
                ValidationOutcome.Reason.InvalidAmount ->
                    _state.update { it.copy(amountError = true) }
                ValidationOutcome.Reason.MissingCategory ->
                    _state.update { it.copy(categoryError = true) }
                ValidationOutcome.Reason.MissingDateOrAccount -> Unit
                ValidationOutcome.Reason.InvalidRecurrence ->
                    _state.update { it.copy(recurrenceError = true) }
            }
            is ValidationOutcome.Ok -> {
                _state.update { it.copy(isSaving = true) }
                viewModelScope.launch {
                    withContext(dispatchers.io) {
                        val rule = outcome.rule
                        if (rule == null) {
                            upsertTransaction(outcome.transaction)
                        } else {
                            val ruleId = recurringTransactionRepository.upsert(rule)
                            val startDate = rule.startDate
                            if (startDate <= today) {
                                upsertTransaction(outcome.transaction.copy(recurringId = ruleId))
                                recurringTransactionRepository.updateCursor(ruleId, startDate)
                            }
                        }
                    }
                    _state.update { it.copy(isSaving = false) }
                    _effects.send(TransactionEditEffect.Saved)
                }
            }
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

private fun parseMinorFromText(text: String): Long {
    if (text.isBlank()) return 0L
    val normalized = text.replace(',', '.')
    val value = normalized.toDoubleOrNull() ?: return 0L
    if (value.isNaN() || value < 0) return 0L
    return (value * 100).toLong()
}

private fun Long.toAmountText(): String {
    val major = this / 100
    val cents = this % 100
    return "$major.${cents.toString().padStart(2, '0')}"
}

private fun AccountId.toCurrencyCode(accounts: List<Account>): CurrencyCode =
    accounts.firstOrNull { it.id == this }?.currency ?: CurrencyCode("USD")
