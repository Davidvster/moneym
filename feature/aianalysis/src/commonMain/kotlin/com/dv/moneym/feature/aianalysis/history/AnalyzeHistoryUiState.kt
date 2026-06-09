package com.dv.moneym.feature.aianalysis.history

import com.dv.moneym.data.aichat.ChatConversation

data class AnalyzeHistoryUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val pendingDeleteId: Long? = null,
)

sealed interface AnalyzeHistoryIntent {
    data class Resume(val id: Long) : AnalyzeHistoryIntent
    data object NewChat : AnalyzeHistoryIntent
    data class RequestDelete(val id: Long) : AnalyzeHistoryIntent
    data object ConfirmDelete : AnalyzeHistoryIntent
    data object DismissDelete : AnalyzeHistoryIntent
}
