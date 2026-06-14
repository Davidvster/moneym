package com.dv.moneym.feature.settings.overview.backuprestore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Icon.ChevronRight
import com.dv.moneym.core.model.Icon.Download
import com.dv.moneym.core.model.Icon.Folder
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmDeleteSheet
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmDialog
import com.dv.moneym.core.ui.MmErrorDialog
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.security.EncryptedBackup
import com.dv.moneym.data.remotebackup.RemoteBackupMetadata
import com.dv.moneym.data.remotebackup.RemoteBackupRuntimeState
import com.dv.moneym.platform.rememberBinaryFilePicker
import com.dv.moneym.platform.rememberFileSaver
import com.dv.moneym.platform.rememberFolderPicker
import com.dv.moneym.core.common.formatDateTime
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_auto_backup
import moneym.feature.settings.generated.resources.settings_auto_backup_subtitle
import moneym.feature.settings.generated.resources.settings_backup_restore
import moneym.feature.settings.generated.resources.settings_backup_saved
import moneym.feature.settings.generated.resources.settings_backup_to_file
import moneym.feature.settings.generated.resources.settings_cloud_sync
import moneym.feature.settings.generated.resources.settings_cloud_sync_subtitle
import moneym.feature.settings.generated.resources.settings_cloud_devices
import moneym.feature.settings.generated.resources.settings_cloud_join_title
import moneym.feature.settings.generated.resources.settings_cloud_join_body
import moneym.feature.settings.generated.resources.settings_cloud_join_confirm
import moneym.feature.settings.generated.resources.settings_cloud_join_plaintext_title
import moneym.feature.settings.generated.resources.settings_cloud_join_plaintext_body
import moneym.feature.settings.generated.resources.settings_last_backup
import moneym.feature.settings.generated.resources.settings_remote_backup_now
import moneym.feature.settings.generated.resources.settings_remote_backup_section
import moneym.feature.settings.generated.resources.settings_remote_connect
import moneym.feature.settings.generated.resources.settings_remote_delete_body
import moneym.feature.settings.generated.resources.settings_remote_delete_confirm
import moneym.feature.settings.generated.resources.settings_remote_delete_data
import moneym.feature.settings.generated.resources.settings_remote_delete_title
import moneym.feature.settings.generated.resources.settings_remote_disconnect
import moneym.feature.settings.generated.resources.settings_remote_disconnect_body
import moneym.feature.settings.generated.resources.settings_remote_disconnect_confirm
import moneym.feature.settings.generated.resources.settings_remote_disconnect_title
import moneym.feature.settings.generated.resources.settings_remote_error_title
import moneym.feature.settings.generated.resources.settings_remote_last_backup
import moneym.feature.settings.generated.resources.settings_remote_last_backup_never
import moneym.feature.settings.generated.resources.settings_remote_status_retry
import moneym.feature.settings.generated.resources.settings_remote_dont_encrypt
import moneym.feature.settings.generated.resources.settings_remote_passphrase_cancel
import moneym.feature.settings.generated.resources.settings_remote_passphrase_label
import moneym.feature.settings.generated.resources.settings_remote_password_body
import moneym.feature.settings.generated.resources.settings_remote_password_label
import moneym.feature.settings.generated.resources.settings_remote_password_mismatch
import moneym.feature.settings.generated.resources.settings_remote_password_repeat_label
import moneym.feature.settings.generated.resources.settings_remote_password_save
import moneym.feature.settings.generated.resources.settings_remote_password_title
import moneym.feature.settings.generated.resources.settings_remote_plaintext_warning
import moneym.feature.settings.generated.resources.settings_remote_quota_warning
import moneym.feature.settings.generated.resources.settings_remote_restore
import moneym.feature.settings.generated.resources.settings_ok
import moneym.feature.settings.generated.resources.settings_remote_restore_body
import moneym.feature.settings.generated.resources.settings_remote_restore_confirm
import moneym.feature.settings.generated.resources.settings_remote_restore_conflict
import moneym.feature.settings.generated.resources.settings_remote_restore_error_title
import moneym.feature.settings.generated.resources.settings_remote_restore_loading
import moneym.feature.settings.generated.resources.settings_remote_restore_preview_app_version
import moneym.feature.settings.generated.resources.settings_remote_restore_preview_created
import moneym.feature.settings.generated.resources.settings_remote_restore_preview_size
import moneym.feature.settings.generated.resources.settings_remote_restore_title
import moneym.feature.settings.generated.resources.settings_remote_restore_too_new
import moneym.feature.settings.generated.resources.settings_remote_signed_in_as
import moneym.feature.settings.generated.resources.settings_remote_status_decrypting
import moneym.feature.settings.generated.resources.settings_remote_status_downloading
import moneym.feature.settings.generated.resources.settings_remote_status_encrypting
import moneym.feature.settings.generated.resources.settings_remote_status_error
import moneym.feature.settings.generated.resources.settings_remote_status_restoring
import moneym.feature.settings.generated.resources.settings_remote_status_uploading
import moneym.feature.settings.generated.resources.settings_restore_app_close_notice
import moneym.feature.settings.generated.resources.settings_restore_confirm
import moneym.feature.settings.generated.resources.settings_restore_from_file
import moneym.feature.settings.generated.resources.settings_restore_passphrase_label
import moneym.feature.settings.generated.resources.settings_restore_warning_body
import moneym.feature.settings.generated.resources.settings_restore_warning_title
import moneym.feature.settings.generated.resources.settings_section_backup
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BackupRestoreKey : NavKey

