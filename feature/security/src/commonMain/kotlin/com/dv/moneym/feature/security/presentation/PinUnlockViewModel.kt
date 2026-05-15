package com.dv.moneym.feature.security.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricResult
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PIN_LENGTH = 4

class PinUnlockViewModel(
    private val pinManager: PinManager,
    private val biometricAuth: BiometricAuthenticator,
    private val settings: AppSettings,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(PinUnlockUiState())
    val state: StateFlow<PinUnlockUiState> = _state.asStateFlow()

    private val _effects = Channel<PinUnlockEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        val biometricEnabled = settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED)
        _state.update {
            it.copy(
                failedAttempts = pinManager.failedAttempts(),
                backoffRemainingMs = pinManager.backoffRemainingMs(),
                biometricAvailable = biometricEnabled && biometricAuth.isAvailable,
            )
        }
        startBackoffTimer()
    }

    fun onIntent(intent: PinUnlockIntent) {
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
            if (result == BiometricResult.Success) {
                pinManager.resetAttempts()
                _effects.send(PinUnlockEffect.Unlocked)
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
