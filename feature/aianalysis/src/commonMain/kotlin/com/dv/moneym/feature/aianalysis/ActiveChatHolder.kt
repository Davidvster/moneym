package com.dv.moneym.feature.aianalysis

/**
 * Carries a resume/new-chat action from the full-screen history back to the still-alive
 * [AnalyzeViewModel] on the back stack. The analyze screen reads and clears it on resume.
 */
class ActiveChatHolder {
    var pendingConversationId: Long? = null
    var pendingNewChat: Boolean = false
}
