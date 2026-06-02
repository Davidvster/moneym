package com.dv.moneym.feature.sync

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.sync.DeviceEntry
import com.dv.moneym.data.sync.DeviceRegistryController
import com.dv.moneym.data.sync.SyncPuller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncSettingsViewModel(
    private val registry: DeviceRegistryController,
    private val appSettings: AppSettings,
    private val syncPuller: SyncPuller,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(SyncSettingsUiState()) }
    internal val state = _state.onStart { init() }.stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private suspend fun init() {
        refreshNow()
    }

    fun onIntent(intent: SyncSettingsIntent) {
        when (intent) {
            SyncSettingsIntent.ToggleSync -> toggleSync()
            SyncSettingsIntent.StartRename -> startRename()
            is SyncSettingsIntent.RenameDraftChanged -> _state.update { it.copy(renameDraft = intent.value) }
            SyncSettingsIntent.SubmitRename -> submitRename()
            SyncSettingsIntent.CancelRename -> _state.update { it.copy(isRenaming = false) }
            is SyncSettingsIntent.RemoveDevice -> removeDevice(intent.id)
            SyncSettingsIntent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        _state.update { it.copy(isLoading = true) }
        val devices = registry.load()
        val enabled = appSettings.getBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, defaultValue = false)
        val thisDeviceName = appSettings.getString(PrefKeys.DEVICE_NAME).orEmpty()
        _state.update {
            it.copy(
                crossDeviceSyncEnabled = enabled,
                thisDeviceName = thisDeviceName,
                devices = toRows(devices),
                isLoading = false,
            )
        }
    }

    private fun toRows(devices: List<DeviceEntry>): List<DeviceRow> {
        val thisId = registry.thisDeviceId
        return devices
            .map { e ->
                DeviceRow(
                    id = e.id,
                    displayName = e.displayName,
                    platform = e.platform,
                    lastSyncMs = e.lastSyncMs,
                    isThisDevice = e.id == thisId,
                )
            }
            .sortedWith(compareByDescending<DeviceRow> { it.isThisDevice }.thenByDescending { it.lastSyncMs })
    }

    private fun toggleSync() {
        val newValue = !_state.value.crossDeviceSyncEnabled
        appSettings.putBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, newValue)
        _state.update { it.copy(crossDeviceSyncEnabled = newValue) }
        if (newValue) {
            viewModelScope.launch {
                syncPuller.pullNow()
                refresh()
            }
        }
    }

    private fun startRename() {
        _state.update { it.copy(isRenaming = true, renameDraft = it.thisDeviceName) }
    }

    private fun submitRename() {
        val draft = _state.value.renameDraft.trim()
        if (draft.isEmpty()) {
            _state.update { it.copy(isRenaming = false) }
            return
        }
        viewModelScope.launch {
            registry.rename(draft)
            _state.update { it.copy(isRenaming = false) }
            refresh()
        }
    }

    private fun removeDevice(id: String) {
        viewModelScope.launch {
            registry.remove(id)
            refresh()
        }
    }
}
