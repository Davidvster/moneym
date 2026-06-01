package com.dv.moneym.core.ai

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val content: String)
