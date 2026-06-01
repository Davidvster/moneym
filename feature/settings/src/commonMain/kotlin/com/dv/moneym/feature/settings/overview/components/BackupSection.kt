package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon.Folder
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.imageVector
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_backup_restore
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BackupSection(
    onNavigateToBackupRestore: () -> Unit,
) {
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmSettingsRow(
            title = stringResource(Res.string.settings_backup_restore),
            leadingIcon = Folder.imageVector,
            onClick = onNavigateToBackupRestore,
            divider = false,
        )
    }
}
