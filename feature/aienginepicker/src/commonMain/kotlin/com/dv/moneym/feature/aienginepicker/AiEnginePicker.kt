package com.dv.moneym.feature.aienginepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.imageVector
import moneym.feature.aienginepicker.generated.resources.Res
import moneym.feature.aienginepicker.generated.resources.ai_model_gemma4_e2b_it
import moneym.feature.aienginepicker.generated.resources.ai_model_qwen25_1_5b
import moneym.feature.aienginepicker.generated.resources.ai_model_smollm2_135m
import moneym.feature.aienginepicker.generated.resources.analyze_download_model
import moneym.feature.aienginepicker.generated.resources.analyze_engine_anthropic
import moneym.feature.aienginepicker.generated.resources.analyze_engine_apple_intelligence
import moneym.feature.aienginepicker.generated.resources.analyze_engine_gemini
import moneym.feature.aienginepicker.generated.resources.analyze_engine_gemini_nano
import moneym.feature.aienginepicker.generated.resources.analyze_engine_label
import moneym.feature.aienginepicker.generated.resources.analyze_engine_local_llm
import moneym.feature.aienginepicker.generated.resources.analyze_engine_needs_download_notice
import moneym.feature.aienginepicker.generated.resources.analyze_engine_openai
import moneym.feature.aienginepicker.generated.resources.analyze_engine_openrouter
import moneym.feature.aienginepicker.generated.resources.analyze_engine_status_download
import moneym.feature.aienginepicker.generated.resources.analyze_engine_status_ready
import moneym.feature.aienginepicker.generated.resources.analyze_engine_status_unavailable
import moneym.feature.aienginepicker.generated.resources.analyze_manage_models
import moneym.feature.aienginepicker.generated.resources.analyze_model_picker_cd
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AiEnginePickerButton(
    state: AiEnginePickerState,
    onSelect: (AiEngineId) -> Unit,
    onManageModels: () -> Unit,
    onRefresh: () -> Unit,
) {
    if (state.engines.isEmpty()) return
    var showSheet by remember { mutableStateOf(false) }

    MmIconButton(
        icon = Icon.Sliders.imageVector,
        onClick = {
            onRefresh()
            showSheet = true
        },
        contentDescription = stringResource(Res.string.analyze_model_picker_cd),
    )

    if (showSheet) {
        AiEnginePickerSheet(
            state = state,
            onSelect = {
                showSheet = false
                onSelect(it)
            },
            onManageModels = {
                showSheet = false
                onManageModels()
            },
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
fun AiEngineDownloadNotice(
    show: Boolean,
    onManageModels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!show) return
    val colors = MM.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x),
        verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
    ) {
        Text(
            text = stringResource(Res.string.analyze_engine_needs_download_notice),
            style = MM.type.caption,
            color = colors.text2,
        )
        MmButton(
            text = stringResource(Res.string.analyze_download_model),
            onClick = onManageModels,
            variant = MmButtonVariant.Secondary,
            size = MmButtonSize.Sm,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiEnginePickerSheet(
    state: AiEnginePickerState,
    onSelect: (AiEngineId) -> Unit,
    onManageModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x,
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_3x,
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = MM.dimen.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            MmSheetHeader(
                title = stringResource(Res.string.analyze_engine_label),
                onClose = onDismiss,
                modifier = Modifier.padding(vertical = MM.dimen.padding_1x),
            )

            state.engines.forEach { engine ->
                EngineRow(
                    engine = engine,
                    selected = engine.id == state.selectedEngine,
                    localModelNameKey = state.localModelNameKey,
                    onClick = {
                        if (engine.available) onSelect(engine.id) else onManageModels()
                    },
                )
            }

            Spacer(Modifier.height(MM.dimen.padding_1x))

            MmButton(
                text = stringResource(Res.string.analyze_manage_models),
                onClick = onManageModels,
                variant = MmButtonVariant.Secondary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun EngineRow(
    engine: AiEngineOption,
    selected: Boolean,
    localModelNameKey: String?,
    onClick: () -> Unit,
) {
    val colors = MM.colors
    val modelNameRes = if (engine.id == AiEngineId.LOCAL_LLM) {
        localModelNameKey?.let(::localModelNameRes)
    } else {
        null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
            .background(if (selected) colors.surface else colors.bg)
            .clickable(onClick = onClick)
            .padding(MM.dimen.padding_2x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(engineLabelRes(engine.id)),
                style = MM.type.body,
                color = colors.text,
            )
            Text(
                text = if (modelNameRes != null) {
                    stringResource(modelNameRes)
                } else {
                    stringResource(engineStatusRes(engine))
                },
                style = MM.type.caption,
                color = when {
                    engine.needsDownload -> colors.accent
                    !engine.available && modelNameRes == null -> colors.danger
                    else -> colors.text3
                },
            )
        }
        if (selected) {
            M3Icon(
                imageVector = Icon.Check.imageVector,
                contentDescription = null,
                tint = colors.accent,
            )
        }
    }
}

private fun engineLabelRes(id: AiEngineId) = when (id) {
    AiEngineId.GEMINI_NANO -> Res.string.analyze_engine_gemini_nano
    AiEngineId.APPLE_INTELLIGENCE -> Res.string.analyze_engine_apple_intelligence
    AiEngineId.LOCAL_LLM -> Res.string.analyze_engine_local_llm
    AiEngineId("remote:openai") -> Res.string.analyze_engine_openai
    AiEngineId("remote:anthropic") -> Res.string.analyze_engine_anthropic
    AiEngineId("remote:gemini") -> Res.string.analyze_engine_gemini
    AiEngineId("remote:openrouter") -> Res.string.analyze_engine_openrouter
    else -> Res.string.analyze_engine_label
}

private fun engineStatusRes(option: AiEngineOption) = when {
    option.available -> Res.string.analyze_engine_status_ready
    option.needsDownload -> Res.string.analyze_engine_status_download
    else -> Res.string.analyze_engine_status_unavailable
}

private fun localModelNameRes(key: String): StringResource? = when (key) {
    "ai_model_smollm2_135m" -> Res.string.ai_model_smollm2_135m
    "ai_model_qwen25_1_5b" -> Res.string.ai_model_qwen25_1_5b
    "ai_model_gemma4_e2b_it" -> Res.string.ai_model_gemma4_e2b_it
    else -> null
}

@Preview
@Composable
private fun AiEngineDownloadNoticePreview_Light() {
    MoneyMTheme(darkTheme = false) {
        AiEngineDownloadNotice(show = true, onManageModels = {})
    }
}

@Preview
@Composable
private fun AiEngineDownloadNoticePreview_Dark() {
    MoneyMTheme(darkTheme = true) {
        AiEngineDownloadNotice(show = true, onManageModels = {})
    }
}
