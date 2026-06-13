package com.dv.moneym.core.oauth

interface GoogleSignInBridge {
    fun signIn(scopes: List<String>, nonce: String, onResult: (email: String?, error: String?) -> Unit)
    fun currentAccessToken(onResult: (token: String?, error: String?) -> Unit)
    fun restore(onResult: (email: String?) -> Unit)
    fun signOut()
}

object GoogleSignInBridgeHolder {
    var instance: GoogleSignInBridge? = null
}
