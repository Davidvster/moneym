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

class OnboardingSecurityViewModel(
    private val settings: AppSettings,
    private val pinManager: PinManager,
    private val biometricAuth: BiometricAuthenticator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingSecurityUiState(biometricAvailable = biometricAuth.isAvailable)
    )
    val state: StateFlow<OnboardingSecurityUiState> = _state.asStateFlow()

    private val _effects = Channel<OnboardingSecurityEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onReturnFromPinSetup() {
        viewModelScope.launch {
            val pinSet = pinManager.isPinSet()
            _state.update { it.copy(pinEnabled = pinSet) }
        }
    }

    fun onIntent(intent: OnboardingSecurityIntent) {
        when (intent) {
            OnboardingSecurityIntent.SetupPinRequested ->
                viewModelScope.launch { _effects.send(OnboardingSecurityEffect.NavigateToPinSetup) }
            is OnboardingSecurityIntent.BiometricToggled -> {
                settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, intent.enabled)
                _state.update { it.copy(biometricEnabled = intent.enabled) }
            }
            OnboardingSecurityIntent.Finish -> {
                settings.putBoolean(SecurityPrefs.PIN_ENABLED, _state.value.pinEnabled)
                settings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)
                viewModelScope.launch { _effects.send(OnboardingSecurityEffect.Complete) }
            }
        }
    }
}
