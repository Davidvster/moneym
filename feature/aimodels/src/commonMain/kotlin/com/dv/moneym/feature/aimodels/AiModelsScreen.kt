package com.dv.moneym.feature.aimodels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmRadio
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.ui.KeepScreenOn
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmDeleteSheet
import com.dv.moneym.core.ui.ScreenHeader
import kotlinx.serialization.Serializable
import moneym.feature.aimodels.generated.resources.Res
import moneym.feature.aimodels.generated.resources.ai_model_qwen25_1_5b
import moneym.feature.aimodels.generated.resources.ai_model_smollm2_135m
import moneym.feature.aimodels.generated.resources.ai_model_gemma4_e2b_it
import moneym.feature.aimodels.generated.resources.ai_models_active
import moneym.feature.aimodels.generated.resources.ai_models_cancel
import moneym.feature.aimodels.generated.resources.ai_models_delete
import moneym.feature.aimodels.generated.resources.ai_models_delete_body
import moneym.feature.aimodels.generated.resources.ai_models_delete_title
import moneym.feature.aimodels.generated.resources.ai_models_download
import moneym.feature.aimodels.generated.resources.ai_models_error_delete
import moneym.feature.aimodels.generated.resources.ai_models_error_download
import moneym.feature.aimodels.generated.resources.ai_models_error_provider_delete
import moneym.feature.aimodels.generated.resources.ai_models_error_provider_refresh
import moneym.feature.aimodels.generated.resources.ai_models_error_provider_save
import moneym.feature.aimodels.generated.resources.ai_models_error_provider_select
import moneym.feature.aimodels.generated.resources.ai_models_error_provider_test
import moneym.feature.aimodels.generated.resources.ai_models_eta_hours
import moneym.feature.aimodels.generated.resources.ai_models_eta_minutes
import moneym.feature.aimodels.generated.resources.ai_models_eta_seconds
import moneym.feature.aimodels.generated.resources.ai_models_keep_awake
import moneym.feature.aimodels.generated.resources.ai_models_local_section
import moneym.feature.aimodels.generated.resources.ai_models_provider_configured
import moneym.feature.aimodels.generated.resources.ai_models_provider_delete_key
import moneym.feature.aimodels.generated.resources.ai_models_provider_key_label
import moneym.feature.aimodels.generated.resources.ai_models_provider_key_placeholder
import moneym.feature.aimodels.generated.resources.ai_models_provider_missing
import moneym.feature.aimodels.generated.resources.ai_models_provider_model_label
import moneym.feature.aimodels.generated.resources.ai_models_provider_refresh
import moneym.feature.aimodels.generated.resources.ai_models_provider_save_key
import moneym.feature.aimodels.generated.resources.ai_models_provider_section
import moneym.feature.aimodels.generated.resources.ai_models_provider_test
import moneym.feature.aimodels.generated.resources.ai_models_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object AiModelsKey : ModalKey

fun EntryProviderScope<NavKey>.aiModelsEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<AiModelsKey>(metadata = metadata) {
    AiModelsScreen(onBack = onBack)
}

@Composable
private fun AiModelsScreen(
    onBack: () -> Unit,
    viewModel: AiModelsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AiModelsContent(state = state, onBack = onBack, onIntent = viewModel::onIntent)
}

