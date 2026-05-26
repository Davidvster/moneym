package com.dv.moneym.core.oauth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dv.moneym.core.common.DispatcherProvider
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AndroidSecureTokenStore(
    context: Context,
    private val dispatchers: DispatcherProvider,
    private val json: Json = DEFAULT_JSON,
) : SecureTokenStore {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun read(): StoredTokens? = withContext(dispatchers.io) {
        val raw = prefs.getString(KEY_TOKENS, null) ?: return@withContext null
        runCatching { json.decodeFromString(StoredTokens.serializer(), raw) }.getOrNull()
    }

    override suspend fun write(tokens: StoredTokens) = withContext(dispatchers.io) {
        val raw = json.encodeToString(StoredTokens.serializer(), tokens)
        prefs.edit().putString(KEY_TOKENS, raw).apply()
    }

    override suspend fun clear() = withContext(dispatchers.io) {
        prefs.edit().remove(KEY_TOKENS).apply()
    }

    companion object {
        private const val FILE_NAME = "moneym_oauth_tokens"
        private const val KEY_TOKENS = "google"
        private val DEFAULT_JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
