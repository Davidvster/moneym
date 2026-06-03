package com.dv.moneym.feature.settings

import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricResult
import com.dv.moneym.core.security.BiometryType

class FakeBiometricAuthenticator(
    override val isAvailable: Boolean = true,
    override val biometryType: BiometryType = BiometryType.Fingerprint,
    private val result: BiometricResult = BiometricResult.Success,
) : BiometricAuthenticator {
    override suspend fun authenticate(reason: String): BiometricResult = result
}