@Composable
private fun AiModelsContent(
    state: AiModelsUiState,
    onBack: () -> Unit,
    onIntent: (AiModelsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val isDownloading = state.models.any { it.status is ModelStatus.Downloading }

    KeepScreenOn(isDownloading)

    if (state.pendingDeleteId != null) {
        MmDeleteSheet(
            title = stringResource(Res.string.ai_models_delete_title),
            body = stringResource(Res.string.ai_models_delete_body),
            cancelText = stringResource(Res.string.ai_models_cancel),
            confirmText = stringResource(Res.string.ai_models_delete),
            onConfirm = { onIntent(AiModelsIntent.DeleteConfirmed) },
            onCancel = { onIntent(AiModelsIntent.DeleteCancelled) },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(title = stringResource(Res.string.ai_models_title), onBack = onBack)

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MM.type.caption,
                color = colors.danger,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIntent(AiModelsIntent.ClearError) }
                    .padding(horizontal = space.padding_2x, vertical = space.padding_1x),
            )
        }

        if (isDownloading) {
            Text(
                text = stringResource(Res.string.ai_models_keep_awake),
                style = MM.type.caption,
                color = colors.text2,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = space.padding_2x, vertical = space.padding_1_5x),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(space.padding_2x),
            verticalArrangement = Arrangement.spacedBy(space.padding_1_5x),
        ) {
            item {
                SectionLabel(text = stringResource(Res.string.ai_models_local_section))
            }
            items(state.models, key = { it.id }) { row ->
                ModelRow(row = row, onIntent = onIntent)
            }
            item {
                SectionLabel(text = stringResource(Res.string.ai_models_provider_section))
            }
            items(state.providers, key = { it.id.name }) { row ->
                ProviderRow(row = row, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MM.type.caption,
        color = MM.colors.text2,
        modifier = Modifier.padding(top = MM.dimen.padding_1x),
    )
}

@Composable
private fun ModelRow(row: ModelRowUi, onIntent: (AiModelsIntent) -> Unit) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    MmCard(padded = true, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.padding_1_5x),
            ) {
                val status = row.status
                if (status == ModelStatus.Downloaded) {
                    MmRadio(selected = false, onClick = { onIntent(AiModelsIntent.SetActive(row.id)) })
                } else if (status == ModelStatus.Active) {
                    MmRadio(selected = true)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                    ) {
                        Text(
                            text = stringResource(row.displayNameKey.modelNameRes()),
                            style = type.body,
                            color = colors.text,
                        )
                        if (status == ModelStatus.Active) {
                            Text(
                                text = stringResource(Res.string.ai_models_active),
                                style = type.caption,
                                color = colors.accent,
                            )
                        }
                    }
                    Text(text = row.sizeLabel, style = type.caption, color = colors.text2)
                }

                when (status) {
                    ModelStatus.NotDownloaded -> MmButton(
                        text = stringResource(Res.string.ai_models_download),
                        onClick = { onIntent(AiModelsIntent.Download(row.id)) },
                        variant = MmButtonVariant.Secondary,
                        size = MmButtonSize.Sm,
                    )

                    is ModelStatus.Downloading -> MmButton(
                        text = stringResource(Res.string.ai_models_cancel),
                        onClick = { onIntent(AiModelsIntent.Cancel(row.id)) },
                        variant = MmButtonVariant.Ghost,
                        size = MmButtonSize.Sm,
                    )

                    ModelStatus.Downloaded, ModelStatus.Active -> MmIconButton(
                        icon = Icon.Trash.imageVector,
                        onClick = { onIntent(AiModelsIntent.Delete(row.id)) },
                        variant = MmIconButtonVariant.Danger,
                        contentDescription = stringResource(Res.string.ai_models_delete),
                    )
                }
            }

            (row.status as? ModelStatus.Downloading)?.let { status ->
                LinearProgressIndicator(
                    progress = { status.progress },
                    color = colors.accent,
                    trackColor = colors.surface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = status.speedText,
                        style = type.caption,
                        color = colors.text2,
                        maxLines = 1,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = status.sizeText,
                        style = type.caption,
                        color = colors.text2,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = status.percentText,
                        style = type.caption,
                        color = colors.text,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(start = space.padding_1x)
                            .width(48.dp),
                    )
                }
                status.etaSeconds?.let { seconds ->
                    Text(
                        text = etaLabel(seconds),
                        style = type.caption,
                        color = colors.text3,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(row: ProviderRowUi, onIntent: (AiModelsIntent) -> Unit) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    var apiKeyVisible by remember(row.id) { mutableStateOf(false) }

    MmCard(padded = true, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.displayName, style = type.body, color = colors.text)
                    Text(
                        text = stringResource(
                            if (row.configured) {
                                Res.string.ai_models_provider_configured
                            } else {
                                Res.string.ai_models_provider_missing
                            },
                        ),
                        style = type.caption,
                        color = if (row.configured) colors.accent else colors.text2,
                    )
                }
                MmButton(
                    text = stringResource(Res.string.ai_models_provider_test),
                    onClick = { onIntent(AiModelsIntent.TestConnection(row.id)) },
                    enabled = row.configured && !row.isTesting,
                    variant = MmButtonVariant.Secondary,
                    size = MmButtonSize.Sm,
                )
            }

            MmField(
                value = row.apiKeyInput,
                onValueChange = { onIntent(AiModelsIntent.ApiKeyChanged(row.id, it)) },
                label = stringResource(Res.string.ai_models_provider_key_label),
                placeholder = stringResource(Res.string.ai_models_provider_key_placeholder),
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                singleLine = true,
                suffix = {
                    MmIconButton(
                        icon = if (apiKeyVisible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                        onClick = { apiKeyVisible = !apiKeyVisible },
                        contentDescription = null,
                    )
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(space.padding_1x)) {
                MmButton(
                    text = stringResource(Res.string.ai_models_provider_save_key),
                    onClick = { onIntent(AiModelsIntent.SaveApiKey(row.id)) },
                    enabled = row.apiKeyInput.isNotBlank(),
                    variant = MmButtonVariant.Secondary,
                    size = MmButtonSize.Sm,
                )
                MmButton(
                    text = stringResource(Res.string.ai_models_provider_delete_key),
                    onClick = { onIntent(AiModelsIntent.DeleteApiKey(row.id)) },
                    enabled = row.configured,
                    variant = MmButtonVariant.Ghost,
                    size = MmButtonSize.Sm,
                )
                MmButton(
                    text = stringResource(Res.string.ai_models_provider_refresh),
                    onClick = { onIntent(AiModelsIntent.RefreshModels(row.id)) },
                    enabled = row.configured && !row.isRefreshing,
                    variant = MmButtonVariant.Ghost,
                    size = MmButtonSize.Sm,
                )
            }

            Text(
                text = stringResource(Res.string.ai_models_provider_model_label),
                style = type.caption,
                color = colors.text2,
            )
            row.models.forEach { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIntent(AiModelsIntent.SelectRemoteModel(row.id, model.id)) }
                        .padding(vertical = space.padding_0_5x),
                    horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MmRadio(
                        selected = model.id == row.selectedModelId,
                        onClick = { onIntent(AiModelsIntent.SelectRemoteModel(row.id, model.id)) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = model.displayName, style = type.body, color = colors.text)
                        Text(text = model.id, style = type.caption, color = colors.text2)
                    }
                }
            }
        }
    }
}

