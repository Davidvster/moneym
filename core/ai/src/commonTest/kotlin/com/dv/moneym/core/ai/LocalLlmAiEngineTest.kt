package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalLlmAiEngineTest {

    private class FakeLocalLlmRunner(
        private val loads: Boolean,
        private val deltas: List<String> = emptyList(),
        private val replies: ArrayDeque<List<String>> = ArrayDeque(),
    ) : LocalLlmRunner {
        private var loaded = false
        val prompts = mutableListOf<String>()
        override suspend fun isModelLoaded(): Boolean = loaded
        override suspend fun loadModel(path: String): Boolean {
            loaded = loads
            return loads
        }
        override fun streamReply(prompt: String): Flow<String> {
            prompts += prompt
            val next = replies.removeFirstOrNull() ?: deltas
            return flowOf(*next.toTypedArray())
        }
    }

    private val messages = listOf(ChatMessage(ChatRole.USER, "How much did I spend?"))
    private val grounding = Grounding.Snapshot("Total spend: 100 EUR")
    private val totalsTool = AiTool(
        name = "totals",
        description = "Return totals",
        paramsSchema = """{"year":"integer"}""",
    ) { args -> "Total spend: ${args["year"] ?: "all"} EUR" }

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
    fun localEngineSupportsTools() {
        val engine = LocalLlmAiEngine(FakeLocalLlmRunner(loads = true)) { "/models/m.task" }
        assertTrue(engine.supportsTools)
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

    @Test
    fun toolsGroundingInvokesToolAndAnswersFromResult() = runTest {
        val runner = FakeLocalLlmRunner(
            loads = true,
            replies = ArrayDeque(
                listOf(
                    listOf("""TOOL_CALL: totals {"year":"2026"}"""),
                    listOf("You spent 2026 EUR."),
                ),
            ),
        )
        val engine = LocalLlmAiEngine(runner) { "/models/m.task" }

        val collected = engine.streamReply(messages, Grounding.Tools(listOf(totalsTool))).toList()

        assertEquals("You spent 2026 EUR.", collected.joinToString(""))
        assertEquals(2, runner.prompts.size)
        assertTrue(runner.prompts.last().contains("TOOL_RESULT totals:\nTotal spend: 2026 EUR"))
    }

    @Test
    fun toolsGroundingFeedsUnknownToolErrorBackToModel() = runTest {
        val runner = FakeLocalLlmRunner(
            loads = true,
            replies = ArrayDeque(
                listOf(
                    listOf("""TOOL_CALL: missing {"year":"2026"}"""),
                    listOf("I cannot use that tool."),
                ),
            ),
        )
        val engine = LocalLlmAiEngine(runner) { "/models/m.task" }

        val collected = engine.streamReply(messages, Grounding.Tools(listOf(totalsTool))).toList()

        assertEquals("I cannot use that tool.", collected.joinToString(""))
        assertTrue(runner.prompts.last().contains("Tool error: unknown tool."))
    }

    @Test
    fun toolsGroundingDoesNotEmitRawToolCall() = runTest {
        val runner = FakeLocalLlmRunner(
            loads = true,
            replies = ArrayDeque(
                listOf(
                    listOf("""TOOL_CALL: totals {"year":"2026"}"""),
                    listOf("Final answer"),
                ),
            ),
        )
        val engine = LocalLlmAiEngine(runner) { "/models/m.task" }

        val collected = engine.streamReply(messages, Grounding.Tools(listOf(totalsTool))).toList()

        assertEquals("Final answer", collected.joinToString(""))
        assertTrue(collected.none { it.contains("TOOL_CALL") })
    }
}
