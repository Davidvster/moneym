package com.dv.moneym.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LABiometryTypeFaceID
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class BiometricAuthenticatorImpl actual constructor() : BiometricAuthenticator {

    override val isAvailable: Boolean
        get() = LAContext().canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null,
        )

    override val biometryType: BiometryType
        get() {
            if (!isAvailable) return BiometryType.None
            val ctx = LAContext()
            ctx.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
            return if (ctx.biometryType == LABiometryTypeFaceID) BiometryType.FaceId
            else BiometryType.Fingerprint
        }

    override suspend fun authenticate(reason: String): BiometricResult =
        suspendCancellableCoroutine { cont ->
            LAContext().evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = reason,
            ) { success, _ ->
                if (cont.isActive) {
                    cont.resume(if (success) BiometricResult.Success else BiometricResult.UserCancelled)
                }
            }
        }
}
