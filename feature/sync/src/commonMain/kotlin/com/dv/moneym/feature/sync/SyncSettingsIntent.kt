package com.dv.moneym.feature.sync

sealed interface SyncSettingsIntent {
    data object ToggleSync : SyncSettingsIntent
    data object StartRename : SyncSettingsIntent
    data class RenameDraftChanged(val value: String) : SyncSettingsIntent
    data object SubmitRename : SyncSettingsIntent
    data object CancelRename : SyncSettingsIntent
    data class RemoveDevice(val id: String) : SyncSettingsIntent
    data object Refresh : SyncSettingsIntent
}
