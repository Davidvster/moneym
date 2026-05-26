package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.imageVector
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_budgets
import moneym.feature.settings.generated.resources.settings_categories
import moneym.feature.settings.generated.resources.settings_language
import moneym.feature.settings.generated.resources.settings_payment_mode_enabled
import moneym.feature.settings.generated.resources.settings_payment_modes
import moneym.feature.settings.generated.resources.settings_recurring_nav
import moneym.feature.settings.generated.resources.settings_wallets
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PreferencesSection(
    languageSubtitle: String,
    paymentModeEnabled: Boolean,
    onNavigateToLanguage: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToPaymentModes: () -> Unit,
    onPaymentModeEnabledChanged: (Boolean) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmSettingsRow(
            title = stringResource(Res.string.settings_language),
            subtitle = languageSubtitle,
            leadingIcon = Icon.Globe.imageVector,
            onClick = onNavigateToLanguage,
        )
        MmSettingsRow(
            title = stringResource(Res.string.settings_categories),
            leadingIcon = Icon.List.imageVector,
            onClick = onNavigateToCategories,
        )
        MmSettingsRow(
            title = stringResource(Res.string.settings_budgets),
            leadingIcon = Icon.Chart.imageVector,
            onClick = onNavigateToBudgets,
        )
        MmSettingsRow(
            title = stringResource(Res.string.settings_recurring_nav),
            leadingIcon = Icon.Calendar.imageVector,
            onClick = onNavigateToRecurring,
        )
        MmSettingsRow(
            title = stringResource(Res.string.settings_wallets),
            leadingIcon = Icon.Wallet.imageVector,
            onClick = onNavigateToWallets,
        )
        MmRow(
            divider = paymentModeEnabled,
            onClick = { onPaymentModeEnabledChanged(!paymentModeEnabled) },
        ) {
            androidx.compose.material3.Icon(
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
            MmSettingsRow(
                title = stringResource(Res.string.settings_payment_modes),
                leadingIcon = Icon.List.imageVector,
                onClick = onNavigateToPaymentModes,
                divider = false,
            )
        }
    }
}
