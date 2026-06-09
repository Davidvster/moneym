package com.dv.moneym.feature.aianalysis.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.data.aichat.AiChatRepository
import com.dv.moneym.feature.aianalysis.ActiveChatHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnalyzeHistoryViewModel(
    private val aiChatRepository: AiChatRepository,
    private val activeChatHolder: ActiveChatHolder,
) : ViewModel() {

    private val pendingDeleteId = MutableStateFlow<Long?>(null)

    internal val state: StateFlow<AnalyzeHistoryUiState> =
        combine(aiChatRepository.observeConversations(), pendingDeleteId) { conversations, pending ->
            AnalyzeHistoryUiState(conversations = conversations, pendingDeleteId = pending)
        }.stateIn(viewModelScope, SharingStarted.Lazily, AnalyzeHistoryUiState())

    fun onIntent(intent: AnalyzeHistoryIntent) {
        when (intent) {
            is AnalyzeHistoryIntent.Resume -> activeChatHolder.pendingConversationId = intent.id
            AnalyzeHistoryIntent.NewChat -> activeChatHolder.pendingNewChat = true
            is AnalyzeHistoryIntent.RequestDelete -> pendingDeleteId.value = intent.id
            AnalyzeHistoryIntent.DismissDelete -> pendingDeleteId.value = null
            AnalyzeHistoryIntent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun confirmDelete() {
        val id = pendingDeleteId.value ?: return
        viewModelScope.launch {
            aiChatRepository.delete(id)
            pendingDeleteId.value = null
        }
    }
}
