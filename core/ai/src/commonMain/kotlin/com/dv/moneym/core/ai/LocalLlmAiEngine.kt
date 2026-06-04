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

    override suspend fun availability(): AiAvailability {
        val path = activeModelPath() ?: return AiAvailability.DOWNLOADABLE
        return if (runner.loadModel(path)) AiAvailability.AVAILABLE else AiAvailability.UNAVAILABLE
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
