package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.AccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class WalletManageViewModel(
    private val accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    internal val state: StateFlow<WalletManageUiState> = combine(
        accountRepository.observeAll(),
        appSettingsRepository.observeSelectedAccountId(),
    ) { accounts, selectedId ->
        WalletManageUiState(
            accounts = accounts,
            selectedAccountId = selectedId,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WalletManageUiState())

    internal fun onIntent(intent: WalletManageIntent) {
        when (intent) {
            is WalletManageIntent.SelectAccount -> viewModelScope.launch {
                appSettingsRepository.setSelectedAccountId(intent.id)
            }

            is WalletManageIntent.AddWallet -> {
                viewModelScope.launch {
                    val accounts = accountRepository.observeAll().stateIn(viewModelScope).value
                    accountRepository.insert(
                        Account(
                            id = AccountId(0),
                            name = intent.name,
                            type = AccountType.CASH,
                            currency = CurrencyCode(intent.currency),
                            isDefault = accounts.isEmpty(),
                            archived = false,
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        )
                    )
                }
            }
        }
    }
}
