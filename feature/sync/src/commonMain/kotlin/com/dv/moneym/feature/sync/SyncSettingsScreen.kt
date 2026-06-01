package com.dv.moneym.feature.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.sync.generated.resources.Res
import moneym.feature.sync.generated.resources.sync_settings_devices_header
import moneym.feature.sync.generated.resources.sync_settings_last_synced_days
import moneym.feature.sync.generated.resources.sync_settings_last_synced_hours
import moneym.feature.sync.generated.resources.sync_settings_last_synced_just_now
import moneym.feature.sync.generated.resources.sync_settings_last_synced_longer
import moneym.feature.sync.generated.resources.sync_settings_last_synced_minutes
import moneym.feature.sync.generated.resources.sync_settings_rename_cancel
import moneym.feature.sync.generated.resources.sync_settings_rename_label
import moneym.feature.sync.generated.resources.sync_settings_rename_save
import moneym.feature.sync.generated.resources.sync_settings_remove
import moneym.feature.sync.generated.resources.sync_settings_this_device
import moneym.feature.sync.generated.resources.sync_settings_title
import moneym.feature.sync.generated.resources.sync_settings_toggle_subtitle
import moneym.feature.sync.generated.resources.sync_settings_toggle_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SyncSettingsKey : NavKey

fun EntryProviderScope<NavKey>.syncSettingsEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<SyncSettingsKey>(metadata = metadata) {
    SyncSettingsScreen(onBack = onBack)
}

@Composable
fun SyncSettingsScreen(
    onBack: () -> Unit,
    viewModel: SyncSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SyncSettingsContent(state = state, onBack = onBack, onIntent = viewModel::onIntent)
}

@Composable
private fun SyncSettingsContent(
    state: SyncSettingsUiState,
    onBack: () -> Unit,
    onIntent: (SyncSettingsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(title = stringResource(Res.string.sync_settings_title), onBack = onBack)

        if (state.isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(space.icon_1x),
                    strokeWidth = space.padding_0_25x,
                    color = colors.accent,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = space.padding_2x,
                    vertical = space.padding_2x,
                ),
                verticalArrangement = Arrangement.spacedBy(space.padding_2x),
            ) {
                item { SyncToggleCard(enabled = state.crossDeviceSyncEnabled, onIntent = onIntent) }
                item { RenameCard(state = state, onIntent = onIntent) }
                item {
                    Text(
                        text = stringResource(Res.string.sync_settings_devices_header),
                        style = MM.type.micro,
                        color = colors.text2,
                        modifier = Modifier.padding(top = space.padding_1x),
                    )
                }
                items(state.devices, key = { it.id }) { device ->
                    DeviceCard(device = device, onIntent = onIntent)
                }
            }
        }
    }
}

@Composable
private fun SyncToggleCard(
    enabled: Boolean,
    onIntent: (SyncSettingsIntent) -> Unit,
) {
    val colors = MM.colors
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.sync_settings_toggle_title),
                    style = MM.type.body,
                    color = colors.text,
                )
                Text(
                    text = stringResource(Res.string.sync_settings_toggle_subtitle),
                    style = MM.type.caption.copy(color = colors.text2),
                )
            }
            MmToggle(checked = enabled, onCheckedChange = { onIntent(SyncSettingsIntent.ToggleSync) })
        }
    }
}

@Composable
private fun RenameCard(
    state: SyncSettingsUiState,
    onIntent: (SyncSettingsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        if (state.isRenaming) {
            MmField(
                value = state.renameDraft,
                onValueChange = { onIntent(SyncSettingsIntent.RenameDraftChanged(it)) },
                label = stringResource(Res.string.sync_settings_rename_label),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x),
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                MmButton(
                    text = stringResource(Res.string.sync_settings_rename_save),
                    onClick = { onIntent(SyncSettingsIntent.SubmitRename) },
                    size = MmButtonSize.Sm,
                )
                MmButton(
                    text = stringResource(Res.string.sync_settings_rename_cancel),
                    onClick = { onIntent(SyncSettingsIntent.CancelRename) },
                    variant = MmButtonVariant.Outline,
                    size = MmButtonSize.Sm,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.sync_settings_rename_label),
                        style = MM.type.micro,
                        color = colors.text2,
                    )
                    Text(
                        text = state.thisDeviceName,
                        style = MM.type.body,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MmIconButton(
                    icon = Icon.Edit.imageVector,
                    onClick = { onIntent(SyncSettingsIntent.StartRename) },
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceRow,
    onIntent: (SyncSettingsIntent) -> Unit,
) {
    val colors = MM.colors
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val name = if (device.isThisDevice) {
                    "${device.displayName} · ${stringResource(Res.string.sync_settings_this_device)}"
                } else {
                    device.displayName
                }
                Text(
                    text = name,
                    style = MM.type.body,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${device.platform} · ${lastSyncedLabel(device.lastSyncMs)}",
                    style = MM.type.caption.copy(color = colors.text2),
                )
            }
            if (!device.isThisDevice) {
                MmIconButton(
                    icon = Icon.Trash.imageVector,
                    onClick = { onIntent(SyncSettingsIntent.RemoveDevice(device.id)) },
                    variant = MmIconButtonVariant.Danger,
                    contentDescription = stringResource(Res.string.sync_settings_remove),
                )
            }
        }
    }
}

@Composable
private fun lastSyncedLabel(lastSyncMs: Long): String {
    val deltaMs = kotlin.time.Clock.System.now().toEpochMilliseconds() - lastSyncMs
    return when {
        deltaMs < 60_000L -> stringResource(Res.string.sync_settings_last_synced_just_now)
        deltaMs < 3_600_000L -> stringResource(Res.string.sync_settings_last_synced_minutes, (deltaMs / 60_000L).toInt())
        deltaMs < 86_400_000L -> stringResource(Res.string.sync_settings_last_synced_hours, (deltaMs / 3_600_000L).toInt())
        deltaMs < 30L * 86_400_000L -> stringResource(Res.string.sync_settings_last_synced_days, (deltaMs / 86_400_000L).toInt())
        else -> stringResource(Res.string.sync_settings_last_synced_longer)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SyncSettingsPreview() {
    MoneyMTheme {
        SyncSettingsContent(
            state = SyncSettingsUiState(
                crossDeviceSyncEnabled = true,
                isLoading = false,
                thisDeviceName = "Pixel 8",
                devices = listOf(
                    DeviceRow("1", "Pixel 8", "Android", 0L, isThisDevice = true),
                    DeviceRow("2", "iPhone 15", "iOS", 0L, isThisDevice = false),
                ),
            ),
            onBack = {},
            onIntent = {},
        )
    }
}
