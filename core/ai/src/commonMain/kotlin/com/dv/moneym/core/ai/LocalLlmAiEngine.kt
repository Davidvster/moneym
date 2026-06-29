package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class LocalLlmAiEngine(
    private val runner: LocalLlmRunner,
    private val activeModelPath: suspend () -> String?,
) : AiEngine {

    override val id = AiEngineId.LOCAL_LLM

    override val supportsTools = true

    // A cheap, non-blocking check: an active downloaded model means the engine is usable. The
    // actual (potentially slow) native load is deferred to streamReply so the availability probe
    // never stalls the caller.
    override suspend fun availability(): AiAvailability {
        activeModelPath() ?: return AiAvailability.DOWNLOADABLE
        return AiAvailability.AVAILABLE
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun streamReply(
        messages: List<ChatMessage>,
        grounding: Grounding,
        responseLanguage: String?,
    ): Flow<String> {
        val prompt = PromptBuilder.build(messages, grounding, aiSystemInstruction(responseLanguage))
        return flow {
            val path = activeModelPath()
            val ready = path != null && (runner.isModelLoaded() || runner.loadModel(path))
            emit(ready)
        }.flatMapConcat { ready ->
            when {
                !ready -> emptyFlow()
                grounding is Grounding.Tools -> streamReplyWithTools(
                    messages = messages,
                    grounding = grounding,
                    responseLanguage = responseLanguage,
                )
                else -> runner.streamReply(prompt).stopAtTurnBoundary()
            }
        }
    }

    private fun streamReplyWithTools(
        messages: List<ChatMessage>,
        grounding: Grounding.Tools,
        responseLanguage: String?,
    ): Flow<String> = flow {
        val results = mutableListOf<ToolResult>()
        repeat(MAX_TOOL_ITERATIONS) {
            val reply = collectReply(messages, grounding, responseLanguage, results)
            val call = parseToolCall(reply)
            if (call == null) {
                emit(reply)
                return@flow
            }
            results += executeToolCall(call, grounding.tools)
        }

        results += ToolResult(
            name = "system",
            text = "Tool call limit reached. Answer using the available tool results without making another tool call.",
        )
        val finalReply = collectReply(messages, grounding, responseLanguage, results)
        if (parseToolCall(finalReply) == null) {
            emit(finalReply)
        } else {
            emit("I couldn't complete the tool request.")
        }
    }

    private suspend fun collectReply(
        messages: List<ChatMessage>,
        grounding: Grounding.Tools,
        responseLanguage: String?,
        toolResults: List<ToolResult>,
    ): String {
        val prompt = PromptBuilder.build(
            messages = messages,
            grounding = grounding,
            systemInstruction = aiSystemInstruction(responseLanguage),
            toolResults = toolResults,
        )
        return runner.streamReply(prompt)
            .stopAtTurnBoundary()
            .toList()
            .joinToString("")
            .trim()
    }

    private suspend fun executeToolCall(
        call: ToolCall,
        tools: List<AiTool>,
    ): ToolResult {
        val tool = tools.firstOrNull { it.name == call.name }
            ?: return ToolResult(call.name, "Tool error: unknown tool.")
        if (call.malformedArgs) {
            return ToolResult(tool.name, "Tool error: malformed parameters.")
        }
        return ToolResult(
            name = tool.name,
            text = runCatching { tool.invoke(call.args) }
                .getOrElse { "Tool error: ${it.message ?: "tool failed"}" },
        )
    }

    private fun parseToolCall(text: String): ToolCall? {
        val match = TOOL_CALL_REGEX.find(text) ?: return null
        val name = match.groupValues[1]
        val rawArgs = match.groupValues.getOrNull(2).orEmpty().ifBlank { "{}" }
        val args = runCatching { parseArgs(rawArgs) }
            .getOrElse { return ToolCall(name, emptyMap(), malformedArgs = true) }
        return ToolCall(name, args)
    }

    private fun parseArgs(rawArgs: String): Map<String, String> {
        val element = Json.parseToJsonElement(rawArgs)
        val obj = element as? JsonObject ?: return emptyMap()
        return obj.mapValues { (_, value) ->
            val primitive = value as? JsonPrimitive
            primitive?.contentOrNull ?: value.toString()
        }
    }

    // The prompt is a hand-rolled "User:/Assistant:" transcript, so a model with no native stop
    // token will happily keep generating the *next* User turn and answer itself forever. Cut the
    // stream the moment the model starts a new role turn, and cap total output length as a backstop.
    private fun Flow<String>.stopAtTurnBoundary(): Flow<String> = flow {
        val acc = StringBuilder()
        var emitted = 0
        try {
            collect { delta ->
                acc.append(delta)
                val stopAt = STOP_SEQUENCES
                    .mapNotNull { seq -> acc.indexOf(seq).takeIf { it >= 0 } }
                    .minOrNull()
                // Hold back only a trailing fragment that could still grow into a stop sequence
                // (e.g. a lone "\n"); everything before it is safe to emit. In the common case the
                // holdback is zero, so runner deltas pass straight through unchanged.
                val limit = stopAt ?: minOf(acc.length - pendingStopPrefix(acc), MAX_OUTPUT_CHARS)
                if (limit > emitted) {
                    emit(acc.substring(emitted, limit))
                    emitted = limit
                }
                if (stopAt != null || acc.length >= MAX_OUTPUT_CHARS) throw StopStreaming
            }
        } catch (_: StopStreaming) {
            return@flow
        }
        if (acc.length > emitted) emit(acc.substring(emitted))
    }

    // Length of the longest suffix of [acc] that is a strict prefix of some stop sequence.
    private fun pendingStopPrefix(acc: CharSequence): Int {
        for (seq in STOP_SEQUENCES) {
            val max = minOf(seq.length - 1, acc.length)
            for (k in max downTo 1) {
                if (regionMatchesPrefix(acc, seq, k)) return k
            }
        }
        return 0
    }

    private fun regionMatchesPrefix(acc: CharSequence, seq: String, k: Int): Boolean {
        val start = acc.length - k
        for (i in 0 until k) if (acc[start + i] != seq[i]) return false
        return true
    }

    private object StopStreaming : RuntimeException() {
        private fun readResolve(): Any = StopStreaming
    }

    private companion object {
        val STOP_SEQUENCES = listOf("\nUser:", "\nAssistant:")
        val TOOL_CALL_REGEX = Regex("""TOOL_CALL:\s*([A-Za-z0-9_-]+)\s*(\{.*})?""", RegexOption.DOT_MATCHES_ALL)
        const val MAX_OUTPUT_CHARS = 4000
        const val MAX_TOOL_ITERATIONS = 3
    }

    private data class ToolCall(
        val name: String,
        val args: Map<String, String>,
        val malformedArgs: Boolean = false,
    )
}
