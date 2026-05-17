package com.dv.moneym.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.settings.presentation.SettingsEffect
import com.dv.moneym.feature.settings.presentation.SettingsIntent
import com.dv.moneym.feature.settings.presentation.SettingsUiState
import com.dv.moneym.feature.settings.presentation.SettingsViewModel
import com.dv.moneym.feature.settings.ui.components.AppearanceSection
import com.dv.moneym.feature.settings.ui.components.DataSection
import com.dv.moneym.feature.settings.ui.components.LockTimeoutPickerDialog
import com.dv.moneym.feature.settings.ui.components.PreferencesSection
import com.dv.moneym.feature.settings.ui.components.SecuritySection
import com.dv.moneym.feature.settings.ui.components.SettingsLazyList
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

@Serializable data object SettingsKey : NavKey
@Serializable data object TxListDisplayKey : NavKey
@Serializable data object CurrencyPickerKey : NavKey
@Serializable data object LanguagePickerKey : NavKey

enum class SettingsItem {
    APPEARANCE_LABEL,
    APPEARANCE_CARD,
    SECURITY_LABEL,
    SECURITY_CARD,
    PREFERENCES_LABEL,
    PREFERENCES_CARD,
    DATA_LABEL,
    DATA_CARD,
    VERSION,
}

fun EntryProviderScope<NavKey>.settingsEntry(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit = {},
    onNavigateToWallets: () -> Unit = {},
    onTabSelected: (TabRoute) -> Unit,
    onExportReady: (suspend (String, String, String) -> Unit)? = null,
    onImportRequested: (suspend () -> String?)? = null,
    viewModel: SettingsViewModel? = null,
) = entry<SettingsKey> {
    SettingsScreen(
        onNavigateToPinSetup = onNavigateToPinSetup,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToTxDisplay = onNavigateToTxDisplay,
        onNavigateToCurrency = onNavigateToCurrency,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToExport = onNavigateToExport,
        onNavigateToWallets = onNavigateToWallets,
        onTabSelected = onTabSelected,
        onExportReady = onExportReady,
        onImportRequested = onImportRequested,
        viewModel = viewModel ?: koinViewModel(),
    )
}

@Composable
fun SettingsScreen(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToTxDisplay: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToWallets: () -> Unit = {},
    onTabSelected: (TabRoute) -> Unit = {},
    onExportReady: (suspend (String, String, String) -> Unit)? = null,
    onImportRequested: (suspend () -> String?)? = null,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToPinSetup -> onNavigateToPinSetup()
                is SettingsEffect.ExportReady -> {
                    onExportReady?.invoke(effect.fileName, effect.content, effect.mimeType)
                }
                SettingsEffect.ImportRequested -> {
                    val content = onImportRequested?.invoke()
                    if (content != null) {
                        viewModel.onIntent(SettingsIntent.ImportJsonChanged(content))
                        viewModel.onIntent(SettingsIntent.ApplyImportRequested)
                    }
                }
            }
        }
    }
    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToTxDisplay = onNavigateToTxDisplay,
        onNavigateToCurrency = onNavigateToCurrency,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToExport = onNavigateToExport,
        onNavigateToWallets = onNavigateToWallets,
        onTabSelected = onTabSelected,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val themeIndex = when (state.themeMode) {
        ThemeMode.Light -> 0
        ThemeMode.Dark -> 1
        ThemeMode.Auto -> 2
    }
    val themeModes = listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.Auto)
    val prefs = state.txDisplayPrefs
    val txDisplaySummary = "${prefs.indicatorStyle.name} · ${if (prefs.showNote) "with note" else "no note"}"
    val lockAfterLabel = when (state.backgroundLockSeconds) {
        0 -> stringResource(Res.string.settings_lock_immediately)
        30 -> stringResource(Res.string.settings_lock_30s)
        60 -> stringResource(Res.string.settings_lock_1m)
        300 -> stringResource(Res.string.settings_lock_5m)
        else -> "${state.backgroundLockSeconds}s"
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
            currentSeconds = state.backgroundLockSeconds,
            onDismiss = { showLockPicker = false },
            onConfirm = { seconds ->
                onIntent(SettingsIntent.LockTimeoutChanged(seconds))
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
                .padding(start = MM.space.padding_2_5x, end = MM.space.padding_2_5x, top = space.padding_0_5x, bottom = space.padding_2x),
        )

        SettingsLazyList(
            modifier = Modifier.weight(1f),
            state = state,
            themeIndex = themeIndex,
            themeModes = themeModes,
            txDisplaySummary = txDisplaySummary,
            lockAfterLabel = lockAfterLabel,
            languageSubtitle = languageSubtitle,
            onIntent = onIntent,
            onNavigateToTxDisplay = onNavigateToTxDisplay,
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToCurrency = onNavigateToCurrency,
            onNavigateToLanguage = onNavigateToLanguage,
            onNavigateToExport = onNavigateToExport,
            onNavigateToWallets = onNavigateToWallets,
            onShowLockPicker = { showLockPicker = true },
        )
        MmTabBar(activeTab = TabRoute.Settings, onTabSelected = onTabSelected)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SettingsScreenPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        SettingsContent(
            state = SettingsUiState(),
            onIntent = {},
            onNavigateToCategories = {},
            onNavigateToTxDisplay = {},
            onNavigateToCurrency = {},
            onNavigateToLanguage = {},
            onNavigateToExport = {},
            onNavigateToWallets = {},
            onTabSelected = {},
        )
    }
}
