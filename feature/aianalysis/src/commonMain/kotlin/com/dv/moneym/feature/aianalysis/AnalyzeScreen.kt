package com.dv.moneym.feature.aianalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.ai.AiGroundingMode
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.feature.aienginepicker.AiEngineDownloadNotice
import com.dv.moneym.feature.aienginepicker.AiEnginePickerButton
import com.dv.moneym.feature.aianalysis.components.AnalyzeInputBar
import com.dv.moneym.feature.aianalysis.components.MessageBubble
import com.dv.moneym.feature.aianalysis.components.SuggestedPrompts
import kotlinx.serialization.Serializable
import moneym.feature.aianalysis.generated.resources.Res
import moneym.feature.aianalysis.generated.resources.analyze_error
import moneym.feature.aianalysis.generated.resources.analyze_history_cd
import moneym.feature.aianalysis.generated.resources.analyze_info_cd
import moneym.feature.aianalysis.generated.resources.analyze_grounding_label
import moneym.feature.aianalysis.generated.resources.analyze_grounding_snapshot
import moneym.feature.aianalysis.generated.resources.analyze_grounding_tools
import moneym.feature.aianalysis.generated.resources.analyze_year_label
import moneym.feature.aianalysis.generated.resources.analyze_year_next_cd
import moneym.feature.aianalysis.generated.resources.analyze_year_prev_cd
import moneym.feature.aianalysis.generated.resources.analyze_title
import moneym.feature.aianalysis.generated.resources.analyze_tools_fallback_notice
import moneym.feature.aianalysis.generated.resources.analyze_remote_privacy_notice
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class AnalyzeKey(val year: Int, val month: Int) : ModalKey

fun EntryProviderScope<NavKey>.analyzeEntry(
    onBack: () -> Unit,
    onManageModels: () -> Unit,
    onShowHistory: (year: Int, month: Int) -> Unit,
    onShowInfo: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<AnalyzeKey>(metadata = metadata) { key ->
    AnalyzeScreen(
        year = key.year,
        month = key.month,
        onBack = onBack,
        onManageModels = onManageModels,
        onShowHistory = { onShowHistory(key.year, key.month) },
        onShowInfo = onShowInfo,
    )
}

@Composable
fun AnalyzeScreen(
    year: Int,
    month: Int,
    onBack: () -> Unit,
    onManageModels: () -> Unit,
    onShowHistory: () -> Unit,
    onShowInfo: () -> Unit,
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
        onShowInfo = onShowInfo,
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
    onShowInfo: () -> Unit,
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
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
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
                    MmIconButton(
                        icon = Icon.Info.imageVector,
                        onClick = onShowInfo,
                        contentDescription = stringResource(Res.string.analyze_info_cd),
                    )
                    AiEnginePickerButton(
                        state = state.enginePicker,
                        onSelect = { onIntent(AnalyzeIntent.EngineChanged(it)) },
                        onManageModels = onManageModels,
                        onRefresh = { onIntent(AnalyzeIntent.RefreshEngines) },
                    )
                }
            },
        )

        AiEngineDownloadNotice(show = state.needsModelDownload, onManageModels = onManageModels)

        val groundingOptions = listOf(
            stringResource(Res.string.analyze_grounding_snapshot),
            stringResource(Res.string.analyze_grounding_tools),
        )
        val isSnapshot = state.groundingMode != AiGroundingMode.TOOLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_0_5x),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    selectedIndex = if (isSnapshot) 0 else 1,
                    onOptionSelected = { index ->
                        val mode = if (index == 1) AiGroundingMode.TOOLS else AiGroundingMode.SNAPSHOT
                        onIntent(AnalyzeIntent.GroundingModeChanged(mode))
                    },
                )
            }
            if (isSnapshot && state.selectedYear != null) {
                YearStepper(
                    year = state.selectedYear,
                    canGoPrev = state.minYear == null || state.selectedYear > state.minYear,
                    canGoNext = state.maxYear == null || state.selectedYear < state.maxYear,
                    onPrev = { onIntent(AnalyzeIntent.YearChanged(state.selectedYear - 1)) },
                    onNext = { onIntent(AnalyzeIntent.YearChanged(state.selectedYear + 1)) },
                )
            }
        }

        if (state.showToolsFallbackNotice) {
            NoticeRow(
                text = stringResource(Res.string.analyze_tools_fallback_notice),
                color = colors.warning,
            )
        }
        if (state.showRemotePrivacyNotice) {
            NoticeRow(
                text = stringResource(Res.string.analyze_remote_privacy_notice),
                color = colors.warning,
            )
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
                    itemsIndexed(state.messages) { index, message ->
                        val isLastAssistant = index == state.messages.lastIndex &&
                            message.role == ChatRole.ASSISTANT
                        MessageBubble(
                            role = message.role,
                            content = message.content,
                            isThinking = state.isGenerating && isLastAssistant && message.content.isEmpty(),
                        )
                    }
                    // Assistant placeholder isn't added until grounding is built; show a thinking
                    // bubble in that gap so the indicator lives in the chat, not at the screen edge.
                    if (state.isGenerating && state.messages.lastOrNull()?.role == ChatRole.USER) {
                        item {
                            MessageBubble(
                                role = ChatRole.ASSISTANT,
                                content = "",
                                isThinking = true,
                            )
                        }
                    }
                }
            }
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
            onShowInfo = {},
        )
    }
}

@Composable
private fun YearStepper(
    year: Int,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = MM.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.analyze_year_label),
            style = MM.type.caption,
            color = colors.text2,
            modifier = Modifier.weight(1f),
        )
        if (canGoPrev) {
            MmIconButton(
                icon = Icon.ChevronLeft.imageVector,
                onClick = onPrev,
                size = MM.dimen.padding_4x,
                contentDescription = stringResource(Res.string.analyze_year_prev_cd),
            )
        } else {
            Spacer(Modifier.size(MM.dimen.padding_4x))
        }
        Text(
            text = year.toString(),
            style = MM.type.body,
            color = colors.text,
        )
        if (canGoNext) {
            MmIconButton(
                icon = Icon.ChevronRight.imageVector,
                onClick = onNext,
                size = MM.dimen.padding_4x,
                contentDescription = stringResource(Res.string.analyze_year_next_cd),
            )
        } else {
            Spacer(Modifier.size(MM.dimen.padding_4x))
        }
    }
}

@Composable
private fun NoticeRow(text: String, color: Color = MM.colors.text2) {
    Text(
        text = text,
        style = MM.type.caption,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x),
    )
}
