package com.dv.moneym.feature.settings.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.common.SupportedLanguage
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.platform.AppInfo
import com.dv.moneym.feature.settings.overview.components.LockTimeoutPickerDialog
import com.dv.moneym.feature.settings.overview.components.SettingsLazyList
import com.dv.moneym.feature.settings.overview.components.ThemePickerSheet
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_lang_system_default
import moneym.feature.settings.generated.resources.settings_lock_1m
import moneym.feature.settings.generated.resources.settings_lock_30s
import moneym.feature.settings.generated.resources.settings_lock_5m
import moneym.feature.settings.generated.resources.settings_lock_immediately
import moneym.feature.settings.generated.resources.settings_app_info_footer
import moneym.feature.settings.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SettingsKey : NavKey

@Serializable
data object TxListDisplayKey : NavKey

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
    ABOUT_LABEL,
    ABOUT_CARD,
    VERSION,
}

fun EntryProviderScope<NavKey>.settingsEntry(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit = {},
    onNavigateToBankSync: () -> Unit = {},
    onNavigateToWalletSync: (() -> Unit)? = null,
    onNavigateToWallets: () -> Unit = {},
    onNavigateToPaymentModes: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onTabSelected: (TabRoute) -> Unit,
    securityViewModel: SecuritySettingsViewModel? = null,
    metadata: Map<String, Any> = emptyMap(),
) = entry<SettingsKey>(metadata = metadata) {
    SettingsScreen(
        onNavigateToPinSetup = onNavigateToPinSetup,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToBudgets = onNavigateToBudgets,
        onNavigateToRecurring = onNavigateToRecurring,
        onNavigateToTxDisplay = onNavigateToTxDisplay,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToExport = onNavigateToExport,
        onNavigateToBankSync = onNavigateToBankSync,
        onNavigateToWalletSync = onNavigateToWalletSync,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToPaymentModes = onNavigateToPaymentModes,
        onNavigateToBackupRestore = onNavigateToBackupRestore,
        onNavigateToAbout = onNavigateToAbout,
        onTabSelected = onTabSelected,
        securityViewModel = securityViewModel ?: koinViewModel(),
    )
}

@Composable
fun SettingsScreen(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToTxDisplay: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToBankSync: () -> Unit = {},
    onNavigateToWalletSync: (() -> Unit)? = null,
    onNavigateToWallets: () -> Unit = {},
    onNavigateToPaymentModes: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onTabSelected: (TabRoute) -> Unit = {},
    overviewViewModel: SettingsOverviewViewModel = koinViewModel(),
    securityViewModel: SecuritySettingsViewModel = koinViewModel(),
) {
    val themeMode by overviewViewModel.themeMode.collectAsStateWithLifecycle()
    val txDisplayPrefs by overviewViewModel.txDisplayPrefs.collectAsStateWithLifecycle()
    val language by overviewViewModel.language.collectAsStateWithLifecycle()
    val defaultTransactionType by overviewViewModel.defaultTransactionType.collectAsStateWithLifecycle()
    val paymentModeEnabled by overviewViewModel.paymentModeEnabled.collectAsStateWithLifecycle()
    val useCurrencySymbol by overviewViewModel.useCurrencySymbol.collectAsStateWithLifecycle()
    val walletCurrency by overviewViewModel.walletCurrency.collectAsStateWithLifecycle()
    val showLockPicker by overviewViewModel.showLockPicker.collectAsStateWithLifecycle()
    val showThemeSheet by overviewViewModel.showThemeSheet.collectAsStateWithLifecycle()
    val securityState by securityViewModel.state.collectAsStateWithLifecycle()
    val appInfo = koinInject<AppInfo>()
    val appInfoLabel = stringResource(
        Res.string.settings_app_info_footer,
        appInfo.appName,
        appInfo.versionName,
    )

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
        useCurrencySymbol = useCurrencySymbol,
        walletCurrency = walletCurrency,
        showLockPicker = showLockPicker,
        showThemeSheet = showThemeSheet,
    )

    SettingsContent(
        state = state,
        securityState = securityState,
        onThemeModeChanged = { overviewViewModel.onIntent(SettingsOverviewIntent.SetThemeMode(it)) },
        onPaymentModeEnabledChanged = {
            overviewViewModel.onIntent(
                SettingsOverviewIntent.SetPaymentModeEnabled(
                    it
                )
            )
        },
        onUseCurrencySymbolChanged = {
            overviewViewModel.onIntent(
                SettingsOverviewIntent.SetUseCurrencySymbol(
                    it
                )
            )
        },
        onShowLockPicker = { overviewViewModel.onIntent(SettingsOverviewIntent.ShowLockPicker(it)) },
        onShowThemeSheet = { overviewViewModel.onIntent(SettingsOverviewIntent.ShowThemeSheet(it)) },
        onSecurityIntent = securityViewModel::onIntent,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToBudgets = onNavigateToBudgets,
        onNavigateToRecurring = onNavigateToRecurring,
        onNavigateToTxDisplay = onNavigateToTxDisplay,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToExport = onNavigateToExport,
        onNavigateToBankSync = onNavigateToBankSync,
        onNavigateToWalletSync = onNavigateToWalletSync,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToPaymentModes = onNavigateToPaymentModes,
        onNavigateToBackupRestore = onNavigateToBackupRestore,
        onNavigateToAbout = onNavigateToAbout,
        onTabSelected = onTabSelected,
        appInfoLabel = appInfoLabel,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    securityState: SecuritySettingsUiState,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onPaymentModeEnabledChanged: (Boolean) -> Unit,
    onUseCurrencySymbolChanged: (Boolean) -> Unit,
    onShowLockPicker: (Boolean) -> Unit,
    onShowThemeSheet: (Boolean) -> Unit,
    onSecurityIntent: (SecuritySettingsIntent) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToBankSync: () -> Unit,
    onNavigateToWalletSync: (() -> Unit)?,
    onNavigateToWallets: () -> Unit,
    onNavigateToPaymentModes: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onTabSelected: (TabRoute) -> Unit,
    appInfoLabel: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val lockAfterLabel = when (securityState.backgroundLockSeconds) {
        0 -> stringResource(Res.string.settings_lock_immediately)
        30 -> stringResource(Res.string.settings_lock_30s)
        60 -> stringResource(Res.string.settings_lock_1m)
        300 -> stringResource(Res.string.settings_lock_5m)
        else -> "${securityState.backgroundLockSeconds}s"
    }
    val languageSubtitle = SupportedLanguage.fromCode(state.language)?.nativeName
        ?: stringResource(Res.string.settings_lang_system_default)

    if (state.showLockPicker) {
        LockTimeoutPickerDialog(
            currentSeconds = securityState.backgroundLockSeconds,
            onDismiss = { onShowLockPicker(false) },
            onConfirm = { seconds ->
                onSecurityIntent(SecuritySettingsIntent.LockTimeoutChanged(seconds))
                onShowLockPicker(false)
            },
        )
    }

    if (state.showThemeSheet) {
        ThemePickerSheet(
            current = state.themeMode,
            onSelect = { onThemeModeChanged(it); onShowThemeSheet(false) },
            onDismiss = { onShowThemeSheet(false) },
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
            lockAfterLabel = lockAfterLabel,
            languageSubtitle = languageSubtitle,
            onOpenThemeSheet = { onShowThemeSheet(true) },
            onPaymentModeEnabledChanged = onPaymentModeEnabledChanged,
            onUseCurrencySymbolChanged = onUseCurrencySymbolChanged,
            onSecurityIntent = onSecurityIntent,
            onNavigateToTxDisplay = onNavigateToTxDisplay,
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToBudgets = onNavigateToBudgets,
            onNavigateToRecurring = onNavigateToRecurring,
            onNavigateToLanguage = onNavigateToLanguage,
            onNavigateToExport = onNavigateToExport,
            onNavigateToBankSync = onNavigateToBankSync,
            onNavigateToWalletSync = onNavigateToWalletSync,
            onNavigateToWallets = onNavigateToWallets,
            onNavigateToPaymentModes = onNavigateToPaymentModes,
            onNavigateToBackupRestore = onNavigateToBackupRestore,
            onNavigateToAbout = onNavigateToAbout,
            onShowLockPicker = { onShowLockPicker(true) },
            appInfoLabel = appInfoLabel,
        )
        MmTabBar(activeTab = TabRoute.Settings, onTabSelected = onTabSelected)
    }
}