fun EntryProviderScope<NavKey>.backupRestoreEntry(
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToSync: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<BackupRestoreKey>(metadata = metadata) {
    BackupRestoreScreen(onBack = onBack, onNavigateToInfo = onNavigateToInfo, onNavigateToSync = onNavigateToSync)
}

@Composable
private fun BackupRestoreScreen(
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToSync: () -> Unit,
    viewModel: BackupRestoreViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val fileSaver = rememberFileSaver { path ->
        viewModel.onIntent(BackupRestoreIntent.BackupSaveCompleted(path))
    }

    val restorePicker = rememberBinaryFilePicker { bytes ->
        if (bytes != null) viewModel.onIntent(BackupRestoreIntent.RestoreFileSelected(bytes))
    }

    val folderPicker = rememberFolderPicker { uri ->
        viewModel.onIntent(BackupRestoreIntent.AutoBackupLocationSelected(uri))
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BackupRestoreEffect.LaunchFileSaver -> fileSaver(effect.bytes, effect.fileName)
                BackupRestoreEffect.LaunchRestorePicker -> restorePicker()
                is BackupRestoreEffect.RestoreError -> Unit
                BackupRestoreEffect.LaunchFolderPicker -> folderPicker()
                BackupRestoreEffect.RemoteSignedIn -> Unit
            }
        }
    }

    if (state.showRestoreWarning) {
        RestoreWarningDialog(
            needsPassphrase = state.restoreNeedsPassphrase,
            inProgress = state.restoreInProgress,
            errorMessage = state.restoreError,
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.RestoreDismissed) },
            onConfirm = { viewModel.onIntent(BackupRestoreIntent.RestoreConfirmed(it)) },
        )
    }

    if (state.showPassphraseDialog) {
        PasswordDialog(
            errorMessage = state.passphraseError,
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.PassphrasePromptDismissed) },
            onSubmit = { value, encrypt ->
                viewModel.onIntent(BackupRestoreIntent.PasswordSubmitted(value, encrypt))
            },
        )
    }

    when (state.cloudEnableStep) {
        CloudEnableStep.Create -> PasswordDialog(
            errorMessage = state.cloudJoinError,
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.CloudEnableDismissed) },
            onSubmit = { value, encrypt -> viewModel.onIntent(BackupRestoreIntent.CloudCreateSubmitted(value, encrypt)) },
        )
        CloudEnableStep.JoinEncrypted -> CloudJoinDialog(
            busy = state.cloudBusy,
            errorMessage = state.cloudJoinError,
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.CloudEnableDismissed) },
            onSubmit = { viewModel.onIntent(BackupRestoreIntent.CloudJoinSubmitted(it)) },
        )
        CloudEnableStep.JoinPlaintext -> MmDialog(
            title = stringResource(Res.string.settings_cloud_join_plaintext_title),
            confirmText = stringResource(Res.string.settings_cloud_join_confirm),
            onConfirm = { viewModel.onIntent(BackupRestoreIntent.CloudJoinPlaintextConfirmed) },
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.CloudEnableDismissed) },
            dismissText = stringResource(Res.string.settings_remote_passphrase_cancel),
        ) {
            Text(
                stringResource(Res.string.settings_cloud_join_plaintext_body),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
        null -> Unit
    }

    if (state.showRemoteRestoreDialog) {
        RemoteRestoreDialog(
            encrypted = state.remoteRestoreEncrypted,
            preview = state.remoteRestorePreview,
            localMutationMs = state.lastLocalMutationMs,
            inProgress = state.remoteRestoreInProgress,
            errorMessage = state.remoteRestoreError,
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.RemoteRestoreDismissed) },
            onConfirm = { viewModel.onIntent(BackupRestoreIntent.RemoteRestoreConfirmed(it)) },
        )
    }

    state.remoteRestoreErrorDialog?.let { message ->
        MmErrorDialog(
            title = stringResource(Res.string.settings_remote_restore_error_title),
            message = message,
            confirmText = stringResource(Res.string.settings_ok),
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.RemoteRestoreErrorDismissed) },
        )
    }

    state.remoteErrorDialog?.let { message ->
        MmErrorDialog(
            title = stringResource(Res.string.settings_remote_error_title),
            message = message,
            confirmText = stringResource(Res.string.settings_ok),
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.RemoteErrorDismissed) },
        )
    }

    if (state.showDisconnectDialog) {
        MmDialog(
            title = stringResource(Res.string.settings_remote_disconnect_title),
            confirmText = stringResource(Res.string.settings_remote_disconnect_confirm),
            onConfirm = { viewModel.onIntent(BackupRestoreIntent.DisconnectGoogleConfirmed) },
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.DisconnectGoogleDismissed) },
            dismissText = stringResource(Res.string.settings_remote_passphrase_cancel),
        ) {
            Text(
                stringResource(Res.string.settings_remote_disconnect_body),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
    }

    if (state.showDeleteRemoteDialog) {
        MmDeleteSheet(
            title = stringResource(Res.string.settings_remote_delete_title),
            body = stringResource(Res.string.settings_remote_delete_body),
            cancelText = stringResource(Res.string.settings_remote_passphrase_cancel),
            confirmText = stringResource(Res.string.settings_remote_delete_confirm),
            onConfirm = { viewModel.onIntent(BackupRestoreIntent.DeleteRemoteDataConfirmed) },
            onCancel = { viewModel.onIntent(BackupRestoreIntent.DeleteRemoteDataDismissed) },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BackupRestoreContent(
            state = state,
            onBack = onBack,
            onNavigateToInfo = onNavigateToInfo,
            onNavigateToSync = onNavigateToSync,
            onBackupTapped = { viewModel.onIntent(BackupRestoreIntent.BackupTapped) },
            onRestoreTapped = restorePicker,
            onAutoBackupToggled = { viewModel.onIntent(BackupRestoreIntent.AutoBackupToggled(it)) },
            onConnectGoogle = { viewModel.onIntent(BackupRestoreIntent.ConnectGoogleTapped) },
            onDisconnectGoogle = { viewModel.onIntent(BackupRestoreIntent.DisconnectGoogleTapped) },
            onCloudSyncToggled = { viewModel.onIntent(BackupRestoreIntent.CloudSyncToggled(it)) },
            onRemoteBackupNow = { viewModel.onIntent(BackupRestoreIntent.RemoteBackupNowTapped) },
            onRemoteRestoreTapped = { viewModel.onIntent(BackupRestoreIntent.RemoteRestoreTapped) },
            onRetryRemoteUpload = { viewModel.onIntent(BackupRestoreIntent.RemoteBackupNowTapped) },
            onDeleteRemoteData = { viewModel.onIntent(BackupRestoreIntent.DeleteRemoteDataTapped) },
        )
        MmLoadingOverlay(visible = state.isLoading)
    }
}

@Composable
private fun BackupRestoreContent(
    state: BackupRestoreUiState,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToSync: () -> Unit,
    onBackupTapped: () -> Unit,
    onRestoreTapped: () -> Unit,
    onAutoBackupToggled: (Boolean) -> Unit,
    onConnectGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onCloudSyncToggled: (Boolean) -> Unit,
    onRemoteBackupNow: () -> Unit,
    onRemoteRestoreTapped: () -> Unit,
    onRetryRemoteUpload: () -> Unit,
    onDeleteRemoteData: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    val lastBackupLabel = remember(state.lastBackupTimeMs) { formatTime(state.lastBackupTimeMs) }
    val lastRemoteLabel = remember(state.lastRemoteBackupMs) { formatTime(state.lastRemoteBackupMs) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(
            title = stringResource(Res.string.settings_backup_restore),
            onBack = onBack,
            trailingContent = {
                MmIconButton(
                    icon = Icon.Info.imageVector,
                    onClick = onNavigateToInfo,
                    contentDescription = "Info",
                )
            },
        )

        LazyColumn(contentPadding = PaddingValues(bottom = space.padding_4x)) {
            item(key = "section_label") {
                SectionLabel(
                    text = stringResource(Res.string.settings_section_backup),
                    modifier = Modifier.padding(horizontal = space.padding_2_5x, vertical = space.padding_0_5x),
                )
            }
            item(key = "card") {
                MmCard(Modifier.padding(horizontal = space.padding_2x)) {
                    MmRow(onClick = if (state.isLocalLoading) null else onBackupTapped) {
                        Icon(imageVector = Folder.imageVector, contentDescription = null, tint = colors.text, modifier = Modifier.size(space.icon_1x))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.settings_backup_to_file), style = type.body, color = colors.text)
                            if (state.showBackupSuccess) {
                                Text(stringResource(Res.string.settings_backup_saved), style = type.caption.copy(color = colors.accent))
                            }
                        }
                        if (state.isLocalLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(space.icon_1x),
                                strokeWidth = space.padding_0_25x,
                                color = colors.accent,
                            )
                        } else {
                            Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
                        }
                    }
                    MmRow(onClick = onRestoreTapped) {
                        Icon(imageVector = Download.imageVector, contentDescription = null, tint = colors.text, modifier = Modifier.size(space.icon_1x))
                        Text(stringResource(Res.string.settings_restore_from_file), style = type.body, color = colors.text, modifier = Modifier.weight(1f))
                        Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
                    }
                    MmRow(onClick = { onAutoBackupToggled(!state.autoBackupEnabled) }, divider = false) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.settings_auto_backup), style = type.body, color = colors.text)
                            Text(stringResource(Res.string.settings_auto_backup_subtitle), style = type.caption.copy(color = colors.text2))
                        }
                        MmToggle(checked = state.autoBackupEnabled, onCheckedChange = onAutoBackupToggled)
                    }
                    if (lastBackupLabel != null) {
                        Text(
                            text = stringResource(Res.string.settings_last_backup, lastBackupLabel),
                            style = type.caption.copy(color = colors.text3),
                            modifier = Modifier.padding(start = space.padding_2x, end = space.padding_2x, bottom = space.padding_1x),
                        )
                        state.lastBackupPath?.let { path ->
                            Text(
                                text = formatBackupPath(path),
                                style = type.captionMono.copy(color = colors.text3),
                                modifier = Modifier.padding(start = space.padding_2x, end = space.padding_2x, bottom = space.padding_1x),
                            )
                        }
                    }
                }
            }
            if (state.remoteAvailable) {
                item(key = "remote_label") {
                    Spacer(Modifier.height(space.padding_2x))
                    SectionLabel(
                        text = stringResource(Res.string.settings_remote_backup_section),
                        modifier = Modifier.padding(horizontal = space.padding_2_5x, vertical = space.padding_0_5x),
                    )
                }
                item(key = "remote_card") {
                    RemoteBackupSection(
                        state = state,
                        lastRemoteLabel = lastRemoteLabel,
                        onConnect = onConnectGoogle,
                        onDisconnect = onDisconnectGoogle,
                        onCloudSyncToggled = onCloudSyncToggled,
                        onNavigateToSync = onNavigateToSync,
                        onBackupNow = onRemoteBackupNow,
                        onRestore = onRemoteRestoreTapped,
                        onRetryUpload = onRetryRemoteUpload,
                        onDeleteRemoteData = onDeleteRemoteData,
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteBackupSection(
    state: BackupRestoreUiState,
    lastRemoteLabel: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCloudSyncToggled: (Boolean) -> Unit,
    onNavigateToSync: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onRetryUpload: () -> Unit,
    onDeleteRemoteData: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val busy = state.remoteRuntime !is RemoteBackupRuntimeState.Idle &&
        state.remoteRuntime !is RemoteBackupRuntimeState.Error &&
        state.remoteRuntime !is RemoteBackupRuntimeState.QuotaWarning

    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        if (!state.remoteSignedIn) {
            MmRow(onClick = onConnect, divider = false) {
                Text(stringResource(Res.string.settings_remote_connect), style = type.body, color = colors.text, modifier = Modifier.weight(1f))
                Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
            }
        } else {
            MmRow(onClick = onDisconnect) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.remoteAccountEmail?.let {
                            stringResource(Res.string.settings_remote_signed_in_as, it)
                        } ?: stringResource(Res.string.settings_remote_connect),
                        style = type.body,
                        color = colors.text,
                    )
                    Text(stringResource(Res.string.settings_remote_disconnect), style = type.caption.copy(color = colors.text3))
                }
            }
            MmRow(onClick = { if (!busy && !state.cloudBusy) onCloudSyncToggled(!state.cloudSyncEnabled) }) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.settings_cloud_sync), style = type.body, color = colors.text)
                    Text(stringResource(Res.string.settings_cloud_sync_subtitle), style = type.caption.copy(color = colors.text2))
                }
                if (state.cloudBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(space.icon_1x),
                        strokeWidth = space.padding_0_25x,
                        color = colors.accent,
                    )
                } else {
                    MmToggle(checked = state.cloudSyncEnabled, onCheckedChange = onCloudSyncToggled, enabled = !busy)
                }
            }
            if (state.cloudSyncEnabled) {
                MmRow(onClick = onNavigateToSync) {
                    Icon(imageVector = Icon.Bolt.imageVector, contentDescription = null, tint = colors.text, modifier = Modifier.size(space.icon_1x))
                    Text(stringResource(Res.string.settings_cloud_devices), style = type.body, color = colors.text, modifier = Modifier.weight(1f))
                    Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
                }
            }
            MmRow(onClick = onBackupNow) {
                Text(stringResource(Res.string.settings_remote_backup_now), style = type.body, color = colors.text, modifier = Modifier.weight(1f))
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(space.icon_1x),
                        strokeWidth = space.padding_0_25x,
                        color = colors.accent,
                    )
                } else {
                    Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
                }
            }
            MmRow(onClick = onRestore) {
                Icon(imageVector = Download.imageVector, contentDescription = null, tint = colors.text, modifier = Modifier.size(space.icon_1x))
                Text(stringResource(Res.string.settings_remote_restore), style = type.body, color = colors.text, modifier = Modifier.weight(1f))
                Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
            }
            MmRow(onClick = onDeleteRemoteData, divider = false) {
                Icon(imageVector = Icon.Trash.imageVector, contentDescription = null, tint = colors.danger, modifier = Modifier.size(space.icon_1x))
                Text(stringResource(Res.string.settings_remote_delete_data), style = type.body, color = colors.danger, modifier = Modifier.weight(1f))
                Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
            }
            RuntimeStatusLine(state.remoteRuntime, state.lastRemoteBackupMs, onRetryUpload)
        }
    }
}

