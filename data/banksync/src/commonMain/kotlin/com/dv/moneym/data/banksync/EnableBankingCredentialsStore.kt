package com.dv.moneym.data.banksync

import com.dv.moneym.core.security.SecureStore
import com.dv.moneym.core.security.SecurityKeys

class EnableBankingCredentialsStore(
    private val secureStore: SecureStore,
) {
    suspend fun saveCredentials(credentials: EbCredentials) {
        secureStore.put(SecurityKeys.BANK_SYNC_APP_ID, credentials.applicationId.encodeToByteArray())
        secureStore.put(SecurityKeys.BANK_SYNC_PRIVATE_KEY_PEM, credentials.privateKeyPem.encodeToByteArray())
    }

    suspend fun loadCredentials(): EbCredentials? {
        val appId = secureStore.get(SecurityKeys.BANK_SYNC_APP_ID)?.decodeToString() ?: return null
        val pem = secureStore.get(SecurityKeys.BANK_SYNC_PRIVATE_KEY_PEM)?.decodeToString() ?: return null
        return EbCredentials(applicationId = appId, privateKeyPem = pem)
    }

    suspend fun saveSessionId(sessionId: String) {
        secureStore.put(SecurityKeys.BANK_SYNC_SESSION_ID, sessionId.encodeToByteArray())
    }

    suspend fun loadSessionId(): String? =
        secureStore.get(SecurityKeys.BANK_SYNC_SESSION_ID)?.decodeToString()

    suspend fun clearSession() {
        secureStore.remove(SecurityKeys.BANK_SYNC_SESSION_ID)
    }

    suspend fun clearAll() {
        secureStore.remove(SecurityKeys.BANK_SYNC_APP_ID)
        secureStore.remove(SecurityKeys.BANK_SYNC_PRIVATE_KEY_PEM)
        secureStore.remove(SecurityKeys.BANK_SYNC_SESSION_ID)
    }
}
