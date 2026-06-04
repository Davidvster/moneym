package com.dv.moneym.data.remotebackup

import com.dv.moneym.core.security.SecureStore

/**
 * Durable persistence for the cloud-sync/backup passphrase, backed by the platform [SecureStore]
 * (iOS Keychain / Android EncryptedSharedPreferences). [SessionPassphrase] is memory-only and is
 * empty after every app restart; this store survives restarts so sync can decrypt the remote
 * snapshot on boot without forcing the user to re-type the password each session.
 */
class SyncPassphraseStore(
    private val secureStore: SecureStore,
) {
    suspend fun persist(passphrase: CharArray) {
        secureStore.put(KEY, passphrase.concatToString().encodeToByteArray())
    }

    suspend fun load(): CharArray? =
        secureStore.get(KEY)?.decodeToString()?.toCharArray()

    suspend fun clear() {
        secureStore.remove(KEY)
    }

    /** Load the persisted passphrase (if any) into [session] so it is available before the first pull. */
    suspend fun hydrate(session: SessionPassphrase) {
        load()?.let { session.set(it) }
    }

    private companion object {
        const val KEY = "sync.passphrase"
    }
}
