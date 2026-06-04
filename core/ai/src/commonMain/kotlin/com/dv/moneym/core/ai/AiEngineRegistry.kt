package com.dv.moneym.core.ai

class AiEngineRegistry(private val engines: List<AiEngine>) {

    fun all(): List<AiEngine> = engines

    fun byId(id: AiEngineId): AiEngine? = engines.firstOrNull { it.id == id }

    suspend fun availabilities(): Map<AiEngineId, AiAvailability> =
        engines.associate { it.id to it.availability() }
}
