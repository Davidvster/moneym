package com.dv.moneym.platform

import android.content.Context

actual class AppInfo(private val context: Context) {
    actual val appName: String
        get() = context.packageManager.getApplicationLabel(context.applicationInfo).toString()

    actual val versionName: String
        get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}
