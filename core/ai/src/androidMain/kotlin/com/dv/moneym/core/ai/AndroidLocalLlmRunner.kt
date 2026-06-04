package com.dv.moneym.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AndroidLocalLlmRunner(private val context: Context) : LocalLlmRunner {

    private val cache = mutableMapOf<String, Engine>()
    private var current: Engine? = null

    override suspend fun isModelLoaded(): Boolean = current != null

    override suspend fun loadModel(path: String): Boolean = runCatching {
        val engine = cache.getOrPut(path) {
            Engine(EngineConfig(modelPath = path, cacheDir = context.cacheDir.path)).also { it.initialize() }
        }
        current = engine
        true
    }.getOrDefault(false)

    override fun streamReply(prompt: String): Flow<String> = flow {
        val engine = current ?: return@flow
        engine.createConversation().use { conversation ->
            conversation.sendMessageAsync(prompt).collect { message -> emit(message.toString()) }
        }
    }
}
