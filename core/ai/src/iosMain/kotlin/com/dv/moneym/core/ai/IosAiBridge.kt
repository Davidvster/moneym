package com.dv.moneym.core.ai

interface IosAiBridge {
    fun isAvailable(): Boolean
    fun streamReply(
        prompt: String,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    )
}

object IosAiBridgeHolder {
    var instance: IosAiBridge? = null
}
