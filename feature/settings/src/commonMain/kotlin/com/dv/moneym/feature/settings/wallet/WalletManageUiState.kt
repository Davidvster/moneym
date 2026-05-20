package com.dv.moneym.feature.settings.wallet

import com.dv.moneym.core.model.Account
import kotlinx.serialization.Serializable

@Serializable
internal data class WalletManageUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long = -1L,
    val pendingDeleteId: Long? = null,
    val pendingEditCurrencyAccountId: Long? = null,
)

internal sealed interface WalletManageIntent {
    data class SelectAccount(val id: Long) : WalletManageIntent
    data class AddWallet(val name: String, val currency: String) : WalletManageIntent
    data class DeleteRequested(val id: Long) : WalletManageIntent
    data object DeleteConfirmed : WalletManageIntent
    data object DeleteCancelled : WalletManageIntent
    data class EditCurrencyRequested(val accountId: Long) : WalletManageIntent
    data object EditCurrencyCancelled : WalletManageIntent
    data class UpdateCurrency(val accountId: Long, val currency: String) : WalletManageIntent
}
