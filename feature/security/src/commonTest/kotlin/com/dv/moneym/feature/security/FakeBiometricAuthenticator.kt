package com.dv.moneym.feature.security

import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricResult
import com.dv.moneym.core.security.BiometryType

class FakeBiometricAuthenticator(
    override var isAvailable: Boolean = false,
    override var biometryType: BiometryType = BiometryType.Fingerprint,
    var result: BiometricResult = BiometricResult.Success,
) : BiometricAuthenticator {
    var authenticateCount = 0
        private set

    override suspend fun authenticate(reason: String): BiometricResult {
        authenticateCount++
        return result
    }
}
