package com.dv.moneym.feature.security.presentation

data class PinUnlockUiState(
    val pin: String = "",
    val error: String? = null,
    val failedAttempts: Int = 0,
    val backoffRemainingMs: Long = 0L,
    val biometricAvailable: Boolean = false,
    val isVerifying: Boolean = false,
)

sealed interface PinUnlockEffect {
    data object Unlocked : PinUnlockEffect
}

sealed interface PinUnlockIntent {
    data class DigitPressed(val digit: Int) : PinUnlockIntent
    data object DeletePressed : PinUnlockIntent
    data object BiometricRequested : PinUnlockIntent
}
