package com.dv.moneym.feature.banksync.bankpicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.dv.moneym.data.banksync.BankAuthCallbackBus
import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.feature.banksync.usecase.CompleteConnectionUseCase
import com.dv.moneym.feature.banksync.usecase.ConnectBankUseCase
import com.dv.moneym.feature.banksync.usecase.ParseRedirectCodeUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BankPickerViewModel(
    private val client: EnableBankingClient,
    private val connectBank: ConnectBankUseCase,
    private val completeConnection: CompleteConnectionUseCase,
    private val parseRedirectCode: ParseRedirectCodeUseCase,
    callbackBus: BankAuthCallbackBus,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface BankPickerSingleUiEvent {
        data object Connected : BankPickerSingleUiEvent
    }

    private val _singleEvent = Channel<BankPickerSingleUiEvent>(Channel.BUFFERED)
    val singleEvents = _singleEvent.receiveAsFlow()

    private val _state by savedStateHandle.saved { MutableStateFlow(BankPickerUiState()) }
    internal val state: StateFlow<BankPickerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            callbackBus.callbacks.collect { url ->
                if (_state.value.awaitingAuth) submitRedirect(url)
            }
        }
    }

    fun onIntent(intent: BankPickerIntent) {
        when (intent) {
            is BankPickerIntent.CountrySearchChanged ->
                _state.update { it.copy(countrySearch = intent.value) }

            is BankPickerIntent.CountrySelected -> selectCountry(intent.code)

            BankPickerIntent.ChangeCountry ->
                _state.update {
                    it.copy(
                        selectedCountry = null,
                        banks = emptyList(),
                        bankSearch = "",
                        connectError = null,
                    )
                }

            is BankPickerIntent.BankSearchChanged ->
                _state.update { it.copy(bankSearch = intent.value) }

            is BankPickerIntent.ConnectBank -> startConnect(intent.name, intent.country)

            BankPickerIntent.AuthUrlOpened -> _state.update { it.copy(authUrlToOpen = null) }

            is BankPickerIntent.RedirectChanged ->
                _state.update {
                    it.copy(
                        redirectDraft = intent.value,
                        connectError = null,
                        redirectInvalid = false
                    )
                }

            BankPickerIntent.SubmitRedirect -> submitRedirect(_state.value.redirectDraft)

            BankPickerIntent.CancelAuth ->
                _state.update {
                    it.copy(
                        awaitingAuth = false,
                        authUrlToOpen = null,
                        redirectDraft = "",
                        connectingBankName = null
                    )
                }
        }
    }

    private fun selectCountry(code: String) {
        val country = code.trim().uppercase()
        if (country.length != 2) return
        viewModelScope.launch {
            _state.update {
                it.copy(selectedCountry = country, isLoadingBanks = true, connectError = null)
            }
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
                        )
                    }
                    _singleEvent.send(BankPickerSingleUiEvent.Connected)
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
}
