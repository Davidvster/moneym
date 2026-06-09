package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow

class LocalLlmAiEngine(
    private val runner: LocalLlmRunner,
    private val activeModelPath: suspend () -> String?,
) : AiEngine {

    override val id = AiEngineId.LOCAL_LLM

    override val supportsTools = false

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
            if (ready) runner.streamReply(prompt).stopAtTurnBoundary() else emptyFlow()
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
        const val MAX_OUTPUT_CHARS = 4000
    }
}
