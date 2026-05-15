package com.dv.moneym.core.security

interface BiometricAuthenticator {
    val isAvailable: Boolean
    suspend fun authenticate(reason: String): BiometricResult
}

sealed interface BiometricResult {
    data object Success : BiometricResult
    data object UserCancelled : BiometricResult
    data class Error(val message: String) : BiometricResult
}

expect class BiometricAuthenticatorImpl() : BiometricAuthenticator
