package com.dv.moneym.feature.settings.settings

import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.serialization.Serializable

@Serializable
data class SettingsUiState(
    // appearance
    val themeMode: ThemeMode = ThemeMode.Auto,
    val txDisplayPrefs: TxDisplayPrefs = TxDisplayPrefs(),
    // currency
    val defaultCurrency: String = "EUR",
    // language
    val language: String = "",
)

sealed interface SettingsIntent {
    // appearance
    data class ThemeModeChanged(val mode: ThemeMode) : SettingsIntent
}
