package com.dv.moneym.feature.security.presentation

data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER_FIRST,
    val firstPin: String = "",
    val secondPin: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
)

enum class PinSetupStep { ENTER_FIRST, CONFIRM }

sealed interface PinSetupEffect {
    data object Done : PinSetupEffect
}

sealed interface PinSetupIntent {
    data class DigitPressed(val digit: Int) : PinSetupIntent
    data object DeletePressed : PinSetupIntent
}
