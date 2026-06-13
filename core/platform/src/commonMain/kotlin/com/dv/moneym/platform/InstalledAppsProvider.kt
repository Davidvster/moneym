package com.dv.moneym.platform

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null,
)

/**
 * Lists user-launchable installed apps so the user can choose which ones to capture payment
 * notifications from. Android-backed; iOS returns an empty list.
 */
interface InstalledAppsProvider {
    suspend fun installedApps(): List<InstalledApp>
}
