package com.dv.moneym.core.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocalLlmRunner(private val context: Context) : LocalLlmRunner {

    private val cache = mutableMapOf<String, LlmInference>()
    private var current: LlmInference? = null

    override suspend fun isModelLoaded(): Boolean = current != null

    override suspend fun loadModel(path: String): Boolean = runCatching {
        val engine = cache.getOrPut(path) {
            LlmInference.createFromOptions(
                context,
                LlmInferenceOptions.builder().setModelPath(path).build(),
            )
        }
        current = engine
        true
    }.getOrDefault(false)

    override fun streamReply(prompt: String): Flow<String> = callbackFlow {
        val engine = current ?: run {
            close()
            return@callbackFlow
        }
        runCatching {
            engine.generateResponseAsync(prompt) { partialResult, done ->
                trySend(partialResult)
                if (done) close()
            }
        }.onFailure { close(it) }
        awaitClose { }
    }
}
