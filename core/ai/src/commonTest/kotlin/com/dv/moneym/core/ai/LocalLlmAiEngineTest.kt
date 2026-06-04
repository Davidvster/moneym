package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalLlmAiEngineTest {

    private class FakeLocalLlmRunner(
        private val loads: Boolean,
        private val deltas: List<String> = emptyList(),
    ) : LocalLlmRunner {
        private var loaded = false
        override suspend fun isModelLoaded(): Boolean = loaded
        override suspend fun loadModel(path: String): Boolean {
            loaded = loads
            return loads
        }
        override fun streamReply(prompt: String): Flow<String> = flowOf(*deltas.toTypedArray())
    }

    private val messages = listOf(ChatMessage(ChatRole.USER, "How much did I spend?"))
    private val grounding = Grounding.Snapshot("Total spend: 100 EUR")

    @Test
    fun noActivePathIsDownloadable() = runTest {
        val engine = LocalLlmAiEngine(FakeLocalLlmRunner(loads = true)) { null }
        assertEquals(AiAvailability.DOWNLOADABLE, engine.availability())
    }

    @Test
    fun activePathAndSuccessfulLoadIsAvailable() = runTest {
        val engine = LocalLlmAiEngine(FakeLocalLlmRunner(loads = true)) { "/models/m.task" }
        assertEquals(AiAvailability.AVAILABLE, engine.availability())
    }

    @Test
    fun activePathAndFailedLoadIsUnavailable() = runTest {
        val engine = LocalLlmAiEngine(FakeLocalLlmRunner(loads = false)) { "/models/m.task" }
        assertEquals(AiAvailability.UNAVAILABLE, engine.availability())
    }

    @Test
    fun streamReplyEmitsRunnerDeltas() = runTest {
        val engine = LocalLlmAiEngine(
            FakeLocalLlmRunner(loads = true, deltas = listOf("Hello", " world")),
        ) { "/models/m.task" }
        val collected = engine.streamReply(messages, grounding).toList()
        assertEquals(listOf("Hello", " world"), collected)
    }
}
