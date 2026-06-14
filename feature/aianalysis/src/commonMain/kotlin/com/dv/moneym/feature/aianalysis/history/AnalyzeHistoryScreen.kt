package com.dv.moneym.feature.aianalysis.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmDeleteSheet
import com.dv.moneym.core.ui.MmEmptyState
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.data.aichat.ChatConversation
import kotlinx.serialization.Serializable
import com.dv.moneym.core.common.formatDateTime
import moneym.feature.aianalysis.generated.resources.Res
import moneym.feature.aianalysis.generated.resources.analyze_history_delete_body
import moneym.feature.aianalysis.generated.resources.analyze_history_delete_cancel
import moneym.feature.aianalysis.generated.resources.analyze_history_delete_cd
import moneym.feature.aianalysis.generated.resources.analyze_history_delete_confirm
import moneym.feature.aianalysis.generated.resources.analyze_history_delete_title
import moneym.feature.aianalysis.generated.resources.analyze_history_empty
import moneym.feature.aianalysis.generated.resources.analyze_history_new_chat
import moneym.feature.aianalysis.generated.resources.analyze_history_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data class AnalyzeHistoryKey(val year: Int, val month: Int) : ModalKey

fun EntryProviderScope<NavKey>.analyzeHistoryEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<AnalyzeHistoryKey>(metadata = metadata) {
    AnalyzeHistoryScreen(onBack = onBack)
}

@Composable
fun AnalyzeHistoryScreen(
    onBack: () -> Unit,
    viewModel: AnalyzeHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AnalyzeHistoryContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
private fun AnalyzeHistoryContent(
    state: AnalyzeHistoryUiState,
    onIntent: (AnalyzeHistoryIntent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.analyze_history_title),
            onBack = onBack,
        )

        if (state.conversations.isEmpty()) {
            MmEmptyState(
                message = stringResource(Res.string.analyze_history_empty),
                icon = Icon.List.imageVector,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(MM.dimen.padding_2_5x),
                verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
            ) {
                items(state.conversations, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = {
                            onIntent(AnalyzeHistoryIntent.Resume(conversation.id))
                            onBack()
                        },
                        onDelete = { onIntent(AnalyzeHistoryIntent.RequestDelete(conversation.id)) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_2x,
                )
                .navigationBarsPadding(),
        ) {
            MmButton(
                text = stringResource(Res.string.analyze_history_new_chat),
                onClick = {
                    onIntent(AnalyzeHistoryIntent.NewChat)
                    onBack()
                },
                variant = MmButtonVariant.Primary,
                fullWidth = true,
                leadingIcon = Icon.Plus.imageVector,
            )
        }
    }

    if (state.pendingDeleteId != null) {
        MmDeleteSheet(
            title = stringResource(Res.string.analyze_history_delete_title),
            body = stringResource(Res.string.analyze_history_delete_body),
            cancelText = stringResource(Res.string.analyze_history_delete_cancel),
            confirmText = stringResource(Res.string.analyze_history_delete_confirm),
            onConfirm = { onIntent(AnalyzeHistoryIntent.ConfirmDelete) },
            onCancel = { onIntent(AnalyzeHistoryIntent.DismissDelete) },
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: ChatConversation,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MM.colors
    MmCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(MM.dimen.padding_2x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MM.type.body,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatTimestamp(conversation.updatedAt),
                    style = MM.type.caption,
                    color = colors.text3,
                )
            }
            MmIconButton(
                icon = Icon.Trash.imageVector,
                onClick = onDelete,
                variant = MmIconButtonVariant.Danger,
                contentDescription = stringResource(Res.string.analyze_history_delete_cd),
            )
        }
    }
}

private fun formatTimestamp(ms: Long): String = formatDateTime(ms)

@Preview
@Composable
private fun AnalyzeHistoryContentPreview() {
    MoneyMTheme {
        AnalyzeHistoryContent(
            state = AnalyzeHistoryUiState(
                conversations = listOf(
                    ChatConversation(1, "How much did I spend on groceries?", 1716700000000L, 1716700000000L),
                    ChatConversation(2, "Compare this month to last", 1716600000000L, 1716600000000L),
                ),
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
