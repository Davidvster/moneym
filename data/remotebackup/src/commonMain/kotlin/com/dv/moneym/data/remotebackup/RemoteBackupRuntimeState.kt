package com.dv.moneym.data.remotebackup

sealed interface RemoteBackupRuntimeState {
    data object Idle : RemoteBackupRuntimeState
    data object Encrypting : RemoteBackupRuntimeState
    data object Uploading : RemoteBackupRuntimeState
    data object Downloading : RemoteBackupRuntimeState
    data object Decrypting : RemoteBackupRuntimeState
    data object Restoring : RemoteBackupRuntimeState
    data class Error(val message: String) : RemoteBackupRuntimeState
    data class QuotaWarning(val remainingBytes: Long, val requiredBytes: Long) : RemoteBackupRuntimeState
}
