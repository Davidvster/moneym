package com.dv.moneym

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.dv.moneym.core.oauth.GoogleAuthActivityBridge
import com.dv.moneym.core.security.BiometricAuthenticatorImpl

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
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenshotSecurity.unbind()
        BiometricAuthenticatorImpl.activityRef = null
        GoogleAuthActivityBridge.unregister()
    }
}
