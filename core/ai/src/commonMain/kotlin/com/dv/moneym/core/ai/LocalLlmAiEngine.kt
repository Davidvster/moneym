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
    override fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String> {
        val prompt = PromptBuilder.build(messages, grounding, SYSTEM_INSTRUCTION)
        return flow {
            val path = activeModelPath()
            val ready = path != null && (runner.isModelLoaded() || runner.loadModel(path))
            emit(ready)
        }.flatMapConcat { ready ->
            if (ready) runner.streamReply(prompt) else emptyFlow()
        }
    }
}