@Composable
private fun RuntimeStatusLine(
    runtime: RemoteBackupRuntimeState,
    lastRemoteMs: Long,
    onRetry: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    val idleLabel: String = if (lastRemoteMs == 0L) {
        stringResource(Res.string.settings_remote_last_backup_never)
    } else {
        stringResource(Res.string.settings_remote_last_backup, formatTime(lastRemoteMs) ?: "—")
    }

    val message: String = when (runtime) {
        RemoteBackupRuntimeState.Encrypting -> stringResource(Res.string.settings_remote_status_encrypting)
        RemoteBackupRuntimeState.Uploading -> stringResource(Res.string.settings_remote_status_uploading)
        RemoteBackupRuntimeState.Downloading -> stringResource(Res.string.settings_remote_status_downloading)
        RemoteBackupRuntimeState.Decrypting -> stringResource(Res.string.settings_remote_status_decrypting)
        RemoteBackupRuntimeState.Restoring -> stringResource(Res.string.settings_remote_status_restoring)
        is RemoteBackupRuntimeState.Error -> stringResource(Res.string.settings_remote_status_error, runtime.message)
        is RemoteBackupRuntimeState.QuotaWarning -> stringResource(
            Res.string.settings_remote_quota_warning,
            (runtime.remainingBytes / 1024L).toInt(),
            (runtime.requiredBytes / 1024L).toInt(),
        )
        RemoteBackupRuntimeState.Idle -> idleLabel
    }

    Column(
        modifier = Modifier.padding(start = space.padding_2x, end = space.padding_2x, bottom = space.padding_1x, top = space.padding_0_5x),
    ) {
        Text(
            text = message,
            style = type.caption.copy(color = if (runtime is RemoteBackupRuntimeState.Error || runtime is RemoteBackupRuntimeState.QuotaWarning) colors.danger else colors.text3),
        )
        if (runtime is RemoteBackupRuntimeState.Error) {
            Text(
                text = stringResource(Res.string.settings_remote_status_retry),
                style = type.caption.copy(color = colors.accent),
                modifier = Modifier
                    .padding(top = space.padding_0_5x)
                    .clickable { onRetry() },
            )
        }
    }
}

