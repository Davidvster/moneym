package com.dv.moneym.feature.settings.overview

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_overview
import moneym.feature.settings.generated.resources.settings_overview_ai_section
import moneym.feature.settings.generated.resources.settings_overview_block_empty
import moneym.feature.settings.generated.resources.settings_overview_create_widget
import moneym.feature.settings.generated.resources.settings_overview_drag_cd
import moneym.feature.settings.generated.resources.settings_overview_reset_defaults
import moneym.feature.settings.generated.resources.settings_overview_section_built_in
import moneym.feature.settings.generated.resources.settings_overview_edit_widget_cd
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Serializable
data object OverviewSettingsKey : NavKey

fun EntryProviderScope<NavKey>.overviewSettingsEntry(
    onBack: () -> Unit,
    onOpenAiWidgetBuilder: (Long?) -> Unit = {},
    metadata: Map<String, Any> = emptyMap(),
) = entry<OverviewSettingsKey>(metadata = metadata) {
    OverviewSettingsScreen(
        onBack = onBack,
        onOpenAiWidgetBuilder = onOpenAiWidgetBuilder,
    )
}

@Composable
private fun OverviewSettingsScreen(
    onBack: () -> Unit,
    onOpenAiWidgetBuilder: (Long?) -> Unit = {},
    viewModel: OverviewSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OverviewSettingsEffect.OpenAiWidgetBuilder ->
                    onOpenAiWidgetBuilder(effect.widgetId)
            }
        }
    }

    OverviewSettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
private fun OverviewSettingsContent(
    state: OverviewSettingsUiState,
    onIntent: (OverviewSettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var localBuiltInBlocks by remember(state.builtInBlocks) { mutableStateOf(state.builtInBlocks) }
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        localBuiltInBlocks = reorderOverviewBuiltInBlocks(
            blocks = localBuiltInBlocks,
            fromLazyListIndex = from.index,
            toLazyListIndex = to.index,
        )
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_overview),
            onBack = onBack,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                horizontal = space.padding_2x,
                vertical = space.padding_2x,
            ),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(bottom = space.padding_1x),
                    verticalArrangement = Arrangement.spacedBy(space.padding_1x),
                ) {
                    SectionLabel(text = stringResource(Res.string.settings_overview_section_built_in))
                }
            }
            itemsIndexed(localBuiltInBlocks, key = { _, row -> row.blockId.value }) { index, row ->
                ReorderableItem(reorderableState, key = row.blockId.value) { isDragging ->
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 6.dp else 0.dp,
                        label = "overview_block_elevation_${row.blockId.value}",
                    )
                    OverviewBuiltInBlockRow(
                        row = row,
                        divider = index < localBuiltInBlocks.lastIndex,
                        isDragging = isDragging,
                        modifier = Modifier.shadow(elevation),
                        dragHandleModifier = Modifier.draggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                onIntent(
                                    OverviewSettingsIntent.ReorderBuiltInBlocks(
                                        localBuiltInBlocks.map { it.blockId },
                                    ),
                                )
                            },
                        ),
                        onVisibleChanged = { visible ->
                            onIntent(
                                OverviewSettingsIntent.SetBuiltInBlockVisible(
                                    blockId = row.blockId,
                                    visible = visible,
                                ),
                            )
                        },
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.padding(top = space.padding_1x),
                    verticalArrangement = Arrangement.spacedBy(space.padding_1x),
                ) {
                    MmButton(
                        text = stringResource(Res.string.settings_overview_reset_defaults),
                        onClick = { onIntent(OverviewSettingsIntent.ResetBuiltInBlocks) },
                        variant = MmButtonVariant.Secondary,
                        fullWidth = true,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(top = space.padding_3x),
                    verticalArrangement = Arrangement.spacedBy(space.padding_1x),
                ) {
                    SectionLabel(text = stringResource(Res.string.settings_overview_ai_section))
                    if (state.aiWidgets.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.settings_overview_block_empty),
                            style = MM.type.body,
                            color = colors.text2,
                        )
                    } else {
                        MmCard(padded = false, shape = MM.dimen.radius_1_5x) {
                            Column {
                                state.aiWidgets.forEachIndexed { index, widget ->
                                    OverviewAiWidgetRow(
                                        widget = widget,
                                        divider = index < state.aiWidgets.lastIndex,
                                        onEnabledChanged = { enabled ->
                                            onIntent(
                                                OverviewSettingsIntent.SetAiWidgetEnabled(
                                                    widgetId = widget.id,
                                                    enabled = enabled,
                                                ),
                                            )
                                        },
                                        onEdit = {
                                            onIntent(OverviewSettingsIntent.EditAiWidget(widget.id))
                                        },
                                    )
                                }
                            }
                        }
                    }
                    MmButton(
                        text = stringResource(Res.string.settings_overview_create_widget),
                        onClick = { onIntent(OverviewSettingsIntent.CreateAiWidget) },
                        variant = MmButtonVariant.Primary,
                        leadingIcon = Icon.Plus.imageVector,
                        fullWidth = true,
                    )
                }
            }

            item {
                Spacer(Modifier.height(space.padding_1x))
            }
        }
    }
}

