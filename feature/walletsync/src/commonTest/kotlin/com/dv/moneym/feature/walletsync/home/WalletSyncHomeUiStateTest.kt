package com.dv.moneym.feature.walletsync.home

import com.dv.moneym.platform.InstalledApp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletSyncHomeUiStateTest {

    @Test
    fun suggestedAppsIncludesPackagesAcrossRegionsAndOtherAppsExcludesThem() {
        val suggestedGlobal = InstalledApp("com.google.android.apps.walletnfcrel", "Google Wallet")
        val suggestedEurope = InstalledApp("de.number26.android", "N26")
        val suggestedUs = InstalledApp("com.venmo", "Venmo")
        val suggestedAsia = InstalledApp("com.phonepe.app", "PhonePe")
        val nonSuggested = InstalledApp("com.example.notes", "Notes")

        val state = WalletSyncHomeUiState(
            installedApps = listOf(suggestedGlobal, suggestedEurope, suggestedUs, suggestedAsia, nonSuggested),
        )

        val suggestedPackages = state.suggestedApps.map { it.packageName }
        val otherPackages = state.otherApps.map { it.packageName }

        assertTrue("com.google.android.apps.walletnfcrel" in suggestedPackages)
        assertTrue("de.number26.android" in suggestedPackages)
        assertTrue("com.venmo" in suggestedPackages)
        assertTrue("com.phonepe.app" in suggestedPackages)
        assertTrue("com.example.notes" in otherPackages)

        assertFalse("com.google.android.apps.walletnfcrel" in otherPackages)
        assertFalse("de.number26.android" in otherPackages)
        assertFalse("com.venmo" in otherPackages)
        assertFalse("com.phonepe.app" in otherPackages)
    }
}
