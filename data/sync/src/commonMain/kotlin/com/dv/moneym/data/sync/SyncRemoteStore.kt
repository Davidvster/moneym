package com.dv.moneym.data.sync

import com.dv.moneym.data.remotebackup.RemoteBackupProvider
import com.dv.moneym.data.remotebackup.RemoteFileRef

class SyncRemoteStore(
    private val provider: RemoteBackupProvider,
) {

    suspend fun readSnapshotBytes(): ByteArray? =
        provider.findByName(SYNC_STATE_FILE)?.let { provider.download(it) }

    suspend fun writeSnapshotBytes(bytes: ByteArray): RemoteFileRef = upsert(SYNC_STATE_FILE, bytes)

    suspend fun readDevicesBytes(): ByteArray? =
        provider.findByName(DEVICES_FILE)?.let { provider.download(it) }

    suspend fun writeDevicesBytes(bytes: ByteArray): RemoteFileRef = upsert(DEVICES_FILE, bytes)

    private suspend fun upsert(name: String, bytes: ByteArray): RemoteFileRef {
        val existing = provider.findByName(name)
        return if (existing != null) provider.updateContents(existing, bytes)
        else provider.upload(bytes, name)
    }

    companion object {
        const val SYNC_STATE_FILE = "moneym-sync-state.json"
        const val DEVICES_FILE = "moneym-devices.json"
    }
}
