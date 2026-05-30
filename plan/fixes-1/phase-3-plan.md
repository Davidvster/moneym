# Phase 3 — Theme flicker fix (task 4)

## Symptoms
- White flicker when navigating Overview → Settings (light mode).
- Dark flicker when the app theme is Light but the **system** is in dark mode.

## Root cause (verified)
- `MainNav.kt` and `OnboardingNav.kt` render `NavDisplay` with 220ms fade transitions but
  **no background** behind it. During the crossfade the Android **window background** shows
  through → white (or, on a dark system, the DayNight window bg flashes **dark**).
- The activity uses `Theme.AppCompat.DayNight.NoActionBar` (`themes.xml`). Its window
  background follows the **system** night mode. The app picks its own theme from a datastore
  `ThemeMode` setting (App.kt), so when the app forces Light on a dark system, the window bg
  is dark → the dark flash.

## Fixes

### Fix A — paint a themed background behind NavDisplay (primary)
- `composeApp/.../MainNav.kt`: wrap the `NavDisplay(...)` call in
  `Box(modifier = Modifier.fillMaxSize().background(MM.colors.bg)) { NavDisplay(...) }`.
  Add imports: `androidx.compose.foundation.background`,
  `androidx.compose.foundation.layout.Box`,
  `androidx.compose.foundation.layout.fillMaxSize`, `androidx.compose.ui.Modifier`,
  `com.dv.moneym.core.designsystem.MM`.
- `composeApp/.../OnboardingNav.kt`: same wrap around its `NavDisplay`.
- `MM.colors.bg` follows the **app** theme (set by `MoneyMTheme` at the App root), so the
  fade now reveals the correct themed colour, not the window.

### Fix B — make the window/DayNight match the app theme (kills the dark flash)
The window bg must follow the **app's** ThemeMode, not the system. Sync AppCompat's night
mode to the persisted ThemeMode so `DayNight` resolves correctly.
- Add a tiny platform hook to apply night mode on Android. Cleanest: in
  `composeApp/androidMain/.../MainActivity.kt`, observe/read the persisted `ThemeMode` and
  call `androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(...)`:
  - `ThemeMode.Dark → MODE_NIGHT_YES`
  - `ThemeMode.Light → MODE_NIGHT_NO`
  - `ThemeMode.Auto → MODE_NIGHT_FOLLOW_SYSTEM`
  Read the setting via Koin (`AppSettingsRepository.observeThemeMode()`) — call
  `setDefaultNightMode` as early as possible (in `onCreate` before `setContent`, collecting
  the first value, or apply on each emission). Keep it minimal; if reading synchronously in
  onCreate is awkward, apply it from a `LaunchedEffect(themeMode)` in `App()` AND also set a
  sensible default in onCreate.
- `composeApp/androidMain/res/values/themes.xml`: add an explicit DayNight window background
  so once night mode is synced the first frame matches. Add
  `<item name="android:windowBackground">@color/window_background</item>` and define
  `window_background` in `androidMain/res/values/colors.xml` (white `#FFFFFFFF`) +
  `androidMain/res/values-night/colors.xml` (black `#FF000000`) to mirror
  `MoneyMColors.bg` (light `#FFFFFFFF`, dark `#FF000000`). Create the colors.xml files if
  absent. Keep the existing splashscreen items.

## Notes / constraints
- Don't change the fade durations or transition specs.
- iOS has no AppCompat/window-bg concept — Fix A (the Box wrap in the shared nav files)
  already covers it; no iOS-specific change needed.
- Verify `MM.colors.bg` is the correct accessor (see existing screens, e.g.
  `OverviewScreen.kt` uses `colors.bg` via `val colors = MM.colors`).

## Build / verify
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- `./gradlew :composeApp:assembleDebug` (catches the Android res/colors changes).
- Report files changed, how you wired Fix B (onCreate vs LaunchedEffect), and build result.
