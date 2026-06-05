package com.dv.moneym.data.sync

import kotlinx.serialization.Serializable

/**
 * A surfaced encryption/password mismatch between this device and the remote sync state. The
 * remote file is self-describing (encrypted envelope vs plaintext JSON); when this device cannot
 * reconcile its own configuration with the remote's, sync pauses and a conflict is raised so the
 * user can resolve it — mirroring how pending deletions are surfaced rather than applied silently.
 *
 * @param remoteEncrypted what the remote sync state currently is.
 *   - `true`  → remote is encrypted but this device can't decrypt it (no / wrong password).
 *   - `false` → remote is plaintext but this device is configured for encryption (a downgrade).
 */
@Serializable
data class SyncConflict(
    val remoteEncrypted: Boolean,
)
