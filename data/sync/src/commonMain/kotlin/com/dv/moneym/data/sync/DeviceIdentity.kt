package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.platform.deviceModelName
import com.dv.moneym.platform.devicePlatformName
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DeviceIdentity(
    private val appSettings: AppSettings,
) {

    @OptIn(ExperimentalUuidApi::class)
    fun deviceId(): String {
        appSettings.getString(PrefKeys.DEVICE_ID)?.let { return it }
        val generated = Uuid.random().toString()
        appSettings.putString(PrefKeys.DEVICE_ID, generated)
        return generated
    }

    fun deviceName(): String {
        appSettings.getString(PrefKeys.DEVICE_NAME)?.let { return it }
        val default = deviceModelName()
        appSettings.putString(PrefKeys.DEVICE_NAME, default)
        return default
    }

    fun setDeviceName(name: String) {
        appSettings.putString(PrefKeys.DEVICE_NAME, name)
    }

    fun platform(): String = devicePlatformName()
}
