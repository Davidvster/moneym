package com.dv.moneym.platform

actual fun deviceModelName(): String = android.os.Build.MODEL ?: "Android"

actual fun devicePlatformName(): String = "Android"
