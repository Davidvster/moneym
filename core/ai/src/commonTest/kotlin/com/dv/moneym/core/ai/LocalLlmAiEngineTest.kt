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
    fun activePathIsAvailableWithoutLoading() = runTest {
        // availability() must not trigger a native load; an active downloaded model is reported
        // available and the actual load is deferred to streamReply.
        val engine = LocalLlmAiEngine(FakeLocalLlmRunner(loads = false)) { "/models/m.task" }
        assertEquals(AiAvailability.AVAILABLE, engine.availability())
    }

    @Test
    fun streamReplyEmitsRunnerDeltas() = runTest {
        val engine = LocalLlmAiEngine(
            FakeLocalLlmRunner(loads = true, deltas = listOf("Hello", " world")),
        ) { "/models/m.task" }
        val collected = engine.streamReply(messages, grounding).toList()
        assertEquals(listOf("Hello", " world"), collected)
    }

    @Test
    fun streamReplyStopsWhenModelStartsNewTurn() = runTest {
        // A model with no native stop token keeps the hand-rolled transcript going by writing the
        // next "User:" turn and answering itself. The engine must cut the stream at that boundary.
        val engine = LocalLlmAiEngine(
            FakeLocalLlmRunner(
                loads = true,
                deltas = listOf("You spent 100 EUR.", "\nUser:", " and next month?", "\nAssistant:", " 200"),
            ),
        ) { "/models/m.task" }
        val collected = engine.streamReply(messages, grounding).toList()
        assertEquals("You spent 100 EUR.", collected.joinToString(""))
    }

    @Test
    fun streamReplyHoldsBackPartialStopFragmentThenFlushes() = runTest {
        // A lone trailing "\n" might still grow into "\nUser:"; if it does not, it must be emitted.
        val engine = LocalLlmAiEngine(
            FakeLocalLlmRunner(loads = true, deltas = listOf("Line one", "\n", "Line two")),
        ) { "/models/m.task" }
        val collected = engine.streamReply(messages, grounding).toList()
        assertEquals("Line one\nLine two", collected.joinToString(""))
    }
}
