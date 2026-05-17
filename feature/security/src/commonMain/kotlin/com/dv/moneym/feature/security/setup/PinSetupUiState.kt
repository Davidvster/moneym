package com.dv.moneym.feature.security.setup

import com.dv.moneym.core.security.BiometryType
import kotlinx.serialization.Serializable

@Serializable
internal data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER_FIRST,
    val firstPin: String = "",
    val secondPin: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    val biometryType: BiometryType = BiometryType.None,
)

internal enum class PinSetupStep { ENTER_FIRST, CONFIRM }

internal sealed interface PinSetupEffect {
    data object Done : PinSetupEffect
    data object OfferBiometrics : PinSetupEffect
}

internal sealed interface PinSetupIntent {
    data class DigitPressed(val digit: Int) : PinSetupIntent
    data object DeletePressed : PinSetupIntent
    data object BiometricOfferAccepted : PinSetupIntent
    data object BiometricOfferDeclined : PinSetupIntent
}
