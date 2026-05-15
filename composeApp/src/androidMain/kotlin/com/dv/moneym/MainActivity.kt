package com.dv.moneym

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.dv.moneym.core.security.BiometricAuthenticatorImpl
import com.dv.moneym.di.androidPlatformModule

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        BiometricAuthenticatorImpl.activityRef = this
        setContent {
            App(platformModules = listOf(androidPlatformModule(applicationContext)))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BiometricAuthenticatorImpl.activityRef = null
    }
}
