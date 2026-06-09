package com.dv.moneym.feature.settings.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecuritySettingsViewModel(
    private val settings: AppSettings,
    private val pinManager: PinManager,
    private val biometricAuth: BiometricAuthenticator,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved {
        MutableStateFlow(
            SecuritySettingsUiState(
                pinEnabled = settings.getBoolean(SecurityPrefs.PIN_ENABLED),
                biometricEnabled = settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED),
                biometricAvailable = biometricAuth.isAvailable,
                backgroundLockSeconds = settings.getInt(
                    SecurityPrefs.BACKGROUND_LOCK_SECONDS,
                    SecurityPrefs.DEFAULT_LOCK_SECONDS
                ),
                allowScreenshots = settings.getBoolean(SecurityPrefs.ALLOW_SCREENSHOTS),
            )
        )
    }

    val state: StateFlow<SecuritySettingsUiState> = _state
        .onStart { }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<SecuritySettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: SecuritySettingsIntent) {
        when (intent) {
            is SecuritySettingsIntent.PinToggled -> {
                if (intent.enable) {
                    _state.update { it.copy(pinEnabled = true) }
                    viewModelScope.launch { _effects.send(SecuritySettingsEffect.NavigateToPinSetup) }
                } else {
                    viewModelScope.launch {
                        withContext(dispatchers.io) { pinManager.clearPin() }
                        _state.update { it.copy(pinEnabled = false, biometricEnabled = false) }
                    }
                }
            }

            is SecuritySettingsIntent.BiometricToggled -> {
                settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, intent.enable)
                _state.update { it.copy(biometricEnabled = intent.enable) }
            }

            is SecuritySettingsIntent.LockTimeoutChanged -> {
                settings.putInt(SecurityPrefs.BACKGROUND_LOCK_SECONDS, intent.seconds)
                _state.update { it.copy(backgroundLockSeconds = intent.seconds) }
            }

            is SecuritySettingsIntent.ScreenshotsToggled -> {
                settings.putBoolean(SecurityPrefs.ALLOW_SCREENSHOTS, intent.enable)
                _state.update { it.copy(allowScreenshots = intent.enable) }
            }

            SecuritySettingsIntent.ChangePinRequested ->
                viewModelScope.launch { _effects.send(SecuritySettingsEffect.NavigateToPinSetup) }

            SecuritySettingsIntent.RefreshPinState -> refreshPinState()
        }
    }

    private fun refreshPinState() {
        _state.update {
            it.copy(
                pinEnabled = settings.getBoolean(SecurityPrefs.PIN_ENABLED),
                biometricEnabled = settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED),
            )
        }
    }
}
