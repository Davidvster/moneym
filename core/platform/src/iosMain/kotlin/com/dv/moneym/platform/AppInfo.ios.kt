package com.dv.moneym.platform

import platform.Foundation.NSBundle

actual class AppInfo {
    actual val appName: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String
            ?: NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleName") as? String
            ?: "MoneyM"

    actual val versionName: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            ?: NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
            ?: "0"
}
