package com.dv.moneym.feature.settings.overview

import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewBuiltInBlockIds
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.data.overview.OverviewLayoutPrefs
import org.jetbrains.compose.resources.StringResource
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_overview_block_averages
import moneym.feature.settings.generated.resources.settings_overview_block_budget_progress
import moneym.feature.settings.generated.resources.settings_overview_block_category_breakdown
import moneym.feature.settings.generated.resources.settings_overview_block_category_trends
import moneym.feature.settings.generated.resources.settings_overview_block_cumulative_spend
import moneym.feature.settings.generated.resources.settings_overview_block_monthly_income
import moneym.feature.settings.generated.resources.settings_overview_block_monthly_net
import moneym.feature.settings.generated.resources.settings_overview_block_monthly_spending
import moneym.feature.settings.generated.resources.settings_overview_block_totals

internal data class OverviewSettingsBuiltInBlockUiState(
    val blockId: OverviewBlockId,
    val titleRes: StringResource,
    val visible: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)

internal data class OverviewSettingsAiWidgetUiState(
    val id: Long,
    val title: String,
    val prompt: String? = null,
    val enabled: Boolean,
)

internal data class OverviewSettingsUiState(
    val builtInBlocks: List<OverviewSettingsBuiltInBlockUiState> = defaultOverviewBuiltInBlocks(),
    val aiWidgets: List<OverviewSettingsAiWidgetUiState> = emptyList(),
)

sealed interface OverviewSettingsIntent {
    data class SetBuiltInBlockVisible(
        val blockId: OverviewBlockId,
        val visible: Boolean,
    ) : OverviewSettingsIntent

    data class ReorderBuiltInBlocks(val orderedIds: List<OverviewBlockId>) : OverviewSettingsIntent
    data object ResetBuiltInBlocks : OverviewSettingsIntent
    data class SetAiWidgetEnabled(val widgetId: Long, val enabled: Boolean) : OverviewSettingsIntent
    data object CreateAiWidget : OverviewSettingsIntent
    data class EditAiWidget(val widgetId: Long) : OverviewSettingsIntent
}

internal sealed interface OverviewSettingsEffect {
    data class OpenAiWidgetBuilder(val widgetId: Long? = null) : OverviewSettingsEffect
}

internal fun defaultOverviewBuiltInBlocks(): List<OverviewSettingsBuiltInBlockUiState> =
    resolveOverviewBuiltInBlocks(
        OverviewLayoutPrefs(
            blocks = OverviewBuiltInBlockIds.all.mapIndexed { index, blockId ->
                OverviewLayoutBlock(blockId = blockId, sortOrder = index, visible = true)
            },
        ),
    )

internal fun resolveOverviewBuiltInBlocks(
    layoutPrefs: OverviewLayoutPrefs,
): List<OverviewSettingsBuiltInBlockUiState> {
    val persistedBlocks = layoutPrefs.blocks.associateBy { it.blockId }
    val orderedBlocks = OverviewBuiltInBlockIds.all.mapIndexed { defaultIndex, blockId ->
        val persisted = persistedBlocks[blockId]
        ResolvedBuiltInBlock(
            blockId = blockId,
            titleRes = overviewBuiltInBlockTitleRes(blockId),
            visible = persisted?.visible ?: true,
            sortOrder = persisted?.sortOrder ?: defaultIndex,
        )
    }

    return orderedBlocks
        .sortedWith(compareBy<ResolvedBuiltInBlock> { it.sortOrder }.thenBy {
            OverviewBuiltInBlockIds.all.indexOf(it.blockId)
        })
        .mapIndexed { index, block ->
            OverviewSettingsBuiltInBlockUiState(
                blockId = block.blockId,
                titleRes = block.titleRes,
                visible = block.visible,
                canMoveUp = index > 0,
                canMoveDown = index < orderedBlocks.lastIndex,
            )
        }
}

internal fun resolveOverviewAiWidgets(
    widgets: List<OverviewAiWidget>,
): List<OverviewSettingsAiWidgetUiState> = widgets.map { widget ->
    OverviewSettingsAiWidgetUiState(
        id = widget.id,
        title = widget.title,
        prompt = widget.prompt.takeIf { it.isNotBlank() },
        enabled = widget.enabled,
    )
}

private data class ResolvedBuiltInBlock(
    val blockId: OverviewBlockId,
    val titleRes: StringResource,
    val visible: Boolean,
    val sortOrder: Int,
)

private fun overviewBuiltInBlockTitleRes(blockId: OverviewBlockId): StringResource =
    when (blockId) {
        OverviewBuiltInBlockIds.Totals -> Res.string.settings_overview_block_totals
        OverviewBuiltInBlockIds.BudgetProgress -> Res.string.settings_overview_block_budget_progress
        OverviewBuiltInBlockIds.Averages -> Res.string.settings_overview_block_averages
        OverviewBuiltInBlockIds.CategoryBreakdown -> Res.string.settings_overview_block_category_breakdown
        OverviewBuiltInBlockIds.CumulativeSpend -> Res.string.settings_overview_block_cumulative_spend
        OverviewBuiltInBlockIds.MonthlySpend -> Res.string.settings_overview_block_monthly_spending
        OverviewBuiltInBlockIds.MonthlyIncome -> Res.string.settings_overview_block_monthly_income
        OverviewBuiltInBlockIds.MonthlyNet -> Res.string.settings_overview_block_monthly_net
        OverviewBuiltInBlockIds.CategoryTrends -> Res.string.settings_overview_block_category_trends
        else -> Res.string.settings_overview_block_totals
    }
