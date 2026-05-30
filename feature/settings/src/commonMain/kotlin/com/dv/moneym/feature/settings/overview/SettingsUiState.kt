package com.dv.moneym.feature.settings.overview

import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.serialization.Serializable

@Serializable
data class SettingsUiState(
    // appearance
    val themeMode: ThemeMode = ThemeMode.Auto,
    val txDisplayPrefs: TxDisplayPrefs = TxDisplayPrefs(),
    // language
    val language: String = "",
    // default transaction type for new transactions
    val defaultTransactionType: TransactionType = TransactionType.EXPENSE,
    // payment mode toggle
    val paymentModeEnabled: Boolean = false,
    // show currency symbol instead of code
    val useCurrencySymbol: Boolean = false,
    val showLockPicker: Boolean = false,
)

sealed interface SettingsOverviewIntent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsOverviewIntent
    data class SetDefaultTransactionType(val type: TransactionType) : SettingsOverviewIntent
    data class SetPaymentModeEnabled(val enabled: Boolean) : SettingsOverviewIntent
    data class SetUseCurrencySymbol(val enabled: Boolean) : SettingsOverviewIntent
    data class ShowLockPicker(val visible: Boolean) : SettingsOverviewIntent
}
