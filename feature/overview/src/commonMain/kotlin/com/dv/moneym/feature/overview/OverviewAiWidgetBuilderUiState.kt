package com.dv.moneym.feature.overview

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
)

sealed interface OverviewAiWidgetBuilderIntent {
    data class TitleChanged(val title: String) : OverviewAiWidgetBuilderIntent
    data class PromptChanged(val prompt: String) : OverviewAiWidgetBuilderIntent
    data class JsonChanged(val json: String) : OverviewAiWidgetBuilderIntent
    data object Generate : OverviewAiWidgetBuilderIntent
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
