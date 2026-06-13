package com.dv.moneym.feature.banksync.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.banksync.BankSyncEngine
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.BankSyncRuntimeState
import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BankSyncHomeViewModel(
    private val credentialsStore: EnableBankingCredentialsStore,
    private val client: EnableBankingClient,
    private val bankSyncRepository: BankSyncRepository,
    private val engine: BankSyncEngine,
    private val appSettings: AppSettings,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(BankSyncHomeUiState()) }
    internal val state = _state
        .onStart { viewModelScope.launch { refreshConnectionState() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), _state.value)

    init {
        viewModelScope.launch {
            bankSyncRepository.observeAccounts().collect { accounts ->
                _state.update { s ->
                    s.copy(
                        accounts = accounts.map {
                            BankAccountRow(
                                uid = it.uid,
                                bankName = it.bankName,
                                displayName = it.displayName,
                                iban = it.iban,
                                currency = it.currency,
                                localAccountId = it.localAccountId,
                                enabled = it.enabled,
                            )
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            bankSyncRepository.observePendingCount().collect { count ->
                _state.update { it.copy(pendingCount = count) }
            }
        }
        viewModelScope.launch {
            accountRepository.observeAll().collect { accounts ->
                _state.update { s ->
                    s.copy(
                        localAccounts = accounts
                            .filter { !it.archived }
                            .map { LocalAccountOption(id = it.id.value, name = it.name) }
                    )
                }
            }
        }
        viewModelScope.launch {
            engine.runtime.collect { runtime ->
                _state.update {
                    it.copy(
                        isSyncing = runtime is BankSyncRuntimeState.Running,
                        syncError = (runtime as? BankSyncRuntimeState.Error)?.message,
                        reconnectRequired = (runtime as? BankSyncRuntimeState.Error)?.reconnectRequired == true,
                        lastSyncMs = appSettings.getLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS),
                    )
                }
            }
        }
    }

    fun onIntent(intent: BankSyncHomeIntent) {
        when (intent) {
            is BankSyncHomeIntent.ShowAccountPicker ->
                _state.update { it.copy(accountPickerForUid = intent.uid) }

            is BankSyncHomeIntent.SetLocalAccountMapping ->
                setMapping(intent.uid, intent.localAccountId)

            is BankSyncHomeIntent.SetAccountEnabled -> setEnabled(intent.uid, intent.enabled)

            BankSyncHomeIntent.ToggleAutoSync -> toggleAutoSync()

            BankSyncHomeIntent.SyncNow -> syncNow()

            BankSyncHomeIntent.Disconnect -> disconnect()
        }
    }

    private suspend fun refreshConnectionState() {
        val configured = credentialsStore.loadCredentials() != null
        val connected = credentialsStore.loadSessionId() != null
        val validUntil = appSettings.getLong(PrefKeys.BANK_SYNC_SESSION_VALID_UNTIL_MS)
        _state.update {
            it.copy(
                isLoading = false,
                configured = configured,
                connected = connected,
                sessionValidUntilMs = validUntil.takeIf { ms -> ms > 0 },
                autoSyncEnabled = appSettings.getBoolean(
                    PrefKeys.BANK_SYNC_AUTO_ENABLED,
                    defaultValue = false
                ),
                lastSyncMs = appSettings.getLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS),
            )
        }
    }

    private fun setMapping(uid: String, localAccountId: Long?) {
        viewModelScope.launch {
            bankSyncRepository.setLocalAccountMapping(uid, localAccountId)
            _state.update { it.copy(accountPickerForUid = null) }
        }
    }

    private fun setEnabled(uid: String, enabled: Boolean) {
        viewModelScope.launch { bankSyncRepository.setAccountEnabled(uid, enabled) }
    }

    private fun toggleAutoSync() {
        val newValue = !_state.value.autoSyncEnabled
        appSettings.putBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED, newValue)
        _state.update { it.copy(autoSyncEnabled = newValue) }
    }

    private fun syncNow() {
        viewModelScope.launch {
            engine.syncNow()
            _state.update { it.copy(lastSyncMs = appSettings.getLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS)) }
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            credentialsStore.loadSessionId()?.let { client.deleteSession(it) }
            credentialsStore.clearAll()
            bankSyncRepository.clearAll()
            appSettings.putBoolean(PrefKeys.BANK_SYNC_CONFIGURED, false)
            appSettings.putBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED, false)
            appSettings.putLong(PrefKeys.BANK_SYNC_SESSION_VALID_UNTIL_MS, 0L)
            _state.update {
                BankSyncHomeUiState(
                    isLoading = false,
                    localAccounts = it.localAccounts,
                )
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
