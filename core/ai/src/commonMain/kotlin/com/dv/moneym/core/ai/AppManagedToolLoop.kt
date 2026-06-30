package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AiToolCall(
    val name: String,
    val params: Map<String, String>,
)

sealed interface AiToolCallParseResult {
    data class Call(val call: AiToolCall) : AiToolCallParseResult
    data object NoCall : AiToolCallParseResult
    data class Invalid(val reason: String) : AiToolCallParseResult
}

object AiToolCallParser {
    const val START_TAG = "<moneym_tool_call>"
    const val END_TAG = "</moneym_tool_call>"

    fun parse(text: String, availableToolNames: Set<String> = emptySet()): AiToolCallParseResult {
        val start = text.indexOf(START_TAG)
        if (start < 0) return parseFunctionTag(text, availableToolNames)
        val contentStart = start + START_TAG.length
        val end = text.indexOf(END_TAG, contentStart)
        if (end < 0) return AiToolCallParseResult.Invalid("missing closing tool-call tag")

        val jsonText = text.substring(contentStart, end).trim()
        val root = runCatching { json.parseToJsonElement(jsonText).jsonObject }
            .getOrElse { return AiToolCallParseResult.Invalid("tool call must be valid JSON") }
        val name = root["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (name.isEmpty()) return AiToolCallParseResult.Invalid("tool call must include a non-empty name")

        val paramsElement = root["params"] ?: return AiToolCallParseResult.Call(AiToolCall(name, emptyMap()))
        val paramsObject = paramsElement as? JsonObject
            ?: return AiToolCallParseResult.Invalid("tool params must be a JSON object")
        val params = paramsObject.toParams()
            .getOrElse { return AiToolCallParseResult.Invalid(it.message ?: "tool params must be primitive values") }
        return AiToolCallParseResult.Call(AiToolCall(name, params))
    }

    private fun parseFunctionTag(text: String, availableToolNames: Set<String>): AiToolCallParseResult {
        val trimmed = text.trim()
        val match = functionTagRegex.matchEntire(trimmed) ?: return AiToolCallParseResult.NoCall
        if (availableToolNames.isEmpty()) return AiToolCallParseResult.NoCall
        val name = match.groupValues[1]
        val jsonText = match.groupValues[2].trim()
        if (name !in availableToolNames) {
            return if (looksLikeJsonObject(jsonText)) {
                AiToolCallParseResult.Invalid("tool '$name' is not available")
            } else {
                AiToolCallParseResult.NoCall
            }
        }

        val paramsObject = runCatching { json.parseToJsonElement(jsonText).jsonObject }
            .getOrElse { return AiToolCallParseResult.Invalid("tool params must be valid JSON") }
        val params = paramsObject.toParams()
            .getOrElse { return AiToolCallParseResult.Invalid(it.message ?: "tool params must be primitive values") }
        return AiToolCallParseResult.Call(AiToolCall(name, params))
    }

    private fun JsonObject.toParams(): Result<Map<String, String>> {
        val params = mutableMapOf<String, String>()
        for ((key, value) in this) {
            val primitive = value as? JsonPrimitive
                ?: return Result.failure(IllegalArgumentException("tool params must be primitive values"))
            if (primitive.isString) {
                params[key] = normalizeParam(key, primitive.content)
            } else {
                params[key] = primitive.contentOrNull?.let { normalizeParam(key, it) }
                    ?: return Result.failure(IllegalArgumentException("tool params must be primitive values"))
            }
        }
        return Result.success(params)
    }

    private fun normalizeParam(key: String, value: String): String {
        val trimmed = value.trim()
        return when (key) {
            "type" -> trimmed.filterNot { it.isWhitespace() }.lowercase()
            else -> trimmed
        }
    }

    private fun looksLikeJsonObject(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}")
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val functionTagRegex = Regex("""<([A-Za-z][A-Za-z0-9_]*)>([\s\S]*)</\1>""")
}

class AppManagedToolLoop(
    private val maxIterations: Int = DEFAULT_MAX_ITERATIONS,
) {
    fun streamReply(
        engine: AiEngine,
        messages: List<ChatMessage>,
        tools: List<AiTool>,
        responseLanguage: String? = null,
    ): Flow<String> = flow {
        val toolsByName = tools.associateBy { it.name }
        var conversation = messages
        var latestToolResult: Pair<AiToolCall, String>? = null

        repeat(maxIterations.coerceAtLeast(1)) {
            val assistantText = engine.streamReply(
                messages = conversation,
                grounding = Grounding.Tools(tools),
                responseLanguage = responseLanguage,
            ).collectToString()

            when (val parsed = AiToolCallParser.parse(assistantText, toolsByName.keys)) {
                is AiToolCallParseResult.NoCall -> {
                    emit(assistantText)
                    return@flow
                }
                is AiToolCallParseResult.Invalid -> {
                    emit("I couldn't run the requested finance tool because the request was invalid: ${parsed.reason}.")
                    return@flow
                }
                is AiToolCallParseResult.Call -> {
                    val tool = toolsByName[parsed.call.name]
                    if (tool == null) {
                        emit("I couldn't run the requested finance tool '${parsed.call.name}' because it is not available.")
                        return@flow
                    }
                    val result = try {
                        tool.invoke(parsed.call.params)
                    } catch (error: Throwable) {
                        emit(
                            "I couldn't run the requested finance tool '${parsed.call.name}': " +
                                (error.message ?: "unknown error") + ".",
                        )
                        return@flow
                    }
                    latestToolResult = parsed.call to result
                    conversation = conversation + ChatMessage(ChatRole.ASSISTANT, assistantText) +
                        ChatMessage(ChatRole.USER, toolResultMessage(parsed.call, result))
                }
            }
        }

        emit(iterationLimitMessage(latestToolResult))
    }

    private suspend fun Flow<String>.collectToString(): String {
        val builder = StringBuilder()
        collect { builder.append(it) }
        return builder.toString()
    }

    private fun toolResultMessage(call: AiToolCall, result: String): String {
        val params = if (call.params.isEmpty()) "{}" else call.params.entries.joinToString(
            prefix = "{",
            postfix = "}",
        ) { (key, value) -> "\"$key\":\"$value\"" }
        return "Finance tool result for '${call.name}' with params $params:\n$result\n\n" +
            "Use this result to answer the user's question. If another finance lookup is essential, " +
            "request exactly one more tool call."
    }

    private fun iterationLimitMessage(latestToolResult: Pair<AiToolCall, String>?): String {
        if (latestToolResult == null) {
            return "I couldn't finish the finance tool request because the tool loop reached its iteration limit."
        }
        val (call, result) = latestToolResult
        return "I ran '${call.name}', but couldn't finish the finance tool request before the iteration limit. " +
            "Latest result: ${result.take(MAX_FALLBACK_RESULT_CHARS)}"
    }

    private companion object {
        const val DEFAULT_MAX_ITERATIONS = 3
        const val MAX_FALLBACK_RESULT_CHARS = 500
    }
}
