package com.dv.moneym.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.dv.moneym.core.ui.wallet
import com.dv.moneym.feature.settings.presentation.SettingsEffect
import com.dv.moneym.feature.settings.presentation.SettingsIntent
import com.dv.moneym.feature.settings.presentation.SettingsUiState
import com.dv.moneym.feature.settings.presentation.SettingsViewModel
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_biometrics
import moneym.feature.settings.generated.resources.settings_biometrics_subtitle
import moneym.feature.settings.generated.resources.settings_cancel
import moneym.feature.settings.generated.resources.settings_categories
import moneym.feature.settings.generated.resources.settings_change_pin
import moneym.feature.settings.generated.resources.settings_currency
import moneym.feature.settings.generated.resources.settings_export_data
import moneym.feature.settings.generated.resources.settings_import_data
import moneym.feature.settings.generated.resources.settings_lang_system_default
import moneym.feature.settings.generated.resources.settings_language
import moneym.feature.settings.generated.resources.settings_lock_1m
import moneym.feature.settings.generated.resources.settings_lock_30s
import moneym.feature.settings.generated.resources.settings_lock_5m
import moneym.feature.settings.generated.resources.settings_lock_after
import moneym.feature.settings.generated.resources.settings_lock_after_title
import moneym.feature.settings.generated.resources.settings_lock_immediately
import moneym.feature.settings.generated.resources.settings_ok
import moneym.feature.settings.generated.resources.settings_pin_lock
import moneym.feature.settings.generated.resources.settings_section_appearance
import moneym.feature.settings.generated.resources.settings_section_data
import moneym.feature.settings.generated.resources.settings_section_preferences
import moneym.feature.settings.generated.resources.settings_section_security
import moneym.feature.settings.generated.resources.settings_theme
import moneym.feature.settings.generated.resources.settings_theme_auto
import moneym.feature.settings.generated.resources.settings_theme_dark
import moneym.feature.settings.generated.resources.settings_theme_light
import moneym.feature.settings.generated.resources.settings_title
import moneym.feature.settings.generated.resources.settings_tx_list
import moneym.feature.settings.generated.resources.settings_wallets
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
                .padding(start = 20.dp, end = 20.dp, top = space.padding_0_5x, bottom = space.padding_2x),
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

// ─── SettingsLazyList ─────────────────────────────────────────────────────────

@Composable
private fun SettingsLazyList(
    modifier: Modifier,
    state: SettingsUiState,
    themeIndex: Int,
    themeModes: List<ThemeMode>,
    txDisplaySummary: String,
    lockAfterLabel: String,
    languageSubtitle: String,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToTxDisplay: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onShowLockPicker: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    LazyColumn(modifier = modifier) {
        item(key = SettingsItem.APPEARANCE_LABEL.name) {
            SectionLabel(stringResource(Res.string.settings_section_appearance), Modifier.padding(horizontal = 20.dp, vertical = space.padding_0_5x))
        }
        item(key = SettingsItem.APPEARANCE_CARD.name) {
            AppearanceSection(
                themeMode = state.themeMode,
                themeIndex = themeIndex,
                themeModes = themeModes,
                txDisplaySummary = txDisplaySummary,
                onIntent = onIntent,
                onNavigateToTxDisplay = onNavigateToTxDisplay,
            )
        }
        item(key = SettingsItem.SECURITY_LABEL.name) {
            SectionLabel(
                stringResource(Res.string.settings_section_security),
                Modifier.padding(start = 20.dp, end = 20.dp, top = space.padding_2x, bottom = space.padding_0_5x),
            )
        }
        item(key = SettingsItem.SECURITY_CARD.name) {
            SecuritySection(
                pinEnabled = state.pinEnabled,
                biometricAvailable = state.biometricAvailable,
                biometricEnabled = state.biometricEnabled,
                lockAfterLabel = lockAfterLabel,
                onIntent = onIntent,
                onShowLockPicker = onShowLockPicker,
            )
        }
        item(key = SettingsItem.PREFERENCES_LABEL.name) {
            SectionLabel(
                stringResource(Res.string.settings_section_preferences),
                Modifier.padding(start = 20.dp, end = 20.dp, top = space.padding_2x, bottom = space.padding_0_5x),
            )
        }
        item(key = SettingsItem.PREFERENCES_CARD.name) {
            PreferencesSection(
                currencySubtitle = state.defaultCurrency,
                languageSubtitle = languageSubtitle,
                onNavigateToCurrency = onNavigateToCurrency,
                onNavigateToLanguage = onNavigateToLanguage,
                onNavigateToCategories = onNavigateToCategories,
                onNavigateToWallets = onNavigateToWallets,
            )
        }
        item(key = SettingsItem.DATA_LABEL.name) {
            SectionLabel(stringResource(Res.string.settings_section_data), Modifier.padding(start = 20.dp, end = 20.dp, top = space.padding_2x, bottom = space.padding_0_5x))
        }
        item(key = SettingsItem.DATA_CARD.name) {
            DataSection(
                onIntent = onIntent,
                onNavigateToExport = onNavigateToExport,
            )
        }
        item(key = SettingsItem.VERSION.name) {
            Text(
                text = "MoneyM v2.0 · build 2026.05.15",
                style = type.captionMono.copy(color = colors.text3),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(space.padding_3x),
            )
        }
    }
}

