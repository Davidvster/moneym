package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon.ChevronRight
import com.dv.moneym.core.model.Icon.Folder
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.imageVector
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_backup_restore
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BackupSection(
    onNavigateToBackupRestore: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow(onClick = onNavigateToBackupRestore, divider = false) {
            Icon(
                imageVector = Folder.imageVector,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_backup_restore),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = ChevronRight.imageVector,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
    }
}
