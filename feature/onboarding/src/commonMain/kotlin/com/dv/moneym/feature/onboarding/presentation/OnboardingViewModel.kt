package com.dv.moneym.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settings: AppSettings,
    private val pinManager: PinManager,
    private val biometricAuth: BiometricAuthenticator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingUiState(
            biometricAvailable = biometricAuth.isAvailable,
        )
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.CurrencySelected ->
                _state.update { it.copy(selectedCurrency = intent.code) }
            OnboardingIntent.ContinueToSecurity ->
                _state.update { it.copy(step = OnboardingStep.SECURITY) }
            OnboardingIntent.SetupPinRequested ->
                viewModelScope.launch { _effects.send(OnboardingEffect.NavigateToPinSetup) }
            OnboardingIntent.SkipSecurity -> finish()
            OnboardingIntent.Finish -> finish()
            is OnboardingIntent.BiometricToggled -> {
                settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, intent.enabled)
                _state.update { it.copy(biometricEnabled = intent.enabled) }
            }
        }
    }

    fun onReturnFromPinSetup() {
        viewModelScope.launch {
            val pinSet = pinManager.isPinSet()
            _state.update { it.copy(pinEnabled = pinSet) }
        }
    }

    private fun finish() {
        val currency = _state.value.selectedCurrency
        settings.putString(PrefKeys.DEFAULT_CURRENCY, currency)
        settings.putBoolean(SecurityPrefs.PIN_ENABLED, _state.value.pinEnabled)
        settings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)
        viewModelScope.launch { _effects.send(OnboardingEffect.Complete) }
    }
}