@Composable
private fun etaLabel(seconds: Long): String = when {
    seconds >= 3600 -> stringResource(
        Res.string.ai_models_eta_hours,
        (seconds / 3600).toInt(),
        ((seconds % 3600) / 60).toInt(),
    )
    seconds >= 60 -> stringResource(Res.string.ai_models_eta_minutes, (seconds / 60).toInt())
    else -> stringResource(Res.string.ai_models_eta_seconds, seconds.toInt())
}

private fun String.modelNameRes(): StringResource = when (this) {
    "ai_model_smollm2_135m" -> Res.string.ai_model_smollm2_135m
    "ai_model_qwen25_1_5b" -> Res.string.ai_model_qwen25_1_5b
    "ai_model_gemma4_e2b_it" -> Res.string.ai_model_gemma4_e2b_it
    else -> Res.string.ai_models_title
}

private fun AiModelsError.messageRes(): StringResource = when (this) {
    AiModelsError.Download -> Res.string.ai_models_error_download
    AiModelsError.Delete -> Res.string.ai_models_error_delete
    AiModelsError.SaveKey -> Res.string.ai_models_error_provider_save
    AiModelsError.DeleteKey -> Res.string.ai_models_error_provider_delete
    AiModelsError.RefreshModels -> Res.string.ai_models_error_provider_refresh
    AiModelsError.TestConnection -> Res.string.ai_models_error_provider_test
    AiModelsError.SelectModel -> Res.string.ai_models_error_provider_select
}

@Preview
@Composable
private fun AiModelsContentPreview() {
    MoneyMTheme {
        AiModelsContent(
            state = AiModelsUiState(
                models = listOf(
                    ModelRowUi("qwen2.5-1.5b-it", "ai_model_qwen25_1_5b", "1.5 GB", ModelStatus.NotDownloaded),
                    ModelRowUi(
                        "gemma4-e2b-it",
                        "ai_model_gemma4_e2b_it",
                        "2.6 GB",
                        ModelStatus.Downloading(
                            progress = 0.4f,
                            percentText = "40%",
                            sizeText = "1.0 GB / 2.6 GB",
                            speedText = "7.2 MB/s",
                            etaSeconds = 222L,
                        ),
                    ),
                ),
            ),
            onBack = {},
            onIntent = {},
        )
    }
}
