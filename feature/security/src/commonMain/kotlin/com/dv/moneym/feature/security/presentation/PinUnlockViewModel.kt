package com.dv.moneym.feature.security.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricResult
import com.dv.moneym.core.security.BiometryType
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PIN_LENGTH = 4

class PinUnlockViewModel(
    private val pinManager: PinManager,
    private val biometricAuth: BiometricAuthenticator,
    private val settings: AppSettings,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved {
        MutableStateFlow(PinUnlockUiState())
    }

    internal val state: StateFlow<PinUnlockUiState> = _state
        .onStart { init() }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<PinUnlockEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    private fun init() {
        val biometricEnabled = settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED)
        val biometricAvailable = biometricEnabled && biometricAuth.isAvailable
        _state.update {
            it.copy(
                failedAttempts = pinManager.failedAttempts(),
                backoffRemainingMs = pinManager.backoffRemainingMs(),
                biometricAvailable = biometricAvailable,
                biometryType = if (biometricAvailable) biometricAuth.biometryType else BiometryType.None,
            )
        }
        startBackoffTimer()

        // Auto-trigger biometrics on unlock screen open if available
        if (biometricAvailable) {
            viewModelScope.launch {
                // Small delay to allow the UI to render first
                triggerBiometric()
            }
        }
    }

    internal fun onIntent(intent: PinUnlockIntent) {
        when (intent) {
            is PinUnlockIntent.DigitPressed -> onDigit(intent.digit)
            PinUnlockIntent.DeletePressed -> _state.update { it.copy(pin = it.pin.dropLast(1)) }
            PinUnlockIntent.BiometricRequested -> triggerBiometric()
        }
    }

    private fun onDigit(digit: Int) {
        if (_state.value.backoffRemainingMs > 0) return
        val updated = _state.value.pin + digit
        _state.update { it.copy(pin = updated, error = null) }
        if (updated.length == PIN_LENGTH) verifyPin(updated)
    }

    private fun verifyPin(pin: String) {
        _state.update { it.copy(isVerifying = true) }
        viewModelScope.launch {
            val correct = withContext(dispatchers.io) { pinManager.verifyPin(pin) }
            if (correct) {
                pinManager.resetAttempts()
                _state.update { it.copy(isVerifying = false, pin = "") }
                _effects.send(PinUnlockEffect.Unlocked)
            } else {
                pinManager.recordFailedAttempt()
                val remaining = pinManager.backoffRemainingMs()
                _state.update {
                    it.copy(
                        pin = "",
                        isVerifying = false,
                        error = "Incorrect PIN",
                        failedAttempts = pinManager.failedAttempts(),
                        backoffRemainingMs = remaining,
                    )
                }
                if (remaining > 0) startBackoffTimer()
            }
        }
    }

    private fun triggerBiometric() {
        viewModelScope.launch {
            val result = biometricAuth.authenticate("Unlock MoneyM")
            when (result) {
                BiometricResult.Success -> {
                    pinManager.resetAttempts()
                    _effects.send(PinUnlockEffect.Unlocked)
                }

                BiometricResult.KeyInvalidated -> {
                    // Biometrics were changed on the device — disable biometrics and fall back to PIN
                    settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, false)
                    _state.update {
                        it.copy(
                            biometricAvailable = false,
                            biometryType = BiometryType.None,
                        )
                    }
                }

                BiometricResult.UserCancelled,
                is BiometricResult.Error -> {
                    // Do nothing — user can still use PIN
                }
            }
        }
    }

    private fun startBackoffTimer() {
        viewModelScope.launch {
            while (true) {
                val remaining = pinManager.backoffRemainingMs()
                _state.update { it.copy(backoffRemainingMs = remaining) }
                if (remaining <= 0) break
                delay(1_000)
            }
        }
    }
}
