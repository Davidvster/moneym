package com.dv.moneym

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.dv.moneym.core.oauth.GoogleAuthActivityBridge
import com.dv.moneym.core.security.BiometricAuthenticatorImpl
import com.dv.moneym.data.banksync.BankAuthCallbackBus
import org.koin.core.context.GlobalContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        BiometricAuthenticatorImpl.activityRef = this
        GoogleAuthActivityBridge.register(this)
        ScreenshotSecurity.bind(this)
        setContent {
            App()
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val url = intent?.dataString ?: return
        if (!url.startsWith(BANK_CALLBACK_PREFIX)) return
        runCatching { GlobalContext.get().get<BankAuthCallbackBus>().emit(url) }
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenshotSecurity.unbind()
        BiometricAuthenticatorImpl.activityRef = null
        GoogleAuthActivityBridge.unregister()
    }

    private companion object {
        const val BANK_CALLBACK_PREFIX = "moneym://bank-callback"
    }
}
