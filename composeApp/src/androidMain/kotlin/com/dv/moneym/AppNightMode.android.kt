package com.dv.moneym

import androidx.appcompat.app.AppCompatDelegate
import com.dv.moneym.core.model.ThemeMode

actual fun applyAppNightMode(themeMode: ThemeMode) {
    val mode = when (themeMode) {
        ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.Auto -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    if (AppCompatDelegate.getDefaultNightMode() != mode) {
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
