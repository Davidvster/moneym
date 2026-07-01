package com.dv.moneym.data.aiproviders.internal

import app.cash.turbine.test
import com.dv.moneym.core.security.SecureStore
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.data.aiproviders.AiProviderId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultAiProviderRepositoryTest {

    @Test
    fun apiKeyRoundTripsThroughSecureStore() = runTest {
        val repository = repository()

        assertNull(repository.apiKey(AiProviderId.OPENAI))
        repository.saveApiKey(AiProviderId.OPENAI, " sk-test ")

        assertEquals("sk-test", repository.apiKey(AiProviderId.OPENAI))
    }

    @Test
    fun observedStateIncludesConfiguredProviderAndSelectedModel() = runTest {
        val repository = repository()
        repository.saveApiKey(AiProviderId.OPENAI, "sk-test")
        repository.setSelectedModel(AiProviderId.OPENAI, "gpt-test")

        repository.observeProviders().test {
            val state = awaitItem().first { it.provider == AiProviderId.OPENAI }
            assertEquals(true, state.configured)
            assertEquals("gpt-test", state.selectedModelId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun repository(
        response: String = """{"data":[{"id":"gpt-test"}]}""",
    ): DefaultAiProviderRepository {
        val http = HttpClient(MockEngine { respondJson(response) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return DefaultAiProviderRepository(
            appSettings = FakeAppSettings(),
            secureStore = InMemorySecureStore(),
            client = RemoteAiClient(http),
        )
    }
}

internal fun MockRequestHandleScope.respondJson(content: String) = respond(
    content = content,
    status = HttpStatusCode.OK,
    headers = headersOf(HttpHeaders.ContentType, "application/json"),
)

private class InMemorySecureStore : SecureStore {
    private val values = mutableMapOf<String, ByteArray>()
    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) {
        values[key] = value
    }
    override suspend fun get(key: String): ByteArray? = values[key]
    override suspend fun remove(key: String) {
        values.remove(key)
    }
}
