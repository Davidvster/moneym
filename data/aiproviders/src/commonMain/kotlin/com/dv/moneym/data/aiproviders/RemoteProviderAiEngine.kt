package com.dv.moneym.data.aiproviders

import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngine
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.Grounding
import com.dv.moneym.core.ai.PromptBuilder
import com.dv.moneym.core.ai.aiSystemInstruction
import com.dv.moneym.data.aiproviders.internal.RemoteAiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteProviderAiEngine(
    private val provider: AiProviderId,
    private val repository: AiProviderRepository,
    private val client: RemoteAiClient,
) : AiEngine {

    override val id: AiEngineId = provider.engineId
    override val supportsTools: Boolean = false

    override suspend fun availability(): AiAvailability =
        if (repository.apiKey(provider) == null) AiAvailability.UNAVAILABLE else AiAvailability.AVAILABLE

    override fun streamReply(
        messages: List<ChatMessage>,
        grounding: Grounding,
        responseLanguage: String?,
    ): Flow<String> = flow {
        val apiKey = repository.apiKey(provider) ?: error("API key is not configured")
        val model = repository.selectedModel(provider)
        val promptMessages = when (grounding) {
            is Grounding.Snapshot -> messages + ChatMessage(
                role = com.dv.moneym.core.ai.ChatRole.USER,
                content = "Financial data:\n${grounding.text}",
            )
            is Grounding.Tools -> messages
        }
        val systemInstruction = when (grounding) {
            is Grounding.Tools -> PromptBuilder.build(
                messages = emptyList(),
                grounding = grounding,
                systemInstruction = aiSystemInstruction(responseLanguage),
            )
            is Grounding.Snapshot -> aiSystemInstruction(responseLanguage)
        }
        val reply = client.generate(provider, apiKey, model, systemInstruction, promptMessages)
        emit(reply)
    }
}
