package com.dv.moneym.feature.transactionedit.presentation

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate

data class TransactionEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val existingId: TransactionId? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val date: LocalDate? = null,
    val selectedCategoryId: CategoryId? = null,
    val selectedAccountId: AccountId? = null,
    val note: String = "",
    val availableCategories: List<Category> = emptyList(),
    val availableAccounts: List<Account> = emptyList(),
    val showDeleteConfirm: Boolean = false,
    val isSaving: Boolean = false,
    val amountError: Boolean = false,
    val categoryError: Boolean = false,
)

sealed interface TransactionEditEffect {
    data object Saved : TransactionEditEffect
    data object Deleted : TransactionEditEffect
}
