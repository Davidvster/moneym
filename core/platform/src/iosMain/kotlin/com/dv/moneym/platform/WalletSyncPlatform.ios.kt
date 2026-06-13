package com.dv.moneym.platform

class NoopNotificationAccessController : NotificationAccessController {
    override fun isAccessGranted(): Boolean = false
    override fun openAccessSettings() = Unit
}

class NoopInstalledAppsProvider : InstalledAppsProvider {
    override suspend fun installedApps(): List<InstalledApp> = emptyList()
}
