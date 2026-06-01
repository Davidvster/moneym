package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.PrefKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeviceIdentityTest {

    @Test
    fun deviceIdStableAndPersisted() {
        val settings = InMemoryAppSettings()
        val identity = DeviceIdentity(settings)
        val first = identity.deviceId()
        val second = identity.deviceId()
        assertEquals(first, second)
        assertTrue(first.isNotBlank())
        assertEquals(first, settings.getString(PrefKeys.DEVICE_ID))
    }

    @Test
    fun deviceNameDefaultsThenRespectsOverride() {
        val settings = InMemoryAppSettings()
        val identity = DeviceIdentity(settings)
        val default = identity.deviceName()
        assertNotNull(default)
        assertTrue(default.isNotBlank())

        identity.setDeviceName("My Phone")
        assertEquals("My Phone", identity.deviceName())
        assertEquals("My Phone", settings.getString(PrefKeys.DEVICE_NAME))
    }
}
