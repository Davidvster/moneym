package com.dv.moneym.feature.aianalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiGroundingMode
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.feature.aianalysis.components.AnalyzeInputBar
import com.dv.moneym.feature.aianalysis.components.MessageBubble
import com.dv.moneym.feature.aianalysis.components.SuggestedPrompts
import kotlinx.serialization.Serializable
import moneym.feature.aianalysis.generated.resources.Res
import moneym.feature.aianalysis.generated.resources.analyze_download_model
import moneym.feature.aianalysis.generated.resources.analyze_engine_apple_intelligence
import moneym.feature.aianalysis.generated.resources.analyze_engine_gemini_nano
import moneym.feature.aianalysis.generated.resources.analyze_engine_label
import moneym.feature.aianalysis.generated.resources.analyze_engine_local_llm
import moneym.feature.aianalysis.generated.resources.analyze_engine_needs_download_notice
import moneym.feature.aianalysis.generated.resources.analyze_engine_status_download
import moneym.feature.aianalysis.generated.resources.analyze_engine_status_ready
import moneym.feature.aianalysis.generated.resources.analyze_engine_status_unavailable
import moneym.feature.aianalysis.generated.resources.analyze_error
import moneym.feature.aianalysis.generated.resources.analyze_generating
import moneym.feature.aianalysis.generated.resources.analyze_history_cd
import moneym.feature.aianalysis.generated.resources.analyze_manage_models
import moneym.feature.aianalysis.generated.resources.analyze_model_picker_cd
import moneym.feature.aianalysis.generated.resources.analyze_grounding_label
import moneym.feature.aianalysis.generated.resources.analyze_grounding_snapshot
import moneym.feature.aianalysis.generated.resources.analyze_grounding_tools
import moneym.feature.aianalysis.generated.resources.analyze_title
import moneym.feature.aianalysis.generated.resources.analyze_tools_fallback_notice
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class AnalyzeKey(val year: Int, val month: Int) : ModalKey

fun EntryProviderScope<NavKey>.analyzeEntry(
    onBack: () -> Unit,
    onManageModels: () -> Unit,
    onShowHistory: (year: Int, month: Int) -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<AnalyzeKey>(metadata = metadata) { key ->
    AnalyzeScreen(
        year = key.year,
        month = key.month,
        onBack = onBack,
        onManageModels = onManageModels,
        onShowHistory = { onShowHistory(key.year, key.month) },
    )
}

@Composable
fun AnalyzeScreen(
    year: Int,
    month: Int,
    onBack: () -> Unit,
    onManageModels: () -> Unit,
    onShowHistory: () -> Unit,
    viewModel: AnalyzeViewModel = koinViewModel { parametersOf(year, month) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Pick up a resume / new-chat action chosen on the full-screen history when it is popped.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(AnalyzeIntent.CheckPending)
    }

    AnalyzeContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onManageModels = onManageModels,
        onShowHistory = onShowHistory,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalyzeContent(
    state: AnalyzeUiState,
    onIntent: (AnalyzeIntent) -> Unit,
    onBack: () -> Unit,
    onManageModels: () -> Unit,
    onShowHistory: () -> Unit,
) {
    val colors = MM.colors
    val listState = rememberLazyListState()

    // Whether the chat is pinned to the latest message. Gates auto-scroll so streaming
    // output follows the bottom without hijacking a manual scroll-up.
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            last.index >= info.totalItemsCount - 1
        }
    }

    // Follow streamed output with an instant scroll (not animated) and only while the user
    // is already at the bottom — animating on every token froze the UI and fought scrolling.
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty() && isAtBottom) {
            listState.scrollToItem(state.messages.lastIndex, Int.MAX_VALUE)
        }
    }

    // Re-pin to the newest message when the keyboard opens, so focusing the input keeps the
    // latest reply in view instead of snapping the list back to the top of the conversation.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex, Int.MAX_VALUE)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.analyze_title),
            onBack = onBack,
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_0_5x),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MmIconButton(
                        icon = Icon.List.imageVector,
                        onClick = onShowHistory,
                        contentDescription = stringResource(Res.string.analyze_history_cd),
                    )
                    if (state.engines.isNotEmpty()) {
                        ModelPicker(
                            engines = state.engines,
                            selectedEngine = state.selectedEngine,
                            onSelect = { onIntent(AnalyzeIntent.EngineChanged(it)) },
                            onManageModels = onManageModels,
                            onRefresh = { onIntent(AnalyzeIntent.RefreshEngines) },
                        )
                    }
                }
            },
        )

        if (state.needsModelDownload) {
            Column(
                modifier = Modifier
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

        val groundingOptions = listOf(
            stringResource(Res.string.analyze_grounding_snapshot),
            stringResource(Res.string.analyze_grounding_tools),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.analyze_grounding_label),
                style = MM.type.caption,
                color = colors.text2,
                modifier = Modifier.weight(1f),
            )
            MmSegmented(
                options = groundingOptions,
                selectedIndex = if (state.groundingMode == AiGroundingMode.TOOLS) 1 else 0,
                onOptionSelected = { index ->
                    val mode = if (index == 1) AiGroundingMode.TOOLS else AiGroundingMode.SNAPSHOT
                    onIntent(AnalyzeIntent.GroundingModeChanged(mode))
                },
            )
        }

        if (state.showToolsFallbackNotice) {
            NoticeRow(text = stringResource(Res.string.analyze_tools_fallback_notice))
        }
        if (state.error != null) {
            NoticeRow(text = stringResource(Res.string.analyze_error))
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.messages.isEmpty()) {
                SuggestedPrompts(
                    onPromptClick = { prompt -> onIntent(AnalyzeIntent.SendMessage(prompt)) },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MM.dimen.padding_2_5x),
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
                ) {
                    itemsIndexed(state.messages) { _, message ->
                        MessageBubble(role = message.role, content = message.content)
                    }
                }
            }
        }

        if (state.isGenerating) {
            Text(
                text = stringResource(Res.string.analyze_generating),
                style = MM.type.caption,
                color = colors.text3,
                modifier = Modifier.padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_0_5x,
                ),
            )
        }

        AnalyzeInputBar(
            input = state.input,
            enabled = !state.isGenerating,
            onInputChange = { onIntent(AnalyzeIntent.InputChanged(it)) },
            onSend = { onIntent(AnalyzeIntent.SendMessage(state.input)) },
        )
    }
}

