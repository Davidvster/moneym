package com.dv.moneym.feature.settings.overview.backuprestore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon.ChevronRight
import com.dv.moneym.core.model.Icon.Download
import com.dv.moneym.core.model.Icon.Folder
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.platform.rememberBinaryFilePicker
import com.dv.moneym.platform.rememberFileSaver
import com.dv.moneym.platform.rememberFolderPicker
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_auto_backup
import moneym.feature.settings.generated.resources.settings_auto_backup_subtitle
import moneym.feature.settings.generated.resources.settings_backup_restore
import moneym.feature.settings.generated.resources.settings_backup_saved
import moneym.feature.settings.generated.resources.settings_backup_to_file
import moneym.feature.settings.generated.resources.settings_last_backup
import moneym.feature.settings.generated.resources.settings_restore_confirm
import moneym.feature.settings.generated.resources.settings_restore_from_file
import moneym.feature.settings.generated.resources.settings_restore_warning_body
import moneym.feature.settings.generated.resources.settings_restore_warning_title
import moneym.feature.settings.generated.resources.settings_section_backup
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BackupRestoreKey : NavKey

fun EntryProviderScope<NavKey>.backupRestoreEntry(
    onBack: () -> Unit,
) = entry<BackupRestoreKey> {
    BackupRestoreScreen(onBack = onBack)
}

@Composable
private fun BackupRestoreScreen(
    onBack: () -> Unit,
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
            }
        }
    }

    if (state.showRestoreWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(BackupRestoreIntent.RestoreDismissed) },
            title = { Text(stringResource(Res.string.settings_restore_warning_title)) },
            text = { Text(stringResource(Res.string.settings_restore_warning_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(BackupRestoreIntent.RestoreConfirmed) }) {
                    Text(stringResource(Res.string.settings_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(BackupRestoreIntent.RestoreDismissed) }) {
                    Text("Cancel")
                }
            },
        )
    }

    BackupRestoreContent(
        state = state,
        onBack = onBack,
        onBackupTapped = { viewModel.onIntent(BackupRestoreIntent.BackupTapped) },
        onRestoreTapped = restorePicker,
        onAutoBackupToggled = { viewModel.onIntent(BackupRestoreIntent.AutoBackupToggled(it)) },
    )
}

@Composable
private fun BackupRestoreContent(
    state: BackupRestoreUiState,
    onBack: () -> Unit,
    onBackupTapped: () -> Unit,
    onRestoreTapped: () -> Unit,
    onAutoBackupToggled: (Boolean) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    val lastBackupLabel = remember(state.lastBackupTimeMs) {
        if (state.lastBackupTimeMs == 0L) null
        else {
            val dt = Instant.fromEpochMilliseconds(state.lastBackupTimeMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val h = dt.hour.toString().padStart(2, '0')
            val m = dt.minute.toString().padStart(2, '0')
            "${dt.date} $h:$m"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(title = stringResource(Res.string.settings_backup_restore), onBack = onBack)

        LazyColumn {
            item(key = "section_label") {
                SectionLabel(
                    text = stringResource(Res.string.settings_section_backup),
                    modifier = Modifier.padding(horizontal = space.padding_2_5x, vertical = space.padding_0_5x),
                )
            }
            item(key = "card") {
                MmCard(Modifier.padding(horizontal = space.padding_2x)) {
                    MmRow(onClick = onBackupTapped) {
                        Icon(imageVector = Folder.imageVector, contentDescription = null, tint = colors.text, modifier = Modifier.size(space.icon_1x))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.settings_backup_to_file), style = type.body, color = colors.text)
                            if (state.showBackupSuccess) {
                                Text(stringResource(Res.string.settings_backup_saved), style = type.caption.copy(color = colors.accent))
                            }
                        }
                        Icon(imageVector = ChevronRight.imageVector, contentDescription = null, tint = colors.text3, modifier = Modifier.size(space.padding_2x))
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
                                text = path,
                                style = type.captionMono.copy(color = colors.text3),
                                modifier = Modifier.padding(start = space.padding_2x, end = space.padding_2x, bottom = space.padding_1x),
                            )
                        }
                    }
                }
            }
        }
    }
}
