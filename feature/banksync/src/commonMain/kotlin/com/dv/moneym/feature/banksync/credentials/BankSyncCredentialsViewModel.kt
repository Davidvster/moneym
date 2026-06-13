package com.dv.moneym.feature.banksync.credentials

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.banksync.EbCredentials
import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BankSyncCredentialsViewModel(
    private val credentialsStore: EnableBankingCredentialsStore,
    private val client: EnableBankingClient,
    private val appSettings: AppSettings,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface BankSyncCredentialsSingleUiEvent {
        data class Saved(val continueToBankPicker: Boolean) : BankSyncCredentialsSingleUiEvent
    }

    private val _singleEvent = Channel<BankSyncCredentialsSingleUiEvent>(Channel.BUFFERED)
    val singleEvents = _singleEvent.receiveAsFlow()

    private val _state by savedStateHandle.saved { MutableStateFlow(BankSyncCredentialsUiState()) }
    internal val state: StateFlow<BankSyncCredentialsUiState> = _state.asStateFlow()

    fun onIntent(intent: BankSyncCredentialsIntent) {
        when (intent) {
            is BankSyncCredentialsIntent.AppIdChanged ->
                _state.update { it.copy(appIdDraft = intent.value, credentialsError = null) }

            is BankSyncCredentialsIntent.PemChanged ->
                _state.update { it.copy(pemDraft = intent.value, credentialsError = null) }

            BankSyncCredentialsIntent.SaveCredentials -> saveCredentials()
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
                    val hasSession = credentialsStore.loadSessionId() != null
                    _state.update {
                        it.copy(
                            isValidatingCredentials = false,
                            appIdDraft = "",
                            pemDraft = ""
                        )
                    }
                    _singleEvent.send(
                        BankSyncCredentialsSingleUiEvent.Saved(continueToBankPicker = !hasSession)
                    )
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
}
