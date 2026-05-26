package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_theme
import moneym.feature.settings.generated.resources.settings_theme_auto
import moneym.feature.settings.generated.resources.settings_theme_dark
import moneym.feature.settings.generated.resources.settings_theme_light
import moneym.feature.settings.generated.resources.settings_tx_list
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppearanceSection(
    themeMode: ThemeMode,
    themeIndex: Int,
    themeModes: List<ThemeMode>,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onNavigateToTxDisplay: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val isDark = themeMode == ThemeMode.Dark
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow {
            Icon(
                imageVector = if (isDark) Icon.Moon.imageVector else Icon.Sun.imageVector,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_theme),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f)
            )
            MmSegmented(
                options = listOf(
                    stringResource(Res.string.settings_theme_light),
                    stringResource(Res.string.settings_theme_dark),
                    stringResource(Res.string.settings_theme_auto),
                ),
                selectedIndex = themeIndex,
                onOptionSelected = { onThemeModeChanged(themeModes[it]) },
                size = MmSegmentedSize.Sm,
            )
        }
        MmSettingsRow(
            title = stringResource(Res.string.settings_tx_list),
            leadingIcon = Icon.Sliders.imageVector,
            onClick = onNavigateToTxDisplay,
            divider = false,
        )
    }
}