@Composable
private fun RestoreWarningDialog(
    needsPassphrase: Boolean,
    inProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (CharArray?) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    var passphrase by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    MmDialog(
        title = stringResource(Res.string.settings_restore_warning_title),
        confirmText = stringResource(Res.string.settings_restore_confirm),
        confirmEnabled = !inProgress && (!needsPassphrase || passphrase.isNotEmpty()),
        onConfirm = { onConfirm(if (needsPassphrase) passphrase.toCharArray() else null) },
        onDismiss = onDismiss,
        dismissText = if (inProgress) null else stringResource(Res.string.settings_remote_passphrase_cancel),
        dismissible = !inProgress,
    ) {
        Text(
            stringResource(Res.string.settings_restore_warning_body),
            style = type.body,
            color = colors.text2,
        )
        Text(
            stringResource(Res.string.settings_restore_app_close_notice),
            style = type.caption,
            color = colors.text3,
            modifier = Modifier.padding(top = space.padding_1x),
        )
        if (needsPassphrase) {
            MmField(
                value = passphrase,
                onValueChange = { if (!inProgress) passphrase = it },
                label = stringResource(Res.string.settings_restore_passphrase_label),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                suffix = {
                    MmIconButton(
                        icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                        onClick = { visible = !visible },
                        contentDescription = null,
                    )
                },
            )
        }
        if (inProgress) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = space.padding_1x).size(space.icon_1x),
                strokeWidth = space.padding_0_25x,
                color = colors.accent,
            )
        }
        if (errorMessage != null) {
            Text(errorMessage, color = colors.danger, style = type.caption)
        }
    }
}

