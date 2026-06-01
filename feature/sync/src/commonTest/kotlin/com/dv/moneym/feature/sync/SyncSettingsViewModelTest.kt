package com.dv.moneym.feature.sync

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.data.sync.DeviceEntry
import com.dv.moneym.data.sync.DeviceRegistryController
import com.dv.moneym.data.sync.SyncPuller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeRegistry(
    override val thisDeviceId: String = "self",
    initial: List<DeviceEntry> = emptyList(),
) : DeviceRegistryController {
    var devices: List<DeviceEntry> = initial
    var renamedTo: String? = null
        private set
    var removed: String? = null
        private set

    override suspend fun load(): List<DeviceEntry> = devices
    override suspend fun rename(name: String) {
        renamedTo = name
        devices = devices.map { if (it.id == thisDeviceId) it.copy(displayName = name) else it }
    }
    override suspend fun remove(deviceId: String) {
        removed = deviceId
        devices = devices.filter { it.id != deviceId }
    }
}

private class FakePuller : SyncPuller {
    var pullCount = 0
        private set
    override suspend fun pullNow(): Result<Unit> {
        pullCount++
        return Result.success(Unit)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SyncSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun entry(id: String, name: String = id, last: Long = 0L) =
        DeviceEntry(id = id, displayName = name, platform = "Android", lastSyncMs = last)

    private fun vm(
        registry: FakeRegistry = FakeRegistry(),
        settings: FakeAppSettings = FakeAppSettings(),
        puller: FakePuller = FakePuller(),
    ) = SyncSettingsViewModel(registry, settings, puller)

    private suspend fun ReceiveTurbine<SyncSettingsUiState>.awaitSettled(): SyncSettingsUiState {
        var s = awaitItem()
        while (s.isLoading) s = awaitItem()
        return s
    }

    @Test
    fun thisDeviceFlaggedFirstAndNotRemovable() = runTest(testDispatcher) {
        val registry = FakeRegistry(
            thisDeviceId = "self",
            initial = listOf(entry("other", last = 100L), entry("self", last = 1L)),
        )
        val vm = vm(registry = registry)

        vm.state.test {
            skipItems(1) // initial default
            val s = awaitSettled()
            assertEquals(2, s.devices.size)
            // this-device sorted first despite lower lastSyncMs
            assertEquals("self", s.devices.first().id)
            assertTrue(s.devices.first().isThisDevice)
            assertFalse(s.devices[1].isThisDevice)
        }
    }

    @Test
    fun togglePersistsAndPullsOnEnable() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val puller = FakePuller()
        val vm = vm(settings = settings, puller = puller)

        vm.state.test {
            skipItems(1)
            awaitSettled() // populated, disabled
            vm.onIntent(SyncSettingsIntent.ToggleSync)
            val enabled = awaitItem()
            assertTrue(enabled.crossDeviceSyncEnabled)
            // enabling kicks pullNow() then refresh — drain to its settled emission
            awaitSettled()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(settings.getBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED))
        assertEquals(1, puller.pullCount)
    }

    @Test
    fun toggleDisableDoesNotPull() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        settings.putBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, true)
        val puller = FakePuller()
        val vm = vm(settings = settings, puller = puller)

        vm.state.test {
            skipItems(1)
            awaitSettled() // populated, enabled
            vm.onIntent(SyncSettingsIntent.ToggleSync)
            val disabled = awaitItem()
            assertFalse(disabled.crossDeviceSyncEnabled)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(settings.getBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED))
        assertEquals(0, puller.pullCount)
    }

    @Test
    fun renameCallsRegistryAndRefreshes() = runTest(testDispatcher) {
        val registry = FakeRegistry(initial = listOf(entry("self", name = "Old")))
        val settings = FakeAppSettings()
        settings.putString(PrefKeys.DEVICE_NAME, "Old")
        val vm = vm(registry = registry, settings = settings)

        vm.state.test {
            skipItems(1)
            awaitSettled()
            vm.onIntent(SyncSettingsIntent.StartRename)
            assertTrue(awaitItem().isRenaming)
            vm.onIntent(SyncSettingsIntent.RenameDraftChanged("New Name"))
            assertEquals("New Name", awaitItem().renameDraft)
            vm.onIntent(SyncSettingsIntent.SubmitRename)
            // submit clears isRenaming, then refresh re-reads registry
            var after = awaitItem()
            while (after.isRenaming || after.isLoading ||
                after.devices.single { it.isThisDevice }.displayName != "New Name"
            ) {
                after = awaitItem()
            }
            assertFalse(after.isRenaming)
            assertEquals("New Name", after.devices.single { it.isThisDevice }.displayName)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("New Name", registry.renamedTo)
    }

    @Test
    fun removeDropsDevice() = runTest(testDispatcher) {
        val registry = FakeRegistry(
            initial = listOf(entry("self"), entry("other")),
        )
        val vm = vm(registry = registry)

        vm.state.test {
            skipItems(1)
            val populated = awaitSettled()
            assertEquals(2, populated.devices.size)
            vm.onIntent(SyncSettingsIntent.RemoveDevice("other"))
            var after = awaitItem()
            while (after.isLoading || after.devices.size != 1) after = awaitItem()
            assertEquals(1, after.devices.size)
            assertNull(after.devices.firstOrNull { it.id == "other" })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("other", registry.removed)
    }
}
