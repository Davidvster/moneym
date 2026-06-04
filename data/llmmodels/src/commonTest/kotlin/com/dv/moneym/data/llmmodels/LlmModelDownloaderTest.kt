package com.dv.moneym.data.llmmodels

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LlmModelDownloaderTest {

    private val payload = ByteArray(200_000) { (it % 251).toByte() }

    private fun model(sha256: String = "", requiresToken: Boolean = false) = LlmModel(
        id = "test-model",
        displayNameKey = "ai_model_test",
        fileName = "test.litertlm",
        url = "https://example.com/test.litertlm",
        sizeBytes = payload.size.toLong(),
        sha256 = sha256,
        format = "litertlm",
        requiresToken = requiresToken,
    )

    private fun client(onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {}): HttpClient {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(
                content = payload,
                headers = headersOf(HttpHeaders.ContentLength, payload.size.toString()),
            )
        }
        return HttpClient(engine)
    }

    @Test
    fun emitsIncreasingProgressAndPromotesFile() = runTest {
        val store = InMemoryModelFileStore()
        val downloader = LlmModelDownloader(client(), store) { null }

        val progress = downloader.download(model()).toList()

        assertTrue(progress.size >= 2)
        val fractions = progress.map { it.fraction }
        assertTrue(fractions.zipWithNext().all { (a, b) -> b >= a })
        assertEquals(1f, fractions.last())
        assertTrue(store.finalExists("test.litertlm"))
        assertNull(store.parts["test.litertlm"])
    }

    @Test
    fun verifiesSha256OnSuccess() = runTest {
        val expected = sha256Hex(payload)
        val store = InMemoryModelFileStore()
        val downloader = LlmModelDownloader(client(), store) { null }

        downloader.download(model(sha256 = expected)).toList()

        assertTrue(store.finalExists("test.litertlm"))
    }

    @Test
    fun failsOnSha256Mismatch() = runTest {
        val store = InMemoryModelFileStore()
        val downloader = LlmModelDownloader(client(), store) { null }

        assertFailsWith<ModelChecksumMismatchException> {
            downloader.download(model(sha256 = "deadbeef")).toList()
        }
        assertTrue(!store.finalExists("test.litertlm"))
        assertNull(store.parts["test.litertlm"])
    }

    @Test
    fun addsAuthHeaderWhenTokenAndRequiresToken() = runTest {
        var seenAuth: String? = null
        val store = InMemoryModelFileStore()
        val httpClient = client { request ->
            seenAuth = request.headers[HttpHeaders.Authorization]
        }
        val downloader = LlmModelDownloader(httpClient, store) { "secret-token" }

        downloader.download(model(requiresToken = true)).toList()

        assertEquals("Bearer secret-token", seenAuth)
    }

    @Test
    fun omitsAuthHeaderWhenModelDoesNotRequireToken() = runTest {
        var seenAuth: String? = null
        val store = InMemoryModelFileStore()
        val httpClient = client { request ->
            seenAuth = request.headers[HttpHeaders.Authorization]
        }
        val downloader = LlmModelDownloader(httpClient, store) { "secret-token" }

        downloader.download(model(requiresToken = false)).toList()

        assertNull(seenAuth)
    }
}