internal fun <T> reorderOverviewBuiltInBlocks(
    blocks: List<T>,
    fromLazyListIndex: Int,
    toLazyListIndex: Int,
): List<T> {
    val fromIndex = fromLazyListIndex - OverviewBuiltInBlocksFirstLazyListIndex
    val toIndex = toLazyListIndex - OverviewBuiltInBlocksFirstLazyListIndex
    if (fromIndex !in blocks.indices || toIndex !in blocks.indices) return blocks

    return blocks.toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

private const val OverviewBuiltInBlocksFirstLazyListIndex = 1

@Composable
private fun OverviewBuiltInBlockRow(
    row: OverviewSettingsBuiltInBlockUiState,
    divider: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    onVisibleChanged: (Boolean) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val title = stringResource(row.titleRes)
    val dragDescription = stringResource(Res.string.settings_overview_drag_cd, title)

    MmRow(
        modifier = modifier.background(if (isDragging) colors.surface else colors.bg),
        divider = divider,
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icon.DragHandle.imageVector,
            contentDescription = dragDescription,
            tint = colors.text3,
            modifier = Modifier
                .padding(top = space.padding_0_25x)
                .size(MM.dimen.icon_1x)
                .then(dragHandleModifier),
        )
        Text(
            text = title,
            style = MM.type.body,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        MmToggle(
            checked = row.visible,
            onCheckedChange = onVisibleChanged,
        )
    }
}

@Composable
private fun OverviewAiWidgetRow(
    widget: OverviewSettingsAiWidgetUiState,
    divider: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    MmRow(divider = divider) {
        androidx.compose.material3.Icon(
            imageVector = Icon.Sparkles.imageVector,
            contentDescription = null,
            tint = colors.text,
            modifier = Modifier.padding(top = space.padding_0_25x).size(MM.dimen.icon_1x),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = widget.title,
                style = MM.type.body,
                color = colors.text,
            )
            widget.prompt?.let {
                Text(
                    text = it,
                    style = MM.type.caption,
                    color = colors.text2,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(space.padding_0_5x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MmIconButton(
                icon = Icon.Edit.imageVector,
                onClick = onEdit,
                contentDescription = stringResource(Res.string.settings_overview_edit_widget_cd, widget.title),
            )
            MmToggle(
                checked = widget.enabled,
                onCheckedChange = onEnabledChanged,
            )
        }
    }
}

private val sampleBuiltInRows = defaultOverviewBuiltInBlocks().take(4)

private val sampleWidgets = listOf(
    OverviewSettingsAiWidgetUiState(
        id = 1L,
        title = "Monthly overview",
        prompt = "Show a concise summary of last month.",
        enabled = true,
    ),
    OverviewSettingsAiWidgetUiState(
        id = 2L,
        title = "Budget pulse",
        prompt = "Spot unusual overspend and suggest next steps.",
        enabled = false,
    ),
)

private val sampleState = OverviewSettingsUiState(
    builtInBlocks = sampleBuiltInRows,
    aiWidgets = sampleWidgets,
)

private val sampleBuiltInRow = OverviewSettingsBuiltInBlockUiState(
    blockId = sampleBuiltInRows.first().blockId,
    titleRes = sampleBuiltInRows.first().titleRes,
    visible = true,
    canMoveUp = true,
    canMoveDown = true,
)

private val sampleAiWidgetRow = sampleWidgets.first()

@Preview
@Composable
private fun OverviewSettingsContentPreviewLight() {
    MoneyMTheme(darkTheme = false) {
        OverviewSettingsContent(
            state = sampleState,
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun OverviewSettingsContentPreviewDark() {
    MoneyMTheme(darkTheme = true) {
        OverviewSettingsContent(
            state = sampleState,
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun OverviewBuiltInBlockRowPreviewLight() {
    MoneyMTheme(darkTheme = false) {
        MmCard(padded = false, shape = MM.dimen.radius_1_5x) {
            OverviewBuiltInBlockRow(
                row = sampleBuiltInRow,
                divider = false,
                isDragging = false,
                dragHandleModifier = Modifier,
                onVisibleChanged = {},
            )
        }
    }
}

@Preview
@Composable
private fun OverviewBuiltInBlockRowPreviewDark() {
    MoneyMTheme(darkTheme = true) {
        MmCard(padded = false, shape = MM.dimen.radius_1_5x) {
            OverviewBuiltInBlockRow(
                row = sampleBuiltInRow,
                divider = false,
                isDragging = false,
                dragHandleModifier = Modifier,
                onVisibleChanged = {},
            )
        }
    }
}

@Preview
@Composable
private fun OverviewAiWidgetRowPreviewLight() {
    MoneyMTheme(darkTheme = false) {
        MmCard(padded = false, shape = MM.dimen.radius_1_5x) {
            OverviewAiWidgetRow(
                widget = sampleAiWidgetRow,
                divider = false,
                onEnabledChanged = {},
                onEdit = {},
            )
        }
    }
}

@Preview
@Composable
private fun OverviewAiWidgetRowPreviewDark() {
    MoneyMTheme(darkTheme = true) {
        MmCard(padded = false, shape = MM.dimen.radius_1_5x) {
            OverviewAiWidgetRow(
                widget = sampleAiWidgetRow,
                divider = false,
                onEnabledChanged = {},
                onEdit = {},
            )
        }
    }
}
