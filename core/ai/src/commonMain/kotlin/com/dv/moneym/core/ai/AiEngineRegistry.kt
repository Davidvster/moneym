package com.dv.moneym.core.ai

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class AiEngineRegistry(private val engines: List<AiEngine>) {

    fun all(): List<AiEngine> = engines

    fun byId(id: AiEngineId): AiEngine? = engines.firstOrNull { it.id == id }

    // Query engines in parallel and cap each probe — a built-in engine (e.g. Gemini Nano on a
    // device without AICore) can otherwise block the whole availability check indefinitely.
    suspend fun availabilities(): Map<AiEngineId, AiAvailability> = coroutineScope {
        engines.map { engine ->
            async {
                engine.id to (
                    withTimeoutOrNull(AVAILABILITY_TIMEOUT_MS) {
                        runCatching { engine.availability() }.getOrNull()
                    } ?: AiAvailability.UNAVAILABLE
                )
            }
        }.awaitAll().toMap()
    }

    private companion object {
        const val AVAILABILITY_TIMEOUT_MS = 3000L
    }
}
