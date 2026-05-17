package com.dv.moneym.core.security

interface BiometricAuthenticator {
    val isAvailable: Boolean
    val biometryType: BiometryType
    suspend fun authenticate(reason: String): BiometricResult
}

enum class BiometryType { Fingerprint, FaceId, None }

sealed interface BiometricResult {
    data object Success : BiometricResult
    data object UserCancelled : BiometricResult
    /** Returned when the user has changed biometrics on the device and the old binding is no longer valid. */
    data object KeyInvalidated : BiometricResult
    data class Error(val message: String) : BiometricResult
}

expect class BiometricAuthenticatorImpl() : BiometricAuthenticator
