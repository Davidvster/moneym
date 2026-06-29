package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode

import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class WalletManageViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val appSettingsRepository: AppSettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _pendingDeleteId by savedStateHandle.saved { MutableStateFlow<Long?>(null) }
    private val _showLastWalletDeleteBlockedDialog by savedStateHandle.saved {
        MutableStateFlow(false)
    }

    internal val state: StateFlow<WalletManageUiState> = combine(
        accountRepository.observeAll(),
        appSettingsRepository.observeSelectedAccountId(),
        _pendingDeleteId,
        _showLastWalletDeleteBlockedDialog,
    ) { accounts, selectedId, pendingDeleteId, showLastWalletDeleteBlockedDialog ->
        WalletManageUiState(
            accounts = accounts,
            selectedAccountId = selectedId,
            pendingDeleteId = pendingDeleteId,
            showLastWalletDeleteBlockedDialog = showLastWalletDeleteBlockedDialog,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, WalletManageUiState())

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

            is WalletManageIntent.DeleteRequested -> {
                val activeWalletCount = state.value.accounts.count { !it.archived }
                if (activeWalletCount <= 1) {
                    _pendingDeleteId.value = null
                    _showLastWalletDeleteBlockedDialog.value = true
                } else {
                    _pendingDeleteId.value = intent.id
                }
            }

            WalletManageIntent.DeleteCancelled -> _pendingDeleteId.value = null

            WalletManageIntent.LastWalletDeleteBlockedDismissed ->
                _showLastWalletDeleteBlockedDialog.value = false

            WalletManageIntent.DeleteConfirmed -> {
                val id = _pendingDeleteId.value ?: return
                _pendingDeleteId.value = null
                viewModelScope.launch {
                    transactionRepository.deleteByAccountId(AccountId(id))
                    accountRepository.delete(AccountId(id))
                }
            }

        }
    }
}
