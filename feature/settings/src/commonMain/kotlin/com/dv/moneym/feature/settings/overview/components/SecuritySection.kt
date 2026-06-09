package com.dv.moneym.feature.settings.overview.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
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
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.feature.settings.overview.SecuritySettingsIntent
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_allow_screenshots
import moneym.feature.settings.generated.resources.settings_allow_screenshots_subtitle
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
    allowScreenshots: Boolean,
    lockAfterLabel: String,
    onIntent: (SecuritySettingsIntent) -> Unit,
    onShowLockPicker: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MM.dimen.padding_7x)
                .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (pinEnabled) onIntent(SecuritySettingsIntent.ChangePinRequested)
                        else onIntent(SecuritySettingsIntent.PinToggled(true))
                    },
            ) {
                Text(
                    stringResource(Res.string.settings_pin_lock),
                    style = type.body,
                    color = colors.text
                )
                if (pinEnabled) {
                    Text(
                        stringResource(Res.string.settings_change_pin),
                        style = type.caption.copy(color = colors.text2)
                    )
                }
            }
            MmToggle(
                checked = pinEnabled,
                onCheckedChange = { onIntent(SecuritySettingsIntent.PinToggled(it)) },
            )
        }
        if (biometricAvailable) {
            MmRow(
                modifier = Modifier.alpha(if (pinEnabled) 1f else 0.45f),
            ) {
                Icon(
                    imageVector = Icon.Fingerprint.imageVector,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(MM.dimen.icon_1x),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_biometrics),
                        style = type.body,
                        color = colors.text
                    )
                    Text(
                        stringResource(Res.string.settings_biometrics_subtitle),
                        style = type.caption.copy(color = colors.text2)
                    )
                }
                MmToggle(
                    checked = biometricEnabled,
                    onCheckedChange = { onIntent(SecuritySettingsIntent.BiometricToggled(it)) },
                    enabled = pinEnabled,
                )
            }
        }
        MmRow(onClick = onShowLockPicker, divider = true) {
            Text(
                stringResource(Res.string.settings_lock_after),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Text(lockAfterLabel, style = type.caption.copy(color = colors.text2))
            Icon(
                imageVector = Icon.ChevronRight.imageVector,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(
            divider = false,
            onClick = { onIntent(SecuritySettingsIntent.ScreenshotsToggled(!allowScreenshots)) },
        ) {
            Icon(
                imageVector = Icon.EyeOff.imageVector,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.settings_allow_screenshots),
                    style = type.body,
                    color = colors.text
                )
                Text(
                    stringResource(Res.string.settings_allow_screenshots_subtitle),
                    style = type.caption.copy(color = colors.text2)
                )
            }
            MmToggle(
                checked = allowScreenshots,
                onCheckedChange = { onIntent(SecuritySettingsIntent.ScreenshotsToggled(it)) },
            )
        }
    }
}
