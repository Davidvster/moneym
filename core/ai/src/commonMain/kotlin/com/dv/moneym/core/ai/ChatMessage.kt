package com.dv.moneym.core.ai

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole { USER, ASSISTANT }

@Serializable
data class ChatMessage(val role: ChatRole, val content: String)
