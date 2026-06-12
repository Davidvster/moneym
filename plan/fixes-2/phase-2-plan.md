# Phase 2 — Android status bar icons follow app theme (not system theme)

## Bug

App theme set to Light while Android system is in dark mode → status bar icons stay white on the app's light background. `androidApp/src/main/kotlin/com/dv/moneym/MainActivity.kt` calls `enableEdgeToEdge()` once before `setContent`; icon appearance follows system dark mode, never the in-app `ThemeMode` setting.

## Fix

Follow the existing `AppNightMode` expect/actual pattern in `shared/`:

1. **New file** `shared/src/commonMain/kotlin/com/dv/moneym/SystemBarStyleEffect.kt`:
```kotlin
package com.dv.moneym

import androidx.compose.runtime.Composable

@Composable
expect fun SystemBarStyleEffect(isDark: Boolean)
```

2. **New file** `shared/src/androidMain/kotlin/com/dv/moneym/SystemBarStyleEffect.android.kt`:
```kotlin
package com.dv.moneym

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SystemBarStyleEffect(isDark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
    }
}
```
If `androidx.core.view.WindowCompat` is not on the shared androidMain classpath, check `gradle/libs.versions.toml` for an androidx-core artifact and add it to `shared/build.gradle.kts` androidMain dependencies (catalog only, no hardcoded versions). It is likely already available transitively via activity-compose.

3. **New file** `shared/src/iosMain/kotlin/com/dv/moneym/SystemBarStyleEffect.ios.kt`:
```kotlin
package com.dv.moneym

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBarStyleEffect(isDark: Boolean) {
}
```

4. **Edit** `shared/src/commonMain/kotlin/com/dv/moneym/App.kt` — after `isDark` computed (around line 145, right next to `LaunchedEffect(themeMode) { applyAppNightMode(themeMode) }` at line 147), add:
```kotlin
SystemBarStyleEffect(isDark)
```
Same package, no import needed.

## Verification

```bash
./gradlew :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 --no-configuration-cache
```
Both must pass. Do NOT commit. No code comments.
