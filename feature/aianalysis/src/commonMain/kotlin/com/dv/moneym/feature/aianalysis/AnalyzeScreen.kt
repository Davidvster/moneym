package com.dv.moneym.feature.aianalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.dv.moneym.core.ai.AiGroundingMode
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.feature.aianalysis.components.AnalyzeInputBar
import com.dv.moneym.feature.aianalysis.components.MessageBubble
import com.dv.moneym.feature.aianalysis.components.SuggestedPrompts
import kotlinx.serialization.Serializable
import moneym.feature.aianalysis.generated.resources.Res
import moneym.feature.aianalysis.generated.resources.analyze_error
import moneym.feature.aianalysis.generated.resources.analyze_generating
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
    metadata: Map<String, Any> = emptyMap(),
) = entry<AnalyzeKey>(metadata = metadata) { key ->
    AnalyzeScreen(year = key.year, month = key.month, onBack = onBack)
}

@Composable
fun AnalyzeScreen(
    year: Int,
    month: Int,
    onBack: () -> Unit,
    viewModel: AnalyzeViewModel = koinViewModel { parametersOf(year, month) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnalyzeContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
private fun AnalyzeContent(
    state: AnalyzeUiState,
    onIntent: (AnalyzeIntent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(title = stringResource(Res.string.analyze_title), onBack = onBack)

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
        )
    }
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
