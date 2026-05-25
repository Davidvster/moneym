package com.dv.moneym.feature.budgets.create

import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.YearMonth
import kotlinx.serialization.Serializable

@Serializable
internal enum class RecurringKind { Single, Unlimited, NMonths }

@Serializable
internal data class BudgetCreateUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val amountText: String = "",
    val currency: String = "EUR",
    val availableCategories: List<Category> = emptyList(),
    val selectedCategoryId: CategoryId? = null,
    val periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
    val startYearMonth: YearMonth? = null,
    val recurringKind: RecurringKind = RecurringKind.Single,
    val recurringNMonths: Int = 3,
    val nameError: Boolean = false,
    val amountError: Boolean = false,
    val recurringCountError: Boolean = false,
)

internal sealed interface BudgetCreateIntent {
    data class NameChanged(val text: String) : BudgetCreateIntent
    data class AmountChanged(val text: String) : BudgetCreateIntent
    data class CategorySelected(val id: CategoryId?) : BudgetCreateIntent
    data class StartMonthChanged(val ym: YearMonth) : BudgetCreateIntent
    data class RecurringKindChanged(val kind: RecurringKind) : BudgetCreateIntent
    data class RecurringCountChanged(val n: Int) : BudgetCreateIntent
    data object Save : BudgetCreateIntent
}
