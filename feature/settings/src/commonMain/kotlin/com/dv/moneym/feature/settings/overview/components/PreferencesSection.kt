package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.imageVector
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_categories
import moneym.feature.settings.generated.resources.settings_language
import moneym.feature.settings.generated.resources.settings_payment_mode_enabled
import moneym.feature.settings.generated.resources.settings_payment_modes
import moneym.feature.settings.generated.resources.settings_wallets
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PreferencesSection(
    languageSubtitle: String,
    paymentModeEnabled: Boolean,
    onNavigateToLanguage: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToPaymentModes: () -> Unit,
    onPaymentModeEnabledChanged: (Boolean) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow(onClick = onNavigateToLanguage) {
            Icon(
                imageVector = Icon.Globe.imageVector,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.settings_language),
                    style = type.body,
                    color = colors.text
                )
                Text(languageSubtitle, style = type.caption.copy(color = colors.text2))
            }
            Icon(
                imageVector = Icon.ChevronRight.imageVector,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(onClick = onNavigateToCategories) {
            Icon(
                imageVector = Icon.List.imageVector,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_categories),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icon.ChevronRight.imageVector,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(onClick = onNavigateToWallets) {
            Icon(
                imageVector = Icon.Wallet.imageVector,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_wallets),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icon.ChevronRight.imageVector,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(
            divider = paymentModeEnabled,
            onClick = { onPaymentModeEnabledChanged(!paymentModeEnabled) }) {
            Icon(
                imageVector = Icon.Banknote.imageVector,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_payment_mode_enabled),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(
                checked = paymentModeEnabled,
                onCheckedChange = onPaymentModeEnabledChanged,
            )
        }
        if (paymentModeEnabled) {
            MmRow(divider = false, onClick = onNavigateToPaymentModes) {
                Icon(
                    imageVector = Icon.List.imageVector,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(MM.dimen.icon_1x),
                )
                Text(
                    stringResource(Res.string.settings_payment_modes),
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icon.ChevronRight.imageVector,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(MM.dimen.padding_2x),
                )
            }
        }
    }
}