@Composable
private fun PasswordDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (CharArray, Boolean) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var dontEncrypt by remember { mutableStateOf(false) }

    val mismatch = !dontEncrypt && repeat.isNotEmpty() && password != repeat
    val valid = dontEncrypt || (password.length >= MIN_PASSWORD_LENGTH && password == repeat)

    MmDialog(
        title = stringResource(Res.string.settings_remote_password_title),
        confirmText = stringResource(Res.string.settings_remote_password_save),
        confirmEnabled = valid,
        onConfirm = { onSubmit(password.toCharArray(), !dontEncrypt) },
        onDismiss = onDismiss,
        dismissText = stringResource(Res.string.settings_remote_passphrase_cancel),
    ) {
        Text(
            stringResource(Res.string.settings_remote_password_body),
            style = type.body,
            color = colors.text2,
        )
        if (!dontEncrypt) {
            MmField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(Res.string.settings_remote_password_label),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                suffix = {
                    MmIconButton(
                        icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                        onClick = { visible = !visible },
                        contentDescription = null,
                    )
                },
            )
            MmField(
                value = repeat,
                onValueChange = { repeat = it },
                label = stringResource(Res.string.settings_remote_password_repeat_label),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                suffix = {
                    MmIconButton(
                        icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                        onClick = { visible = !visible },
                        contentDescription = null,
                    )
                },
            )
            if (mismatch) {
                Text(
                    stringResource(Res.string.settings_remote_password_mismatch),
                    color = colors.danger,
                    style = type.caption,
                )
            }
        }
        MmRow(onClick = { dontEncrypt = !dontEncrypt }, divider = false) {
            Text(
                stringResource(Res.string.settings_remote_dont_encrypt),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(checked = dontEncrypt, onCheckedChange = { dontEncrypt = it })
        }
        if (dontEncrypt) {
            Text(
                stringResource(Res.string.settings_remote_plaintext_warning),
                color = colors.danger,
                style = type.caption,
            )
        }
        if (errorMessage != null) {
            Text(errorMessage, color = colors.danger, style = type.caption)
        }
    }
}

