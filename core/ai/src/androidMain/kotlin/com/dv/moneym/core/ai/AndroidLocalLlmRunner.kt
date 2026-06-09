package com.dv.moneym.core.ai

import android.content.Context
import com.dv.moneym.core.common.LocalModelRuntime
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class AndroidLocalLlmRunner(private val context: Context) : LocalLlmRunner, LocalModelRuntime {

    // Dedicated subdir so compiled-model artifacts can be wiped without touching the rest of the
    // app cache. LiteRT-LM writes its compiled copy of the model here on first load.
    private val cacheDir = File(context.cacheDir, "litertlm")

    private val cache = mutableMapOf<String, Engine>()
    private var current: Engine? = null

    override suspend fun isModelLoaded(): Boolean = current != null

    override suspend fun loadModel(path: String): Boolean = runCatching {
        cacheDir.mkdirs()
        val engine = cache.getOrPut(path) {
            Engine(EngineConfig(modelPath = path, cacheDir = cacheDir.path)).also { it.initialize() }
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

    // Closing the engine releases the open handle on the model file; only then do the on-disk blocks
    // of an already-deleted blob get reclaimed. Wiping the cache subdir removes the compiled copy.
    override suspend fun releaseAndClearCache() = withContext(Dispatchers.IO) {
        current = null
        cache.values.forEach { runCatching { it.close() } }
        cache.clear()
        cacheDir.deleteRecursively()
        Unit
    }
}
