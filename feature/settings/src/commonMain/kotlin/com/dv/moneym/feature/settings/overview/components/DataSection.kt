package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_export_data
import moneym.feature.settings.generated.resources.settings_import_data
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DataSection(
    onNavigateToExport: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow(onClick = onNavigateToExport) {
            Icon(
                imageVector = MmIcons.download,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_export_data),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(onClick = onNavigateToExport, divider = false) {
            Icon(
                imageVector = MmIcons.folder,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_import_data),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
    }
}
