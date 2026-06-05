package com.dv.moneym

import com.dv.moneym.core.model.ThemeMode

/**
 * Syncs the platform window/system night mode to the app's persisted [ThemeMode].
 * On Android this drives the DayNight window background so the first frame and the
 * NavDisplay crossfade match the app theme. On iOS this is a no-op.
 */
expect fun applyAppNightMode(themeMode: ThemeMode)
