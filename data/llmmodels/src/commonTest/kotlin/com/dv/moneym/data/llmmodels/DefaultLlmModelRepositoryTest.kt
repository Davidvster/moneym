package com.dv.moneym.data.llmmodels

import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultLlmModelRepositoryTest {

    private val model = LlmModelCatalog.models.first()

    private fun repo(
        store: InMemoryModelFileStore = InMemoryModelFileStore(),
        appSettings: FakeAppSettings = FakeAppSettings(),
        scope: kotlinx.coroutines.CoroutineScope,
    ): DefaultLlmModelRepository {
        val payload = ByteArray(128) { it.toByte() }
        val engine = MockEngine {
            respond(payload, headers = headersOf(HttpHeaders.ContentLength, payload.size.toString()))
        }
        val downloader = LlmModelDownloader(HttpClient(engine), store)
        return DefaultLlmModelRepository(appSettings, store, downloader, scope)
    }

    @Test
    fun setActivePersistsActiveModelId() = runTest {
        val appSettings = FakeAppSettings()
        val repository = repo(appSettings = appSettings, scope = backgroundScope)

        repository.setActive(model.id)

        assertEquals(model.id, appSettings.getString(PrefKeys.AI_ACTIVE_MODEL_ID, null))
    }

    @Test
    fun activeModelPathReturnsPathWhenDownloadedAndActive() = runTest {
        val store = InMemoryModelFileStore()
        val appSettings = FakeAppSettings()
        val repository = repo(store = store, appSettings = appSettings, scope = backgroundScope)

        assertNull(repository.activeModelPath())

        store.finals[model.fileName] = ByteArray(10)
        repository.setActive(model.id)

        assertEquals(store.finalPath(model.fileName), repository.activeModelPath())
    }

    @Test
    fun deleteFlipsDownloadedFalseAndClearsActive() = runTest {
        val store = InMemoryModelFileStore()
        val appSettings = FakeAppSettings()
        val repository = repo(store = store, appSettings = appSettings, scope = backgroundScope)

        store.finals[model.fileName] = ByteArray(10)
        store.sizeOverrides[model.fileName] = model.sizeBytes
        repository.setActive(model.id)

        repository.observeModels().test {
            val state = awaitItem().first { it.model.id == model.id }
            assertTrue(state.downloaded)
            assertTrue(state.active)

            repository.delete(model.id)

            var after = awaitItem().first { it.model.id == model.id }
            while (after.downloaded || after.active) {
                after = awaitItem().first { it.model.id == model.id }
            }
            assertFalse(after.downloaded)
            assertFalse(after.active)
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(appSettings.getString(PrefKeys.AI_ACTIVE_MODEL_ID, null))
        assertFalse(store.finals.containsKey(model.fileName))
    }

    @Test
    fun downloadFailurePropagatesToCallerWithoutCrashing() = runTest {
        val store = InMemoryModelFileStore()
        val engine = MockEngine { respond(ByteArray(0), HttpStatusCode.Forbidden) }
        val downloader = LlmModelDownloader(HttpClient(engine), store)
        val repository = DefaultLlmModelRepository(
            FakeAppSettings(), store, downloader, backgroundScope,
        )

        val error = assertFailsWith<ModelDownloadHttpException> { repository.download(model.id) }
        assertEquals(403, error.status)
        assertFalse(store.finals.containsKey(model.fileName))
    }

    @Test
    fun successfulDownloadMarksModelActive() = runTest {
        val testModel = LlmModel(
            id = "m1",
            displayNameKey = "k",
            fileName = "m1.litertlm",
            url = "https://example/m1",
            sizeBytes = 0L,
            sha256 = "",
            format = "litertlm",
        )
        val store = InMemoryModelFileStore()
        val payload = ByteArray(64) { it.toByte() }
        val engine = MockEngine {
            respond(payload, headers = headersOf(HttpHeaders.ContentLength, payload.size.toString()))
        }
        val downloader = LlmModelDownloader(HttpClient(engine), store)
        val appSettings = FakeAppSettings()
        val repository = DefaultLlmModelRepository(
            appSettings, store, downloader, backgroundScope, catalog = listOf(testModel),
        )

        repository.download("m1")

        assertEquals(store.finalPath("m1.litertlm"), repository.activeModelPath())
    }

}
