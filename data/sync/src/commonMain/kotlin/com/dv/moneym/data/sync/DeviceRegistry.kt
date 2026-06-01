package com.dv.moneym.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class DeviceEntry(
    val id: String,
    val displayName: String,
    val platform: String,
    val lastSyncMs: Long,
)

@Serializable
data class DeviceRegistry(
    val formatVersion: Int = 1,
    val devices: List<DeviceEntry> = emptyList(),
)
