package com.dv.moneym.feature.walletsync.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.walletsync.WalletSyncRepository
import com.dv.moneym.platform.InstalledAppsProvider
import com.dv.moneym.platform.NotificationAccessController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalletSyncHomeViewModel(
    private val notificationAccess: NotificationAccessController,
    private val installedAppsProvider: InstalledAppsProvider,
    private val walletSyncRepository: WalletSyncRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(WalletSyncHomeUiState())
    internal val state = _state.onStart { init() }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private fun init() {
        val selected = loadSelectedPackages()
        _state.update {
            it.copy(
                isLoading = false,
                enabled = appSettings.getBoolean(PrefKeys.WALLET_SYNC_ENABLED, defaultValue = false),
                selectedPackages = selected,
                accessGranted = notificationAccess.isAccessGranted(),
            )
        }
        if (selected.isNotEmpty()) loadApps()
        viewModelScope.launch {
            walletSyncRepository.observePendingCount().collect { count ->
                _state.update { it.copy(pendingCount = count) }
            }
        }
    }

    fun onIntent(intent: WalletSyncHomeIntent) {
        when (intent) {
            WalletSyncHomeIntent.Refresh ->
                _state.update { it.copy(accessGranted = notificationAccess.isAccessGranted()) }

            WalletSyncHomeIntent.ToggleEnabled -> {
                val next = !_state.value.enabled
                appSettings.putBoolean(PrefKeys.WALLET_SYNC_ENABLED, next)
                _state.update { it.copy(enabled = next) }
            }

            WalletSyncHomeIntent.OpenAccessSettings -> notificationAccess.openAccessSettings()

            is WalletSyncHomeIntent.ShowAppPicker -> {
                _state.update { it.copy(showAppPicker = intent.show, appQuery = "") }
                if (intent.show && _state.value.installedApps.isEmpty()) loadApps()
            }

            is WalletSyncHomeIntent.ToggleApp -> {
                val next = _state.value.selectedPackages.toMutableSet()
                if (!next.remove(intent.packageName)) next.add(intent.packageName)
                persistSelection(next)
            }

            is WalletSyncHomeIntent.SetAppQuery ->
                _state.update { it.copy(appQuery = intent.text) }

            is WalletSyncHomeIntent.RemoveAppRequested ->
                _state.update { it.copy(pendingRemovePackage = intent.packageName) }

            WalletSyncHomeIntent.ConfirmRemoveApp -> {
                val pkg = _state.value.pendingRemovePackage
                if (pkg != null) {
                    val next = _state.value.selectedPackages - pkg
                    persistSelection(next)
                }
                _state.update { it.copy(pendingRemovePackage = null) }
            }

            WalletSyncHomeIntent.DismissRemoveDialog ->
                _state.update { it.copy(pendingRemovePackage = null) }
        }
    }

    private fun persistSelection(packages: Set<String>) {
        appSettings.putString(PrefKeys.WALLET_SYNC_PACKAGES, packages.joinToString(","))
        _state.update { it.copy(selectedPackages = packages) }
    }

    private fun loadApps() {
        _state.update { it.copy(appsLoading = true) }
        viewModelScope.launch {
            val apps = installedAppsProvider.installedApps()
            _state.update { it.copy(appsLoading = false, installedApps = apps) }
        }
    }

    private fun loadSelectedPackages(): Set<String> =
        appSettings.getString(PrefKeys.WALLET_SYNC_PACKAGES, defaultValue = null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
}
