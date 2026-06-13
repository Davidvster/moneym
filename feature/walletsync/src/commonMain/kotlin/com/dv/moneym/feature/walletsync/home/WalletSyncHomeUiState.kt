package com.dv.moneym.feature.walletsync.home

import com.dv.moneym.platform.InstalledApp

val WALLET_SYNC_SUGGESTED_PACKAGES: Set<String> = setOf(
    "com.google.android.apps.walletnfcrel",   // Google Wallet
    "com.google.android.apps.nbu.paisa.user", // Google Pay
    "com.paypal.android.p2pmobile",           // PayPal
    "com.revolut.revolut",                    // Revolut
    "com.transferwise.android",               // Wise
    "de.number26.android",                    // N26
    "com.squareup.cash",                      // Cash App
    "com.venmo",                              // Venmo
)

data class WalletSyncHomeUiState(
    val isLoading: Boolean = true,
    val accessGranted: Boolean = false,
    val enabled: Boolean = false,
    val pendingCount: Int = 0,
    val selectedPackages: Set<String> = emptySet(),
    val showAppPicker: Boolean = false,
    val appsLoading: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val appQuery: String = "",
) {
    val filteredApps: List<InstalledApp>
        get() = if (appQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(appQuery, ignoreCase = true) }

    val suggestedApps: List<InstalledApp>
        get() = filteredApps.filter { it.packageName in WALLET_SYNC_SUGGESTED_PACKAGES }

    val otherApps: List<InstalledApp>
        get() = filteredApps.filterNot { it.packageName in WALLET_SYNC_SUGGESTED_PACKAGES }
}

sealed interface WalletSyncHomeIntent {
    data object Refresh : WalletSyncHomeIntent
    data object ToggleEnabled : WalletSyncHomeIntent
    data object OpenAccessSettings : WalletSyncHomeIntent
    data class ShowAppPicker(val show: Boolean) : WalletSyncHomeIntent
    data class ToggleApp(val packageName: String) : WalletSyncHomeIntent
    data class SetAppQuery(val text: String) : WalletSyncHomeIntent
}
