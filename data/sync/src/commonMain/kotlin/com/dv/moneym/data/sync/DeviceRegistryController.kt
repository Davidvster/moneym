package com.dv.moneym.data.sync

/**
 * Minimal device-registry surface the settings UI depends on. [DeviceRegistryManager] implements
 * it. Keeping this an interface lets the ViewModel be tested without a remote store.
 */
interface DeviceRegistryController {
    val thisDeviceId: String
    suspend fun load(): List<DeviceEntry>
    suspend fun rename(name: String)
    suspend fun remove(deviceId: String)
}
