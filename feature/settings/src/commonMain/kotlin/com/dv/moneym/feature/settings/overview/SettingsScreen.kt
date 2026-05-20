package com.dv.moneym.feature.settings.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.settings.overview.components.LockTimeoutPickerDialog
import com.dv.moneym.feature.settings.overview.components.SettingsLazyList
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_lang_system_default
import moneym.feature.settings.generated.resources.settings_lock_1m
import moneym.feature.settings.generated.resources.settings_lock_30s
import moneym.feature.settings.generated.resources.settings_lock_5m
import moneym.feature.settings.generated.resources.settings_lock_immediately
import moneym.feature.settings.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SettingsKey : NavKey

@Serializable
data object TxListDisplayKey : NavKey

@Serializable
data object CurrencyPickerKey : NavKey

@Serializable
data object LanguagePickerKey : NavKey

@Serializable
data object PaymentModeListKey : NavKey

enum class SettingsItem {
    APPEARANCE_LABEL,
    APPEARANCE_CARD,
    SECURITY_LABEL,
    SECURITY_CARD,
    PREFERENCES_LABEL,
    PREFERENCES_CARD,
    DATA_LABEL,
    DATA_CARD,
    BACKUP_LABEL,
    BACKUP_CARD,
    VERSION,
}

fun EntryProviderScope<NavKey>.settingsEntry(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit = {},
    onNavigateToWallets: () -> Unit = {},
    onNavigateToPaymentModes: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onTabSelected: (TabRoute) -> Unit,
    securityViewModel: SecuritySettingsViewModel? = null,
) = entry<SettingsKey> {
    SettingsScreen(
        onNavigateToPinSetup = onNavigateToPinSetup,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToTxDisplay = onNavigateToTxDisplay,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToExport = onNavigateToExport,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToPaymentModes = onNavigateToPaymentModes,
        onNavigateToBackupRestore = onNavigateToBackupRestore,
        onTabSelected = onTabSelected,
        securityViewModel = securityViewModel ?: koinViewModel(),
    )
}

@Composable
fun SettingsScreen(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToTxDisplay: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToWallets: () -> Unit = {},
    onNavigateToPaymentModes: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onTabSelected: (TabRoute) -> Unit = {},
    overviewViewModel: SettingsOverviewViewModel = koinViewModel(),
    securityViewModel: SecuritySettingsViewModel = koinViewModel(),
) {
    val themeMode by overviewViewModel.themeMode.collectAsStateWithLifecycle()
    val txDisplayPrefs by overviewViewModel.txDisplayPrefs.collectAsStateWithLifecycle()
    val language by overviewViewModel.language.collectAsStateWithLifecycle()
    val defaultTransactionType by overviewViewModel.defaultTransactionType.collectAsStateWithLifecycle()
    val paymentModeEnabled by overviewViewModel.paymentModeEnabled.collectAsStateWithLifecycle()
    val securityState by securityViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(securityViewModel) {
        securityViewModel.effects.collect { effect ->
            when (effect) {
                SecuritySettingsEffect.NavigateToPinSetup -> onNavigateToPinSetup()
            }
        }
    }

    val state = SettingsUiState(
        themeMode = themeMode,
        txDisplayPrefs = txDisplayPrefs,
        language = language,
        defaultTransactionType = defaultTransactionType,
        paymentModeEnabled = paymentModeEnabled,
    )

    SettingsContent(
        state = state,
        securityState = securityState,
        onThemeModeChanged = { overviewViewModel.setThemeMode(it) },
        onPaymentModeEnabledChanged = { overviewViewModel.setPaymentModeEnabled(it) },
        onSecurityIntent = securityViewModel::onIntent,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToTxDisplay = onNavigateToTxDisplay,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToExport = onNavigateToExport,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToPaymentModes = onNavigateToPaymentModes,
        onNavigateToBackupRestore = onNavigateToBackupRestore,
        onTabSelected = onTabSelected,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    securityState: SecuritySettingsUiState,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onPaymentModeEnabledChanged: (Boolean) -> Unit,
    onSecurityIntent: (SecuritySettingsIntent) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToPaymentModes: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val themeIndex = when (state.themeMode) {
        ThemeMode.Light -> 0
        ThemeMode.Dark -> 1
        ThemeMode.Auto -> 2
    }
    val themeModes = listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.Auto)
    val lockAfterLabel = when (securityState.backgroundLockSeconds) {
        0 -> stringResource(Res.string.settings_lock_immediately)
        30 -> stringResource(Res.string.settings_lock_30s)
        60 -> stringResource(Res.string.settings_lock_1m)
        300 -> stringResource(Res.string.settings_lock_5m)
        else -> "${securityState.backgroundLockSeconds}s"
    }
    val languageSubtitle = when (state.language) {
        "en" -> "English"
        "de" -> "Deutsch"
        "es" -> "Español"
        "it" -> "Italiano"
        "fr" -> "Français"
        "pt" -> "Português"
        else -> stringResource(Res.string.settings_lang_system_default)
    }

    var showLockPicker by remember { mutableStateOf(false) }
    if (showLockPicker) {
        LockTimeoutPickerDialog(
            currentSeconds = securityState.backgroundLockSeconds,
            onDismiss = { showLockPicker = false },
            onConfirm = { seconds ->
                onSecurityIntent(SecuritySettingsIntent.LockTimeoutChanged(seconds))
                showLockPicker = false
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        // Fixed title header — stays pinned at top during scroll
        Text(
            text = stringResource(Res.string.settings_title),
            style = type.title1,
            color = colors.text,
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_0_5x,
                    bottom = space.padding_2x
                ),
        )

        SettingsLazyList(
            modifier = Modifier.weight(1f),
            state = state,
            securityState = securityState,
            themeIndex = themeIndex,
            themeModes = themeModes,
            lockAfterLabel = lockAfterLabel,
            languageSubtitle = languageSubtitle,
            onThemeModeChanged = onThemeModeChanged,
            onPaymentModeEnabledChanged = onPaymentModeEnabledChanged,
            onSecurityIntent = onSecurityIntent,
            onNavigateToTxDisplay = onNavigateToTxDisplay,
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToLanguage = onNavigateToLanguage,
            onNavigateToExport = onNavigateToExport,
            onNavigateToWallets = onNavigateToWallets,
            onNavigateToPaymentModes = onNavigateToPaymentModes,
            onNavigateToBackupRestore = onNavigateToBackupRestore,
            onShowLockPicker = { showLockPicker = true },
        )
        MmTabBar(activeTab = TabRoute.Settings, onTabSelected = onTabSelected)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SettingsScreenPreview() {
    MoneyMTheme {
        SettingsContent(
            state = SettingsUiState(),
            securityState = SecuritySettingsUiState(),
            onThemeModeChanged = {},
            onPaymentModeEnabledChanged = {},
            onSecurityIntent = {},
            onNavigateToCategories = {},
            onNavigateToTxDisplay = {},
            onNavigateToLanguage = {},
            onNavigateToExport = {},
            onNavigateToWallets = {},
            onNavigateToPaymentModes = {},
            onNavigateToBackupRestore = {},
            onTabSelected = {},
        )
    }
}
