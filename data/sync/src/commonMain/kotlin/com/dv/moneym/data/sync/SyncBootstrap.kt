package com.dv.moneym.data.sync

enum class RemoteSyncState { NONE, ENCRYPTED, PLAINTEXT }

/**
 * Lets the enable/join UI inspect the remote sync state before turning cloud sync on: whether a
 * remote already exists (this device is joining vs. seeding) and whether a candidate password can
 * decrypt it. [SyncEngine] implements it. Mirrors the narrow-interface pattern of
 * [SyncStatusProvider] / [SyncPuller] so the ViewModel stays testable.
 */
interface SyncBootstrap {
    suspend fun remoteState(): RemoteSyncState
    suspend fun canDecrypt(passphrase: CharArray): Boolean
}
