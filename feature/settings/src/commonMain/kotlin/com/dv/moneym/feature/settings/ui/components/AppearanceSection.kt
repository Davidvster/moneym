package com.dv.moneym.feature.settings.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.feature.settings.presentation.SettingsIntent
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
    txDisplaySummary: String,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToTxDisplay: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val isDark = themeMode == ThemeMode.Dark
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow {
            Icon(
                imageVector = if (isDark) MmIcons.moon else MmIcons.sun,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Text(stringResource(Res.string.settings_theme), style = type.body, color = colors.text, modifier = Modifier.weight(1f))
            MmSegmented(
                options = listOf(
                    stringResource(Res.string.settings_theme_light),
                    stringResource(Res.string.settings_theme_dark),
                    stringResource(Res.string.settings_theme_auto),
                ),
                selectedIndex = themeIndex,
                onOptionSelected = { onIntent(SettingsIntent.ThemeModeChanged(themeModes[it])) },
                size = MmSegmentedSize.Sm,
            )
        }
        MmRow(onClick = onNavigateToTxDisplay, divider = false) {
            Icon(
                imageVector = MmIcons.sliders,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.settings_tx_list), style = type.body, color = colors.text)
                Text(txDisplaySummary, style = type.caption.copy(color = colors.text2))
            }
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.space.padding_2x),
            )
        }
    }
}
