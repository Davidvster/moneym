package com.dv.moneym.feature.budgets.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DefaultSingleUiEvent
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.common.SingleUiEvent
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class BudgetCreateViewModel(
    private val budgetId: BudgetId?,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val clock: AppClock,
    private val dispatchers: DispatcherProvider,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface BudgetCreateSingleUiEvent : SingleUiEvent {
        data object NavigateBack : BudgetCreateSingleUiEvent,
            SingleUiEvent by DefaultSingleUiEvent()
    }

    private val _singleEvent: MutableStateFlow<BudgetCreateSingleUiEvent?> = MutableStateFlow(null)
    val singleEvents: StateFlow<BudgetCreateSingleUiEvent?> = _singleEvent

    private val _state = MutableStateFlow(BudgetCreateUiState(isEditMode = budgetId != null, isLoading = true))
    internal val state: StateFlow<BudgetCreateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = withContext(dispatchers.io) {
                categoryRepository.observeAll().first()
            }
            val existing = budgetId?.let {
                withContext(dispatchers.io) { budgetRepository.getById(it) }
            }
            if (existing != null) {
                _state.update {
                    BudgetCreateUiState(
                        isEditMode = true,
                        isLoading = false,
                        availableCategories = categories,
                        name = existing.name,
                        amountText = minorToDecimalText(existing.amount.minorUnits),
                        currency = existing.amount.currency.value,
                        selectedCategoryId = existing.categoryId,
                        periodType = existing.periodType,
                        startYearMonth = existing.startYearMonth,
                        recurringKind = recurringKindFromMonths(existing.recurringMonths),
                        recurringNMonths = existing.recurringMonths.takeIf { it != null && it > 0 } ?: 3,
                    )
                }
            } else {
                val defaultCurrency = withContext(dispatchers.io) {
                    accountRepository.observeDefault().first()?.currency?.value ?: "EUR"
                }
                val today = clock.today()
                @Suppress("DEPRECATION")
                val startYm = YearMonth(today.year, today.monthNumber)
                _state.update {
                    BudgetCreateUiState(
                        isEditMode = false,
                        isLoading = false,
                        availableCategories = categories,
                        currency = defaultCurrency,
                        startYearMonth = startYm,
                    )
                }
            }
        }
    }

    internal fun onIntent(intent: BudgetCreateIntent) {
        when (intent) {
            is BudgetCreateIntent.NameChanged -> _state.update { it.copy(name = intent.text, nameError = false) }
            is BudgetCreateIntent.AmountChanged -> _state.update {
                it.copy(amountText = sanitizeAmount(intent.text), amountError = false)
            }
            is BudgetCreateIntent.CategorySelected -> _state.update { it.copy(selectedCategoryId = intent.id) }
            is BudgetCreateIntent.StartMonthChanged -> _state.update { it.copy(startYearMonth = intent.ym) }
            is BudgetCreateIntent.RecurringKindChanged -> _state.update {
                it.copy(recurringKind = intent.kind, recurringCountError = false)
            }
            is BudgetCreateIntent.RecurringCountChanged -> _state.update {
                it.copy(recurringNMonths = intent.n.coerceAtLeast(1), recurringCountError = false)
            }
            BudgetCreateIntent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        val name = s.name.trim()
        val minor = parseAmountToMinor(s.amountText)
        val startYm = s.startYearMonth
        val recurringValid = s.recurringKind != RecurringKind.NMonths || s.recurringNMonths > 0

        val nameErr = name.isBlank()
        val amountErr = minor == null || minor <= 0
        val recurringErr = !recurringValid
        if (nameErr || amountErr || recurringErr || startYm == null) {
            _state.update {
                it.copy(
                    nameError = nameErr,
                    amountError = amountErr,
                    recurringCountError = recurringErr,
                )
            }
            return
        }
        val recurringMonths = when (s.recurringKind) {
            RecurringKind.Single -> null
            RecurringKind.Unlimited -> Budget.UNLIMITED
            RecurringKind.NMonths -> s.recurringNMonths
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            withContext(dispatchers.io) {
                val now = Clock.System.now()
                val budget = Budget(
                    id = budgetId ?: BudgetId(0),
                    name = name,
                    amount = Money(minor, CurrencyCode(s.currency)),
                    categoryId = s.selectedCategoryId,
                    periodType = s.periodType,
                    startYearMonth = startYm,
                    recurringMonths = recurringMonths,
                    createdAt = now,
                    updatedAt = now,
                )
                if (budgetId != null) budgetRepository.update(budget)
                else budgetRepository.insert(budget)
            }
            _state.update { it.copy(isSaving = false) }
            _singleEvent.value = BudgetCreateSingleUiEvent.NavigateBack
        }
    }

    private fun sanitizeAmount(raw: String): String =
        raw.filter { it.isDigit() || it == '.' || it == ',' }
            .replace(',', '.')
            .let { txt ->
                val dot = txt.indexOf('.')
                if (dot < 0) txt
                else txt.substring(0, dot + 1) + txt.substring(dot + 1).filter { it.isDigit() }.take(2)
            }

    private fun parseAmountToMinor(text: String): Long? {
        if (text.isBlank()) return null
        val normalized = text.replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        if (value.isNaN() || value < 0) return null
        return (value * 100).toLong()
    }

    private fun minorToDecimalText(minor: Long): String {
        val whole = minor / 100
        val frac = (minor % 100).toString().padStart(2, '0')
        return "$whole.$frac"
    }

    private fun recurringKindFromMonths(months: Int?): RecurringKind = when {
        months == null -> RecurringKind.Single
        months == Budget.UNLIMITED -> RecurringKind.Unlimited
        months > 0 -> RecurringKind.NMonths
        else -> RecurringKind.Single
    }
}
