package com.dv.moneym.data.remotebackup

data class RemoteFileRef(
    val id: String,
    val name: String,
    val modifiedAtMs: Long,
    val sizeBytes: Long,
)

interface RemoteBackupProvider {
    val id: String

    suspend fun upload(
        bytes: ByteArray,
        name: String,
        properties: Map<String, String> = emptyMap(),
    ): RemoteFileRef

    suspend fun latest(): RemoteFileRef?

    suspend fun list(limit: Int = 10): List<RemoteFileRef>

    suspend fun download(ref: RemoteFileRef): ByteArray

    suspend fun delete(ref: RemoteFileRef)
}

sealed class RemoteBackupError(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class NotAuthenticated : RemoteBackupError("Remote backup provider is not authenticated")
    class Network(cause: Throwable) : RemoteBackupError("Network error: ${cause.message}", cause)
    class Http(val status: Int, val body: String) :
        RemoteBackupError("HTTP $status: ${body.take(500)}")
    class NotFound : RemoteBackupError("Backup not found on remote")
}
