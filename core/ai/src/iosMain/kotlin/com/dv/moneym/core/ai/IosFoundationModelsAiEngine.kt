package com.dv.moneym.core.ai

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class IosFoundationModelsAiEngine : AiEngine {

    override val supportsTools = false

    override suspend fun availability(): AiAvailability =
        if (IosAiBridgeHolder.instance?.isAvailable() == true) {
            AiAvailability.AVAILABLE
        } else {
            AiAvailability.UNAVAILABLE
        }

    override fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String> = callbackFlow {
        val bridge = IosAiBridgeHolder.instance ?: run {
            close()
            return@callbackFlow
        }
        val prompt = PromptBuilder.build(messages, grounding, SYSTEM_INSTRUCTION)
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

    private companion object {
        const val SYSTEM_INSTRUCTION =
            "You are a personal finance assistant inside a budgeting app. " +
                "Answer questions about the user's spending, income, accounts, and budgets " +
                "using only the financial data provided. Be concise and never invent numbers."
    }
}
