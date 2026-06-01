package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceRegistryManagerTest {

    private fun manager(
        provider: FakeRemoteBackupProvider = FakeRemoteBackupProvider(),
        settings: InMemoryAppSettings = InMemoryAppSettings(),
        now: Long = 1_000L,
    ): DeviceRegistryManager {
        settings.putString(PrefKeys.DEVICE_ID, "self")
        settings.putString(PrefKeys.DEVICE_NAME, "My Phone")
        return DeviceRegistryManager(
            store = SyncRemoteStore(provider),
            deviceIdentity = DeviceIdentity(settings),
            nowMs = { now },
        )
    }

    @Test
    fun loadReturnsEmptyWhenAbsent() = runTest {
        assertEquals(emptyList(), manager().load())
    }

    @Test
    fun loadToleratesGarbage() = runTest {
        val provider = FakeRemoteBackupProvider()
        SyncRemoteStore(provider).writeDevicesBytes("not json".encodeToByteArray())
        assertEquals(emptyList(), manager(provider).load())
    }

    @Test
    fun touchSelfUpsertsWithoutDuplicateOnRepeat() = runTest {
        val provider = FakeRemoteBackupProvider()
        val mgr = manager(provider)
        mgr.touchSelf()
        mgr.touchSelf()
        val devices = mgr.load()
        assertEquals(1, devices.size)
        val self = devices.single()
        assertEquals("self", self.id)
        assertEquals("My Phone", self.displayName)
        assertEquals(1_000L, self.lastSyncMs)
    }

    @Test
    fun touchSelfPreservesOtherDevices() = runTest {
        val provider = FakeRemoteBackupProvider()
        val mgr = manager(provider)
        // Seed another device.
        SyncRemoteStore(provider).writeDevicesBytes(
            "{\"formatVersion\":1,\"devices\":[{\"id\":\"other\",\"displayName\":\"iPad\",\"platform\":\"iOS\",\"lastSyncMs\":5}]}".encodeToByteArray(),
        )
        mgr.touchSelf()
        val devices = mgr.load()
        assertEquals(2, devices.size)
        assertTrue(devices.any { it.id == "other" })
        assertTrue(devices.any { it.id == "self" })
    }

    @Test
    fun renameUpdatesNameAndUpserts() = runTest {
        val provider = FakeRemoteBackupProvider()
        val mgr = manager(provider)
        mgr.touchSelf()
        mgr.rename("Work Phone")
        val self = mgr.load().single { it.id == "self" }
        assertEquals("Work Phone", self.displayName)
    }

    @Test
    fun removeDropsEntry() = runTest {
        val provider = FakeRemoteBackupProvider()
        val mgr = manager(provider)
        SyncRemoteStore(provider).writeDevicesBytes(
            "{\"formatVersion\":1,\"devices\":[{\"id\":\"other\",\"displayName\":\"iPad\",\"platform\":\"iOS\",\"lastSyncMs\":5}]}".encodeToByteArray(),
        )
        mgr.touchSelf()
        mgr.remove("other")
        val devices = mgr.load()
        assertEquals(1, devices.size)
        assertEquals("self", devices.single().id)
        assertNull(devices.firstOrNull { it.id == "other" })
    }
}
