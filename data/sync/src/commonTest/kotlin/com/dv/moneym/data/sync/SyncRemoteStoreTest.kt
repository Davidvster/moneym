package com.dv.moneym.data.sync

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncRemoteStoreTest {

    @Test
    fun readSnapshotReturnsNullWhenAbsent() = runTest {
        val store = SyncRemoteStore(FakeRemoteBackupProvider())
        assertNull(store.readSnapshotBytes())
    }

    @Test
    fun secondWriteUpdatesSameFile() = runTest {
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        store.writeSnapshotBytes("v1".encodeToByteArray())
        store.writeSnapshotBytes("v2".encodeToByteArray())
        assertEquals(1, provider.uploadCount)
        assertEquals("v2", store.readSnapshotBytes()!!.decodeToString())
    }

    @Test
    fun devicesRoundTrip() = runTest {
        val store = SyncRemoteStore(FakeRemoteBackupProvider())
        store.writeDevicesBytes("devices".encodeToByteArray())
        assertEquals("devices", store.readDevicesBytes()!!.decodeToString())
    }

    @Test
    fun snapshotAndDevicesAreSeparateFiles() = runTest {
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        store.writeSnapshotBytes("snap".encodeToByteArray())
        store.writeDevicesBytes("dev".encodeToByteArray())
        assertEquals("snap", store.readSnapshotBytes()!!.decodeToString())
        assertEquals("dev", store.readDevicesBytes()!!.decodeToString())
        assertEquals(2, provider.uploadCount)
    }
}
