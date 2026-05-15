package com.dv.moneym.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64

class AndroidSecureStore(context: Context) : SecureStore {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "moneym_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) {
        prefs.edit().putString(key, Base64.encodeToString(value, Base64.NO_WRAP)).apply()
    }

    override suspend fun get(key: String): ByteArray? {
        val encoded = prefs.getString(key, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    override suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
