package com.dv.moneym.data.sync

import kotlinx.serialization.json.Json

/**
 * Maintains the participating-device list in `devices.json` (plaintext — not routed through the
 * encrypted snapshot codec). Each device upserts its own entry on every successful sync.
 */
class DeviceRegistryManager(
    private val store: SyncRemoteStore,
    private val deviceIdentity: DeviceIdentity,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
    private val nowMs: () -> Long,
) : DeviceRegistryController {

    override val thisDeviceId: String = deviceIdentity.deviceId()

    override suspend fun load(): List<DeviceEntry> {
        val bytes = store.readDevicesBytes() ?: return emptyList()
        return runCatching {
            json.decodeFromString(DeviceRegistry.serializer(), bytes.decodeToString()).devices
        }.getOrDefault(emptyList())
    }

    suspend fun touchSelf() {
        val self = DeviceEntry(
            id = deviceIdentity.deviceId(),
            displayName = deviceIdentity.deviceName(),
            platform = deviceIdentity.platform(),
            lastSyncMs = nowMs(),
        )
        val merged = load().filter { it.id != self.id } + self
        write(merged)
    }

    override suspend fun rename(name: String) {
        deviceIdentity.setDeviceName(name)
        touchSelf()
    }

    override suspend fun remove(deviceId: String) {
        val remaining = load().filter { it.id != deviceId }
        write(remaining)
    }

    private suspend fun write(devices: List<DeviceEntry>) {
        val bytes = json
            .encodeToString(DeviceRegistry.serializer(), DeviceRegistry(devices = devices))
            .encodeToByteArray()
        store.writeDevicesBytes(bytes)
    }
}
