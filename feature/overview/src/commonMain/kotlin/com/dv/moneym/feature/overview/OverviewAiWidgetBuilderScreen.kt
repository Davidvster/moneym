package com.dv.moneym.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.feature.overview.a2ui.BuildOverviewWidgetContextUseCase
import com.dv.moneym.feature.overview.a2ui.OverviewA2UiWidgetCard
import com.dv.moneym.feature.overview.a2ui.sampleA2UiJson
import kotlinx.serialization.Serializable
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_error_empty_prompt
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_error_empty_title
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_error_generation
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_error_invalid_json
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_error_no_engine
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_error_unavailable
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_generate
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_json
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_json_placeholder
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_preview
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_prompt
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_prompt_placeholder
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_save
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_title
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_title_field
import moneym.feature.overview.generated.resources.overview_ai_widget_builder_title_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class OverviewAiWidgetBuilderKey(val widgetId: Long? = null) : NavKey

fun EntryProviderScope<NavKey>.overviewAiWidgetBuilderEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<OverviewAiWidgetBuilderKey>(metadata = metadata) { key ->
    OverviewAiWidgetBuilderScreen(
        widgetId = key.widgetId,
        onBack = onBack,
    )
}

@Composable
private fun OverviewAiWidgetBuilderScreen(
    widgetId: Long?,
    onBack: () -> Unit,
    viewModel: OverviewAiWidgetBuilderViewModel = koinViewModel(parameters = { parametersOf(widgetId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OverviewAiWidgetBuilderEffect.Saved -> onBack()
            }
        }
    }

    OverviewAiWidgetBuilderContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
private fun OverviewAiWidgetBuilderContent(
    state: OverviewAiWidgetBuilderUiState,
    onIntent: (OverviewAiWidgetBuilderIntent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.overview_ai_widget_builder_title),
            onBack = onBack,
        )
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(space.padding_2x)),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            MmField(
                value = state.title,
                onValueChange = { onIntent(OverviewAiWidgetBuilderIntent.TitleChanged(it)) },
                label = stringResource(Res.string.overview_ai_widget_builder_title_field),
                placeholder = stringResource(Res.string.overview_ai_widget_builder_title_placeholder),
            )
            MmField(
                value = state.prompt,
                onValueChange = { onIntent(OverviewAiWidgetBuilderIntent.PromptChanged(it)) },
                label = stringResource(Res.string.overview_ai_widget_builder_prompt),
                placeholder = stringResource(Res.string.overview_ai_widget_builder_prompt_placeholder),
                singleLine = false,
            )
            MmButton(
                text = stringResource(Res.string.overview_ai_widget_builder_generate),
                onClick = { onIntent(OverviewAiWidgetBuilderIntent.Generate) },
                enabled = !state.isGenerating && !state.isSaving,
                variant = MmButtonVariant.Primary,
                fullWidth = true,
            )
            MmField(
                value = state.a2uiJson,
                onValueChange = { onIntent(OverviewAiWidgetBuilderIntent.JsonChanged(it)) },
                label = stringResource(Res.string.overview_ai_widget_builder_json),
                placeholder = stringResource(Res.string.overview_ai_widget_builder_json_placeholder),
                singleLine = false,
            )
            state.error?.let {
                Text(
                    text = stringResource(it.messageRes()),
                    style = MM.type.body,
                    color = colors.danger,
                )
            }
            Text(
                text = stringResource(Res.string.overview_ai_widget_builder_preview),
                style = MM.type.title3,
                color = colors.text,
            )
            OverviewA2UiWidgetCard(
                json = state.previewJson.ifBlank { state.a2uiJson },
                context = BuildOverviewWidgetContextUseCase.sample(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(space.padding_1x))
            MmButton(
                text = stringResource(Res.string.overview_ai_widget_builder_save),
                onClick = { onIntent(OverviewAiWidgetBuilderIntent.Save) },
                enabled = state.canSave && !state.isGenerating && !state.isSaving,
                variant = MmButtonVariant.Primary,
                fullWidth = true,
            )
        }
    }
}

private fun OverviewAiWidgetBuilderError.messageRes() = when (this) {
    OverviewAiWidgetBuilderError.MissingEngine -> Res.string.overview_ai_widget_builder_error_no_engine
    OverviewAiWidgetBuilderError.EngineUnavailable -> Res.string.overview_ai_widget_builder_error_unavailable
    OverviewAiWidgetBuilderError.GenerationFailed -> Res.string.overview_ai_widget_builder_error_generation
    OverviewAiWidgetBuilderError.InvalidJson -> Res.string.overview_ai_widget_builder_error_invalid_json
    OverviewAiWidgetBuilderError.EmptyPrompt -> Res.string.overview_ai_widget_builder_error_empty_prompt
    OverviewAiWidgetBuilderError.EmptyTitle -> Res.string.overview_ai_widget_builder_error_empty_title
}

@Preview
@Composable
private fun OverviewAiWidgetBuilderContentPreview_Light() {
    MoneyMTheme(darkTheme = false) {
        OverviewAiWidgetBuilderContent(
            state = OverviewAiWidgetBuilderUiState(
                isLoading = false,
                title = "Cash flow",
                prompt = "Show income, expenses, and the top spending categories.",
                a2uiJson = sampleA2UiJson,
                previewJson = sampleA2UiJson,
                canSave = true,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun OverviewAiWidgetBuilderContentPreview_Dark() {
    MoneyMTheme(darkTheme = true) {
        OverviewAiWidgetBuilderContent(
            state = OverviewAiWidgetBuilderUiState(
                isLoading = false,
                title = "Cash flow",
                prompt = "Show income, expenses, and the top spending categories.",
                a2uiJson = sampleA2UiJson,
                previewJson = sampleA2UiJson,
                canSave = true,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
