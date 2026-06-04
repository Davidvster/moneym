package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AiEngineRegistryTest {

    private class StubEngine(
        override val id: AiEngineId,
        private val availability: AiAvailability,
    ) : AiEngine {
        override val supportsTools = false
        override suspend fun availability(): AiAvailability = availability
        override fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String> = emptyFlow()
    }

    private val gemini = StubEngine(AiEngineId.GEMINI_NANO, AiAvailability.AVAILABLE)
    private val local = StubEngine(AiEngineId.LOCAL_LLM, AiAvailability.DOWNLOADABLE)
    private val registry = AiEngineRegistry(listOf(gemini, local))

    @Test
    fun byIdResolvesEngine() {
        assertSame(gemini, registry.byId(AiEngineId.GEMINI_NANO))
        assertSame(local, registry.byId(AiEngineId.LOCAL_LLM))
    }

    @Test
    fun byIdReturnsNullWhenMissing() {
        assertNull(registry.byId(AiEngineId.APPLE_INTELLIGENCE))
    }

    @Test
    fun allReturnsInsertedOrder() {
        assertEquals(listOf(gemini, local), registry.all())
    }

    @Test
    fun availabilitiesMapsIds() = runTest {
        val result = registry.availabilities()
        assertEquals(
            mapOf(
                AiEngineId.GEMINI_NANO to AiAvailability.AVAILABLE,
                AiEngineId.LOCAL_LLM to AiAvailability.DOWNLOADABLE,
            ),
            result,
        )
    }
}
