package com.dv.moneym.feature.transactionedit

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.transactionedit.usecase.CategoryBudgetRemaining
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
internal data class TransactionEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val existingId: TransactionId? = null,
    val draftApplied: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val date: LocalDate? = null,
    val selectedCategoryId: CategoryId? = null,
    val selectedAccountId: AccountId? = null,
    val note: String = "",
    val noteSuggestions: List<String> = emptyList(),
    val isToday: Boolean? = null,
    val availableCategories: List<Category> = emptyList(),
    val availableAccounts: List<Account> = emptyList(),
    val showDeleteConfirm: Boolean = false,
    val isSaving: Boolean = false,
    val amountError: Boolean = false,
    val categoryError: Boolean = false,
    val paymentModes: List<PaymentMode> = emptyList(),
    val selectedPaymentModeId: PaymentModeId? = null,
    val showPaymentMode: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isRecurring: Boolean = false,
    val freqUnit: FreqUnit = FreqUnit.MONTHS,
    val freqInterval: Int = 1,
    val weekDay: Int = 1,
    val monthDayKind: MonthlyDayKind = MonthlyDayKind.OnDay(1),
    val endKind: EndKind = EndKind.UNLIMITED,
    val endCount: Int = 12,
    val endDate: LocalDate? = null,
    val recurrenceError: Boolean = false,
    @kotlinx.serialization.Transient val budgetRemaining: CategoryBudgetRemaining? = null,
    @kotlinx.serialization.Transient val budgetProjected: CategoryBudgetRemaining? = null,
)

enum class FreqUnit { DAYS, WEEKS, MONTHS }
enum class EndKind { UNLIMITED, COUNT, UNTIL }

internal sealed interface TransactionEditEffect {
    data object Saved : TransactionEditEffect
    data object Deleted : TransactionEditEffect
}
