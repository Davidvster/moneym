package com.dv.moneym.core.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeminiNanoAiEngine : AiEngine {

    private val model: GenerativeModel by lazy { Generation.getClient() }

    override val supportsTools = false

    override suspend fun availability(): AiAvailability = runCatching {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> AiAvailability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> AiAvailability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> AiAvailability.DOWNLOADING
            else -> AiAvailability.UNAVAILABLE
        }
    }.getOrDefault(AiAvailability.UNAVAILABLE)

    override fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String> {
        val prompt = PromptBuilder.build(messages, grounding, SYSTEM_INSTRUCTION)
        return model.generateContentStream(prompt).map { it.candidates.firstOrNull()?.text.orEmpty() }
    }

    private companion object {
        const val SYSTEM_INSTRUCTION =
            "You are a personal finance assistant inside a budgeting app. " +
                "Answer questions about the user's spending, income, accounts, and budgets " +
                "using only the financial data provided. Be concise and never invent numbers."
    }
}
