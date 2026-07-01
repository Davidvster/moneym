package com.dv.moneym.feature.overview.a2ui

import kotlin.test.Test
import kotlin.test.assertTrue

class BuildOverviewWidgetPromptUseCaseTest {
    @Test
    fun promptIncludesComponentCatalogAndOverviewDataKeys() {
        val prompt = BuildOverviewWidgetPromptUseCase()(
            userPrompt = "Show a cash flow summary",
            title = "Cash flow",
        )

        assertTrue(prompt.contains("Allowed component types"))
        assertTrue(prompt.contains("moneyMetric"))
        assertTrue(prompt.contains("Allowed bindings"))
        assertTrue(prompt.contains("income"))
        assertTrue(prompt.contains("category.expenseBreakdown"))
        assertTrue(prompt.contains("Return only valid JSON"))
    }
}
