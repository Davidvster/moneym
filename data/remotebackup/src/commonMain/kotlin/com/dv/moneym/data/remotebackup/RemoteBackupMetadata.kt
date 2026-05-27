package com.dv.moneym.data.remotebackup

import kotlinx.serialization.Serializable

@Serializable
data class RemoteBackupMetadata(
    val createdAtMs: Long,
    val appVersion: String,
    val schema: Int,
    val envelopeVersion: Int,
    val sizeBytes: Long,
    val fileName: String,
)
