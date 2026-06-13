package com.dv.moneym.feature.walletsync.home

import com.dv.moneym.platform.InstalledApp

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
}

sealed interface WalletSyncHomeIntent {
    data object Refresh : WalletSyncHomeIntent
    data object ToggleEnabled : WalletSyncHomeIntent
    data object OpenAccessSettings : WalletSyncHomeIntent
    data class ShowAppPicker(val show: Boolean) : WalletSyncHomeIntent
    data class ToggleApp(val packageName: String) : WalletSyncHomeIntent
    data class SetAppQuery(val text: String) : WalletSyncHomeIntent
}
