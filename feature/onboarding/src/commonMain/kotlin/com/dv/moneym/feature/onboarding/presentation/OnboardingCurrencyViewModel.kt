package com.dv.moneym.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingCurrencyViewModel(
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingCurrencyUiState())
    val state: StateFlow<OnboardingCurrencyUiState> = _state.asStateFlow()

    private val _effects = Channel<OnboardingCurrencyEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: OnboardingCurrencyIntent) {
        when (intent) {
            is OnboardingCurrencyIntent.CurrencySelected ->
                _state.update { it.copy(selectedCurrency = intent.code) }
            is OnboardingCurrencyIntent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = intent.query) }
            OnboardingCurrencyIntent.Continue -> {
                settings.putString(PrefKeys.DEFAULT_CURRENCY, _state.value.selectedCurrency)
                viewModelScope.launch { _effects.send(OnboardingCurrencyEffect.NavigateToSecurity) }
            }
        }
    }
}
