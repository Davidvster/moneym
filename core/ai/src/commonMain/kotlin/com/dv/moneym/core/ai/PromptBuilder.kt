package com.dv.moneym.core.ai

object PromptBuilder {

    fun build(
        messages: List<ChatMessage>,
        grounding: Grounding,
        systemInstruction: String,
    ): String {
        val sections = mutableListOf<String>()
        sections += systemInstruction

        when (grounding) {
            is Grounding.Snapshot -> sections += "Financial data:\n${grounding.text}"
            is Grounding.Tools -> sections += "Tools are available to query the financial data."
        }

        for (message in messages) {
            val prefix = when (message.role) {
                ChatRole.USER -> "User:"
                ChatRole.ASSISTANT -> "Assistant:"
            }
            sections += "$prefix ${message.content}"
        }

        sections += "Assistant:"
        return sections.joinToString("\n\n")
    }
}
