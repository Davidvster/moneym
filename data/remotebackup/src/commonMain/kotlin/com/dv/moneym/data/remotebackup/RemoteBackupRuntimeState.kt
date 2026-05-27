package com.dv.moneym.data.remotebackup

import kotlinx.serialization.Serializable

@Serializable
sealed interface RemoteBackupRuntimeState {
    @Serializable
    data object Idle : RemoteBackupRuntimeState
    @Serializable
    data object Encrypting : RemoteBackupRuntimeState
    @Serializable
    data object Uploading : RemoteBackupRuntimeState
    @Serializable
    data object Downloading : RemoteBackupRuntimeState
    @Serializable
    data object Decrypting : RemoteBackupRuntimeState
    @Serializable
    data object Restoring : RemoteBackupRuntimeState
    @Serializable
    data class Error(val message: String) : RemoteBackupRuntimeState
    @Serializable
    data class QuotaWarning(val remainingBytes: Long, val requiredBytes: Long) : RemoteBackupRuntimeState
}
