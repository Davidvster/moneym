# Phase 3: Theme Switch Activity Recreation Loop

## Root Cause

**File:** `composeApp/src/androidMain/AndroidManifest.xml`

`MainActivity` has no `android:configChanges` attribute. When the user switches dark→light mode while system is in dark mode:

1. `SettingsOverviewViewModel.setThemeMode(ThemeMode.Light)` persists the choice
2. `App.kt` `LaunchedEffect(themeMode)` → `applyAppNightMode(ThemeMode.Light)` → `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`
3. Android detects `uiMode` config change → **Activity recreates**
4. New Activity starts with `initialValue = ThemeMode.Auto` (the flow initial value before first emission)
5. `LaunchedEffect(ThemeMode.Auto)` fires → `setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)` → system is dark → **Activity recreates again**
6. **Infinite loop**

## Fix

**File:** `composeApp/src/androidMain/AndroidManifest.xml`

Add `android:configChanges="uiMode|screenLayout|screenSize|smallestScreenSize"` to `MainActivity`:

```xml
<activity
    android:exported="true"
    android:name=".MainActivity"
    android:configChanges="uiMode|screenLayout|screenSize|smallestScreenSize">
```

`screenLayout|screenSize|smallestScreenSize` are standard additions to prevent unnecessary recreations on orientation/size changes (they're common in modern Compose apps). The critical one is `uiMode`.

With `uiMode` in `configChanges`, `setDefaultNightMode()` calls do NOT trigger Activity recreation. Compose handles the theme reactively through the `themeMode` state flow.

## No other changes needed
`applyAppNightMode()` already has the guard `if (AppCompatDelegate.getDefaultNightMode() != mode)`. The Compose UI already observes theme changes via `appSettingsRepo.observeThemeMode()`. Navigation state is preserved because there's no recreation.

## Verification
- Build: `./gradlew :composeApp:assembleDebug`
- Install on device in system dark mode
- Open Settings → Appearance → switch to Light
- App theme changes immediately, no recreation, stays on Settings screen
- Switch back to Auto/Dark → works
- No "Schedule relaunch activity" spam in logcat
