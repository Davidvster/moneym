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
            is Grounding.Tools -> sections += toolInstructions(grounding.tools)
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

    private fun toolInstructions(tools: List<AiTool>): String {
        val definitions = if (tools.isEmpty()) {
            "No tools are currently available."
        } else {
            tools.joinToString("\n") { tool ->
                "- ${tool.name}: ${tool.description} Params schema: ${tool.paramsSchema}"
            }
        }
        return """
            Tools are available to query the financial data.
            Available tools:
            $definitions

            To request data, reply with exactly one tool call and no other text:
            ${AiToolCallParser.START_TAG}{"name":"toolName","params":{"param":"value"}}${AiToolCallParser.END_TAG}
            Use an empty params object when the tool has no params. After a tool result is provided, answer the user normally.
        """.trimIndent()
    }
}
