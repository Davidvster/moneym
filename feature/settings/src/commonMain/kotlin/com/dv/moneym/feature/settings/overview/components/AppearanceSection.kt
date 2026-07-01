package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.imageVector
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
    onOpenThemeSheet: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
) {
    val space = MM.dimen
    val isDark = themeMode == ThemeMode.Dark
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmSettingsRow(
            title = stringResource(Res.string.settings_theme),
            leadingIcon = if (isDark) Icon.Moon.imageVector else Icon.Sun.imageVector,
            onClick = onOpenThemeSheet,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(themeLabel(themeMode), style = MM.type.caption, color = MM.colors.text2)
                    Spacer(Modifier.width(MM.dimen.padding_0_5x))
                    Icon(
                        imageVector = Icon.ChevronRight.imageVector,
                        contentDescription = null,
                        tint = MM.colors.text3,
                        modifier = Modifier.size(MM.dimen.padding_2x),
                    )
                }
            },
        )
        MmSettingsRow(
            title = stringResource(Res.string.settings_tx_list),
            leadingIcon = Icon.Sliders.imageVector,
            onClick = onNavigateToTxDisplay,
            divider = false,
        )
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.Light -> stringResource(Res.string.settings_theme_light)
    ThemeMode.Dark -> stringResource(Res.string.settings_theme_dark)
    ThemeMode.Auto -> stringResource(Res.string.settings_theme_auto)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePickerSheet(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = space.padding_2_5x, topEnd = space.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = space.padding_2_5x, vertical = space.padding_3x),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 36.dp, height = space.padding_0_5x)
                        .clip(RoundedCornerShape(50)).background(colors.borderStrong),
                )
            }
            MmSheetHeader(title = stringResource(Res.string.settings_theme), onClose = onDismiss)
            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_25x)) {
                listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.Auto).forEach { mode ->
                    MmRow(
                        onClick = { onSelect(mode) },
                        divider = false,
                        padding = PaddingValues(horizontal = space.padding_0_5x, vertical = space.padding_1x),
                    ) {
                        Text(themeLabel(mode), style = MM.type.body, color = colors.text, modifier = Modifier.weight(1f))
                        if (mode == current) {
                            Icon(
                                imageVector = Icon.Check.imageVector,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(space.iconMd),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(space.padding_1x))
        }
    }
}
