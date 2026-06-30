package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class QueueAiEngine(
    private val replies: ArrayDeque<String>,
) : AiEngine {
    override val id = AiEngineId.LOCAL_LLM
    override val supportsTools = false
    val requests = mutableListOf<List<ChatMessage>>()

    override suspend fun availability(): AiAvailability = AiAvailability.AVAILABLE

    override fun streamReply(
        messages: List<ChatMessage>,
        grounding: Grounding,
        responseLanguage: String?,
    ): Flow<String> {
        requests += messages
        return listOf(replies.removeFirst()).asFlow()
    }
}

class AppManagedToolLoopTest {

    @Test
    fun parserReadsValidToolCall() {
        val result = AiToolCallParser.parse(
            """${AiToolCallParser.START_TAG}{"name":"topExpenses","params":{"n":3}}${AiToolCallParser.END_TAG}""",
        )

        val call = assertIs<AiToolCallParseResult.Call>(result).call
        assertEquals("topExpenses", call.name)
        assertEquals(mapOf("n" to "3"), call.params)
    }

    @Test
    fun parserReportsInvalidJson() {
        val result = AiToolCallParser.parse(
            """${AiToolCallParser.START_TAG}{"name":${AiToolCallParser.END_TAG}""",
        )

        assertIs<AiToolCallParseResult.Invalid>(result)
    }

    @Test
    fun parserReportsInvalidParams() {
        val result = AiToolCallParser.parse(
            """${AiToolCallParser.START_TAG}{"name":"totals","params":[]}${AiToolCallParser.END_TAG}""",
        )

        assertIs<AiToolCallParseResult.Invalid>(result)
    }

    @Test
    fun unknownToolProducesGracefulNote() = runTest {
        val engine = QueueAiEngine(
            ArrayDeque(
                listOf(
                    """${AiToolCallParser.START_TAG}{"name":"missing","params":{}}${AiToolCallParser.END_TAG}""",
                ),
            ),
        )

        val output = AppManagedToolLoop().streamReply(
            engine = engine,
            messages = listOf(ChatMessage(ChatRole.USER, "What changed?")),
            tools = listOf(tool("totals", "Income 10")),
        ).toList().joinToString("")

        assertTrue(output.contains("not available"), output)
    }

    @Test
    fun toolResultIsFedBackIntoFinalAnswer() = runTest {
        val engine = QueueAiEngine(
            ArrayDeque(
                listOf(
                    """${AiToolCallParser.START_TAG}{"name":"totals","params":{}}${AiToolCallParser.END_TAG}""",
                    "You spent 42 EUR.",
                ),
            ),
        )

        val output = AppManagedToolLoop().streamReply(
            engine = engine,
            messages = listOf(ChatMessage(ChatRole.USER, "How much did I spend?")),
            tools = listOf(tool("totals", "Expense 42.00 EUR")),
        ).toList().joinToString("")

        assertEquals("You spent 42 EUR.", output)
        assertEquals(2, engine.requests.size)
        assertTrue(engine.requests[1].last().content.contains("Expense 42.00 EUR"))
    }

    @Test
    fun functionTagToolCallIsExecutedAndFollowUpAnswerIsEmitted() = runTest {
        val engine = QueueAiEngine(
            ArrayDeque(
                listOf(
                    """<searchTransactions>{"q":" groceries ","type":"expense"}</searchTransactions>""",
                    "Groceries were 12 EUR.",
                ),
            ),
        )
        var receivedParams = emptyMap<String, String>()

        val output = AppManagedToolLoop().streamReply(
            engine = engine,
            messages = listOf(ChatMessage(ChatRole.USER, "Find groceries")),
            tools = listOf(
                AiTool(
                    name = "searchTransactions",
                    description = "Search transactions",
                    paramsSchema = "{}",
                    invoke = { params ->
                        receivedParams = params
                        "2026-05-01 Groceries -12.00 EUR"
                    },
                ),
            ),
        ).toList().joinToString("")

        assertEquals("Groceries were 12 EUR.", output)
        assertEquals(mapOf("q" to "groceries", "type" to "expense"), receivedParams)
        assertTrue(engine.requests[1].last().content.contains("2026-05-01 Groceries -12.00 EUR"))
    }

    @Test
    fun unknownFunctionTagIsNotReturnedAsSuccessfulAnswer() = runTest {
        val engine = QueueAiEngine(ArrayDeque(listOf("""<missingTool>{"q":"groceries"}</missingTool>""")))

        val output = AppManagedToolLoop().streamReply(
            engine = engine,
            messages = listOf(ChatMessage(ChatRole.USER, "Find groceries")),
            tools = listOf(tool("searchTransactions", "should not run")),
        ).toList().joinToString("")

        assertTrue(output.contains("not available"), output)
        assertFalse(output.contains("<missingTool>"), output)
    }

    @Test
    fun maxIterationsProduceGracefulNote() = runTest {
        val engine = QueueAiEngine(
            ArrayDeque(
                listOf(
                    """${AiToolCallParser.START_TAG}{"name":"totals","params":{}}${AiToolCallParser.END_TAG}""",
                    """${AiToolCallParser.START_TAG}{"name":"totals","params":{}}${AiToolCallParser.END_TAG}""",
                ),
            ),
        )

        val output = AppManagedToolLoop(maxIterations = 2).streamReply(
            engine = engine,
            messages = listOf(ChatMessage(ChatRole.USER, "Loop?")),
            tools = listOf(tool("totals", "Expense 42.00 EUR")),
        ).toList().joinToString("")

        assertTrue(output.contains("iteration limit"), output)
        assertTrue(output.contains("Expense 42.00 EUR"), output)
        assertFalse(output.contains(AiToolCallParser.START_TAG), output)
    }

    private fun tool(name: String, result: String) = AiTool(
        name = name,
        description = "A tool",
        paramsSchema = "{}",
        invoke = { result },
    )
}