// Store screenshot — full settings home, light theme, EUR. Rendered directly by
// StoreScreenshotTest (no @Preview so the package scanner skips it).
@Composable
internal fun StoreSettingsPreview() {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        MoneyMTheme(darkTheme = false) {
            SettingsContent(
                state = SettingsUiState(
                    themeMode = ThemeMode.Light,
                    paymentModeEnabled = true,
                    useCurrencySymbol = true,
                    walletCurrency = "EUR",
                ),
                securityState = SecuritySettingsUiState(
                    pinEnabled = true,
                    biometricAvailable = true,
                    biometricEnabled = true,
                ),
                onThemeModeChanged = {},
                onPaymentModeEnabledChanged = {},
                onUseCurrencySymbolChanged = {},
                onShowLockPicker = {},
                onShowThemeSheet = {},
                onSecurityIntent = {},
                onNavigateToCategories = {},
                onNavigateToBudgets = {},
                onNavigateToRecurring = {},
                onNavigateToTxDisplay = {},
                onNavigateToLanguage = {},
                onNavigateToExport = {},
                onNavigateToBankSync = {},
                onNavigateToWalletSync = {},
                onNavigateToWallets = {},
                onNavigateToPaymentModes = {},
                onNavigateToBackupRestore = {},
                onNavigateToAbout = {},
                onTabSelected = {},
                appInfoLabel = stringResource(
                    Res.string.settings_app_info_footer,
                    "MoneyM",
                    "1.0.0",
                ),
            )
        }
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
            onUseCurrencySymbolChanged = {},
            onShowLockPicker = {},
            onShowThemeSheet = {},
            onSecurityIntent = {},
            onNavigateToCategories = {},
            onNavigateToBudgets = {},
            onNavigateToRecurring = {},
            onNavigateToTxDisplay = {},
            onNavigateToLanguage = {},
            onNavigateToExport = {},
            onNavigateToBankSync = {},
            onNavigateToWalletSync = {},
            onNavigateToWallets = {},
            onNavigateToPaymentModes = {},
            onNavigateToBackupRestore = {},
            onNavigateToAbout = {},
            onTabSelected = {},
            appInfoLabel = stringResource(
                Res.string.settings_app_info_footer,
                "MoneyM",
                "1.0.0",
            ),
        )
    }
}
