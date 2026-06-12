package com.dv.moneym.feature.banksync

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.banksync.BankAuthCallbackBus
import com.dv.moneym.data.banksync.BankSyncEngine
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.BankSyncRuntimeState
import com.dv.moneym.data.banksync.EbCredentials
import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.feature.banksync.usecase.CompleteConnectionUseCase
import com.dv.moneym.feature.banksync.usecase.ConnectBankUseCase
import com.dv.moneym.feature.banksync.usecase.ParseRedirectCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BankSyncSettingsViewModel(
    private val credentialsStore: EnableBankingCredentialsStore,
    private val client: EnableBankingClient,
    private val bankSyncRepository: BankSyncRepository,
    private val engine: BankSyncEngine,
    private val appSettings: AppSettings,
    private val accountRepository: AccountRepository,
    private val connectBank: ConnectBankUseCase,
    private val completeConnection: CompleteConnectionUseCase,
    private val parseRedirectCode: ParseRedirectCodeUseCase,
    private val callbackBus: BankAuthCallbackBus,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(BankSyncSettingsUiState()) }
    internal val state = _state
        .onStart { init() }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private fun init() {
        viewModelScope.launch { refreshConnectionState() }
        viewModelScope.launch {
            callbackBus.callbacks.collect { url ->
                if (_state.value.awaitingAuth) submitRedirect(url)
            }
        }
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

    fun onIntent(intent: BankSyncSettingsIntent) {
        when (intent) {
            is BankSyncSettingsIntent.AppIdChanged ->
                _state.update { it.copy(appIdDraft = intent.value, credentialsError = null) }

            is BankSyncSettingsIntent.PemChanged ->
                _state.update { it.copy(pemDraft = intent.value, credentialsError = null) }

            BankSyncSettingsIntent.SaveCredentials -> saveCredentials()

            is BankSyncSettingsIntent.CountryChanged ->
                _state.update { it.copy(countryDraft = intent.value.uppercase().take(2)) }

            BankSyncSettingsIntent.LoadBanks -> loadBanks()

            is BankSyncSettingsIntent.BankSearchChanged ->
                _state.update { it.copy(bankSearch = intent.value) }

            is BankSyncSettingsIntent.ConnectBank -> startConnect(intent.name, intent.country)

            BankSyncSettingsIntent.AuthUrlOpened -> _state.update { it.copy(authUrlToOpen = null) }

            is BankSyncSettingsIntent.RedirectChanged ->
                _state.update {
                    it.copy(
                        redirectDraft = intent.value,
                        connectError = null,
                        redirectInvalid = false
                    )
                }

            BankSyncSettingsIntent.SubmitRedirect -> submitRedirect(_state.value.redirectDraft)

            is BankSyncSettingsIntent.RedirectReceived -> submitRedirect(intent.url)

            BankSyncSettingsIntent.CancelAuth ->
                _state.update {
                    it.copy(
                        awaitingAuth = false,
                        authUrlToOpen = null,
                        redirectDraft = "",
                        connectingBankName = null
                    )
                }

            is BankSyncSettingsIntent.ShowAccountPicker ->
                _state.update { it.copy(accountPickerForUid = intent.uid) }

            is BankSyncSettingsIntent.SetLocalAccountMapping -> setMapping(
                intent.uid,
                intent.localAccountId
            )

            is BankSyncSettingsIntent.SetAccountEnabled -> setEnabled(intent.uid, intent.enabled)

            BankSyncSettingsIntent.ToggleAutoSync -> toggleAutoSync()

            BankSyncSettingsIntent.SyncNow -> syncNow()

            BankSyncSettingsIntent.Disconnect -> disconnect()
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

    private fun saveCredentials() {
        val appId = _state.value.appIdDraft.trim()
        val pem = _state.value.pemDraft.trim()
        if (appId.isEmpty() || pem.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isValidatingCredentials = true, credentialsError = null) }
            val credentials = EbCredentials(applicationId = appId, privateKeyPem = pem)
            client.validateCredentials(credentials).fold(
                onSuccess = {
                    credentialsStore.saveCredentials(credentials)
                    appSettings.putBoolean(PrefKeys.BANK_SYNC_CONFIGURED, true)
                    _state.update {
                        it.copy(
                            isValidatingCredentials = false,
                            configured = true,
                            appIdDraft = "",
                            pemDraft = ""
                        )
                    }
                },
                onFailure = { t ->
                    Logger.e("Save credentials failed", t)
                    _state.update {
                        it.copy(isValidatingCredentials = false, credentialsError = t.message)
                    }
                },
            )
        }
    }

    private fun loadBanks() {
        val country = _state.value.countryDraft.trim().uppercase()
        if (country.length != 2) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingBanks = true, connectError = null) }
            client.listBanks(country).fold(
                onSuccess = { banks ->
                    _state.update { s ->
                        s.copy(
                            isLoadingBanks = false,
                            banks = banks.map { BankRow(name = it.name, country = it.country) },
                        )
                    }
                },
                onFailure = { t ->
                    Logger.e("Load banks", t)
                    _state.update { it.copy(isLoadingBanks = false, connectError = t.message) }
                },
            )
        }
    }

    private fun startConnect(name: String, country: String) {
        viewModelScope.launch {
            _state.update { it.copy(connectError = null) }
            connectBank(bankName = name, country = country).fold(
                onSuccess = { auth ->
                    _state.update {
                        it.copy(
                            authUrlToOpen = auth.url,
                            awaitingAuth = true,
                            connectingBankName = name,
                            redirectDraft = "",
                        )
                    }
                },
                onFailure = { t ->
                    Logger.e("Connect failed", t)
                    _state.update { it.copy(connectError = t.message) }
                },
            )
        }
    }

    private fun submitRedirect(input: String) {
        if (!_state.value.awaitingAuth) return
        val code = parseRedirectCode(input)
        if (code == null) {
            _state.update { it.copy(redirectInvalid = true) }
            return
        }
        val bankName = _state.value.connectingBankName.orEmpty()
        viewModelScope.launch {
            _state.update { it.copy(isCompletingConnection = true, connectError = null) }
            completeConnection(code = code, bankName = bankName).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isCompletingConnection = false,
                            awaitingAuth = false,
                            authUrlToOpen = null,
                            redirectDraft = "",
                            connectingBankName = null,
                            banks = emptyList(),
                            bankSearch = "",
                        )
                    }
                    refreshConnectionState()
                },
                onFailure = { t ->
                    Logger.e("Submit redirect failed", t)
                    _state.update {
                        it.copy(
                            isCompletingConnection = false,
                            connectError = t.message
                        )
                    }
                },
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
                BankSyncSettingsUiState(
                    isLoading = false,
                    localAccounts = it.localAccounts,
                )
            }
        }
    }
}
