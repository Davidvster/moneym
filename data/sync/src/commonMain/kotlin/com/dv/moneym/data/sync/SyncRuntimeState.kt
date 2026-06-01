package com.dv.moneym.data.sync

import kotlinx.serialization.Serializable

@Serializable
sealed interface SyncRuntimeState {
    @Serializable data object Idle : SyncRuntimeState
    @Serializable data object Pulling : SyncRuntimeState
    @Serializable data object Applying : SyncRuntimeState
    @Serializable data object Pushing : SyncRuntimeState
    @Serializable data class Error(val message: String) : SyncRuntimeState
}
