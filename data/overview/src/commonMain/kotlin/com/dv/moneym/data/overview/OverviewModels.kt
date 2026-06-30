package com.dv.moneym.data.overview

import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
value class OverviewBlockId(val value: String)

object OverviewBuiltInBlockIds {
    val Totals = OverviewBlockId("totals")
    val BudgetProgress = OverviewBlockId("budget_progress")
    val Averages = OverviewBlockId("averages")
    val CategoryBreakdown = OverviewBlockId("category_breakdown")
    val CumulativeSpend = OverviewBlockId("cumulative_spend")
    val MonthlySpend = OverviewBlockId("monthly_spend")
    val MonthlyIncome = OverviewBlockId("monthly_income")
    val MonthlyNet = OverviewBlockId("monthly_net")
    val CategoryTrends = OverviewBlockId("category_trends")

    val all = listOf(
        Totals,
        BudgetProgress,
        Averages,
        CategoryBreakdown,
        CumulativeSpend,
        MonthlySpend,
        MonthlyIncome,
        MonthlyNet,
        CategoryTrends,
    )
}

data class OverviewLayoutBlock(
    val blockId: OverviewBlockId,
    val sortOrder: Int,
    val visible: Boolean,
)

data class OverviewLayoutPrefs(
    val blocks: List<OverviewLayoutBlock> = emptyList(),
)

data class OverviewAiWidget(
    val id: Long = 0,
    val title: String,
    val prompt: String,
    val a2uiJson: String,
    val enabled: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastGeneratedAt: Instant? = null,
    val lastGenerationEngineId: String? = null,
)
