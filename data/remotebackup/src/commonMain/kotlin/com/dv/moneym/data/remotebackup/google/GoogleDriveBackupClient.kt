package com.dv.moneym.data.remotebackup.google

import com.dv.moneym.data.remotebackup.RemoteBackupError
import com.dv.moneym.data.remotebackup.RemoteBackupProvider
import com.dv.moneym.data.remotebackup.RemoteFileRef
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Instant
import kotlinx.serialization.json.Json

private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
private const val APP_DATA_FOLDER = "appDataFolder"
private const val FILES_FIELDS = "files(id,name,size,modifiedTime,appProperties),nextPageToken"
private const val SINGLE_FILE_FIELDS = "id,name,size,modifiedTime,appProperties"

class GoogleDriveBackupClient(
    private val httpClient: HttpClient,
    private val accessTokenProvider: suspend () -> String?,
    private val json: Json = defaultJson,
) : RemoteBackupProvider {

    override val id: String = "google_drive"

    override suspend fun upload(
        bytes: ByteArray,
        name: String,
        properties: Map<String, String>,
    ): RemoteFileRef {
        val token = requireToken()
        val metadata = DriveFileMetadataRequest(
            name = name,
            parents = listOf(APP_DATA_FOLDER),
            mimeType = "application/octet-stream",
            appProperties = properties.takeIf { it.isNotEmpty() },
        )
        val boundary = "moneym_${name}_${bytes.size}"
        val body = multipartRelatedBody(boundary, json.encodeToString(DriveFileMetadataRequest.serializer(), metadata), bytes)
        val response: HttpResponse = httpClient.post("$DRIVE_UPLOAD_BASE/files") {
            parameter("uploadType", "multipart")
            parameter("fields", SINGLE_FILE_FIELDS)
            authHeader(token)
            contentType(ContentType.parse("multipart/related; boundary=$boundary"))
            setBody(body)
        }
        if (!response.status.isSuccess()) throw response.toError()
        val dto = json.decodeFromString(DriveFileDto.serializer(), response.bodyAsText())
        return dto.toRef()
    }

    override suspend fun latest(): RemoteFileRef? = list(limit = 1).firstOrNull()

    override suspend fun list(limit: Int): List<RemoteFileRef> {
        val token = requireToken()
        val response: HttpResponse = httpClient.get("$DRIVE_API_BASE/files") {
            parameter("spaces", APP_DATA_FOLDER)
            parameter("orderBy", "modifiedTime desc")
            parameter("pageSize", limit.toString())
            parameter("fields", FILES_FIELDS)
            authHeader(token)
        }
        if (!response.status.isSuccess()) throw response.toError()
        val dto = json.decodeFromString(DriveFileListDto.serializer(), response.bodyAsText())
        return dto.files.map { it.toRef() }
    }

    override suspend fun download(ref: RemoteFileRef): ByteArray {
        val token = requireToken()
        val response: HttpResponse = httpClient.get("$DRIVE_API_BASE/files/${ref.id}") {
            parameter("alt", "media")
            authHeader(token)
        }
        if (!response.status.isSuccess()) throw response.toError()
        return response.readRawBytes()
    }

    override suspend fun delete(ref: RemoteFileRef) {
        val token = requireToken()
        val response: HttpResponse = httpClient.delete("$DRIVE_API_BASE/files/${ref.id}") {
            authHeader(token)
        }
        if (!response.status.isSuccess() && response.status != HttpStatusCode.NoContent) {
            throw response.toError()
        }
    }

    override suspend fun remainingQuotaBytes(): Long? {
        val token = requireToken()
        val response: HttpResponse = httpClient.get("$DRIVE_API_BASE/about") {
            parameter("fields", "storageQuota")
            authHeader(token)
        }
        if (!response.status.isSuccess()) return null
        val dto = json.decodeFromString(DriveAboutDto.serializer(), response.bodyAsText())
        val quota = dto.storageQuota ?: return null
        val limit = quota.limit?.toLongOrNull() ?: return null
        val usage = quota.usage?.toLongOrNull() ?: 0L
        return (limit - usage).coerceAtLeast(0L)
    }

    private suspend fun requireToken(): String =
        accessTokenProvider() ?: throw RemoteBackupError.NotAuthenticated()

    private fun io.ktor.client.request.HttpRequestBuilder.authHeader(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun HttpResponse.toError(): RemoteBackupError =
        if (status == HttpStatusCode.NotFound) {
            RemoteBackupError.NotFound()
        } else {
            RemoteBackupError.Http(status.value, bodyAsText())
        }

    private fun HttpStatusCode.isSuccess() = value in 200..299

    private fun DriveFileDto.toRef(): RemoteFileRef = RemoteFileRef(
        id = id,
        name = name,
        modifiedAtMs = modifiedTime?.parseRfc3339() ?: 0L,
        sizeBytes = size?.toLongOrNull() ?: 0L,
    )

    private fun String.parseRfc3339(): Long? = runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrNull()

    companion object {
        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            isLenient = true
        }

        fun newHttpClient(engineFactoryClient: HttpClient): HttpClient = engineFactoryClient.config {
            install(ContentNegotiation) { json(defaultJson) }
        }
    }
}

private fun multipartRelatedBody(boundary: String, metadataJson: String, payload: ByteArray): ByteArray {
    val header = buildString {
        append("--").append(boundary).append("\r\n")
        append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        append(metadataJson).append("\r\n")
        append("--").append(boundary).append("\r\n")
        append("Content-Type: application/octet-stream\r\n\r\n")
    }.encodeToByteArray()
    val footer = "\r\n--$boundary--\r\n".encodeToByteArray()
    val out = ByteArray(header.size + payload.size + footer.size)
    header.copyInto(out, 0)
    payload.copyInto(out, header.size)
    footer.copyInto(out, header.size + payload.size)
    return out
}
