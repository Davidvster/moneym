package com.dv.moneym.feature.security.unlock

import com.dv.moneym.core.security.BiometryType
import kotlinx.serialization.Serializable

@Serializable
internal data class PinUnlockUiState(
    val pin: String = "",
    val error: String? = null,
    val failedAttempts: Int = 0,
    val backoffRemainingMs: Long = 0L,
    val biometricAvailable: Boolean = false,
    val biometryType: BiometryType = BiometryType.Fingerprint,
    val isVerifying: Boolean = false,
)

internal sealed interface PinUnlockEffect {
    data object Unlocked : PinUnlockEffect
}

internal sealed interface PinUnlockIntent {
    data class DigitPressed(val digit: Int) : PinUnlockIntent
    data object DeletePressed : PinUnlockIntent
    data object BiometricRequested : PinUnlockIntent
}
