package com.dv.moneym.platform

import platform.UIKit.UIDevice

actual fun deviceModelName(): String {
    val device = UIDevice.currentDevice
    val name = device.name
    return name.ifBlank { device.model }
}

actual fun devicePlatformName(): String = "iOS"
