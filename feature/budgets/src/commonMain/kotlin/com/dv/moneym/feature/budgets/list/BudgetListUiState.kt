package com.dv.moneym.feature.budgets.list

import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.Money
import kotlinx.serialization.Serializable

@Serializable
internal data class BudgetListUiState(
    val isLoading: Boolean = true,
    val rows: List<BudgetRowVm> = emptyList(),
    val deleteRequestId: BudgetId? = null,
    val deleteRequestName: String? = null,
)

@Serializable
internal data class BudgetRowVm(
    val id: BudgetId,
    val name: String,
    val amount: Money,
    val scopeLabel: String,
    val recurringLabel: String?,
)

internal sealed interface BudgetListIntent {
    data class DeleteRequested(val id: BudgetId) : BudgetListIntent
    data object ConfirmDelete : BudgetListIntent
    data object DismissDelete : BudgetListIntent
}