@Composable
private fun CloudJoinDialog(
    busy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (CharArray) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    MmDialog(
        title = stringResource(Res.string.settings_cloud_join_title),
        confirmText = stringResource(Res.string.settings_cloud_join_confirm),
        confirmEnabled = password.isNotEmpty() && !busy,
        onConfirm = { onSubmit(password.toCharArray()) },
        onDismiss = onDismiss,
        dismissText = if (busy) null else stringResource(Res.string.settings_remote_passphrase_cancel),
        dismissible = !busy,
    ) {
        Text(stringResource(Res.string.settings_cloud_join_body), style = type.body, color = colors.text2)
        MmField(
            value = password,
            onValueChange = { if (!busy) password = it },
            label = stringResource(Res.string.settings_remote_password_label),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password,
            suffix = {
                MmIconButton(
                    icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                    onClick = { visible = !visible },
                    contentDescription = null,
                )
            },
        )
        if (errorMessage != null) {
            Text(errorMessage, color = colors.danger, style = type.caption)
        }
    }
}

private const val MIN_PASSWORD_LENGTH = 4

@Composable
private fun RemoteRestoreDialog(
    encrypted: Boolean,
    preview: RemoteBackupMetadata?,
    localMutationMs: Long,
    inProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val space = MM.dimen
    val type = MM.type
    val colors = MM.colors
    val tooNew = preview != null && preview.envelopeVersion > EncryptedBackup.ENVELOPE_VERSION
    val conflict = preview != null && localMutationMs > preview.createdAtMs

    MmDialog(
        title = stringResource(Res.string.settings_remote_restore_title),
        confirmText = stringResource(Res.string.settings_remote_restore_confirm),
        confirmEnabled = (!encrypted || input.isNotEmpty()) && !tooNew && !inProgress,
        onConfirm = { onConfirm(if (encrypted) input.toCharArray() else CharArray(0)) },
        onDismiss = onDismiss,
        dismissText = if (inProgress) null else stringResource(Res.string.settings_remote_passphrase_cancel),
        dismissible = !inProgress,
    ) {
        Text(stringResource(Res.string.settings_remote_restore_body), style = type.body, color = colors.text2)
        Text(
            stringResource(Res.string.settings_restore_app_close_notice),
            style = type.caption,
            color = colors.text3,
            modifier = Modifier.padding(top = space.padding_1x),
        )
        if (inProgress) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                modifier = Modifier.padding(top = space.padding_1x),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(space.icon_1x),
                    strokeWidth = space.padding_0_25x,
                    color = colors.accent,
                )
                Text(stringResource(Res.string.settings_remote_restore_loading), style = type.caption)
            }
        } else if (preview != null) {
            Column(modifier = Modifier.padding(top = space.padding_1x)) {
                Text(
                    stringResource(
                        Res.string.settings_remote_restore_preview_created,
                        formatTime(preview.createdAtMs) ?: "—",
                    ),
                    style = type.caption,
                )
                Text(
                    stringResource(
                        Res.string.settings_remote_restore_preview_app_version,
                        preview.appVersion,
                    ),
                    style = type.caption,
                )
                Text(
                    stringResource(
                        Res.string.settings_remote_restore_preview_size,
                        (preview.sizeBytes / 1024L).toInt(),
                    ),
                    style = type.caption,
                )
            }
            if (tooNew) {
                Text(
                    stringResource(Res.string.settings_remote_restore_too_new),
                    style = type.caption.copy(color = colors.danger),
                    modifier = Modifier.padding(top = space.padding_1x),
                )
            }
            if (conflict && !tooNew) {
                Text(
                    stringResource(
                        Res.string.settings_remote_restore_conflict,
                        formatTime(localMutationMs) ?: "",
                    ),
                    style = type.caption.copy(color = colors.danger),
                    modifier = Modifier.padding(top = space.padding_1x),
                )
            }
        }
        if (encrypted) {
            MmField(
                value = input,
                onValueChange = { if (!tooNew && !inProgress) input = it },
                label = stringResource(Res.string.settings_remote_passphrase_label),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                suffix = {
                    MmIconButton(
                        icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                        onClick = { visible = !visible },
                        contentDescription = null,
                    )
                },
            )
        }
        if (errorMessage != null) {
            Text(errorMessage, color = colors.danger, style = type.caption)
        }
    }
}

