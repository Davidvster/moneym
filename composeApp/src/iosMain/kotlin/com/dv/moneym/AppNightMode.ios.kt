package com.dv.moneym

import com.dv.moneym.core.model.ThemeMode

actual fun applyAppNightMode(themeMode: ThemeMode) {
    // iOS has no AppCompat/window-bg night mode; the themed Box behind NavDisplay covers it.
}
