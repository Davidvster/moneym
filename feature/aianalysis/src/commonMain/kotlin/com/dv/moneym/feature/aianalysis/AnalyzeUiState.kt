package com.dv.moneym.feature.aianalysis

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
    val error: AnalyzeError? = null,
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
    data object DismissFallbackNotice : AnalyzeIntent
    data object ClearError : AnalyzeIntent
}