private fun formatBackupPath(raw: String): String {
    val decoded = raw
        .replace("%3A", ":").replace("%3a", ":")
        .replace("%2F", "/").replace("%2f", "/")
        .replace("%20", " ")
    val treeIdx = decoded.lastIndexOf("/tree/")
    val docIdx = decoded.lastIndexOf("/document/")
    val startIdx = when {
        treeIdx >= 0 -> treeIdx + 6
        docIdx >= 0 -> docIdx + 10
        else -> -1
    }
    val docId = if (startIdx >= 0) decoded.substring(startIdx) else decoded
    return docId.replace(":", "/")
}

private fun formatTime(ms: Long): String? {
    if (ms == 0L) return null
    return formatDateTime(ms)
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun BackupRestoreContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        BackupRestoreContent(
            state = BackupRestoreUiState(
                autoBackupEnabled = true,
                lastBackupTimeMs = 1716700000000L,
                remoteAvailable = true,
                remoteSignedIn = true,
            ),
            onBack = {},
            onNavigateToInfo = {},
            onNavigateToSync = {},
            onBackupTapped = {},
            onRestoreTapped = {},
            onAutoBackupToggled = {},
            onConnectGoogle = {},
            onDisconnectGoogle = {},
            onCloudSyncToggled = {},
            onRemoteBackupNow = {},
            onRemoteRestoreTapped = {},
            onRetryRemoteUpload = {},
            onDeleteRemoteData = {},
        )
    }
}
