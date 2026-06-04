package com.dv.moneym.data.llmmodels

import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.InMemorySecureStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultLlmModelRepositoryTest {

    private val model = LlmModelCatalog.models.first()

    private fun repo(
        store: InMemoryModelFileStore = InMemoryModelFileStore(),
        appSettings: FakeAppSettings = FakeAppSettings(),
        secureStore: InMemorySecureStore = InMemorySecureStore(),
        scope: kotlinx.coroutines.CoroutineScope,
    ): DefaultLlmModelRepository {
        val payload = ByteArray(128) { it.toByte() }
        val engine = MockEngine {
            respond(payload, headers = headersOf(HttpHeaders.ContentLength, payload.size.toString()))
        }
        val downloader = LlmModelDownloader(HttpClient(engine), store) { null }
        return DefaultLlmModelRepository(appSettings, secureStore, store, downloader, scope)
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
    fun setHfTokenUpdatesObserveHasToken() = runTest {
        val secureStore = InMemorySecureStore()
        val repository = repo(secureStore = secureStore, scope = backgroundScope)

        repository.observeHasToken().test {
            assertFalse(awaitItem())
            repository.setHfToken("hf_abc")
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
