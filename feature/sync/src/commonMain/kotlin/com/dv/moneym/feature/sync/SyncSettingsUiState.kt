package com.dv.moneym.feature.sync

import kotlinx.serialization.Serializable

@Serializable
data class DeviceRow(
    val id: String,
    val displayName: String,
    val platform: String,
    val lastSyncMs: Long,
    val isThisDevice: Boolean,
)

@Serializable
data class SyncSettingsUiState(
    val crossDeviceSyncEnabled: Boolean = false,
    val thisDeviceName: String = "",
    val isRenaming: Boolean = false,
    val isRenameSaving: Boolean = false,
    val renameDraft: String = "",
    val devices: List<DeviceRow> = emptyList(),
    val removingIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
)
