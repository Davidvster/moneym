package com.dv.moneym.data.sync

import com.dv.moneym.data.remotebackup.RemoteBackupProvider
import com.dv.moneym.data.remotebackup.RemoteFileRef

class FakeRemoteBackupProvider : RemoteBackupProvider {
    override val id: String = "fake"

    private data class Stored(val ref: RemoteFileRef, val bytes: ByteArray)

    private val files = mutableMapOf<String, Stored>()
    private var nextId = 0
    var uploadCount = 0
        private set

    override suspend fun upload(
        bytes: ByteArray,
        name: String,
        properties: Map<String, String>,
    ): RemoteFileRef {
        uploadCount++
        val ref = RemoteFileRef(id = "id-${nextId++}", name = name, modifiedAtMs = 1L, sizeBytes = bytes.size.toLong())
        files[name] = Stored(ref, bytes)
        return ref
    }

    override suspend fun updateContents(
        ref: RemoteFileRef,
        bytes: ByteArray,
        properties: Map<String, String>,
    ): RemoteFileRef {
        val updated = ref.copy(sizeBytes = bytes.size.toLong())
        files[ref.name] = Stored(updated, bytes)
        return updated
    }

    override suspend fun latest(): RemoteFileRef? = files.values.lastOrNull()?.ref

    override suspend fun list(limit: Int): List<RemoteFileRef> = files.values.map { it.ref }.take(limit)

    override suspend fun download(ref: RemoteFileRef): ByteArray =
        files[ref.name]?.bytes ?: error("not found: ${ref.name}")

    override suspend fun delete(ref: RemoteFileRef) {
        files.remove(ref.name)
    }
}