// ─── AppearanceSection ────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    themeIndex: Int,
    themeModes: List<ThemeMode>,
    txDisplaySummary: String,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToTxDisplay: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val isDark = themeMode == ThemeMode.Dark
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow {
            Icon(
                imageVector = if (isDark) MmIcons.moon else MmIcons.sun,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Text(stringResource(Res.string.settings_theme), style = type.body, color = colors.text, modifier = Modifier.weight(1f))
            MmSegmented(
                options = listOf(
                    stringResource(Res.string.settings_theme_light),
                    stringResource(Res.string.settings_theme_dark),
                    stringResource(Res.string.settings_theme_auto),
                ),
                selectedIndex = themeIndex,
                onOptionSelected = { onIntent(SettingsIntent.ThemeModeChanged(themeModes[it])) },
                size = MmSegmentedSize.Sm,
            )
        }
        MmRow(onClick = onNavigateToTxDisplay, divider = false) {
            Icon(
                imageVector = MmIcons.sliders,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.settings_tx_list), style = type.body, color = colors.text)
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
}

// ─── SecuritySection ──────────────────────────────────────────────────────────

@Composable
private fun SecuritySection(
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
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
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ─── PreferencesSection ───────────────────────────────────────────────────────

@Composable
private fun PreferencesSection(
    currencySubtitle: String,
    languageSubtitle: String,
    onNavigateToCurrency: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToWallets: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow(onClick = onNavigateToCurrency) {
            Icon(
                imageVector = MmIcons.info,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.settings_currency), style = type.body, color = colors.text)
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
                Text(stringResource(Res.string.settings_language), style = type.body, color = colors.text)
                Text(languageSubtitle, style = type.caption.copy(color = colors.text2))
            }
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(16.dp),
            )
        }
        MmRow(onClick = onNavigateToCategories) {
            Icon(
                imageVector = MmIcons.list,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(Res.string.settings_categories),
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
        MmRow(onClick = onNavigateToWallets, divider = false) {
            Icon(
                imageVector = MmIcons.wallet,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(Res.string.settings_wallets),
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
}

// ─── DataSection ──────────────────────────────────────────────────────────────

@Composable
private fun DataSection(
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToExport: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow(onClick = onNavigateToExport) {
            Icon(
                imageVector = MmIcons.download,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(Res.string.settings_export_data),
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
        MmRow(onClick = { onIntent(SettingsIntent.ImportRequested) }, divider = false) {
            Icon(
                imageVector = MmIcons.folder,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(Res.string.settings_import_data),
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
}

// ─── Lock Timeout Picker Dialog ───────────────────────────────────────────────

@Composable
private fun LockTimeoutPickerDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space

    val options = listOf(
        0 to stringResource(Res.string.settings_lock_immediately),
        30 to stringResource(Res.string.settings_lock_30s),
        60 to stringResource(Res.string.settings_lock_1m),
        300 to stringResource(Res.string.settings_lock_5m),
    )
    var selectedSeconds by remember { mutableStateOf(currentSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.settings_lock_after_title),
                style = type.title3,
                color = colors.text,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_5x)) {
                options.forEach { (seconds, label) ->
                    val isSelected = seconds == selectedSeconds
                    MmRow(
                        onClick = { selectedSeconds = seconds },
                        divider = false,
                        padding = PaddingValues(horizontal = space.padding_0_5x, vertical = space.padding_0_25x),
                    ) {
                        Text(
                            text = label,
                            style = type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (isSelected) colors.accent else colors.borderStrong,
                                    CircleShape,
                                )
                                .background(if (isSelected) colors.accent else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = MmIcons.check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSeconds) }) {
                Text(stringResource(Res.string.settings_ok), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_cancel), color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
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
