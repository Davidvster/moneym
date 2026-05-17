package com.dv.moneym.feature.settings.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.feature.settings.presentation.SettingsIntent
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_biometrics
import moneym.feature.settings.generated.resources.settings_biometrics_subtitle
import moneym.feature.settings.generated.resources.settings_change_pin
import moneym.feature.settings.generated.resources.settings_lock_after
import moneym.feature.settings.generated.resources.settings_pin_lock
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SecuritySection(
    pinEnabled: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    lockAfterLabel: String,
    onIntent: (SettingsIntent) -> Unit,
    onShowLockPicker: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = MM.space.padding_2_5x, vertical = MM.space.padding_2x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (pinEnabled) onIntent(SettingsIntent.ChangePinRequested)
                        else onIntent(SettingsIntent.PinToggled(true))
                    },
            ) {
                Text(stringResource(Res.string.settings_pin_lock), style = type.body, color = colors.text)
                if (pinEnabled) {
                    Text(stringResource(Res.string.settings_change_pin), style = type.caption.copy(color = colors.text2))
                }
            }
            MmToggle(
                checked = pinEnabled,
                onCheckedChange = { onIntent(SettingsIntent.PinToggled(it)) },
            )
        }
        if (biometricAvailable) {
            MmRow(
                modifier = Modifier.alpha(if (pinEnabled) 1f else 0.45f),
            ) {
                Icon(
                    imageVector = MmIcons.fingerprint,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.settings_biometrics), style = type.body, color = colors.text)
                    Text(stringResource(Res.string.settings_biometrics_subtitle), style = type.caption.copy(color = colors.text2))
                }
                MmToggle(
                    checked = biometricEnabled,
                    onCheckedChange = { onIntent(SettingsIntent.BiometricToggled(it)) },
                    enabled = pinEnabled,
                )
            }
        }
        MmRow(onClick = onShowLockPicker, divider = false) {
            Text(
                stringResource(Res.string.settings_lock_after),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Text(lockAfterLabel, style = type.caption.copy(color = colors.text2))
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.space.padding_2x),
            )
        }
    }
}
