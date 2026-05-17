package com.dv.moneym.feature.settings.presentation

import com.dv.moneym.core.model.Account

data class WalletManageUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long = -1L,
    val showAddDialog: Boolean = false,
)

sealed interface WalletManageIntent {
    data class SelectAccount(val id: Long) : WalletManageIntent
    data object ShowAddDialog : WalletManageIntent
    data object DismissAddDialog : WalletManageIntent
    data class AddWallet(val name: String, val currency: String) : WalletManageIntent
}
