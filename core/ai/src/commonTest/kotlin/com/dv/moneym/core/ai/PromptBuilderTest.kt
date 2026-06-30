package com.dv.moneym.core.ai

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptBuilderTest {

    private val system = "You are a financial assistant."

    @Test
    fun snapshotGroundingInjectsDataBlock() {
        val prompt = PromptBuilder.build(
            messages = listOf(ChatMessage(ChatRole.USER, "How much did I spend?")),
            grounding = Grounding.Snapshot("Total spend: 100 EUR"),
            systemInstruction = system,
        )
        assertContains(prompt, "Financial data:\nTotal spend: 100 EUR")
    }

    @Test
    fun historyOrderingAndRolePrefixesPreserved() {
        val prompt = PromptBuilder.build(
            messages = listOf(
                ChatMessage(ChatRole.USER, "Hi"),
                ChatMessage(ChatRole.ASSISTANT, "Hello"),
                ChatMessage(ChatRole.USER, "Bye"),
            ),
            grounding = Grounding.Snapshot("data"),
            systemInstruction = system,
        )
        val userHi = prompt.indexOf("User: Hi")
        val assistantHello = prompt.indexOf("Assistant: Hello")
        val userBye = prompt.indexOf("User: Bye")
        assertTrue(userHi in 0 until assistantHello)
        assertTrue(assistantHello in 0 until userBye)
    }

    @Test
    fun emptyHistoryYieldsSystemAndDataPreambleOnly() {
        val prompt = PromptBuilder.build(
            messages = emptyList(),
            grounding = Grounding.Snapshot("data"),
            systemInstruction = system,
        )
        assertEquals(
            "$system\n\nFinancial data:\ndata\n\nAssistant:",
            prompt,
        )
    }

    @Test
    fun toolsGroundingOmitsDataBlock() {
        val tool = AiTool(
            name = "totals",
            description = "Income and expense totals.",
            paramsSchema = "{}",
            invoke = { "" },
        )
        val prompt = PromptBuilder.build(
            messages = listOf(ChatMessage(ChatRole.USER, "Hi")),
            grounding = Grounding.Tools(listOf(tool)),
            systemInstruction = system,
        )
        assertFalse(prompt.contains("Financial data:"))
        assertContains(prompt, "Tools are available")
        assertContains(prompt, "totals")
        assertContains(prompt, AiToolCallParser.START_TAG)
    }
}
