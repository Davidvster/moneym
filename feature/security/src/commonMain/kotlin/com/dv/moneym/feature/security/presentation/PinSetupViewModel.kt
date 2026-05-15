package com.dv.moneym.feature.security.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.security.PinManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PIN_LENGTH = 4

class PinSetupViewModel(
    private val pinManager: PinManager,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(PinSetupUiState())
    val state: StateFlow<PinSetupUiState> = _state.asStateFlow()

    private val _effects = Channel<PinSetupEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: PinSetupIntent) {
        when (intent) {
            is PinSetupIntent.DigitPressed -> onDigit(intent.digit)
            PinSetupIntent.DeletePressed -> onDelete()
        }
    }

    private fun onDigit(digit: Int) {
        val s = _state.value
        when (s.step) {
            PinSetupStep.ENTER_FIRST -> {
                val updated = s.firstPin + digit
                if (updated.length == PIN_LENGTH) {
                    _state.update { it.copy(firstPin = updated, step = PinSetupStep.CONFIRM, error = null) }
                } else {
                    _state.update { it.copy(firstPin = updated) }
                }
            }
            PinSetupStep.CONFIRM -> {
                val updated = s.secondPin + digit
                _state.update { it.copy(secondPin = updated) }
                if (updated.length == PIN_LENGTH) confirmPin(s.firstPin, updated)
            }
        }
    }

    private fun onDelete() {
        val s = _state.value
        when (s.step) {
            PinSetupStep.ENTER_FIRST -> _state.update { it.copy(firstPin = it.firstPin.dropLast(1)) }
            PinSetupStep.CONFIRM -> {
                if (s.secondPin.isEmpty()) {
                    _state.update { it.copy(step = PinSetupStep.ENTER_FIRST, secondPin = "") }
                } else {
                    _state.update { it.copy(secondPin = it.secondPin.dropLast(1)) }
                }
            }
        }
    }

    private fun confirmPin(first: String, second: String) {
        if (first != second) {
            _state.update { it.copy(step = PinSetupStep.ENTER_FIRST, firstPin = "", secondPin = "", error = "PINs don't match") }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            withContext(dispatchers.io) { pinManager.setPin(first) }
            _effects.send(PinSetupEffect.Done)
        }
    }

    fun reset() {
        _state.value = PinSetupUiState()
        while (_effects.tryReceive().isSuccess) {}
    }
}
