package com.dv.moneym.core.ai

import com.dv.moneym.core.common.LocalModelRuntime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosLocalLlmRunner : LocalLlmRunner, LocalModelRuntime {

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

    // The Swift bridge owns model lifetime and exposes no unload hook, so there is nothing to free
    // from here yet. Kept to satisfy the shared seam; revisit if the bridge gains an unload API.
    override suspend fun releaseAndClearCache() = Unit
}
