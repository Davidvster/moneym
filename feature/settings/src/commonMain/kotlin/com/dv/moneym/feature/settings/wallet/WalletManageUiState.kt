package com.dv.moneym.feature.settings.wallet

import com.dv.moneym.core.model.Account
import kotlinx.serialization.Serializable

@Serializable
internal data class WalletManageUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long = -1L,
    val pendingDeleteId: Long? = null,
    val showLastWalletDeleteBlockedDialog: Boolean = false,
)

internal sealed interface WalletManageIntent {
    data class SelectAccount(val id: Long) : WalletManageIntent
    data class AddWallet(val name: String, val currency: String) : WalletManageIntent
    data class DeleteRequested(val id: Long) : WalletManageIntent
    data object DeleteConfirmed : WalletManageIntent
    data object DeleteCancelled : WalletManageIntent
    data object LastWalletDeleteBlockedDismissed : WalletManageIntent
}
