# Phase 1: About Section

## Goal
Add "About" section at bottom of Settings. New `feature/about` module with Terms & Conditions and Privacy Policy rows, both opening `https://davidvster.github.io/moneym.github.io/`.

## Files to create

### `feature/about/build.gradle.kts`
Copy pattern from `feature/infopage/build.gradle.kts`. Dependencies: compose.runtime, compose.foundation, compose.material3, compose.ui, compose.components.resources, navigation3.runtime, core.designsystem, core.ui, core.model, core.navigation. baseName = `"FeatureAbout"`. namespace = `"com.dv.moneym.feature.about"`.

### `feature/about/src/commonMain/kotlin/com/dv/moneym/feature/about/AboutScreen.kt`
- `@Serializable data object AboutKey : NavKey`
- `fun EntryProviderScope<NavKey>.aboutEntry(onBack: () -> Unit, metadata: Map<String,Any> = emptyMap())` — entry function (no VM)
- `AboutScreen` composable: `ScreenHeader` + `MmCard` + two `MmSettingsRow` rows
  - "Terms & Conditions" + "Privacy Policy" both open `https://davidvster.github.io/moneym.github.io/` via `LocalUriHandler.current.openUri(url)`
  - Use `Icon.Doc` or `Icon.Info` (check what exists in `MmIconsExtra.kt`) for leading icons; fallback to `Icons.Default.Info` / `Icons.Default.Description`

### `feature/about/src/commonMain/composeResources/values/strings.xml`
Keys: `settings_about`, `settings_about_terms`, `settings_about_privacy`

### Same keys in `values-de/`, `values-es/`, `values-it/strings.xml`

## Files to modify

### `settings.gradle.kts`
Add `include(":feature:about")` after `:feature:infopage` line (line 64).

### `composeApp/build.gradle.kts`
Add `implementation(projects.feature.about)` to `commonMain.dependencies`.

### `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SettingsScreen.kt`
1. Add `ABOUT_LABEL`, `ABOUT_CARD` to `SettingsItem` enum (after `VERSION`)
2. Add `onNavigateToAbout: () -> Unit = {}` param to `settingsEntry()` and `SettingsScreen()`
3. Pass `onNavigateToAbout` through `settingsEntry` → `SettingsScreen` → `SettingsLazyList`

### `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/components/SettingsLazyList.kt`
1. Add `onNavigateToAbout: () -> Unit` param
2. Add items after `BACKUP_CARD` and before `VERSION`:
   - `SectionLabel` "About" (key = `SettingsItem.ABOUT_LABEL.name`)
   - `MmCard` with `MmSettingsRow` "About" row (key = `SettingsItem.ABOUT_CARD.name`, onClick = `onNavigateToAbout`)
3. Add string resource import for `settings_about`

### `composeApp/src/commonMain/kotlin/com/dv/moneym/MainNav.kt`
1. Add `onNavigateToAbout = { tabBackStack.push(AboutKey) }` to `settingsEntry(...)` call
2. Add `aboutEntry(onBack = { tabBackStack.removeLast() }, metadata = modalTransitionMeta)` call

## No ViewModel, no Koin registration needed
About screen is static content (no state, no DI).

## Verification
- `./gradlew :composeApp:assembleDebug` — must pass
- Open Settings → scroll to bottom → "About" section visible → tap → About screen opens → tap Terms or Privacy → browser opens `https://davidvster.github.io/moneym.github.io/`
