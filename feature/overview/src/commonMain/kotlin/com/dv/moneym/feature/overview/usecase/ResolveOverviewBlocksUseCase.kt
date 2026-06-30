package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewBuiltInBlockIds
import com.dv.moneym.data.overview.OverviewLayoutPrefs
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.page.OverviewPageUiState

internal sealed interface OverviewResolvedBlock {
    val blockId: OverviewBlockId

    data class BuiltIn(override val blockId: OverviewBlockId) : OverviewResolvedBlock

    data class AiWidget(val widget: OverviewAiWidget) : OverviewResolvedBlock {
        override val blockId: OverviewBlockId = OverviewBlockId("ai_widget_${widget.id}")
    }
}

class ResolveOverviewBlocksUseCase {
    internal operator fun invoke(
        period: OverviewPeriod,
        spendingFilter: SpendingFilter,
        layoutPrefs: OverviewLayoutPrefs,
        pageState: OverviewPageUiState,
        aiWidgets: List<OverviewAiWidget>,
    ): List<OverviewResolvedBlock> {
        val eligibleBuiltIns = defaultBuiltIns(
            period = period,
            spendingFilter = spendingFilter,
            pageState = pageState,
        )
        val eligibleIds = eligibleBuiltIns.toSet()
        val persistedKnownBlocks = layoutPrefs.blocks
            .filter { it.blockId in OverviewBuiltInBlockIds.all }
            .distinctBy { it.blockId }
        val hiddenIds = persistedKnownBlocks
            .filterNot { it.visible }
            .map { it.blockId }
            .toSet()
        val persistedOrder = persistedKnownBlocks
            .filter { it.visible && it.blockId in eligibleIds }
            .sortedBy { it.sortOrder }
            .map { it.blockId }
        val remainingDefaults = eligibleBuiltIns.filterNot { it in hiddenIds || it in persistedOrder }

        return (persistedOrder + remainingDefaults)
            .map { OverviewResolvedBlock.BuiltIn(it) } +
            aiWidgets
                .filter { it.enabled }
                .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
                .map { OverviewResolvedBlock.AiWidget(it) }
    }

    private fun defaultBuiltIns(
        period: OverviewPeriod,
        spendingFilter: SpendingFilter,
        pageState: OverviewPageUiState,
    ): List<OverviewBlockId> {
        val base = buildList {
            add(OverviewBuiltInBlockIds.Totals)
            if (pageState.budgetProgress.isNotEmpty()) {
                add(OverviewBuiltInBlockIds.BudgetProgress)
            }
            add(OverviewBuiltInBlockIds.Averages)
            add(OverviewBuiltInBlockIds.CategoryBreakdown)
        }
        return base + when (period) {
            is OverviewPeriod.Month -> listOf(
                OverviewBuiltInBlockIds.CumulativeSpend,
                OverviewBuiltInBlockIds.CategoryTrends,
            )

            is OverviewPeriod.Year -> buildList {
                if (spendingFilter != SpendingFilter.Income) {
                    add(OverviewBuiltInBlockIds.MonthlySpend)
                }
                if (spendingFilter != SpendingFilter.Expenses) {
                    add(OverviewBuiltInBlockIds.MonthlyIncome)
                }
                if (spendingFilter == SpendingFilter.All) {
                    add(OverviewBuiltInBlockIds.MonthlyNet)
                }
                add(OverviewBuiltInBlockIds.CategoryTrends)
            }

            is OverviewPeriod.DateRange -> {
                if (rangeTrendsExist(spendingFilter, pageState)) {
                    listOf(OverviewBuiltInBlockIds.CategoryTrends)
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun rangeTrendsExist(
        spendingFilter: SpendingFilter,
        pageState: OverviewPageUiState,
    ): Boolean = when (spendingFilter) {
        SpendingFilter.Expenses -> pageState.categoryDailyTrend.isNotEmpty()
        SpendingFilter.Income -> pageState.categoryIncomeDailyTrend.isNotEmpty()
        SpendingFilter.All -> pageState.categoryDailyTrend.isNotEmpty() ||
                pageState.categoryIncomeDailyTrend.isNotEmpty()
    }
}
