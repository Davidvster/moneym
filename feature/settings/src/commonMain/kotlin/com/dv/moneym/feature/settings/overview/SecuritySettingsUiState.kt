package com.dv.moneym.feature.settings.overview

import com.dv.moneym.core.security.SecurityPrefs
import kotlinx.serialization.Serializable

@Serializable
data class SecuritySettingsUiState(
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val backgroundLockSeconds: Int = SecurityPrefs.DEFAULT_LOCK_SECONDS,
)

sealed interface SecuritySettingsIntent {
    data class PinToggled(val enable: Boolean) : SecuritySettingsIntent
    data class BiometricToggled(val enable: Boolean) : SecuritySettingsIntent
    data class LockTimeoutChanged(val seconds: Int) : SecuritySettingsIntent
    data object ChangePinRequested : SecuritySettingsIntent
}

sealed interface SecuritySettingsEffect {
    data object NavigateToPinSetup : SecuritySettingsEffect
}
