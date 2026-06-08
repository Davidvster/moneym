package com.dv.moneym.data.llmmodels

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.readByteArray

class ModelChecksumMismatchException(modelId: String) :
    Exception("Checksum mismatch for model '$modelId'")

class ModelDownloadHttpException(modelId: String, val status: Int) :
    Exception("Download failed for model '$modelId' (HTTP $status)")

class LlmModelDownloader(
    private val client: HttpClient,
    private val fileStore: ModelFileStore,
) {

    fun download(model: LlmModel): Flow<DownloadProgress> = flow {
        var resumeFrom = fileStore.partSize(model.fileName)
        if (resumeFrom > 0L && model.sizeBytes in 1 until resumeFrom + 1) {
            if (resumeFrom >= model.sizeBytes) {
                fileStore.resetPart(model.fileName)
                resumeFrom = 0L
            }
        }

        client.prepareGet(model.url) {
            if (resumeFrom > 0L) header(HttpHeaders.Range, "bytes=$resumeFrom-")
        }.execute { response ->
            if (!response.status.isSuccess()) {
                if (resumeFrom > 0L) fileStore.resetPart(model.fileName)
                throw ModelDownloadHttpException(model.id, response.status.value)
            }
            val resumed = response.status == HttpStatusCode.PartialContent
            if (!resumed && resumeFrom > 0L) {
                fileStore.resetPart(model.fileName)
                resumeFrom = 0L
            }

            val remaining = response.contentLength() ?: 0L
            val total = when {
                model.sizeBytes > 0L -> model.sizeBytes
                remaining > 0L -> resumeFrom + remaining
                else -> 0L
            }

            var bytesRead = resumeFrom
            val channel = response.bodyAsChannel()
            emit(DownloadProgress(bytesRead, total))

            while (!channel.isClosedForRead) {
                currentCoroutineContext().ensureActive()
                val packet = channel.readRemaining(CHUNK_SIZE)
                while (!packet.exhausted()) {
                    val chunk = packet.readByteArray()
                    if (chunk.isEmpty()) break
                    fileStore.appendToPart(model.fileName, chunk, 0, chunk.size)
                    bytesRead += chunk.size
                    emit(DownloadProgress(bytesRead, total))
                }
            }
        }

        if (model.sha256.isNotEmpty()) {
            val actual = fileStore.sha256OfPart(model.fileName)
            if (!actual.equals(model.sha256, ignoreCase = true)) {
                fileStore.deletePart(model.fileName)
                throw ModelChecksumMismatchException(model.id)
            }
        }

        fileStore.promotePart(model.fileName)
    }

    private companion object {
        const val CHUNK_SIZE = 64L * 1024L
    }
}
