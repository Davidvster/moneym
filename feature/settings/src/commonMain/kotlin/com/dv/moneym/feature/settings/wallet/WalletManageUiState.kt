package com.dv.moneym.feature.settings.wallet

import com.dv.moneym.core.model.Account
import kotlinx.serialization.Serializable

@Serializable
internal data class WalletManageUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long = -1L,
)

internal sealed interface WalletManageIntent {
    data class SelectAccount(val id: Long) : WalletManageIntent
    data class AddWallet(val name: String, val currency: String) : WalletManageIntent
}
