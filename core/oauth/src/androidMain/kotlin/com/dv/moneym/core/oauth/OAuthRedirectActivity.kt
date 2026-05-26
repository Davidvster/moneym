package com.dv.moneym.core.oauth

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class OAuthRedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        AndroidAuthorizationLauncher.deliverRedirect(data)
    }
}
