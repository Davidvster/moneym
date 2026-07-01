package com.dv.moneym.data.aiproviders.internal

import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.data.aiproviders.AiProviderId
import com.dv.moneym.data.aiproviders.RemoteAiModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class RemoteAiClient(
    private val httpClient: HttpClient,
) {
    suspend fun generate(
        provider: AiProviderId,
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        messages: List<ChatMessage>,
    ): String = when (provider) {
        AiProviderId.OPENAI -> openAiReply(apiKey, modelId, systemInstruction, messages, OPENAI_BASE_URL)
        AiProviderId.OPENROUTER -> openAiReply(apiKey, modelId, systemInstruction, messages, OPENROUTER_BASE_URL)
        AiProviderId.ANTHROPIC -> anthropicReply(apiKey, modelId, systemInstruction, messages)
        AiProviderId.GEMINI -> geminiReply(apiKey, modelId, systemInstruction, messages)
    }

    suspend fun models(provider: AiProviderId, apiKey: String): List<RemoteAiModel> = when (provider) {
        AiProviderId.OPENAI -> openAiModels(apiKey, OPENAI_BASE_URL)
        AiProviderId.OPENROUTER -> openRouterModels(apiKey)
        AiProviderId.ANTHROPIC -> anthropicModels(apiKey)
        AiProviderId.GEMINI -> geminiModels(apiKey)
    }.ifEmpty { listOf(RemoteAiModel(provider.defaultModelId)) }

    private suspend fun openAiReply(
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        messages: List<ChatMessage>,
        baseUrl: String,
    ): String {
        val response: OpenAiChatResponse = httpClient.post("$baseUrl/chat/completions") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                OpenAiChatRequest(
                    model = modelId,
                    messages = listOf(OpenAiMessage("system", systemInstruction)) + messages.map {
                        OpenAiMessage(role = it.role.remoteRole(), content = it.content)
                    },
                    stream = false,
                ),
            )
        }.body()
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    private suspend fun anthropicReply(
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        messages: List<ChatMessage>,
    ): String {
        val response: AnthropicMessageResponse = httpClient.post("$ANTHROPIC_BASE_URL/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(
                AnthropicMessageRequest(
                    model = modelId,
                    system = systemInstruction,
                    messages = messages.map {
                        AnthropicMessage(role = it.role.remoteRole(), content = it.content)
                    },
                ),
            )
        }.body()
        return response.content.joinToString("") { it.text.orEmpty() }
    }

    private suspend fun geminiReply(
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        messages: List<ChatMessage>,
    ): String {
        val response: GeminiGenerateResponse = httpClient.post("$GEMINI_BASE_URL/models/$modelId:generateContent") {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                GeminiGenerateRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemInstruction))),
                    contents = messages.map {
                        GeminiContent(
                            role = if (it.role == ChatRole.USER) "user" else "model",
                            parts = listOf(GeminiPart(it.content)),
                        )
                    },
                ),
            )
        }.body()
        return response.candidates.firstOrNull()?.content?.parts.orEmpty().joinToString("") { it.text }
    }

    private suspend fun openAiModels(apiKey: String, baseUrl: String): List<RemoteAiModel> {
        val response: OpenAiModelsResponse = httpClient.get("$baseUrl/models") { bearerAuth(apiKey) }.body()
        return response.data.map { RemoteAiModel(it.id, it.id) }
    }

    private suspend fun openRouterModels(apiKey: String): List<RemoteAiModel> {
        val response: OpenRouterModelsResponse = httpClient.get("$OPENROUTER_BASE_URL/models") { bearerAuth(apiKey) }.body()
        return response.data.map { RemoteAiModel(it.id, it.name ?: it.id) }
    }

    private suspend fun anthropicModels(apiKey: String): List<RemoteAiModel> {
        val response: AnthropicModelsResponse = httpClient.get("$ANTHROPIC_BASE_URL/models") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
        }.body()
        return response.data.map { RemoteAiModel(it.id, it.displayName ?: it.id) }
    }

    private suspend fun geminiModels(apiKey: String): List<RemoteAiModel> {
        val response: GeminiModelsResponse = httpClient.get("$GEMINI_BASE_URL/models") { parameter("key", apiKey) }.body()
        return response.models
            .filter { it.supportedGenerationMethods.orEmpty().contains("generateContent") }
            .map { RemoteAiModel(it.name.removePrefix("models/"), it.displayName ?: it.name.removePrefix("models/")) }
    }

    private fun ChatRole.remoteRole(): String = when (this) {
        ChatRole.USER -> "user"
        ChatRole.ASSISTANT -> "assistant"
    }

    private companion object {
        const val OPENAI_BASE_URL = "https://api.openai.com/v1"
        const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1"
        const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
    }
}

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val stream: Boolean,
)

@Serializable
private data class OpenAiMessage(val role: String, val content: String)

@Serializable
private data class OpenAiChatResponse(val choices: List<OpenAiChoice> = emptyList())

@Serializable
private data class OpenAiChoice(val message: OpenAiMessage? = null)

@Serializable
private data class OpenAiModelsResponse(val data: List<OpenAiModel> = emptyList())

@Serializable
private data class OpenAiModel(val id: String)

@Serializable
private data class OpenRouterModelsResponse(val data: List<OpenRouterModel> = emptyList())

@Serializable
private data class OpenRouterModel(val id: String, val name: String? = null)

@Serializable
private data class AnthropicMessageRequest(
    val model: String,
    val system: String,
    @SerialName("max_tokens") val maxTokens: Int = 1200,
    val messages: List<AnthropicMessage>,
)

@Serializable
private data class AnthropicMessage(val role: String, val content: String)

@Serializable
private data class AnthropicMessageResponse(val content: List<AnthropicContent> = emptyList())

@Serializable
private data class AnthropicContent(val type: String? = null, val text: String? = null)

@Serializable
private data class AnthropicModelsResponse(val data: List<AnthropicModel> = emptyList())

@Serializable
private data class AnthropicModel(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
private data class GeminiGenerateRequest(
    @SerialName("system_instruction") val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
)

@Serializable
private data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiGenerateResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
private data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
private data class GeminiModelsResponse(val models: List<GeminiModel> = emptyList())

@Serializable
private data class GeminiModel(
    val name: String,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null,
)
