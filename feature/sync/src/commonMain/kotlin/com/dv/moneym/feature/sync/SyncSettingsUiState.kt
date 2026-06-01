package com.dv.moneym.feature.sync

data class DeviceRow(
    val id: String,
    val displayName: String,
    val platform: String,
    val lastSyncMs: Long,
    val isThisDevice: Boolean,
)

data class SyncSettingsUiState(
    val crossDeviceSyncEnabled: Boolean = false,
    val thisDeviceName: String = "",
    val isRenaming: Boolean = false,
    val renameDraft: String = "",
    val devices: List<DeviceRow> = emptyList(),
    val isLoading: Boolean = false,
)
