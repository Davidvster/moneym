package com.dv.moneym.feature.transactionedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecurringEditViewModel(
    private val ruleId: RecurringTransactionId,
    private val recurringRepo: RecurringTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved {
        MutableStateFlow(
            TransactionEditUiState(
                isLoading = true,
                isEditMode = true,
                isRecurring = true
            )
        )
    }
    internal val state: StateFlow<TransactionEditUiState> = _state
        .onStart { init() }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<TransactionEditEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    private suspend fun init() {
        if (ruleId.value == 0L) {
            initNewRule()
            return
        }
        val rule = recurringRepo.getById(ruleId) ?: run {
            _effects.send(TransactionEditEffect.Deleted)
            return
        }
        viewModelScope.launch {
            combine(
                categoryRepository.observeActive(),
                accountRepository.observeAll(),
                paymentModeRepository.observeAll(),
                appSettingsRepository.observePaymentModeEnabled(),
            ) { cats, accs, modes, pmEnabled ->
                Quad(cats, accs, modes, pmEnabled)
            }.collect { (cats, accs, modes, pmEnabled) ->
                _state.update { s ->
                    s.copy(
                        availableCategories = cats,
                        availableAccounts = accs,
                        paymentModes = modes,
                        showPaymentMode = pmEnabled,
                    )
                }
            }
        }
        _state.update {
            it.copy(
                isLoading = false,
                isEditMode = true,
                isRecurring = true,
                existingId = null,
                type = rule.type,
                amountText = rule.amount.minorUnits.toAmountText(),
                date = rule.startDate,
                isToday = false,
                selectedCategoryId = rule.categoryId,
                selectedAccountId = rule.accountId,
                note = rule.note ?: "",
                selectedPaymentModeId = rule.paymentModeId,
                freqUnit = when (val r = rule.rule) {
                    is RecurrenceRule.Daily -> FreqUnit.DAYS
                    is RecurrenceRule.Weekly -> FreqUnit.WEEKS
                    is RecurrenceRule.Monthly -> FreqUnit.MONTHS
                },
                freqInterval = rule.rule.interval,
                weekDay = (rule.rule as? RecurrenceRule.Weekly)?.dayOfWeek ?: 1,
                monthDayKind = (rule.rule as? RecurrenceRule.Monthly)?.dayKind
                    ?: MonthlyDayKind.OnDay(1),
                endKind = when (rule.endCondition) {
                    EndCondition.Unlimited -> EndKind.UNLIMITED
                    is EndCondition.Count -> EndKind.COUNT
                    is EndCondition.Until -> EndKind.UNTIL
                },
                endCount = (rule.endCondition as? EndCondition.Count)?.occurrences ?: 12,
                endDate = (rule.endCondition as? EndCondition.Until)?.date,
            )
        }
    }

    private fun initNewRule() {
        viewModelScope.launch {
            combine(
                categoryRepository.observeActive(),
                accountRepository.observeAll(),
                paymentModeRepository.observeAll(),
                appSettingsRepository.observePaymentModeEnabled(),
            ) { cats, accs, modes, pmEnabled ->
                Quad(cats, accs, modes, pmEnabled)
            }.collect { (cats, accs, modes, pmEnabled) ->
                val defaultExpenseCat = cats.firstOrNull { it.type == TransactionType.EXPENSE }
                val defaultAcc = accs.firstOrNull { it.isDefault } ?: accs.firstOrNull()
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        isEditMode = false,
                        isRecurring = true,
                        availableCategories = cats,
                        availableAccounts = accs,
                        paymentModes = modes,
                        showPaymentMode = pmEnabled,
                        selectedCategoryId = s.selectedCategoryId ?: defaultExpenseCat?.id,
                        selectedAccountId = s.selectedAccountId ?: defaultAcc?.id,
                        date = s.date ?: clock.today(),
                    )
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
                it.copy(amountText = filterAmountInput(intent.text), amountError = false)
            }

            is TransactionEditIntent.CategorySelected -> _state.update {
                it.copy(selectedCategoryId = intent.id, categoryError = false)
            }

            is TransactionEditIntent.AccountSelected -> _state.update { it.copy(selectedAccountId = intent.id) }
            is TransactionEditIntent.NoteChanged -> _state.update { it.copy(note = intent.note) }
            is TransactionEditIntent.PaymentModeSelected -> _state.update {
                it.copy(
                    selectedPaymentModeId = intent.id
                )
            }

            TransactionEditIntent.SaveRequested -> save()
            TransactionEditIntent.DeleteRequested,
            TransactionEditIntent.DeleteConfirmed -> delete()

            TransactionEditIntent.DeleteCancelled -> _state.update {
                it.copy(
                    showDeleteConfirm = false,
                    showDeleteDialog = false
                )
            }

            is TransactionEditIntent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = intent.visible) }
            is TransactionEditIntent.FreqUnitChanged -> _state.update {
                it.copy(
                    freqUnit = intent.unit,
                    recurrenceError = false
                )
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

            is TransactionEditIntent.EndKindChanged -> _state.update {
                it.copy(endKind = intent.kind, recurrenceError = false)
            }

            is TransactionEditIntent.EndCountChanged -> _state.update {
                it.copy(endCount = intent.value.coerceAtLeast(1), recurrenceError = false)
            }

            is TransactionEditIntent.EndDateChanged -> _state.update {
                it.copy(endDate = intent.date, recurrenceError = false)
            }
            // No-ops in recurring-edit mode:
            is TransactionEditIntent.DateChanged,
            TransactionEditIntent.YesterdayTodayClicked,
            is TransactionEditIntent.NoteSelected,
            is TransactionEditIntent.RecurringToggled -> Unit
        }
    }

    private fun filterAmountInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val dotIndex = filtered.indexOf('.')
        return if (dotIndex == -1) filtered
        else filtered.substring(0, dotIndex) + "." + filtered.substring(dotIndex + 1)
            .filter { it.isDigit() }.take(2)
    }

    private fun save() {
        val s = _state.value
        val minorUnits = s.amountText.toMinorUnits() ?: run {
            _state.update { it.copy(amountError = true) }
            return
        }
        if (minorUnits <= 0) {
            _state.update { it.copy(amountError = true) }
            return
        }
        val catId = s.selectedCategoryId ?: run {
            _state.update { it.copy(categoryError = true) }
            return
        }
        val accId = s.selectedAccountId ?: return
        val date = s.date ?: return
        val rule = buildRule(s) ?: run {
            _state.update { it.copy(recurrenceError = true) }
            return
        }
        val end = buildEnd(s, date) ?: run {
            _state.update { it.copy(recurrenceError = true) }
            return
        }
        val account = s.availableAccounts.firstOrNull { it.id == accId }
        val currency = account?.currency ?: return
        val now = clock.now()
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (ruleId.value == 0L) {
                val newRule = RecurringTransaction(
                    id = RecurringTransactionId(0),
                    type = s.type,
                    amount = Money(minorUnits, currency),
                    note = s.note.trim().ifEmpty { null },
                    categoryId = catId,
                    accountId = accId,
                    paymentModeId = if (s.showPaymentMode) s.selectedPaymentModeId else null,
                    startDate = date,
                    rule = rule,
                    endCondition = end,
                    lastMaterializedDate = null,
                    createdAt = now,
                    updatedAt = now,
                )
                withContext(dispatchers.io) { recurringRepo.upsert(newRule) }
            } else {
                val existing = withContext(dispatchers.io) { recurringRepo.getById(ruleId) }
                if (existing == null) {
                    _effects.send(TransactionEditEffect.Deleted)
                    return@launch
                }
                val updated = existing.copy(
                    type = s.type,
                    amount = Money(minorUnits, currency),
                    note = s.note.trim().ifEmpty { null },
                    categoryId = catId,
                    accountId = accId,
                    paymentModeId = if (s.showPaymentMode) s.selectedPaymentModeId else null,
                    rule = rule,
                    endCondition = end,
                    updatedAt = now,
                )
                withContext(dispatchers.io) { recurringRepo.upsert(updated) }
            }
            _state.update { it.copy(isSaving = false) }
            _effects.send(TransactionEditEffect.Saved)
        }
    }

    private fun buildRule(s: TransactionEditUiState): RecurrenceRule? {
        if (s.freqInterval !in 1..30) return null
        return when (s.freqUnit) {
            FreqUnit.DAYS -> RecurrenceRule.Daily(s.freqInterval)
            FreqUnit.WEEKS -> {
                if (s.weekDay !in 1..7) return null
                RecurrenceRule.Weekly(s.freqInterval, s.weekDay)
            }

            FreqUnit.MONTHS -> {
                val k = s.monthDayKind
                if (k is MonthlyDayKind.OnDay && k.day !in 1..28) return null
                RecurrenceRule.Monthly(s.freqInterval, k)
            }
        }
    }

    private fun buildEnd(
        s: TransactionEditUiState,
        startDate: kotlinx.datetime.LocalDate
    ): EndCondition? = when (s.endKind) {
        EndKind.UNLIMITED -> EndCondition.Unlimited
        EndKind.COUNT -> if (s.endCount < 1) null else EndCondition.Count(s.endCount)
        EndKind.UNTIL -> s.endDate?.takeIf { it >= startDate }?.let { EndCondition.Until(it) }
    }

    private fun delete() {
        viewModelScope.launch {
            if (ruleId.value != 0L) {
                withContext(dispatchers.io) { recurringRepo.delete(ruleId) }
            }
            _effects.send(TransactionEditEffect.Deleted)
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private fun Long.toAmountText(): String {
    val major = this / 100
    val cents = this % 100
    return "$major.${cents.toString().padStart(2, '0')}"
}

private fun String.toMinorUnits(): Long? {
    if (isBlank()) return null
    val normalized = replace(',', '.')
    val v = normalized.toDoubleOrNull() ?: return null
    if (v.isNaN() || v < 0) return null
    return (v * 100).toLong()
}
