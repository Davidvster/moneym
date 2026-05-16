package com.dv.moneym.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.MmTabBar
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.settings.presentation.SettingsEffect
import com.dv.moneym.feature.settings.presentation.SettingsIntent
import com.dv.moneym.feature.settings.presentation.SettingsUiState
import com.dv.moneym.feature.settings.presentation.SettingsViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object SettingsKey : NavKey
@Serializable data object TxListDisplayKey : NavKey
@Serializable data object CurrencyPickerKey : NavKey
@Serializable data object LanguagePickerKey : NavKey

fun EntryProviderScope<NavKey>.settingsEntry(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onTabSelected: (TabRoute) -> Unit,
) = entry<SettingsKey> {
    SettingsScreen(
        onNavigateToPinSetup = onNavigateToPinSetup,
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToTxDisplay = onNavigateToTxDisplay,
        onNavigateToCurrency = onNavigateToCurrency,
        onNavigateToLanguage = onNavigateToLanguage,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun SettingsScreen(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToTxDisplay: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onTabSelected: (TabRoute) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToPinSetup -> onNavigateToPinSetup()
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
    onTabSelected: (TabRoute) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val isDark = state.themeMode == ThemeMode.Dark
    val themeIndex = when (state.themeMode) {
        ThemeMode.Light -> 0
        ThemeMode.Dark -> 1
        ThemeMode.Auto -> 2
    }
    val themeModes = listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.Auto)

    val prefs = state.txDisplayPrefs
    val txDisplaySummary = "${prefs.indicatorStyle.name} · ${if (prefs.showNote) "with note" else "no note"}"

    val lockAfterLabel = when (state.backgroundLockSeconds) {
        0 -> "Immediately"
        30 -> "30 seconds"
        60 -> "1 minute"
        300 -> "5 minutes"
        else -> "${state.backgroundLockSeconds}s"
    }

    val currencySubtitle = state.defaultCurrency

    val languageSubtitle = when (state.language) {
        "en" -> "English"
        "de" -> "Deutsch"
        "es" -> "Español"
        "it" -> "Italiano"
        "fr" -> "Français"
        "pt" -> "Português"
        else -> "System default"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Settings",
            style = type.title1,
            color = colors.text,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
        )

        // APPEARANCE
        SectionLabel("APPEARANCE", Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        MmCard(Modifier.padding(horizontal = 16.dp)) {
            // Theme row
            MmRow {
                Icon(
                    imageVector = if (isDark) MmIcons.moon else MmIcons.sun,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Text("Theme", style = type.body, color = colors.text, modifier = Modifier.weight(1f))
                MmSegmented(
                    options = listOf("Light", "Dark", "Auto"),
                    selectedIndex = themeIndex,
                    onOptionSelected = { onIntent(SettingsIntent.ThemeModeChanged(themeModes[it])) },
                    size = MmSegmentedSize.Sm,
                )
            }
            // Transaction list row
            MmRow(onClick = onNavigateToTxDisplay, divider = false) {
                Icon(
                    imageVector = MmIcons.sliders,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("Transaction list", style = type.body, color = colors.text)
                    Text(txDisplaySummary, style = type.caption.copy(color = colors.text2))
                }
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // SECURITY
        SectionLabel(
            "SECURITY",
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        )
        MmCard(Modifier.padding(horizontal = 16.dp)) {
            // Enable PIN lock
            MmRow {
                Icon(
                    imageVector = MmIcons.lock,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Enable PIN lock",
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                MmToggle(
                    checked = state.pinEnabled,
                    onCheckedChange = { onIntent(SettingsIntent.PinToggled(it)) },
                )
            }
            // Biometrics
            MmRow(
                modifier = Modifier.alpha(if (state.pinEnabled) 1f else 0.45f),
            ) {
                Icon(
                    imageVector = MmIcons.fingerprint,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("Unlock with biometrics", style = type.body, color = colors.text)
                    Text("Face ID / Fingerprint", style = type.caption.copy(color = colors.text2))
                }
                MmToggle(
                    checked = state.biometricEnabled,
                    onCheckedChange = { onIntent(SettingsIntent.BiometricToggled(it)) },
                    enabled = state.pinEnabled,
                )
            }
            // Change PIN
            MmRow(onClick = { onIntent(SettingsIntent.ChangePinRequested) }) {
                Text(
                    "Change PIN",
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
            // Lock after
            MmRow(onClick = {}, divider = false) {
                Text(
                    "Lock after",
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(lockAfterLabel, style = type.caption.copy(color = colors.text2))
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // PREFERENCES
        SectionLabel(
            "PREFERENCES",
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        )
        MmCard(Modifier.padding(horizontal = 16.dp)) {
            MmRow(onClick = onNavigateToCurrency) {
                Icon(
                    imageVector = MmIcons.info,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("Default currency", style = type.body, color = colors.text)
                    Text(currencySubtitle, style = type.caption.copy(color = colors.text2))
                }
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
            MmRow(onClick = onNavigateToLanguage) {
                Icon(
                    imageVector = MmIcons.globe,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("Language", style = type.body, color = colors.text)
                    Text(languageSubtitle, style = type.caption.copy(color = colors.text2))
                }
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
            MmRow(onClick = onNavigateToCategories, divider = false) {
                Icon(
                    imageVector = MmIcons.list,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Manage categories",
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // DATA
        SectionLabel("DATA", Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp))
        MmCard(Modifier.padding(horizontal = 16.dp)) {
            MmRow(onClick = { onIntent(SettingsIntent.ExportJsonRequested) }) {
                Icon(
                    imageVector = MmIcons.download,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Export as JSON",
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
            MmRow(onClick = { onIntent(SettingsIntent.ExportCsvRequested) }) {
                Icon(
                    imageVector = MmIcons.download,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Export as CSV",
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
            MmRow(onClick = {}, divider = false) {
                Icon(
                    imageVector = MmIcons.folder,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Import data",
                    style = type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Text(
            text = "MoneyM v2.0 · build 2026.05.15",
            style = type.captionMono.copy(color = colors.text3),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )

        MmTabBar(activeTab = TabRoute.Settings, onTabSelected = onTabSelected)
    }
}
