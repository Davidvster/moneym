package com.dv.moneym.data.sync

/**
 * Resolution actions for a password/encryption [SyncConflict]. [SyncEngine] implements it; the UI
 * depends on this narrow surface. Mirrors [SyncDeletionController].
 */
interface SyncConflictController {
    /**
     * Apply [passphrase] to the remote sync state.
     * @param makeAuthoritative `false` = "I know the other device's password" — validated by
     *   decrypting the current remote; on mismatch the conflict stays and the result fails.
     *   `true` = "override" — re-encrypt the remote with [passphrase] (other devices then hit
     *   [SyncConflict] and must re-enter), matching the deletion-confirm override model.
     */
    suspend fun resolveWithPassword(passphrase: CharArray, makeAuthoritative: Boolean): Result<Unit>

    /** Drop encryption: clear the stored password and (re)write the remote as plaintext. */
    suspend fun resolveWithPlaintext(): Result<Unit>
}
