package com.dv.moneym.core.ai

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class IosFoundationModelsAiEngine : AiEngine {

    override val id = AiEngineId.APPLE_INTELLIGENCE

    override val supportsTools = false

    override suspend fun availability(): AiAvailability =
        if (IosAiBridgeHolder.instance?.isAvailable() == true) {
            AiAvailability.AVAILABLE
        } else {
            AiAvailability.UNAVAILABLE
        }

    override fun streamReply(
        messages: List<ChatMessage>,
        grounding: Grounding,
        responseLanguage: String?,
    ): Flow<String> = callbackFlow {
        val bridge = IosAiBridgeHolder.instance ?: run {
            close()
            return@callbackFlow
        }
        val prompt = PromptBuilder.build(messages, grounding, aiSystemInstruction(responseLanguage))
        var last = ""
        bridge.streamReply(
            prompt = prompt,
            onChunk = { cumulative ->
                val delta = cumulative.removePrefix(last)
                last = cumulative
                trySend(delta)
            },
            onComplete = { close() },
            onError = { message -> close(IllegalStateException(message)) },
        )
        awaitClose { }
    }
}
