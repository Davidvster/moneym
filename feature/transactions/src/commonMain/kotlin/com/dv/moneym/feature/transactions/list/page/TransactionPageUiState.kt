package com.dv.moneym.feature.transactions.list.page

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TxDisplayPrefs

internal data class TransactionPageUiState(
    val isLoading: Boolean = true,
    val dayGroups: List<com.dv.moneym.feature.transactions.list.DayGroup> = emptyList(),
    val isEmpty: Boolean = false,
    val txDisplayPrefs: TxDisplayPrefs = TxDisplayPrefs(),
    val selection: TransactionSelectionUiState = TransactionSelectionUiState(),
    val availableCategories: List<Category> = emptyList(),
    val availableAccounts: List<Account> = emptyList(),
    val paymentModes: List<PaymentMode> = emptyList(),
    val paymentModeEnabled: Boolean = false,
    val bulkSheet: BulkSheetState = BulkSheetState.None,
    val bulkRateText: String = "",
    val bulkRateError: Boolean = false,
)

internal data class TransactionSelectionUiState(
    val selectedIds: Set<TransactionId> = emptySet(),
    val selectedCount: Int = 0,
    val currencyTotals: List<SelectionCurrencyTotal> = emptyList(),
    val canMoveWallet: Boolean = false,
    val canMovePaymentMode: Boolean = false,
) {
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
}

internal data class SelectionCurrencyTotal(
    val currency: String,
    val minorUnits: Long,
)

internal sealed interface BulkSheetState {
    data object None : BulkSheetState
    data object Actions : BulkSheetState
    data object DeleteConfirm : BulkSheetState
    data object CategoryPicker : BulkSheetState
    data class CategoryConfirm(val category: Category) : BulkSheetState
    data object WalletPicker : BulkSheetState
    data class WalletConfirm(val account: Account, val requiresRate: Boolean) : BulkSheetState
    data object PaymentModePicker : BulkSheetState
    data class PaymentModeConfirm(val paymentMode: PaymentMode) : BulkSheetState
}

internal sealed interface TransactionPageIntent {
    data class TransactionPressed(val id: TransactionId) : TransactionPageIntent
    data class TransactionLongPressed(val id: TransactionId) : TransactionPageIntent
    data object ClearSelection : TransactionPageIntent
    data object DeleteRequested : TransactionPageIntent
    data object EditRequested : TransactionPageIntent
    data object DismissBulkSheet : TransactionPageIntent
    data object ConfirmDelete : TransactionPageIntent
    data object PickCategoryRequested : TransactionPageIntent
    data class CategoryPicked(val id: CategoryId) : TransactionPageIntent
    data object ConfirmCategory : TransactionPageIntent
    data object PickWalletRequested : TransactionPageIntent
    data class WalletPicked(val account: Account) : TransactionPageIntent
    data class WalletRateChanged(val text: String) : TransactionPageIntent
    data object ConfirmWallet : TransactionPageIntent
    data object PickPaymentModeRequested : TransactionPageIntent
    data class PaymentModePicked(val id: PaymentModeId) : TransactionPageIntent
    data object ConfirmPaymentMode : TransactionPageIntent
}
