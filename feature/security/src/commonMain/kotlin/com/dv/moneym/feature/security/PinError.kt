package com.dv.moneym.feature.security

import kotlinx.serialization.Serializable

@Serializable
sealed interface PinError {
    @Serializable
    data object IncorrectPin : PinError

    @Serializable
    data object PinsMismatch : PinError
}
