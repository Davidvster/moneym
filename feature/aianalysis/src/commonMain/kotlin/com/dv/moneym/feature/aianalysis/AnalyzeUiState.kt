package com.dv.moneym.feature.aianalysis

import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiGroundingMode
import com.dv.moneym.core.ai.ChatMessage
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeUiState(
    val available: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val groundingMode: AiGroundingMode = AiGroundingMode.SNAPSHOT,
    val showToolsFallbackNotice: Boolean = false,
    val engines: List<AiEngineOption> = emptyList(),
    val selectedEngine: AiEngineId? = null,
    val needsModelDownload: Boolean = false,
    val error: AnalyzeError? = null,
)

@Serializable
data class AiEngineOption(
    val id: AiEngineId,
    val available: Boolean,
    val needsDownload: Boolean,
)

@Serializable
sealed interface AnalyzeError {
    @Serializable
    data object GenerationFailed : AnalyzeError
}

sealed interface AnalyzeIntent {
    data class InputChanged(val text: String) : AnalyzeIntent
    data class SendMessage(val text: String) : AnalyzeIntent
    data class GroundingModeChanged(val mode: AiGroundingMode) : AnalyzeIntent
    data class EngineChanged(val id: AiEngineId) : AnalyzeIntent
    data object DismissFallbackNotice : AnalyzeIntent
    data object ClearError : AnalyzeIntent
}
