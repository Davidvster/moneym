package com.dv.moneym.feature.settings.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.data.overview.OverviewRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverviewSettingsViewModel(
    private val overviewRepository: OverviewRepository,
) : ViewModel() {

    private val _effects = Channel<OverviewSettingsEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    internal val state: StateFlow<OverviewSettingsUiState> = combine(
        overviewRepository.observeLayoutPrefs(),
        overviewRepository.observeAiWidgets(),
    ) { layoutPrefs, widgets ->
        OverviewSettingsUiState(
            builtInBlocks = resolveOverviewBuiltInBlocks(layoutPrefs),
            aiWidgets = resolveOverviewAiWidgets(widgets),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = OverviewSettingsUiState(),
    )

    fun onIntent(intent: OverviewSettingsIntent) {
        when (intent) {
            is OverviewSettingsIntent.SetBuiltInBlockVisible ->
                setBuiltInBlockVisible(intent.blockId, intent.visible)

            is OverviewSettingsIntent.ReorderBuiltInBlocks ->
                reorderBuiltInBlocks(intent.orderedIds)

            OverviewSettingsIntent.ResetBuiltInBlocks -> resetBuiltInBlocks()
            is OverviewSettingsIntent.SetAiWidgetEnabled ->
                setAiWidgetEnabled(intent.widgetId, intent.enabled)

            OverviewSettingsIntent.CreateAiWidget -> emitAiWidgetBuilderEffect(null)
            is OverviewSettingsIntent.EditAiWidget -> emitAiWidgetBuilderEffect(intent.widgetId)
        }
    }

    private fun setBuiltInBlockVisible(blockId: OverviewBlockId, visible: Boolean) {
        val updatedBlocks = state.value.builtInBlocks.map {
            if (it.blockId == blockId) it.copy(visible = visible) else it
        }
        persistBuiltInBlocks(updatedBlocks)
    }

    private fun reorderBuiltInBlocks(orderedIds: List<OverviewBlockId>) {
        val currentById = state.value.builtInBlocks.associateBy { it.blockId }
        val reordered = orderedIds.mapNotNull { currentById[it] }
        if (reordered.size != currentById.size) return
        persistBuiltInBlocks(reordered)
    }

    private fun resetBuiltInBlocks() {
        persistBuiltInBlocks(defaultOverviewBuiltInBlocks())
    }

    private fun setAiWidgetEnabled(widgetId: Long, enabled: Boolean) {
        viewModelScope.launch {
            overviewRepository.setAiWidgetEnabled(widgetId, enabled)
        }
    }

    private fun emitAiWidgetBuilderEffect(widgetId: Long?) {
        viewModelScope.launch {
            _effects.send(OverviewSettingsEffect.OpenAiWidgetBuilder(widgetId))
        }
    }

    private fun persistBuiltInBlocks(blocks: List<OverviewSettingsBuiltInBlockUiState>) {
        viewModelScope.launch {
            overviewRepository.replaceLayout(
                blocks.mapIndexed { index, block ->
                    OverviewLayoutBlock(
                        blockId = block.blockId,
                        sortOrder = index,
                        visible = block.visible,
                    )
                },
            )
        }
    }
}
