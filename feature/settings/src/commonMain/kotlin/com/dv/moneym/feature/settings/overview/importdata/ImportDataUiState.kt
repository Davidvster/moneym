package com.dv.moneym.feature.settings.overview.importdata

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ImportDataUiState(
    val isParsing: Boolean = true,
    val isImporting: Boolean = false,
    val parseError: String? = null,
    val availableAccounts: List<Account> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val selectedAccountId: AccountId? = null,
    val categoryMappings: List<CategoryMappingUiItem> = emptyList(),
    val transactions: List<ImportTransactionUiItem> = emptyList(),
)

@Serializable
data class CategoryMappingUiItem(
    val csvName: String,
    val mappedToCategoryId: CategoryId?,
    val mappedToCategoryName: String?,
)

@Serializable
data class ImportTransactionUiItem(
    val id: String,
    val date: LocalDate,
    val type: TransactionType,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val categoryName: String,
    val note: String?,
    val isSelected: Boolean = true,
)

sealed interface ImportDataIntent {
    data class AccountSelected(val id: AccountId) : ImportDataIntent
    data class CategoryMappingChanged(
        val csvName: String,
        val newCategoryId: CategoryId?,
    ) : ImportDataIntent
    data class TransactionToggled(val id: String) : ImportDataIntent
    data object SelectAllToggled : ImportDataIntent
    data object ImportConfirmed : ImportDataIntent
}

sealed interface ImportDataEffect {
    data object ImportDone : ImportDataEffect
    data class ShowError(val message: String) : ImportDataEffect
}
