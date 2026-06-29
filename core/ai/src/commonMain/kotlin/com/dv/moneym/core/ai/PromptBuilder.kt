package com.dv.moneym.core.ai

object PromptBuilder {

    fun build(
        messages: List<ChatMessage>,
        grounding: Grounding,
        systemInstruction: String,
        toolResults: List<ToolResult> = emptyList(),
    ): String {
        val sections = mutableListOf<String>()
        sections += systemInstruction

        when (grounding) {
            is Grounding.Snapshot -> sections += "Financial data:\n${grounding.text}"
            is Grounding.Tools -> sections += buildToolsSection(grounding.tools, toolResults)
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

    private fun buildToolsSection(
        tools: List<AiTool>,
        toolResults: List<ToolResult>,
    ): String = buildString {
        appendLine("Tools are available to query the financial data.")
        appendLine("To use a tool, reply with exactly one line:")
        appendLine("""TOOL_CALL: toolName {"param":"value"}""")
        appendLine("After tool results are provided, answer normally without another TOOL_CALL unless more data is needed.")
        appendLine()
        appendLine("Available tools:")
        tools.forEach { tool ->
            appendLine("- ${tool.name}: ${tool.description}")
            appendLine("  params: ${tool.paramsSchema}")
        }
        if (toolResults.isNotEmpty()) {
            appendLine()
            appendLine("Tool results:")
            toolResults.forEach { result ->
                appendLine("TOOL_RESULT ${result.name}:")
                appendLine(result.text)
            }
        }
    }.trimEnd()
}

data class ToolResult(
    val name: String,
    val text: String,
)
