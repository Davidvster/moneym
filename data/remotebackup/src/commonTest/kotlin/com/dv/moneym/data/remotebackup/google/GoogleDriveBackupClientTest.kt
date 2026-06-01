package com.dv.moneym.data.remotebackup.google

import com.dv.moneym.data.remotebackup.RemoteBackupError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleDriveBackupClientTest {

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): GoogleDriveBackupClient {
        val http = HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return GoogleDriveBackupClient(httpClient = http, accessTokenProvider = { "fake-token" })
    }

    @Test
    fun upload_postsMultipartToAppDataFolder() = runTest {
        var capturedUrl: String? = null
        var capturedAuth: String? = null
        var capturedContentType: String? = null
        val c = client { req ->
            capturedUrl = req.url.toString()
            capturedAuth = req.headers[HttpHeaders.Authorization]
            capturedContentType = req.body.contentType?.toString()
            respond(
                content = """{"id":"abc","name":"moneym-backup.bin","size":"42","modifiedTime":"2026-01-01T00:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val ref = c.upload(ByteArray(42) { 1 }, "moneym-backup.bin", mapOf("schema" to "1"))
        assertEquals("abc", ref.id)
        assertEquals(42L, ref.sizeBytes)
        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("uploadType=multipart"))
        assertTrue(capturedUrl!!.contains("/upload/drive/v3/files"))
        assertEquals("Bearer fake-token", capturedAuth)
        assertTrue(capturedContentType!!.startsWith("multipart/related"))
    }

    @Test
    fun list_setsAppDataFolderSpaceAndOrdersByModifiedTime() = runTest {
        var capturedQuery: String? = null
        val c = client { req ->
            capturedQuery = req.url.encodedQuery
            respond(
                """{"files":[{"id":"x","name":"a.bin","size":"10","modifiedTime":"2026-01-01T00:00:00Z"}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val refs = c.list(limit = 5)
        assertEquals(1, refs.size)
        assertTrue(capturedQuery!!.contains("spaces=appDataFolder"))
        assertTrue(capturedQuery!!.contains("orderBy=modifiedTime+desc") || capturedQuery!!.contains("orderBy=modifiedTime%20desc"))
        assertTrue(capturedQuery!!.contains("pageSize=5"))
    }

    @Test
    fun download_usesAltMedia() = runTest {
        val payload = ByteArray(7) { (it + 1).toByte() }
        val c = client { req ->
            assertEquals(HttpMethod.Get, req.method)
            assertTrue(req.url.encodedQuery.contains("alt=media"))
            respond(payload, HttpStatusCode.OK)
        }
        val bytes = c.download(com.dv.moneym.data.remotebackup.RemoteFileRef("id1", "n", 0L, 7L))
        assertEquals(payload.toList(), bytes.toList())
    }

    @Test
    fun updateContents_patchesMediaUpload() = runTest {
        var capturedUrl: String? = null
        var capturedMethod: HttpMethod? = null
        val c = client { req ->
            capturedUrl = req.url.toString()
            capturedMethod = req.method
            respond(
                content = """{"id":"file1","name":"moneym-sync-state.json","size":"9","modifiedTime":"2026-01-01T00:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val ref = c.updateContents(
            com.dv.moneym.data.remotebackup.RemoteFileRef("file1", "moneym-sync-state.json", 0L, 0L),
            ByteArray(9) { 2 },
        )
        assertEquals("file1", ref.id)
        assertEquals(HttpMethod.Patch, capturedMethod)
        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("/upload/drive/v3/files/file1"))
        assertTrue(capturedUrl!!.contains("uploadType=media"))
    }

    @Test
    fun missingToken_throwsNotAuthenticated() = runTest {
        val http = HttpClient(MockEngine { respond("") })
        val c = GoogleDriveBackupClient(httpClient = http, accessTokenProvider = { null })
        assertFailsWith<RemoteBackupError.NotAuthenticated> { c.list() }
    }

    @Test
    fun http500_throwsHttpError() = runTest {
        val c = client { respond("boom", HttpStatusCode.InternalServerError) }
        val err = assertFailsWith<RemoteBackupError.Http> { c.list() }
        assertEquals(500, err.status)
    }
}
