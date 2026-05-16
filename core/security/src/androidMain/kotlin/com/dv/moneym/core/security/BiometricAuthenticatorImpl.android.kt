package com.dv.moneym.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class BiometricAuthenticatorImpl actual constructor() : BiometricAuthenticator {

    override val isAvailable: Boolean
        get() {
            val activity = activityRef ?: return false
            val mgr = BiometricManager.from(activity)
            val allowedAuth = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            return mgr.canAuthenticate(allowedAuth) == BiometricManager.BIOMETRIC_SUCCESS
        }

    // Android does not easily distinguish Face vs Fingerprint without API 30+;
    // return Fingerprint as a safe default for all Android biometrics.
    override val biometryType: BiometryType
        get() = if (isAvailable) BiometryType.Fingerprint else BiometryType.None

    override suspend fun authenticate(reason: String): BiometricResult {
        val activity = activityRef ?: return BiometricResult.Error("No activity")
        return suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(BiometricResult.Success)
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    if (cont.isActive) cont.resume(
                        if (code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                            BiometricResult.UserCancelled
                        else BiometricResult.Error(msg.toString())
                    )
                }
                override fun onAuthenticationFailed() {
                    // individual failure — don't resolve, let user retry
                }
            }
            val prompt = BiometricPrompt(activity, executor, callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("MoneyM")
                .setSubtitle(reason)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .setNegativeButtonText("Use PIN")
                .build()
            prompt.authenticate(info)
        }
    }

    companion object {
        var activityRef: FragmentActivity? = null
    }
}
