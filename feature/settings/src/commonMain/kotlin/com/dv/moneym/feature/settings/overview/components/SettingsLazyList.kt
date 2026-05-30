package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.settings.overview.SecuritySettingsIntent
import com.dv.moneym.feature.settings.overview.SecuritySettingsUiState
import com.dv.moneym.feature.settings.overview.SettingsItem
import com.dv.moneym.feature.settings.overview.SettingsUiState
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_section_appearance
import moneym.feature.settings.generated.resources.settings_section_backup
import moneym.feature.settings.generated.resources.settings_section_data
import moneym.feature.settings.generated.resources.settings_section_preferences
import moneym.feature.settings.generated.resources.settings_section_security
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsLazyList(
    modifier: Modifier,
    state: SettingsUiState,
    securityState: SecuritySettingsUiState,
    themeIndex: Int,
    themeModes: List<ThemeMode>,
    lockAfterLabel: String,
    languageSubtitle: String,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onPaymentModeEnabledChanged: (Boolean) -> Unit,
    onUseCurrencySymbolChanged: (Boolean) -> Unit,
    onSecurityIntent: (SecuritySettingsIntent) -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToPaymentModes: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onShowLockPicker: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    LazyColumn(modifier = modifier) {
        item(key = SettingsItem.APPEARANCE_LABEL.name) {
            SectionLabel(
                text = stringResource(Res.string.settings_section_appearance),
                modifier = Modifier.padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = space.padding_0_5x
                )
            )
        }
        item(key = SettingsItem.APPEARANCE_CARD.name) {
            AppearanceSection(
                themeMode = state.themeMode,
                themeIndex = themeIndex,
                themeModes = themeModes,
                onThemeModeChanged = onThemeModeChanged,
                onNavigateToTxDisplay = onNavigateToTxDisplay,
            )
        }
        item(key = SettingsItem.SECURITY_LABEL.name) {
            SectionLabel(
                text = stringResource(Res.string.settings_section_security),
                modifier = Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_2x,
                    bottom = space.padding_0_5x
                ),
            )
        }
        item(key = SettingsItem.SECURITY_CARD.name) {
            SecuritySection(
                pinEnabled = securityState.pinEnabled,
                biometricAvailable = securityState.biometricAvailable,
                biometricEnabled = securityState.biometricEnabled,
                lockAfterLabel = lockAfterLabel,
                onIntent = onSecurityIntent,
                onShowLockPicker = onShowLockPicker,
            )
        }
        item(key = SettingsItem.PREFERENCES_LABEL.name) {
            SectionLabel(
                text = stringResource(Res.string.settings_section_preferences),
                modifier = Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_2x,
                    bottom = space.padding_0_5x
                ),
            )
        }
        item(key = SettingsItem.PREFERENCES_CARD.name) {
            PreferencesSection(
                languageSubtitle = languageSubtitle,
                paymentModeEnabled = state.paymentModeEnabled,
                useCurrencySymbol = state.useCurrencySymbol,
                walletCurrency = state.walletCurrency,
                onNavigateToLanguage = onNavigateToLanguage,
                onNavigateToCategories = onNavigateToCategories,
                onNavigateToBudgets = onNavigateToBudgets,
                onNavigateToRecurring = onNavigateToRecurring,
                onNavigateToWallets = onNavigateToWallets,
                onNavigateToPaymentModes = onNavigateToPaymentModes,
                onPaymentModeEnabledChanged = onPaymentModeEnabledChanged,
                onUseCurrencySymbolChanged = onUseCurrencySymbolChanged,
            )
        }
        item(key = SettingsItem.DATA_LABEL.name) {
            SectionLabel(
                text = stringResource(Res.string.settings_section_data),
                modifier = Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_2x,
                    bottom = space.padding_0_5x
                )
            )
        }
        item(key = SettingsItem.DATA_CARD.name) {
            DataSection(
                onNavigateToExport = onNavigateToExport,
            )
        }
        item(key = SettingsItem.BACKUP_LABEL.name) {
            SectionLabel(
                text = stringResource(Res.string.settings_section_backup),
                modifier = Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_2x,
                    bottom = space.padding_0_5x
                )
            )
        }
        item(key = SettingsItem.BACKUP_CARD.name) {
            BackupSection(onNavigateToBackupRestore = onNavigateToBackupRestore)
        }
        item(key = SettingsItem.VERSION.name) {
            Text(
                text = "MoneyM v1.0",
                style = type.captionMono.copy(color = colors.text3),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(space.padding_3x),
            )
        }
    }
}
