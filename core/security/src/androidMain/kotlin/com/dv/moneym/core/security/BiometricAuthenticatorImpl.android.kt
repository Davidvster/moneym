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
            return mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }

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
                .setNegativeButtonText("Use PIN")
                .build()
            prompt.authenticate(info)
        }
    }

    companion object {
        var activityRef: FragmentActivity? = null
    }
}
