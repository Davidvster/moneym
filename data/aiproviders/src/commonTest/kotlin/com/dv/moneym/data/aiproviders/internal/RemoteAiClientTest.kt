package com.dv.moneym.data.aiproviders.internal

import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.data.aiproviders.AiProviderId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoteAiClientTest {

    @Test
    fun openAiRequestMapsMessagesAndParsesReply() = runTest {
        var requestBody = ""
        val client = client(
            response = """{"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
            onBody = { requestBody = it },
        )

        val reply = client.generate(AiProviderId.OPENAI, "key", "gpt-test", "system", userMessages())

        assertEquals("ok", reply)
        assertTrue(requestBody.contains("gpt-test"), requestBody)
        assertTrue(requestBody.contains("system"), requestBody)
        assertTrue(requestBody.contains("question"), requestBody)
    }

    @Test
    fun anthropicRequestMapsMessagesAndParsesReply() = runTest {
        var requestBody = ""
        val client = client(
            response = """{"content":[{"type":"text","text":"claude ok"}]}""",
            onBody = { requestBody = it },
        )

        val reply = client.generate(AiProviderId.ANTHROPIC, "key", "claude-test", "system", userMessages())

        assertEquals("claude ok", reply)
        assertTrue(requestBody.contains("claude-test"), requestBody)
        assertTrue(requestBody.contains("system"), requestBody)
        assertTrue(requestBody.contains("question"), requestBody)
    }

    @Test
    fun geminiRequestMapsMessagesAndParsesReply() = runTest {
        var requestBody = ""
        val client = client(
            response = """{"candidates":[{"content":{"parts":[{"text":"gemini ok"}]}}]}""",
            onBody = { requestBody = it },
        )

        val reply = client.generate(AiProviderId.GEMINI, "key", "gemini-test", "system", userMessages())

        assertEquals("gemini ok", reply)
        assertTrue(requestBody.contains("system_instruction"), requestBody)
        assertTrue(requestBody.contains("question"), requestBody)
    }

    @Test
    fun openRouterUsesOpenAiShape() = runTest {
        var requestBody = ""
        val client = client(
            response = """{"choices":[{"message":{"role":"assistant","content":"router ok"}}]}""",
            onBody = { requestBody = it },
        )

        val reply = client.generate(AiProviderId.OPENROUTER, "key", "openai/gpt-test", "system", userMessages())

        assertEquals("router ok", reply)
        assertTrue(requestBody.contains("openai/gpt-test"), requestBody)
    }

    private fun client(response: String, onBody: (String) -> Unit): RemoteAiClient {
        val http = HttpClient(MockEngine { request ->
            onBody(request.body.readTextBody())
            respondJson(response)
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return RemoteAiClient(http)
    }

    private fun userMessages(): List<ChatMessage> = listOf(ChatMessage(ChatRole.USER, "question"))

    private suspend fun OutgoingContent.readTextBody(): String = when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.ReadChannelContent -> readFrom().readRemaining().readText()
        is OutgoingContent.NoContent -> ""
        is OutgoingContent.WriteChannelContent -> ""
        is OutgoingContent.ProtocolUpgrade -> ""
        else -> ""
    }
}
