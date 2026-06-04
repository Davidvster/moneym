package com.dv.moneym.core.ai

interface IosLocalLlmBridge {
    fun isLoaded(): Boolean
    fun loadModel(path: String, onResult: (Boolean) -> Unit)
    fun streamReply(
        prompt: String,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    )
}

object IosLocalLlmBridgeHolder {
    var instance: IosLocalLlmBridge? = null
}