@Preview
@Composable
private fun AnalyzeContentPreview() {
    MoneyMTheme {
        AnalyzeContent(
            state = AnalyzeUiState(
                messages = listOf(
                    ChatMessage(ChatRole.USER, "How much did I spend on groceries?"),
                    ChatMessage(ChatRole.ASSISTANT, "You spent 240 EUR on groceries this month."),
                ),
            ),
            onIntent = {},
            onBack = {},
            onManageModels = {},
            onShowHistory = {},
        )
    }
}

private fun engineLabelRes(id: AiEngineId) = when (id) {
    AiEngineId.GEMINI_NANO -> Res.string.analyze_engine_gemini_nano
    AiEngineId.APPLE_INTELLIGENCE -> Res.string.analyze_engine_apple_intelligence
    AiEngineId.LOCAL_LLM -> Res.string.analyze_engine_local_llm
}

@Composable
private fun ModelPicker(
    engines: List<AiEngineOption>,
    selectedEngine: AiEngineId?,
    onSelect: (AiEngineId) -> Unit,
    onManageModels: () -> Unit,
    onRefresh: () -> Unit,
) {
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
        ModelPickerSheet(
            engines = engines,
            selectedEngine = selectedEngine,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    engines: List<AiEngineOption>,
    selectedEngine: AiEngineId?,
    onSelect: (AiEngineId) -> Unit,
    onManageModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
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

            Text(
                text = stringResource(Res.string.analyze_engine_label),
                style = type.title3,
                color = colors.text,
                modifier = Modifier.padding(vertical = MM.dimen.padding_1x),
            )

            engines.forEach { engine ->
                EngineRow(
                    engine = engine,
                    selected = engine.id == selectedEngine,
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
    onClick: () -> Unit,
) {
    val colors = MM.colors
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
                text = stringResource(engineStatusRes(engine)),
                style = MM.type.caption,
                color = if (engine.needsDownload) colors.accent else colors.text3,
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

private fun engineStatusRes(option: AiEngineOption) = when {
    option.available -> Res.string.analyze_engine_status_ready
    option.needsDownload -> Res.string.analyze_engine_status_download
    else -> Res.string.analyze_engine_status_unavailable
}

@Composable
private fun NoticeRow(text: String) {
    Text(
        text = text,
        style = MM.type.caption,
        color = MM.colors.text2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x),
    )
}
