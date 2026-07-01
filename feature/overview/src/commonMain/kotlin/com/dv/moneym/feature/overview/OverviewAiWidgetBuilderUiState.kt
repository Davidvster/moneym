package com.dv.moneym.feature.overview

import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.feature.aienginepicker.AiEnginePickerState

internal data class OverviewAiWidgetBuilderUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val prompt: String = "",
    val a2uiJson: String = "",
    val previewJson: String = "",
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val error: OverviewAiWidgetBuilderError? = null,
    val canSave: Boolean = false,
    val enginePicker: AiEnginePickerState = AiEnginePickerState(),
    val needsModelDownload: Boolean = false,
)

sealed interface OverviewAiWidgetBuilderIntent {
    data class TitleChanged(val title: String) : OverviewAiWidgetBuilderIntent
    data class PromptChanged(val prompt: String) : OverviewAiWidgetBuilderIntent
    data class JsonChanged(val json: String) : OverviewAiWidgetBuilderIntent
    data class EngineChanged(val id: AiEngineId) : OverviewAiWidgetBuilderIntent
    data object Generate : OverviewAiWidgetBuilderIntent
    data object RefreshEngines : OverviewAiWidgetBuilderIntent
    data object Save : OverviewAiWidgetBuilderIntent
    data object ClearError : OverviewAiWidgetBuilderIntent
}

internal enum class OverviewAiWidgetBuilderError {
    MissingEngine,
    EngineUnavailable,
    GenerationFailed,
    InvalidJson,
    EmptyPrompt,
    EmptyTitle,
}

internal sealed interface OverviewAiWidgetBuilderEffect {
    data object Saved : OverviewAiWidgetBuilderEffect
}
