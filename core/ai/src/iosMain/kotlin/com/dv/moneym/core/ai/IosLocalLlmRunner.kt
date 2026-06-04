package com.dv.moneym.core.ai

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosLocalLlmRunner : LocalLlmRunner {

    override suspend fun isModelLoaded(): Boolean =
        IosLocalLlmBridgeHolder.instance?.isLoaded() == true

    override suspend fun loadModel(path: String): Boolean {
        val bridge = IosLocalLlmBridgeHolder.instance ?: return false
        return suspendCancellableCoroutine { continuation ->
            bridge.loadModel(path) { loaded -> continuation.resume(loaded) }
        }
    }

    override fun streamReply(prompt: String): Flow<String> = callbackFlow {
        val bridge = IosLocalLlmBridgeHolder.instance ?: run {
            close()
            return@callbackFlow
        }
        var last = ""
        bridge.streamReply(
            prompt = prompt,
            onChunk = { cumulative ->
                val delta = cumulative.removePrefix(last)
                last = cumulative
                trySend(delta)
            },
            onComplete = { close() },
            onError = { message -> close(IllegalStateException(message)) },
        )
        awaitClose { }
    }
}
