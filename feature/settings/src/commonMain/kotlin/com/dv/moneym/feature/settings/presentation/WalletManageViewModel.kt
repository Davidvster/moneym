package com.dv.moneym.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletManageViewModel(
    private val accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)

    val state: StateFlow<WalletManageUiState> = combine(
        accountRepository.observeAll(),
        appSettingsRepository.observeSelectedAccountId(),
        _showAddDialog,
    ) { accounts, selectedId, showDialog ->
        WalletManageUiState(
            accounts = accounts,
            selectedAccountId = selectedId,
            showAddDialog = showDialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalletManageUiState())

    fun onIntent(intent: WalletManageIntent) {
        when (intent) {
            WalletManageIntent.ShowAddDialog -> _showAddDialog.value = true
            WalletManageIntent.DismissAddDialog -> _showAddDialog.value = false
            is WalletManageIntent.SelectAccount -> viewModelScope.launch {
                appSettingsRepository.setSelectedAccountId(intent.id)
            }
            is WalletManageIntent.AddWallet -> {
                _showAddDialog.value = false
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
                            createdAt = kotlin.time.Clock.System.now(),
                            updatedAt = kotlin.time.Clock.System.now(),
                        )
                    )
                }
            }
        }
    }
}
